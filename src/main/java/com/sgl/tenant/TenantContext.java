package com.sgl.tenant;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    public static final String HEADER_UNIDADE = "X-SGL-Unidade-Id";

    private static final ThreadLocal<UUID> UNIDADE_ATUAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void definir(UUID unidadeId) {
        UNIDADE_ATUAL.set(unidadeId);
    }

    public static Optional<UUID> unidadeAtual() {
        return Optional.ofNullable(UNIDADE_ATUAL.get());
    }

    public static boolean ativo() {
        return UNIDADE_ATUAL.get() != null;
    }

    public static boolean pertence(UUID unidadeId) {
        UUID atual = UNIDADE_ATUAL.get();
        return atual == null || (unidadeId != null && atual.equals(unidadeId));
    }

    public static void limpar() {
        UNIDADE_ATUAL.remove();
    }
}
