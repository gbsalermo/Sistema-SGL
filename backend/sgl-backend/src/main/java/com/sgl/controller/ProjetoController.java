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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.service.ProjetoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> criar(@Valid @RequestBody ProjetoRequestDTO dto) {
        ProjetoResponseDTO criado = projetoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(projetoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<ProjetoResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(projetoService.listarPorLaboratorio(laboratorioId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProjetoRequestDTO dto) {
        return ResponseEntity.ok(projetoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        projetoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ProjetoResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(projetoService.listarAtivos());
    }
}
