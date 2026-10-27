package com.sgl.corporate;

/** Keeps CPFs out of logs and responses: only the check digits stay visible. */
public final class CpfMasker {

    private CpfMasker() {
    }

    public static String mask(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***";
        }
        return "***.***.***-" + digits.substring(9);
    }
}
