package com.sgl.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.UsuarioRepository;

/** The developer-only test login over HTTP: demo seed plus the teste.* users, through the real security chain. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@DirtiesContext
class DevAuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;

    @Test
    void entrar_loopbackIpv4_emiteSessao() throws Exception {
        mockMvc.perform(entrar("teste.gestor.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.perfil").value("GESTOR"));
    }

    @Test
    void entrar_loopbackIpv6_emiteSessao() throws Exception {
        mockMvc.perform(entrar("teste.gestor.1").with(remoteAddr("::1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.perfil").value("GESTOR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/dev/usuarios", "/api/v1/auth/dev/resumo", "/api/v1/auth/dev/entrar"})
    void rotas_ipExterno_403(String rota) throws Exception {
        MockHttpServletRequestBuilder requisicao = rota.endsWith("entrar")
                ? entrar("teste.gestor.1")
                : get(rota);
        mockMvc.perform(requisicao.with(remoteAddr("10.0.0.5")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/dev/usuarios", "/api/v1/auth/dev/resumo", "/api/v1/auth/dev/entrar"})
    void rotas_origemVazia_403(String rota) throws Exception {
        MockHttpServletRequestBuilder requisicao = rota.endsWith("entrar")
                ? entrar("teste.gestor.1")
                : get(rota);
        mockMvc.perform(requisicao.with(remoteAddr("")))
                .andExpect(status().isForbidden());
    }

    @Test
    void entrar_xForwardedForLoopback_ignorado_403() throws Exception {
        mockMvc.perform(entrar("teste.gestor.1")
                        .with(remoteAddr("10.0.0.5"))
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void entrar_inexistente_401() throws Exception {
        mockMvc.perform(entrar("ninguem.existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void entrar_inativo_401() throws Exception {
        Usuario usuario = usuarioRepository.findByLogin("teste.tecnico.3").orElseThrow();
        usuario.setAtivo(false);
        usuarioRepository.saveAndFlush(usuario);
        try {
            mockMvc.perform(entrar("teste.tecnico.3"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        } finally {
            usuario.setAtivo(true);
            usuarioRepository.saveAndFlush(usuario);
        }
    }

    @Test
    void usuarios_filtraPorPerfil() throws Exception {
        mockMvc.perform(get("/api/v1/auth/dev/usuarios").param("perfil", "ESTAGIARIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].perfil", everyItem(is("ESTAGIARIO"))))
                .andExpect(jsonPath("$[*].login",
                        hasItems("teste.estagiario.1", "teste.estagiario.2", "teste.estagiario.3")));
    }

    @Test
    void usuarios_buscaComCoringaLiteral_naoCasaTudo() throws Exception {
        mockMvc.perform(get("/api/v1/auth/dev/usuarios").param("busca", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void usuarios_semUnidade_naoQuebra() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Fulano Sem Unidade Zzz");
        usuario.setLogin("fulano.sem.unidade");
        usuario.setPerfil(Perfil.ADMINISTRADOR_GERAL);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setOrigem(OrigemUsuario.DEV);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario);
        try {
            mockMvc.perform(get("/api/v1/auth/dev/usuarios").param("busca", "sem unidade zzz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].unidadeId", nullValue()))
                    .andExpect(jsonPath("$[0].laboratorios", empty()));
        } finally {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        }
    }

    @Test
    void resumo_trazSetePerfisEUnidades() throws Exception {
        mockMvc.perform(get("/api/v1/auth/dev/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contagemPorPerfil.length()").value(Perfil.values().length))
                .andExpect(jsonPath("$.contagemPorPerfil.ADMINISTRADOR_GERAL", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.ADMINISTRADOR", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.GESTOR", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.TECNICO", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.ANALISTA", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.PESQUISADOR", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.contagemPorPerfil.ESTAGIARIO", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.unidades", not(empty())));
    }

    private static MockHttpServletRequestBuilder entrar(String login) {
        return post("/api/v1/auth/dev/entrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"%s\"}".formatted(login));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(String addr) {
        return request -> {
            request.setRemoteAddr(addr);
            return request;
        };
    }
}
