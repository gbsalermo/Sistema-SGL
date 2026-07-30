package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProdutoDTO;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.repository.ProdutoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

	private final ProdutoRepository produtoRepository;
	
	@Transactional
	public ProdutoDTO criar(ProdutoDTO dto) {
		
		Produto produto = new Produto();
		preencherProduto(produto, dto);

		Produto salvo = produtoRepository.save(produto);
		return new ProdutoDTO(salvo);
	}
	
	@Transactional(readOnly = true)
	public List<ProdutoDTO> listarTodos(){
		return produtoRepository.findAll()
				.stream()
				.map(ProdutoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<ProdutoDTO> listarPorRisco(NivelRisco risco){
		return produtoRepository.findByRisco(risco)
				.stream()
				.map(ProdutoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<ProdutoDTO> listarPereciveis(){
		return produtoRepository.findByPerecivelTrue()
				.stream()
				.map(ProdutoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<ProdutoDTO> buscarPorNome(String nome){
		return produtoRepository.findByNomeContainingIgnoreCase(nome)
				.stream()
				.map(ProdutoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
    public ProdutoDTO buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com o id: " + id));
        return new ProdutoDTO(produto);
    }
    @Transactional
    public ProdutoDTO atualizar(Long id, ProdutoDTO dto) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));
        
        preencherProduto(produto, dto);
        Produto atualizado = produtoRepository.save(produto);
        return new ProdutoDTO(atualizado);
    }
    
    @Transactional
    public void deletar(Long id, Produto produto) {
    	if(!produtoRepository.existsById(id)) {
    		throw new EntityNotFoundException("Produto não encontrado com o id: " + id);
    	}
    	produto.setAtivo(false);
    }
    
    //Metodo para Listar validade Proxima
    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarValidadeProxima(int dias){
    	LocalDate dataAtual = LocalDate.now();
    	LocalDate dataLimite = dataAtual.plusDays(dias);
    	return produtoRepository.findPereciveisComValidadeProxima(dataAtual, dataLimite)
    			.stream()
    			.map(ProdutoDTO::new)
    			.toList();
    }
    
    
    //Metodo privado para preencher o produto
    private void preencherProduto(Produto produto, ProdutoDTO dto) {

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        if (produtoRepository.existsByCodigoReferencia(dto.getCodigoReferencia())) {
        	 throw new IllegalArgumentException("Já existe um produto com este código de referência.");
        	 }else { produto.setCodigoReferencia(dto.getCodigoReferencia());}
        produto.setUnidadeMedida(dto.getUnidadeMedida());
        produto.setLocalizacaoFisica(dto.getLocalizacaoFisica());
        if (dto.getRisco() == NivelRisco.NENHUM) {
            produto.setTipoRisco(null);
            produto.setDescricaoRisco(null);
        } else { produto.setRisco(dto.getRisco());}
        produto.setTipoRisco(dto.getTipoRisco());
        produto.setDescricaoRisco(dto.getDescricaoRisco());
        produto.setPerecivel(dto.getPerecivel());
        produto.setDataValidade(dto.getDataValidade());
        if (!dto.getPerecivel()) {
            produto.setDataValidade(null);
            produto.setTipoPerecivel(null);
        } else { produto.setTipoPerecivel(dto.getTipoPerecivel());}
        produto.setCondicoesArmazenamento(dto.getCondicoesArmazenamento());
        produto.setUnidadeArmazenamento(dto.getUnidadeArmazenamento());
        produto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
    }
}
