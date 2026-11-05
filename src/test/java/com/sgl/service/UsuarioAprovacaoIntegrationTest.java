package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.exception.UsuarioRejectedException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.JwtService;
import com.sgl.tenant.TenantContext;

/**
 * Pending queue, approval, refusal and profile changes through the real security chain and H2.
 * Not @Transactional (the filters read outside the test's persistence context); everything created
 * here is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class UsuarioAprovacaoIntegrationTest {

    private static final int CODIGO = 7;
    private static final String USUARIOS = "/api/v1/usuarios";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioService usuarioService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario gestor;
    private final List<Unidade> unidades = new ArrayList<>();
    private final List<Usuario> administradoresGerais = new ArrayList<>();

    @BeforeEach
    void setUp() {
        gateway.reset();
        unidade = novaUnidade(CODIGO);
        laboratorio = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Base").ativo(true).build());
        gestor = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.limpar();
        // Other test classes share this context and count active general administrators.
        usuarioRepository.deleteAll(administradoresGerais);
        for (Unidade u : unidades) {
            usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(u.getPublicId()));
            laboratorioRepository.deleteAll(laboratorioRepository.findByUnidadePublicId(u.getPublicId()));
            unidadeRepository.delete(u);
        }
    }

    @Test
    void pendentes_listaSoDaUnidadeEmOperacao() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);
        novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);
        novoUsuario(novaUnidade(98), Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(get(USUARIOS + "/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(pendente.getPublicId().toString()))
                .andExpect(jsonPath("$[0].situacao").value("PENDENTE_VINCULACAO"));
    }

    @Test
    void pendentes_tecnico_403() throws Exception {
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(get(USUARIOS + "/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovar_ativaComPerfilELaboratorio() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(aprovar(pendente, gestor, "ANALISTA", laboratorio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.perfil").value("ANALISTA"))
                .andExpect(jsonPath("$.laboratorioId").value(laboratorio.getPublicId().toString()));

        Usuario salvo = usuarioRepository.findWithUnidadeAndLaboratorioByPublicId(pendente.getPublicId()).orElseThrow();
        assertThat(salvo.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.ANALISTA);
        assertThat(salvo.getLaboratorio().getPublicId()).isEqualTo(laboratorio.getPublicId());
    }

    @Test
    void aprovar_pessoaDeOutraUnidade_403() throws Exception {
        Unidade outra = novaUnidade(98);
        Usuario pendente = novoUsuario(outra, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(aprovar(pendente, gestor, "ANALISTA", laboratorio))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow().getSituacao())
                .isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
    }

    @Test
    void aprovar_perfilAcimaDoPermitido_403() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(aprovar(pendente, gestor, "ADMINISTRADOR", laboratorio))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovar_usuarioJaAtivo_409() throws Exception {
        Usuario ativo = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(aprovar(ativo, gestor, "ANALISTA", laboratorio))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_PENDENTE"));
    }

    @Test
    void recusar_removePendente_eNovoLoginRecriaPendente() throws Exception {
        // Só colaborador fica pendente: empregado entra direto.
        gateway.loginOk("12345678909", "12345678909", 512);
        gateway.empregadoPorCpfNaoEncontrado("12345678909");
        gateway.colaboradorPorCpf("12345678909", "Fulano de Tal", CODIGO, true);
        mockMvc.perform(login("12345678909"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.situacao").value("PENDENTE_VINCULACAO"));
        Usuario pendente = usuarioRepository.findByCpf("12345678909").orElseThrow();

        mockMvc.perform(delete(USUARIOS + "/" + pendente.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId())).isEmpty();

        gateway.reset();
        // Só colaborador fica pendente: empregado entra direto.
        gateway.loginOk("12345678909", "12345678909", 512);
        gateway.empregadoPorCpfNaoEncontrado("12345678909");
        gateway.colaboradorPorCpf("12345678909", "Fulano de Tal", CODIGO, true);
        mockMvc.perform(login("12345678909"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.situacao").value("PENDENTE_VINCULACAO"));
        assertThat(usuarioRepository.findByCpf("12345678909")).isPresent();
    }

    @Test
    void recusar_usuarioAtivo_409() throws Exception {
        Usuario ativo = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(delete(USUARIOS + "/" + ativo.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_PENDENTE"));
        assertThat(usuarioRepository.findByPublicId(ativo.getPublicId())).isPresent();
    }

    @Test
    void recusar_pessoaDeOutraUnidade_403() throws Exception {
        Usuario pendente = novoUsuario(novaUnidade(98), Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(delete(USUARIOS + "/" + pendente.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId())).isPresent();
    }

    // Caso real (homologação, 05/11/2026): o gestor trocou o perfil de um estagiário pendente achando que
    // isso liberava o acesso, e ele seguiu bloqueado. Só "Aprovar" tira alguém de PENDENTE_VINCULACAO,
    // então trocar perfil ou laboratório de um pendente é recusado com um código que a tela entende.
    @Test
    void trocarPerfil_usuarioPendente_409() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(trocarPerfil(pendente, gestor, "ESTAGIARIO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
        Usuario salvo = usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow();
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.TECNICO);
        assertThat(salvo.getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
    }

    @Test
    void atualizar_usuarioPendenteMudandoPerfil_409() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(atualizar(pendente, gestor, "ESTAGIARIO", true, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow().getPerfil())
                .isEqualTo(Perfil.TECNICO);
    }

    @Test
    void atualizar_usuarioPendenteRecebendoLaboratorio_409() throws Exception {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(atualizar(pendente, gestor, "TECNICO", true, laboratorio))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
    }

    @Test
    void trocarPerfil_proprioPerfil_403() throws Exception {
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(trocarPerfil(admin, admin, "TECNICO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trocarPerfil_gestorRebaixandoAdministrador_403() throws Exception {
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        // GESTOR may grant TECNICO, but may not touch someone whose current profile is above theirs.
        mockMvc.perform(trocarPerfil(admin, gestor, "TECNICO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trocarPerfil_ultimoAdministradorGeral_409() {
        // Unreachable over HTTP today (the actor is itself an active general administrator and cannot
        // change its own profile), so the guard is exercised directly with a synthetic session.
        Usuario ultimo = novoAdministradorGeral(true);
        assertThat(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL)).isEqualTo(1);
        AuthenticatedUser ator = new AuthenticatedUser(UUID.randomUUID(), Perfil.ADMINISTRADOR_GERAL,
                SituacaoUsuario.ATIVO, unidade.getPublicId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ator, null, List.of()));
        TenantContext.definir(unidade.getPublicId());

        assertThatThrownBy(() -> usuarioService.alterarPerfil(ultimo.getPublicId(), Perfil.ADMINISTRADOR))
                .isInstanceOfSatisfying(UsuarioRejectedException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR_GERAL"));
        assertThat(usuarioRepository.findByPublicId(ultimo.getPublicId()).orElseThrow().getPerfil())
                .isEqualTo(Perfil.ADMINISTRADOR_GERAL);
    }

    @Test
    void trocarPerfil_administradorGeralRebaixandoOutro_200() throws Exception {
        Usuario ator = novoAdministradorGeral(true);
        Usuario alvo = novoAdministradorGeral(true);

        mockMvc.perform(trocarPerfil(alvo, ator, "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"));
    }

    @Test
    void trocarPerfil_administradorLocalPromovendoAAdministradorGeral_403() throws Exception {
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(trocarPerfil(tecnico, admin, "ADMINISTRADOR_GERAL"))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizar_gestorPromovendoAAdministrador_403() throws Exception {
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        // The general update route also carries a profile; it must not bypass the profile rules.
        mockMvc.perform(put(USUARIOS + "/" + tecnico.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Tecnico","email":"tec-%s@embrapa.br","perfil":"ADMINISTRADOR","unidadeId":"%s"}
                            """.formatted(UUID.randomUUID(), unidade.getPublicId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void inativar_gestorInativandoAdministrador_403() throws Exception {
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(delete(USUARIOS + "/" + admin.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByPublicId(admin.getPublicId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void atualizar_tecnicoDesativandoGestor_403() throws Exception {
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(atualizar(gestor, tecnico, "GESTOR", false, null))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByPublicId(gestor.getPublicId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void atualizar_gestorDesativandoAdministrador_403() throws Exception {
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(atualizar(admin, gestor, "ADMINISTRADOR", false, null))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByPublicId(admin.getPublicId()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void atualizar_proprioLaboratorio_403() throws Exception {
        mockMvc.perform(atualizar(gestor, gestor, "GESTOR", true, laboratorio))
                .andExpect(status().isForbidden());
    }

    @Test
    void atualizar_gestorEditandoTecnico_200() throws Exception {
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(atualizar(tecnico, gestor, "TECNICO", false, laboratorio))
                .andExpect(status().isOk());
        Usuario salvo = usuarioRepository.findByPublicId(tecnico.getPublicId()).orElseThrow();
        assertThat(salvo.getAtivo()).isFalse();
    }

    @Test
    void atualizar_desativandoUltimoAdministradorGeral_409() {
        Usuario ultimo = novoAdministradorGeral(true);
        assertThat(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL)).isEqualTo(1);
        sessaoSintetica(Perfil.ADMINISTRADOR_GERAL);
        com.sgl.dto.request.UsuarioRequestDTO dto = new com.sgl.dto.request.UsuarioRequestDTO();
        dto.setNome(ultimo.getNome());
        dto.setEmail("ag-" + UUID.randomUUID() + "@embrapa.br");
        dto.setPerfil(Perfil.ADMINISTRADOR_GERAL);
        dto.setUnidadeId(unidade.getPublicId());
        dto.setAtivo(false);

        assertThatThrownBy(() -> usuarioService.atualizar(ultimo.getPublicId(), dto))
                .isInstanceOfSatisfying(UsuarioRejectedException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR_GERAL"));
    }

    @Test
    void vinculos_tecnicoNaoConcedeVinculoNemTrocaLaboratorioBase_403() throws Exception {
        Usuario tecnico = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.ATIVO);

        mockMvc.perform(post("/api/v1/vinculos-operacionais").header(HttpHeaders.AUTHORIZATION, bearer(tecnico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"%s\",\"laboratorioId\":\"%s\",\"dataInicio\":\"2026-10-01\"}"
                                .formatted(tecnico.getPublicId(), laboratorio.getPublicId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/vinculos-operacionais/usuarios/" + tecnico.getPublicId() + "/laboratorio-base")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tecnico))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioId\":\"%s\"}".formatted(laboratorio.getPublicId())))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findWithUnidadeAndLaboratorioByPublicId(tecnico.getPublicId()).orElseThrow().getLaboratorio())
                .isNull();
    }

    @Test
    void criar_gestorCriandoAdministrador_403() throws Exception {
        mockMvc.perform(post(USUARIOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Novo","email":"novo-%s@embrapa.br","senha":"SenhaForte123!","perfil":"ADMINISTRADOR","unidadeId":"%s"}
                            """.formatted(UUID.randomUUID(), unidade.getPublicId())))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.RequestBuilder aprovar(Usuario alvo, Usuario ator, String perfil, Laboratorio lab) {
        return post(USUARIOS + "/" + alvo.getPublicId() + "/aprovar")
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfil\":\"%s\",\"laboratorioBaseId\":\"%s\"}".formatted(perfil, lab.getPublicId()));
    }

    private org.springframework.test.web.servlet.RequestBuilder atualizar(Usuario alvo, Usuario ator, String perfil,
            boolean ativo, Laboratorio lab) {
        String labJson = lab == null ? "" : ",\"laboratorioId\":\"" + lab.getPublicId() + "\"";
        return put(USUARIOS + "/" + alvo.getPublicId())
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Nome","email":"e-%s@embrapa.br","perfil":"%s","unidadeId":"%s","ativo":%s%s}
                    """.formatted(UUID.randomUUID(), perfil, unidade.getPublicId(), ativo, labJson));
    }

    private void sessaoSintetica(Perfil perfil) {
        AuthenticatedUser ator = new AuthenticatedUser(UUID.randomUUID(), perfil, SituacaoUsuario.ATIVO, unidade.getPublicId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ator, null, List.of()));
        TenantContext.definir(unidade.getPublicId());
    }

    private org.springframework.test.web.servlet.RequestBuilder trocarPerfil(Usuario alvo, Usuario ator, String perfil) {
        return put(USUARIOS + "/" + alvo.getPublicId() + "/perfil")
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"perfil\":\"%s\"}".formatted(perfil));
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String login) {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"senha\":\"senha\"}".formatted(login));
    }

    private Unidade novaUnidade(int codigo) {
        Unidade nova = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade " + codigo)
                .sigla("UA" + codigo + "-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(codigo).build());
        unidades.add(nova);
        return nova;
    }

    private Usuario novoUsuario(Unidade dono, Perfil perfil, SituacaoUsuario situacao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(dono);
        usuario.setAtivo(true);
        usuario.setSituacao(situacao);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Usuario novoAdministradorGeral(boolean ativo) {
        Usuario usuario = novoUsuario(unidade, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);
        usuario.setAtivo(ativo);
        usuario = usuarioRepository.saveAndFlush(usuario);
        administradoresGerais.add(usuario);
        return usuario;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
