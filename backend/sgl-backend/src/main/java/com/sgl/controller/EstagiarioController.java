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

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.service.EstagiarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Estagiários", description = "Operações de cadastro, consulta e encerramento de estágios.")
@RestController
@RequestMapping("/api/v1/estagiarios")
@RequiredArgsConstructor
public class EstagiarioController {

    private final EstagiarioService estagiarioService;

    @Operation(summary = "Listar todos os estagiarios", description = "Lista todos os estagiarios(Ativos ou não)")
    @GetMapping
    public ResponseEntity<List<EstagiarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estagiarioService.listarTodos());
    }

    @Operation(summary = "Buscar estagiario por Id", description = "Busca estagiario por identificador")
    @GetMapping("/{id}")
    public ResponseEntity<EstagiarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estagiarioService.buscarPorId(id));
    }

    @Operation(summary = "Listar estagiario por Laboratorio", description = "Lista estagiarios a partir do identificador do laboratorio")
    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<EstagiarioResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(estagiarioService.listarPorLaboratorio(laboratorioId));
    }

    @Operation(summary = "Listar estagiarios ativos", description = "Lista todos os estagiarios com o status Ativo" )
    @GetMapping("/ativos")
    public ResponseEntity<List<EstagiarioResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(estagiarioService.listarAtivos());
    }

    @Operation(summary = "Criar Estagiario", description = "Cria um novo cadastro de estagiario")
    @PostMapping
    public ResponseEntity<EstagiarioResponseDTO> criar(@Valid @RequestBody EstagiarioRequestDTO dto) {
        EstagiarioResponseDTO novoEstagiario = estagiarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoEstagiario);
    }

    @Operation(summary = "Atualizar dados do Estagiario", description = "Atualiza os dados do estagiario")
    @PutMapping("/{id}")
    public ResponseEntity<EstagiarioResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EstagiarioRequestDTO dto) {
        return ResponseEntity.ok(estagiarioService.atualizar(id, dto));
    }

    @Operation(summary = "Deletar Estagiario", description = "Deleta o cadastro do estagiario")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        estagiarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Encerrar Estagio", description = "Encerra o periodo de Estagio")
    @PutMapping("/{id}/encerrar")
    public ResponseEntity<EstagiarioResponseDTO> encerrarEstagio(@PathVariable UUID id) {
        return ResponseEntity.ok(estagiarioService.encerrarEstagio(id));
    }
}
