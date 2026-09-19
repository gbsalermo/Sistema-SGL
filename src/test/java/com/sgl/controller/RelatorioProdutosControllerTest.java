package com.sgl.controller;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.service.RelatorioProdutosService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioProdutosController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * {@link RelatorioProdutosResponseDTO} não tem construtor sem argumentos (é
 * {@code @AllArgsConstructor}), então é montado com o construtor completo —
 * sem precisar de entidades reais, já que o Service está mockado.
 *
 * {@link RelatorioProdutosService#gerar} não lança nenhuma exceção de
 * negócio (só filtra a lista de produtos), então este Controller só precisa
 * do teste de caminho feliz — não há regra de validação para testar um
 * caso de erro 400/404, conforme o brief do batch C5.
 */
@WebMvcTest(RelatorioProdutosController.class)
@Import(SecurityConfig.class)
class RelatorioProdutosControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/produtos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioProdutosService relatorioProdutosService;

    private RelatorioProdutosResponseDTO montarRelatorio() {
        return new RelatorioProdutosResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                5,
                4,
                1,
                2,
                1,
                1,
                List.of());
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioProdutosService.gerar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.fiscalizados").value(2));
    }
}
