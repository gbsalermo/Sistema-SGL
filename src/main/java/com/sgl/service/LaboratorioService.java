package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.LaboratorioDTO;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaboratorioService {
	
	private final LaboratorioRepository laboratorioRepository;
	private final UnidadeRepository unidadeRepository;
	
	
	//CRIAR
	@Transactional
	public LaboratorioDTO criar(LaboratorioDTO dto) {
		
		Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
				.orElseThrow(() ->
						new EntityNotFoundException("Unidade não encontrada"));
		Laboratorio laboratorio = new Laboratorio();
		laboratorio.setDescricao(dto.getDescricao());
		laboratorio.setResponsavel(dto.getResponsavel());
		laboratorio.setAtivo(dto.isAtivo());
		laboratorio.setNome(dto.getNome());
		laboratorio.setUnidade(unidade);
		
		Laboratorio salvo = laboratorioRepository.save(laboratorio);
		return new LaboratorioDTO(salvo);
	}
	
	//LISTAR TODOS
	@Transactional(readOnly = true)
	public List<LaboratorioDTO> listarTodos(){
		return laboratorioRepository.findAll()
				.stream()
				.map(LaboratorioDTO::new)
				.toList();
	}
	
	//Listar por unidade
	@Transactional(readOnly = true)
	public List<LaboratorioDTO> listarPorUnidade(Long unidadeId){
		return laboratorioRepository.findByUnidadeId(unidadeId)
				.stream()
				.map(LaboratorioDTO::new)
				.toList();
	}
	
	//BUSCAR POR ID
	@Transactional(readOnly = true)
	public LaboratorioDTO buscarPorId(Long id) {
		Laboratorio laboratorio = laboratorioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Laboratorio não encontrado com o id: " + id));
				return new LaboratorioDTO(laboratorio);
	}
	
	//ATUALIZAR
	@Transactional
	public LaboratorioDTO atualizar(Long id, LaboratorioDTO dto) {
		Laboratorio laboratorio = laboratorioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Laboratorio não encontrado"));
		
		Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
				.orElseThrow(() -> new EntityNotFoundException("Unidade não encontrada"));
		
		laboratorio.setDescricao(dto.getDescricao());
		laboratorio.setResponsavel(dto.getResponsavel());
		laboratorio.setAtivo(dto.isAtivo());
		laboratorio.setNome(dto.getNome());
		laboratorio.setUnidade(unidade);
		
		Laboratorio atualizado = laboratorioRepository.save(laboratorio);
		
		return new LaboratorioDTO(atualizado);
	}
	
	//DELETE
	@Transactional
	public void deletar(Long id) {
		try {
			laboratorioRepository.deleteById(id);
		}
		catch(RuntimeException e) {
				throw new EntityNotFoundException("Laboratorio não encontrada com esse id: " + id);
		}
	}

}
