package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link UsuarioService}.
 *
 * Assim como em LaboratorioServiceTest, a maioria das operações passa por
 * checagem de tenant ({@code TenantContext}) antes de qualquer outra regra —
 * por isso cada teste "caminho feliz" precisa definir o tenant coincidindo
 * com a unidade do usuário/DTO envolvido, e o {@code @AfterEach} limpa o
 * ThreadLocal para não vazar estado entre testes.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USUARIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Unidade unidade;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        usuario = new Usuario();
        usuario.setId(10L);
        usuario.setPublicId(USUARIO_PUBLIC_ID);
        usuario.setNome("Maria Oliveira");
        usuario.setEmail("maria.oliveira@embrapa.br");
        usuario.setSenha("hash-antigo");
        usuario.setPerfil(Perfil.TECNICO);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarUsuarioComDadosValidos() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Maria Oliveira");
        dto.setEmail("maria.oliveira@embrapa.br");
        dto.setSenha("SenhaForte123!");
        dto.setPerfil(Perfil.TECNICO);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.existsByEmail("maria.oliveira@embrapa.br")).thenReturn(false);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(passwordEncoder.encode("SenhaForte123!")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioResponseDTO resultado = usuarioService.criar(dto);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
        // A senha em texto puro nunca deve ser persistida diretamente: o service
        // sempre passa pelo encoder antes de montar a entidade.
        verify(passwordEncoder).encode("SenhaForte123!");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveRejeitarCriacaoQuandoEmailJaCadastrado() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setEmail("maria.oliveira@embrapa.br");
        dto.setSenha("SenhaForte123!");

        // A checagem de tenant acontece antes da checagem de e-mail, então
        // precisa estar satisfeita para o teste realmente exercitar a regra de e-mail duplicado.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.existsByEmail("maria.oliveira@embrapa.br")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.criar(dto));

        assertEquals("Email já cadastrado: maria.oliveira@embrapa.br", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoSemSenha() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setEmail("novo.usuario@embrapa.br");
        dto.setSenha("   ");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.existsByEmail("novo.usuario@embrapa.br")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.criar(dto));

        assertEquals("Senha é obrigatória na criação do usuário.", ex.getMessage());
    }

    @Test
    void deveListarTodosOsUsuarios() {
        // listarTodos() é a única exceção deliberada à regra de tenant (usada
        // pelo "login de desenvolvimento" do frontend, que ainda não tem
        // header de unidade para descobrir a qual unidade o usuário pertence).
        // Aqui simulamos o tenant ativo, então o service filtra por unidade.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> resultado = usuarioService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveListarUsuariosPorLaboratorio() {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setId(20L);
        laboratorio.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorio.setUnidade(unidade);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(usuarioRepository.findByLaboratorioId(20L)).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> resultado = usuarioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarUsuarioPorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));

        UsuarioResponseDTO resultado = usuarioService.buscarPorId(USUARIO_PUBLIC_ID);

        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.buscarPorId(idInexistente));

        assertEquals("Usuário não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveAtualizarUsuario() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Maria Oliveira Silva");
        dto.setEmail("maria.oliveira@embrapa.br");
        dto.setPerfil(Perfil.TECNICO);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot("maria.oliveira@embrapa.br", usuario.getId())).thenReturn(false);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        usuarioService.atualizar(USUARIO_PUBLIC_ID, dto);

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveRejeitarAtualizacaoQuandoEmailJaPertenceAOutroUsuario() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setEmail("outro@embrapa.br");
        dto.setPerfil(Perfil.TECNICO);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot("outro@embrapa.br", usuario.getId())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.atualizar(USUARIO_PUBLIC_ID, dto));

        assertEquals("Já existe um usuário com este email.", ex.getMessage());
    }

    @Test
    void deveAlterarPerfilComSucesso() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Usuário criado com perfil TECNICO (não ESTAGIARIO), então
        // validarAlteracaoPerfil não precisa consultar EstagiarioRepository.
        UsuarioResponseDTO resultado = usuarioService.alterarPerfil(USUARIO_PUBLIC_ID, Perfil.ANALISTA);

        assertEquals(Perfil.ANALISTA, usuario.getPerfil());
        assertEquals(USUARIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveRejeitarAlteracaoDePerfilNulo() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.alterarPerfil(USUARIO_PUBLIC_ID, null));

        assertEquals("Perfil é obrigatório.", ex.getMessage());
    }

    @Test
    void deveInativarUsuario() {
        // Atenção: o método se chama "Inativar" (I maiúsculo) no código-fonte,
        // não "inativar" — mantido como está no service, sem corrigir a nomenclatura
        // (fora do escopo deste batch, que é só de testes).
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));

        usuarioService.Inativar(USUARIO_PUBLIC_ID);

        assertFalse(usuario.getAtivo());
    }

    @Test
    void deveRejeitarInativacaoQuandoJaInativo() {
        usuario.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.Inativar(USUARIO_PUBLIC_ID));

        assertEquals("O usuário já está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarOperacaoQuandoUnidadeDeOutroTenant() {
        UsuarioRequestDTO dto = new UsuarioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setEmail("maria.oliveira@embrapa.br");
        dto.setPerfil(Perfil.TECNICO);

        // Tenant ativo é outra unidade: buscarUsuarioNoTenant() ainda encontra o
        // usuário (a busca é filtrada pelo tenant atual, que aqui devolvemos
        // via mock), mas validarTenantUnidade(dto.getUnidadeId()) barra a
        // operação porque a unidade do DTO não é a unidade do tenant ativo.
        TenantContext.definir(OUTRA_UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO_PUBLIC_ID, OUTRA_UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> usuarioService.atualizar(USUARIO_PUBLIC_ID, dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
    }
}
