package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.SciRequestDTO;
import com.sgl.dto.response.SciResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class SciServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID OUTRO_PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000104");

    @Mock
    private SciRepository sciRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Spy
    private CodigoSegValidator codigoSegValidator = new CodigoSegValidator();

    @InjectMocks
    private SciService sciService;

    private Projeto projeto;
    private Sci sci;

    @BeforeEach
    void setUp() {
        Unidade unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        Laboratorio laboratorio = Laboratorio.builder()
                .id(10L)
                .unidade(unidade)
                .nome("Laboratório Teste")
                .ativo(true)
                .build();

        projeto = Projeto.builder()
                .id(20L)
                .publicId(PROJETO_ID)
                .laboratorio(laboratorio)
                .nome("Projeto Pai")
                .codigoSeg("95.95.95.001.01.00")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();

        sci = Sci.builder()
                .id(30L)
                .publicId(SCI_ID)
                .projeto(projeto)
                .codigoSeg("95.95.95.001.01.01")
                .nome("SCI Teste")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 6, 30))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private SciRequestDTO requestValido() {
        SciRequestDTO dto = new SciRequestDTO();
        dto.setProjetoId(PROJETO_ID);
        dto.setCodigoSeg(" 95.95.95.001.01.01 ");
        dto.setNome(" SCI Teste ");
        dto.setResponsavel(" Pesquisador ");
        dto.setDataInicio(LocalDate.of(2026, 2, 1));
        dto.setDataFim(LocalDate.of(2026, 6, 30));
        return dto;
    }

    @Test
    void deveCriarSciComDefaultsENormalizacao() {
        SciRequestDTO dto = requestValido();

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        when(sciRepository.save(any(Sci.class)))
                .thenAnswer(invocation -> {
                    Sci salvo = invocation.getArgument(0);
                    salvo.setPublicId(SCI_ID);
                    return salvo;
                });

        SciResponseDTO resultado = sciService.criar(dto);

        ArgumentCaptor<Sci> captor = ArgumentCaptor.forClass(Sci.class);
        verify(sciRepository).save(captor.capture());

        Sci salvo = captor.getValue();
        assertEquals(SCI_ID, resultado.getId());
        assertEquals("95.95.95.001.01.01", salvo.getCodigoSeg());
        assertEquals("SCI Teste", salvo.getNome());
        assertEquals("Pesquisador", salvo.getResponsavel());
        assertEquals(StatusProjeto.ATIVO, salvo.getStatus());
        assertEquals(SituacaoExecucaoProjeto.NAO_INFORMADO, salvo.getSituacaoExecucao());
        assertTrue(salvo.getAtivo());
    }

    @Test
    void deveRejeitarCriacaoSemTenant() {
        SciRequestDTO dto = requestValido();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarProjetoSemDataInicio() {
        projeto.setDataInicio(null);
        SciRequestDTO dto = requestValido();

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "O projeto deve possuir data de início antes de receber um SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarSciIniciandoAntesDoProjeto() {
        SciRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2025, 12, 31));

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "A data de início do SCI não pode ser anterior à data de início do projeto.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarSciIniciandoDepoisDoFimDoProjeto() {
        SciRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2027, 1, 1));
        dto.setDataFim(null);

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "A data de início do SCI não pode ser posterior à data de fim do projeto.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarSciTerminandoDepoisDoProjeto() {
        SciRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2027, 1, 1));

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "A data de fim do SCI não pode ser posterior à data de fim do projeto.",
                ex.getMessage()
        );
    }

    @Test
    void deveImpedirTransferenciaDoSciParaOutroProjeto() {
        SciRequestDTO dto = requestValido();
        dto.setProjetoId(OUTRO_PROJETO_ID);

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.atualizar(SCI_ID, dto)
        );

        assertEquals(
                "O SCI não pode ser transferido para outro projeto.",
                ex.getMessage()
        );
    }

    @Test
    void deveAtualizarSciPreservandoCamposOpcionaisNaoInformados() {
        SciRequestDTO dto = requestValido();
        dto.setNome("SCI Atualizado");
        dto.setStatus(null);
        dto.setSituacaoExecucao(null);
        dto.setAtivo(null);

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        when(sciRepository.save(any(Sci.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        sciService.atualizar(SCI_ID, dto);

        assertEquals("SCI Atualizado", sci.getNome());
        assertEquals(StatusProjeto.ATIVO, sci.getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO, sci.getSituacaoExecucao());
        assertTrue(sci.getAtivo());
    }

    @Test
    void deveListarSciDoTenantAtual() {
        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByProjetoLaboratorioUnidadePublicId(UNIDADE_ID))
                .thenReturn(List.of(sci));

        List<SciResponseDTO> resultado = sciService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(SCI_ID, resultado.get(0).getId());
    }

    @Test
    void deveListarSciPorProjetoDoTenantAtual() {
        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        when(sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(List.of(sci));

        List<SciResponseDTO> resultado = sciService.listarPorProjeto(PROJETO_ID);

        assertEquals(1, resultado.size());
        assertEquals(PROJETO_ID, resultado.get(0).getProjetoId());
    }

    @Test
    void deveDesativarSciLogicamente() {
        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        sciService.deletar(SCI_ID);

        assertFalse(sci.getAtivo());
    }

    @Test
    void deveRejeitarAlteracaoDaDataInicioDoSci() {
        SciRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2026, 2, 2));

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.atualizar(SCI_ID, dto)
        );

        assertEquals(
                "A data de início do SCI não pode ser alterada após a criação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAmpliacaoDaDataFimDoSciNoPutComum() {
        SciRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2026, 7, 31));

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.atualizar(SCI_ID, dto)
        );

        assertEquals(
                "A ampliação da data final deve ser realizada pelo fluxo de prorrogação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarReducaoDoSciQueDeixaAtividadeForaDoPeriodo() {
        Atividade atividade = Atividade.builder()
                .publicId(UUID.randomUUID())
                .sci(sci)
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 6, 15))
                .build();

        SciRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2026, 5, 31));

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        when(atividadeRepository.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                SCI_ID, UNIDADE_ID))
                .thenReturn(List.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.atualizar(SCI_ID, dto)
        );

        assertEquals(
                "A nova data de fim do SCI deixaria uma Atividade fora do período do SCI.",
                ex.getMessage()
        );
    }


    @Test
    void deveRejeitarAlteracaoDoCodigoSegDoSciNoPutComum() {
        SciRequestDTO dto = requestValido();
        dto.setCodigoSeg("95.95.95.001.01.02");

        TenantContext.definir(UNIDADE_ID);

        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
                SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.atualizar(SCI_ID, dto)
        );

        assertEquals(
                "O Código SEG do SCI não pode ser alterado pelo fluxo comum de atualização. Use o fluxo administrativo de correção.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarCodigoSegGlobalmenteDuplicadoNaCriacaoDoSci() {
        SciRequestDTO dto = requestValido();

        TenantContext.definir(UNIDADE_ID);

        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));

        when(sciRepository.existsByCodigoSeg(
                "95.95.95.001.01.01"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> sciService.criar(dto)
        );

        assertEquals(
                "Já existe um SCI com este Código SEG.",
                ex.getMessage()
        );
    }

}
