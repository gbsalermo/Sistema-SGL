package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoRisco;

class ResiduoArmazenamentoTest {

    private Usuario gestor;
    private LocalArmazenamentoResiduo local;

    @BeforeEach
    void prepararCenario() {
        gestor = new Usuario();
        gestor.setId(1L);
        gestor.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        gestor.setNome("Gestor Teste");
        gestor.setAtivo(true);

        local = LocalArmazenamentoResiduo.builder()
                .id(10L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
                .nome("Almoxarifado Químico")
                .ativo(true)
                .build();
    }

    @Test
    void deveLiberarComLocalCadastradoEComplementoMantendoSnapshot() {
        Residuo residuo = residuoEmAnalise();

        residuo.liberarParaArmazenamento(
                gestor,
                NivelRisco.MEDIO,
                Set.of(TipoRisco.IRRITANTE),
                local,
                "Prateleira B2",
                null,
                "Tratamento externo",
                LocalDate.now().plusDays(1),
                null
        );

        assertSame(local, residuo.getLocalArmazenamentoResiduo());
        assertEquals("Prateleira B2", residuo.getComplementoLocalArmazenamento());
        assertEquals(
                "Almoxarifado Químico - Prateleira B2",
                residuo.getLocalArmazenamentoTemporario()
        );
        assertEquals(
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO,
                residuo.getStatus()
        );
    }

    @Test
    void deveLiberarComLocalManual() {
        Residuo residuo = residuoEmAnalise();

        residuo.liberarParaArmazenamento(
                gestor,
                NivelRisco.BAIXO,
                Set.of(),
                null,
                null,
                "  Área provisória externa  ",
                "Tratamento externo",
                null,
                null
        );

        assertNull(residuo.getLocalArmazenamentoResiduo());
        assertNull(residuo.getComplementoLocalArmazenamento());
        assertEquals(
                "Área provisória externa",
                residuo.getLocalArmazenamentoTemporario()
        );
    }

    @Test
    void deveRejeitarLocalCadastradoEManualAoMesmoTempo() {
        Residuo residuo = residuoEmAnalise();

        assertThrows(
                BusinessRuleException.class,
                () -> residuo.liberarParaArmazenamento(
                        gestor,
                        NivelRisco.MEDIO,
                        Set.of(),
                        local,
                        null,
                        "Outro local",
                        "Tratamento externo",
                        null,
                        null
                )
        );
    }

    @Test
    void deveRejeitarComplementoSemLocalCadastrado() {
        Residuo residuo = residuoEmAnalise();

        assertThrows(
                BusinessRuleException.class,
                () -> residuo.liberarParaArmazenamento(
                        gestor,
                        NivelRisco.MEDIO,
                        Set.of(),
                        null,
                        "Prateleira C3",
                        "Área manual",
                        "Tratamento externo",
                        null,
                        null
                )
        );
    }

    @Test
    void deveManterLocalPlanejadoQuandoConfirmacaoNaoCorrigeLocal() {
        Residuo residuo = residuoLiberado();

        residuo.confirmarArmazenamento(
                null,
                null,
                null
        );

        assertSame(local, residuo.getLocalArmazenamentoResiduo());
        assertEquals(
                "Prateleira B2",
                residuo.getComplementoLocalArmazenamento()
        );
        assertEquals(
                "Almoxarifado Químico - Prateleira B2",
                residuo.getLocalArmazenamentoTemporario()
        );
        assertEquals(
                StatusResiduo.ARMAZENADO_TEMPORARIAMENTE,
                residuo.getStatus()
        );
    }

    @Test
    void deveCorrigirConfirmacaoParaLocalManualELimparReferenciaEstruturada() {
        Residuo residuo = residuoLiberado();

        residuo.confirmarArmazenamento(
                null,
                null,
                "Área provisória externa"
        );

        assertNull(residuo.getLocalArmazenamentoResiduo());
        assertNull(residuo.getComplementoLocalArmazenamento());
        assertEquals(
                "Área provisória externa",
                residuo.getLocalArmazenamentoTemporario()
        );
    }

    @Test
    void deveRejeitarNovoUsoDeLocalInativo() {
        Residuo residuo = residuoLiberado();

        local.setAtivo(false);

        assertThrows(
                BusinessRuleException.class,
                () -> residuo.confirmarArmazenamento(
                        local,
                        "Prateleira B2",
                        null
                )
        );
    }

    private Residuo residuoEmAnalise() {
        return Residuo.builder()
                .gestorRecebedorInicial(gestor)
                .status(StatusResiduo.EM_ANALISE)
                .riscosConfirmados(new LinkedHashSet<>())
                .build();
    }

    private Residuo residuoLiberado() {
        return Residuo.builder()
                .gestorRecebedorInicial(gestor)
                .status(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO)
                .localArmazenamentoResiduo(local)
                .complementoLocalArmazenamento("Prateleira B2")
                .localArmazenamentoTemporario(
                        "Almoxarifado Químico - Prateleira B2"
                )
                .riscosConfirmados(new LinkedHashSet<>())
                .build();
    }
}
