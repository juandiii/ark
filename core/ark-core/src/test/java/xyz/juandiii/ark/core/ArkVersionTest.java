package xyz.juandiii.ark.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArkVersionTest {

    @Test
    void nameIsArk() {
        assertEquals("Ark", ArkVersion.NAME);
    }

    @Test
    void versionIsResolved() {
        assertNotNull(ArkVersion.VERSION);
        assertNotEquals("unresolved", ArkVersion.VERSION);
    }
}