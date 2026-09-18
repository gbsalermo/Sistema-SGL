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
import com.sgl.dto.request.LaboratorioRequestDTO;
import com.sgl.dto.response.LaboratorioResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.service.LaboratorioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link LaboratorioController}. Segue o
 * mesmo padrão fixado em UnidadeControllerTest (ver notas de configuração lá): bean de
 * {@link ObjectMapper} declarado via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir a regra real de
 * "anyRequest().permitAll()" em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link LaboratorioService} é mockado - toda a regra de tenant/negócio (já coberta em
 * LaboratorioServiceTest) não é exercitada aqui, só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(LaboratorioController.class)
@Import(SecurityConfig.class)
class LaboratorioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/laboratorios";

    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LaboratorioService laboratorioService;

    private LaboratorioResponseDTO montarResponseDTO() {
        return new LaboratorioResponseDTO(
                LABORATORIO_PUBLIC_ID,
                UNIDADE_PUBLIC_ID,
                "Laboratório de Química Orgânica",
                "Descrição do laboratório",
                null,
                null,
                true);
    }

    @Test
    void deveListarTodosOsLaboratoriosERetornar200() throws Exception {
        when(laboratorioService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarLaboratorioPorIdERetornar200() throws Exception {
        when(laboratorioService.buscarPorId(LABORATORIO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", LABORATORIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Laboratório de Química Orgânica"));
    }

    @Test
    void deveRetornar404QuandoLaboratorioNaoEncontrado() throws Exception {
        when(laboratorioService.buscarPorId(LABORATORIO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Laboratório", LABORATORIO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", LABORATORIO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarLaboratoriosPorUnidadeERetornar200() throws Exception {
        when(laboratorioService.listarPorUnidade(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-unidade").param("unidadeId", UNIDADE_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].unidadeId").value(UNIDADE_PUBLIC_ID.toString()));
    }

    @Test
    void deveRetornar400QuandoParametroUnidadeIdAusente() throws Exception {
        // unidadeId é @RequestParam obrigatório (sem "required = false") - sem ele, o
        // MissingServletRequestParameterException do Spring vira 400 (ver RestExceptionHandler).
        mockMvc.perform(get(BASE_URL + "/por-unidade"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCriarLaboratorioERetornar201() throws Exception {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO(
                UNIDADE_PUBLIC_ID, "Laboratório de Química Orgânica", "Descrição do laboratório", null, true);
        when(laboratorioService.criar(any(LaboratorioRequestDTO.class))).thenReturn(montarResponseDTO());

        // LaboratorioController.criar usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (LaboratorioController.java linha 66) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Laboratório de Química Orgânica"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // LaboratorioRequestDTO exige unidadeId (@NotNull) e nome (@NotBlank) - corpo vazio viola ambos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarLaboratorioERetornar200() throws Exception {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO(
                UNIDADE_PUBLIC_ID, "Laboratório Atualizado", "Nova descrição", null, true);
        when(laboratorioService.atualizar(eq(LABORATORIO_PUBLIC_ID), any(LaboratorioRequestDTO.class)))
                .thenReturn(new LaboratorioResponseDTO(
                        LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID, "Laboratório Atualizado", "Nova descrição",
                        null, null, true));

        mockMvc.perform(put(BASE_URL + "/{id}", LABORATORIO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Laboratório Atualizado"));
    }

    @Test
    void deveDeletarLaboratorioERetornar204() throws Exception {
        doNothing().when(laboratorioService).deletar(LABORATORIO_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", LABORATORIO_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(laboratorioService).deletar(LABORATORIO_PUBLIC_ID);
    }
}
