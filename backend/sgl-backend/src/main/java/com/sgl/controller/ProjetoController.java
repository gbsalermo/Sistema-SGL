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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Projetos", description = "Operações de cadastro e consulta de projetos vinculados aos laboratórios.")
@RestController
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetoService projetoService;

    @Operation(summary = "Criar projeto", description = "Cadastra um novo projeto vinculado a um laboratório.")
    @PostMapping
    public ResponseEntity<ProjetoResponseDTO> criar(@Valid @RequestBody ProjetoRequestDTO dto) {
        ProjetoResponseDTO criado = projetoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @Operation(summary = "Listar projetos", description = "Retorna todos os projetos cadastrados no sistema.")
    @GetMapping
    public ResponseEntity<List<ProjetoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(projetoService.listarTodos());
    }

    @Operation(summary = "Buscar projeto por ID", description = "Retorna um projeto pelo seu identificador público UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(projetoService.buscarPorId(id));
    }

    @Operation(summary = "Listar projetos por laboratório", description = "Retorna os projetos vinculados ao laboratório informado.")
    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<ProjetoResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(projetoService.listarPorLaboratorio(laboratorioId));
    }

    @Operation(summary = "Atualizar projeto", description = "Atualiza os dados do projeto identificado pelo UUID informado.")
    @PutMapping("/{id}")
    public ResponseEntity<ProjetoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProjetoRequestDTO dto) {
        return ResponseEntity.ok(projetoService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir projeto", description = "Remove o projeto identificado pelo UUID informado.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        projetoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar projetos ativos", description = "Retorna somente os projetos atualmente ativos.")
    @GetMapping("/ativos")
    public ResponseEntity<List<ProjetoResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(projetoService.listarAtivos());
    }
}
