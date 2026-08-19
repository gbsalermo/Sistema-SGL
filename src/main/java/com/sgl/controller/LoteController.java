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

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.request.AtualizarLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.service.LoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping
    public ResponseEntity<List<LoteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(loteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(loteService.buscarPorId(id));
    }

    @GetMapping("/por-estoque")
    public ResponseEntity<List<LoteResponseDTO>> listarPorEstoque(@RequestParam UUID estoqueId) {
        return ResponseEntity.ok(loteService.listarPorEstoque(estoqueId));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<LoteResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarLoteRequestDTO request) {

        AtualizarLoteDTO dto = new AtualizarLoteDTO();
        dto.setNumeroLote(request.getNumeroLote());
        dto.setDataValidade(request.getDataValidade());
        dto.setAtivo(request.getAtivo());

        return ResponseEntity.ok(loteService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        loteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
