package com.sgl.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Estagiario;
import com.sgl.model.enums.TipoBolsa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Relatório consolidado de estagiários.")
@Getter
@AllArgsConstructor
public class RelatorioEstagiariosResponseDTO {

    private LocalDateTime geradoEm;
    private Integer total;
    private Integer ativos;
    private Integer inativos;
    private List<Item> itens;

    @Getter
    public static class Item {
        private final UUID id;
        private final String nome;
        private final String email;
        private final UUID laboratorioId;
        private final String laboratorioNome;
        private final String unidadeNome;
        private final LocalDate dataInicioEstagio;
        private final LocalDate dataFimEstagio;
        private final TipoBolsa tipoBolsa;
        private final Boolean ativo;
        private final String observacao;

        public Item(Estagiario entity) {
            this.id = entity.getPublicId();
            this.nome = entity.getNome();
            this.email = entity.getEmail();
            this.laboratorioId = entity.getLaboratorio() != null
                    ? entity.getLaboratorio().getPublicId()
                    : null;
            this.laboratorioNome = entity.getLaboratorio() != null
                    ? entity.getLaboratorio().getNome()
                    : null;
            this.unidadeNome = entity.getUnidade() != null
                    ? entity.getUnidade().getNome()
                    : null;
            this.dataInicioEstagio = entity.getDataInicioEstagio();
            this.dataFimEstagio = entity.getDataFimEstagio();
            this.tipoBolsa = entity.getTipoBolsa();
            this.ativo = entity.getAtivo();
            this.observacao = entity.getObservacao();
        }
    }
}
