package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sgl.dto.request.ModalidadeBolsaRequestDTO;
import com.sgl.dto.response.ModalidadeBolsaResponseDTO;
import com.sgl.service.ModalidadeBolsaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Modalidades de bolsa", description = "Catalogo de modalidades de bolsa por Unidade.")
@RestController
@RequestMapping("/api/v1/modalidades-bolsa")
@RequiredArgsConstructor
public class ModalidadeBolsaController {

    private final ModalidadeBolsaService service;

    @GetMapping
    public ResponseEntity<List<ModalidadeBolsaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/ativas")
    public ResponseEntity<List<ModalidadeBolsaResponseDTO>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }

    @PostMapping
    public ResponseEntity<ModalidadeBolsaResponseDTO> criar(
            @Valid @RequestBody ModalidadeBolsaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ModalidadeBolsaResponseDTO> atualizar(
            @PathVariable UUID id, @Valid @RequestBody ModalidadeBolsaRequestDTO dto) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
