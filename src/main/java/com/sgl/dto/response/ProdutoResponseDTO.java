package com.sgl.dto.response;

import java.util.Set;
import java.util.UUID;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Representação de um produto do catálogo retornado pela API.")
@Getter
public class ProdutoResponseDTO {

    @Schema(description = "Identificador público UUID do produto.", example = "550e8400-e29b-41d4-a716-446655440004")
    private final UUID id;
    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard")
    private final String nome;
    @Schema(description = "Descrição do produto.", example = "Kit para extração de DNA vegetal.")
    private final String descricao;
    @Schema(description = "Código de referência do produto.", example = "DNA-PW-50")
    private final String codigoReferencia;
    @Schema(description = "Unidade de medida do produto.", example = "UNIDADE")
    private final UnidadeMedida unidadeMedida;
    @Schema(description = "Localização física do produto.", example = "Armário A - Prateleira 2")
    private final String localizacaoFisica;
    @Schema(description = "Nível de risco associado ao produto.", example = "BAIXO")
    private final NivelRisco risco;
    @Schema(description = "Tipo específico de risco, quando aplicável.", example = "QUIMICO")
    private final TipoRisco tipoRisco;
    @Schema(description = "Descrição complementar de risco.", example = "Evitar contato direto com pele e olhos.")
    private final String descricaoRisco;
    @Schema(description = "Indica se o produto é perecível.", example = "true")
    private final Boolean perecivel;
    @Schema(description = "Tipo de perecibilidade do produto, quando aplicável.", example = "VALIDADE")
    private final TipoPerecivel tipoPerecivel;
    @Schema(description = "Condições de armazenamento recomendadas.", example = "Manter entre 2°C e 8°C.")
    private final String condicoesArmazenamento;
    @Schema(description = "Forma de armazenamento apresentada ao usuário.", example = "kit com 50 reações")
    private final String unidadeArmazenamento;
    @Schema(description = "Indica se o produto está sujeito a controle/fiscalização externa.", example = "true")
    private final Boolean fiscalizado;
    @Schema(description = "Órgãos fiscalizadores associados ao produto.")
    private final Set<OrgaoFiscalizador> orgaosFiscalizadores;
    @Schema(description = "Observação complementar sobre fiscalização.")
    private final String observacaoFiscalizacao;
    @Schema(description = "Indica se o produto está ativo no catálogo.", example = "true")
    private final Boolean ativo;

    public ProdutoResponseDTO(Produto entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.codigoReferencia = entity.getCodigoReferencia();
        this.unidadeMedida = entity.getUnidadeMedida();
        this.localizacaoFisica = entity.getLocalizacaoFisica();
        this.risco = entity.getRisco();
        this.tipoRisco = entity.getTipoRisco();
        this.descricaoRisco = entity.getDescricaoRisco();
        this.perecivel = entity.getPerecivel();
        this.tipoPerecivel = entity.getTipoPerecivel();
        this.condicoesArmazenamento = entity.getCondicoesArmazenamento();
        this.unidadeArmazenamento = entity.getUnidadeArmazenamento();
        this.fiscalizado = Boolean.TRUE.equals(entity.getFiscalizado());
        this.orgaosFiscalizadores = Set.copyOf(entity.getOrgaosFiscalizadores());
        this.observacaoFiscalizacao = entity.getObservacaoFiscalizacao();
        this.ativo = entity.getAtivo();
    }
}
