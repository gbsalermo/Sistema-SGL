package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.UsuarioPerfilRequestDTO;
import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.enums.Perfil;
import com.sgl.service.UsuarioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link UsuarioController}. Segue o mesmo
 * padrão fixado em UnidadeControllerTest/LaboratorioControllerTest: bean de
 * {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real da aplicação em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link UsuarioService} é mockado - toda a regra de tenant/negócio (já coberta em
 * UsuarioServiceTest) não é exercitada aqui, só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/usuarios";

    private static final UUID USUARIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    private UsuarioResponseDTO montarResponseDTO() {
        return new UsuarioResponseDTO(
                USUARIO_PUBLIC_ID,
                "Maria Oliveira",
                "maria.oliveira@ufrb.edu.br",
                Perfil.PESQUISADOR,
                UNIDADE_PUBLIC_ID,
                "Instituto de Química",
                "IQ",
                LABORATORIO_PUBLIC_ID,
                "Laboratório de Química Orgânica",
                true);
    }

    @Test
    void deveListarTodosOsUsuariosERetornar200() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(USUARIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarUsuarioPorIdERetornar200() throws Exception {
        when(usuarioService.buscarPorId(USUARIO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", USUARIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Oliveira"));
    }

    @Test
    void deveRetornar404QuandoUsuarioNaoEncontrado() throws Exception {
        when(usuarioService.buscarPorId(USUARIO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Usuário", USUARIO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", USUARIO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarUsuariosPorLaboratorioERetornar200() throws Exception {
        when(usuarioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-laboratorio")
                        .param("laboratorioId", LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveCriarUsuarioERetornar201() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "Maria Oliveira", "maria.oliveira@ufrb.edu.br", "SenhaForte123!",
                Perfil.PESQUISADOR, UNIDADE_PUBLIC_ID, LABORATORIO_PUBLIC_ID, true);
        when(usuarioService.criar(any(UsuarioRequestDTO.class))).thenReturn(montarResponseDTO());

        // UsuarioController.criar usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (UsuarioController.java linha 61) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria.oliveira@ufrb.edu.br"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // UsuarioRequestDTO exige nome, email e perfil (@NotBlank/@NotNull) e unidadeId
        // (@NotNull) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarUsuarioERetornar200() throws Exception {
        UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "Maria Oliveira Atualizada", "maria.oliveira@ufrb.edu.br", null,
                Perfil.PESQUISADOR, UNIDADE_PUBLIC_ID, LABORATORIO_PUBLIC_ID, true);
        when(usuarioService.atualizar(eq(USUARIO_PUBLIC_ID), any(UsuarioRequestDTO.class)))
                .thenReturn(new UsuarioResponseDTO(
                        USUARIO_PUBLIC_ID, "Maria Oliveira Atualizada", "maria.oliveira@ufrb.edu.br",
                        Perfil.PESQUISADOR, UNIDADE_PUBLIC_ID, "Instituto de Química", "IQ",
                        LABORATORIO_PUBLIC_ID, "Laboratório de Química Orgânica", true));

        mockMvc.perform(put(BASE_URL + "/{id}", USUARIO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Oliveira Atualizada"));
    }

    @Test
    void deveAlterarPerfilERetornar200() throws Exception {
        UsuarioPerfilRequestDTO dto = new UsuarioPerfilRequestDTO(Perfil.GESTOR);
        when(usuarioService.alterarPerfil(USUARIO_PUBLIC_ID, Perfil.GESTOR))
                .thenReturn(new UsuarioResponseDTO(
                        USUARIO_PUBLIC_ID, "Maria Oliveira", "maria.oliveira@ufrb.edu.br",
                        Perfil.GESTOR, UNIDADE_PUBLIC_ID, "Instituto de Química", "IQ",
                        LABORATORIO_PUBLIC_ID, "Laboratório de Química Orgânica", true));

        mockMvc.perform(put(BASE_URL + "/{id}/perfil", USUARIO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("GESTOR"));
    }

    @Test
    void deveInativarUsuarioERetornar204() throws Exception {
        // O método do service chama-se "Inativar" (I maiúsculo) - bug de nomenclatura
        // pré-existente em UsuarioController.java linha 89, não corrigido aqui (não
        // altera src/main, só documentado).
        doNothing().when(usuarioService).Inativar(USUARIO_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", USUARIO_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(usuarioService).Inativar(USUARIO_PUBLIC_ID);
    }
}
