package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.ObservacaoVinculoEstagioRequestDTO;
import com.sgl.dto.request.TreinamentoSegurancaVinculoRequestDTO;
import com.sgl.dto.response.ObservacaoVinculoEstagioResponseDTO;
import com.sgl.service.ObservacaoVinculoEstagioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
        name = "Vínculos de estágio — observações",
        description = "Observações operacionais e auditoria do treinamento de segurança.")
@RestController
@RequestMapping("/api/v1/vinculos-estagio")
@RequiredArgsConstructor
public class ObservacaoVinculoEstagioController {

    private final ObservacaoVinculoEstagioService service;

    @Operation(summary = "Listar observações e eventos auditáveis do vínculo")
    @GetMapping("/{vinculoId}/observacoes")
    public ResponseEntity<List<ObservacaoVinculoEstagioResponseDTO>> listar(
            @PathVariable UUID vinculoId) {

        return ResponseEntity.ok(service.listar(vinculoId));
    }

    @Operation(summary = "Adicionar observação operacional ao vínculo")
    @PostMapping("/{vinculoId}/observacoes")
    public ResponseEntity<ObservacaoVinculoEstagioResponseDTO> adicionar(
            @PathVariable UUID vinculoId,
            @Valid @RequestBody ObservacaoVinculoEstagioRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.adicionarOperacional(vinculoId, dto));
    }

    @Operation(
            summary = "Alterar estado do treinamento de segurança",
            description = "Permite concluir ou reverter o treinamento, preservando auditoria e observação opcional.")
    @PutMapping("/{vinculoId}/treinamento-seguranca")
    public ResponseEntity<ObservacaoVinculoEstagioResponseDTO> alterarTreinamento(
            @PathVariable UUID vinculoId,
            @Valid @RequestBody TreinamentoSegurancaVinculoRequestDTO dto) {

        return ResponseEntity.ok(service.alterarTreinamento(vinculoId, dto));
    }
}
