package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para cadastrar ou atualizar um projeto.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoRequestDTO {

    @Schema(description = "Identificador público UUID do laboratório ao qual o projeto pertence.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id do laboratório é obrigatorio")
    private UUID laboratorioId;

    @Schema(description = "Nome do projeto.", example = "Síntese de Novos Compostos", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @Schema(description = "Descrição do projeto.", example = "Desenvolvimento de novos compostos orgânicos para catálise.")
    private String descricao;

    @Schema(description = "Data de início do projeto.", example = "2026-07-12")
    private LocalDate dataInicio;

    @Schema(description = "Data de encerramento do projeto, quando houver.", example = "2026-12-20")
    private LocalDate dataFim;

    @Schema(description = "Nome do responsável pelo projeto.", example = "Maria Oliveira")
    private String responsavel;
    
    @Schema(description = "Código SEG institucional do projeto", example = "XX.XX.XX.XXX.XX.00")
    private String codigoSeg;
    
    @Schema(description = "Status do ciclo de vida do projeto.", example = "ATIVO")
    private StatusProjeto status;
    
    @Schema(description = "Situacao de execucao do projeto.", example = "NAO_INFORMADO")
    private SituacaoExecucaoProjeto situacaoExecucao;
    
    @Schema(description = "Indica se o projeto possui recurso externo.", example = "false")
    private Boolean possuiRecursoExterno;
    
    @Schema(description = "Empresa Responsável pelo recurso externo, quando aplicável.", example = "Empresa Parceira LTDA")
    private String empresaRecursoExterno;

    @Schema(description = "Indica se o projeto está ativo.", example = "true")
    private Boolean ativo;
}
