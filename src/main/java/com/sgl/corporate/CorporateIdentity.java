package com.sgl.corporate;

/** Person data resolved from the corporate bases; cpf holds 11 digits, matricula is null for colaboradores. */
public record CorporateIdentity(TipoPessoaCorporativa tipo, String matricula, String cpf, String nome,
                                String email, String cargo, String sublotacao,
                                Integer unidadeCorporativaId, boolean ativo) {
}
