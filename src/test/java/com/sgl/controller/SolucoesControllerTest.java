package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoPedido;
import com.sgl.service.PedidoService;

/** Regressao HTTP para o contrato de solicitacao personalizada e por catalogo. */
@WebMvcTest(SolucoesController.class)
@Import(SecurityConfig.class)
class SolucoesControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LAB = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PROJETO = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PRODUTO = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID MODELO = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PedidoService service;

    private PedidoResponseDTO resposta() {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        dto.setUsuarioId(USUARIO);
        dto.setLaboratorioId(LAB);
        dto.setStatus(StatusPedido.PENDENTE);
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setDataSolicitacao(LocalDateTime.of(2026, 10, 13, 19, 0));
        dto.setItens(List.of());
        return dto;
    }

    @Test
    void aceitaAvulsaComUnidadeFisicaSemCamposDeEmbalagem() throws Exception {
        when(service.criar(any(PedidoRequestDTO.class))).thenReturn(resposta());
        String json = """
            {
              "tipo": "SOLUCAO",
              "usuarioId": "%s",
              "laboratorioId": "%s",
              "projetoId": "%s",
              "nomeSolucao": "Solução personalizada",
              "urgente": false,
              "itens": [
                {
                  "produtoId": "%s",
                  "quantidadeSolicitada": 1,
                  "unidadeMedidaSolicitada": "REACAO"
                }
              ]
            }
            """.formatted(USUARIO, LAB, PROJETO, PRODUTO);
        mockMvc.perform(post("/api/v1/solucoes/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("SOLUCAO"))
            .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void aceitaModeloSemItensInformadosPeloCliente() throws Exception {
        when(service.criar(any(PedidoRequestDTO.class))).thenReturn(resposta());
        String json = """
            {
              "tipo": "SOLUCAO",
              "usuarioId": "%s",
              "laboratorioId": "%s",
              "projetoId": "%s",
              "modeloSolucaoId": "%s",
              "itens": []
            }
            """.formatted(USUARIO, LAB, PROJETO, MODELO);
        mockMvc.perform(post("/api/v1/solucoes/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    void rejeitaAvulsaSemQuantidade() throws Exception {
        String json = """
            {
              "tipo": "SOLUCAO",
              "usuarioId": "%s",
              "laboratorioId": "%s",
              "projetoId": "%s",
              "nomeSolucao": "Solução personalizada",
              "itens": [
                { "produtoId": "%s", "unidadeMedidaSolicitada": "REACAO" }
              ]
            }
            """.formatted(USUARIO, LAB, PROJETO, PRODUTO);
        mockMvc.perform(post("/api/v1/solucoes/pedidos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
