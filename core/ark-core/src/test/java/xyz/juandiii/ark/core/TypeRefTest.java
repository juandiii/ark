package xyz.juandiii.ark.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeRefTest {

    @Test
    void capturesSimpleType() {
        TypeRef<String> ref = new TypeRef<>() {};
        assertEquals(String.class, ref.getType());
    }

    @Test
    void capturesGenericType() {
        TypeRef<List<String>> ref = new TypeRef<>() {};
        assertInstanceOf(ParameterizedType.class, ref.getType());
        ParameterizedType pt = (ParameterizedType) ref.getType();
        assertEquals(List.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    void ofCreatesFromType() {
        TypeRef<String> ref = TypeRef.of(String.class);
        assertEquals(String.class, ref.getType());
    }

    @Test
    void ofRejectsNull() {
        assertThrows(NullPointerException.class, () -> TypeRef.of(null));
    }

    @Test
    void ofListWithClass_thenCreatesListType() {
        TypeRef<List<String>> ref = TypeRef.ofList(String.class);
        assertInstanceOf(ParameterizedType.class, ref.getType());
        ParameterizedType pt = (ParameterizedType) ref.getType();
        assertEquals(List.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
        assertNull(pt.getOwnerType());
    }

    @Test
    void ofListWithTypeRef_thenCreatesListType() {
        TypeRef<String> elementRef = TypeRef.of(String.class);
        TypeRef<List<String>> ref = TypeRef.ofList(elementRef);
        assertInstanceOf(ParameterizedType.class, ref.getType());
        ParameterizedType pt = (ParameterizedType) ref.getType();
        assertEquals(List.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }
}