package com.sgl.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.EstoqueLaboratorioDTO;
import com.sgl.service.EstoqueLaboratorioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/estoque-laboratorio")
@RequiredArgsConstructor
public class EstoqueLaboratorioController {
	
	private final EstoqueLaboratorioService estoqueLaboratorioService;
	
	
	@GetMapping
	public ResponseEntity<List<EstoqueLaboratorioDTO>> listarTodos(){
		return ResponseEntity.ok(estoqueLaboratorioService.listarTodos());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<EstoqueLaboratorioDTO> buscarPorId(@PathVariable Long id){
		return ResponseEntity.ok(estoqueLaboratorioService.buscarPorId(id));
	}
	
	@GetMapping("/laboratorio/{laboratorioId}")
	public ResponseEntity<List<EstoqueLaboratorioDTO>> listarPorLaboratorio(@PathVariable Long laboratorioId){
		return ResponseEntity.ok(estoqueLaboratorioService.listarPorLaboratorio(laboratorioId));
	}
	
	@GetMapping("/produto/{produtoId}")
	public ResponseEntity<List<EstoqueLaboratorioDTO>> listarProduto(@PathVariable Long produtoId){
		return ResponseEntity.ok(estoqueLaboratorioService.listarPorProduto(produtoId));
	}
	
	@GetMapping("/pedido/{pedidoId}")
	public ResponseEntity<List<EstoqueLaboratorioDTO>> listarPorPedido(@PathVariable Long pedidoId){
		return ResponseEntity.ok(estoqueLaboratorioService.listarPorPedido(pedidoId));
	}
	
	@GetMapping("/laboratorio/{laboratorioId}/periodo")
	public ResponseEntity<List<EstoqueLaboratorioDTO>> listarPorPeriodo(@PathVariable Long laboratorioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim){
		return ResponseEntity.ok(estoqueLaboratorioService.listarPorPeriodo(laboratorioId, dataInicio, dataFim));
}

}
