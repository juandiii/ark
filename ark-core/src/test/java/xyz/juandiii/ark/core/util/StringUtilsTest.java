package xyz.juandiii.ark.core.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Nested
    class IsEmpty {

        @Test
        void givenNull_thenReturnsTrue() {
            assertTrue(StringUtils.isEmpty(null));
        }

        @Test
        void givenEmptyString_thenReturnsTrue() {
            assertTrue(StringUtils.isEmpty(""));
        }

        @Test
        void givenNonEmptyString_thenReturnsFalse() {
            assertFalse(StringUtils.isEmpty("a"));
        }
    }

    @Nested
    class IsNotEmpty {

        @Test
        void givenNull_thenReturnsFalse() {
            assertFalse(StringUtils.isNotEmpty(null));
        }

        @Test
        void givenNonEmptyString_thenReturnsTrue() {
            assertTrue(StringUtils.isNotEmpty("a"));
        }
    }

    @Nested
    class IsBlank {

        @Test
        void givenNull_thenReturnsTrue() {
            assertTrue(StringUtils.isBlank(null));
        }

        @Test
        void givenSpaces_thenReturnsTrue() {
            assertTrue(StringUtils.isBlank("  "));
        }

        @Test
        void givenNonBlankString_thenReturnsFalse() {
            assertFalse(StringUtils.isBlank("a"));
        }
    }

    @Nested
    class IsNotBlank {

        @Test
        void givenNull_thenReturnsFalse() {
            assertFalse(StringUtils.isNotBlank(null));
        }

        @Test
        void givenNonBlankString_thenReturnsTrue() {
            assertTrue(StringUtils.isNotBlank("a"));
        }
    }

    @Nested
    class TrimToEmpty {

        @Test
        void givenNull_thenReturnsEmpty() {
            assertEquals("", StringUtils.trimToEmpty(null));
        }

        @Test
        void givenSpaces_thenReturnsEmpty() {
            assertEquals("", StringUtils.trimToEmpty("  "));
        }

        @Test
        void givenPaddedString_thenReturnsTrimmed() {
            assertEquals("a", StringUtils.trimToEmpty(" a "));
        }
    }

    @Nested
    class TrimToNull {

        @Test
        void givenNull_thenReturnsNull() {
            assertNull(StringUtils.trimToNull(null));
        }

        @Test
        void givenSpaces_thenReturnsNull() {
            assertNull(StringUtils.trimToNull("  "));
        }

        @Test
        void givenPaddedString_thenReturnsTrimmed() {
            assertEquals("a", StringUtils.trimToNull(" a "));
        }
    }

    @Nested
    class StripTrailingSlash {

        @Test
        void givenNull_thenReturnsNull() {
            assertNull(StringUtils.stripTrailingSlash(null));
        }

        @Test
        void givenNoTrailingSlash_thenReturnsUnchanged() {
            assertEquals("https://api.com", StringUtils.stripTrailingSlash("https://api.com"));
        }

        @Test
        void givenTrailingSlash_thenStripsIt() {
            assertEquals("https://api.com", StringUtils.stripTrailingSlash("https://api.com/"));
        }
    }

    @Nested
    class EnsureLeadingSlash {

        @Test
        void givenNull_thenReturnsSlash() {
            assertEquals("/", StringUtils.ensureLeadingSlash(null));
        }

        @Test
        void givenEmpty_thenReturnsSlash() {
            assertEquals("/", StringUtils.ensureLeadingSlash(""));
        }

        @Test
        void givenNoLeadingSlash_thenAddsSlash() {
            assertEquals("/users", StringUtils.ensureLeadingSlash("users"));
        }

        @Test
        void givenLeadingSlash_thenReturnsUnchanged() {
            assertEquals("/users", StringUtils.ensureLeadingSlash("/users"));
        }
    }
}