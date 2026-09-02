package com.sgl.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
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
import com.sgl.dto.response.RelatorioPessoaLaboratorioItemDTO;
import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.model.enums.Perfil;

@Service
public class RelatorioPessoasLaboratorioExportacaoService {

    private static final String MIME_PDF = "application/pdf";
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter ARQUIVO_DATA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ArquivoRelatorioDTO exportar(RelatorioPessoasLaboratorioResponseDTO relatorio, FormatoExportacaoRelatorio formato) {
        String sufixo = LocalDateTime.now().format(ARQUIVO_DATA);
        String base = "sgl-pessoas-laboratorio-" + sufixo;

        if (formato == FormatoExportacaoRelatorio.PDF) {
            return new ArquivoRelatorioDTO(gerarPdf(relatorio), base + ".pdf", MIME_PDF);
        }
        return new ArquivoRelatorioDTO(gerarXlsx(relatorio), base + ".xlsx", MIME_XLSX);
    }

    private byte[] gerarPdf(RelatorioPessoasLaboratorioResponseDTO relatorio) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            adicionarLogo(document);
            com.lowagie.text.Font tituloFonte = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 17, com.lowagie.text.Font.BOLD, new Color(13, 43, 94));
            Paragraph titulo = new Paragraph("Pessoas por Laboratório", tituloFonte);
            titulo.setSpacingAfter(5);
            document.add(titulo);

            com.lowagie.text.Font meta = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.NORMAL, new Color(70, 84, 105));
            document.add(new Paragraph("Laboratório: " + texto(relatorio.getLaboratorioNome())
                    + " | Unidade: " + texto(relatorio.getUnidadeNome()), meta));
            document.add(new Paragraph("Responsável: " + texto(relatorio.getResponsavelNome())
                    + (relatorio.getResponsavelEmail() == null ? "" : " — " + relatorio.getResponsavelEmail()), meta));
            document.add(new Paragraph("Total: " + relatorio.getTotalPessoas()
                    + " | Ativos: " + relatorio.getAtivos()
                    + " | Inativos: " + relatorio.getInativos()
                    + " | Perfis: " + resumoPerfis(relatorio.getPorPerfil()), meta));
            document.add(new Paragraph("Gerado em: " + relatorio.getGeradoEm().format(DATA_HORA), meta));

            PdfPTable tabela = new PdfPTable(new float[]{1.2f, 2.2f, 2.5f, 1.3f, 1.1f, 1.6f, 1.2f, 1.2f});
            tabela.setWidthPercentage(100);
            tabela.setSpacingBefore(12);
            String[] cabecalhos = {"Papel", "Nome", "E-mail", "Perfil", "Situação", "Vínculo estágio", "Início", "Fim"};
            for (String cabecalho : cabecalhos) tabela.addCell(cabecalhoPdf(cabecalho));

            for (RelatorioPessoaLaboratorioItemDTO item : relatorio.getPessoas()) {
                tabela.addCell(celulaPdf(Boolean.TRUE.equals(item.getResponsavelLaboratorio()) ? "Responsável" : "Vinculado"));
                tabela.addCell(celulaPdf(texto(item.getNome())));
                tabela.addCell(celulaPdf(texto(item.getEmail())));
                tabela.addCell(celulaPdf(rotulo(item.getPerfil())));
                tabela.addCell(celulaPdf(Boolean.TRUE.equals(item.getAtivo()) ? "Ativo" : "Inativo"));
                tabela.addCell(celulaPdf(item.getTipoVinculoEstagio() == null ? "—" : rotulo(item.getTipoVinculoEstagio())));
                tabela.addCell(celulaPdf(data(item.getDataInicioEstagio())));
                tabela.addCell(celulaPdf(data(item.getDataFimEstagio())));
            }

            document.add(tabela);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o PDF de pessoas por laboratório.", e);
        }
    }

    private byte[] gerarXlsx(RelatorioPessoasLaboratorioResponseDTO relatorio) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Pessoas do laboratório");
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
            titulo.createCell(0).setCellValue("SGL — Pessoas por Laboratório");
            titulo.getCell(0).setCellStyle(tituloStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Row laboratorio = sheet.createRow(2);
            laboratorio.createCell(0).setCellValue("Laboratório");
            laboratorio.createCell(1).setCellValue(texto(relatorio.getLaboratorioNome()));
            laboratorio.createCell(2).setCellValue("Unidade");
            laboratorio.createCell(3).setCellValue(texto(relatorio.getUnidadeNome()));
            laboratorio.createCell(4).setCellValue("Responsável");
            laboratorio.createCell(5).setCellValue(texto(relatorio.getResponsavelNome()));

            Row resumo = sheet.createRow(3);
            resumo.createCell(0).setCellValue("Total");
            resumo.createCell(1).setCellValue(relatorio.getTotalPessoas());
            resumo.createCell(2).setCellValue("Ativos");
            resumo.createCell(3).setCellValue(relatorio.getAtivos());
            resumo.createCell(4).setCellValue("Inativos");
            resumo.createCell(5).setCellValue(relatorio.getInativos());
            resumo.createCell(6).setCellValue("Perfis");
            resumo.createCell(7).setCellValue(resumoPerfis(relatorio.getPorPerfil()));

            String[] cabecalhos = {"Papel", "Nome", "E-mail", "Perfil", "Situação", "Vínculo estágio", "Início", "Fim"};
            Row header = sheet.createRow(5);
            for (int i = 0; i < cabecalhos.length; i++) {
                header.createCell(i).setCellValue(cabecalhos[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }

            int rowIndex = 6;
            for (RelatorioPessoaLaboratorioItemDTO item : relatorio.getPessoas()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(Boolean.TRUE.equals(item.getResponsavelLaboratorio()) ? "Responsável" : "Vinculado");
                row.createCell(1).setCellValue(texto(item.getNome()));
                row.createCell(2).setCellValue(texto(item.getEmail()));
                row.createCell(3).setCellValue(rotulo(item.getPerfil()));
                row.createCell(4).setCellValue(Boolean.TRUE.equals(item.getAtivo()) ? "Ativo" : "Inativo");
                row.createCell(5).setCellValue(item.getTipoVinculoEstagio() == null ? "—" : rotulo(item.getTipoVinculoEstagio()));
                row.createCell(6).setCellValue(data(item.getDataInicioEstagio()));
                row.createCell(7).setCellValue(data(item.getDataFimEstagio()));
            }

            for (int i = 0; i < cabecalhos.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 16000));
            }
            sheet.createFreezePane(0, 6);
            sheet.setAutoFilter(new CellRangeAddress(5, Math.max(5, rowIndex - 1), 0, cabecalhos.length - 1));

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível gerar o XLSX de pessoas por laboratório.", e);
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
            // Mantém a exportação funcional mesmo se a marca não puder ser carregada.
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
                com.lowagie.text.Font.HELVETICA, 6.8f, com.lowagie.text.Font.NORMAL, new Color(45, 55, 72));
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private String resumoPerfis(Map<Perfil, Long> valores) {
        if (valores == null || valores.isEmpty()) return "—";
        return valores.entrySet().stream()
                .map(entry -> rotulo(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining(" | "));
    }

    private String data(LocalDate valor) {
        return valor == null ? "—" : valor.format(DATA);
    }

    private String texto(Object valor) {
        if (valor == null) return "—";
        String texto = String.valueOf(valor);
        return texto.isBlank() ? "—" : texto;
    }

    private String rotulo(Object valor) {
        if (valor == null) return "—";
        return Arrays.stream(String.valueOf(valor).toLowerCase().split("_"))
                .map(parte -> parte.isEmpty() ? parte : Character.toUpperCase(parte.charAt(0)) + parte.substring(1))
                .collect(Collectors.joining(" "));
    }
}
