package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EstoqueLaboratorioDTO;
import com.sgl.repository.EstoqueLaboratorioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueLaboratorioService {
	
	private final EstoqueLaboratorioRepository estoqueLaboratorioRepository;
	
	@Transactional(readOnly = true)
	public List<EstoqueLaboratorioDTO> listarTodos(){
		return estoqueLaboratorioRepository.findAll()
				.stream()
				.map(EstoqueLaboratorioDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public EstoqueLaboratorioDTO buscarPorId(Long id) {
		return estoqueLaboratorioRepository.findById(id)
				.map(EstoqueLaboratorioDTO::new)
				.orElseThrow(() -> new EntityNotFoundException("Estoque laboratório não encontrado com id: " + id));
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueLaboratorioDTO> listarPorLaboratorio(Long laboratorioId){
		return estoqueLaboratorioRepository.findByLaboratorioId(laboratorioId)
				.stream()
				.map(EstoqueLaboratorioDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueLaboratorioDTO> listarPorProduto(Long produtoId){
		return estoqueLaboratorioRepository.findByProdutoId(produtoId)
				.stream()
				.map(EstoqueLaboratorioDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueLaboratorioDTO> listarPorPedido(Long pedidoId){
		return estoqueLaboratorioRepository.findByPedidoId(pedidoId)
				.stream()
				.map(EstoqueLaboratorioDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueLaboratorioDTO> listarPorPeriodo(Long laboratorioId, LocalDateTime dataInicio, LocalDateTime dataFim){
		return estoqueLaboratorioRepository.findByLaboratorioIdAndPeriodo(laboratorioId, dataInicio, dataFim)
				.stream()
				.map(EstoqueLaboratorioDTO::new)
				.toList();
	}
	

}
