package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.SolucaoComponenteRequestDTO;
import com.sgl.dto.request.SolucaoRequestDTO;
import com.sgl.dto.response.SolucaoResponseDTO;
import com.sgl.model.Produto;
import com.sgl.model.Solucao;
import com.sgl.model.SolucaoComponente;
import com.sgl.model.Unidade;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.service.SolucaoService;

@WebMvcTest(SolucaoController.class)
@Import(SecurityConfig.class)
class SolucaoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/solucoes";

    private static final UUID UNIDADE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final UUID PRODUTO_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000002");
    private static final UUID SOLUCAO_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000003");
    private static final UUID COMPONENTE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SolucaoService solucaoService;

    @Test
    void deveCriarSolucaoERetornar201() throws Exception {
        when(solucaoService.criar(any(SolucaoRequestDTO.class)))
                .thenReturn(responseValida());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SOLUCAO_ID.toString()))
                .andExpect(jsonPath("$.componentes[0].produtoId").value(PRODUTO_ID.toString()));
    }

    @Test
    void deveRetornar400ParaComposicaoVazia() throws Exception {
        SolucaoRequestDTO dto = requestValida();
        dto.setComponentes(List.of());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarSolucoesERetornar200() throws Exception {
        when(solucaoService.listarTodos()).thenReturn(List.of(responseValida()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Solução Etanólica"));
    }

    @Test
    void deveListarSolucoesAtivasERetornar200() throws Exception {
        when(solucaoService.listarAtivas()).thenReturn(List.of(responseValida()));

        mockMvc.perform(get(BASE_URL + "/ativas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(solucaoService.buscarPorId(SOLUCAO_ID)).thenReturn(responseValida());

        mockMvc.perform(get(BASE_URL + "/{id}", SOLUCAO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SOLUCAO_ID.toString()));
    }

    @Test
    void deveAtualizarERetornar200() throws Exception {
        when(solucaoService.atualizar(eq(SOLUCAO_ID), any(SolucaoRequestDTO.class)))
                .thenReturn(responseValida());

        mockMvc.perform(put(BASE_URL + "/{id}", SOLUCAO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValida())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendimentoUnidade").value("L"));
    }

    @Test
    void deveInativarERetornar204() throws Exception {
        doNothing().when(solucaoService).deletar(SOLUCAO_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", SOLUCAO_ID))
                .andExpect(status().isNoContent());

        verify(solucaoService).deletar(SOLUCAO_ID);
    }

    private SolucaoRequestDTO requestValida() {
        SolucaoComponenteRequestDTO componente = new SolucaoComponenteRequestDTO();
        componente.setProdutoId(PRODUTO_ID);
        componente.setQuantidade(new BigDecimal("500.000"));
        componente.setUnidadeMedida(UnidadeMedida.ML);

        SolucaoRequestDTO dto = new SolucaoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("Solução Etanólica");
        dto.setRendimentoQuantidade(BigDecimal.ONE);
        dto.setRendimentoUnidade(UnidadeMedida.L);
        dto.setComponentes(List.of(componente));
        return dto;
    }

    private SolucaoResponseDTO responseValida() {
        Unidade unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        Produto produto = Produto.builder()
                .id(2L)
                .publicId(PRODUTO_ID)
                .nome("Etanol")
                .codigoReferencia("ET-001")
                .unidadeMedida(UnidadeMedida.L)
                .ativo(true)
                .build();

        Solucao solucao = Solucao.builder()
                .id(3L)
                .publicId(SOLUCAO_ID)
                .unidade(unidade)
                .nome("Solução Etanólica")
                .rendimentoQuantidade(BigDecimal.ONE)
                .rendimentoUnidade(UnidadeMedida.L)
                .ativo(true)
                .build();

        SolucaoComponente componente = SolucaoComponente.builder()
                .id(4L)
                .publicId(COMPONENTE_ID)
                .solucao(solucao)
                .produto(produto)
                .quantidade(new BigDecimal("500"))
                .unidadeMedida(UnidadeMedida.ML)
                .build();

        solucao.getComponentes().add(componente);
        return new SolucaoResponseDTO(solucao);
    }
}
