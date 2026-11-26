package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.JwtService;

/**
 * Every responsável of the Unidade sees all pending newcomers and approves them into their own
 * laboratory; only administrators refuse (spec fase 2, D1/D1b and 7.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class EquipePendentesIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private final List<Unidade> unidades = new ArrayList<>();
    private Unidade unidade;
    private Laboratorio labA;
    private Laboratorio labB;
    private Usuario responsavelA;
    private Usuario responsavelB;

    @BeforeEach
    void setUp() {
        unidade = novaUnidade();
        responsavelA = novoUsuario(unidade, Perfil.PESQUISADOR, SituacaoUsuario.ATIVO);
        responsavelB = novoUsuario(unidade, Perfil.PESQUISADOR, SituacaoUsuario.ATIVO);
        labA = novoLaboratorio("Lab A", responsavelA);
        labB = novoLaboratorio("Lab B", responsavelB);
    }

    @AfterEach
    void tearDown() {
        for (Unidade u : unidades) {
            List<Laboratorio> labs = laboratorioRepository.findByUnidadePublicId(u.getPublicId());
            labs.forEach(l -> l.setResponsavel(null));
            laboratorioRepository.saveAllAndFlush(labs);
            usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(u.getPublicId()));
            laboratorioRepository.deleteAll(labs);
            unidadeRepository.delete(u);
        }
    }

    @Test
    void responsavelVeTodosOsPendentesDaUnidadeEAprovaParaOSeuLaboratorio() throws Exception {
        Usuario pendente1 = pendente();
        pendente();

        mockMvc.perform(get(equipe(labA) + "/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(aprovar(labA, pendente1, responsavelA, "{\"categoriaCargo\":\"ESTAGIARIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"));

        Usuario salvo = usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(pendente1.getPublicId()).orElseThrow();
        assertThat(salvo.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(salvo.getPapeis()).isEmpty();
        assertThat(salvo.getCategoriaCargo()).isEqualTo(CategoriaCargo.ESTAGIARIO);
        assertThat(salvo.getLaboratorios()).extracting(Laboratorio::getPublicId).containsExactly(labA.getPublicId());
    }

    @Test
    void aprovarSemCategoriaMantemAAtual() throws Exception {
        Usuario pendente = pendente();

        mockMvc.perform(aprovar(labA, pendente, responsavelA, "{}")).andExpect(status().isOk());

        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow().getCategoriaCargo())
                .isEqualTo(CategoriaCargo.TECNICO);
    }

    @Test
    void segundoResponsavelAprovandoAMesmaPessoa_409() throws Exception {
        Usuario pendente = pendente();

        mockMvc.perform(aprovar(labA, pendente, responsavelA, "{}")).andExpect(status().isOk());
        mockMvc.perform(aprovar(labB, pendente, responsavelB, "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_NAO_PENDENTE"));
    }

    @Test
    void responsavelNaoAprovaParaOLaboratorioDeOutro_403() throws Exception {
        Usuario pendente = pendente();

        mockMvc.perform(aprovar(labB, pendente, responsavelA, "{}")).andExpect(status().isForbidden());
    }

    @Test
    void gestorSozinho_403() throws Exception {
        Usuario gestor = novoUsuario(unidade, Perfil.GESTOR, SituacaoUsuario.ATIVO);
        Usuario pendente = pendente();

        mockMvc.perform(get(equipe(labA) + "/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(aprovar(labA, pendente, gestor, "{}")).andExpect(status().isForbidden());
    }

    @Test
    void responsavelNaoRecusaSoOAdministradorLocal() throws Exception {
        Usuario pendente = pendente();
        Usuario admin = novoUsuario(unidade, Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO);

        mockMvc.perform(delete("/api/v1/usuarios/" + pendente.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/usuarios/" + pendente.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    // Same rule as the central approval: a target from another Unidade is a 403, not a 404.
    void pendenteDeOutraUnidade_403() throws Exception {
        Unidade outra = novaUnidade();
        Usuario deOutra = novoUsuario(outra, Perfil.ESTAGIARIO, SituacaoUsuario.PENDENTE_VINCULACAO);

        mockMvc.perform(aprovar(labA, deOutra, responsavelA, "{}")).andExpect(status().isForbidden());
    }

    @Test
    void aprovacaoEhAuditadaComOLaboratorio(CapturedOutput output) throws Exception {
        Usuario pendente = pendente();

        mockMvc.perform(aprovar(labA, pendente, responsavelA, "{}")).andExpect(status().isOk());

        assertThat(output.getOut()).contains("acao=PENDENTE_APROVADO ator=" + responsavelA.getPublicId()
                + " alvo=" + pendente.getPublicId() + " laboratorio=" + labA.getPublicId());
    }

    private Usuario pendente() {
        Usuario pendente = novoUsuario(unidade, Perfil.TECNICO, SituacaoUsuario.PENDENTE_VINCULACAO);
        return pendente;
    }

    private static String equipe(Laboratorio lab) {
        return "/api/v1/laboratorios/" + lab.getPublicId() + "/equipe";
    }

    private RequestBuilder aprovar(Laboratorio lab, Usuario alvo, Usuario ator, String corpo) {
        return post(equipe(lab) + "/pendentes/" + alvo.getPublicId() + "/aprovar")
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }

    private Unidade novaUnidade() {
        Unidade nova = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade pendentes")
                .sigla("PE-" + UUID.randomUUID().toString().substring(0, 6)).build());
        unidades.add(nova);
        return nova;
    }

    private Laboratorio novoLaboratorio(String nome, Usuario responsavel) {
        return laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome(nome).ativo(true).responsavel(responsavel).build());
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

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
