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
import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.RelatorioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (ver
 * controllers-context.md, seção "Atualização pós-Batch C1"):
 * {@code @MockitoBean} no lugar de {@code @MockBean}, {@code ObjectMapper}
 * próprio via {@code @TestConfiguration} e {@code @Import(SecurityConfig.class)}.
 *
 * {@link RelatorioEstagiariosResponseDTO} não tem construtor sem argumentos
 * (é {@code @AllArgsConstructor}), então é montado com o construtor completo
 * — sem precisar de entidades reais, já que o Service está mockado.
 *
 * A validação de negócio (período opcional, laboratório inexistente) já é
 * coberta em RelatorioServiceTest; aqui o foco é só roteamento HTTP e
 * o mapeamento de exceção para status code (BusinessRuleException -> 400).
 */
@WebMvcTest(RelatorioController.class)
@Import(SecurityConfig.class)
class RelatorioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/estagiarios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    private RelatorioEstagiariosResponseDTO montarRelatorio() {
        return new RelatorioEstagiariosResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                2,
                1,
                1,
                List.of());
    }

    @Test
    void deveGerarRelatorioDeEstagiariosERetornar200() throws Exception {
        when(relatorioService.gerarRelatorioEstagiarios(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.ativos").value(1))
                .andExpect(jsonPath("$.inativos").value(1));
    }

    @Test
    void deveRetornar400AoGerarComParametrosInvalidos() throws Exception {
        // Mensagem exata lançada por RelatorioService.validarPeriodoOpcional()
        // quando a data inicial é posterior à data final.
        LocalDate dataInicio = LocalDate.of(2026, 9, 30);
        LocalDate dataFim = LocalDate.of(2026, 9, 1);
        when(relatorioService.gerarRelatorioEstagiarios(isNull(), isNull(), eq(dataInicio), eq(dataFim)))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get(BASE_URL)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isBadRequest());
    }
}
