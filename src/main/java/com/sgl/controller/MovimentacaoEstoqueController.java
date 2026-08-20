package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.DescarteProdutoRequestDTO;
import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Usuario;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.UsuarioRepository;
import com.sgl.service.MovimentacaoEstoqueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Movimentações de Estoque", description = "Consultas e operações físicas de entrada, saída e descarte de materiais.")
@RestController
@RequestMapping("/api/v1/movimentacoes")
@RequiredArgsConstructor
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Listar movimentações", description = "Retorna todas as movimentações de estoque registradas no sistema.")
    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodos() {
        return ResponseEntity.ok(movimentacaoService.listarTodos());
    }

    @Operation(summary = "Buscar movimentação por ID", description = "Retorna uma movimentação pelo seu identificador público UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(movimentacaoService.buscarPorId(id));
    }

    @Operation(summary = "Listar movimentações por produto", description = "Retorna as movimentações vinculadas ao produto informado.")
    @GetMapping("/produto")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorProduto(
            @RequestParam UUID produtoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorProduto(produtoId));
    }

    @Operation(summary = "Listar movimentações por laboratório", description = "Retorna as movimentações vinculadas ao laboratório informado.")
    @GetMapping("/laboratorio")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorLaboratorio(
            @RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorLaboratorio(laboratorioId));
    }

    @Operation(summary = "Listar movimentações por usuário", description = "Retorna as movimentações registradas pelo usuário informado.")
    @GetMapping("/usuario")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorUsuario(
            @RequestParam UUID usuarioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Listar movimentações por pedido", description = "Retorna as movimentações geradas pelo pedido informado.")
    @GetMapping("/pedido")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorPedido(
            @RequestParam UUID pedidoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorPedido(pedidoId));
    }

    @Operation(summary = "Listar movimentações por tipo", description = "Retorna as movimentações filtradas pelo tipo informado.")
    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorTipo(
            @RequestParam TipoMovimentacao tipo) {
        return ResponseEntity.ok(movimentacaoService.listarPorTipo(tipo));
    }

    // usuarioId temporário até a autenticação fornecer o usuário atual.
    @Operation(summary = "Registrar entrada de lote", description = "Registra a entrada física de um novo lote no estoque e gera a movimentação correspondente.")
    @PostMapping("/estoques/{estoqueId}/lotes")
    public ResponseEntity<LoteResponseDTO> registrarEntradaLote(
            @PathVariable UUID estoqueId,
            @RequestParam UUID usuarioId,
            @Valid @RequestBody EntradaLoteRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        LoteResponseDTO lote = movimentacaoService.registrarEntradaLote(
                estoqueId,
                dto,
                usuario
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(lote);
    }

    // usuarioId temporário até a autenticação fornecer o usuário atual.
    @Operation(summary = "Descartar produto vencido", description = "Registra o descarte de quantidade vencida do estoque e gera as movimentações correspondentes.")
    @PostMapping("/estoques/{estoqueId}/descarte-vencimento")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> descartarVencidos(
            @PathVariable UUID estoqueId,
            @RequestParam UUID usuarioId,
            @Valid @RequestBody DescarteProdutoRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        return ResponseEntity.ok(
                movimentacaoService.registrarDescarteVencimento(
                        estoqueId,
                        dto.getQuantidade(),
                        dto.getJustificativa(),
                        usuario
                )
        );
    }
}
