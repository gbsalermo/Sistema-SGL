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

import com.sgl.dto.ProjetoDTO;
import com.sgl.service.ProjetoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
public class ProjetoController {

	private final ProjetoService projetoService;
	
	
	@PostMapping
	public ResponseEntity<ProjetoDTO> criar(@Valid @RequestBody ProjetoDTO dto){
		
		ProjetoDTO criado = projetoService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}
	
	@GetMapping
	public ResponseEntity<List<ProjetoDTO>> listarTodos(){
		return ResponseEntity.ok(projetoService.listarTodos());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ProjetoDTO> buscarPorId(@PathVariable Long id){
		return ResponseEntity.ok(projetoService.buscarPorId(id));
	}
	
	@GetMapping("/por-laboratorio")
	public ResponseEntity<List<ProjetoDTO>> listarPorLaboratorio(@RequestParam Long laboratorioId){
		return ResponseEntity.ok(projetoService.listarPorLaboratorio(laboratorioId));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<ProjetoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ProjetoDTO dto){
		return ResponseEntity.ok(projetoService.atualizar(id, dto));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		projetoService.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
