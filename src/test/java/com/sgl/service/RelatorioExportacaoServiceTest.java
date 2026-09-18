package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;

/**
 * Testes de {@link RelatorioExportacaoService}.
 *
 * <p>Este service não tem nenhum {@code *Repository} para mockar — ele só
 * recebe um DTO de relatório já pronto e devolve os bytes de um PDF (iText/
 * OpenPDF) ou XLSX (Apache POI). Seguindo a exceção documentada no brief do
 * Batch S6 (Global Constraints do plano pedem mockar dependências, mas aqui
 * não há nenhuma dependência de repositório a mockar, e mockar a biblioteca
 * de geração de arquivo não agrega valor), os testes de caminho feliz são
 * "de integração leve": chamam o método real e conferem apenas que o array
 * de bytes não é vazio e que o {@link ArquivoRelatorioDTO} retornado tem o
 * nome/content-type esperado — não validamos o conteúdo binário do PDF/XLSX
 * byte a byte.</p>
 *
 * <p><b>Sobre os dois testes de exceção pedidos pelo brief</b>
 * ({@code deveLancarExcecaoQuandoFalhaNaGeracaoDoPdf} e
 * {@code ...DoXlsx}):</p>
 * <ul>
 *   <li>PDF: {@code gerarPdf()} só captura {@code IOException} ou
 *   {@code DocumentException}. A única forma de provocar uma dessas exceções
 *   através de uma chamada real (sem mockar o OpenPDF) é corromper a imagem
 *   do logo carregada do classpath ({@code carregarLogo()}), pois
 *   {@code Image.getInstance(byte[])} lança {@code IOException} para bytes
 *   que não correspondem a nenhum formato de imagem reconhecido. Como
 *   {@code logoCache} é um campo de instância (não estático), usamos
 *   reflection para injetar bytes inválidos apenas na instância de teste,
 *   sem alterar nenhum recurso em {@code src/main/resources} e sem afetar
 *   os demais testes desta classe (cada um usa uma instância nova do
 *   service). Isso conta como "avaliar se é viável forçar a falha sem mock
 *   pesado" — decidimos que sim, é viável, com essa técnica pontual.</li>
 *   <li>XLSX: {@code gerarXlsx()} só captura {@code IOException}. Todas as
 *   colunas das tabelas são fixas no código-fonte (nunca vêm vazias a partir
 *   do DTO de entrada), e a escrita do Apache POI num
 *   {@code ByteArrayOutputStream} em memória não lança {@code IOException}
 *   em nenhum cenário alcançável a partir da API pública sem mockar
 *   {@code Workbook}/{@code XSSFWorkbook} diretamente (classes concretas,
 *   instanciadas internamente com {@code new XSSFWorkbook()} — não são
 *   injetadas, então não há como substituí-las por um mock sem alterar
 *   {@code src/main}, o que as regras deste plano proíbem). Por isso,
 *   decidimos <b>não escrever</b> este teste específico e documentar aqui e
 *   no relatório do batch por que ele não é viável sem mock pesado.</li>
 * </ul>
 */
class RelatorioExportacaoServiceTest {

    private final RelatorioExportacaoService service = new RelatorioExportacaoService();

    @Test
    void deveExportarEstagiariosEmPdf() {
        RelatorioEstagiariosResponseDTO relatorio =
                new RelatorioEstagiariosResponseDTO(LocalDateTime.now(), 0, 0, 0, List.of());

        ArquivoRelatorioDTO arquivo = service.exportarEstagiarios(
                relatorio, FormatoExportacaoRelatorio.PDF, List.of("Unidade: CNPMF"));

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
        assertEquals("application/pdf", arquivo.contentType());
    }

