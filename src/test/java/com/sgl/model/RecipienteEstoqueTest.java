package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.EstadoRecipienteEstoque;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;

class RecipienteEstoqueTest {

    @Test
    void deveAceitarRecipienteAbertoCheioAposAjusteSemRestaurarLacre() {
        RecipienteEstoque recipiente =
                recipienteBase(
                        UnidadeMedida.ML,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(500),
                        EstadoRecipienteEstoque.ABERTO
                );
        recipiente.setDataAbertura(
                LocalDateTime.now().minusDays(1)
        );

        assertDoesNotThrow(
                recipiente::validarConsistencia
        );
    }

    @Test
    void deveRejeitarRecipienteFechadoComSaldoParcial() {
        RecipienteEstoque recipiente =
                recipienteBase(
                        UnidadeMedida.ML,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(300),
                        EstadoRecipienteEstoque.FECHADO
                );

        assertThrows(
                BusinessRuleException.class,
                recipiente::validarConsistencia
        );
    }

    @Test
    void deveRejeitarRecipienteEsgotadoComSaldoPositivo() {
        RecipienteEstoque recipiente =
                recipienteBase(
                        UnidadeMedida.ML,
                        BigDecimal.valueOf(500),
                        BigDecimal.ONE,
                        EstadoRecipienteEstoque.ESGOTADO
                );
        recipiente.setDataEsgotamento(
                LocalDateTime.now()
        );

        assertThrows(
                BusinessRuleException.class,
                recipiente::validarConsistencia
        );
    }

    @Test
    void deveExigirUnidadeCanonicaDoProduto() {
        RecipienteEstoque recipiente =
                recipienteBase(
                        UnidadeMedida.G,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(500),
                        EstadoRecipienteEstoque.FECHADO
                );

        assertThrows(
                BusinessRuleException.class,
                recipiente::validarConsistencia
        );
    }

    private RecipienteEstoque recipienteBase(
            UnidadeMedida unidadeRecipiente,
            BigDecimal capacidade,
            BigDecimal disponivel,
            EstadoRecipienteEstoque estado) {

        Produto produto = Produto.builder()
                .nome("Produto")
                .codigoReferencia("PRD-REC")
                .unidadeMedida(UnidadeMedida.ML)
                .risco(NivelRisco.NENHUM)
                .perecivel(false)
                .ativo(true)
                .build();

        EstoqueCentral estoque = EstoqueCentral.builder()
                .produto(produto)
                .quantidadeAtual(disponivel)
                .quantidadeMinima(BigDecimal.ZERO)
                .ativo(true)
                .build();

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno("LOT-REC-001", 1);
        lote.setNumeroLote("REC-001");
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setQuantidadeApresentacoes(1);
        lote.setConteudoPorApresentacao(capacidade);
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(capacidade);
        lote.setQuantidadeDisponivel(disponivel);
        lote.setAtivo(true);

        RecipienteEstoque recipiente = new RecipienteEstoque();
        recipiente.setLote(lote);
        recipiente.definirIdentificacao("LOT-REC-001-R001", 1);
        recipiente.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        recipiente.setCapacidadeInicial(capacidade);
        recipiente.setQuantidadeDisponivel(disponivel);
        recipiente.setUnidadeMedida(unidadeRecipiente);
        recipiente.setEstado(estado);
        return recipiente;
    }
}
