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

import com.sgl.dto.UsuarioDTO;
import com.sgl.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor

public class UsuarioController {

	private final UsuarioService usuarioService;
	
	
	@GetMapping
	public ResponseEntity<List<UsuarioDTO>> listarTodos(){
		return ResponseEntity.ok(usuarioService.listarTodos());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id){
		return ResponseEntity.ok(usuarioService.buscarPorId(id));
	}
	
	@GetMapping("/por-laboratorio")
	public ResponseEntity<List<UsuarioDTO>> listarPorLaboratorio(@RequestParam Long laboratorioId){
		return ResponseEntity.ok(usuarioService.listarPorLaboratorio(laboratorioId));
	}
	
	@PostMapping
	public ResponseEntity<UsuarioDTO> criar(@Valid @RequestBody UsuarioDTO dto){
		UsuarioDTO novoUsuario = usuarioService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<UsuarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioDTO dto){
		return ResponseEntity.ok(usuarioService.atualizar(id, dto));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		usuarioService.deletar(id);
		return ResponseEntity.noContent().build();
	}
	
	
	

}
