package xyz.juandiii.ark.proxy.spring;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import xyz.juandiii.ark.Ark;
import xyz.juandiii.ark.TypeRef;
import xyz.juandiii.ark.exceptions.ArkException;
import xyz.juandiii.ark.http.ArkResponse;
import xyz.juandiii.ark.http.ClientRequest;
import xyz.juandiii.ark.type.MediaType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

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
                return method.invoke(this, args);
            }

            // Check if return type is a sub-resource interface
            Class<?> returnClass = method.getReturnType();
            if (returnClass.isInterface() && returnClass.isAnnotationPresent(HttpExchange.class)) {
                String currentBasePath = parentPath + resolveBasePath(method.getDeclaringClass());
                return ArkProxy.create(returnClass, ark, currentBasePath);
            }

            String basePath = parentPath + resolveBasePath(method.getDeclaringClass());
            MethodInfo info = resolveMethod(method);
            String resolvedPath = resolvePath(basePath + info.path(), method, args);

            ClientRequest request = switch (info.httpMethod()) {
                case "GET" -> ark.get(resolvedPath);
                case "POST" -> ark.post(resolvedPath);
                case "PUT" -> ark.put(resolvedPath);
                case "PATCH" -> ark.patch(resolvedPath);
                case "DELETE" -> ark.delete(resolvedPath);
                default -> throw new ArkException("Unsupported HTTP method: " + info.httpMethod());
            };

            applyParameters(request, method, args);

            return executeAndReturn(request, method.getGenericReturnType());
        }

        private String resolveBasePath(Class<?> declaringClass) {
            HttpExchange exchange = declaringClass.getAnnotation(HttpExchange.class);
            return exchange != null ? exchange.url() : "";
        }

        private MethodInfo resolveMethod(Method method) {
            GetExchange get = method.getAnnotation(GetExchange.class);
            if (get != null) return new MethodInfo("GET", get.url());

            PostExchange post = method.getAnnotation(PostExchange.class);
            if (post != null) return new MethodInfo("POST", post.url());

            PutExchange put = method.getAnnotation(PutExchange.class);
            if (put != null) return new MethodInfo("PUT", put.url());

            PatchExchange patch = method.getAnnotation(PatchExchange.class);
            if (patch != null) return new MethodInfo("PATCH", patch.url());

            DeleteExchange delete = method.getAnnotation(DeleteExchange.class);
            if (delete != null) return new MethodInfo("DELETE", delete.url());

            throw new ArkException("No @HttpExchange annotation found on method: " + method.getName());
        }

        private String resolvePath(String path, Method method, Object[] args) {
            if (args == null) return path;
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                PathVariable pv = params[i].getAnnotation(PathVariable.class);
                if (pv != null) {
                    String name = pv.value().isEmpty() ? params[i].getName() : pv.value();
                    path = path.replace("{" + name + "}", String.valueOf(args[i]));
                }
            }
            return path;
        }

        private void applyParameters(ClientRequest request, Method method, Object[] args) {
            if (args == null) return;
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(PathVariable.class)) {
                    continue;
                }

                RequestParam rp = params[i].getAnnotation(RequestParam.class);
                if (rp != null) {
                    String name = rp.value().isEmpty() ? params[i].getName() : rp.value();
                    if (args[i] != null) {
                        request.queryParam(name, String.valueOf(args[i]));
                    }
                    continue;
                }

                RequestHeader rh = params[i].getAnnotation(RequestHeader.class);
                if (rh != null) {
                    String name = rh.value().isEmpty() ? params[i].getName() : rh.value();
                    if (args[i] != null) {
                        request.header(name, String.valueOf(args[i]));
                    }
                    continue;
                }

                if (params[i].isAnnotationPresent(RequestBody.class) || hasNoAnnotation(params[i])) {
                    applyBody(request, method, args[i]);
                }
            }
        }

        private void applyBody(ClientRequest request, Method method, Object body) {
            if (body instanceof MultiValueMap<?, ?> formData) {
                request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
                request.body(encodeFormData(formData));
            } else if (body instanceof Map<?, ?> map && isFormContentType(method)) {
                request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
                request.body(encodeMap(map));
            } else {
                request.body(body);
            }
        }

        private boolean isFormContentType(Method method) {
            HttpExchange exchange = method.getDeclaringClass().getAnnotation(HttpExchange.class);
            if (exchange != null && MediaType.APPLICATION_FORM_URLENCODED.equals(exchange.contentType())) {
                return true;
            }
            PostExchange post = method.getAnnotation(PostExchange.class);
            if (post != null && MediaType.APPLICATION_FORM_URLENCODED.equals(post.contentType())) {
                return true;
            }
            PutExchange put = method.getAnnotation(PutExchange.class);
            return put != null && MediaType.APPLICATION_FORM_URLENCODED.equals(put.contentType());
        }

        private String encodeFormData(MultiValueMap<?, ?> formData) {
            StringJoiner joiner = new StringJoiner("&");
            formData.forEach((key, values) -> {
                for (Object value : values) {
                    joiner.add(encode(String.valueOf(key)) + "=" + encode(String.valueOf(value)));
                }
            });
            return joiner.toString();
        }

        private String encodeMap(Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner("&");
            map.forEach((key, value) ->
                    joiner.add(encode(String.valueOf(key)) + "=" + encode(String.valueOf(value))));
            return joiner.toString();
        }

        private String encode(String value) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private boolean hasNoAnnotation(Parameter param) {
            return !param.isAnnotationPresent(PathVariable.class)
                    && !param.isAnnotationPresent(RequestParam.class)
                    && !param.isAnnotationPresent(RequestHeader.class)
                    && !param.isAnnotationPresent(RequestBody.class);
        }

        private Object executeAndReturn(ClientRequest request, Type returnType) {
            if (returnType == void.class || returnType == Void.class) {
                request.retrieve().toBodilessEntity();
                return null;
            }

            if (returnType instanceof ParameterizedType pt
                    && pt.getRawType() == ArkResponse.class) {
                Type bodyType = pt.getActualTypeArguments()[0];
                return request.retrieve().toEntity(TypeRef.of(bodyType));
            }

            return request.retrieve().body(TypeRef.of(returnType));
        }

        private record MethodInfo(String httpMethod, String path) {}
    }
}
