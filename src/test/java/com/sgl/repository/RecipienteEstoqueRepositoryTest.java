package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.Produto;
import com.sgl.model.RecipienteEstoque;
import com.sgl.model.Unidade;
import com.sgl.model.enums.EstadoRecipienteEstoque;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantProvider;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class RecipienteEstoqueRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecipienteEstoqueRepository repository;

    @Test
    void deveBuscarSomenteRecipientesDisponiveisEmOrdemDeterministicaComBloqueio() {
        Lote lote = criarLote();

        RecipienteEstoque r2 =
                criarRecipiente(
                        lote,
                        2,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(200),
                        EstadoRecipienteEstoque.ABERTO
                );
        RecipienteEstoque r1 =
                criarRecipiente(
                        lote,
                        1,
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(500),
                        EstadoRecipienteEstoque.FECHADO
                );
        criarRecipiente(
                lote,
                3,
                BigDecimal.valueOf(500),
                BigDecimal.ZERO,
                EstadoRecipienteEstoque.ESGOTADO
        );

        entityManager.flush();
        entityManager.clear();

        List<RecipienteEstoque> resultado =
                repository.buscarDisponiveisPorLoteComBloqueio(
                        lote.getId()
                );

        assertEquals(2, resultado.size());
        assertEquals(r1.getId(), resultado.get(0).getId());
        assertEquals(r2.getId(), resultado.get(1).getId());
    }

    @Test
    void deveCalcularMaiorSequencialDoLote() {
        Lote lote = criarLote();

        criarRecipiente(
                lote,
                1,
                BigDecimal.TEN,
                BigDecimal.TEN,
                EstadoRecipienteEstoque.FECHADO
        );
        criarRecipiente(
                lote,
                4,
                BigDecimal.TEN,
                BigDecimal.TEN,
                EstadoRecipienteEstoque.FECHADO
        );

        entityManager.flush();

        assertEquals(
                4,
                repository.buscarMaiorSequencialPorLote(
                        lote.getId()
                )
        );
    }

    private Lote criarLote() {
        Unidade unidade = Unidade.builder()
                .nome("Unidade Teste")
                .sigla("UT-R")
                .build();
        entityManager.persist(unidade);

        Produto produto = Produto.builder()
                .nome("Produto Físico")
                .codigoReferencia("FIS-001")
                .unidadeMedida(UnidadeMedida.ML)
                .risco(NivelRisco.NENHUM)
                .perecivel(false)
                .ativo(true)
                .build();
        entityManager.persist(produto);

        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(BigDecimal.valueOf(700))
                .quantidadeMinima(BigDecimal.ZERO)
                .ativo(true)
                .build();
        entityManager.persist(estoque);

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno("LOT-FIS-001", 1);
        lote.setNumeroLote("FORN-FIS-001");
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setApresentacao("frasco de 500 mL");
        lote.setQuantidadeApresentacoes(2);
        lote.setConteudoPorApresentacao(BigDecimal.valueOf(500));
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(BigDecimal.valueOf(1000));
        lote.setQuantidadeDisponivel(BigDecimal.valueOf(700));
        lote.setDataEntrada(LocalDate.now().minusDays(10));
        lote.setAtivo(true);
        entityManager.persist(lote);

        return lote;
    }

    private RecipienteEstoque criarRecipiente(
            Lote lote,
            int sequencial,
            BigDecimal capacidade,
            BigDecimal disponivel,
            EstadoRecipienteEstoque estado) {

        RecipienteEstoque recipiente = new RecipienteEstoque();
        recipiente.setLote(lote);
        recipiente.definirIdentificacao(
                lote.getCodigoInterno()
                        + "-R"
                        + String.format("%03d", sequencial),
                sequencial
        );
        recipiente.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        recipiente.setCapacidadeInicial(capacidade);
        recipiente.setQuantidadeDisponivel(disponivel);
        recipiente.setUnidadeMedida(UnidadeMedida.ML);
        recipiente.setEstado(estado);

        if (estado == EstadoRecipienteEstoque.ABERTO) {
            recipiente.setDataAbertura(
                    LocalDateTime.now().minusDays(1)
            );
        }

        if (estado == EstadoRecipienteEstoque.ESGOTADO) {
            recipiente.setDataEsgotamento(
                    LocalDateTime.now().minusHours(2)
            );
        }

        entityManager.persist(recipiente);
        return recipiente;
    }
}
