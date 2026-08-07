package com.sgl.controller;

import java.util.List;

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

import com.sgl.dto.ProdutoDTO;
import com.sgl.model.enums.NivelRisco;
import com.sgl.service.ProdutoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    public ResponseEntity<ProdutoDTO> criar(@Valid @RequestBody ProdutoDTO dto) {
        ProdutoDTO salvo = produtoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<ProdutoDTO>> listarTodos() {
        return ResponseEntity.ok(produtoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoDTO dto) {
        return ResponseEntity.ok(produtoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/risco/{nivel}")
    public ResponseEntity<List<ProdutoDTO>> listarPorRisco(
            @PathVariable NivelRisco nivel) {
        return ResponseEntity.ok(produtoService.listarPorRisco(nivel));
    }

    @GetMapping("/pereciveis")
    public ResponseEntity<List<ProdutoDTO>> listarPereciveis() {
        return ResponseEntity.ok(produtoService.listarPereciveis());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProdutoDTO>> buscarPorNome(@RequestParam String nome) {
        return ResponseEntity.ok(produtoService.buscarPorNome(nome));
    }
}
