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

import com.sgl.dto.UnidadeDTO;
import com.sgl.service.UnidadeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
public class UnidadeController {
	
	private final UnidadeService unidadeService;
	
	@GetMapping
	public ResponseEntity<List<UnidadeDTO>> listarTodos(){
		return ResponseEntity.ok(unidadeService.listarTodos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<UnidadeDTO> buscarPorId(@PathVariable Long id){
		return ResponseEntity.ok(unidadeService.buscarPorId(id));
	}
	
	@PostMapping
	public ResponseEntity<UnidadeDTO> salvar(@RequestBody UnidadeDTO dto){
		UnidadeDTO novaUnidade = unidadeService.salvar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(novaUnidade);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<UnidadeDTO> atualizar(@PathVariable Long id, @RequestBody UnidadeDTO dto){
		return ResponseEntity.ok(unidadeService.atualizar(id, dto));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		unidadeService.delete(id);
		return ResponseEntity.noContent().build();
	}
}

