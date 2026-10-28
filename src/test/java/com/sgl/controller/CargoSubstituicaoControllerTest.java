package com.sgl.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.response.CargoSubstituicaoDTO;
import com.sgl.dto.response.CargoSubstituicaoDTO.TipoCargo;
import com.sgl.service.CargoSubstituicaoService;

/**
 * Fatia web de {@link CargoSubstituicaoController}: roteamento, JSON e o 403 quando o service
 * recusa. A regra de quem pode ver (gestão) e o cálculo da lista estão em CargoSubstituicaoServiceTest.
 */
@WebMvcTest(CargoSubstituicaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class CargoSubstituicaoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CargoSubstituicaoService service;

    @Test
    void listaCargos200() throws Exception {
        when(service.listar()).thenReturn(List.of(new CargoSubstituicaoDTO(
                TipoCargo.ORIENTADOR_ESTAGIO, UUID.randomUUID(), "Orientador de Ana", UUID.randomUUID(), "Fulano")));

        mockMvc.perform(get("/api/v1/cargos-a-substituir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ORIENTADOR_ESTAGIO"))
                .andExpect(jsonPath("$[0].ocupanteNome").value("Fulano"));
    }

    @Test
    void foraDaGestaoRecebe403() throws Exception {
        when(service.listar()).thenThrow(new AccessDeniedException("Apenas gestão pode ver os cargos a substituir."));

        mockMvc.perform(get("/api/v1/cargos-a-substituir"))
                .andExpect(status().isForbidden());
    }
}
