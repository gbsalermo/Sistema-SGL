package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.sgl.dto.request.AtualizarVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.request.ProrrogarBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovaBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoInstitucionalRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.VinculoEstagioAtividadeCultura;
import com.sgl.model.Cultura;
import com.sgl.model.Estagiario;
import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.HistoricoSincronizacaoVinculoEstagioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeCulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class VinculoEstagioServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ESTAGIARIO_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID ORIENTADOR_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID VINCULO_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000005");
    private static final UUID PARTICIPACAO_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000006");

    @Mock
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Mock
    private VinculoEstagioAtividadeRepository participacaoRepository;

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private VinculoEstagioAtividadeCulturaRepository participacaoCulturaRepository;

    @Mock
    private VinculoEstagioAtividadeService vinculoEstagioAtividadeService;

    @Mock
    private HistoricoSincronizacaoVinculoEstagioRepository historicoSincronizacaoRepository;

    @InjectMocks
    private VinculoEstagioService service;

    private Unidade unidade;
    private Estagiario estagiario;
    private Usuario orientador;
    private Atividade atividade;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        Laboratorio laboratorio = Laboratorio.builder()
                .id(2L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .nome("Laboratório Teste")
                .ativo(true)
                .build();

        Projeto projeto = Projeto.builder()
                .id(3L)
                .publicId(UUID.randomUUID())
                .laboratorio(laboratorio)
                .nome("Projeto Teste")
                .codigoSeg("PRJ-TESTE")
                .ativo(true)
                .build();

        Sci sci = Sci.builder()
                .id(4L)
                .publicId(UUID.randomUUID())
                .projeto(projeto)
                .codigoSeg("SCI-TESTE")
                .nome("SCI Teste")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .build();

        atividade = Atividade.builder()
                .id(5L)
                .publicId(ATIVIDADE_ID)
                .sci(sci)
                .codigoSeg("ATV-TESTE")
                .nome("Atividade Teste")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2027, 12, 31))
                .ativo(true)
                .build();

        estagiario = new Estagiario();
        estagiario.setId(6L);
        estagiario.setPublicId(ESTAGIARIO_ID);
        estagiario.setNome("Estagiário Teste");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setAtivo(true);

        orientador = new Usuario();
        orientador.setId(7L);
        orientador.setPublicId(ORIENTADOR_ID);
        orientador.setNome("Orientador Teste");
        orientador.setPerfil(Perfil.PESQUISADOR);
        orientador.setUnidade(unidade);
        orientador.setAtivo(true);

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private NovoVinculoEstagioRequestDTO montarDto() {
        NovoVinculoEstagioRequestDTO dto =
                new NovoVinculoEstagioRequestDTO();
        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicio(LocalDate.of(2026, 10, 1));
        dto.setDataFimPrevista(LocalDate.of(2027, 3, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);
        dto.setObservacao("  novo período  ");
        dto.setObservacaoParticipacao("  primeira atividade  ");
        return dto;
    }


    private NovoVinculoInstitucionalRequestDTO montarDtoInstitucional() {
        NovoVinculoInstitucionalRequestDTO dto =
                new NovoVinculoInstitucionalRequestDTO();

        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setDataInicio(LocalDate.of(2027, 4, 1));
        dto.setDataFimPrevista(LocalDate.of(2027, 12, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);
        dto.setOrigem(
                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL);
        dto.setReferenciaEvento("EVT-NOVA-BOLSA-001");
        dto.setReferenciaInstitucional("BOLSA-2027-001");
        dto.setObservacao("  nova bolsa institucional  ");

        return dto;
    }

    private void mockarBuscasInstitucionais() {
        lenient().when(estagiarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ESTAGIARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(estagiario));

        lenient().when(usuarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ORIENTADOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(orientador));
    }

    private void mockarBuscasBase() {
        lenient().when(estagiarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ESTAGIARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(estagiario));

        lenient().when(usuarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ORIENTADOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(orientador));

        lenient().when(atividadeRepository
                .findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
    }

    @Test
    void deveCriarNovoVinculoComPrimeiraAtividadeNaMesmaOperacao() {
        mockarBuscasBase();

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagio salvo = invocation.getArgument(0);
                    salvo.setId(8L);
                    salvo.setPublicId(VINCULO_ID);
                    return salvo;
                });

        VinculoEstagioAtividade participacao = new VinculoEstagioAtividade();
        participacao.setId(9L);
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(new VinculoEstagio());
        participacao.getVinculoEstagio().setPublicId(VINCULO_ID);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 10, 1));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        VinculoEstagioResponseDTO resultado =
                service.criar(ESTAGIARIO_ID, montarDto());

        assertEquals(VINCULO_ID, resultado.getId());
        assertEquals(SituacaoEstagio.EM_ANDAMENTO, resultado.getSituacao());
        assertEquals("novo período", resultado.getObservacao());
        assertEquals(1, resultado.getParticipacoesAtividade().size());
        assertEquals(ATIVIDADE_ID,
                resultado.getParticipacoesAtividade().get(0).getAtividadeId());
        assertEquals(LocalDate.of(2026, 10, 1),
                resultado.getParticipacoesAtividade().get(0)
                        .getDataInicioParticipacao());

        verify(vinculoEstagioRepository)
                .save(any(VinculoEstagio.class));
        verify(vinculoEstagioAtividadeService)
                .adicionar(any(), any());
    }

    @Test
    void deveBloquearNovoVinculoQuandoExisteOutroNaoFinalizado() {
        when(estagiarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ESTAGIARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(estagiario));

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, montarDto()));

        assertEquals(
                "O Estagiário já possui um vínculo de estágio em andamento.",
                ex.getMessage());
    }

    @Test
    void deveBloquearOrientadorComPerfilInvalido() {
        mockarBuscasBase();
        orientador.setPerfil(Perfil.GESTOR);

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, montarDto()));

        assertEquals(
                "Orientador deve possuir perfil ANALISTA ou PESQUISADOR.",
                ex.getMessage());
    }

    @Test
    void deveBloquearAtividadeInativa() {
        mockarBuscasBase();
        atividade.setAtivo(false);

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, montarDto()));

        assertEquals(
                "A Atividade informada está inativa.",
                ex.getMessage());
    }

    @Test
    void deveBloquearPeriodoDoVinculoInvalido() {
        mockarBuscasBase();

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        NovoVinculoEstagioRequestDTO dto = montarDto();
        dto.setDataFimPrevista(LocalDate.of(2026, 9, 30));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, dto));

        assertTrue(ex.getMessage().contains(
                "Data final prevista não pode ser anterior"));
    }

    @Test
    void deveBloquearEstagiarioInativo() {
        estagiario.setAtivo(false);

        when(estagiarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ESTAGIARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, montarDto()));

        assertEquals("O usuário está inativo.", ex.getMessage());
    }

    @Test
    void deveExigirTenantAtivo() {
        TenantContext.limpar();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, montarDto()));

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());
    }

    @Test
    void deveCriarNovoVinculoInstitucionalQuandoAnteriorJaFoiFinalizado() {
        mockarBuscasInstitucionais();

        when(historicoSincronizacaoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-NOVA-BOLSA-001"))
                .thenReturn(Optional.empty());

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagio salvo = invocation.getArgument(0);
                    salvo.setId(20L);
                    salvo.setPublicId(VINCULO_ID);
                    return salvo;
                });

        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VinculoEstagioResponseDTO resultado =
                service.criarInstitucional(
                        ESTAGIARIO_ID,
                        montarDtoInstitucional());

        assertEquals(VINCULO_ID, resultado.getId());
        assertEquals(SituacaoEstagio.EM_ANDAMENTO, resultado.getSituacao());
        assertEquals("BOLSA-2027-001",
                resultado.getReferenciaInstitucional());
        assertEquals("nova bolsa institucional",
                resultado.getObservacao());

        verify(vinculoEstagioRepository)
                .save(any(VinculoEstagio.class));
        verify(historicoSincronizacaoRepository)
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearNovoVinculoInstitucionalQuandoExisteOutroNaoFinalizado() {
        mockarBuscasInstitucionais();

        when(historicoSincronizacaoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-NOVA-BOLSA-001"))
                .thenReturn(Optional.empty());

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criarInstitucional(
                        ESTAGIARIO_ID,
                        montarDtoInstitucional()));

        assertEquals(
                "O Estagiário já possui um vínculo de estágio em andamento.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
        verify(historicoSincronizacaoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveCriarVinculoInstitucionalSemParticipacaoInicial() {
        mockarBuscasInstitucionais();

        when(historicoSincronizacaoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-NOVA-BOLSA-001"))
                .thenReturn(Optional.empty());

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagio salvo = invocation.getArgument(0);
                    salvo.setId(21L);
                    salvo.setPublicId(VINCULO_ID);
                    return salvo;
                });

        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VinculoEstagioResponseDTO resultado =
                service.criarInstitucional(
                        ESTAGIARIO_ID,
                        montarDtoInstitucional());

        assertTrue(resultado.getParticipacoesAtividade().isEmpty());

        verify(vinculoEstagioAtividadeService, never())
                .adicionar(any(), any());
        verify(participacaoRepository, never())
                .save(any(VinculoEstagioAtividade.class));
    }

    @Test
    void deveReaproveitarVinculoQuandoEventoInstitucionalForReenviado() {
        when(estagiarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ESTAGIARIO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(estagiario));

        VinculoEstagio vinculoExistente = new VinculoEstagio();
        vinculoExistente.setId(30L);
        vinculoExistente.setPublicId(VINCULO_ID);
        vinculoExistente.setEstagiario(estagiario);
        vinculoExistente.setOrientador(orientador);
        vinculoExistente.setDataInicio(LocalDate.of(2027, 4, 1));
        vinculoExistente.setDataFimPrevista(LocalDate.of(2027, 12, 31));
        vinculoExistente.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculoExistente.setFormacao(FormacaoEstagiario.GRADUACAO);
        vinculoExistente.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculoExistente.setTreinamentoSegurancaConcluido(false);
        vinculoExistente.setReferenciaInstitucional("BOLSA-2027-001");

        HistoricoSincronizacaoVinculoEstagio historico =
                HistoricoSincronizacaoVinculoEstagio.builder()
                        .publicId(UUID.randomUUID())
                        .vinculoEstagio(vinculoExistente)
                        .tipoEvento(
                                TipoEventoSincronizacaoVinculoEstagio.CRIACAO)
                        .origem(
                                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL)
                        .referenciaEvento("EVT-NOVA-BOLSA-001")
                        .situacaoNova(SituacaoEstagio.EM_ANDAMENTO)
                        .dataFimPrevistaNova(
                                LocalDate.of(2027, 12, 31))
                        .build();

        when(historicoSincronizacaoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-NOVA-BOLSA-001"))
                .thenReturn(Optional.of(historico));

        VinculoEstagioResponseDTO resultado =
                service.criarInstitucional(
                        ESTAGIARIO_ID,
                        montarDtoInstitucional());

        assertEquals(VINCULO_ID, resultado.getId());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
        verify(historicoSincronizacaoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
        verify(vinculoEstagioRepository, never())
                .existsByEstagiarioIdAndSituacaoNot(
                        any(),
                        any());
    }

    @Test
    void deveExigirDataFinalPrevistaAoCriarVinculo() {
        mockarBuscasBase();

        when(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO))
                .thenReturn(false);

        NovoVinculoEstagioRequestDTO dto = montarDto();
        dto.setDataFimPrevista(null);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(ESTAGIARIO_ID, dto));

        assertEquals("Data final prevista é obrigatória.", ex.getMessage());
    }

    @Test
    void devePermitirEdicaoLocalERegistrarProrrogacao() {
        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setId(40L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setOrientador(orientador);
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setFormacao(FormacaoEstagiario.GRADUACAO);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(usuarioRepository
                .findByPublicIdAndUnidadePublicId(
                        ORIENTADOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(orientador));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of());

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarVinculoEstagioRequestDTO dto =
                new AtualizarVinculoEstagioRequestDTO();
        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setDataInicio(LocalDate.of(2026, 2, 1));
        dto.setDataFimPrevista(LocalDate.of(2027, 3, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.MESTRADO);
        dto.setObservacao("ajuste local");

        VinculoEstagioResponseDTO resultado =
                service.atualizarLocal(VINCULO_ID, dto);

        assertEquals(SituacaoEstagio.PRORROGADO, resultado.getSituacao());
        assertEquals(TipoBolsa.BOLSA_CNPQ, resultado.getTipoBolsa());
        assertEquals(FormacaoEstagiario.MESTRADO, resultado.getFormacao());
        assertEquals(LocalDate.of(2027, 3, 31), resultado.getDataFimPrevista());
        assertEquals("ajuste local", resultado.getObservacao());

        verify(historicoSincronizacaoRepository)
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearTrocaDeBolsaNoEditarVinculo() {
        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setId(50L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setOrientador(orientador);
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setFormacao(FormacaoEstagiario.GRADUACAO);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        AtualizarVinculoEstagioRequestDTO dto =
                new AtualizarVinculoEstagioRequestDTO();
        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setDataInicio(LocalDate.of(2026, 2, 1));
        dto.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.atualizarLocal(VINCULO_ID, dto));

        assertEquals(
                "O tipo de bolsa não pode ser alterado em Editar vínculo. Use o fluxo de nova bolsa.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
    }

    @Test
    void deveProrrogarBolsaAtualERegistrarHistorico() {
        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setId(51L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setOrientador(orientador);
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setFormacao(FormacaoEstagiario.GRADUACAO);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));
        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of());

        ProrrogarBolsaVinculoEstagioRequestDTO dto =
                new ProrrogarBolsaVinculoEstagioRequestDTO();
        dto.setNovaDataFimPrevista(LocalDate.of(2027, 3, 31));

        VinculoEstagioResponseDTO resultado =
                service.prorrogarBolsaLocal(VINCULO_ID, dto);

        assertEquals(SituacaoEstagio.PRORROGADO, resultado.getSituacao());
        assertEquals(LocalDate.of(2027, 3, 31),
                resultado.getDataFimPrevista());

        ArgumentCaptor<HistoricoSincronizacaoVinculoEstagio> captor =
                ArgumentCaptor.forClass(HistoricoSincronizacaoVinculoEstagio.class);

        verify(historicoSincronizacaoRepository).save(captor.capture());

        assertEquals(TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO,
                captor.getValue().getTipoEvento());
        assertEquals(OrigemSincronizacaoVinculoEstagio.DEV,
                captor.getValue().getOrigem());
        assertEquals(LocalDate.of(2026, 12, 31),
                captor.getValue().getDataFimPrevistaAnterior());
        assertEquals(LocalDate.of(2027, 3, 31),
                captor.getValue().getDataFimPrevistaNova());
    }

    @Test
    void deveBloquearProrrogacaoSemAumentarDataFinal() {
        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setId(52L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        ProrrogarBolsaVinculoEstagioRequestDTO dto =
                new ProrrogarBolsaVinculoEstagioRequestDTO();
        dto.setNovaDataFimPrevista(LocalDate.of(2026, 12, 31));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.prorrogarBolsaLocal(VINCULO_ID, dto));

        assertEquals(
                "A nova data final prevista deve ser posterior ao término atual da bolsa.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
    }

    @Test
    void deveRegistrarNovaBolsaNaDataAtualMovendoParticipacaoDoPontoDeCorte() {
        LocalDate hoje = LocalDate.now();
        UUID novoVinculoId =
                UUID.fromString("20000000-0000-0000-0000-000000000050");

        VinculoEstagio atual = new VinculoEstagio();
        atual.setId(53L);
        atual.setPublicId(VINCULO_ID);
        atual.setEstagiario(estagiario);
        atual.setOrientador(orientador);
        atual.setDataInicio(hoje.minusMonths(6));
        atual.setDataFimPrevista(hoje.plusMonths(2));
        atual.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        atual.setFormacao(FormacaoEstagiario.GRADUACAO);
        atual.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        atual.setTreinamentoSegurancaConcluido(true);

        VinculoEstagioAtividade participacao = new VinculoEstagioAtividade();
        participacao.setId(54L);
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(atual);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(hoje);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atual));

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagio salvo = invocation.getArgument(0);
                    if (salvo.getPublicId() == null) {
                        salvo.setId(55L);
                        salvo.setPublicId(novoVinculoId);
                    }
                    return salvo;
                });

        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        novoVinculoId, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        when(participacaoRepository.save(any(VinculoEstagioAtividade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participacaoCulturaRepository
                .findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(List.of());

        NovaBolsaVinculoEstagioRequestDTO dto =
                new NovaBolsaVinculoEstagioRequestDTO();
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setDataInicio(hoje);
        dto.setDataFimPrevista(hoje.plusMonths(8));

        VinculoEstagioResponseDTO resultado =
                service.registrarNovaBolsaLocal(VINCULO_ID, dto);

        assertEquals(novoVinculoId, resultado.getId());
        assertEquals(TipoBolsa.BOLSA_CAPES, resultado.getTipoBolsa());
        assertEquals(SituacaoEstagio.FINALIZADO, atual.getSituacao());
        assertEquals(hoje.minusDays(1), atual.getDataFimEfetiva());
        assertEquals(novoVinculoId,
                participacao.getVinculoEstagio().getPublicId());
        assertEquals(1, resultado.getParticipacoesAtividade().size());

        verify(historicoSincronizacaoRepository, times(2))
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveDividirParticipacaoQueAtravesseTrocaDeBolsaEPreservarCultura() {
        LocalDate hoje = LocalDate.now();
        UUID novoVinculoId =
                UUID.fromString("20000000-0000-0000-0000-000000000060");
        UUID culturaId =
                UUID.fromString("20000000-0000-0000-0000-000000000061");

        VinculoEstagio atual = new VinculoEstagio();
        atual.setId(60L);
        atual.setPublicId(VINCULO_ID);
        atual.setEstagiario(estagiario);
        atual.setOrientador(orientador);
        atual.setDataInicio(hoje.minusMonths(8));
        atual.setDataFimPrevista(hoje.plusMonths(1));
        atual.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        atual.setFormacao(FormacaoEstagiario.GRADUACAO);
        atual.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        VinculoEstagioAtividade participacao = new VinculoEstagioAtividade();
        participacao.setId(61L);
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(atual);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(hoje.minusMonths(2));
        participacao.setObservacao("participação contínua");

        Cultura cultura = Cultura.builder()
                .id(62L)
                .publicId(culturaId)
                .unidade(unidade)
                .nome("Mandioca")
                .ativo(true)
                .build();

        VinculoEstagioAtividadeCultura associacao =
                new VinculoEstagioAtividadeCultura();
        associacao.setId(63L);
        associacao.setPublicId(UUID.randomUUID());
        associacao.setParticipacao(participacao);
        associacao.setCultura(cultura);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atual));

        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagio salvo = invocation.getArgument(0);
                    if (salvo.getPublicId() == null) {
                        salvo.setId(64L);
                        salvo.setPublicId(novoVinculoId);
                    }
                    return salvo;
                });

        when(historicoSincronizacaoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        novoVinculoId, UNIDADE_ID))
                .thenReturn(List.of());

        when(participacaoRepository.save(any(VinculoEstagioAtividade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participacaoCulturaRepository
                .findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(List.of(associacao));

        when(participacaoCulturaRepository
                .save(any(VinculoEstagioAtividadeCultura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NovaBolsaVinculoEstagioRequestDTO dto =
                new NovaBolsaVinculoEstagioRequestDTO();
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setDataInicio(hoje);
        dto.setDataFimPrevista(hoje.plusMonths(10));

        service.registrarNovaBolsaLocal(VINCULO_ID, dto);

        assertEquals(hoje.minusDays(1),
                participacao.getDataFimParticipacao());

        ArgumentCaptor<VinculoEstagioAtividade> participacaoCaptor =
                ArgumentCaptor.forClass(VinculoEstagioAtividade.class);

        verify(participacaoRepository, times(2))
                .save(participacaoCaptor.capture());

        VinculoEstagioAtividade continuacao =
                participacaoCaptor.getAllValues().stream()
                        .filter(item -> item != participacao)
                        .findFirst()
                        .orElseThrow();

        assertEquals(novoVinculoId,
                continuacao.getVinculoEstagio().getPublicId());
        assertEquals(hoje, continuacao.getDataInicioParticipacao());
        assertEquals("participação contínua",
                continuacao.getObservacao());

        ArgumentCaptor<VinculoEstagioAtividadeCultura> culturaCaptor =
                ArgumentCaptor.forClass(VinculoEstagioAtividadeCultura.class);

        verify(participacaoCulturaRepository)
                .save(culturaCaptor.capture());

        assertEquals(culturaId,
                culturaCaptor.getValue().getCultura().getPublicId());
        assertEquals(continuacao,
                culturaCaptor.getValue().getParticipacao());
    }

    @Test
    void deveBloquearNovaBolsaLocalComInicioFuturo() {
        LocalDate hoje = LocalDate.now();

        VinculoEstagio atual = new VinculoEstagio();
        atual.setId(70L);
        atual.setPublicId(VINCULO_ID);
        atual.setEstagiario(estagiario);
        atual.setDataInicio(hoje.minusMonths(3));
        atual.setDataFimPrevista(hoje.plusMonths(4));
        atual.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        atual.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atual));

        NovaBolsaVinculoEstagioRequestDTO dto =
                new NovaBolsaVinculoEstagioRequestDTO();
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setDataInicio(hoje.plusDays(1));
        dto.setDataFimPrevista(hoje.plusMonths(6));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.registrarNovaBolsaLocal(VINCULO_ID, dto));

        assertEquals(
                "A nova bolsa local só pode entrar em vigor hoje ou em uma data passada.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
    }

}
