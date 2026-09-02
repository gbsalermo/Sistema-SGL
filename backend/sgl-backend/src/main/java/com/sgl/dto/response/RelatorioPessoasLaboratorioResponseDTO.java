package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sgl.model.enums.Perfil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioPessoasLaboratorioResponseDTO {

    private LocalDateTime geradoEm;
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID unidadeId;
    private String unidadeNome;
    private UUID responsavelId;
    private String responsavelNome;
    private String responsavelEmail;
    private Integer totalPessoas;
    private Integer ativos;
    private Integer inativos;
    private Map<Perfil, Long> porPerfil;
    private List<RelatorioPessoaLaboratorioItemDTO> pessoas;
}
