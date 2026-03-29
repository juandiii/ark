package xyz.juandiii.ark.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * Type token for capturing generic types at runtime.
 *
 * @author Juan Diego Lopez V.
 */
public abstract class TypeRef<T> {

    private final Type type;

    protected TypeRef() {
        Type superclass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    TypeRef(Type type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public Type getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <T> TypeRef<T> of(Type type) {
        return new TypeRef<>(type) {};
    }

    public static <T> TypeRef<List<T>> ofList(TypeRef<T> elementType) {
        return of(listType(elementType.getType()));
    }

    public static <T> TypeRef<List<T>> ofList(Class<T> elementType) {
        return of(listType(elementType));
    }

    private static Type listType(Type elementType) {
        return new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return new Type[]{elementType}; }
            @Override public Type getRawType() { return List.class; }
            @Override public Type getOwnerType() { return null; }
        };
    }
}
