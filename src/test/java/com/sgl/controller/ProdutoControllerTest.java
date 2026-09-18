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

import java.util.List;
import java.util.Set;
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
import com.sgl.dto.request.ProdutoRequestDTO;
import com.sgl.dto.response.ProdutoResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.service.ProdutoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link ProdutoController}. Segue o mesmo
 * padrão fixado em ProjetoControllerTest/EstagiarioControllerTest (ver notas de
 * configuração lá): bean de {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link ProdutoService} é mockado - toda a regra de negócio (tenant, código de
 * referência duplicado, risco/perecibilidade/fiscalização) já é coberta em
 * ProdutoServiceTest; aqui o foco é só roteamento HTTP, serialização e validação de
 * request.
 */
@WebMvcTest(ProdutoController.class)
@Import(SecurityConfig.class)
class ProdutoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // ProdutoRequestDTO/ProdutoResponseDTO não têm campos de data - um
            // ObjectMapper "cru" é suficiente (diferente do caso de Lote/Estagiario).
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/produtos";

    private static final UUID PRODUTO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProdutoService produtoService;

    private ProdutoRequestDTO montarRequestDTO() {
        return new ProdutoRequestDTO(
                "Etanol Absoluto PA",
                "Álcool etílico absoluto para uso em laboratório.",
                "ETL-ABS-001",
                UnidadeMedida.L,
                "Armário B - Prateleira 1",
                NivelRisco.ALTO,
                TipoRisco.INFLAMAVEL,
                "Manter longe de fontes de ignição.",
                false,
                null,
                null,
                "frasco de 1L",
                false,
                Set.of(),
                null,
                true,
                Set.of(),
                null);
    }

    // ProdutoResponseDTO só tem construtor a partir da entidade Produto (campos
    // "final", sem setters) - por isso montamos um Produto real via builder (igual
    // ao que ProdutoService faz) em vez de mockar o próprio DTO.
    private ProdutoResponseDTO montarResponseDTO() {
        Produto produto = Produto.builder()
                .publicId(PRODUTO_PUBLIC_ID)
                .nome("Etanol Absoluto PA")
                .descricao("Álcool etílico absoluto para uso em laboratório.")
                .codigoReferencia("ETL-ABS-001")
                .unidadeMedida(UnidadeMedida.L)
                .localizacaoFisica("Armário B - Prateleira 1")
                .risco(NivelRisco.ALTO)
                .tipoRisco(TipoRisco.INFLAMAVEL)
                .descricaoRisco("Manter longe de fontes de ignição.")
                .perecivel(false)
                .unidadeArmazenamento("frasco de 1L")
                .fiscalizado(false)
                .orgaosFiscalizadores(Set.of())
                .ativo(true)
                .medidasSegurancaRecomendadas(Set.of())
                .build();

        return new ProdutoResponseDTO(produto);
    }

    @Test
    void deveCriarProdutoERetornar201() throws Exception {
        ProdutoRequestDTO dto = montarRequestDTO();
        when(produtoService.criar(any(ProdutoRequestDTO.class))).thenReturn(montarResponseDTO());

        // ProdutoController usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (ProdutoController.java linha 46) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // ProdutoRequestDTO exige nome, codigoReferencia, unidadeMedida, risco e
        // perecivel (todos @NotBlank/@NotNull) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTodosOsProdutosERetornar200() throws Exception {
        when(produtoService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(PRODUTO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarProdutoPorIdERetornar200() throws Exception {
        when(produtoService.buscarPorId(PRODUTO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", PRODUTO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveRetornar404QuandoProdutoNaoEncontrado() throws Exception {
        when(produtoService.buscarPorId(PRODUTO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Produto", PRODUTO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", PRODUTO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarProdutoERetornar200() throws Exception {
        ProdutoRequestDTO dto = montarRequestDTO();
        when(produtoService.atualizar(eq(PRODUTO_PUBLIC_ID), any(ProdutoRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}", PRODUTO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveDeletarProdutoERetornar204() throws Exception {
        doNothing().when(produtoService).deletar(PRODUTO_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", PRODUTO_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(produtoService).deletar(PRODUTO_PUBLIC_ID);
    }

    @Test
    void deveListarPorRiscoERetornar200() throws Exception {
        when(produtoService.listarPorRisco(NivelRisco.ALTO)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/risco/{nivel}", "ALTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].risco").value("ALTO"));
    }

    @Test
    void deveListarPereciveisERetornar200() throws Exception {
        when(produtoService.listarPereciveis()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/pereciveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveBuscarPorNomeERetornar200() throws Exception {
        when(produtoService.buscarPorNome("Etanol")).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/buscar").param("nome", "Etanol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("Etanol Absoluto PA"));
    }
}
