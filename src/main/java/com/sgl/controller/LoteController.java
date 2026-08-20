package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.AtualizarLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.service.LoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Lotes", description = "Operações de consulta, atualização e inativação dos lotes de estoque.")
@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @Operation(summary = "Listar lotes", description = "Retorna todos os lotes cadastrados no sistema.")
    @GetMapping
    public ResponseEntity<List<LoteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @Operation(summary = "Buscar lote por ID", description = "Retorna um lote pelo seu identificador público UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(loteService.buscarPorId(id));
    }

    @Operation(summary = "Listar lotes por estoque", description = "Retorna os lotes vinculados ao registro de estoque informado.")
    @GetMapping("/por-estoque")
    public ResponseEntity<List<LoteResponseDTO>> listarPorEstoque(@RequestParam UUID estoqueId) {
        return ResponseEntity.ok(loteService.listarPorEstoque(estoqueId));
    }

    @Operation(summary = "Listar lotes vencidos", description = "Retorna os lotes cuja data de validade já foi ultrapassada.")
    @GetMapping("/vencidos")
    public ResponseEntity<List<LoteResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @Operation(summary = "Atualizar lote", description = "Atualiza os dados permitidos do lote identificado pelo UUID informado.")
    @PutMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarLoteRequestDTO dto) {
        return ResponseEntity.ok(loteService.atualizar(id, dto));
    }

    @Operation(summary = "Inativar lote", description = "Inativa o lote identificado pelo UUID informado.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        loteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
