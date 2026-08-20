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

import com.sgl.dto.request.EstoqueCentralRequestDTO;
import com.sgl.dto.response.EstoqueCentralResponseDTO;
import com.sgl.service.EstoqueCentralService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Estoque Central", description = "Operações de consulta e gerenciamento do estoque central por unidade e produto.")
@RestController
@RequestMapping("/api/v1/estoque-central")
@RequiredArgsConstructor
public class EstoqueCentralController {

    private final EstoqueCentralService estoqueCentralService;

    @Operation(summary = "Criar novo Estoque-central", description = "Cria um novo estoque-central")
    @PostMapping
    public ResponseEntity<EstoqueCentralResponseDTO> criar(
            @Valid @RequestBody EstoqueCentralRequestDTO dto) {
        EstoqueCentralResponseDTO salvo = estoqueCentralService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Operation(summary = "Listar todos os estoque", description = "Retorna todos os estoques cadastrados no sistema")
    @GetMapping
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estoqueCentralService.listarTodos());
    }
    
    @Operation(summary = "Busca o Estoque por id", description = "Busca um estoque pelo identificador")
    @GetMapping("/{id}")
    public ResponseEntity<EstoqueCentralResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estoqueCentralService.buscarPorId(id));
    }

    @Operation(summary = "Lista Estoques por Unidade", description = "Recebe todos os estoques vinculados ao id da Unidade")
    @GetMapping("/por-unidade")
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarPorUnidade(
            @RequestParam UUID unidadeId) {
        return ResponseEntity.ok(estoqueCentralService.listarPorUnidade(unidadeId));
    }

    @Operation(summary = "Busca o Estoque por Unidade e Produto", description = "Busca o estoque vinculado ao id da Unidade e do Produto")
    @GetMapping("/por-unidade-produto")
    public ResponseEntity<EstoqueCentralResponseDTO> buscarPorUnidadeEProduto(
            @RequestParam UUID unidadeId,
            @RequestParam UUID produtoId) {
        return ResponseEntity.ok(
                estoqueCentralService.buscarPorUnidadeEProduto(unidadeId, produtoId));
    }

    @Operation(summary = "Atualiza o Estoque", description = "Atualizar dados do estoque")
    @PutMapping("/{id}")
    public ResponseEntity<EstoqueCentralResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EstoqueCentralRequestDTO dto) {
        return ResponseEntity.ok(estoqueCentralService.atualizar(id, dto));
    }

    @Operation(summary = "Deletar o estoque", description = "Deleta o estoque daquele produto a partir do identificador")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        estoqueCentralService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar Estoque com armazenamento baixo", description = "Recebe todos os estoques que estão com o armazenamento abaixo do ideal")
    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarEstoqueBaixo(
            @RequestParam UUID unidadeId) {
        return ResponseEntity.ok(
                estoqueCentralService.listarEstoqueBaixoPorUnidade(unidadeId));
    }
}
