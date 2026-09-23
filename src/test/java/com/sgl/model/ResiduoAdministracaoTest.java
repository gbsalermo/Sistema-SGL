package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.StatusResiduo;

class ResiduoAdministracaoTest {

    @Test
    void deveCancelarResiduoNaoDespachado() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.EM_ANALISE)
                .build();

        residuo.cancelarAdministrativamente();

        assertEquals(StatusResiduo.CANCELADO, residuo.getStatus());
    }

    @Test
    void deveExigirRetornoAntesDeCancelarResiduoDespachado() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.DESPACHADO)
                .build();

        assertThrows(
                BusinessRuleException.class,
                residuo::cancelarAdministrativamente
        );
    }

    @Test
    void deveRetornarDespachadoParaArmazenadoELimparConfirmacaoDeDespacho() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.DESPACHADO)
                .dataDespacho(LocalDateTime.now())
                .destinoFinalConfirmado("Destino confirmado")
                .build();

        StatusResiduo novoStatus =
                residuo.retornarEtapaAdministrativamente();

        assertEquals(
                StatusResiduo.ARMAZENADO_TEMPORARIAMENTE,
                novoStatus
        );
        assertNull(residuo.getDataDespacho());
        assertNull(residuo.getDestinoFinalConfirmado());
    }

    @Test
    void deveRetornarArmazenadoParaLiberado() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE)
                .dataArmazenamentoTemporario(LocalDateTime.now())
                .build();

        residuo.retornarEtapaAdministrativamente();

        assertEquals(
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO,
                residuo.getStatus()
        );
        assertNull(residuo.getDataArmazenamentoTemporario());
    }

    @Test
    void deveRetornarLiberadoParaAnalise() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO)
                .dataLiberacao(LocalDateTime.now())
                .build();

        residuo.retornarEtapaAdministrativamente();

        assertEquals(StatusResiduo.EM_ANALISE, residuo.getStatus());
        assertNull(residuo.getDataLiberacao());
    }

    @Test
    void deveRetornarAnaliseParaInformadoELimparRecebimento() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.EM_ANALISE)
                .gestorRecebedorInicial(new Usuario())
                .dataRecebimento(LocalDateTime.now())
                .observacaoGestor("Recebido")
                .build();

        residuo.retornarEtapaAdministrativamente();

        assertEquals(StatusResiduo.INFORMADO, residuo.getStatus());
        assertNull(residuo.getGestorRecebedorInicial());
        assertNull(residuo.getDataRecebimento());
        assertNull(residuo.getObservacaoGestor());
    }

    @Test
    void deveRejeitarRetornoQuandoJaEstaInformado() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.INFORMADO)
                .build();

        assertThrows(
                BusinessRuleException.class,
                residuo::retornarEtapaAdministrativamente
        );
    }

    @Test
    void deveRejeitarRetornoDeResiduoCancelado() {
        Residuo residuo = Residuo.builder()
                .status(StatusResiduo.CANCELADO)
                .build();

        assertThrows(
                BusinessRuleException.class,
                residuo::retornarEtapaAdministrativamente
        );
    }
}
