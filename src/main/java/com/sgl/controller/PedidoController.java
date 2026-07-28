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

import com.sgl.dto.AprovarPedidoDTO;
import com.sgl.dto.PedidoDTO;
import com.sgl.model.enums.StatusPedido;
import com.sgl.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

	
	private final PedidoService pedidoService;
	
	@PostMapping
	public ResponseEntity<PedidoDTO> criar(@Valid @RequestBody PedidoDTO dto){
		PedidoDTO criado = pedidoService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}
	
	@GetMapping
	public ResponseEntity<List<PedidoDTO>> listarTodos(){
		return ResponseEntity.ok(pedidoService.listarTodos());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<PedidoDTO> buscarPorId(@PathVariable Long id){
		return ResponseEntity.ok(pedidoService.buscarPorId(id));
	}
	
	@GetMapping("/por-usario")
	public ResponseEntity<List<PedidoDTO>> listarPorUsuario(@RequestParam Long usuarioId){
		return ResponseEntity.ok(pedidoService.listarPorUsuario(usuarioId));
	}
	
	@GetMapping("/por-status")
	public ResponseEntity<List<PedidoDTO>> listarPorStatus(@RequestParam StatusPedido status){
		return ResponseEntity.ok(pedidoService.listarPorStatus(status));
	}
	
	@PutMapping("/{id}/aprovar")
	public ResponseEntity<PedidoDTO> aprovar(@PathVariable Long id, @Valid @RequestBody AprovarPedidoDTO dto){
		return ResponseEntity.ok(pedidoService.aprovar(id, dto));
	}
	
	@PutMapping("/{id}/rejeitar")
	public ResponseEntity<PedidoDTO> rejeitar(@PathVariable Long id, @RequestParam(required = false) String observacao){
		return ResponseEntity.ok(pedidoService.rejeitar(id, observacao));
	}
	
	@PutMapping("/{id}/entregar")
	public ResponseEntity<PedidoDTO> entregar(@PathVariable Long id){
		return ResponseEntity.ok(pedidoService.entregar(id));
	}
	
	@PutMapping("/{id}/cancelar")
	public ResponseEntity<PedidoDTO> cancelar(@PathVariable Long id, @RequestParam(required = false) String observacao){
		return ResponseEntity.noContent().build();
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		pedidoService.deletar(id);
		return ResponseEntity.noContent().build();
	}

	
	
}
