package com.sgl.controller;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.sgl.dto.request.ModeloSolucaoRequestDTO;
import com.sgl.dto.response.ModeloSolucaoResponseDTO;
import com.sgl.service.ModeloSolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/modelos-solucao")
@RequiredArgsConstructor
public class ModeloSolucaoController {
    private final ModeloSolucaoService service;

    @PostMapping
    public ResponseEntity<ModeloSolucaoResponseDTO> criar(@Valid @RequestBody ModeloSolucaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dto));
    }
    @PostMapping("/promover-pedido/{pedidoId}")
    public ResponseEntity<ModeloSolucaoResponseDTO> promover(@PathVariable UUID pedidoId,
            @Valid @RequestBody com.sgl.dto.request.PromoverSolucaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.promoverDePedido(pedidoId, dto));
    }
    @GetMapping
    public List<ModeloSolucaoResponseDTO> listar() { return service.listar(false); }
    @GetMapping("/ativos")
    public List<ModeloSolucaoResponseDTO> listarAtivos() { return service.listar(true); }
    @GetMapping("/{id}")
    public ModeloSolucaoResponseDTO buscar(@PathVariable UUID id) { return service.buscar(id); }
    @PutMapping("/{id}")
    public ModeloSolucaoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ModeloSolucaoRequestDTO dto) {
        return service.atualizar(id, dto);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}

