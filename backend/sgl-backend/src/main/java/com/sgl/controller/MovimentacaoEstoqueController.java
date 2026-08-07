package com.sgl.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.DescarteProdutoDTO;
import com.sgl.dto.EntradaLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
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
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarTodos() {
        return ResponseEntity.ok(movimentacaoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueDTO> buscarPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(movimentacaoService.buscarPorId(id));
    }

    @GetMapping("/produto")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarPorProduto(
            @RequestParam Long produtoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorProduto(produtoId));
    }

    @GetMapping("/laboratorio")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarPorLaboratorio(
            @RequestParam Long laboratorioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorLaboratorio(laboratorioId));
    }

    @GetMapping("/usuario")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarPorUsuario(
            @RequestParam Long usuarioId) {
        return ResponseEntity.ok(movimentacaoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/pedido")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarPorPedido(
            @RequestParam Long pedidoId) {
        return ResponseEntity.ok(movimentacaoService.listarPorPedido(pedidoId));
    }

    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> listarPorTipo(
            @RequestParam TipoMovimentacao tipo) {
        return ResponseEntity.ok(movimentacaoService.listarPorTipo(tipo));
    }

    /**
     * Endpoint temporário para desenvolvimento local.
     * O usuarioId será substituído pelo contexto autenticado posteriormente.
     */
    @PostMapping("/estoques/{estoqueId}/lotes")
    public ResponseEntity<LoteDTO> registrarEntradaLote(
            @PathVariable Long estoqueId,
            @RequestParam Long usuarioId,
            @Valid @RequestBody EntradaLoteDTO dto) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        LoteDTO lote = movimentacaoService.registrarEntradaLote(
                estoqueId,
                dto,
                usuario
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(lote);
    }

    /**
     * Endpoint temporário para desenvolvimento local.
     * O usuarioId será substituído pelo contexto autenticado posteriormente.
     */
    @PostMapping("/estoques/{estoqueId}/descarte-vencimento")
    public ResponseEntity<List<MovimentacaoEstoqueDTO>> descartarVencidos(
            @PathVariable Long estoqueId,
            @RequestParam Long usuarioId,
            @Valid @RequestBody DescarteProdutoDTO dto) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
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
