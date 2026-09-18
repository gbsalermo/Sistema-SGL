package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import com.sgl.dto.request.DescarteProdutoRequestDTO;
import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.UsuarioRepository;
import com.sgl.service.MovimentacaoEstoqueService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link MovimentacaoEstoqueController}.
 * Segue o mesmo padrão fixado em LoteControllerTest/ProdutoControllerTest (ver notas
 * de configuração lá): bean de {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * Diferente dos demais Controllers deste batch, este injeta DUAS dependências
 * ({@link MovimentacaoEstoqueService} e {@link UsuarioRepository} diretamente,
 * usada para resolver o usuário responsável a partir do {@code usuarioId} de
 * query param antes de delegar ao Service) - por isso precisa de dois
 * {@code @MockitoBean}. Toda a regra de negócio de FIFO/FEFO e descarte por
 * vencimento (docs/testes.md seções 5-8 e 13) já está coberta em
 * MovimentacaoEstoqueServiceTest; aqui o foco é só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(MovimentacaoEstoqueController.class)
@Import(SecurityConfig.class)
class MovimentacaoEstoqueControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // EntradaLoteRequestDTO tem campo dataValidade (LocalDate) - registra o
            // JavaTimeModule pelo mesmo motivo do LoteControllerTest/
            // EstagiarioControllerTest.
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/movimentacoes";

    private static final UUID MOVIMENTACAO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUTO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID USUARIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PEDIDO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID LOTE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID ESTOQUE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovimentacaoEstoqueService movimentacaoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    // MovimentacaoEstoqueResponseDTO tem @NoArgsConstructor + @Setter, então
    // montamos o DTO diretamente sem precisar de uma entidade real.
    private MovimentacaoEstoqueResponseDTO montarResponseDTO() {
        MovimentacaoEstoqueResponseDTO dto = new MovimentacaoEstoqueResponseDTO();
        dto.setId(MOVIMENTACAO_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setProdutoNome("Etanol Absoluto PA");
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setLaboratorioNome("Laboratório de Química Orgânica");
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setUsuarioNome("Maria Oliveira");
        dto.setEstoqueCentralId(ESTOQUE_PUBLIC_ID);
        dto.setPedidoId(PEDIDO_PUBLIC_ID);
        dto.setLoteId(LOTE_PUBLIC_ID);
        dto.setCodigoInternoLote("LOT-ETL-ABS-001-001");
        dto.setNumeroLote("FAB-2026-8841");
        dto.setTipoMovimentacao(TipoMovimentacao.ENTRADA);
        dto.setOrigem(OrigemMovimentacao.COMPRA);
        dto.setQuantidadeMovimentada(100);
        dto.setQuantidadeAnterior(0);
        dto.setQuantidadeAtual(100);
        dto.setObservacao("Material recebido conforme nota fiscal.");
        return dto;
    }

    private Usuario montarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setPublicId(USUARIO_PUBLIC_ID);
        usuario.setNome("Maria Oliveira");
        usuario.setAtivo(true);
        return usuario;
    }

    private EntradaLoteRequestDTO montarEntradaLoteRequestDTO() {
        EntradaLoteRequestDTO dto = new EntradaLoteRequestDTO();
        dto.setNumeroLote("FAB-2026-8841");
        dto.setTipoEmbalagem(TipoEmbalagem.KIT);
        dto.setApresentacao("kit com 50 unidades");
        dto.setQuantidade(2);
        dto.setConteudoPorApresentacao(50);
        dto.setFracionavel(true);
        dto.setDataValidade(LocalDate.of(2027, 8, 31));
        dto.setOrigem(OrigemMovimentacao.COMPRA);
        dto.setObservacao("Material recebido conforme nota fiscal.");
        return dto;
    }

    // LoteResponseDTO tem @NoArgsConstructor + @Setter, então montamos o DTO
    // diretamente (mesmo padrão do LoteControllerTest).
    private LoteResponseDTO montarLoteResponseDTO() {
        LoteResponseDTO dto = new LoteResponseDTO();
        dto.setId(LOTE_PUBLIC_ID);
        dto.setEstoqueCentralId(ESTOQUE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setProdutoNome("Etanol Absoluto PA");
        dto.setNumeroLote("FAB-2026-8841");
        dto.setQuantidadeInicial(100);
        dto.setQuantidadeDisponivel(100);
        dto.setAtivo(true);
        return dto;
    }

    @Test
    void deveListarTodasERetornar200() throws Exception {
        when(movimentacaoService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(MOVIMENTACAO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(movimentacaoService.buscarPorId(MOVIMENTACAO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", MOVIMENTACAO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoNome").value("Etanol Absoluto PA"));
    }

    @Test
    void deveRetornar404QuandoMovimentacaoNaoEncontrada() throws Exception {
        when(movimentacaoService.buscarPorId(MOVIMENTACAO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Movimentação", MOVIMENTACAO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", MOVIMENTACAO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorProdutoERetornar200() throws Exception {
        when(movimentacaoService.listarPorProduto(PRODUTO_PUBLIC_ID)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/produto").param("produtoId", PRODUTO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].produtoId").value(PRODUTO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorLaboratorioERetornar200() throws Exception {
        when(movimentacaoService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/laboratorio").param("laboratorioId", LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorUsuarioERetornar200() throws Exception {
        when(movimentacaoService.listarPorUsuario(USUARIO_PUBLIC_ID)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/usuario").param("usuarioId", USUARIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].usuarioId").value(USUARIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorPedidoERetornar200() throws Exception {
        when(movimentacaoService.listarPorPedido(PEDIDO_PUBLIC_ID)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/pedido").param("pedidoId", PEDIDO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].pedidoId").value(PEDIDO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorLoteERetornar200() throws Exception {
        when(movimentacaoService.listarPorLote(LOTE_PUBLIC_ID)).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/lote").param("loteId", LOTE_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].loteId").value(LOTE_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorTipoERetornar200() throws Exception {
        when(movimentacaoService.listarPorTipo(TipoMovimentacao.ENTRADA))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/tipo").param("tipo", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tipoMovimentacao").value("ENTRADA"));
    }

    @Test
    void deveRegistrarEntradaDeLoteERetornar201() throws Exception {
        Usuario usuario = montarUsuario();
        when(usuarioRepository.findByPublicId(USUARIO_PUBLIC_ID)).thenReturn(Optional.of(usuario));
        when(movimentacaoService.registrarEntradaLote(
                        eq(ESTOQUE_PUBLIC_ID), any(EntradaLoteRequestDTO.class), eq(usuario)))
                .thenReturn(montarLoteResponseDTO());

        // MovimentacaoEstoqueController usa ResponseEntity.status(HttpStatus.CREATED)
        // explicitamente (MovimentacaoEstoqueController.java linha 85) - por isso o
        // teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL + "/estoques/{estoqueId}/lotes", ESTOQUE_PUBLIC_ID)
                        .param("usuarioId", USUARIO_PUBLIC_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarEntradaLoteRequestDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroLote").value("FAB-2026-8841"));
    }

    @Test
    void deveRetornar404AoRegistrarEntradaComUsuarioInexistente() throws Exception {
        when(usuarioRepository.findByPublicId(USUARIO_PUBLIC_ID)).thenReturn(Optional.empty());

        // O próprio Controller resolve o usuário (não delega ao Service) e lança
        // ResourceNotFoundException quando o usuarioId não existe
        // (MovimentacaoEstoqueController.java linhas 82-83).
        mockMvc.perform(post(BASE_URL + "/estoques/{estoqueId}/lotes", ESTOQUE_PUBLIC_ID)
                        .param("usuarioId", USUARIO_PUBLIC_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarEntradaLoteRequestDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400AoRegistrarEntradaComCorpoInvalido() throws Exception {
        // EntradaLoteRequestDTO exige numeroLote, tipoEmbalagem, quantidade e origem
        // (todos @NotBlank/@NotNull) - corpo vazio viola todos. A validação do corpo
        // acontece antes de o Controller consultar o usuarioRepository, então o
        // resultado é 400 independentemente do usuarioId informado.
        mockMvc.perform(post(BASE_URL + "/estoques/{estoqueId}/lotes", ESTOQUE_PUBLIC_ID)
                        .param("usuarioId", USUARIO_PUBLIC_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveDescartarVencidosERetornar200() throws Exception {
        Usuario usuario = montarUsuario();
        when(usuarioRepository.findByPublicId(USUARIO_PUBLIC_ID)).thenReturn(Optional.of(usuario));

        DescarteProdutoRequestDTO dto = new DescarteProdutoRequestDTO(
                5, "Lotes vencidos identificados durante conferência mensal.");

        MovimentacaoEstoqueResponseDTO movimentacao = montarResponseDTO();
        movimentacao.setTipoMovimentacao(TipoMovimentacao.DESCARTE_VENCIMENTO);
        when(movimentacaoService.registrarDescarteVencimento(
                        eq(ESTOQUE_PUBLIC_ID), eq(dto.getQuantidade()), eq(dto.getJustificativa()), eq(usuario)))
                .thenReturn(List.of(movimentacao));

        mockMvc.perform(post(BASE_URL + "/estoques/{estoqueId}/descarte-vencimento", ESTOQUE_PUBLIC_ID)
                        .param("usuarioId", USUARIO_PUBLIC_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tipoMovimentacao").value("DESCARTE_VENCIMENTO"));
    }
}
