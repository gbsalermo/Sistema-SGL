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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/movimentacoes")
@RequiredArgsConstructor
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodos() {
        return ResponseEntity.ok(movimentacaoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(movimentacaoService.buscarPorId(id));
    }

    @GetMapping("/produto")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorProduto(
            @RequestParam UUID produtoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorProduto(produtoId));
    }

    @GetMapping("/laboratorio")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorLaboratorio(
            @RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorLaboratorio(laboratorioId));
    }

    @GetMapping("/usuario")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorUsuario(
            @RequestParam UUID usuarioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/pedido")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorPedido(
            @RequestParam UUID pedidoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorPedido(pedidoId));
    }

    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorTipo(
            @RequestParam TipoMovimentacao tipo) {
        return ResponseEntity.ok(movimentacaoService.listarPorTipo(tipo));
    }

    // usuarioId temporário até a autenticação fornecer o usuário atual.
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
