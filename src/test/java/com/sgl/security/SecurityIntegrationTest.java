package com.sgl.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

/**
 * Exercises the real security chain (JWT filter, authorization rules and tenant filter) against
 * H2, with tokens minted by the real {@link JwtService}. Deliberately not @Transactional: the
 * filter must load the user and its Unidade outside any test-managed persistence context. The
 * context (and its H2) is shared with other test classes, so everything created here is deleted
 * afterwards; a leftover active ADMINISTRADOR_GERAL would skew tests that count them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class) // same context as the auth integration tests
class SecurityIntegrationTest {

    private static final String LABORATORIOS = "/api/v1/laboratorios";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CorporateGatewayStubs gateway;

    private final List<Unidade> unidadesCriadas = new ArrayList<>();
    private final List<Laboratorio> laboratoriosCriados = new ArrayList<>();
    private final List<Usuario> usuariosCriados = new ArrayList<>();

    @BeforeEach
    void resetGateway() {
        gateway.reset();
    }

    @AfterEach
    void removerDadosCriados() {
        usuarioRepository.deleteAll(usuariosCriados);
        laboratorioRepository.deleteAll(laboratoriosCriados);
        unidadeRepository.deleteAll(unidadesCriadas);
    }

    @Test
    void semToken_rotaProtegida_retorna401() throws Exception {
        mockMvc.perform(get(LABORATORIOS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenInvalido_rotaProtegida_retorna401() throws Exception {
        mockMvc.perform(get(LABORATORIOS).header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenValido_rotaProtegida_retorna200() throws Exception {
        Unidade unidade = novaUnidade();
        Laboratorio laboratorio = novoLaboratorio(unidade);
        Usuario usuario = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS).header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(laboratorio.getPublicId().toString()));
    }

    @Test
    void requisicao_comTokenDeUsuarioInativo_retorna401() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario usuario = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);
        String token = bearer(usuario);

        usuario.setAtivo(false);
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(get(LABORATORIOS).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void tokenDeUsuarioInexistente_retorna401() throws Exception {
        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generate(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void pendente_rotaOperacional_retorna403UsuarioPendente() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(get(LABORATORIOS).header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
    }

    @Test
    void pendente_authMe_naoEhBloqueadoPeloFiltro() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendente.getPublicId().toString()))
                .andExpect(jsonPath("$.situacao").value("PENDENTE_VINCULACAO"));
    }

    @Test
    void pendente_authMeComBarraFinal_retorna403UsuarioPendente() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        // Only the exact "who am I" path is open to a pendente; path variants must not slip through.
        mockMvc.perform(get("/api/v1/auth/me/").header(HttpHeaders.AUTHORIZATION, bearer(pendente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
    }

    @Test
    void pendente_authMeComHeaderDeOutraUnidade_naoEhBloqueadoPeloTenant() throws Exception {
        Unidade unidadeA = novaUnidade();
        Unidade unidadeB = novaUnidade();
        Usuario pendente = novoUsuario(unidadeA, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        // /me reports the user's own Unidade, never the one named in the header.
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pendente))
                        .header(TenantContext.HEADER_UNIDADE, unidadeB.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadeId").value(unidadeA.getPublicId().toString()));
    }

    @Test
    void authLogin_comTokenEHeaderDeOutraUnidade_naoEhBloqueadoPeloTenant() throws Exception {
        Unidade unidadeA = novaUnidade();
        Unidade unidadeB = novaUnidade();
        Usuario gestor = novoUsuario(unidadeA, Perfil.GESTOR, SituacaoUsuario.ATIVO);

        // E.g. a demoted ADMINISTRADOR_GERAL whose browser still sends the old token and Unidade.
        // The empty body reaches bean validation, i.e. neither security nor the tenant filter stopped it.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .header(TenantContext.HEADER_UNIDADE, unidadeB.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gestor_headerDeOutraUnidade_retorna403() throws Exception {
        Unidade unidadeA = novaUnidade();
        Unidade unidadeB = novaUnidade();
        Usuario gestor = novoUsuario(unidadeA, Perfil.GESTOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .header(TenantContext.HEADER_UNIDADE, unidadeB.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_NAO_PERMITIDA"));
    }

    @Test
    void administradorLocal_headerDeOutraUnidade_retorna403() throws Exception {
        Unidade unidadeA = novaUnidade();
        Unidade unidadeB = novaUnidade();
        Usuario administrador = novoUsuario(unidadeA, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(administrador))
                        .header(TenantContext.HEADER_UNIDADE, unidadeB.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_NAO_PERMITIDA"));
    }

    @Test
    void gestor_headerDaPropriaUnidade_retorna200() throws Exception {
        Unidade unidade = novaUnidade();
        novoLaboratorio(unidade);
        Usuario gestor = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .header(TenantContext.HEADER_UNIDADE, unidade.getPublicId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void administradorGeral_headerDeOutraUnidade_operaNaquelaUnidade() throws Exception {
        Unidade unidadeA = novaUnidade();
        Unidade unidadeB = novaUnidade();
        novoLaboratorio(unidadeA);
        Laboratorio laboratorioB = novoLaboratorio(unidadeB);
        Usuario adminGeral = novoUsuario(unidadeA, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminGeral))
                        .header(TenantContext.HEADER_UNIDADE, unidadeB.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(laboratorioB.getPublicId().toString()));
    }

    @Test
    void administradorGeral_headerDeUnidadeInexistente_retorna403() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario adminGeral = novoUsuario(unidade, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminGeral))
                        .header(TenantContext.HEADER_UNIDADE, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_NAO_PERMITIDA"));
    }

    @Test
    void headerMalformado_retorna403() throws Exception {
        Unidade unidade = novaUnidade();
        Usuario gestor = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(LABORATORIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .header(TenantContext.HEADER_UNIDADE, "nao-e-uuid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_NAO_PERMITIDA"));
    }

    @Test
    void authLogin_ehPublico() throws Exception {
        // A gateway outage is the one outcome only the login controller itself can produce
        // (a security rejection would be 401/403 NAO_AUTENTICADO), so it proves the request got through.
        gateway.loginStatus(HttpStatus.SERVICE_UNAVAILABLE, 1);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"m344136\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACAO_INDISPONIVEL"));
        gateway.verify();
    }

    @Test
    void authConfig_ehPublico() throws Exception {
        mockMvc.perform(get("/api/v1/auth/config")).andExpect(status().isOk());
    }

    @Test
    void apiDocs_ehPublico() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void h2Console_foraDoPerfilDev_exigeSessao() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    private Unidade novaUnidade() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Unidade unidade = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Unidade " + sufixo)
                .sigla("U" + sufixo)
                .build());
        unidadesCriadas.add(unidade);
        return unidade;
    }

    private Laboratorio novoLaboratorio(Unidade unidade) {
        Laboratorio laboratorio = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade)
                .nome("Laboratório " + unidade.getSigla())
                .ativo(true)
                .build());
        laboratoriosCriados.add(laboratorio);
        return laboratorio;
    }

    private Usuario novoUsuario(Unidade unidade, Perfil perfil, SituacaoUsuario situacao) {
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

    @Test
    void naoDeixaUsuariosOuUnidadesAposOTeste_verificadoPeloAfterEach() {
        // Guards M4: the cleanup lists must track every helper-created entity.
        Unidade unidade = novaUnidade();
        novoUsuario(unidade, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);
        removerDadosCriados();

        assertThat(unidadeRepository.findByPublicId(unidade.getPublicId())).isEmpty();
        assertThat(usuarioRepository.findByUnidadePublicId(unidade.getPublicId())).isEmpty();
        unidadesCriadas.clear();
        usuariosCriados.clear();
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
