package com.sgl.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.RelatorioMovimentacoesService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioMovimentacoesController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * {@link RelatorioMovimentacoesResponseDTO} não tem construtor sem
 * argumentos (é {@code @AllArgsConstructor}), então é montado com o
 * construtor completo — sem precisar de entidades reais, já que o Service
 * está mockado.
 */
@WebMvcTest(RelatorioMovimentacoesController.class)
@Import(SecurityConfig.class)
class RelatorioMovimentacoesControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/movimentacoes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioMovimentacoesService relatorioMovimentacoesService;

    private RelatorioMovimentacoesResponseDTO montarRelatorio() {
        return new RelatorioMovimentacoesResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                10,
                6,
                3,
                1,
                0,
                0,
                List.of());
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioMovimentacoesService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovimentacoes").value(10))
                .andExpect(jsonPath("$.quantidadeEntradas").value(6));
    }

    @Test
    void deveRetornar400QuandoFiltroDePeriodoIncompleto() throws Exception {
        // Mensagem exata lançada por RelatorioMovimentacoesService.validarPeriodoOpcional()
        // quando apenas uma das datas (início/fim) é informada.
        LocalDate dataInicio = LocalDate.of(2026, 9, 1);
        when(relatorioMovimentacoesService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(dataInicio), isNull()))
                .thenThrow(new BusinessRuleException("Para filtrar por período, informe dataInicio e dataFim."));

        mockMvc.perform(get(BASE_URL).param("dataInicio", dataInicio.toString()))
                .andExpect(status().isBadRequest());
    }
}
