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

import com.sgl.dto.request.LaboratorioRequestDTO;
import com.sgl.dto.response.LaboratorioResponseDTO;
import com.sgl.service.LaboratorioService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Laboratórios", description = "Operações de cadastro e consulta dos laboratórios vinculados às unidades.")
@RestController
@RequestMapping("/api/v1/laboratorios")
@RequiredArgsConstructor
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    @GetMapping
    public ResponseEntity<List<LaboratorioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(laboratorioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaboratorioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(laboratorioService.buscarPorId(id));
    }

    @GetMapping("/por-unidade")
    public ResponseEntity<List<LaboratorioResponseDTO>> listarPorUnidade(
            @RequestParam UUID unidadeId) {
        return ResponseEntity.ok(laboratorioService.listarPorUnidade(unidadeId));
    }

    @PostMapping
    public ResponseEntity<LaboratorioResponseDTO> criar(
            @Valid @RequestBody LaboratorioRequestDTO dto) {
        LaboratorioResponseDTO novoLaboratorio = laboratorioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoLaboratorio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaboratorioResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody LaboratorioRequestDTO dto) {
        return ResponseEntity.ok(laboratorioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        laboratorioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
