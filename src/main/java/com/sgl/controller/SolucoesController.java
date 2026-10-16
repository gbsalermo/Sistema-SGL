package com.sgl.controller;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.sgl.dto.request.*;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.model.enums.TipoPedido;
import com.sgl.service.PedidoService;
import com.sgl.exception.BusinessRuleException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/solucoes/pedidos")
@RequiredArgsConstructor
public class SolucoesController {
    private final PedidoService service;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(@Valid @RequestBody PedidoRequestDTO dto) {
        if (dto.getTipo() != TipoPedido.SOLUCAO)
            throw new BusinessRuleException("Informe tipo SOLUCAO.");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }
    @GetMapping
    public List<PedidoResponseDTO> listar() { return service.listarSolucoes(); }
    @GetMapping("/{id}")
    public PedidoResponseDTO buscar(@PathVariable UUID id) { return service.buscarSolucao(id); }
    @PutMapping("/{id}/aprovar")
    public PedidoResponseDTO aprovar(@PathVariable UUID id, @Valid @RequestBody AprovarPedidoRequestDTO dto) {
        return service.aprovarSolucao(id, dto);
    }
    @PutMapping("/{id}/entregar")
    public PedidoResponseDTO entregar(@PathVariable UUID id) { return service.entregarSolucao(id); }
    @PutMapping("/{id}/cancelar")
    public PedidoResponseDTO cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarSolucaoRequestDTO dto) {
        return service.cancelarSolucao(id, dto.getPreparada(), dto.getJustificativa());
    }
    @PutMapping("/{id}/rejeitar")
    public PedidoResponseDTO rejeitar(@PathVariable UUID id, @RequestParam(required=false) String observacao) {
        service.buscarSolucao(id);
        return service.rejeitar(id, observacao);
    }
}
