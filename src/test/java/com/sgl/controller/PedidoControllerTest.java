package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.request.ItemPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.service.PedidoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link PedidoController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (ver
 * controllers-context.md): {@code @MockitoBean} no lugar de
 * {@code @MockBean}, {@code ObjectMapper} próprio via
 * {@code @TestConfiguration} (com {@code JavaTimeModule}, já que
 * {@link PedidoResponseDTO} tem campos {@code LocalDateTime}) e
 * {@code @Import(SecurityConfig.class)}.
 *
 * {@link PedidoResponseDTO} tem {@code @NoArgsConstructor} + {@code @Setter},
 * então é montado diretamente sem precisar de uma entidade real - mesmo
 * padrão do LoteControllerTest/MovimentacaoEstoqueControllerTest.
 *
 * Toda a regra de negócio de concorrência, FIFO/FEFO e transições de status
 * (docs/testes.md seções 9-14, 18) já está coberta em
 * PedidoServiceTest/PedidoConcorrenciaIntegrationTest; aqui o foco é só
 * roteamento HTTP, serialização e validação de request (@Valid).
 */
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/pedidos";

    private static final UUID PEDIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USUARIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ITEM_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PedidoService pedidoService;

    // PedidoResponseDTO tem @NoArgsConstructor + @Setter, então montamos o
    // DTO diretamente sem precisar de uma entidade real.
    private PedidoResponseDTO montarPedidoResponseDTO(StatusPedido status) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(PEDIDO_PUBLIC_ID);
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setUsuarioNome("Maria Oliveira");
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setLaboratorioNome("Laboratório de Química Orgânica");
        dto.setDataSolicitacao(LocalDateTime.of(2026, 8, 20, 10, 0));
        dto.setStatus(status);
        dto.setUrgente(false);
        dto.setObservacao("Materiais destinados ao experimento da próxima semana.");
        dto.setItens(List.of());
        return dto;
    }

    private PedidoRequestDTO montarPedidoRequestDTO() {
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setProdutoId(PRODUTO_PUBLIC_ID);
        item.setQuantidadeSolicitada(50);
        item.setTipoEmbalagemSolicitada(TipoEmbalagem.KIT);
        item.setQuantidadeEmbalagensSolicitada(1);
        item.setMultiplicadorSolicitado(50);

        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(USUARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setUrgente(false);
        dto.setObservacao("Materiais destinados ao experimento da próxima semana.");
        dto.setItens(List.of(item));
        return dto;
    }

    private AprovarPedidoRequestDTO montarAprovarRequestDTO() {
        AprovarPedidoRequestDTO.ItemAprovacaoDTO item =
                new AprovarPedidoRequestDTO.ItemAprovacaoDTO(ITEM_PUBLIC_ID, 50);

        AprovarPedidoRequestDTO dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(USUARIO_PUBLIC_ID);
        dto.setObservacao("Aprovado para atendimento.");
        dto.setItens(List.of(item));
        return dto;
    }

    @Test
    void deveCriarPedidoERetornar201() throws Exception {
        when(pedidoService.criar(any(PedidoRequestDTO.class)))
                .thenReturn(montarPedidoResponseDTO(StatusPedido.PENDENTE));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarPedidoRequestDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PEDIDO_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void deveRetornar400AoCriarPedidoComCorpoInvalido() throws Exception {
        // PedidoRequestDTO exige usuarioId, laboratorioId e itens (@NotNull/
        // @NotEmpty) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTodosERetornar200() throws Exception {
        when(pedidoService.listarTodos()).thenReturn(List.of(montarPedidoResponseDTO(StatusPedido.PENDENTE)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(PEDIDO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorUsuarioERetornar200() throws Exception {
        when(pedidoService.listarPorUsuario(USUARIO_PUBLIC_ID))
                .thenReturn(List.of(montarPedidoResponseDTO(StatusPedido.PENDENTE)));

        mockMvc.perform(get(BASE_URL + "/por-usuario").param("usuarioId", USUARIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(USUARIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(pedidoService.buscarPorId(PEDIDO_PUBLIC_ID)).thenReturn(montarPedidoResponseDTO(StatusPedido.PENDENTE));

        mockMvc.perform(get(BASE_URL + "/{id}", PEDIDO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laboratorioNome").value("Laboratório de Química Orgânica"));
    }

    @Test
    void deveRetornar404QuandoPedidoNaoEncontrado() throws Exception {
        when(pedidoService.buscarPorId(PEDIDO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Pedido", PEDIDO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", PEDIDO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorStatusERetornar200() throws Exception {
        when(pedidoService.listarPorStatus(StatusPedido.PENDENTE))
                .thenReturn(List.of(montarPedidoResponseDTO(StatusPedido.PENDENTE)));

        mockMvc.perform(get(BASE_URL + "/por-status").param("status", "PENDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    @Test
    void deveListarPorUrgenciaERetornar200() throws Exception {
        when(pedidoService.listarPorUrgencia(true))
                .thenReturn(List.of(montarPedidoResponseDTO(StatusPedido.PENDENTE)));

        mockMvc.perform(get(BASE_URL + "/por-urgencia").param("urgente", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveListarPorProjetoEPeriodoERetornar200() throws Exception {
        LocalDate dataInicio = LocalDate.of(2026, 8, 1);
        LocalDate dataFim = LocalDate.of(2026, 8, 31);
        when(pedidoService.listarPorProjetoEPeriodo(LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID, dataInicio, dataFim))
                .thenReturn(List.of(montarPedidoResponseDTO(StatusPedido.PENDENTE)));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo",
                        LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornar400AoConsultarComPeriodoInvertido() throws Exception {
        // Mensagem exata lançada por PedidoService.validarPeriodo().
        LocalDate dataInicio = LocalDate.of(2026, 8, 31);
        LocalDate dataFim = LocalDate.of(2026, 8, 1);
        when(pedidoService.listarPorProjetoEPeriodo(LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID, dataInicio, dataFim))
                .thenThrow(new BusinessRuleException("A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get(BASE_URL + "/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo",
                        LABORATORIO_PUBLIC_ID, PROJETO_PUBLIC_ID)
                        .param("dataInicio", dataInicio.toString())
                        .param("dataFim", dataFim.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAprovarPedidoERetornar200() throws Exception {
        when(pedidoService.aprovar(any(UUID.class), any(AprovarPedidoRequestDTO.class)))
                .thenReturn(montarPedidoResponseDTO(StatusPedido.APROVADO));

        mockMvc.perform(put(BASE_URL + "/{id}/aprovar", PEDIDO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarAprovarRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void deveRetornar400AoAprovarPedidoJaAprovado() throws Exception {
        // Mensagem exata lançada por PedidoService.aprovar() quando o pedido
        // não está mais PENDENTE.
        when(pedidoService.aprovar(any(UUID.class), any(AprovarPedidoRequestDTO.class)))
                .thenThrow(new BusinessRuleException(
                        "Apenas pedidos PENDENTES podem ser aprovados. Status atual: APROVADO"));

        mockMvc.perform(put(BASE_URL + "/{id}/aprovar", PEDIDO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarAprovarRequestDTO())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarPedidoERetornar200() throws Exception {
        when(pedidoService.rejeitar(PEDIDO_PUBLIC_ID, "Estoque insuficiente."))
                .thenReturn(montarPedidoResponseDTO(StatusPedido.REJEITADO));

        mockMvc.perform(put(BASE_URL + "/{id}/rejeitar", PEDIDO_PUBLIC_ID)
                        .param("observacao", "Estoque insuficiente."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADO"));
    }

    @Test
    void deveEntregarPedidoERetornar200() throws Exception {
        when(pedidoService.entregar(PEDIDO_PUBLIC_ID)).thenReturn(montarPedidoResponseDTO(StatusPedido.ENTREGUE));

        mockMvc.perform(put(BASE_URL + "/{id}/entregar", PEDIDO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void deveCancelarPedidoERetornar200() throws Exception {
        when(pedidoService.cancelar(PEDIDO_PUBLIC_ID, "Não será mais necessário."))
                .thenReturn(montarPedidoResponseDTO(StatusPedido.CANCELADO));

        mockMvc.perform(put(BASE_URL + "/{id}/cancelar", PEDIDO_PUBLIC_ID)
                        .param("observacao", "Não será mais necessário."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }
}
