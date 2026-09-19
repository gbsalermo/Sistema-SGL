package com.sgl.controller;

import static org.hamcrest.Matchers.containsString;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.service.RelatorioPessoasLaboratorioExportacaoService;
import com.sgl.service.RelatorioPessoasLaboratorioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioPessoasLaboratorioController}.
 *
 * <p>Segue o template corrigido para Spring Boot 4.1 (controllers-context.md):
 * pacote novo de {@code @WebMvcTest}, {@code @MockitoBean} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o {@code permitAll()}
 * de produção.</p>
 *
 * <p>Este Controller tem duas dependências: {@link RelatorioPessoasLaboratorioService}
 * (gera o DTO consolidado) e {@link RelatorioPessoasLaboratorioExportacaoService}
 * (converte o DTO em bytes de PDF/XLSX). O endpoint {@code /exportar} chama
 * os dois em sequência — assim como em {@code RelatorioResiduosController} e
 * {@code RelatorioExportacaoController} — então o teste de exportação mocka
 * ambos.</p>
 *
 * <p>{@link RelatorioPessoasLaboratorioResponseDTO} é {@code @Data} com
 * {@code @NoArgsConstructor}/{@code @AllArgsConstructor}, então é montado com
 * o construtor completo — sem precisar de entidades reais, já que o Service
 * está mockado.</p>
 */
@WebMvcTest(RelatorioPessoasLaboratorioController.class)
@Import(SecurityConfig.class)
class RelatorioPessoasLaboratorioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios/pessoas-laboratorio";
    private static final UUID LABORATORIO_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioPessoasLaboratorioService relatorioService;

    @MockitoBean
    private RelatorioPessoasLaboratorioExportacaoService exportacaoService;

    private RelatorioPessoasLaboratorioResponseDTO montarRelatorio() {
        return new RelatorioPessoasLaboratorioResponseDTO(
                LocalDateTime.of(2026, 9, 19, 10, 0),
                LABORATORIO_ID,
                "Laboratório de Microbiologia",
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "CNPMF",
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "Fulano de Tal",
                "fulano@sgl.gov.br",
                3,
                2,
                1,
                Map.of(),
                List.of());
    }

    @Test
    void deveGerarRelatorioERetornar200() throws Exception {
        when(relatorioService.gerar(eq(LABORATORIO_ID), isNull(), isNull())).thenReturn(montarRelatorio());

        mockMvc.perform(get(BASE_URL).param("laboratorioId", LABORATORIO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPessoas").value(3))
                .andExpect(jsonPath("$.laboratorioNome").value("Laboratório de Microbiologia"));
    }

    @Test
    void deveExportarRelatorioERetornarBytes() throws Exception {
        RelatorioPessoasLaboratorioResponseDTO relatorio = montarRelatorio();
        byte[] conteudo = "conteudo-pdf-simulado".getBytes(StandardCharsets.UTF_8);
        ArquivoRelatorioDTO arquivo = new ArquivoRelatorioDTO(conteudo, "sgl-pessoas-laboratorio-20260919.pdf", "application/pdf");

        when(relatorioService.gerar(eq(LABORATORIO_ID), isNull(), isNull())).thenReturn(relatorio);
        when(exportacaoService.exportar(any(RelatorioPessoasLaboratorioResponseDTO.class), eq(FormatoExportacaoRelatorio.PDF)))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/exportar")
                        .param("formato", "PDF")
                        .param("laboratorioId", LABORATORIO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("sgl-pessoas-laboratorio-20260919.pdf")))
                .andExpect(content().bytes(conteudo));
    }
}
