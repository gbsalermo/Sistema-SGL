package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.ProrrogacaoRequestDTO;
import com.sgl.dto.response.HistoricoProrrogacaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoAtividade;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoProjeto;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoSci;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoAtividadeRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoProjetoRepository;
import com.sgl.repository.prorrogacao.HistoricoProrrogacaoSciRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ProrrogacaoServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000502");
    private static final UUID SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000503");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000504");
    private static final UUID USUARIO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000505");

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private SciRepository sciRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HistoricoProrrogacaoProjetoRepository historicoProjetoRepository;

    @Mock
    private HistoricoProrrogacaoSciRepository historicoSciRepository;

    @Mock
    private HistoricoProrrogacaoAtividadeRepository historicoAtividadeRepository;

    @InjectMocks
    private ProrrogacaoService prorrogacaoService;

    private Projeto projeto;
    private Sci sci;
    private Atividade atividade;
    private Usuario usuario;

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
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .nome("Laboratório Teste")
                .ativo(true)
                .build();

        projeto = Projeto.builder()
                .id(20L)
                .publicId(PROJETO_ID)
                .laboratorio(laboratorio)
                .nome("Projeto Pai")
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
                .nome("Atividade")
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 6, 30))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();

        usuario = new Usuario(
                50L,
                USUARIO_ID,
                "Gestor Teste",
                "gestor@teste.local",
                "senha",
                Perfil.GESTOR,
                unidade,
                laboratorio,
                true
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private ProrrogacaoRequestDTO request(LocalDate novaDataFim) {
        return new ProrrogacaoRequestDTO(
                USUARIO_ID,
                novaDataFim,
                " Necessidade institucional "
        );
    }

    private void mockUsuario() {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void deveProrrogarProjetoEGerarHistorico() {
        TenantContext.definir(UNIDADE_ID);

        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        mockUsuario();
        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoProjetoRepository.save(any(HistoricoProrrogacaoProjeto.class)))
                .thenAnswer(invocation -> {
                    HistoricoProrrogacaoProjeto historico = invocation.getArgument(0);
                    historico.setPublicId(UUID.randomUUID());
                    return historico;
                });

        HistoricoProrrogacaoResponseDTO resposta =
                prorrogacaoService.prorrogarProjeto(
                        PROJETO_ID,
                        request(LocalDate.of(2027, 3, 31))
                );

        assertEquals(LocalDate.of(2027, 3, 31), projeto.getDataFim());
        assertEquals(LocalDate.of(2026, 12, 31), resposta.getDataFimAnterior());
        assertEquals(LocalDate.of(2027, 3, 31), resposta.getDataFimNova());
        assertEquals("Necessidade institucional", resposta.getJustificativa());
        assertEquals(USUARIO_ID, resposta.getUsuarioId());

        ArgumentCaptor<HistoricoProrrogacaoProjeto> captor =
                ArgumentCaptor.forClass(HistoricoProrrogacaoProjeto.class);
        verify(historicoProjetoRepository).save(captor.capture());
        assertEquals(projeto, captor.getValue().getProjeto());
        assertEquals(usuario, captor.getValue().getUsuario());
    }

    @Test
    void deveProrrogarSciDentroDoLimiteDoProjeto() {
        TenantContext.definir(UNIDADE_ID);

        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        mockUsuario();
        when(sciRepository.save(any(Sci.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoSciRepository.save(any(HistoricoProrrogacaoSci.class)))
                .thenAnswer(invocation -> {
                    HistoricoProrrogacaoSci historico = invocation.getArgument(0);
                    historico.setPublicId(UUID.randomUUID());
                    return historico;
                });

        prorrogacaoService.prorrogarSci(
                SCI_ID,
                request(LocalDate.of(2026, 11, 30))
        );

        assertEquals(LocalDate.of(2026, 11, 30), sci.getDataFim());
        verify(historicoSciRepository).save(any(HistoricoProrrogacaoSci.class));
    }

    @Test
    void deveRejeitarProrrogacaoDoSciAcimaDoProjeto() {
        TenantContext.definir(UNIDADE_ID);

        when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(SCI_ID, UNIDADE_ID))
                .thenReturn(Optional.of(sci));
        mockUsuario();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarSci(
                        SCI_ID,
                        request(LocalDate.of(2027, 1, 1))
                )
        );

        assertEquals(
                "A nova data de fim do SCI não pode ultrapassar a data de fim do projeto.",
                ex.getMessage()
        );
    }

    @Test
    void deveProrrogarAtividadeDentroDosLimitesHierarquicos() {
        TenantContext.definir(UNIDADE_ID);

        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
        mockUsuario();
        when(atividadeRepository.save(any(Atividade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoAtividadeRepository.save(any(HistoricoProrrogacaoAtividade.class)))
                .thenAnswer(invocation -> {
                    HistoricoProrrogacaoAtividade historico = invocation.getArgument(0);
                    historico.setPublicId(UUID.randomUUID());
                    return historico;
                });

        prorrogacaoService.prorrogarAtividade(
                ATIVIDADE_ID,
                request(LocalDate.of(2026, 9, 30))
        );

        assertEquals(LocalDate.of(2026, 9, 30), atividade.getDataFim());
        verify(historicoAtividadeRepository).save(any(HistoricoProrrogacaoAtividade.class));
    }

    @Test
    void deveRejeitarProrrogacaoDaAtividadeAcimaDoSci() {
        TenantContext.definir(UNIDADE_ID);

        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
        mockUsuario();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarAtividade(
                        ATIVIDADE_ID,
                        request(LocalDate.of(2026, 11, 1))
                )
        );

        assertEquals(
                "A nova data de fim da Atividade não pode ultrapassar a data de fim do SCI.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarProrrogacaoQuandoProjetoAncestralEstaConcluido() {
        projeto.setStatus(StatusProjeto.CONCLUIDO);

        TenantContext.definir(UNIDADE_ID);
        when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
        mockUsuario();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarAtividade(
                        ATIVIDADE_ID,
                        request(LocalDate.of(2026, 7, 31))
                )
        );

        assertEquals(
                "O projeto ancestral está encerrado e não pode ser prorrogado.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarUsuarioSemPerfilDeGestao() {
        usuario.setPerfil(Perfil.PESQUISADOR);

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        mockUsuario();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarProjeto(
                        PROJETO_ID,
                        request(LocalDate.of(2027, 1, 31))
                )
        );

        assertEquals(
                "A prorrogação exige perfil GESTOR ou ADMINISTRADOR.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarProrrogacaoSemTenant() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarProjeto(
                        PROJETO_ID,
                        request(LocalDate.of(2027, 1, 31))
                )
        );

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarProrrogacaoSemDataFimExistente() {
        projeto.setDataFim(null);

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        mockUsuario();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> prorrogacaoService.prorrogarProjeto(
                        PROJETO_ID,
                        request(LocalDate.of(2027, 1, 31))
                )
        );

        assertEquals(
                "O projeto ainda não possui data de fim definida. A primeira definição deve ser realizada pelo fluxo comum de atualização.",
                ex.getMessage()
        );
    }

    @Test
    void deveListarHistoricoDoProjetoNoTenantAtual() {
        HistoricoProrrogacaoProjeto historico = HistoricoProrrogacaoProjeto.builder()
                .publicId(UUID.randomUUID())
                .projeto(projeto)
                .usuario(usuario)
                .dataFimAnterior(LocalDate.of(2026, 10, 31))
                .dataFimNova(LocalDate.of(2026, 12, 31))
                .justificativa("Ajuste")
                .build();

        TenantContext.definir(UNIDADE_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(projeto));
        when(historicoProjetoRepository
                .findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdOrderByDataHoraAsc(
                        PROJETO_ID, UNIDADE_ID))
                .thenReturn(List.of(historico));

        List<HistoricoProrrogacaoResponseDTO> resultado =
                prorrogacaoService.listarHistoricoProjeto(PROJETO_ID);

        assertEquals(1, resultado.size());
        assertEquals(LocalDate.of(2026, 12, 31), resultado.get(0).getDataFimNova());
    }
}
