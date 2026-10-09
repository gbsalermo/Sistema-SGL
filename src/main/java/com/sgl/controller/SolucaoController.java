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

import com.sgl.dto.request.SolucaoRequestDTO;
import com.sgl.dto.response.SolucaoResponseDTO;
import com.sgl.service.SolucaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
        name = "Soluções",
        description = "Cadastro de soluções e suas composições por Unidade."
)
@RestController
@RequestMapping("/api/v1/solucoes")
@RequiredArgsConstructor
public class SolucaoController {

    private final SolucaoService solucaoService;

    @Operation(summary = "Criar solução")
    @PostMapping
    public ResponseEntity<SolucaoResponseDTO> criar(
            @Valid @RequestBody SolucaoRequestDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solucaoService.criar(dto));
    }

    @Operation(summary = "Listar soluções da unidade atual")
    @GetMapping
    public ResponseEntity<List<SolucaoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(solucaoService.listarTodos());
    }

    @Operation(summary = "Listar soluções ativas da unidade atual")
    @GetMapping("/ativas")
    public ResponseEntity<List<SolucaoResponseDTO>> listarAtivas() {
        return ResponseEntity.ok(solucaoService.listarAtivas());
    }

    @Operation(summary = "Buscar solução por ID")
    @GetMapping("/{id}")
    public ResponseEntity<SolucaoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(solucaoService.buscarPorId(id));
    }

    @Operation(summary = "Atualizar solução e sua composição")
    @PutMapping("/{id}")
    public ResponseEntity<SolucaoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody SolucaoRequestDTO dto) {

        return ResponseEntity.ok(solucaoService.atualizar(id, dto));
    }

    @Operation(
            summary = "Inativar solução",
            description = "Inativa a solução sem remover sua composição histórica."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        solucaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
