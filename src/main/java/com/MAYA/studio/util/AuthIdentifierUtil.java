package com.MAYA.studio.util;

import java.util.regex.Pattern;

public final class AuthIdentifierUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private AuthIdentifierUtil() {
    }

    public static boolean isEmail(String identifier) {
        return identifier != null && EMAIL_PATTERN.matcher(identifier.trim()).matches();
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        return digits;
    }

    public static String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String trimmed = identifier.trim();
        return isEmail(trimmed) ? trimmed.toLowerCase() : normalizePhone(trimmed);
    }
}
