package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.ContextoOperacionalResponseDTO;
import com.sgl.service.ContextoOperacionalConsultaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Consultas de contexto da 7.5. Nao realizam mutacoes nem alteram Pedidos.
 * Em DEV, o filtro de Unidade ainda depende do header temporario; nao
 * substitui autorizacao real por identidade antes de producao.
 */
@Tag(name = "Contextos operacionais", description =
        "Consulta de participacoes de estagio e seus contextos hierarquicos.")
@RestController
@RequestMapping("/api/v1/contextos-operacionais")
@RequiredArgsConstructor
public class ContextoOperacionalController {

    private final ContextoOperacionalConsultaService service;

    @Operation(summary = "Listar contextos e elegibilidade de um Estagiario")
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<ContextoOperacionalResponseDTO>> listarPorUsuario(
            @PathVariable UUID usuarioId) {
        return ResponseEntity.ok(service.listarDoEstagiario(usuarioId));
    }

    @Operation(summary = "Listar participantes atuais e historicos de uma Atividade")
    @GetMapping("/atividades/{atividadeId}/participacoes")
    public ResponseEntity<List<ContextoOperacionalResponseDTO>> listarPorAtividade(
            @PathVariable UUID atividadeId) {
        return ResponseEntity.ok(service.listarDaAtividade(atividadeId));
    }

    @Operation(summary = "Listar participantes por Projeto para o hub sem N+1")
    @GetMapping("/projetos/{projetoId}/participacoes")
    public ResponseEntity<List<ContextoOperacionalResponseDTO>> listarPorProjeto(
            @PathVariable UUID projetoId) {
        return ResponseEntity.ok(service.listarDoProjeto(projetoId));
    }
}
