package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.RelatorioEstoqueLotesService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioEstoqueLotesController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * {@link RelatorioEstoqueLotesResponseDTO} usa {@code @Builder}, então é
 * montado com o builder — sem precisar de entidades reais, já que o Service
 * está mockado.
 */
@WebMvcTest(RelatorioEstoqueLotesController.class)
@Import(SecurityConfig.class)
class RelatorioEstoqueLotesControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/estoque-lotes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioEstoqueLotesService relatorioEstoqueLotesService;

    private RelatorioEstoqueLotesResponseDTO montarRelatorio() {
        return RelatorioEstoqueLotesResponseDTO.builder()
                .geradoEm(LocalDateTime.of(2026, 9, 19, 10, 0))
                .totalEstoques(3)
                .estoquesAtivos(3)
                .estoquesAbaixoMinimo(1)
                .quantidadeTotalEstoque(150L)
                .totalLotes(4)
                .lotesAtivos(3)
                .lotesVencidos(1)
                .lotesProximosVencimento(0)
                .lotesEsgotados(0)
                .estoques(List.of())
                .lotes(List.of())
                .build();
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioEstoqueLotesService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Integer.class)))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEstoques").value(3))
                .andExpect(jsonPath("$.totalLotes").value(4));
    }

    @Test
    void deveRetornar400QuandoPeriodoDeVencimentoInvalido() throws Exception {
        // Mensagem exata lançada por RelatorioEstoqueLotesService.gerar()
        // quando diasVencimento está fora do intervalo [1, 365].
        when(relatorioEstoqueLotesService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(400)))
                .thenThrow(new BusinessRuleException("O período de vencimento deve estar entre 1 e 365 dias."));

        mockMvc.perform(get(BASE_URL).param("diasVencimento", "400"))
                .andExpect(status().isBadRequest());
    }
}
