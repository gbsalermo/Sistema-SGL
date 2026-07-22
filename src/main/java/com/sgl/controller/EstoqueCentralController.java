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
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.service.EstoqueCentralService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/estoque-central")
@RequiredArgsConstructor
public class EstoqueCentralController {

	private final EstoqueCentralService estoqueCentralService;
	
	@PostMapping
	public ResponseEntity<EstoqueCentralDTO> criar(@Valid @RequestBody EstoqueCentralDTO dto){
		EstoqueCentralDTO salvo = estoqueCentralService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
	}
	
	@GetMapping
	public ResponseEntity<List<EstoqueCentralDTO>> listarTodos()
	{
		return ResponseEntity.ok(estoqueCentralService.listarTodos());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<EstoqueCentralDTO> buscarPorId( @PathVariable Long id){
		return ResponseEntity.ok(estoqueCentralService.buscarPorId(id));
	}
	
	@GetMapping("/produto/{produtoId}")
	public ResponseEntity<EstoqueCentralDTO>
	buscarPorProdutoId(@PathVariable Long produtoId){
		return ResponseEntity.ok(estoqueCentralService.buscarPorId(produtoId));
	}
	
	@PutMapping("/{id}")
    public ResponseEntity<EstoqueCentralDTO> atualizar(@PathVariable Long id, @Valid @RequestBody EstoqueCentralDTO dto) {
        return ResponseEntity.ok(estoqueCentralService.atualizar(id, dto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        estoqueCentralService.deletar(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<EstoqueCentralDTO>> listarEstoqueBaixo() {
        return ResponseEntity.ok(estoqueCentralService.listarEstoqueBaixo());
    }
	
	
}
