package com.sgl.corporate;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of the corporate gateway payloads the SGL needs. Unknown fields are
 * ignored so gateway additions never break us.
 */
public final class CorporateDtos {

    private CorporateDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CorporateLoginUser(String login, String matricula, String nome, String email,
                                     String lotacao, String sublotacao, String cargo,
                                     Integer userAccountControl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Empregado(String matricula, String nome, String cpf, String email,
                            String cargoDescricao, Integer unidadeIdLotacao, Boolean ativo,
                            String subLotacaoNome) {
    }

    // Colaboradorapi keys are capitalized, hence the explicit property names.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Colaborador(@JsonProperty("ColaboradorId") Long colaboradorId,
                              @JsonProperty("CPF") String cpf,
                              @JsonProperty("Nome") String nome,
                              @JsonProperty("UnidadeLotacaoId") Integer unidadeLotacaoId,
                              @JsonProperty("NomeUnidadeLotacao") String nomeUnidadeLotacao,
                              @JsonProperty("TipoVinculo") String tipoVinculo,
                              @JsonProperty("Ativo") Boolean ativo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnidadeCorporativa(Integer unidadeId, String sigla, String nome) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page<T>(List<T> items, int pageIndex, int totalPages, boolean hasNextPage) {
    }

    /** Wrapper for the login response; the corporate token is deliberately not mapped. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record LoginResponse(CorporateLoginUser user) {
    }
}
