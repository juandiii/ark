package xyz.juandiii.ark.proxy;

import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.interceptor.RequestContext;
import xyz.juandiii.ark.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Pattern;

/**
 * Dynamic proxy factory for creating HTTP clients from annotated interfaces.
 * Auto-detects annotation framework (Spring @HttpExchange, JAX-RS @Path) and
 * execution model (sync, async, reactor, mutiny) via classpath detection.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkProxy {

    private ArkProxy() {}

    private static final Pattern DOUBLE_SLASH = Pattern.compile("//+");

    private static final String[][] PROXY_PROVIDERS = {
            {"jakarta.ws.rs.Path", "jakarta.ws.rs.GET", "xyz.juandiii.ark.proxy.jaxrs.JaxRsProxyProvider"},
            {"org.springframework.web.service.annotation.HttpExchange", null, "xyz.juandiii.ark.proxy.spring.SpringProxyProvider"}
    };

    private static final String[][] EXECUTION_MODEL_PROVIDERS = {
            {"xyz.juandiii.ark.async.AsyncArk", "xyz.juandiii.ark.async.proxy.AsyncExecutionModelProvider"},
            {"xyz.juandiii.ark.reactor.ReactorArk", "xyz.juandiii.ark.reactor.proxy.ReactorExecutionModelProvider"},
            {"xyz.juandiii.ark.mutiny.MutinyArk", "xyz.juandiii.ark.mutiny.proxy.MutinyExecutionModelProvider"}
    };

    /**
     * Creates a proxy with auto-detection of annotation framework and execution model.
     *
     * @param clientInterface the annotated interface
     * @param arkClient the Ark client (Ark, AsyncArk, ReactorArk, MutinyArk)
     */
    public static <T> T create(Class<T> clientInterface, Object arkClient) {
        ProxyProviderAccess proxyProvider = resolveProxyProvider(clientInterface);
        ExecutionModelAccess modelProvider = resolveExecutionModelProvider(arkClient);

        return create(clientInterface,
                modelProvider.dispatcher(arkClient),
                proxyProvider.annotationResolver(),
                proxyProvider.parameterBinder(),
                modelProvider.returnTypeHandler());
    }

    /**
     * Creates a proxy with explicit collaborators — for advanced use or custom execution models.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clientInterface,
                               RequestDispatcher dispatcher,
                               AnnotationResolver annotationResolver,
                               ParameterBinder parameterBinder,
                               ReturnTypeHandler returnTypeHandler) {
        return create(clientInterface, dispatcher, "", annotationResolver, parameterBinder, returnTypeHandler);
    }

    @SuppressWarnings("unchecked")
    static <T> T create(Class<T> clientInterface, RequestDispatcher dispatcher, String parentPath,
                         AnnotationResolver annotationResolver,
                         ParameterBinder parameterBinder,
                         ReturnTypeHandler returnTypeHandler) {
        if (!clientInterface.isInterface()) {
            throw new ArkException(clientInterface.getName() + " must be an interface");
        }

        return (T) Proxy.newProxyInstance(
                clientInterface.getClassLoader(),
                new Class<?>[]{clientInterface},
                new ProxyInvocationHandler(dispatcher, parentPath, annotationResolver, parameterBinder, returnTypeHandler)
        );
    }

    // --- Auto-detection via Class.forName ---

    private static ProxyProviderAccess resolveProxyProvider(Class<?> clientInterface) {
        for (String[] entry : PROXY_PROVIDERS) {
            if (matchesAnnotation(clientInterface, entry[0], entry[1])) {
                ProxyProviderAccess provider = instantiate(entry[2]);
                if (provider != null) return provider;
            }
        }
        throw new ArkException("No proxy provider found for " + clientInterface.getName()
                + ". Add ark-proxy-spring or ark-proxy-jaxrs to your classpath.");
    }

    private static ExecutionModelAccess resolveExecutionModelProvider(Object arkClient) {
        if (arkClient instanceof Ark) {
            return new SyncExecutionModelAccess();
        }
        for (String[] entry : EXECUTION_MODEL_PROVIDERS) {
            if (isInstance(arkClient, entry[0])) {
                ExecutionModelAccess provider = instantiate(entry[1]);
                if (provider != null) return provider;
            }
        }
        throw new ArkException("No execution model provider found for " + arkClient.getClass().getName()
                + ". Add the corresponding ark module to your classpath.");
    }

    private static final class SyncExecutionModelAccess implements ExecutionModelAccess {
        @Override
        public RequestDispatcher dispatcher(Object arkClient) {
            return Dispatchers.sync((Ark) arkClient);
        }

        @Override
        public ReturnTypeHandler returnTypeHandler() {
            return new SyncReturnTypeHandler();
        }
    }

    // --- Internal contracts for Class.forName providers ---

    public interface ProxyProviderAccess {
        AnnotationResolver annotationResolver();
        ParameterBinder parameterBinder();
    }

    public interface ExecutionModelAccess {
        RequestDispatcher dispatcher(Object arkClient);
        ReturnTypeHandler returnTypeHandler();
    }

    // --- Helpers ---

    private static boolean matchesAnnotation(Class<?> iface, String classAnnotation, String methodAnnotation) {
        if (hasAnnotation(iface, classAnnotation)) return true;
        return methodAnnotation != null && hasMethodAnnotation(iface, methodAnnotation);
    }

    private static boolean hasAnnotation(Class<?> iface, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> a = (Class<? extends Annotation>) Class.forName(annotationClassName);
            return iface.isAnnotationPresent(a);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasMethodAnnotation(Class<?> iface, String annotationClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> a = (Class<? extends Annotation>) Class.forName(annotationClassName);
            for (Method m : iface.getMethods()) {
                if (m.isAnnotationPresent(a)) return true;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    private static boolean isInstance(Object obj, String className) {
        try {
            return Class.forName(className).isInstance(obj);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(String className) {
        try {
            return (T) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new ArkException("Failed to instantiate " + className + ": " + e.getMessage());
        }
    }

    // --- InvocationHandler ---

    private static final class ProxyInvocationHandler implements InvocationHandler {

        private final RequestDispatcher dispatcher;
        private final String parentPath;
        private final AnnotationResolver annotationResolver;
        private final ParameterBinder parameterBinder;
        private final ReturnTypeHandler returnTypeHandler;

        private ProxyInvocationHandler(RequestDispatcher dispatcher, String parentPath,
                                       AnnotationResolver annotationResolver,
                                       ParameterBinder parameterBinder,
                                       ReturnTypeHandler returnTypeHandler) {
            this.dispatcher = dispatcher;
            this.parentPath = parentPath;
            this.annotationResolver = annotationResolver;
            this.parameterBinder = parameterBinder;
            this.returnTypeHandler = returnTypeHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> proxy.getClass().getInterfaces()[0].getName() + "@proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> method.invoke(this, args);
                };
            }

            if (annotationResolver.isSubResource(method)) {
                Class<?> returnClass = method.getReturnType();
                String basePath = combinePaths(parentPath, annotationResolver.resolveBasePath(method.getDeclaringClass()));
                String subPath = annotationResolver.resolveSubResourcePath(method, args);
                return ArkProxy.create(returnClass, dispatcher, combinePaths(basePath, subPath),
                        annotationResolver, parameterBinder, returnTypeHandler);
            }

            String basePath = combinePaths(parentPath, annotationResolver.resolveBasePath(method.getDeclaringClass()));
            AnnotationResolver.MethodInfo info = annotationResolver.resolveMethod(method);
            String resolvedPath = annotationResolver.resolvePath(combinePaths(basePath, info.path()), method, args);

            RequestContext request = dispatcher.dispatch(info.httpMethod(), resolvedPath);

            if (info.consumes() != null) {
                request.contentType(info.consumes());
            }
            if (info.produces() != null) {
                request.accept(info.produces());
            }

            parameterBinder.apply(request, method, args);

            return returnTypeHandler.handle(request, method.getGenericReturnType());
        }

        private static String combinePaths(String left, String right) {
            if (right == null || right.isEmpty() || right.equals("/")) return left;
            if (left == null || left.isEmpty() || left.equals("/")) return right;
            String combined = StringUtils.stripTrailingSlash(left) + StringUtils.ensureLeadingSlash(right);
            return DOUBLE_SLASH.matcher(combined).replaceAll("/");
        }
    }
}

