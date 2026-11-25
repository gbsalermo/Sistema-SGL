package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sgl.dto.request.BolsaRequestDTO;
import com.sgl.dto.response.BolsaResponseDTO;
import com.sgl.service.BolsaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bolsas", description = "Catálogo de bolsas cadastradas pela Unidade.")
@RestController
@RequestMapping("/api/v1/bolsas")
@RequiredArgsConstructor
public class BolsaController {

    private final BolsaService service;

    @GetMapping
    public ResponseEntity<List<BolsaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/ativas")
    public ResponseEntity<List<BolsaResponseDTO>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @PostMapping
    public ResponseEntity<BolsaResponseDTO> criar(
            @Valid @RequestBody BolsaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BolsaResponseDTO> atualizar(
            @PathVariable UUID id, @Valid @RequestBody BolsaRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
