package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.sgl.dto.request.CursoRequestDTO;
import com.sgl.dto.response.CursoResponseDTO;
import com.sgl.model.Curso;
import com.sgl.model.Unidade;
import com.sgl.service.CursoService;

@WebMvcTest(CursoController.class)
@Import(SecurityConfig.class)
class CursoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final UUID UNIDADE_ID =
            UUID.fromString("74000000-0000-0000-0000-000000000001");
    private static final UUID CURSO_ID =
            UUID.fromString("74000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CursoService service;

    private CursoResponseDTO montarResponse() {
        Unidade unidade = Unidade.builder()
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .build();

        Curso curso = Curso.builder()
                .publicId(CURSO_ID)
                .unidade(unidade)
                .nome("Engenharia Ambiental")
                .ativo(true)
                .build();

        return new CursoResponseDTO(curso);
    }

    @Test
    void deveCriarCursoERetornar201()
            throws Exception {

        CursoRequestDTO dto = new CursoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("Engenharia Ambiental");

        when(service.criar(any(CursoRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(post("/api/v1/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(CURSO_ID.toString()))
                .andExpect(jsonPath("$.nome")
                        .value("Engenharia Ambiental"));
    }

    @Test
    void deveListarCursosAtivosERetornar200()
            throws Exception {

        when(service.listarAtivos())
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get("/api/v1/cursos/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id")
                        .value(CURSO_ID.toString()));
    }

    @Test
    void deveRetornar400AoCriarCursoSemNome()
            throws Exception {

        CursoRequestDTO dto = new CursoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);

        mockMvc.perform(post("/api/v1/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
