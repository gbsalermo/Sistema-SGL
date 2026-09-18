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

    /**
     * Verifica se {@code unidadeId} é a mesma unidade do tenant atual.
     *
     * IMPORTANTE (correção de segurança): antes, quando nenhum tenant estava
     * definido (nenhum header X-SGL-Unidade-Id chegou na requisição), este
     * método retornava {@code true} para qualquer unidade — ou seja, "sem
     * tenant" era tratado como "pode acessar tudo". Isso é uma falha grave:
     * como hoje não existe autenticação real, bastava o cliente NÃO enviar o
     * header para pular toda checagem de isolamento entre unidades.
     *
     * Agora o método falha fechado: sem tenant definido, a resposta é sempre
     * {@code false} (não pertence a ninguém), obrigando quem chama a tratar
     * a ausência de tenant como acesso negado, e não como acesso livre.
     */
    public static boolean pertence(UUID unidadeId) {
        UUID atual = UNIDADE_ATUAL.get();
        return atual != null && atual.equals(unidadeId);
    }

    public static void limpar() {
        UNIDADE_ATUAL.remove();
    }
}
