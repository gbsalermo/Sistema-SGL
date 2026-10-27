package com.sgl.corporate;

import java.util.regex.Pattern;

/** Canonicalizes what users type in the login field so the same person always maps to the same key. */
public final class LoginNormalizer {

    private static final Pattern CPF_FORMATTED = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");
    private static final Pattern MATRICULA = Pattern.compile("[mM]?(\\d+)");

    private LoginNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (CPF_FORMATTED.matcher(value).matches()) {
            return value.replaceAll("\\D", "");
        }
        var matricula = MATRICULA.matcher(value);
        if (matricula.matches()) {
            // Leading zeros are noise; "0" alone must not collapse to an empty number.
            String number = matricula.group(1).replaceFirst("^0+(?=\\d)", "");
            return "m" + number;
        }
        return value;
    }

    public static boolean isCpf(String value) {
        return value != null && value.matches("\\d{11}");
    }
}
