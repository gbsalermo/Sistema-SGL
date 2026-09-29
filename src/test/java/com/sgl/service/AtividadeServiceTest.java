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

import com.sgl.dto.request.AtividadeRequestDTO;
import com.sgl.dto.response.AtividadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.SciRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class AtividadeServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000303");
    private static final UUID OUTRO_SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000304");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000305");

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private SciRepository sciRepository;

    @Spy
    private CodigoSegValidator codigoSegValidator = new CodigoSegValidator();

    @InjectMocks
    private AtividadeService atividadeService;

    private Projeto projeto;
    private Sci sci;
    private Atividade atividade;

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
                .nome("SCI Pai")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 10, 31))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();

        atividade = Atividade.builder()
                .id(40L)
                .publicId(ATIVIDADE_ID)
                .sci(sci)
                .codigoSeg("95.95.95.001.01.01.001")
                .nome("Atividade Teste")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 3, 1))
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

    private AtividadeRequestDTO requestValido() {
        AtividadeRequestDTO dto = new AtividadeRequestDTO();
        dto.setSciId(SCI_ID);
        dto.setCodigoSeg(" 95.95.95.001.01.01.001 ");
        dto.setNome(" Atividade Teste ");
        dto.setResponsavel(" Pesquisador ");
        dto.setDataInicio(LocalDate.of(2026, 3, 1));
        dto.setDataFim(LocalDate.of(2026, 6, 30));
        return dto;
    }

    @Test
    void deveCriarAtividadeComDefaultsENormalizacao() {
        AtividadeRequestDTO dto = requestValido();

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        when(atividadeRepository.save(any(Atividade.class)))
                .thenAnswer(invocation -> {
                    Atividade salva = invocation.getArgument(0);
                    salva.setPublicId(ATIVIDADE_ID);
                    return salva;
                });

        AtividadeResponseDTO resultado = atividadeService.criar(dto);

        ArgumentCaptor<Atividade> captor = ArgumentCaptor.forClass(Atividade.class);
        verify(atividadeRepository).save(captor.capture());

        Atividade salva = captor.getValue();
        assertEquals(ATIVIDADE_ID, resultado.getId());
        assertEquals(SCI_ID, resultado.getSciId());
        assertEquals(PROJETO_ID, resultado.getProjetoId());
        assertEquals("95.95.95.001.01.01.001", salva.getCodigoSeg());
        assertEquals("Atividade Teste", salva.getNome());
        assertEquals("Pesquisador", salva.getResponsavel());
        assertEquals(StatusProjeto.ATIVO, salva.getStatus());
        assertEquals(SituacaoExecucaoProjeto.NAO_INFORMADO, salva.getSituacaoExecucao());
        assertTrue(salva.getAtivo());
    }

    @Test
    void deveRejeitarCriacaoSemTenant() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.criar(requestValido())
        );

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAtividadeIniciandoAntesDoSci() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2026, 1, 31));

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.criar(dto)
        );

        assertEquals(
                "A data de início da Atividade não pode ser anterior à data de início do SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAtividadeIniciandoDepoisDoFimDoSci() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2026, 11, 1));
        dto.setDataFim(null);

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.criar(dto)
        );

        assertEquals(
                "A data de início da Atividade não pode ser posterior à data de fim do SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAtividadeTerminandoDepoisDoSci() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2026, 11, 1));

        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.criar(dto)
        );

        assertEquals(
                "A data de fim da Atividade não pode ser posterior à data de fim do SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveImpedirTransferenciaDaAtividadeParaOutroSci() {
        AtividadeRequestDTO dto = requestValido();
        dto.setSciId(OUTRO_SCI_ID);

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.atualizar(ATIVIDADE_ID, dto)
        );

        assertEquals(
                "A Atividade não pode ser transferida para outro SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAmpliacaoDaDataFimNoUpdateComum() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2026, 7, 31));

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.atualizar(ATIVIDADE_ID, dto)
        );

        assertEquals(
                "A ampliação da data final deve ser realizada pelo fluxo de prorrogação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarRemocaoDaDataFimNoUpdateComum() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataFim(null);

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.atualizar(ATIVIDADE_ID, dto)
        );

        assertEquals(
                "A data de fim existente não pode ser removida pelo fluxo comum de atualização.",
                ex.getMessage()
        );
    }

    @Test
    void devePermitirPrimeiraDefinicaoDeDataFim() {
        atividade.setDataFim(null);

        AtividadeRequestDTO dto = requestValido();
        dto.setDataFim(LocalDate.of(2026, 8, 31));

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtividadeResponseDTO resultado =
                atividadeService.atualizar(ATIVIDADE_ID, dto);

        assertEquals(LocalDate.of(2026, 8, 31), resultado.getDataFim());
    }

    @Test
    void deveAtualizarPreservandoCamposOpcionaisNaoInformados() {
        AtividadeRequestDTO dto = requestValido();
        dto.setNome("Atividade Atualizada");
        dto.setDataFim(LocalDate.of(2026, 5, 31));
        dto.setStatus(null);
        dto.setSituacaoExecucao(null);
        dto.setAtivo(null);

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        atividadeService.atualizar(ATIVIDADE_ID, dto);

        assertEquals("Atividade Atualizada", atividade.getNome());
        assertEquals(LocalDate.of(2026, 5, 31), atividade.getDataFim());
        assertEquals(StatusProjeto.ATIVO, atividade.getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO, atividade.getSituacaoExecucao());
        assertTrue(atividade.getAtivo());
    }

    @Test
    void deveListarAtividadesDoTenantAtual() {
        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findBySciProjetoLaboratorioUnidadePublicId(UNIDADE_ID))
                .thenReturn(List.of(atividade));

        List<AtividadeResponseDTO> resultado =
                atividadeService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(ATIVIDADE_ID, resultado.get(0).getId());
    }

    @Test
    void deveListarAtividadesPorSciDoTenantAtual() {
        TenantContext.definir(UNIDADE_ID);
        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        when(atividadeRepository.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                SCI_ID, UNIDADE_ID))
                .thenReturn(List.of(atividade));

        List<AtividadeResponseDTO> resultado =
                atividadeService.listarPorSci(SCI_ID);

        assertEquals(1, resultado.size());
        assertEquals(SCI_ID, resultado.get(0).getSciId());
    }

    @Test
    void deveDesativarAtividadeLogicamente() {
        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        atividadeService.deletar(ATIVIDADE_ID);

        assertFalse(atividade.getAtivo());
    }

    @Test
    void deveRejeitarAlteracaoDaDataInicioDaAtividade() {
        AtividadeRequestDTO dto = requestValido();
        dto.setDataInicio(LocalDate.of(2026, 3, 2));

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.atualizar(ATIVIDADE_ID, dto)
        );

        assertEquals(
                "A data de início da Atividade não pode ser alterada após a criação.",
                ex.getMessage()
        );
    }


    @Test
    void deveRejeitarAlteracaoDoCodigoSegDaAtividadeNoPutComum() {
        AtividadeRequestDTO dto = requestValido();
        dto.setCodigoSeg("95.95.95.001.01.01.002");

        TenantContext.definir(UNIDADE_ID);

        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.atualizar(ATIVIDADE_ID, dto)
        );

        assertEquals(
                "O Código SEG da Atividade não pode ser alterado pelo fluxo comum de atualização. Use o fluxo administrativo de correção.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarCodigoSegGlobalmenteDuplicadoNaCriacaoDaAtividade() {
        AtividadeRequestDTO dto = requestValido();

        TenantContext.definir(UNIDADE_ID);

        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
                SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));

        when(atividadeRepository.existsByCodigoSeg(
                "95.95.95.001.01.01.001"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> atividadeService.criar(dto)
        );

        assertEquals(
                "Já existe uma Atividade com este Código SEG.",
                ex.getMessage()
        );
    }

}
