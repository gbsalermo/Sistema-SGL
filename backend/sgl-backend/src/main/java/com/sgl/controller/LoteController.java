package com.sgl.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.service.LoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    public ResponseEntity<List<LoteDTO>> listarTodos() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.buscarPorId(id));
    }

    @GetMapping("/por-estoque")
    public ResponseEntity<List<LoteDTO>> listarPorEstoque(
            @RequestParam Long estoqueId) {
        return ResponseEntity.ok(loteService.listarPorEstoque(estoqueId));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<LoteDTO>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarLoteDTO dto) {
        return ResponseEntity.ok(loteService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        loteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
