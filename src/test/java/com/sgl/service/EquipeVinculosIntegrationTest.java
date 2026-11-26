package com.sgl.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.web.servlet.RequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;
import com.sgl.security.JwtService;

/** Temporary access to a laboratory, managed from that laboratory's team (spec fase 2, 7.2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class EquipeVinculosIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VinculoOperacionalUsuarioRepository vinculoRepository;

    private Unidade unidade;
    private Laboratorio labA;
    private Laboratorio labB;
    private Usuario responsavelA;

    @BeforeEach
    void setUp() {
        unidade = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade vínculos")
                .sigla("VI-" + UUID.randomUUID().toString().substring(0, 6)).build());
        responsavelA = novoUsuario(Perfil.PESQUISADOR);
        labA = novoLaboratorio("Lab A", responsavelA);
        labB = novoLaboratorio("Lab B", novoUsuario(Perfil.PESQUISADOR));
    }

    @AfterEach
    void tearDown() {
        vinculoRepository.deleteAll(vinculoRepository.findByUsuarioUnidadePublicId(unidade.getPublicId()));
        List<Laboratorio> labs = laboratorioRepository.findByUnidadePublicId(unidade.getPublicId());
        labs.forEach(l -> l.setResponsavel(null));
        laboratorioRepository.saveAllAndFlush(labs);
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(unidade.getPublicId()));
        laboratorioRepository.deleteAll(labs);
        unidadeRepository.delete(unidade);
    }

    @Test
    void responsavelCriaEditaEEncerraAcessoTemporarioNoSeuLaboratorio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        String corpo = mockMvc.perform(criar(labA, responsavelA, tecnico, labA, LocalDate.now().plusDays(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laboratorioId").value(labA.getPublicId().toString()))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");

        mockMvc.perform(put(vinculos(labA) + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(responsavelA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataFim\":\"%s\",\"observacao\":\"prorrogado\"}".formatted(LocalDate.now().plusDays(20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacao").value("prorrogado"));
        mockMvc.perform(get(vinculos(labA)).header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(put(vinculos(labA) + "/" + id + "/encerrar").header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encerrado").value(true));
    }

    @Test
    // A link that has not started yet ends on its own start date, never before it.
    void encerrarAcessoAgendadoNaoDeixaFimAntesDoInicio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        LocalDate inicio = LocalDate.now().plusDays(5);

        String id = JsonPath.read(mockMvc.perform(criarAgendado(tecnico, inicio)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put(vinculos(labA) + "/" + id + "/encerrar").header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encerrado").value(true))
                .andExpect(jsonPath("$.dataFim").value(inicio.toString()));
    }

    @Test
    void retirarDaEquipeQuemTemAcessoAgendadoNaoDeixaFimAntesDoInicio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        tecnico.substituirLaboratorios(List.of(labA));
        usuarioRepository.saveAndFlush(tecnico);
        LocalDate inicio = LocalDate.now().plusDays(5);
        mockMvc.perform(criarAgendado(tecnico, inicio)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/laboratorios/" + labA.getPublicId() + "/equipe/" + tecnico.getPublicId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isNoContent());

        assertThat(vinculoRepository.findByUsuarioUnidadePublicId(unidade.getPublicId()))
                .allSatisfy(v -> assertThat(v.getDataFim()).isEqualTo(inicio));
    }

    private RequestBuilder criarAgendado(Usuario alvo, LocalDate inicio) {
        return post(vinculos(labA)).header(HttpHeaders.AUTHORIZATION, bearer(responsavelA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":"%s","laboratorioId":"%s","dataInicio":"%s","dataFim":"%s"}
                    """.formatted(alvo.getPublicId(), labA.getPublicId(), inicio, inicio.plusDays(10)));
    }

    @Test
    void vinculoComLaboratorioDiferenteDoCaminho_400() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(criar(labA, responsavelA, tecnico, labB, LocalDate.now().plusDays(10)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vinculoDeOutroLaboratorioNaoEhEncontradoPeloCaminhoDoSeu() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        Usuario admin = novoUsuario(Perfil.ADMINISTRADOR);
        String corpo = mockMvc.perform(criar(labB, admin, tecnico, labB, LocalDate.now().plusDays(10)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String idDeB = JsonPath.read(corpo, "$.id");

        mockMvc.perform(put(vinculos(labA) + "/" + idDeB + "/encerrar").header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isNotFound());
        assertThat(vinculoRepository.findAll().stream()
                .filter(v -> v.getPublicId().toString().equals(idDeB)).findFirst().orElseThrow().getEncerrado()).isFalse();
    }

    @Test
    void gestorSozinho_403() throws Exception {
        Usuario gestor = novoUsuario(Perfil.GESTOR);
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(criar(labA, gestor, tecnico, labA, LocalDate.now().plusDays(10)))
                .andExpect(status().isForbidden());
    }

    @Test
    void estagiarioNaoRecebeAcessoTemporario_400() throws Exception {
        Usuario estagiario = novoUsuario(Perfil.ESTAGIARIO);

        mockMvc.perform(criar(labA, responsavelA, estagiario, labA, LocalDate.now().plusDays(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Estagiários usam vínculos de estágio e participações existentes."));
    }

    @Test
    void ninguemDaAcessoTemporarioASiMesmo_403() throws Exception {
        Usuario admin = novoUsuario(Perfil.ADMINISTRADOR);

        mockMvc.perform(criar(labA, admin, admin, labA, LocalDate.now().plusDays(10)))
                .andExpect(status().isForbidden());
    }

    private static String vinculos(Laboratorio lab) {
        return "/api/v1/laboratorios/" + lab.getPublicId() + "/equipe/vinculos";
    }

    private RequestBuilder criar(Laboratorio caminho, Usuario ator, Usuario alvo, Laboratorio lab, LocalDate fim) {
        return post(vinculos(caminho)).header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":"%s","laboratorioId":"%s","dataInicio":"%s","dataFim":"%s"}
                    """.formatted(alvo.getPublicId(), lab.getPublicId(), LocalDate.now(), fim));
    }

    private Laboratorio novoLaboratorio(String nome, Usuario responsavel) {
        return laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome(nome).ativo(true).responsavel(responsavel).build());
    }

    private Usuario novoUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
