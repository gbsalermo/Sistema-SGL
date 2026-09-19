package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.service.RelatorioResiduosExportacaoService;
import com.sgl.service.RelatorioResiduosService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioResiduosController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (controllers-context.md).
 *
 * Este Controller tem duas dependências ({@link RelatorioResiduosService} e
 * {@link RelatorioResiduosExportacaoService}), então ambas viram
 * {@code @MockitoBean}. O endpoint {@code /exportar} chama os dois serviços
 * em sequência (gera o relatório e depois exporta para bytes), então o
 * teste de exportação mocka os dois.
 *
 * {@link RelatorioResiduosResponseDTO} não tem construtor sem argumentos (é
 * {@code @AllArgsConstructor}), então é montado com o construtor completo —
 * sem precisar de entidades reais, já que o Service está mockado.
 */
@WebMvcTest(RelatorioResiduosController.class)
@Import(SecurityConfig.class)
class RelatorioResiduosControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/residuos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioResiduosService relatorioResiduosService;

    @MockitoBean
    private RelatorioResiduosExportacaoService relatorioResiduosExportacaoService;

    private RelatorioResiduosResponseDTO montarRelatorio() {
        return new RelatorioResiduosResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                4,
                2,
                1,
                1,
                0,
                0,
                1,
                List.of());
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioResiduosService.gerar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.altoRisco").value(1));
    }

    @Test
    void deveRetornar400QuandoDataInicialPosteriorAFinal() throws Exception {
        // Mensagem exata lançada por RelatorioResiduosService.gerar() quando
        // a data inicial é posterior à data final.
        LocalDate dataInicio = LocalDate.of(2026, 9, 30);
        LocalDate dataFim = LocalDate.of(2026, 9, 1);
        when(relatorioResiduosService.gerar(isNull(), isNull(), isNull(), eq(dataInicio), eq(dataFim)))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final"));

        mockMvc.perform(get(BASE_URL)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveExportarRelatorioERetornarBytesComContentTypeCorreto() throws Exception {
        byte[] conteudo = "conteudo-pdf-simulado".getBytes(StandardCharsets.UTF_8);
        ArquivoRelatorioDTO arquivo = new ArquivoRelatorioDTO(conteudo, "sgl-residuos-20260919.pdf", "application/pdf");

        when(relatorioResiduosService.gerar(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(montarRelatorio());
        when(relatorioResiduosExportacaoService.exportar(any(RelatorioResiduosResponseDTO.class),
                eq(FormatoExportacaoRelatorio.PDF)))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("sgl-residuos-20260919.pdf")))
                .andExpect(content().bytes(conteudo));
    }
}
