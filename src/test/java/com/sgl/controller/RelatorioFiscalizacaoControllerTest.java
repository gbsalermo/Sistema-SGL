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
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.RelatorioFiscalizacaoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioFiscalizacaoController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * {@link RelatorioFiscalizacaoResponseDTO} usa {@code @Builder}, então é
 * montado com o builder — sem precisar de entidades reais, já que o Service
 * está mockado.
 */
@WebMvcTest(RelatorioFiscalizacaoController.class)
@Import(SecurityConfig.class)
class RelatorioFiscalizacaoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/fiscalizacao";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioFiscalizacaoService relatorioFiscalizacaoService;

    private RelatorioFiscalizacaoResponseDTO montarRelatorio() {
        return RelatorioFiscalizacaoResponseDTO.builder()
                .geradoEm(LocalDateTime.of(2026, 9, 19, 10, 0))
                .totalProdutosFiscalizados(2)
                .saldoAtualTotal(100)
                .lotesAtivos(2)
                .lotesVencidos(0)
                .lotesProximosVencimento(1)
                .quantidadeEntradas(50)
                .quantidadeSaidas(20)
                .produtos(List.of())
                .movimentacoes(List.of())
                .build();
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioFiscalizacaoService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Integer.class)))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProdutosFiscalizados").value(2))
                .andExpect(jsonPath("$.saldoAtualTotal").value(100));
    }

    @Test
    void deveRetornar400QuandoJanelaDeVencimentoInvalida() throws Exception {
        // Mensagem exata lançada por RelatorioFiscalizacaoService.gerar()
        // quando diasVencimento está fora do intervalo [1, 365].
        when(relatorioFiscalizacaoService.gerar(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(0)))
                .thenThrow(new BusinessRuleException("A janela de vencimento deve estar entre 1 e 365 dias."));

        mockMvc.perform(get(BASE_URL).param("diasVencimento", "0"))
                .andExpect(status().isBadRequest());
    }
}
