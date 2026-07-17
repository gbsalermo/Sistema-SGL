package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.UnidadeDTO;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;

import lombok.RequiredArgsConstructor;


@Service 
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    
    //CREATE
    @Transactional
    public UnidadeDTO criar(UnidadeDTO dto){
        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome());
        unidade.setSigla(dto.getSigla());
        
        Unidade salva = unidadeRepository.save(unidade);
        return new UnidadeDTO(salva);
    }
    
    //LISTAR TODOS
    @Transactional(readOnly = true)
    public List<UnidadeDTO> listarTodos(){
        return unidadeRepository.findAll()
        		.stream()
        		.map(UnidadeDTO::new)
        		.toList();
    }

    //BUSCAR POR ID
    @Transactional(readOnly = true)
    public UnidadeDTO buscarPorId(Long id){
    	Unidade unidade = unidadeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Unidade não encontrada com id: " 
            + id));
            return new UnidadeDTO(unidade);
    }


    //ATUALIZAR
    @Transactional
    public UnidadeDTO atualizar(Long id, UnidadeDTO dto){
    	Unidade unidade = unidadeRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Unidade não encontrada com id " + id));
    	unidade.setNome(dto.getNome());
    	unidade.setSigla(dto.getSigla());
    	Unidade atualizada = unidadeRepository.save(unidade);
    	return new UnidadeDTO(atualizada);
    }
    
    //DELETAR
    @Transactional
    public void deletar(Long id) {
    	try {
    		unidadeRepository.deleteById(id);
    	}
    	catch(RuntimeException e) {
    			throw new RuntimeException("Unidade não encontrada com id " + id);
    	}
    }
    
}
