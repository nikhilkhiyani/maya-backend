package com.MAYA.studio;

import com.MAYA.studio.util.AuthIdentifierUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthIdentifierUtilTest {

    @Test
    void isEmail_detectsValidEmail() {
        assertTrue(AuthIdentifierUtil.isEmail("user@example.com"));
        assertFalse(AuthIdentifierUtil.isEmail("9876543210"));
    }

    @Test
    void normalizePhone_addsIndiaCountryCodeForTenDigits() {
        assertEquals("+919876543210", AuthIdentifierUtil.normalizePhone("9876543210"));
    }

    @Test
    void normalizeIdentifier_lowercasesEmail() {
        assertEquals("user@example.com", AuthIdentifierUtil.normalizeIdentifier(" User@Example.com "));
    }
}
