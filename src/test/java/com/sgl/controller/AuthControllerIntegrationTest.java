package com.sgl.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

/** The auth endpoints over HTTP, through the real security chain, with only the gateway faked. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerIntegrationTest {

    private static final int CNPMF = 7;
    private static final String CPF = "12345678909";
    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired private MockMvc mockMvc;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;

    private Unidade cnpmf;

    @BeforeEach
    void setUp() {
        gateway.reset();
        cnpmf = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("CNPMF-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CNPMF)
                .build());
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId()));
        unidadeRepository.delete(cnpmf);
    }

    @Test
    void login_sucesso_retornaTokenQueAbreMe() throws Exception {
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Fulano de Tal", CPF, "Analista A", CNPMF, true);

        MvcResult result = mockMvc.perform(login("m344136", "senha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.usuario.situacao").value("PENDENTE_VINCULACAO"))
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fulano de Tal"))
                .andExpect(jsonPath("$.unidadeId").value(cnpmf.getPublicId().toString()))
                .andExpect(jsonPath("$.unidadeSigla").value(cnpmf.getSigla()))
                .andExpect(jsonPath("$.situacao").value("PENDENTE_VINCULACAO"));
    }

    @Test
    void login_credenciaisInvalidas_401() throws Exception {
        gateway.loginStatus(HttpStatus.UNAUTHORIZED, 1);

        mockMvc.perform(login("m777", "errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos."));
    }

    @Test
    void login_semSenha_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"m1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_contaDesabilitada_403() throws Exception {
        gateway.loginOk("m344136", "344136", 514);

        mockMvc.perform(login("m344136", "senha"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONTA_DESATIVADA"));
        gateway.verify();
    }

    @Test
    void login_inativoNaBase_403() throws Exception {
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Aposentado", CPF, "Técnico A", CNPMF, false);

        mockMvc.perform(login("m344136", "senha"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONTA_DESATIVADA"));
    }

    @Test
    void login_unidadeNaoHabilitada_403() throws Exception {
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "De Outra Unidade", CPF, "Analista A", 99, true);

        mockMvc.perform(login("m344136", "senha"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_NAO_HABILITADA"));
        // Units are never created on the fly.
        assertThat(unidadeRepository.findByCodigoCorporativo(99)).isEmpty();
    }

    @Test
    void login_pessoaForaDasBasesCorporativas_403() throws Exception {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfNaoEncontrado(CPF);
        gateway.colaboradorPorCpfNaoEncontrado(CPF);

        mockMvc.perform(login(CPF, "senha"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PESSOA_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.message")
                        .value("Seu cadastro não foi encontrado nas bases corporativas da Embrapa."))
                .andExpect(content().string(not(containsString(CPF))));
    }

    @Test
    void login_gatewayFora_503() throws Exception {
        gateway.loginStatus(HttpStatus.SERVICE_UNAVAILABLE, 1);

        mockMvc.perform(login("m344136", "senha"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("AUTENTICACAO_INDISPONIVEL"))
                .andExpect(jsonPath("$.message")
                        .value("Autenticação corporativa indisponível. Tente novamente em instantes."));
    }

    @Test
    void gatewayFora_logaCausaEStatusSemUrlNemCpf(CapturedOutput output) throws Exception {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfStatus(CPF, HttpStatus.INTERNAL_SERVER_ERROR);

        mockMvc.perform(login(CPF, "senha"))
                .andExpect(status().isServiceUnavailable());

        String log = output.getOut() + output.getErr();
        assertThat(log).contains("Corporate gateway unavailable").contains("status=500");
        // The gateway URL carries the CPF as a path segment, so neither may reach the log.
        assertThat(log).doesNotContain(CPF).doesNotContain("porCpf");
    }

    @Test
    void login_sextaFalhaEmQuinzeMinutos_429() throws Exception {
        // A login used nowhere else, so earlier tests cannot have consumed its attempts.
        String login = "m9" + (System.nanoTime() % 100000);
        gateway.loginStatus(HttpStatus.UNAUTHORIZED, 5);

        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            mockMvc.perform(login(login, "errada")).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login(login, "errada"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("MUITAS_TENTATIVAS"));
        // The sixth attempt is refused before it reaches the gateway.
        gateway.verify();
    }

    @Test
    void respostaDeLoginEMe_naoContemCpf() throws Exception {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfNaoEncontrado(CPF);
        gateway.colaboradorPorCpf(CPF, "Estagiária", CNPMF, true);

        MvcResult result = mockMvc.perform(login("123.456.789-09", "senha"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(CPF))))
                .andExpect(content().string(not(containsString("123.456.789-09"))))
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.matricula").doesNotExist())
                .andExpect(content().string(not(containsString(CPF))));
    }

    @Test
    void me_semToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void config_informaDevLoginInativoForaDeDevEDemo() throws Exception {
        mockMvc.perform(get("/api/v1/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devLoginAtivo").value(false));
    }

    @Test
    void devLogin_inexistenteForaDeDevEDemo() throws Exception {
        mockMvc.perform(post("/api/v1/auth/dev-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"gestor.cnpmf\"}"))
                .andExpect(status().isNotFound());
    }

    private static MockHttpServletRequestBuilder login(String login, String senha) {
        return post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha));
    }
}
