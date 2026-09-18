package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ResiduoRepository;

/**
 * Testes unitários de {@link RelatorioResiduosService}.
 *
 * O "risco efetivo" usado no filtro/contagem prioriza o risco CONFIRMADO
 * (pela análise do gestor) sobre o risco INFORMADO (pelo gerador do
 * resíduo) — ver {@code riscoEfetivo()} no fonte. Os dois resíduos do setUp
 * exploram justamente essa diferença.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioResiduosServiceTest {

    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ResiduoRepository residuoRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @InjectMocks
    private RelatorioResiduosService relatorioResiduosService;

    private Laboratorio laboratorio;
    private Residuo informadoAltoRisco;
    private Residuo despachadoRiscoBaixoConfirmado;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000099"));

        laboratorio = Laboratorio.builder()
                .id(10L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .unidade(unidade)
                .nome("Laboratório de Química")
                .ativo(true)
                .build();

        Usuario gerador = new Usuario();
        gerador.setId(20L);
        gerador.setPublicId(UUID.randomUUID());
        gerador.setNome("Gerador de Teste");

        // Risco informado ALTO, sem análise ainda: riscoEfetivo() = ALTO.
        informadoAltoRisco = Residuo.builder()
                .id(100L)
                .publicId(UUID.randomUUID())
                .laboratorio(laboratorio)
                .gerador(gerador)
                .descricao("Resíduo ácido")
                .estadoFisico(EstadoFisicoResiduo.LIQUIDO)
                .tratamentoRealizado(false)
                .quantidade(new BigDecimal("1.000"))
                .unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.ALTO)
                .nivelRiscoConfirmado(null)
                .status(StatusResiduo.INFORMADO)
                .dataInformacao(LocalDateTime.of(2026, 6, 10, 9, 0))
                .build();

        // Risco informado ALTO, mas já ANALISADO com risco confirmado BAIXO:
        // riscoEfetivo() precisa priorizar o confirmado.
        despachadoRiscoBaixoConfirmado = Residuo.builder()
                .id(101L)
                .publicId(UUID.randomUUID())
                .laboratorio(laboratorio)
                .gerador(gerador)
                .descricao("Resíduo neutralizado")
                .estadoFisico(EstadoFisicoResiduo.LIQUIDO)
                .tratamentoRealizado(true)
                .quantidade(new BigDecimal("2.000"))
                .unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.ALTO)
                .nivelRiscoConfirmado(NivelRisco.BAIXO)
                .status(StatusResiduo.DESPACHADO)
                .dataInformacao(LocalDateTime.of(2026, 1, 5, 14, 30))
                .build();
    }

    @Test
    void deveGerarRelatorioDeResiduosComSucesso() {
        when(residuoRepository.findAllByOrderByDataInformacaoDesc())
                .thenReturn(List.of(informadoAltoRisco, despachadoRiscoBaixoConfirmado));

        RelatorioResiduosResponseDTO resultado = relatorioResiduosService.gerar(
                null, null, null, null, null);

        assertEquals(2, resultado.getTotal());
        assertEquals(1, resultado.getInformados());
        assertEquals(1, resultado.getDespachados());
        assertEquals(0, resultado.getEmAnalise());
        assertEquals(0, resultado.getLiberados());
        assertEquals(0, resultado.getArmazenados());
        // Só o primeiro resíduo tem risco efetivo ALTO (o segundo teve o
        // risco rebaixado para BAIXO na análise).
        assertEquals(1, resultado.getAltoRisco());
        assertEquals(2, resultado.getItens().size());
    }

    @Test
    void deveFiltrarPorNivelDeRiscoEfetivo() {
        when(residuoRepository.findAllByOrderByDataInformacaoDesc())
                .thenReturn(List.of(informadoAltoRisco, despachadoRiscoBaixoConfirmado));

        RelatorioResiduosResponseDTO resultado = relatorioResiduosService.gerar(
                null, null, NivelRisco.BAIXO, null, null);

        assertEquals(1, resultado.getTotal());
        assertEquals(StatusResiduo.DESPACHADO, resultado.getItens().get(0).getStatus());
    }

    @Test
    void deveFiltrarPorLaboratorioValido() {
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(residuoRepository.findAllByOrderByDataInformacaoDesc())
                .thenReturn(List.of(informadoAltoRisco, despachadoRiscoBaixoConfirmado));

        RelatorioResiduosResponseDTO resultado = relatorioResiduosService.gerar(
                null, LABORATORIO_PUBLIC_ID, null, null, null);

        assertEquals(2, resultado.getTotal());
    }

    @Test
    void deveLancarExcecaoQuandoLaboratorioDoFiltroNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(laboratorioRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> relatorioResiduosService.gerar(null, idInexistente, null, null, null));

        assertEquals("Laboratório não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataInicialPosteriorADataFinal() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioResiduosService.gerar(
                        null, null, null,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)));

        // Nota: mensagem sem ponto final no código-fonte atual, diferente
        // das outras validações equivalentes de período neste mesmo batch.
        assertEquals("A data inicial não pode ser posterior à data final", ex.getMessage());
    }
}