    @Test
    void deveExportarEstagiariosEmXlsx() {
        RelatorioEstagiariosResponseDTO relatorio =
                new RelatorioEstagiariosResponseDTO(LocalDateTime.now(), 0, 0, 0, List.of());

        ArquivoRelatorioDTO arquivo = service.exportarEstagiarios(
                relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", arquivo.contentType());
    }

    @Test
    void deveExportarProdutosEmPdf() {
        RelatorioProdutosResponseDTO relatorio =
                new RelatorioProdutosResponseDTO(LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of());

        ArquivoRelatorioDTO arquivo = service.exportarProdutos(relatorio, FormatoExportacaoRelatorio.PDF, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
    }

    @Test
    void deveExportarProdutosEmXlsx() {
        RelatorioProdutosResponseDTO relatorio =
                new RelatorioProdutosResponseDTO(LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of());

        ArquivoRelatorioDTO arquivo = service.exportarProdutos(relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
    }

    @Test
    void deveExportarMovimentacoesEmPdf() {
        RelatorioMovimentacoesResponseDTO relatorio = new RelatorioMovimentacoesResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.<MovimentacaoEstoqueResponseDTO>of());

        ArquivoRelatorioDTO arquivo = service.exportarMovimentacoes(relatorio, FormatoExportacaoRelatorio.PDF, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
    }

    @Test
    void deveExportarMovimentacoesEmXlsx() {
        RelatorioMovimentacoesResponseDTO relatorio = new RelatorioMovimentacoesResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.<MovimentacaoEstoqueResponseDTO>of());

        ArquivoRelatorioDTO arquivo = service.exportarMovimentacoes(relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
    }

    @Test
    void deveExportarResumoOperacionalEmPdf() {
        RelatorioResumoOperacionalResponseDTO relatorio = new RelatorioResumoOperacionalResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());

        ArquivoRelatorioDTO arquivo = service.exportarResumoOperacional(relatorio, FormatoExportacaoRelatorio.PDF, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
    }

    @Test
    void deveExportarResumoOperacionalEmXlsx() {
        RelatorioResumoOperacionalResponseDTO relatorio = new RelatorioResumoOperacionalResponseDTO(
                LocalDateTime.now(), 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());

        ArquivoRelatorioDTO arquivo = service.exportarResumoOperacional(relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
    }

    @Test
    void deveExportarEstoqueLotesEmPdf() {
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

        ArquivoRelatorioDTO arquivo = service.exportarEstoqueLotes(relatorio, FormatoExportacaoRelatorio.PDF, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
    }

    @Test
    void deveExportarEstoqueLotesEmXlsx() {
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

        ArquivoRelatorioDTO arquivo = service.exportarEstoqueLotes(relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
    }

    @Test
    void deveExportarFiscalizacaoEmPdf() {
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

        ArquivoRelatorioDTO arquivo = service.exportarFiscalizacao(relatorio, FormatoExportacaoRelatorio.PDF, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
    }

    @Test
    void deveExportarFiscalizacaoEmXlsx() {
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

        ArquivoRelatorioDTO arquivo = service.exportarFiscalizacao(relatorio, FormatoExportacaoRelatorio.XLSX, List.of());

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
    }

    @Test
    void deveLancarExcecaoQuandoFalhaNaGeracaoDoPdf() throws Exception {
        // Instância isolada só para este teste: corrompemos o cache interno
        // do logo (campo de instância "logoCache") para forçar
        // Image.getInstance() a lançar IOException ao tentar interpretar
        // bytes que não correspondem a nenhum formato de imagem conhecido.
        // Nenhum outro teste desta classe é afetado, pois cada um usa sua
        // própria instância de RelatorioExportacaoService.
        RelatorioExportacaoService servicoComLogoCorrompido = new RelatorioExportacaoService();
        Field logoCacheField = RelatorioExportacaoService.class.getDeclaredField("logoCache");
        logoCacheField.setAccessible(true);
        logoCacheField.set(servicoComLogoCorrompido, new byte[]{1, 2, 3, 4, 5});

        RelatorioEstagiariosResponseDTO relatorio =
                new RelatorioEstagiariosResponseDTO(LocalDateTime.now(), 0, 0, 0, List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> servicoComLogoCorrompido.exportarEstagiarios(
                        relatorio, FormatoExportacaoRelatorio.PDF, List.of()));

        assertEquals("Não foi possível gerar o PDF do relatório.", ex.getMessage());
    }

    // deveLancarExcecaoQuandoFalhaNaGeracaoDoXlsx: não escrito.
    // Ver justificativa detalhada no Javadoc da classe, acima.
}
