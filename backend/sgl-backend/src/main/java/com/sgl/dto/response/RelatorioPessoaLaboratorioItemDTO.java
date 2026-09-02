package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoBolsa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioPessoaLaboratorioItemDTO {

    private UUID usuarioId;
    private String nome;
    private String email;
    private Perfil perfil;
    private Boolean ativo;
    private Boolean responsavelLaboratorio;
    private TipoBolsa tipoVinculoEstagio;
    private LocalDate dataInicioEstagio;
    private LocalDate dataFimEstagio;
}
