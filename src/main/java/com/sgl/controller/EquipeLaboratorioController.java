package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.MembroEquipeResponseDTO;
import com.sgl.service.EquipeLaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Equipe do laboratório", description = "Equipe organizada pelo responsável do laboratório ou por um administrador da Unidade.")
@RestController
@RequestMapping("/api/v1/laboratorios/{laboratorioId}/equipe")
@RequiredArgsConstructor
public class EquipeLaboratorioController {

    private final EquipeLaboratorioService service;

    @Operation(summary = "Listar a equipe", description = "Pessoas com o laboratório na lista e pessoas com acesso temporário vigente.")
    @GetMapping
    public List<MembroEquipeResponseDTO> listar(@PathVariable UUID laboratorioId) {
        return service.listar(laboratorioId);
    }

    @Operation(summary = "Incluir pessoa já cadastrada na equipe", description = "Idempotente: incluir quem já é da equipe não muda nada.")
    @PostMapping("/{usuarioId}")
    public ResponseEntity<Void> incluir(@PathVariable UUID laboratorioId, @PathVariable UUID usuarioId) {
        service.incluir(laboratorioId, usuarioId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Retirar pessoa da equipe", description = "Tira o laboratório da lista da pessoa e encerra os acessos temporários dela a ele.")
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> retirar(@PathVariable UUID laboratorioId, @PathVariable UUID usuarioId) {
        service.retirar(laboratorioId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
