package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Testes unitários de {@link EstagiarioService}.
 *
 * Segue o mesmo padrão de {@code ProjetoServiceTest}/{@code UsuarioServiceTest}
 * quanto ao tratamento do {@code TenantContext} (ThreadLocal limpo no
 * {@code @AfterEach}). Uma particularidade deste service: {@code criar()} usa
 * uma query nativa via {@code EntityManager} para inserir na tabela filha
 * "estagiarios" (herança JOINED de {@code Usuario}), então o
 * {@code EntityManager} e o {@code jakarta.persistence.Query} retornado por
 * {@code createNativeQuery(...)} também precisam ser mockados nesses testes.
 */
@ExtendWith(MockitoExtension.class)
class EstagiarioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USUARIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @InjectMocks
    private EstagiarioService estagiarioService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario usuario;
    private Estagiario estagiario;

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
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        estagiario.setObservacao("Estágio vinculado ao projeto de síntese.");

        // @InjectMocks não injeta o EntityManager automaticamente aqui: o
        // service usa @PersistenceContext (campo não-final, fora do
        // construtor do @RequiredArgsConstructor) e o Mockito, ao conseguir
        // resolver o construtor com os repositórios, não tenta mais a
        // injeção por campo. Fazemos essa injeção manualmente.
        ReflectionTestUtils.setField(estagiarioService, "entityManager", entityManager);

        // Stubs "de infraestrutura" para o INSERT nativo usado em criar():
        // usados só nos testes que chegam até lá, mas o lenient() evita
        // UnnecessaryStubbingException nos demais.
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
    }

    @AfterEach
    void tearDown() {
        // Evita que o tenant definido em um teste vaze para o próximo (ThreadLocal).
        TenantContext.limpar();
    }

    @Test
    void deveCriarEstagiarioComDadosValidos() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setObservacao("Estágio vinculado ao projeto de síntese.");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(estagiarioRepository.findById(usuario.getId())).thenReturn(Optional.of(estagiario));

        EstagiarioResponseDTO resultado = estagiarioService.criar(dto);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
        // O vínculo de laboratório do usuário precisa ser persistido junto
        // (usuário e estagiário compartilham a tabela "usuarios" na herança JOINED).
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveRejeitarCriacaoQuandoUsuarioJaPossuiCadastro() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("Usuário já possui cadastro de estagiário.", ex.getMessage());
    }

    @Test
    void deveListarTodosOsEstagiarios() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(estagiario));

        List<EstagiarioResponseDTO> resultado = estagiarioService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarEstagiarioPorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        EstagiarioResponseDTO resultado = estagiarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoEstagiarioNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> estagiarioService.buscarPorId(idInexistente));

        assertEquals("Estagiário não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveListarEstagiariosPorLaboratorio() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.findByLaboratorioId(laboratorio.getId())).thenReturn(List.of(estagiario));

        List<EstagiarioResponseDTO> resultado = estagiarioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveListarEstagiariosAtivos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByUnidadePublicIdAndAtivoTrue(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(estagiario));

        List<EstagiarioResponseDTO> resultado = estagiarioService.listarAtivos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveAtualizarEstagiario() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setObservacao("Observação atualizada");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.save(any(Estagiario.class))).thenReturn(estagiario);

        estagiarioService.atualizar(USUARIO_PUBLIC_ID, dto);

        verify(estagiarioRepository).save(any(Estagiario.class));
    }

    @Test
    void deveRejeitarAtualizacaoInvalida() {
        // A regra proíbe trocar o usuário vinculado ao estagiário: o id da
        // URL (dono do registro) precisa ser igual ao usuarioId do corpo.
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(UUID.randomUUID());
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.atualizar(USUARIO_PUBLIC_ID, dto));

        assertEquals("Não é permitido trocar o usuário vinculado do estagiário.", ex.getMessage());
    }

    @Test
    void deveDeletarEstagiario() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        estagiarioService.deletar(USUARIO_PUBLIC_ID);

        // deletar() só marca o fim do vínculo de estágio (dataFimEstagio);
        // não mexe em "ativo" — que é a mesma coluna usada para bloquear o
        // login do usuário (herança JOINED com Usuario). Ver comentário no
        // código-fonte sobre o bug de lógica corrigido nesse ponto.
        assertEquals(LocalDate.now(), estagiario.getDataFimEstagio());
        assertTrue(estagiario.getAtivo());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontradoAoDeletar() {
        // Nota sobre o nome deste teste (herdado do brief da task): no
        // código-fonte atual, deletar() busca via buscarEstagiarioNoTenant(),
        // que lança ResourceNotFoundException("Estagiário", id) — não
        // "Usuário". Mantido o nome original para rastreabilidade da task,
        // mas a asserção reflete a mensagem real do código-fonte.
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> estagiarioService.deletar(idInexistente));

        assertEquals("Estagiário não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarOperacaoQuandoUnidadeDeOutroTenant() {
        // Diferente de ProjetoService/LaboratorioService, buscarLaboratorio()
        // aqui NÃO filtra por tenant na consulta (usa findByPublicId puro) —
        // a proteção contra vazamento entre unidades depende inteiramente da
        // checagem validarTenantUnidade() logo em seguida. Este teste
        // pegaria uma regressão caso essa checagem fosse removida.
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setTipoBolsa(TipoBolsa.VOLUNTARIO);

        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(99L);
        outraUnidade.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);

        Laboratorio laboratorioDeOutraUnidade = new Laboratorio();
        laboratorioDeOutraUnidade.setId(30L);
        laboratorioDeOutraUnidade.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorioDeOutraUnidade.setUnidade(outraUnidade);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorioDeOutraUnidade));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoDataFimAnteriorADataInicio() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2026, 7, 1));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("Data de fim do estágio não pode ser menor que data de início.", ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoLaboratorioNaoPertenceAMesmaUnidadeDoEstagiario() {
        // validarTenantUnidade() já garante que usuário e laboratório
        // pertencem à MESMA unidade (publicId) do tenant ativo. Para
        // exercitar a checagem redundante de validarUnidadeCompativel()
        // (que compara o id primário/Long, não o publicId), simulamos duas
        // linhas de "unidade" com o mesmo publicId mas ids internos
        // diferentes. Isso não deveria acontecer com dados reais (publicId é
        // único por unidade), mas isola essa checagem específica do restante
        // da regra de tenant.
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);

        Unidade unidadeDoLaboratorio = new Unidade();
        unidadeDoLaboratorio.setId(77L);
        unidadeDoLaboratorio.setPublicId(UNIDADE_PUBLIC_ID);

        Laboratorio laboratorioComUnidadeDivergente = new Laboratorio();
        laboratorioComUnidadeDivergente.setId(30L);
        laboratorioComUnidadeDivergente.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorioComUnidadeDivergente.setUnidade(unidadeDoLaboratorio);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorioComUnidadeDivergente));
        when(estagiarioRepository.existsById(usuario.getId())).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.criar(dto));

        assertEquals("O estagiário e o laboratório devem pertencer à mesma unidade.", ex.getMessage());
    }

    @Test
    void deveEncerrarEstagioComSucesso() {
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.now().minusDays(10));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));
        when(estagiarioRepository.save(any(Estagiario.class))).thenReturn(estagiario);

        EstagiarioResponseDTO resultado = estagiarioService.encerrarEstagio(USUARIO_PUBLIC_ID);

        assertFalse(estagiario.getAtivo());
        assertEquals(LocalDate.now(), estagiario.getDataFimEstagio());
        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveRejeitarEncerramentoQuandoJaEncerrado() {
        estagiario.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.encerrarEstagio(USUARIO_PUBLIC_ID));

        assertEquals("O estágio já está encerrado.", ex.getMessage());
    }

    @Test
    void deveRejeitarEncerramentoAntesDaDataDeInicio() {
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.now().plusDays(5));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estagiarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estagiario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estagiarioService.encerrarEstagio(USUARIO_PUBLIC_ID));

        assertEquals("Não é possível encerrar um estágio antes da data de início.", ex.getMessage());
    }
}
