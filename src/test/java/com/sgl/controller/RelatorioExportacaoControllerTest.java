package com.sgl.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.service.RelatorioEstoqueLotesService;
import com.sgl.service.RelatorioExportacaoService;
import com.sgl.service.RelatorioFiscalizacaoService;
import com.sgl.service.RelatorioMovimentacoesService;
import com.sgl.service.RelatorioProdutosService;
import com.sgl.service.RelatorioResumoOperacionalService;
import com.sgl.service.RelatorioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link RelatorioExportacaoController}.
 *
 * <p>Segue o template corrigido para Spring Boot 4.1 documentado em
 * {@code controllers-context.md} (pacote novo de {@code @WebMvcTest},
 * {@code @MockitoBean} no lugar de {@code @MockBean}, {@code @Import(SecurityConfig.class)}
 * para reproduzir o {@code permitAll()} de produção em vez de mascarar com
 * {@code addFilters = false}).</p>
 *
 * <p>Este Controller tem 7 dependências (um Service "gerador" por relatório +
 * o {@link RelatorioExportacaoService}, que transforma o DTO do relatório em
 * bytes de PDF/XLSX). Cada endpoint chama os dois Services em sequência —
 * primeiro gera o relatório, depois exporta — então cada teste mocka os
 * dois. Os parâmetros de filtro (datas, UUIDs, booleans) não são enviados na
 * requisição de teste (só {@code formato}), então os Services "geradores"
 * recebem {@code null} em todos eles; usamos {@code any()} como matcher para
 * não depender da assinatura exata de cada overload.</p>
 *
 * <p>O método privado {@code filtros(...)} do controller delega para
 * {@code exportacaoService.filtros(...)}, que também está mockado — o valor
 * de retorno (lista vazia, resposta padrão do Mockito para coleções) não é
 * relevante para o teste, então o terceiro argumento dos stubs de
 * exportação também usa {@code any()}.</p>
 */
@WebMvcTest(RelatorioExportacaoController.class)
@Import(SecurityConfig.class)
class RelatorioExportacaoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/relatorios";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioService relatorioService;

    @MockitoBean
    private RelatorioProdutosService produtosService;

    @MockitoBean
    private RelatorioMovimentacoesService movimentacoesService;

    @MockitoBean
    private RelatorioResumoOperacionalService resumoService;

    @MockitoBean
    private RelatorioEstoqueLotesService estoqueLotesService;

    @MockitoBean
    private RelatorioFiscalizacaoService fiscalizacaoService;

    @MockitoBean
    private RelatorioExportacaoService exportacaoService;

    private ArquivoRelatorioDTO montarArquivo(String nomeArquivo) {
        byte[] conteudo = ("conteudo-simulado-" + nomeArquivo).getBytes(StandardCharsets.UTF_8);
        return new ArquivoRelatorioDTO(conteudo, nomeArquivo, "application/pdf");
    }

    @Test
    void deveExportarEstagiariosERetornarBytes() throws Exception {
        RelatorioEstagiariosResponseDTO relatorio =
                new RelatorioEstagiariosResponseDTO(LocalDateTime.now(), 0, 0, 0, List.of());
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-estagiarios-20260919.pdf");

        when(relatorioService.gerarRelatorioEstagiarios(any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarEstagiarios(any(), eq(FormatoExportacaoRelatorio.PDF), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/estagiarios/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("sgl-estagiarios-20260919.pdf")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }

    @Test
    void deveExportarProdutosERetornarBytes() throws Exception {
        RelatorioProdutosResponseDTO relatorio =
                new RelatorioProdutosResponseDTO(LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of());
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-produtos-20260919.pdf");

        when(produtosService.gerar(any(), any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarProdutos(any(), eq(FormatoExportacaoRelatorio.PDF), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/produtos/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("sgl-produtos-20260919.pdf")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }

    @Test
    void deveExportarMovimentacoesERetornarBytes() throws Exception {
        RelatorioMovimentacoesResponseDTO relatorio = new RelatorioMovimentacoesResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.<MovimentacaoEstoqueResponseDTO>of());
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-movimentacoes-20260919.xlsx");

        when(movimentacoesService.gerar(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarMovimentacoes(any(), eq(FormatoExportacaoRelatorio.XLSX), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/movimentacoes/exportar").param("formato", "XLSX"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sgl-movimentacoes-20260919.xlsx")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }

    @Test
    void deveExportarResumoOperacionalERetornarBytes() throws Exception {
        RelatorioResumoOperacionalResponseDTO relatorio = new RelatorioResumoOperacionalResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-resumo-operacional-20260919.pdf");

        when(resumoService.gerar(any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarResumoOperacional(any(), eq(FormatoExportacaoRelatorio.PDF), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/resumo-operacional/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sgl-resumo-operacional-20260919.pdf")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }

    @Test
    void deveExportarEstoqueLotesERetornarBytes() throws Exception {
        RelatorioEstoqueLotesResponseDTO relatorio = RelatorioEstoqueLotesResponseDTO.builder()
                .geradoEm(LocalDateTime.now())
                .totalEstoques(0)
                .estoquesAtivos(0)
                .estoquesAbaixoMinimo(0)
                .quantidadeTotalEstoque(0L)
                .totalLotes(0)
                .lotesAtivos(0)
                .lotesVencidos(0)
                .lotesProximosVencimento(0)
                .lotesEsgotados(0)
                .estoques(List.of())
                .lotes(List.of())
                .build();
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-estoque-lotes-20260919.pdf");

        when(estoqueLotesService.gerar(any(), any(), any(), any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarEstoqueLotes(any(), eq(FormatoExportacaoRelatorio.PDF), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/estoque-lotes/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sgl-estoque-lotes-20260919.pdf")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }

    @Test
    void deveExportarFiscalizacaoERetornarBytes() throws Exception {
        RelatorioFiscalizacaoResponseDTO relatorio = RelatorioFiscalizacaoResponseDTO.builder()
                .geradoEm(LocalDateTime.now())
                .totalProdutosFiscalizados(0)
                .saldoAtualTotal(0)
                .lotesAtivos(0)
                .lotesVencidos(0)
                .lotesProximosVencimento(0)
                .quantidadeEntradas(0)
                .quantidadeSaidas(0)
                .produtos(List.of())
                .movimentacoes(List.of())
                .build();
        ArquivoRelatorioDTO arquivo = montarArquivo("sgl-fiscalizacao-20260919.pdf");

        when(fiscalizacaoService.gerar(any(), any(), any(), any(), any(), any())).thenReturn(relatorio);
        when(exportacaoService.exportarFiscalizacao(any(), eq(FormatoExportacaoRelatorio.PDF), any()))
                .thenReturn(arquivo);

        mockMvc.perform(get(BASE_URL + "/fiscalizacao/exportar").param("formato", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("sgl-fiscalizacao-20260919.pdf")))
                .andExpect(content().bytes(arquivo.conteudo()));
    }
}
