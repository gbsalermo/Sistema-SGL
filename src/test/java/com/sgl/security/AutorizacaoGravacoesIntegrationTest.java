package com.sgl.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

/**
 * Guards the default-deny rule for writes: the web only hides buttons, so the API itself must refuse
 * a write from a profile that has no screen for it. Runs the real filter chain with real JWTs.
 *
 * <p>Requests carry random ids and empty bodies on purpose: a request that passes security then
 * fails with 400/404 and changes nothing, so "not 403" proves only that security let it through.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class) // same context as the other security integration tests
class AutorizacaoGravacoesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final List<Usuario> usuariosCriados = new ArrayList<>();
    private Unidade unidade;

    @BeforeEach
    void criarUnidade() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        unidade = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Unidade " + sufixo)
                .sigla("A" + sufixo)
                .build());
    }

    @AfterEach
    void removerDadosCriados() {
        usuarioRepository.deleteAll(usuariosCriados);
        unidadeRepository.delete(unidade);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    /** One representative write per previously unprotected controller family, plus gestão-only actions. */
    static Stream<Arguments> gravacoesDeGestao() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/estoque-central"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/estoque-central/" + id()),
                Arguments.of(HttpMethod.PUT, "/api/v1/lotes/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/lotes/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/movimentacoes/estoques/" + id() + "/lotes"),
                Arguments.of(HttpMethod.POST, "/api/v1/movimentacoes/estoques/" + id() + "/descarte-vencimento"),
                Arguments.of(HttpMethod.POST, "/api/v1/movimentacoes/estoques/" + id() + "/ajustes"),
                Arguments.of(HttpMethod.PUT, "/api/v1/scis/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/scis/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/atividades"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/atividades/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/projetos/" + id() + "/prorrogacoes"),
                Arguments.of(HttpMethod.POST, "/api/v1/scis/" + id() + "/correcoes-codigo-seg"),
                Arguments.of(HttpMethod.POST, "/api/v1/cursos"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/cursos/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/culturas"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/culturas/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/vinculos-estagio/estagiarios/" + id()),
                Arguments.of(HttpMethod.PUT, "/api/v1/vinculos-estagio/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/vinculos-estagio/" + id() + "/nova-bolsa"),
                Arguments.of(HttpMethod.POST, "/api/v1/vinculos-estagio/" + id() + "/sincronizacoes-institucionais"),
                Arguments.of(HttpMethod.POST, "/api/v1/vinculos-estagio/" + id() + "/atividades"),
                Arguments.of(HttpMethod.PUT, "/api/v1/vinculos-estagio/participacoes/" + id()),
                Arguments.of(HttpMethod.PUT, "/api/v1/vinculos-estagio/participacoes/" + id() + "/encerrar"),
                Arguments.of(HttpMethod.POST, "/api/v1/vinculos-estagio/" + id() + "/observacoes"),
                Arguments.of(HttpMethod.PUT, "/api/v1/pedidos/" + id() + "/aprovar"),
                Arguments.of(HttpMethod.PUT, "/api/v1/pedidos/" + id() + "/cancelar"),
                Arguments.of(HttpMethod.PUT, "/api/v1/solucoes/pedidos/" + id() + "/entregar"),
                Arguments.of(HttpMethod.PUT, "/api/v1/residuos/" + id() + "/receber"),
                Arguments.of(HttpMethod.PUT, "/api/v1/estagiarios/" + id() + "/encerrar"),
                Arguments.of(HttpMethod.PUT, "/api/v1/vinculos-operacionais/" + id() + "/encerrar"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/usuarios/" + id()),
                Arguments.of(HttpMethod.PUT, "/api/v1/usuarios/" + id() + "/perfil"),
                Arguments.of(HttpMethod.POST, "/api/v1/pessoas-corporativas/importar"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/estoque-central/" + id()));
    }

    /** Cadastros the web only offers on PERFIS_ADMIN routes, and the admin-only resíduo correction. */
    static Stream<Arguments> gravacoesDeAdministrador() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/laboratorios"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/laboratorios/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/produtos"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/produtos/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/projetos"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/projetos/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/classes-residuo/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/locais-armazenamento-residuo/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/modelos-residuo/" + id()),
                Arguments.of(HttpMethod.DELETE, "/api/v1/modelos-solucao/" + id()),
                Arguments.of(HttpMethod.POST, "/api/v1/modelos-solucao/promover-pedido/" + id()),
                Arguments.of(HttpMethod.PUT, "/api/v1/residuos/" + id() + "/administrar"));
    }

    /** The only writes a solicitante screen performs (NovoPedidoView, NovaSolucaoView, InformarResiduoView). */
    static Stream<Arguments> gravacoesDeSolicitante() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/pedidos"),
                Arguments.of(HttpMethod.POST, "/api/v1/solucoes/pedidos"),
                Arguments.of(HttpMethod.POST, "/api/v1/residuos"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("gravacoesDeGestao")
    void gravacaoDeGestao_estagiarioETecnico_recebem403(HttpMethod metodo, String caminho) throws Exception {
        assertStatus(metodo, caminho, Perfil.ESTAGIARIO).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(metodo, caminho, Perfil.TECNICO).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("gravacoesDeGestao")
    void gravacaoDeGestao_gestor_naoEhBarradoPelaSeguranca(HttpMethod metodo, String caminho) throws Exception {
        assertStatus(metodo, caminho, Perfil.GESTOR).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("gravacoesDeAdministrador")
    void gravacaoDeAdministrador_gestorEstagiarioETecnico_recebem403(HttpMethod metodo, String caminho)
            throws Exception {
        assertStatus(metodo, caminho, Perfil.ESTAGIARIO).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(metodo, caminho, Perfil.TECNICO).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(metodo, caminho, Perfil.GESTOR).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("gravacoesDeAdministrador")
    void gravacaoDeAdministrador_administrador_naoEhBarradoPelaSeguranca(HttpMethod metodo, String caminho)
            throws Exception {
        assertStatus(metodo, caminho, Perfil.ADMINISTRADOR).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("gravacoesDeSolicitante")
    void gravacaoDeSolicitante_estagiarioETecnico_naoSaoBarradosPelaSeguranca(HttpMethod metodo, String caminho)
            throws Exception {
        assertStatus(metodo, caminho, Perfil.ESTAGIARIO).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
        assertStatus(metodo, caminho, Perfil.TECNICO).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    void cadastroDeUnidade_somenteAdministradorGeral() throws Exception {
        // The {} body fails validation, so before the rule an ADMINISTRADOR saw 400 instead of 403.
        assertStatus(HttpMethod.POST, "/api/v1/unidades", Perfil.ADMINISTRADOR)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(HttpMethod.DELETE, "/api/v1/unidades/" + id(), Perfil.GESTOR)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(HttpMethod.POST, "/api/v1/unidades", Perfil.ADMINISTRADOR_GERAL)
                .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    void habilitarUnidade_somenteAdministradorGeral() throws Exception {
        assertStatus(HttpMethod.POST, "/api/v1/unidades/habilitar", Perfil.ADMINISTRADOR)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(HttpMethod.POST, "/api/v1/unidades/habilitar", Perfil.GESTOR)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(HttpMethod.POST, "/api/v1/unidades/habilitar", Perfil.ADMINISTRADOR_GERAL)
                .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    void projeto_cadastroEhDeAdministradorMasProrrogacaoEhDeGestao() throws Exception {
        // Pins the boundary between /projetos/* (admin) and /projetos/*/prorrogacoes (gestão).
        assertStatus(HttpMethod.PUT, "/api/v1/projetos/" + id(), Perfil.GESTOR)
                .isEqualTo(HttpStatus.FORBIDDEN.value());
        assertStatus(HttpMethod.POST, "/api/v1/projetos/" + id() + "/prorrogacoes", Perfil.GESTOR)
                .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @Test
    void gravacaoDeGestao_usuarioPendenteComPerfilDeGestao_recebe403() throws Exception {
        Usuario pendente = novoUsuario(Perfil.GESTOR, SituacaoUsuario.PENDENTE_VINCULACAO);

        int status = mockMvc.perform(request(HttpMethod.DELETE, "/api/v1/cursos/" + id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void leitura_estagiario_continuaPermitida() throws Exception {
        Usuario estagiario = novoUsuario(Perfil.ESTAGIARIO, SituacaoUsuario.ATIVO);

        mockMvc.perform(get("/api/v1/produtos").header(HttpHeaders.AUTHORIZATION, bearer(estagiario)))
                .andExpect(status().isOk());
    }

    private org.assertj.core.api.AbstractIntegerAssert<?> assertStatus(HttpMethod metodo, String caminho, Perfil perfil)
            throws Exception {
        Usuario usuario = novoUsuario(perfil, SituacaoUsuario.ATIVO);
        int status = mockMvc.perform(request(metodo, caminho)
                        .header(HttpHeaders.AUTHORIZATION, bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        return assertThat(status).as("%s %s como %s", metodo, caminho, perfil);
    }

    private Usuario novoUsuario(Perfil perfil, SituacaoUsuario situacao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(situacao);
        Usuario salvo = usuarioRepository.saveAndFlush(usuario);
        usuariosCriados.add(salvo);
        return salvo;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
