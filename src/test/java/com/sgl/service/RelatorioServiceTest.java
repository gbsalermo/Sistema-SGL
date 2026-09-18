package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;

/**
 * Testes unitários de {@link RelatorioService}.
 *
 * Service "somente leitura": agrega {@link Estagiario} vindos do repositório,
 * aplica filtros em memória (ativo/laboratório/período) e devolve totais
 * calculados. Não há persistência nem mutação de estado — por isso não é
 * necessário mockar {@code save()} em nenhum cenário aqui.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    private Laboratorio laboratorio;
    private Estagiario ativoRecente;
    private Estagiario inativoAntigo;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        laboratorio = new Laboratorio();
        laboratorio.setId(10L);
        laboratorio.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorio.setNome("Laboratório de Química");
        laboratorio.setAtivo(true);
        laboratorio.setUnidade(unidade);

        // Nome "Bruno" propositalmente cadastrado ANTES de "Ana" no mock de
        // findAll(), para provar que o service reordena por nome
        // (case-insensitive), e não devolve na ordem do repositório.
        inativoAntigo = new Estagiario();
        inativoAntigo.setId(2L);
        inativoAntigo.setPublicId(UUID.randomUUID());
        inativoAntigo.setNome("Bruno Silva");
        inativoAntigo.setAtivo(false);
        inativoAntigo.setLaboratorio(null);
        inativoAntigo.setDataInicioEstagio(LocalDate.of(2024, 1, 1));
        inativoAntigo.setDataFimEstagio(LocalDate.of(2024, 6, 30));

        ativoRecente = new Estagiario();
        ativoRecente.setId(1L);
        ativoRecente.setPublicId(UUID.randomUUID());
        ativoRecente.setNome("Ana Costa");
        ativoRecente.setAtivo(true);
        ativoRecente.setLaboratorio(laboratorio);
        ativoRecente.setDataInicioEstagio(LocalDate.of(2026, 1, 1));
        ativoRecente.setDataFimEstagio(null);
    }

    @Test
    void deveGerarRelatorioDeEstagiariosComSucesso() {
        when(estagiarioRepository.findAll()).thenReturn(List.of(inativoAntigo, ativoRecente));

        RelatorioEstagiariosResponseDTO resultado = relatorioService.gerarRelatorioEstagiarios(
                null, null, null, null);

        assertEquals(2, resultado.getTotal());
        assertEquals(1, resultado.getAtivos());
        assertEquals(1, resultado.getInativos());
        assertEquals(2, resultado.getItens().size());
        // Reordenado alfabeticamente: "Ana Costa" antes de "Bruno Silva".
        assertEquals("Ana Costa", resultado.getItens().get(0).getNome());
        assertEquals("Bruno Silva", resultado.getItens().get(1).getNome());
    }

    @Test
    void deveFiltrarRelatorioPorLaboratorio() {
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.findAll()).thenReturn(List.of(inativoAntigo, ativoRecente));

        RelatorioEstagiariosResponseDTO resultado = relatorioService.gerarRelatorioEstagiarios(
                null, LABORATORIO_PUBLIC_ID, null, null);

        assertEquals(1, resultado.getTotal());
        assertEquals("Ana Costa", resultado.getItens().get(0).getNome());
    }

    @Test
    void deveLancarExcecaoQuandoLaboratorioDoFiltroNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(laboratorioRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> relatorioService.gerarRelatorioEstagiarios(null, idInexistente, null, null));

        assertEquals("Laboratório não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoRegraDeNegocioDaLinha90() {
        // Linha 90 de RelatorioService: (dataInicio == null) != (dataFim == null)
        // — só um dos dois filtros de período foi informado.
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioService.gerarRelatorioEstagiarios(
                        null, null, LocalDate.of(2026, 1, 1), null));

        assertEquals("Para filtrar por período, informe dataInicio e dataFim.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoRegraDeNegocioDaLinha96() {
        // Linha 96: dataInicio.isAfter(dataFim).
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioService.gerarRelatorioEstagiarios(
                        null, null, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 1)));

        assertEquals("A data inicial não pode ser posterior à data final.", ex.getMessage());
    }
}
