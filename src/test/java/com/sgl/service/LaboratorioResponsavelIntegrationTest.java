package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.sgl.security.JwtService;

/** Every laboratory has a responsável, who is part of its team (spec fase 2, 7.5). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class LaboratorioResponsavelIntegrationTest {

    private static final String LABORATORIOS = "/api/v1/laboratorios";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Unidade unidade;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        unidade = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade responsável")
                .sigla("RS-" + UUID.randomUUID().toString().substring(0, 6)).build());
        admin = novoUsuario(Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO, true);
    }

    @AfterEach
    void tearDown() {
        List<Laboratorio> labs = laboratorioRepository.findByUnidadePublicId(unidade.getPublicId());
        labs.forEach(l -> l.setResponsavel(null));
        laboratorioRepository.saveAllAndFlush(labs);
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(unidade.getPublicId()));
        laboratorioRepository.deleteAll(labs);
        unidadeRepository.delete(unidade);
    }

    @Test
    void criarSemResponsavel_400() throws Exception {
        mockMvc.perform(criar(null)).andExpect(status().isBadRequest());
    }

    @Test
    void responsavelInativoOuPendente_400() throws Exception {
        Usuario inativo = novoUsuario(Perfil.PESQUISADOR, SituacaoUsuario.ATIVO, false);
        Usuario pendente = novoUsuario(Perfil.PESQUISADOR, SituacaoUsuario.PENDENTE_VINCULACAO, true);

        mockMvc.perform(criar(inativo)).andExpect(status().isBadRequest());
        mockMvc.perform(criar(pendente)).andExpect(status().isBadRequest());
    }

    @Test
    void responsavelPassaAFazerParteDaEquipe() throws Exception {
        Usuario pesquisador = novoUsuario(Perfil.PESQUISADOR, SituacaoUsuario.ATIVO, true);

        String corpo = mockMvc.perform(criar(pesquisador)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID labId = UUID.fromString(JsonPath.read(corpo, "$.id"));

        assertThat(laboratoriosDe(pesquisador)).containsExactly(labId);
    }

    @Test
    void trocarOResponsavelMantemOAnteriorNaEquipeEIncluiONovo() throws Exception {
        Usuario anterior = novoUsuario(Perfil.PESQUISADOR, SituacaoUsuario.ATIVO, true);
        Usuario novo = novoUsuario(Perfil.ANALISTA, SituacaoUsuario.ATIVO, true);
        String corpo = mockMvc.perform(criar(anterior)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID labId = UUID.fromString(JsonPath.read(corpo, "$.id"));

        mockMvc.perform(put(LABORATORIOS + "/" + labId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Lab Novo", novo)))
                .andExpect(status().isOk());

        assertThat(laboratoriosDe(anterior)).containsExactly(labId);
        assertThat(laboratoriosDe(novo)).containsExactly(labId);
    }

    @Test
    // Closing a laboratory whose responsável left must not require naming a new one.
    void inativarLaboratorioComResponsavelInativo_200() throws Exception {
        Usuario saiu = novoUsuario(Perfil.PESQUISADOR, SituacaoUsuario.ATIVO, true);
        Laboratorio lab = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Fechando").ativo(true).responsavel(saiu).build());
        saiu.setAtivo(false);
        usuarioRepository.saveAndFlush(saiu);

        mockMvc.perform(put(LABORATORIOS + "/" + lab.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInativo("Lab Fechando", saiu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void inativarLaboratorioSemResponsavel_200() throws Exception {
        Laboratorio lab = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Antigo").ativo(true).build());

        mockMvc.perform(put(LABORATORIOS + "/" + lab.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInativo("Lab Antigo", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void editarLaboratorioAtivoSemResponsavel_400() throws Exception {
        Laboratorio lab = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Ativo").ativo(true).build());

        mockMvc.perform(put(LABORATORIOS + "/" + lab.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Lab Ativo", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laboratorioSemResponsavelApareceEmCargosASubstituir() throws Exception {
        Laboratorio semResponsavel = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Órfão").ativo(true).build());

        mockMvc.perform(get("/api/v1/cargos-a-substituir").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.registroId == '%s')].tipo".formatted(semResponsavel.getPublicId()))
                        .value("RESPONSAVEL_LABORATORIO"))
                .andExpect(jsonPath("$[?(@.registroId == '%s')].descricao".formatted(semResponsavel.getPublicId()))
                        .value("Laboratório Lab Órfão sem responsável"));
    }

    private RequestBuilder criar(Usuario responsavel) {
        return post(LABORATORIOS).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("Lab Novo", responsavel));
    }

    private String corpo(String nome, Usuario responsavel) {
        String resp = responsavel == null ? "" : ",\"responsavelId\":\"" + responsavel.getPublicId() + "\"";
        return "{\"unidadeId\":\"%s\",\"nome\":\"%s\",\"ativo\":true%s}".formatted(unidade.getPublicId(), nome, resp);
    }

    private String corpoInativo(String nome, Usuario responsavel) {
        String resp = responsavel == null ? "" : ",\"responsavelId\":\"" + responsavel.getPublicId() + "\"";
        return "{\"unidadeId\":\"%s\",\"nome\":\"%s\",\"ativo\":false%s}".formatted(unidade.getPublicId(), nome, resp);
    }

    private List<UUID> laboratoriosDe(Usuario usuario) {
        return usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(usuario.getPublicId()).orElseThrow()
                .getLaboratorios().stream().map(Laboratorio::getPublicId).toList();
    }

    private Usuario novoUsuario(Perfil perfil, SituacaoUsuario situacao, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(ativo);
        usuario.setSituacao(situacao);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
