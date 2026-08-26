package com.sgl.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.model.enums.StatusPedido;
import com.sgl.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pedidos", description = "Operações de criação, consulta e fluxo de aprovação, rejeição, entrega e cancelamento de pedidos.")
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @Operation(summary = "Criar pedido", description = "Cria um novo pedido de materiais para um laboratório, podendo estar vinculado a um projeto e marcado como urgente apenas para fins informativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário, laboratório, projeto, produto ou estoque relacionado não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de integridade dos dados",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO criado = pedidoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @Operation(summary = "Listar todos os pedidos", description = "Retorna todos os pedidos cadastrados no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PedidoResponseDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @Operation(summary = "Listar pedidos por usuário", description = "Retorna os pedidos vinculados ao usuário informado pelo identificador público UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos do usuário retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PedidoResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-usuario")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorUsuario(@RequestParam UUID usuarioId) {
        return ResponseEntity.ok(pedidoService.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Buscar pedido por ID", description = "Retorna um pedido específico pelo seu identificador público UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @Operation(summary = "Listar pedidos por status", description = "Retorna os pedidos filtrados pelo status informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos filtrados retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PedidoResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Status informado é inválido",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-status")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorStatus(@RequestParam StatusPedido status) {
        return ResponseEntity.ok(pedidoService.listarPorStatus(status));
    }

    @Operation(summary = "Listar pedidos por urgência", description = "Retorna pedidos filtrados pela marcação informativa de urgência. A urgência não altera o fluxo operacional do pedido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos filtrados por urgência retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PedidoResponseDTO.class)))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-urgencia")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorUrgencia(@RequestParam Boolean urgente) {
        return ResponseEntity.ok(pedidoService.listarPorUrgencia(urgente));
    }

    @Operation(summary = "Listar pedidos por projeto e período", description = "Retorna os pedidos de um laboratório e projeto dentro do período informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos do projeto e período retornados com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PedidoResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Período inválido ou projeto não pertence ao laboratório informado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório ou projeto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorProjetoEPeriodo(
            @PathVariable UUID laboratorioId,
            @PathVariable UUID projetoId,
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {

        return ResponseEntity.ok(
                pedidoService.listarPorProjetoEPeriodo(
                        laboratorioId,
                        projetoId,
                        dataInicio,
                        dataFim
                )
        );
    }

    @Operation(summary = "Aprovar pedido", description = "Aprova um pedido pendente, valida as quantidades aprovadas e realiza a baixa do estoque utilizando FEFO para produtos perecíveis e FIFO para não perecíveis.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido aprovado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, pedido em status incompatível ou estoque insuficiente",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido, aprovador, item ou estoque relacionado não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/aprovar")
    public ResponseEntity<PedidoResponseDTO> aprovar(
            @PathVariable UUID id,
            @Valid @RequestBody AprovarPedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.aprovar(id, dto));
    }

    @Operation(summary = "Rejeitar pedido", description = "Rejeita um pedido pendente, permitindo informar uma observação opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido rejeitado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pedido não está em status compatível para rejeição",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<PedidoResponseDTO> rejeitar(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacao) {
        return ResponseEntity.ok(pedidoService.rejeitar(id, observacao));
    }

    @Operation(summary = "Registrar entrega do pedido", description = "Marca como entregue um pedido previamente aprovado. A entrega não realiza nova baixa de estoque.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega registrada com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pedido não está aprovado para entrega",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/entregar")
    public ResponseEntity<PedidoResponseDTO> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.entregar(id));
    }

    @Operation(summary = "Cancelar pedido", description = "Cancela um pedido e, quando ele já foi aprovado, restaura os lotes exatos anteriormente consumidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Pedido está em status incompatível com o cancelamento",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelar(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacao) {
        return ResponseEntity.ok(pedidoService.cancelar(id, observacao));
    }
}
