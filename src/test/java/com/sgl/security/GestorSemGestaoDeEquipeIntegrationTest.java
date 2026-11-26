package com.sgl.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;

/**
 * Team management moved to each laboratory's responsável; centrally, only administrators keep it.
 * A GESTOR keeps the transversal work and loses these routes (spec fase 2, 7.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class GestorSemGestaoDeEquipeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VinculoOperacionalUsuarioRepository vinculoRepository;

    private Unidade unidade;
    private Laboratorio lab;
    private Usuario gestor;
    private Usuario admin;
    private Usuario tecnico;

    @BeforeEach
    void setUp() {
        unidade = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade gestor")
                .sigla("GS-" + UUID.randomUUID().toString().substring(0, 6)).build());
        lab = laboratorioRepository.saveAndFlush(Laboratorio.builder().unidade(unidade).nome("Lab").ativo(true).build());
        gestor = novoUsuario(Perfil.GESTOR, List.of());
        admin = novoUsuario(Perfil.ADMINISTRADOR, List.of());
        tecnico = novoUsuario(Perfil.TECNICO, List.of(lab));
    }

    @AfterEach
    void tearDown() {
        vinculoRepository.deleteAll(vinculoRepository.findByUsuarioUnidadePublicId(unidade.getPublicId()));
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(unidade.getPublicId()));
        laboratorioRepository.deleteAll(laboratorioRepository.findByUnidadePublicId(unidade.getPublicId()));
        unidadeRepository.delete(unidade);
    }

    @Test
    void gestorNaoTrocaAListaDeLaboratoriosNemCriaVinculos() throws Exception {
        mockMvc.perform(put("/api/v1/vinculos-operacionais/usuarios/" + tecnico.getPublicId() + "/laboratorios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioIds\":[\"%s\"]}".formatted(lab.getPublicId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/vinculos-operacionais").header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"%s\",\"laboratorioId\":\"%s\",\"dataInicio\":\"%s\",\"dataFim\":\"%s\"}"
                                .formatted(tecnico.getPublicId(), lab.getPublicId(), LocalDate.now(), LocalDate.now().plusDays(5))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/vinculos-operacionais/resumo").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorNaoVePendentesNemAprovaNemRecusa() throws Exception {
        Usuario pendente = novoUsuario(Perfil.ESTAGIARIO, List.of());
        pendente.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);
        pendente = usuarioRepository.saveAndFlush(pendente);

        mockMvc.perform(get("/api/v1/usuarios/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/usuarios/" + pendente.getPublicId() + "/aprovar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioIds\":[\"%s\"]}".formatted(lab.getPublicId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/usuarios/" + pendente.getPublicId() + "/pendente")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/usuarios/pendentes").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void gestorNaoBuscaNemImportaDaEmbrapaNemVeCargosASubstituir() throws Exception {
        mockMvc.perform(get("/api/v1/pessoas-corporativas").param("tipo", "EMPREGADO").param("termo", "joao")
                        .header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/pessoas-corporativas/importar").header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMPREGADO\",\"referencia\":\"1\",\"categoriaCargo\":\"TECNICO\",\"laboratorioIds\":[\"%s\"]}"
                                .formatted(lab.getPublicId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/cargos-a-substituir").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/cargos-a-substituir").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void cadaPessoaLeOsPropriosVinculosMasNaoOsDeOutra() throws Exception {
        VinculoOperacionalUsuario vinculo = new VinculoOperacionalUsuario();
        vinculo.setUsuario(tecnico);
        vinculo.setLaboratorio(lab);
        vinculo.setDataInicio(LocalDate.now());
        vinculo.setDataFim(LocalDate.now().plusDays(5));
        vinculoRepository.saveAndFlush(vinculo);
        Usuario outro = novoUsuario(Perfil.TECNICO, List.of(lab));

        mockMvc.perform(get("/api/v1/vinculos-operacionais/usuarios/" + tecnico.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/vinculos-operacionais/usuarios/" + tecnico.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(outro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/vinculos-operacionais/usuarios/" + tecnico.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void gestorEditaNomeMasNaoTrocaLaboratoriosPeloCadastroDeUsuario() throws Exception {
        mockMvc.perform(atualizar(tecnico, gestor, "Nome Novo", lab)).andExpect(status().isOk());
        mockMvc.perform(atualizar(tecnico, gestor, "Nome Novo", null)).andExpect(status().isForbidden());

        Usuario salvo = usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(tecnico.getPublicId()).orElseThrow();
        assertThat(salvo.getLaboratorios()).hasSize(1);
    }

    @Test
    void edicaoDeQuemEstaSemLaboratorioNaoExigeLaboratorio() throws Exception {
        Usuario semLab = novoUsuario(Perfil.TECNICO, List.of());

        mockMvc.perform(atualizar(semLab, admin, "Nome Corrigido", null)).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.RequestBuilder atualizar(Usuario alvo, Usuario ator, String nome,
            Laboratorio laboratorio) {
        String labs = laboratorio == null ? "[]" : "[\"" + laboratorio.getPublicId() + "\"]";
        return put("/api/v1/usuarios/" + alvo.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"%s\",\"email\":\"e-%s@embrapa.br\",\"unidadeId\":\"%s\",\"ativo\":true,\"laboratorioIds\":%s}"
                        .formatted(nome, UUID.randomUUID(), unidade.getPublicId(), labs));
    }

    private Usuario novoUsuario(Perfil perfil, List<Laboratorio> labs) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.substituirLaboratorios(labs);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
