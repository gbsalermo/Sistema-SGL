package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeCulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Testes unitários da fundação de Estagiários da Etapa 6.
 *
 * O usuário institucional continua sendo a identidade da pessoa; Estagiario
 * representa o papel no SGL e VinculoEstagio guarda cada ocorrência
 * institucional ao longo do tempo.
 */
@ExtendWith(MockitoExtension.class)
class EstagiarioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USUARIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ORIENTADOR_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ATIVIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Mock
    private VinculoEstagioAtividadeRepository vinculoEstagioAtividadeRepository;

    @Mock
    private VinculoEstagioAtividadeCulturaRepository vinculoEstagioAtividadeCulturaRepository;

    @Mock
    private VinculoEstagioAtividadeService vinculoEstagioAtividadeService;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @InjectMocks
    private EstagiarioService estagiarioService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario usuario;
    private Usuario orientador;
    private Estagiario estagiario;
    private VinculoEstagio vinculo;
    private VinculoEstagioAtividade participacao;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        laboratorio = new Laboratorio();
        laboratorio.setId(10L);
        laboratorio.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorio.setNome("Laboratório de Química Orgânica");
        laboratorio.setAtivo(true);
        laboratorio.setUnidade(unidade);

        usuario = new Usuario();
        usuario.setId(20L);
        usuario.setPublicId(USUARIO_PUBLIC_ID);
        usuario.setNome("João Pedro");
        usuario.setEmail("joao.pedro@embrapa.br");
        usuario.setSenha("hash");
        usuario.setPerfil(Perfil.ESTAGIARIO);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);

        orientador = new Usuario();
        orientador.setId(30L);
        orientador.setPublicId(ORIENTADOR_PUBLIC_ID);
        orientador.setNome("Dra. Ana Souza");
        orientador.setEmail("ana.souza@embrapa.br");
        orientador.setSenha("hash");
        orientador.setPerfil(Perfil.PESQUISADOR);
        orientador.setUnidade(unidade);
        orientador.setAtivo(true);

        estagiario = new Estagiario();
        estagiario.setId(20L);
        estagiario.setPublicId(USUARIO_PUBLIC_ID);
        estagiario.setNome("João Pedro");
        estagiario.setEmail("joao.pedro@embrapa.br");
        estagiario.setSenha("hash");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorio);
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        estagiario.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        estagiario.setSituacaoEstagio(SituacaoEstagio.EM_ANDAMENTO);
        estagiario.setOrientador(orientador);
        estagiario.setObservacao("Estágio vinculado ao projeto de síntese.");

        vinculo = new VinculoEstagio();
        vinculo.setId(100L);
        vinculo.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000006"));
        vinculo.setEstagiario(estagiario);
        vinculo.setOrientador(orientador);
        vinculo.setDataInicio(LocalDate.of(2026, 8, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2027, 1, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setObservacao("Estágio vinculado ao projeto de síntese.");

        participacao = new VinculoEstagioAtividade();
        participacao.setId(200L);
        participacao.setPublicId(UUID.fromString("00000000-0000-0000-0000-000000000008"));
        participacao.setVinculoEstagio(vinculo);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 8, 1));

        ReflectionTestUtils.setField(estagiarioService, "entityManager", entityManager);

        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.executeUpdate()).thenReturn(1);
        lenient().when(vinculoEstagioAtividadeRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        any(UUID.class), any(UUID.class)))
                .thenReturn(List.of(participacao));
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarEstagiarioEPrimeiroVinculoComDadosValidos() {
        EstagiarioRequestDTO dto = montarDtoValido();

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(estagiarioRepository.findById(usuario.getId())).thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        EstagiarioResponseDTO resultado = estagiarioService.criar(dto);

        ArgumentCaptor<VinculoEstagio> captor =
                ArgumentCaptor.forClass(VinculoEstagio.class);
        verify(vinculoEstagioRepository).save(captor.capture());

        VinculoEstagio salvo = captor.getValue();

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
        assertEquals(estagiario, salvo.getEstagiario());
        assertEquals(orientador, salvo.getOrientador());
        assertEquals(SituacaoEstagio.EM_ANDAMENTO, salvo.getSituacao());
        assertEquals(TipoBolsa.BOLSA_CNPQ, salvo.getTipoBolsa());
        assertEquals(LocalDate.of(2026, 8, 1), salvo.getDataInicio());
        assertEquals(LocalDate.of(2027, 1, 31), salvo.getDataFimPrevista());
        verify(usuarioRepository).save(any(Usuario.class));

        ArgumentCaptor<VinculoEstagioAtividadeRequestDTO> participacaoCaptor =
                ArgumentCaptor.forClass(VinculoEstagioAtividadeRequestDTO.class);
        verify(vinculoEstagioAtividadeService)
                .adicionar(any(), participacaoCaptor.capture());

        assertEquals(ATIVIDADE_PUBLIC_ID, participacaoCaptor.getValue().getAtividadeId());
        assertEquals(LocalDate.of(2026, 8, 1),
                participacaoCaptor.getValue().getDataInicioParticipacao());
    }

    @Test
    void deveRejeitarCriacaoQuandoUsuarioJaPossuiCadastroDeEstagiario() {
        EstagiarioRequestDTO dto = montarDtoValido();

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals(
                "Usuário já possui cadastro de estagiário. "
                        + "Novos períodos devem ser registrados como novo vínculo de estágio.",
                ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoUsuarioEstaInativo() {
        EstagiarioRequestDTO dto = montarDtoValido();
        usuario.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("O usuário está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarOrientadorInativo() {
        EstagiarioRequestDTO dto = montarDtoValido();
        orientador.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("O usuário está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarOrientadorComPerfilInvalido() {
        EstagiarioRequestDTO dto = montarDtoValido();
        orientador.setPerfil(Perfil.GESTOR);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals(
                "Orientador deve possuir perfil ANALISTA ou PESQUISADOR.",
                ex.getMessage());
    }

    @Test
    void deveAceitarOrientadorComPerfilAnalista() {
        EstagiarioRequestDTO dto = montarDtoValido();
        orientador.setPerfil(Perfil.ANALISTA);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(estagiarioRepository.findById(usuario.getId())).thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        EstagiarioResponseDTO resultado = estagiarioService.criar(dto);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveRejeitarOrientadorDeOutraUnidade() {
        EstagiarioRequestDTO dto = montarDtoValido();

        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(99L);
        outraUnidade.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);
        orientador.setUnidade(outraUnidade);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals(
                "Estagiário e orientador devem pertencer à mesma unidade.",
                ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataFimAnteriorADataInicio() {
        EstagiarioRequestDTO dto = montarDtoValido();
        dto.setDataFimEstagio(LocalDate.of(2026, 7, 1));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        mockarBuscaBaseCriacao();
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals(
                "Data de fim do estágio não pode ser menor que data de início.",
                ex.getMessage());
    }

    @Test
    void deveRejeitarLaboratorioDeOutroTenant() {
        EstagiarioRequestDTO dto = montarDtoValido();

        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(99L);
        outraUnidade.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);

        Laboratorio outroLaboratorio = new Laboratorio();
        outroLaboratorio.setId(40L);
        outroLaboratorio.setPublicId(LABORATORIO_PUBLIC_ID);
        outroLaboratorio.setUnidade(outraUnidade);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID))
                .thenReturn(Optional.of(outroLaboratorio));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                ORIENTADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(orientador));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals(
                "A operação não pode acessar dados de outra unidade.",
                ex.getMessage());
    }

    @Test
    void deveListarTodosComHistoricoDeVinculos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(estagiarioRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        List<EstagiarioResponseDTO> resultado =
                estagiarioService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(1, resultado.get(0).getVinculos().size());
        assertEquals(
                SituacaoEstagio.EM_ANDAMENTO,
                resultado.get(0).getVinculos().get(0).getSituacao());
    }

    @Test
    void deveBuscarEstagiarioPorIdComHistoricoDeVinculos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        EstagiarioResponseDTO resultado =
                estagiarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
        assertTrue(resultado.getAtivo());
        assertEquals(1, resultado.getVinculos().size());
    }

    @Test
    void deveLancarExcecaoQuandoEstagiarioNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> estagiarioService.buscarPorId(idInexistente));

        assertEquals(
                "Estagiário não encontrado com id: " + idInexistente,
                ex.getMessage());
    }

    @Test
    void deveListarSomenteEstagiariosComVinculoAtivo() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(estagiarioRepository
                .findEstagiariosComVinculoAtivo(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        List<EstagiarioResponseDTO> resultado =
                estagiarioService.listarAtivos();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
        assertEquals(1, resultado.get(0).getVinculos().size());
    }

    @Test
    void deveMarcarResponseComoInativaQuandoUsuarioEstaInativo() {
        usuario.setAtivo(false);
        estagiario.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));

        EstagiarioResponseDTO resultado =
                estagiarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(Boolean.FALSE, resultado.getUsuarioAtivo());
        assertEquals(Boolean.FALSE, resultado.getAtivo());
    }

    @Test
    void deveMarcarResponseComoInativoSemParticipacaoAberta() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo));
        when(vinculoEstagioAtividadeRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        vinculo.getPublicId(), UNIDADE_PUBLIC_ID))
                .thenReturn(List.of());

        EstagiarioResponseDTO resultado =
                estagiarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(Boolean.FALSE, resultado.getAtivo());
    }

    @Test
    void devePreservarMultiplosVinculosNoResponse() {
        VinculoEstagio encerrado = new VinculoEstagio();
        encerrado.setId(90L);
        encerrado.setPublicId(UUID.randomUUID());
        encerrado.setEstagiario(estagiario);
        encerrado.setOrientador(orientador);
        encerrado.setDataInicio(LocalDate.of(2025, 1, 1));
        encerrado.setDataFimPrevista(LocalDate.of(2025, 12, 31));
        encerrado.setDataFimEfetiva(LocalDate.of(2025, 12, 31));
        encerrado.setTipoBolsa(TipoBolsa.VOLUNTARIO);
        encerrado.setSituacao(SituacaoEstagio.FINALIZADO);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(vinculoEstagioRepository
                .findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
                        USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(vinculo, encerrado));

        EstagiarioResponseDTO resultado =
                estagiarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(2, resultado.getVinculos().size());
        assertEquals(
                SituacaoEstagio.EM_ANDAMENTO,
                resultado.getVinculos().get(0).getSituacao());
        assertEquals(
                SituacaoEstagio.FINALIZADO,
                resultado.getVinculos().get(1).getSituacao());
    }

    @Test
    void deveBloquearAtualizacaoDiretaDoEstagio() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.atualizar(
                        USUARIO_PUBLIC_ID,
                        new EstagiarioRequestDTO()));

        assertTrue(
                ex.getMessage().contains(
                        "gerenciamento de vínculos institucionais"));
    }

    @Test
    void deveBloquearExclusaoDiretaDoEstagiario() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.deletar(USUARIO_PUBLIC_ID));

        assertTrue(
                ex.getMessage().contains(
                        "histórico institucional"));
        assertTrue(estagiario.getAtivo());
    }

    @Test
    void deveBloquearEncerramentoLegadoSemDesativarUsuario() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.encerrarEstagio(
                        USUARIO_PUBLIC_ID));

        assertTrue(
                ex.getMessage().contains(
                        "encerramento do vínculo institucional"));
        assertTrue(estagiario.getAtivo());
    }

    @Test
    void deveExigirTenantParaListagem() {
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> estagiarioService.listarTodos());

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());
    }

    private EstagiarioRequestDTO montarDtoValido() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);
        dto.setObservacao("Estágio vinculado ao projeto de síntese.");
        dto.setOrientadorId(ORIENTADOR_PUBLIC_ID);
        dto.setAtividadeId(ATIVIDADE_PUBLIC_ID);
        return dto;
    }

    private void mockarBuscaBaseCriacao() {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(
                ORIENTADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(orientador));
    }
}
