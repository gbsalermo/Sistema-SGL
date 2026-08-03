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

import com.sgl.dto.EstagiarioDTO;
import com.sgl.service.EstagiarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/estagiarios")
@RequiredArgsConstructor
public class EstagiarioController {

	private final EstagiarioService estagiarioService;

	@GetMapping
	public ResponseEntity<List<EstagiarioDTO>> listarTodos() {
		return ResponseEntity.ok(estagiarioService.listarTodos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<EstagiarioDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(estagiarioService.buscarPorId(id));
	}

	@GetMapping("/por-laboratorio")
	public ResponseEntity<List<EstagiarioDTO>> listarPorLaboratorio(@RequestParam Long laboratorioId) {
		return ResponseEntity.ok(estagiarioService.listarPorLaboratorio(laboratorioId));
	}

	@GetMapping("/ativos")
	public ResponseEntity<List<EstagiarioDTO>> listarAtivos() {
		return ResponseEntity.ok(estagiarioService.listarAtivos());
	}

	@PostMapping
	public ResponseEntity<EstagiarioDTO> criar(@Valid @RequestBody EstagiarioDTO dto) {
		EstagiarioDTO novoEstagiario = estagiarioService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(novoEstagiario);
	}

	@PutMapping("/{id}")
	public ResponseEntity<EstagiarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody EstagiarioDTO dto) {
		return ResponseEntity.ok(estagiarioService.atualizar(id, dto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		estagiarioService.deletar(id);
		return ResponseEntity.noContent().build();
	}
	
	//Encerrar estagio
	@PutMapping("/{id}/encerrar")
	public ResponseEntity<EstagiarioDTO> encerrarEstagio(@PathVariable Long id) {
	    return ResponseEntity.ok(estagiarioService.encerrarEstagio(id));
	}
}
