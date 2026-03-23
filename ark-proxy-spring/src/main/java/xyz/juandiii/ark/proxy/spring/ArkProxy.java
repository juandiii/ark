package xyz.juandiii.ark.proxy.spring;

import org.springframework.web.service.annotation.HttpExchange;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.ClientRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Dynamic proxy factory for creating HTTP clients from @HttpExchange annotated interfaces.
 *
 * @author Juan Diego Lopez V.
 */
public final class ArkProxy {

    private ArkProxy() {}

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clientInterface, Ark ark) {
        return create(clientInterface, ark, "");
    }

    @SuppressWarnings("unchecked")
    static <T> T create(Class<T> clientInterface, Ark ark, String parentPath) {
        if (!clientInterface.isInterface()) {
            throw new ArkException(clientInterface.getName() + " must be an interface");
        }

        return (T) Proxy.newProxyInstance(
                clientInterface.getClassLoader(),
                new Class<?>[]{clientInterface},
                new ArkInvocationHandler(ark, parentPath)
        );
    }

    private static final class ArkInvocationHandler implements InvocationHandler {

        private final Ark ark;
        private final String parentPath;
        private final AnnotationResolver annotationResolver = new AnnotationResolver();
        private final ParameterBinder parameterBinder = new ParameterBinder();
        private final ReturnTypeHandler returnTypeHandler = new ReturnTypeHandler();

        private ArkInvocationHandler(Ark ark, String parentPath) {
            this.ark = ark;
            this.parentPath = parentPath;
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

            Class<?> returnClass = method.getReturnType();
            if (returnClass.isInterface() && returnClass.isAnnotationPresent(HttpExchange.class)) {
                String currentBasePath = parentPath + annotationResolver.resolveBasePath(method.getDeclaringClass());
                return ArkProxy.create(returnClass, ark, currentBasePath);
            }

            String basePath = parentPath + annotationResolver.resolveBasePath(method.getDeclaringClass());
            AnnotationResolver.MethodInfo info = annotationResolver.resolveMethod(method);
            String resolvedPath = annotationResolver.resolvePath(basePath + info.path(), method, args);

            ClientRequest request = switch (info.httpMethod()) {
                case "GET" -> ark.get(resolvedPath);
                case "POST" -> ark.post(resolvedPath);
                case "PUT" -> ark.put(resolvedPath);
                case "PATCH" -> ark.patch(resolvedPath);
                case "DELETE" -> ark.delete(resolvedPath);
                default -> throw new ArkException("Unsupported HTTP method: " + info.httpMethod());
            };

            parameterBinder.apply(request, method, args);

            return returnTypeHandler.handle(request, method.getGenericReturnType());
        }
    }
}
