package com.sgl.controller;

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
import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.RelatorioResumoOperacionalService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioResumoOperacionalController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * {@link RelatorioResumoOperacionalResponseDTO} não tem construtor sem
 * argumentos (é {@code @AllArgsConstructor}), então é montado com o
 * construtor completo — sem precisar de entidades reais, já que o Service
 * está mockado.
 */
@WebMvcTest(RelatorioResumoOperacionalController.class)
@Import(SecurityConfig.class)
class RelatorioResumoOperacionalControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/resumo-operacional";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioResumoOperacionalService relatorioResumoOperacionalService;

    private RelatorioResumoOperacionalResponseDTO montarRelatorio() {
        return new RelatorioResumoOperacionalResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                20,
                12,
                8,
                0,
                3,
                4,
                List.of(),
                List.of(),
                List.of());
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioResumoOperacionalService.gerar(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovimentacoes").value(20))
                .andExpect(jsonPath("$.produtosMovimentados").value(3));
    }

    @Test
    void deveRetornar400QuandoLimiteDeRankingInvalido() throws Exception {
        // Mensagem exata lançada por RelatorioResumoOperacionalService.validarLimite()
        // quando o limite está fora do intervalo [1, 50].
        when(relatorioResumoOperacionalService.gerar(isNull(), isNull(), isNull(), eq(100)))
                .thenThrow(new BusinessRuleException("O limite do ranking deve estar entre 1 e 50."));

        mockMvc.perform(get(BASE_URL).param("limite", "100"))
                .andExpect(status().isBadRequest());
    }
}
