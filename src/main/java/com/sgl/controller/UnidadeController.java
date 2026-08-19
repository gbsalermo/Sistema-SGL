package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.service.UnidadeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listarTodos() {
        return ResponseEntity.ok(unidadeService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(unidadeService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> criar(
            @Valid @RequestBody UnidadeRequestDTO dto) {
        UnidadeResponseDTO novaUnidade = unidadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaUnidade);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody UnidadeRequestDTO dto) {
        return ResponseEntity.ok(unidadeService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        unidadeService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
