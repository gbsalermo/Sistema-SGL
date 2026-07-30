package com.sgl.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.ProdutoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

	private final EstoqueCentralRepository estoqueCentralRepository;
	private final ProdutoRepository produtoRepository;
	
	
	@Transactional
	public EstoqueCentralDTO criar(EstoqueCentralDTO dto) {

	    if (estoqueCentralRepository.findByProdutoId(dto.getProdutoId()).isPresent()) {
	        throw new RuntimeException(
	                "Já existe estoque central para o produto com id: " + dto.getProdutoId());
	    }

	    Produto produto = produtoRepository.findById(dto.getProdutoId())
	            .orElseThrow(() -> new EntityNotFoundException(
	                    "Produto não encontrado com id: " + dto.getProdutoId()));

	    EstoqueCentral estoque = EstoqueCentral.builder()
	            .produto(produto)
	            .quantidadeAtual(dto.getQuantidadeAtual())
	            .quantidadeMinima(dto.getQuantidadeMinima())
	            .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
	            .build();

	    try {
	        EstoqueCentral salvo = estoqueCentralRepository.save(estoque);
	        return new EstoqueCentralDTO(salvo);

	    } catch (DataIntegrityViolationException e) {
	        throw new RuntimeException(
	                "Já existe um estoque cadastrado para esse produto.");
	    }
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueCentralDTO> listarTodos(){
		return estoqueCentralRepository.findAll()
				.stream()
				.map(EstoqueCentralDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public EstoqueCentralDTO buscarPorId(Long id) {
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado com id: " + id));
		return new EstoqueCentralDTO(estoque);
	}
	
	@Transactional(readOnly = true)
	public EstoqueCentralDTO buscarPorProdutoID(Long produtoId) {
		EstoqueCentral estoque = estoqueCentralRepository.findByProdutoId(produtoId)
				.orElseThrow(() -> new EntityNotFoundException("EStoque central não encontrado para o produto id: " + produtoId));
		return new EstoqueCentralDTO(estoque);
	}
	
	@Transactional
	public EstoqueCentralDTO atualizar(Long id, EstoqueCentralDTO dto) {
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado"));
		
				estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
				estoque.setAtivo(dto.getAtivo());
				
				EstoqueCentral atualizado = estoqueCentralRepository.save(estoque);
				return new EstoqueCentralDTO(atualizado);
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueCentralDTO> listarEstoqueBaixo(){
		List<EstoqueCentral> estoques =
				estoqueCentralRepository.findAll();
		return estoques.stream()
				.filter(e -> e.getQuantidadeAtual() <= e.getQuantidadeMinima())
				.map(EstoqueCentralDTO::new)
				.toList();
	}
	
	@Transactional
	public EstoqueCentralDTO entrada(Long id, MovimentacaoEstoqueDTO dto) {
		
		if (dto.getQuantidade() <= 0) {
	        throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
	    } 
		
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado com id: " + id));

		estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() + dto.getQuantidade());

		EstoqueCentral atualizado = estoqueCentralRepository.save(estoque);
		return new EstoqueCentralDTO(atualizado);
	}

	@Transactional
	public EstoqueCentralDTO saida(Long id, MovimentacaoEstoqueDTO dto) {
		
		 if (dto.getQuantidade() <= 0) {
		        throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
		    }
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado com id: " + id));

		int novaQuantidade = estoque.getQuantidadeAtual() - dto.getQuantidade();
		if (novaQuantidade < 0) {
			throw new IllegalArgumentException("Estoque insuficiente. Disponível: "
					+ estoque.getQuantidadeAtual() + ", solicitado: " + dto.getQuantidade());
		}

		estoque.setQuantidadeAtual(novaQuantidade);

		EstoqueCentral atualizado = estoqueCentralRepository.save(estoque);
		return new EstoqueCentralDTO(atualizado);
	}

	@Transactional
	public void deletar(Long id) {
	    if (!estoqueCentralRepository.existsById(id)) {
	        throw new EntityNotFoundException("Estoque central não encontrado com id: " + id);
	    }
	    estoqueCentralRepository.deleteById(id);
	}
	}

