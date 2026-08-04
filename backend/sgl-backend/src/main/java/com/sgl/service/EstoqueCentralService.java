package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

	private final EstoqueCentralRepository estoqueCentralRepository;
	private final ProdutoRepository produtoRepository;
	private final UnidadeRepository unidadeRepository;
	
	@Transactional
	public EstoqueCentralDTO criar(EstoqueCentralDTO dto) {

		if (estoqueCentralRepository.existsByUnidadeIdAndProdutoId(
	            dto.getUnidadeId(),
	            dto.getProdutoId())) {

	        throw new IllegalArgumentException(
	                "Já existe estoque para esse produto nesta unidade."
	        );
	    }

	    Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
	            .orElseThrow(() -> new EntityNotFoundException(
	                    "Unidade não encontrada com id: " + dto.getUnidadeId()
	            ));

	    Produto produto = produtoRepository.findById(dto.getProdutoId())
	            .orElseThrow(() -> new EntityNotFoundException(
	                    "Produto não encontrado com id: " + dto.getProdutoId()
	            ));

	    EstoqueCentral estoque = EstoqueCentral.builder()
	            .unidade(unidade)
	            .produto(produto)
	            .quantidadeAtual(dto.getQuantidadeAtual())
	            .quantidadeMinima(dto.getQuantidadeMinima())
	            .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
	            .build();

	    EstoqueCentral salvo = estoqueCentralRepository.save(estoque);
	    return new EstoqueCentralDTO(salvo);
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
	public EstoqueCentralDTO buscarPorUnidadeEProduto(
	        Long unidadeId,
	        Long produtoId) {

	    EstoqueCentral estoque =
	            estoqueCentralRepository.findByUnidadeIdAndProdutoId(
	                    unidadeId,
	                    produtoId
	            ).orElseThrow(() -> new EntityNotFoundException(
	                    "Estoque não encontrado para a unidade "
	                            + unidadeId + " e produto " + produtoId
	            ));

	    return new EstoqueCentralDTO(estoque);
	}
	
	@Transactional(readOnly = true)
	public List<EstoqueCentralDTO> listarPorUnidade(Long unidadeId) {

	    if (!unidadeRepository.existsById(unidadeId)) {
	        throw new EntityNotFoundException(
	                "Unidade não encontrada com id: " + unidadeId
	        );
	    }

	    return estoqueCentralRepository.findByUnidadeId(unidadeId)
	            .stream()
	            .map(EstoqueCentralDTO::new)
	            .toList();
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
	public List<EstoqueCentralDTO> listarEstoqueBaixoPorUnidade(
	        Long unidadeId) {

	    return estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidadeId)
	            .stream()
	            .filter(estoque ->
	                    estoque.getQuantidadeAtual()
	                            <= estoque.getQuantidadeMinima())
	            .map(EstoqueCentralDTO::new)
	            .toList();
	}
	
	@Transactional
	public EstoqueCentralDTO entrada(Long id, MovimentacaoEstoqueDTO dto) {
		
		if (dto.getQuantidadeMovimentada() <= 0) {
	        throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
	    } 
		
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado com id: " + id));

		estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() + dto.getQuantidadeMovimentada());

		EstoqueCentral atualizado = estoqueCentralRepository.save(estoque);
		return new EstoqueCentralDTO(atualizado);
	}

	@Transactional
	public EstoqueCentralDTO saida(Long id, MovimentacaoEstoqueDTO dto) {
		
		 if (dto.getQuantidadeMovimentada() <= 0) {
		        throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
		    }
		EstoqueCentral estoque = estoqueCentralRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado com id: " + id));

		int novaQuantidade = estoque.getQuantidadeAtual() - dto.getQuantidadeMovimentada();
		if (novaQuantidade < 0) {
			throw new IllegalArgumentException("Estoque insuficiente. Disponível: "
					+ estoque.getQuantidadeAtual() + ", solicitado: " + dto.getQuantidadeMovimentada());
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

