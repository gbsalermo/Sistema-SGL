package com.sgl.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;

@Service
public class RelatorioResiduosExportacaoService {

    private static final String MIME_PDF = "application/pdf";
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter ARQUIVO_DATA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ArquivoRelatorioDTO exportar(RelatorioResiduosResponseDTO relatorio, FormatoExportacaoRelatorio formato) {
        String sufixo = LocalDateTime.now().format(ARQUIVO_DATA);
        if (formato == FormatoExportacaoRelatorio.PDF) {
            return new ArquivoRelatorioDTO(
                    gerarPdf(relatorio),
                    "sgl-residuos-" + sufixo + ".pdf",
                    MIME_PDF
            );
        }

        return new ArquivoRelatorioDTO(
                gerarXlsx(relatorio),
                "sgl-residuos-" + sufixo + ".xlsx",
                MIME_XLSX
        );
    }

    private byte[] gerarPdf(RelatorioResiduosResponseDTO relatorio) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 26, 26, 30, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            adicionarLogo(document);
            com.lowagie.text.Font tituloFonte = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD,
                    new Color(13, 43, 94));
            Paragraph titulo = new Paragraph("Relatório de Resíduos", tituloFonte);
            titulo.setSpacingAfter(5);
            document.add(titulo);

            com.lowagie.text.Font metaFonte = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL,
                    new Color(70, 84, 105));
            document.add(new Paragraph("Gerado em: " + relatorio.getGeradoEm().format(DATA_HORA), metaFonte));
            document.add(new Paragraph(
                    "Total: " + relatorio.getTotal()
                            + " | A receber: " + relatorio.getInformados()
                            + " | Em análise: " + relatorio.getEmAnalise()
                            + " | Liberados: " + relatorio.getLiberados()
                            + " | Armazenados: " + relatorio.getArmazenados()
                            + " | Despachados: " + relatorio.getDespachados()
                            + " | Alto risco: " + relatorio.getAltoRisco(),
                    metaFonte));

            PdfPTable tabela = new PdfPTable(new float[]{1.2f, 2.4f, 1.7f, 1.5f, 1.2f, 1.2f, 1.8f, 1.8f, 1.7f});
            tabela.setWidthPercentage(100);
            tabela.setSpacingBefore(12);

            List<String> cabecalhos = List.of(
                    "Código", "Resíduo", "Laboratório", "Gerador", "Status", "Risco",
                    "Composição", "Armazenamento", "Destino"
            );
            cabecalhos.forEach(texto -> tabela.addCell(cabecalhoPdf(texto)));

            for (ResiduoResponseDTO item : relatorio.getItens()) {
                tabela.addCell(celulaPdf(texto(item.getCodigoRastreio())));
                tabela.addCell(celulaPdf(texto(item.getDescricao())));
                tabela.addCell(celulaPdf(texto(item.getLaboratorioNome())));
                tabela.addCell(celulaPdf(texto(item.getUsuarioGeradorNome())));
                tabela.addCell(celulaPdf(rotulo(item.getStatus().name())));
                tabela.addCell(celulaPdf(rotulo(riscoEfetivo(item).name())));
                tabela.addCell(celulaPdf(componentes(item)));
                tabela.addCell(celulaPdf(texto(item.getLocalArmazenamentoTemporario())));
                tabela.addCell(celulaPdf(texto(destinoEfetivo(item))));
            }

            document.add(tabela);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF do relatório de resíduos.", e);
        }
    }

    private byte[] gerarXlsx(RelatorioResiduosResponseDTO relatorio) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Resíduos");
            sheet.setDisplayGridlines(false);

            CellStyle tituloStyle = workbook.createCellStyle();
            Font tituloFont = workbook.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 16);
            tituloFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            tituloStyle.setFont(tituloFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titulo = sheet.createRow(0);
            titulo.createCell(0).setCellValue("SGL — Relatório de Resíduos");
            titulo.getCell(0).setCellStyle(tituloStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));

            Row resumo = sheet.createRow(2);
            resumo.createCell(0).setCellValue("Total");
            resumo.createCell(1).setCellValue(relatorio.getTotal());
            resumo.createCell(2).setCellValue("A receber");
            resumo.createCell(3).setCellValue(relatorio.getInformados());
            resumo.createCell(4).setCellValue("Em análise");
            resumo.createCell(5).setCellValue(relatorio.getEmAnalise());
            resumo.createCell(6).setCellValue("Liberados");
            resumo.createCell(7).setCellValue(relatorio.getLiberados());
            resumo.createCell(8).setCellValue("Armazenados");
            resumo.createCell(9).setCellValue(relatorio.getArmazenados());
            resumo.createCell(10).setCellValue("Despachados");
            resumo.createCell(11).setCellValue(relatorio.getDespachados());

            String[] cabecalhos = {
                    "Código", "Resíduo", "Laboratório", "Gerador", "Projeto", "Status",
                    "Risco", "Riscos", "Quantidade", "Composição", "Armazenamento", "Destino",
                    "Informado em", "Recebido em", "Liberado em", "Armazenado em", "Despachado em"
            };
            Row header = sheet.createRow(4);
            for (int i = 0; i < cabecalhos.length; i++) {
                header.createCell(i).setCellValue(cabecalhos[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            int rowIndex = 5;
            for (ResiduoResponseDTO item : relatorio.getItens()) {
                Row row = sheet.createRow(rowIndex++);
                int c = 0;
                row.createCell(c++).setCellValue(texto(item.getCodigoRastreio()));
                row.createCell(c++).setCellValue(texto(item.getDescricao()));
                row.createCell(c++).setCellValue(texto(item.getLaboratorioNome()));
                row.createCell(c++).setCellValue(texto(item.getUsuarioGeradorNome()));
                row.createCell(c++).setCellValue(texto(item.getProjetoNome()));
                row.createCell(c++).setCellValue(rotulo(item.getStatus().name()));
                row.createCell(c++).setCellValue(rotulo(riscoEfetivo(item).name()));
                row.createCell(c++).setCellValue(riscos(item));
                row.createCell(c++).setCellValue(item.getQuantidade() + " " + item.getUnidadeMedida());
                row.createCell(c++).setCellValue(componentes(item));
                row.createCell(c++).setCellValue(texto(item.getLocalArmazenamentoTemporario()));
                row.createCell(c++).setCellValue(texto(destinoEfetivo(item)));
                row.createCell(c++).setCellValue(dataHora(item.getDataInformacao()));
                row.createCell(c++).setCellValue(dataHora(item.getDataRecebimento()));
                row.createCell(c++).setCellValue(dataHora(item.getDataLiberacao()));
                row.createCell(c++).setCellValue(dataHora(item.getDataArmazenamentoTemporario()));
                row.createCell(c).setCellValue(dataHora(item.getDataDespacho()));
            }

            for (int i = 0; i < cabecalhos.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 16000));
            }
            sheet.createFreezePane(0, 5);
            sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, rowIndex - 1), 0, cabecalhos.length - 1));

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o XLSX do relatório de resíduos.", e);
        }
    }

    private void adicionarLogo(Document document) {
        try {
            ClassPathResource resource = new ClassPathResource("relatorios/logo-sgl.png");
            Image logo = Image.getInstance(resource.getInputStream().readAllBytes());
            logo.scaleToFit(90, 40);
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception ignored) {
            // A exportação continua funcional mesmo se a marca não puder ser carregada.
        }
    }

    private PdfPCell cabecalhoPdf(String texto) {
        com.lowagie.text.Font fonte = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 7, com.lowagie.text.Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setBackgroundColor(new Color(13, 43, 94));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell celulaPdf(String texto) {
        com.lowagie.text.Font fonte = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 6.5f, com.lowagie.text.Font.NORMAL, new Color(45, 55, 72));
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private NivelRisco riscoEfetivo(ResiduoResponseDTO item) {
        return item.getNivelRiscoConfirmado() != null ? item.getNivelRiscoConfirmado() : item.getNivelRiscoInformado();
    }

    private String riscos(ResiduoResponseDTO item) {
        Set<TipoRisco> valores = item.getRiscosConfirmados() != null && !item.getRiscosConfirmados().isEmpty()
                ? item.getRiscosConfirmados()
                : item.getRiscosInformados();
        if (valores == null || valores.isEmpty()) return "—";
        return valores.stream().map(risco -> rotulo(risco.name())).sorted().collect(Collectors.joining(", "));
    }

    private String componentes(ResiduoResponseDTO item) {
        if (item.getComponentes() == null || item.getComponentes().isEmpty()) return "—";
        return item.getComponentes().stream()
                .map(componente -> componente.getNomeComponente()
                        + (componente.getConcentracaoOuQuantidade() == null || componente.getConcentracaoOuQuantidade().isBlank()
                        ? "" : " (" + componente.getConcentracaoOuQuantidade() + ")"))
                .collect(Collectors.joining("; "));
    }

    private String destinoEfetivo(ResiduoResponseDTO item) {
        return item.getDestinoFinalConfirmado() != null && !item.getDestinoFinalConfirmado().isBlank()
                ? item.getDestinoFinalConfirmado()
                : item.getDestinoFinalPrevisto();
    }

    private String dataHora(LocalDateTime data) {
        return data == null ? "—" : data.format(DATA_HORA);
    }

    private String texto(Object valor) {
        if (valor == null) return "—";
        String texto = String.valueOf(valor);
        return texto.isBlank() ? "—" : texto;
    }

    private String rotulo(String valor) {
        if (valor == null || valor.isBlank()) return "—";
        String[] partes = valor.toLowerCase().split("_");
        return java.util.Arrays.stream(partes)
                .map(parte -> parte.isEmpty() ? parte : Character.toUpperCase(parte.charAt(0)) + parte.substring(1))
                .collect(Collectors.joining(" "));
    }
}
