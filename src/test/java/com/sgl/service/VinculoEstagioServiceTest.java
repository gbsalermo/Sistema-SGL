package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.EstagiarioRepository;
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
}
