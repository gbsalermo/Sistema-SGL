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
import com.sgl.model.enums.StatusPedido;
import com.sgl.service.PedidoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pedidos", description = "Operações de criação, consulta e fluxo de aprovação, rejeição, entrega e cancelamento de pedidos.")
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @Operation(summary = "Criar pedido", description = "Cria um novo pedido de materiais para um laboratório, podendo estar vinculado a um projeto.")
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@Valid @RequestBody PedidoRequestDTO dto) {
        PedidoResponseDTO criado = pedidoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @Operation(summary = "Listar todos os pedidos", description = "Retorna todos os pedidos cadastrados no sistema.")
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @Operation(summary = "Listar pedidos por usuário", description = "Retorna os pedidos vinculados ao usuário informado pelo identificador público UUID.")
    @GetMapping("/por-usuario")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorUsuario(@RequestParam UUID usuarioId) {
        return ResponseEntity.ok(pedidoService.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Buscar pedido por ID", description = "Retorna um pedido específico pelo seu identificador público UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @Operation(summary = "Listar pedidos por status", description = "Retorna os pedidos filtrados pelo status informado.")
    @GetMapping("/por-status")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorStatus(@RequestParam StatusPedido status) {
        return ResponseEntity.ok(pedidoService.listarPorStatus(status));
    }

    @Operation(summary = "Listar pedidos por projeto e período", description = "Retorna os pedidos de um laboratório e projeto dentro do período informado.")
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
    @PutMapping("/{id}/aprovar")
    public ResponseEntity<PedidoResponseDTO> aprovar(
            @PathVariable UUID id,
            @Valid @RequestBody AprovarPedidoRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.aprovar(id, dto));
    }

    @Operation(summary = "Rejeitar pedido", description = "Rejeita um pedido pendente, permitindo informar uma observação opcional.")
    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<PedidoResponseDTO> rejeitar(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacao) {
        return ResponseEntity.ok(pedidoService.rejeitar(id, observacao));
    }

    @Operation(summary = "Registrar entrega do pedido", description = "Marca como entregue um pedido previamente aprovado. A entrega não realiza nova baixa de estoque.")
    @PutMapping("/{id}/entregar")
    public ResponseEntity<PedidoResponseDTO> entregar(@PathVariable UUID id) {
        return ResponseEntity.ok(pedidoService.entregar(id));
    }

    @Operation(summary = "Cancelar pedido", description = "Cancela um pedido e, quando ele já foi aprovado, restaura os lotes exatos anteriormente consumidos.")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelar(
            @PathVariable UUID id,
            @RequestParam(required = false) String observacao) {
        return ResponseEntity.ok(pedidoService.cancelar(id, observacao));
    }
}
