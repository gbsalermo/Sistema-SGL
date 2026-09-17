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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.ClasseResiduoRequestDTO;
import com.sgl.dto.response.ClasseResiduoResponseDTO;
import com.sgl.service.ClasseResiduoService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/classes-residuo")
@RequiredArgsConstructor
public class ClasseResiduoController {

    private final ClasseResiduoService classeResiduoService;

    @PostMapping
    public ResponseEntity<ClasseResiduoResponseDTO> criar(
            @Valid @RequestBody ClasseResiduoRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(classeResiduoService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<ClasseResiduoResponseDTO>>
            listarTodos() {

        return ResponseEntity.ok(
                classeResiduoService.listarTodos()
        );
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<ClasseResiduoResponseDTO>>
            listarAtivos() {

        return ResponseEntity.ok(
                classeResiduoService.listarAtivos()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClasseResiduoResponseDTO>
            buscarPorId(@PathVariable UUID id) {

        return ResponseEntity.ok(
                classeResiduoService.buscarPorId(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClasseResiduoResponseDTO>
            atualizar(
                    @PathVariable UUID id,
                    @Valid
                    @RequestBody
                    ClasseResiduoRequestDTO dto) {

        return ResponseEntity.ok(
                classeResiduoService.atualizar(id, dto)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id) {

        classeResiduoService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}

