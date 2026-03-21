package xyz.juandiii.ark;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeRef<T> {

    private final Type type;

    protected TypeRef() {
        Type superclass = getClass().getGenericSuperclass();
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
