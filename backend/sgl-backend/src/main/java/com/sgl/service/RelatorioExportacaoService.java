package com.sgl.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;

@Service
public class RelatorioExportacaoService {

    private static final String MIME_PDF = "application/pdf";
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter ARQUIVO_DATA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color AZUL_SGL = new Color(26, 77, 161);
    private static final Color AZUL_ESCURO = new Color(13, 43, 94);
    private static final Color CINZA_CLARO = new Color(245, 247, 250);
    private static final Color CINZA_TEXTO = new Color(70, 84, 105);

    private byte[] logoCache;

    public ArquivoRelatorioDTO exportarEstagiarios(
            RelatorioEstagiariosResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Total", relatorio.getTotal(),
                "Ativos", relatorio.getAtivos(),
                "Inativos", relatorio.getInativos()
        );
        Tabela tabela = new Tabela("Estagiários",
                List.of("Estagiário", "E-mail", "Laboratório", "Unidade", "Bolsa", "Início", "Fim", "Situação"),
                relatorio.getItens().stream().map(item -> List.of(
                        texto(item.getNome()), texto(item.getEmail()), texto(item.getLaboratorioNome()), texto(item.getUnidadeNome()),
                        rotulo(item.getTipoBolsa()), data(item.getDataInicioEstagio()), data(item.getDataFimEstagio()),
                        Boolean.TRUE.equals(item.getAtivo()) ? "Ativo" : "Inativo"
                )).toList());
        return gerar("Relatório de Estagiários", "estagiarios", formato, filtros, resumo, List.of(tabela), true);
    }

    public ArquivoRelatorioDTO exportarProdutos(
            RelatorioProdutosResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Produtos", relatorio.getTotal(), "Ativos", relatorio.getAtivos(), "Inativos", relatorio.getInativos(),
                "Fiscalizados", relatorio.getFiscalizados(), "Perecíveis", relatorio.getPereciveis(), "Com risco", relatorio.getComRisco()
        );
        Tabela tabela = new Tabela("Produtos",
                List.of("Produto", "Código", "Unidade", "Risco", "Perecível", "Órgãos fiscalizadores", "Situação"),
                relatorio.getItens().stream().map(item -> List.of(
                        texto(item.getNome()), texto(item.getCodigoReferencia()), rotulo(item.getUnidadeMedida()), rotulo(item.getRisco()),
                        Boolean.TRUE.equals(item.getPerecivel()) ? "Sim" : "Não",
                        item.getOrgaosFiscalizadores() == null || item.getOrgaosFiscalizadores().isEmpty() ? "—" : item.getOrgaosFiscalizadores().stream().map(this::rotulo).sorted().collect(Collectors.joining(", ")),
                        Boolean.TRUE.equals(item.getAtivo()) ? "Ativo" : "Inativo"
                )).toList());
        return gerar("Relatório de Produtos", "produtos", formato, filtros, resumo, List.of(tabela), true);
    }

    public ArquivoRelatorioDTO exportarMovimentacoes(
            RelatorioMovimentacoesResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Movimentações", relatorio.getTotalMovimentacoes(), "Entradas", relatorio.getQuantidadeEntradas(),
                "Saídas", relatorio.getQuantidadeSaidas(), "Devoluções", relatorio.getQuantidadeDevolucoes(),
                "Descartes", relatorio.getQuantidadeDescartes(), "Ajustes", relatorio.getQuantidadeAjustes()
        );
        Tabela tabela = new Tabela("Movimentações",
                List.of("Data", "Produto", "Tipo", "Quantidade", "Lote", "Laboratório", "Origem", "Responsável", "Solicitante", "Saldo"),
                relatorio.getItens().stream().map(item -> List.of(
                        dataHora(item.getDataMovimentacao()), texto(item.getProdutoNome()), rotulo(item.getTipoMovimentacao()),
                        texto(item.getQuantidadeMovimentada()), texto(item.getCodigoInternoLote()), texto(item.getLaboratorioNome()),
                        rotulo(item.getOrigem()), texto(item.getUsuarioNome()), texto(item.getPedidoSolicitanteNome()),
                        texto(item.getQuantidadeAnterior()) + " → " + texto(item.getQuantidadeAtual())
                )).toList());
        return gerar("Relatório de Movimentações", "movimentacoes", formato, filtros, resumo, List.of(tabela), true);
    }

    public ArquivoRelatorioDTO exportarResumoOperacional(
            RelatorioResumoOperacionalResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Movimentações", relatorio.getTotalMovimentacoes(), "Entradas", relatorio.getQuantidadeEntradas(),
                "Saídas", relatorio.getQuantidadeSaidas(), "Descartes", relatorio.getQuantidadeDescartes(),
                "Produtos", relatorio.getProdutosMovimentados(), "Lotes", relatorio.getLotesMovimentados()
        );
        Tabela entradas = new Tabela("Principais entradas", List.of("Produto", "Quantidade", "Movimentações"),
                relatorio.getPrincipaisEntradas().stream().map(i -> List.of(texto(i.getProdutoNome()), texto(i.getQuantidade()), texto(i.getMovimentacoes()))).toList());
        Tabela saidas = new Tabela("Principais saídas", List.of("Produto", "Quantidade", "Movimentações"),
                relatorio.getPrincipaisSaidas().stream().map(i -> List.of(texto(i.getProdutoNome()), texto(i.getQuantidade()), texto(i.getMovimentacoes()))).toList());
        Tabela lotes = new Tabela("Lotes mais movimentados",
                List.of("Lote", "Fornecedor", "Produto", "Movimentado", "Entradas", "Saídas", "Saldo", "Validade"),
                relatorio.getLotesMaisMovimentados().stream().map(i -> List.of(
                        texto(i.getCodigoInterno()), texto(i.getNumeroLote()), texto(i.getProdutoNome()), texto(i.getQuantidadeMovimentada()),
                        texto(i.getQuantidadeEntradas()), texto(i.getQuantidadeSaidas()), texto(i.getSaldoAtual()), data(i.getDataValidade())
                )).toList());
        return gerar("Resumo Operacional", "resumo-operacional", formato, filtros, resumo, List.of(entradas, saidas, lotes), true);
    }

    public ArquivoRelatorioDTO exportarEstoqueLotes(
            RelatorioEstoqueLotesResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Estoques", relatorio.getTotalEstoques(), "Abaixo do mínimo", relatorio.getEstoquesAbaixoMinimo(),
                "Saldo total", relatorio.getQuantidadeTotalEstoque(), "Lotes ativos", relatorio.getLotesAtivos(),
                "Vencidos", relatorio.getLotesVencidos(), "Próx. vencimento", relatorio.getLotesProximosVencimento()
        );
        Tabela estoques = new Tabela("Posição de estoque",
                List.of("Produto", "Unidade", "Saldo", "Mínimo", "Nível", "Lotes ativos", "Vencidos", "Próx. vencimento"),
                relatorio.getEstoques().stream().map(i -> List.of(
                        texto(i.getProdutoNome()), texto(i.getUnidadeSigla() != null ? i.getUnidadeSigla() : i.getUnidadeNome()),
                        texto(i.getQuantidadeAtual()), texto(i.getQuantidadeMinima()), Boolean.TRUE.equals(i.getAbaixoMinimo()) ? "Abaixo do mínimo" : "Normal",
                        texto(i.getLotesAtivos()), texto(i.getLotesVencidos()), texto(i.getLotesProximosVencimento())
                )).toList());
        Tabela lotes = new Tabela("Lotes",
                List.of("Lote", "Fornecedor", "Produto", "Unidade", "Entrada", "Inicial", "Disponível", "Validade", "Situação"),
                relatorio.getLotes().stream().map(i -> List.of(
                        texto(i.getCodigoInterno()), texto(i.getNumeroLote()), texto(i.getProdutoNome()), texto(i.getUnidadeNome()),
                        data(i.getDataEntrada()), texto(i.getQuantidadeInicial()), texto(i.getQuantidadeDisponivel()), data(i.getDataValidade()), rotulo(i.getSituacao())
                )).toList());
        return gerar("Relatório de Estoque e Lotes", "estoque-lotes", formato, filtros, resumo, List.of(estoques, lotes), true);
    }

    public ArquivoRelatorioDTO exportarFiscalizacao(
            RelatorioFiscalizacaoResponseDTO relatorio,
            FormatoExportacaoRelatorio formato,
            List<String> filtros) {

        Map<String, String> resumo = resumo(
                "Produtos controlados", relatorio.getTotalProdutosFiscalizados(), "Saldo atual", relatorio.getSaldoAtualTotal(),
                "Lotes ativos", relatorio.getLotesAtivos(), "Vencidos", relatorio.getLotesVencidos(),
                "Próx. vencimento", relatorio.getLotesProximosVencimento(), "Entradas", relatorio.getQuantidadeEntradas(), "Saídas", relatorio.getQuantidadeSaidas()
        );
        Tabela produtos = new Tabela("Produtos controlados",
                List.of("Produto", "Código", "Órgãos", "Saldo", "Lotes ativos", "Vencidos", "Próx. vencimento", "Validade mais próxima", "Entradas", "Saídas"),
                relatorio.getProdutos().stream().map(i -> List.of(
                        texto(i.getProdutoNome()), texto(i.getCodigoReferencia()),
                        i.getOrgaosFiscalizadores() == null ? "—" : i.getOrgaosFiscalizadores().stream().map(this::rotulo).sorted().collect(Collectors.joining(", ")),
                        texto(i.getSaldoAtual()), texto(i.getLotesAtivos()), texto(i.getLotesVencidos()), texto(i.getLotesProximosVencimento()),
                        data(i.getProximoVencimento()), texto(i.getQuantidadeEntradas()), texto(i.getQuantidadeSaidas())
                )).toList());
        Tabela movimentos = new Tabela("Rastreabilidade",
                List.of("Data", "Produto", "Tipo", "Qtd", "Lote", "Validade", "Laboratório", "Projeto", "Solicitante", "Pedido", "Responsável", "Saldo"),
                relatorio.getMovimentacoes().stream().map(i -> List.of(
                        dataHora(i.getDataMovimentacao()), texto(i.getProdutoNome()), rotulo(i.getTipoMovimentacao()), texto(i.getQuantidadeMovimentada()),
                        texto(i.getCodigoInternoLote()), data(i.getDataValidadeLote()), texto(i.getLaboratorioNome()), texto(i.getProjetoNome()),
                        texto(i.getSolicitanteNome()), texto(i.getPedidoId()), texto(i.getResponsavelNome()), texto(i.getSaldoAposMovimentacao())
                )).toList());
        return gerar("Relatório de Fiscalização", "fiscalizacao", formato, filtros, resumo, List.of(produtos, movimentos), true);
    }

    private ArquivoRelatorioDTO gerar(
            String titulo,
            String slug,
            FormatoExportacaoRelatorio formato,
            List<String> filtros,
            Map<String, String> resumo,
            List<Tabela> tabelas,
            boolean paisagemQuandoLargo) {

        String sufixo = LocalDateTime.now().format(ARQUIVO_DATA);
        if (formato == FormatoExportacaoRelatorio.PDF) {
            byte[] bytes = gerarPdf(titulo, filtros, resumo, tabelas, paisagemQuandoLargo);
            return new ArquivoRelatorioDTO(bytes, "sgl-" + slug + "-" + sufixo + ".pdf", MIME_PDF);
        }
        byte[] bytes = gerarXlsx(titulo, filtros, resumo, tabelas);
        return new ArquivoRelatorioDTO(bytes, "sgl-" + slug + "-" + sufixo + ".xlsx", MIME_XLSX);
    }

    private byte[] gerarPdf(String titulo, List<String> filtros, Map<String, String> resumo, List<Tabela> tabelas, boolean paisagemQuandoLargo) {
        boolean paisagem = paisagemQuandoLargo && tabelas.stream().anyMatch(t -> t.colunas().size() > 6);
        Rectangle pagina = paisagem ? PageSize.A4.rotate() : PageSize.A4;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(pagina, 28, 28, 36, 32);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new RodapePagina());
            document.open();
            adicionarCabecalhoPdf(document, titulo, filtros);
            adicionarResumoPdf(document, resumo);
            for (Tabela tabela : tabelas) {
                adicionarTabelaPdf(document, tabela, paisagem);
            }
            document.close();
            return out.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do relatório.", e);
        }
    }

    private void adicionarCabecalhoPdf(Document document, String titulo, List<String> filtros) throws DocumentException, IOException {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 6.8f});
        header.setWidthPercentage(100);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        byte[] logo = carregarLogo();
        if (logo.length > 0) {
            Image image = Image.getInstance(logo);
            image.scaleToFit(74, 46);
            logoCell.addElement(image);
        }
        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.addElement(new Paragraph(titulo, fontePdf(15, true, AZUL_ESCURO)));
        titleCell.addElement(new Paragraph("SGL — Sistema de Gestão de Laboratórios", fontePdf(8, false, CINZA_TEXTO)));
        titleCell.addElement(new Paragraph("Gerado em " + LocalDateTime.now().format(DATA_HORA), fontePdf(7, false, CINZA_TEXTO)));
        header.addCell(titleCell);
        document.add(header);

        if (filtros != null && !filtros.isEmpty()) {
            Paragraph filtrosP = new Paragraph("Filtros: " + String.join(" • ", filtros), fontePdf(7, false, CINZA_TEXTO));
            filtrosP.setSpacingBefore(5);
            filtrosP.setSpacingAfter(8);
            document.add(filtrosP);
        } else {
            document.add(new Paragraph(" "));
        }
    }

    private void adicionarResumoPdf(Document document, Map<String, String> resumo) throws DocumentException {
        if (resumo.isEmpty()) return;
        int colunas = Math.min(4, resumo.size());
        PdfPTable tabela = new PdfPTable(colunas);
        tabela.setWidthPercentage(100);
        tabela.setSpacingAfter(12);
        for (Map.Entry<String, String> item : resumo.entrySet()) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(CINZA_CLARO);
            cell.setBorderColor(new Color(221, 228, 237));
            cell.setPadding(7);
            cell.addElement(new Paragraph(item.getKey(), fontePdf(7, false, CINZA_TEXTO)));
            cell.addElement(new Paragraph(item.getValue(), fontePdf(11, true, AZUL_ESCURO)));
            tabela.addCell(cell);
        }
        int resto = resumo.size() % colunas;
        if (resto != 0) {
            for (int i = resto; i < colunas; i++) {
                PdfPCell vazio = new PdfPCell(new Phrase(""));
                vazio.setBorder(Rectangle.NO_BORDER);
                tabela.addCell(vazio);
            }
        }
        document.add(tabela);
    }

    private void adicionarTabelaPdf(Document document, Tabela tabela, boolean paisagem) throws DocumentException {
        Paragraph secao = new Paragraph(tabela.nome(), fontePdf(10, true, AZUL_ESCURO));
        secao.setSpacingBefore(6);
        secao.setSpacingAfter(5);
        document.add(secao);

        PdfPTable pdf = new PdfPTable(tabela.colunas().size());
        pdf.setWidthPercentage(100);
        pdf.setHeaderRows(1);
        float tamanho = tabela.colunas().size() >= 10 ? 5.5f : tabela.colunas().size() >= 8 ? 6.2f : 7f;
        for (String coluna : tabela.colunas()) {
            PdfPCell cell = new PdfPCell(new Phrase(coluna, fontePdf(tamanho, true, Color.WHITE)));
            cell.setBackgroundColor(AZUL_SGL);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            pdf.addCell(cell);
        }
        for (List<String> linha : tabela.linhas()) {
            for (String valor : linha) {
                PdfPCell cell = new PdfPCell(new Phrase(texto(valor), fontePdf(tamanho, false, new Color(45, 58, 78))));
                cell.setPadding(4);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                pdf.addCell(cell);
            }
        }
        if (tabela.linhas().isEmpty()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum registro encontrado.", fontePdf(7, false, CINZA_TEXTO)));
            vazio.setColspan(tabela.colunas().size());
            vazio.setPadding(8);
            pdf.addCell(vazio);
        }
        pdf.setSpacingAfter(10);
        document.add(pdf);
    }

    private byte[] gerarXlsx(String titulo, List<String> filtros, Map<String, String> resumo, List<Tabela> tabelas) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (Tabela tabela : tabelas) {
                criarAba(workbook, titulo, filtros, resumo, tabela);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gerar o XLSX do relatório.", e);
        }
    }

    private void criarAba(Workbook workbook, String titulo, List<String> filtros, Map<String, String> resumo, Tabela tabela) {
        Sheet sheet = workbook.createSheet(nomeAba(tabela.nome()));
        int totalColunas = Math.max(2, tabela.colunas().size());
        int linha = 0;

        Row cabecalho = sheet.createRow(linha++);
        cabecalho.setHeightInPoints(42);
        adicionarLogoXlsx(workbook, sheet);
        Cell tituloCell = cabecalho.createCell(2);
        tituloCell.setCellValue(titulo);
        tituloCell.setCellStyle(estiloTitulo(workbook));
        if (totalColunas > 3) sheet.addMergedRegion(new CellRangeAddress(0, 1, 2, totalColunas - 1));
        linha++;

        Row info = sheet.createRow(linha++);
        Cell infoCell = info.createCell(0);
        infoCell.setCellValue("Gerado em " + LocalDateTime.now().format(DATA_HORA) + (filtros == null || filtros.isEmpty() ? "" : " | Filtros: " + String.join(" • ", filtros)));
        infoCell.setCellStyle(estiloInfo(workbook));
        sheet.addMergedRegion(new CellRangeAddress(info.getRowNum(), info.getRowNum(), 0, totalColunas - 1));

        if (!resumo.isEmpty()) {
            Row resumoRow = sheet.createRow(linha++);
            int coluna = 0;
            for (Map.Entry<String, String> item : resumo.entrySet()) {
                if (coluna >= totalColunas) break;
                Cell cell = resumoRow.createCell(coluna++);
                cell.setCellValue(item.getKey() + ": " + item.getValue());
                cell.setCellStyle(estiloResumo(workbook));
            }
        }

        linha++;
        int headerRow = linha++;
        Row headers = sheet.createRow(headerRow);
        CellStyle headerStyle = estiloCabecalho(workbook);
        for (int c = 0; c < tabela.colunas().size(); c++) {
            Cell cell = headers.createCell(c);
            cell.setCellValue(tabela.colunas().get(c));
            cell.setCellStyle(headerStyle);
        }

        CellStyle corpo = estiloCorpo(workbook);
        for (List<String> valores : tabela.linhas()) {
            Row row = sheet.createRow(linha++);
            for (int c = 0; c < valores.size(); c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(texto(valores.get(c)));
                cell.setCellStyle(corpo);
            }
        }

        sheet.createFreezePane(0, headerRow + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRow, Math.max(headerRow, linha - 1), 0, Math.max(0, tabela.colunas().size() - 1)));
        sheet.setRepeatingRows(CellRangeAddress.valueOf((headerRow + 1) + ":" + (headerRow + 1)));
        sheet.setAutobreaks(true);
        sheet.setFitToPage(true);
        PrintSetup print = sheet.getPrintSetup();
        print.setLandscape(tabela.colunas().size() > 6);
        print.setFitWidth((short) 1);
        print.setFitHeight((short) 0);
        print.setPaperSize(PrintSetup.A4_PAPERSIZE);

        for (int c = 0; c < tabela.colunas().size(); c++) {
            sheet.autoSizeColumn(c);
            int largura = Math.min(sheet.getColumnWidth(c) + 768, 48 * 256);
            sheet.setColumnWidth(c, Math.max(largura, 12 * 256));
        }
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.RightMargin, 0.25);
        sheet.setMargin(Sheet.TopMargin, 0.45);
        sheet.setMargin(Sheet.BottomMargin, 0.45);
    }

    private void adicionarLogoXlsx(Workbook workbook, Sheet sheet) {
        byte[] logo = carregarLogo();
        if (logo.length == 0) return;
        int picture = workbook.addPicture(logo, Workbook.PICTURE_TYPE_PNG);
        CreationHelper helper = workbook.getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        var anchor = helper.createClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setCol2(2);
        anchor.setRow2(2);
        drawing.createPicture(anchor, picture);
    }

    private byte[] carregarLogo() {
        if (logoCache != null) return logoCache;
        try {
            logoCache = new ClassPathResource("relatorios/logo-sgl.png").getInputStream().readAllBytes();
        } catch (IOException e) {
            logoCache = new byte[0];
        }
        return logoCache;
    }

    private com.lowagie.text.Font fontePdf(float tamanho, boolean negrito, Color cor) {
        return new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA,
                tamanho,
                negrito ? com.lowagie.text.Font.BOLD : com.lowagie.text.Font.NORMAL,
                cor
        );
    }

    private CellStyle estiloTitulo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle estiloInfo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle estiloResumo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle estiloCabecalho(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle estiloCorpo(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private Map<String, String> resumo(Object... valores) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < valores.length; i += 2) {
            mapa.put(texto(valores[i]), texto(valores[i + 1]));
        }
        return mapa;
    }

    public List<String> filtros(Object... valores) {
        List<String> resultado = new ArrayList<>();
        for (int i = 0; i + 1 < valores.length; i += 2) {
            Object valor = valores[i + 1];
            if (valor != null && !valor.toString().isBlank()) {
                resultado.add(texto(valores[i]) + ": " + rotulo(valor));
            }
        }
        return resultado;
    }

    private String nomeAba(String nome) {
        String limpo = nome.replaceAll("[\\/*?:\\[\\]]", "-");
        return limpo.length() > 31 ? limpo.substring(0, 31) : limpo;
    }

    private String texto(Object valor) {
        if (valor == null || valor.toString().isBlank()) return "—";
        return valor.toString();
    }

    private String rotulo(Object valor) {
        if (valor == null) return "—";
        String base = valor.toString().replace('_', ' ').toLowerCase();
        StringBuilder saida = new StringBuilder();
        for (String palavra : base.split(" ")) {
            if (!saida.isEmpty()) saida.append(' ');
            if (!palavra.isEmpty()) saida.append(Character.toUpperCase(palavra.charAt(0))).append(palavra.substring(1));
        }
        return saida.toString();
    }

    private String data(LocalDate valor) {
        return valor == null ? "—" : valor.format(DATA);
    }

    private String dataHora(LocalDateTime valor) {
        return valor == null ? "—" : valor.format(DATA_HORA);
    }

    private record Tabela(String nome, List<String> colunas, List<List<String>> linhas) {
    }

    private static class RodapePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase rodape = new Phrase("SGL • Página " + writer.getPageNumber(),
                    new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 7, com.lowagie.text.Font.NORMAL, CINZA_TEXTO));
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, rodape,
                    (document.right() + document.left()) / 2, document.bottom() - 14, 0);
        }
    }
}
