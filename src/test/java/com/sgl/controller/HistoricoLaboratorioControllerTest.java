package com.sgl.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.response.ConsumoProdutoLaboratorioResponseDTO;
import com.sgl.dto.response.HistoricoLaboratorioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.service.HistoricoLaboratorioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de
 * {@link HistoricoLaboratorioController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (ver
 * controllers-context.md): {@code @MockitoBean} no lugar de
 * {@code @MockBean}, {@code ObjectMapper} próprio via
 * {@code @TestConfiguration} (com {@code JavaTimeModule}, já que vários
 * DTOs de resposta têm campo {@code LocalDate}) e
 * {@code @Import(SecurityConfig.class)}.
 *
 * {@link HistoricoLaboratorioResponseDTO} tem {@code @NoArgsConstructor} +
 * {@code @Setter}, então é montado diretamente sem precisar de uma entidade
 * real - diferente de ResiduoResponseDTO/RotuloResiduoResponseDTO no batch
 * anterior.
 *
 * Controller é só de consulta (nenhum endpoint com {@code @RequestBody}),
 * então não há teste de validação de {@code @Valid}. Os cenários de erro
 * exigidos pela brief (período invertido e projeto que não pertence ao
 * laboratório) já estão cobertos em detalhe por
 * HistoricoLaboratorioServiceTest; aqui só confirmamos que o Controller
 * propaga a {@link BusinessRuleException} do Service mockado como 400.
 */
@WebMvcTest(HistoricoLaboratorioController.class)
@Import(SecurityConfig.class)
class HistoricoLaboratorioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/historico-laboratorio";

    private static final UUID HISTORICO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PEDIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoricoLaboratorioService historicoLaboratorioService;

    // HistoricoLaboratorioResponseDTO tem @NoArgsConstructor + @Setter,
    // então montamos o DTO diretamente sem precisar de uma entidade real
    // (mesmo padrão do LoteControllerTest/MovimentacaoEstoqueControllerTest).
    private HistoricoLaboratorioResponseDTO montarHistoricoResponseDTO() {
        HistoricoLaboratorioResponseDTO dto = new HistoricoLaboratorioResponseDTO();
        dto.setId(HISTORICO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setLaboratorioNome("Laboratório de Química Orgânica");
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setProdutoNome("Extrato de DNA Plant Wizard");
        dto.setProdutoUnidadeArmazenamento("kit com 50 reações");
        dto.setQuantidade(8);
        dto.setDataRecebimento(LocalDate.of(2026, 8, 20));
        dto.setPedidoId(PEDIDO_PUBLIC_ID);
        dto.setAtivo(true);
        return dto;
    }

    private ConsumoProdutoLaboratorioResponseDTO montarConsumoResponseDTO() {
        return new ConsumoProdutoLaboratorioResponseDTO(
                LABORATORIO_PUBLIC_ID,
                "Laboratório de Química Orgânica",
                PRODUTO_PUBLIC_ID,
                "Extrato de DNA Plant Wizard",
                "kit com 50 reações",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                4L,
                32,
                new BigDecimal("8.00"),
                1,
                new BigDecimal("32.00"),
                32);
    }

    @Test
    void deveListarTodosERetornar200() throws Exception {
        when(historicoLaboratorioService.listarTodos()).thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(HISTORICO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(historicoLaboratorioService.buscarPorId(HISTORICO_PUBLIC_ID)).thenReturn(montarHistoricoResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", HISTORICO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoNome").value("Extrato de DNA Plant Wizard"));
    }

    @Test
    void deveListarPorLaboratorioERetornar200() throws Exception {
        when(historicoLaboratorioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}", LABORATORIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorProdutoERetornar200() throws Exception {
        when(historicoLaboratorioService.listarPorProduto(PRODUTO_PUBLIC_ID))
                .thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/produto/{produtoId}", PRODUTO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produtoId").value(PRODUTO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorPedidoERetornar200() throws Exception {
        when(historicoLaboratorioService.listarPorPedido(PEDIDO_PUBLIC_ID))
                .thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/pedido/{pedidoId}", PEDIDO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pedidoId").value(PEDIDO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorPeriodoERetornar200() throws Exception {
        LocalDate dataInicio = LocalDate.of(2026, 8, 1);
        LocalDate dataFim = LocalDate.of(2026, 8, 31);
        when(historicoLaboratorioService.listarPorPeriodo(LABORATORIO_PUBLIC_ID, dataInicio, dataFim))
                .thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/periodo", LABORATORIO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(HISTORICO_PUBLIC_ID.toString()));
    }

    @Test
    void deveRetornar400QuandoPeriodoInvertido() throws Exception {
        // docs/testes.md item 17 - mensagem exata lançada por
        // HistoricoLaboratorioService.validarPeriodo().
        LocalDate dataInicio = LocalDate.of(2026, 8, 31);
        LocalDate dataFim = LocalDate.of(2026, 8, 1);
        when(historicoLaboratorioService.listarPorPeriodo(LABORATORIO_PUBLIC_ID, dataInicio, dataFim))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/periodo", LABORATORIO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCalcularConsumoProdutoERetornar200() throws Exception {
        LocalDate dataInicio = LocalDate.of(2026, 8, 1);
        LocalDate dataFim = LocalDate.of(2026, 8, 31);
        when(historicoLaboratorioService.calcularConsumoProduto(LABORATORIO_PUBLIC_ID, PRODUTO_PUBLIC_ID, dataInicio, dataFim))
                .thenReturn(montarConsumoResponseDTO());

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/produto/{produtoId}/consumo",
                        LABORATORIO_PUBLIC_ID, PRODUTO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeTotalRecebida").value(32));
    }

    @Test
    void deveListarPorProjetoEPeriodoERetornar200() throws Exception {
        LocalDate dataInicio = LocalDate.of(2026, 8, 1);
        LocalDate dataFim = LocalDate.of(2026, 8, 31);
        when(historicoLaboratorioService.listarPorProjetoEPeriodo(LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID, dataInicio, dataFim))
                .thenReturn(List.of(montarHistoricoResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo",
                        LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornar400QuandoProjetoNaoPertenceAoLaboratorio() throws Exception {
        // docs/testes.md item 16 - mensagem exata lançada por
        // HistoricoLaboratorioService.buscarProjetoDoLaboratorio().
        LocalDate dataInicio = LocalDate.of(2026, 8, 1);
        LocalDate dataFim = LocalDate.of(2026, 8, 31);
        when(historicoLaboratorioService.listarPorProjetoEPeriodo(LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID, dataInicio, dataFim))
                .thenThrow(new BusinessRuleException("O projeto informado não pertence ao laboratório informado."));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo",
                        LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isBadRequest());
    }
}
