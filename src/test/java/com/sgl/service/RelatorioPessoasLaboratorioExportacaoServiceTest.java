package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioPessoaLaboratorioItemDTO;
import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;

/**
 * Testes de {@link RelatorioPessoasLaboratorioExportacaoService}.
 *
 * <p>Mesmo padrão dos outros dois services de exportação do Batch S6: sem
 * {@code *Repository} para mockar, recebe um DTO pronto e gera PDF/XLSX de
 * verdade. Os testes de caminho feliz conferem só que os bytes gerados não
 * são vazios (teste de integração leve, conforme exceção documentada no
 * brief).</p>
 *
 * <p><b>Testes de exceção:</b> {@code gerarPdf()}/{@code gerarXlsx()}
 * capturam {@code Exception} genérico. Usamos um DTO com
 * {@code pessoas == null}: o laço
 * {@code for (RelatorioPessoaLaboratorioItemDTO item : relatorio.getPessoas())}
 * lança {@code NullPointerException} ao iterar sobre {@code null}, que cai
 * no catch real e é relançada como {@code IllegalStateException} com a
 * mensagem exata do fonte — sem mockar nada.</p>
 */
class RelatorioPessoasLaboratorioExportacaoServiceTest {

    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final RelatorioPessoasLaboratorioExportacaoService service =
            new RelatorioPessoasLaboratorioExportacaoService();

    @Test
    void deveExportarPessoasPorLaboratorioEmPdf() {
        RelatorioPessoasLaboratorioResponseDTO relatorio = relatorioValido(List.of());

        ArquivoRelatorioDTO arquivo = service.exportar(relatorio, FormatoExportacaoRelatorio.PDF);

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
        assertEquals("application/pdf", arquivo.contentType());
    }

    @Test
    void deveExportarPessoasPorLaboratorioEmXlsx() {
        RelatorioPessoasLaboratorioResponseDTO relatorio = relatorioValido(List.of());

        ArquivoRelatorioDTO arquivo = service.exportar(relatorio, FormatoExportacaoRelatorio.XLSX);

        assertTrue(arquivo.conteudo().length > 0);
        assertTrue(arquivo.nomeArquivo().endsWith(".xlsx"));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", arquivo.contentType());
    }

    @Test
    void deveLancarExcecaoQuandoFalhaNaGeracaoDoPdf() {
        // pessoas == null força NullPointerException dentro do laço de
        // montagem da tabela do PDF, capturada pelo catch (Exception e)
        // genérico do gerarPdf() real.
        RelatorioPessoasLaboratorioResponseDTO relatorio = relatorioValido(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.exportar(relatorio, FormatoExportacaoRelatorio.PDF));

        assertEquals("Não foi possível gerar o PDF de pessoas por laboratório.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoFalhaNaGeracaoDoXlsx() {
        RelatorioPessoasLaboratorioResponseDTO relatorio = relatorioValido(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.exportar(relatorio, FormatoExportacaoRelatorio.XLSX));

        assertEquals("Não foi possível gerar o XLSX de pessoas por laboratório.", ex.getMessage());
    }

    private RelatorioPessoasLaboratorioResponseDTO relatorioValido(List<RelatorioPessoaLaboratorioItemDTO> pessoas) {
        RelatorioPessoasLaboratorioResponseDTO relatorio = new RelatorioPessoasLaboratorioResponseDTO();
        relatorio.setGeradoEm(LocalDateTime.now());
        relatorio.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        relatorio.setLaboratorioNome("Laboratório de Biologia Molecular");
        relatorio.setUnidadeNome("Embrapa Mandioca e Fruticultura");
        relatorio.setResponsavelNome("Responsável Teste");
        relatorio.setResponsavelEmail("responsavel@embrapa.br");
        relatorio.setTotalPessoas(0);
        relatorio.setAtivos(0);
        relatorio.setInativos(0);
        relatorio.setPorPerfil(null);
        relatorio.setPessoas(pessoas);
        return relatorio;
    }
}
