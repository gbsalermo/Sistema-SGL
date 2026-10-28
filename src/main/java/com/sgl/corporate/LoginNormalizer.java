package com.sgl.corporate;

import java.util.regex.Pattern;

/** Canonicalizes what users type in the login field so the same person always maps to the same key. */
public final class LoginNormalizer {

    private static final Pattern CPF_FORMATTED = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");
    private static final Pattern MATRICULA = Pattern.compile("[mM]?(\\d+)");
    private static final int MATRICULA_DIGITS = 8;

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

    /**
     * The key Empregadoapi expects: the numeric matrícula, zero-padded to 8 digits as the login answer
     * returns it ("00344136"). The "m" prefix belongs only to the AD login.
     */
    public static String matriculaCorporativa(String input) {
        if (input == null) {
            return null;
        }
        var matricula = MATRICULA.matcher(input.trim());
        if (!matricula.matches()) {
            return input.trim();
        }
        String digits = matricula.group(1);
        return digits.length() >= MATRICULA_DIGITS ? digits : "0".repeat(MATRICULA_DIGITS - digits.length()) + digits;
    }

    public static boolean isCpf(String value) {
        return value != null && value.matches("\\d{11}");
    }
}
