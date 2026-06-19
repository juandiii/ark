package xyz.juandiii.ark.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * Type token that captures a generic Java type at runtime so the
 * {@link JsonSerializer} can deserialize into generic containers like
 * {@code List<User>} that would otherwise be erased.
 *
 * <p>Two construction styles:
 * <pre>{@code
 *   // Anonymous subclass — captures the type from the superclass parameter
 *   TypeRef<List<User>> users = new TypeRef<>() {};
 *
 *   // Factory methods — when you already have a Type or Class
 *   TypeRef<List<User>> users = TypeRef.ofList(User.class);
 * }</pre>
 *
 * @param <T> the captured Java type
 *
 * @author Juan Diego Lopez V.
 */
public abstract class TypeRef<T> {

    private final Type type;

    /**
     * Subclass constructor — reflects the type argument supplied at the anonymous-subclass site.
     */
    protected TypeRef() {
        Type superclass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    TypeRef(Type type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * @return the captured generic {@link Type}
     */
    public Type getType() {
        return type;
    }

    /**
     * Create a {@code TypeRef} from a {@link Type} obtained reflectively.
     *
     * @param type Java type
     * @param <T>  captured type
     * @return wrapping type ref
     */
    @SuppressWarnings("unchecked")
    public static <T> TypeRef<T> of(Type type) {
        return new TypeRef<>(type) {};
    }

    /**
     * Build a {@code TypeRef<List<T>>} from an existing element type ref.
     *
     * @param elementType element type ref
     * @param <T>         element type
     * @return list type ref
     */
    public static <T> TypeRef<List<T>> ofList(TypeRef<T> elementType) {
        return of(listType(elementType.getType()));
    }

    /**
     * Build a {@code TypeRef<List<T>>} from an element class.
     *
     * @param elementType element class
     * @param <T>         element type
     * @return list type ref
     */
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
