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
		produto.setNome(dto.getNome());
		produto.setDescricao(dto.getDescricao());
		produto.setCodigoReferencia(dto.getCodigoReferencia());
		produto.setUnidadeMedida(dto.getUnidadeMedida());
		produto.setLocalizacaoFisica(dto.getLocalizacaoFisica());
		produto.setRisco(dto.getRisco());
		produto.setTipoRisco(dto.getTipoRisco());
		produto.setDescricaoRisco(dto.getDescricaoRisco());
		produto.setPerecivel(dto.getPerecivel());
		produto.setDataValidade(dto.getDataValidade());
		produto.setTipoPerecivel(dto.getTipoPerecivel());
		produto.setCondicoesArmazenamento(dto.getCondicoesArmazenamento());
		produto.setUnidadeArmazenamento(dto.getUnidadeArmazenamento());
		produto.setAtivo(dto.isAtivo());

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
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setCodigoReferencia(dto.getCodigoReferencia());
        produto.setUnidadeMedida(dto.getUnidadeMedida());
        produto.setLocalizacaoFisica(dto.getLocalizacaoFisica());
        produto.setRisco(dto.getRisco());
        produto.setTipoRisco(dto.getTipoRisco());
        produto.setDescricaoRisco(dto.getDescricaoRisco());
        produto.setPerecivel(dto.getPerecivel());
        produto.setDataValidade(dto.getDataValidade());
        produto.setTipoPerecivel(dto.getTipoPerecivel());
        produto.setCondicoesArmazenamento(dto.getCondicoesArmazenamento());
        produto.setUnidadeArmazenamento(dto.getUnidadeArmazenamento());
        produto.setAtivo(dto.isAtivo());
        Produto atualizado = produtoRepository.save(produto);
        return new ProdutoDTO(atualizado);
    }
    
    @Transactional
    public void deletar(Long id) {
    	if(!produtoRepository.existsById(id)) {
    		throw new EntityNotFoundException("Produto não encontrado com o id: " + id);
    	}
    	produtoRepository.deleteById(id);
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
}
