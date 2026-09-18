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
import com.sgl.dto.request.EstoqueCentralRequestDTO;
import com.sgl.dto.response.EstoqueCentralResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.service.EstoqueCentralService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link EstoqueCentralController}. Segue o
 * mesmo padrão fixado em ProdutoControllerTest/ProjetoControllerTest (ver notas de
 * configuração lá): bean de {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link EstoqueCentralService} é mockado - toda a regra de tenant/negócio (já coberta
 * em EstoqueCentralServiceTest) não é exercitada aqui, só roteamento HTTP, serialização
 * e validação de request.
 */
@WebMvcTest(EstoqueCentralController.class)
@Import(SecurityConfig.class)
class EstoqueCentralControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // EstoqueCentralRequestDTO/EstoqueCentralResponseDTO não têm campos de
            // data - um ObjectMapper "cru" é suficiente.
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/estoque-central";

    private static final UUID ESTOQUE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUTO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstoqueCentralService estoqueCentralService;

    private EstoqueCentralRequestDTO montarRequestDTO() {
        return new EstoqueCentralRequestDTO(UNIDADE_PUBLIC_ID, PRODUTO_PUBLIC_ID, 5, true);
    }

    // Diferente de ProdutoResponseDTO, EstoqueCentralResponseDTO tem
    // @NoArgsConstructor + @Setter, então montamos o DTO diretamente sem precisar
    // de uma entidade EstoqueCentral real.
    private EstoqueCentralResponseDTO montarResponseDTO() {
        EstoqueCentralResponseDTO dto = new EstoqueCentralResponseDTO();
        dto.setId(ESTOQUE_PUBLIC_ID);
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setUnidadeNome("Instituto de Química");
        dto.setUnidadeSigla("IQ");
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setProdutoNome("Etanol Absoluto PA");
        dto.setProdutoCodigoReferencia("ETL-ABS-001");
        dto.setProdutoLocalizacaoFisica("Armário B - Prateleira 1");
        dto.setProdutoUnidadeArmazenamento("frasco de 1L");
        dto.setProdutoUnidadeMedida(UnidadeMedida.L);
        dto.setQuantidadeAtual(10);
        dto.setQuantidadeMinima(5);
        dto.setAtivo(true);
        return dto;
    }

    @Test
    void deveCriarEstoqueERetornar201() throws Exception {
        EstoqueCentralRequestDTO dto = montarRequestDTO();
        when(estoqueCentralService.criar(any(EstoqueCentralRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        // EstoqueCentralController usa ResponseEntity.status(HttpStatus.CREATED)
        // explicitamente (EstoqueCentralController.java linha 51) - por isso o teste
        // espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.produtoNome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // EstoqueCentralRequestDTO exige unidadeId, produtoId e quantidadeMinima
        // (todos @NotNull) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTodosOsEstoquesERetornar200() throws Exception {
        when(estoqueCentralService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(ESTOQUE_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(estoqueCentralService.buscarPorId(ESTOQUE_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", ESTOQUE_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoNome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveRetornar404QuandoEstoqueNaoEncontrado() throws Exception {
        when(estoqueCentralService.buscarPorId(ESTOQUE_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Estoque central", ESTOQUE_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", ESTOQUE_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorUnidadeERetornar200() throws Exception {
        when(estoqueCentralService.listarPorUnidade(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-unidade")
                        .param("unidadeId", UNIDADE_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].unidadeId").value(UNIDADE_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorUnidadeEProdutoERetornar200() throws Exception {
        when(estoqueCentralService.buscarPorUnidadeEProduto(UNIDADE_PUBLIC_ID, PRODUTO_PUBLIC_ID))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/por-unidade-produto")
                        .param("unidadeId", UNIDADE_PUBLIC_ID.toString())
                        .param("produtoId", PRODUTO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoId").value(PRODUTO_PUBLIC_ID.toString()));
    }

    @Test
    void deveAtualizarEstoqueERetornar200() throws Exception {
        EstoqueCentralRequestDTO dto = montarRequestDTO();
        when(estoqueCentralService.atualizar(eq(ESTOQUE_PUBLIC_ID), any(EstoqueCentralRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}", ESTOQUE_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeMinima").value(5));
    }

    @Test
    void deveDeletarEstoqueERetornar204() throws Exception {
        doNothing().when(estoqueCentralService).deletar(ESTOQUE_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", ESTOQUE_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(estoqueCentralService).deletar(ESTOQUE_PUBLIC_ID);
    }

    @Test
    void deveListarEstoqueBaixoERetornar200() throws Exception {
        when(estoqueCentralService.listarEstoqueBaixoPorUnidade(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/estoque-baixo")
                        .param("unidadeId", UNIDADE_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
