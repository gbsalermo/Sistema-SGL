package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;

/**
 * Testes de {@link RelatorioResiduosExportacaoService}.
 *
 * <p>Assim como o {@code RelatorioExportacaoService}, este service não tem
 * nenhum {@code *Repository} para mockar — só recebe um DTO já pronto e
 * gera PDF/XLSX de verdade (OpenPDF/Apache POI). Seguindo a exceção
 * documentada no brief do Batch S6, os testes de caminho feliz são "de
 * integração leve": conferem só que o array de bytes não é vazio, sem
 * inspecionar o conteúdo binário.</p>
 *
 * <p><b>Testes de exceção:</b> diferente do {@code RelatorioExportacaoService},
 * aqui {@code gerarPdf()}/{@code gerarXlsx()} capturam {@code Exception}
 * genérico (não só {@code IOException}/{@code DocumentException}). Isso
 * torna possível forçar a falha de um jeito simples e realista, sem
 * reflection nem mock: basta um DTO com {@code itens == null}. O laço
 * {@code for (ResiduoResponseDTO item : relatorio.getItens())} dentro do
 * bloco {@code try} lança {@code NullPointerException} ao iterar sobre
 * {@code null}, que é capturada pelo {@code catch (Exception e)} e
 * relançada como {@code IllegalStateException} com a mensagem exata do
 * fonte — exercitando o catch real, sem mockar nenhuma biblioteca.</p>
 */
class RelatorioResiduosExportacaoServiceTest {

    private final RelatorioResiduosExportacaoService service = new RelatorioResiduosExportacaoService();

    @Test
    void deveExportarResiduosEmPdf() {
        RelatorioResiduosResponseDTO relatorio = relatorioValido(List.of());

        ArquivoRelatorioDTO arquivo = service.exportar(relatorio, FormatoExportacaoRelatorio.PDF);

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
        assertEquals("application/pdf", arquivo.contentType());
    }

    @Test
    void deveExportarResiduosEmXlsx() {
        RelatorioResiduosResponseDTO relatorio = relatorioValido(List.of());

        ArquivoRelatorioDTO arquivo = service.exportar(relatorio, FormatoExportacaoRelatorio.XLSX);

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", arquivo.contentType());
    }

    @Test
    void deveLancarExcecaoQuandoFalhaNaGeracaoDoPdf() {
        // itens == null força NullPointerException dentro do laço de
        // montagem da tabela do PDF, capturada pelo catch (Exception e)
        // genérico do gerarPdf() real.
        RelatorioResiduosResponseDTO relatorio = relatorioValido(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.exportar(relatorio, FormatoExportacaoRelatorio.PDF));

        assertEquals("Não foi possível gerar o PDF do relatório de resíduos.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoFalhaNaGeracaoDoXlsx() {
        RelatorioResiduosResponseDTO relatorio = relatorioValido(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.exportar(relatorio, FormatoExportacaoRelatorio.XLSX));

        assertEquals("Não foi possível gerar o XLSX do relatório de resíduos.", ex.getMessage());
    }

    private RelatorioResiduosResponseDTO relatorioValido(List<ResiduoResponseDTO> itens) {
        return new RelatorioResiduosResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, 0, itens);
    }
}
