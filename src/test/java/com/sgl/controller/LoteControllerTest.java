package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.AtualizarLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.service.LoteService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link LoteController}. Segue o mesmo
 * padrão fixado em EstagiarioControllerTest/ProdutoControllerTest (ver notas de
 * configuração lá): bean de {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link LoteService} é mockado - toda a regra de tenant/negócio (FIFO/FEFO,
 * fracionamento, imutabilidade do código interno etc., já coberta em
 * LoteServiceTest) não é exercitada aqui, só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(LoteController.class)
@Import(SecurityConfig.class)
class LoteControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // Lote tem campo dataValidade (LocalDate) - o ObjectMapper "cru" do slice
            // não vem com o JavaTimeModule registrado (isso é feito pela
            // JacksonAutoConfiguration real, que este @WebMvcTest não sobe), então é
            // preciso registrar explicitamente aqui (mesmo achado do Batch C2, com
            // EstagiarioRequestDTO).
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/lotes";

    private static final UUID LOTE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ESTOQUE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUTO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoteService loteService;

    private AtualizarLoteRequestDTO montarRequestDTO() {
        AtualizarLoteRequestDTO dto = new AtualizarLoteRequestDTO();
        dto.setNumeroLote("FAB-2026-8841");
        dto.setTipoEmbalagem(TipoEmbalagem.KIT);
        dto.setApresentacao("kit com 50 unidades");
        dto.setFracionavel(true);
        dto.setObservacao("Material recebido lacrado.");
        dto.setDataValidade(LocalDate.of(2027, 8, 31));
        dto.setAtivo(true);
        return dto;
    }

    // LoteResponseDTO tem @NoArgsConstructor + @Setter, então montamos o DTO
    // diretamente sem precisar de uma entidade Lote/EstoqueCentral real.
    private LoteResponseDTO montarResponseDTO() {
        LoteResponseDTO dto = new LoteResponseDTO();
        dto.setId(LOTE_PUBLIC_ID);
        dto.setEstoqueCentralId(ESTOQUE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setProdutoNome("Etanol Absoluto PA");
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setUnidadeNome("Instituto de Química");
        dto.setCodigoInterno("LOT-ETL-ABS-001-001");
        dto.setNumeroLote("FAB-2026-8841");
        dto.setTipoEmbalagem(TipoEmbalagem.KIT);
        dto.setApresentacao("kit com 50 unidades");
        dto.setQuantidadeApresentacoes(2);
        dto.setConteudoPorApresentacao(50);
        dto.setFracionavel(true);
        dto.setObservacao("Material recebido lacrado.");
        dto.setUnidadeBase(UnidadeMedida.L);
        dto.setQuantidadeInicial(100);
        dto.setQuantidadeDisponivel(80);
        dto.setDataEntrada(LocalDate.of(2026, 9, 1));
        dto.setDataValidade(LocalDate.of(2027, 8, 31));
        dto.setAtivo(true);
        return dto;
    }

    @Test
    void deveListarTodosOsLotesERetornar200() throws Exception {
        when(loteService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(LOTE_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarLotePorIdERetornar200() throws Exception {
        when(loteService.buscarPorId(LOTE_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", LOTE_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLote").value("FAB-2026-8841"));
    }

    @Test
    void deveRetornar404QuandoLoteNaoEncontrado() throws Exception {
        when(loteService.buscarPorId(LOTE_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Lote", LOTE_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", LOTE_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorEstoqueERetornar200() throws Exception {
        when(loteService.listarPorEstoque(ESTOQUE_PUBLIC_ID)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-estoque")
                        .param("estoqueId", ESTOQUE_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].estoqueCentralId").value(ESTOQUE_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarVencidosERetornar200() throws Exception {
        when(loteService.listarVencidos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/vencidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveAtualizarLoteERetornar200() throws Exception {
        AtualizarLoteRequestDTO dto = montarRequestDTO();
        when(loteService.atualizar(eq(LOTE_PUBLIC_ID), any(AtualizarLoteRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}", LOTE_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLote").value("FAB-2026-8841"));
    }

    @Test
    void deveRetornar400AoAtualizarComCorpoInvalido() throws Exception {
        // AtualizarLoteRequestDTO exige numeroLote (@NotBlank) - corpo vazio o viola.
        mockMvc.perform(put(BASE_URL + "/{id}", LOTE_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveInativarLoteERetornar204() throws Exception {
        // LoteController.inativar está mapeado como @DeleteMapping, mas o Service
        // chamado é loteService.inativar (LoteController.java linha 66) - não há
        // exclusão física do registro.
        doNothing().when(loteService).inativar(LOTE_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", LOTE_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(loteService).inativar(LOTE_PUBLIC_ID);
    }
}
