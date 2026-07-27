package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProjetoDTO;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjetoService {

	private final ProjetoRepository projetoRepository;
	private final LaboratorioRepository laboratorioRepository;
	
	@Transactional
	public ProjetoDTO criar(ProjetoDTO dto) {
		Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
				.orElseThrow(() -> new EntityNotFoundException("Laboratorio não encontrado com id: "
						+ dto.getLaboratorioId()));
		
		Projeto projeto = Projeto.builder()
				.laboratorio(laboratorio)
				.nome(dto.getNome())
				.descricao(dto.getDescricao())
				.dataInicio(dto.getDataInicio())
				.dataFim(dto.getDataFim())
				.responsavel(dto.getResponsavel())
				.ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
				.build();
		
		Projeto salvo = projetoRepository.save(projeto);
		return new ProjetoDTO(salvo);
	}
	
	@Transactional(readOnly = true)
	public List<ProjetoDTO> listarTodos(){
		return projetoRepository.findAll()
				.stream()
				.map(ProjetoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public ProjetoDTO buscarPorId( long id) {
		return projetoRepository.findById(id)
				.map(ProjetoDTO::new)
				.orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id " + id));
	}
	
	@Transactional(readOnly = true)
	public List<ProjetoDTO> listarPorLaboratorio(Long laboratorioId){
		return projetoRepository.findByLaboratorioId(laboratorioId)
				.stream()
				.map(ProjetoDTO::new)
				.toList();
	}
	
	@Transactional
	public ProjetoDTO atualizar(Long id, ProjetoDTO dto) {
		Projeto projeto = projetoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com o id: " + id));
		
		projeto.setNome(dto.getNome());
		projeto.setDescricao(dto.getDescricao());
		projeto.setDataInicio(dto.getDataInicio());
		projeto.setDataFim(dto.getDataFim());
		projeto.setResponsavel(dto.getResponsavel());
		projeto.setAtivo(dto.getAtivo());
		
		Projeto atualizado = projetoRepository.save(projeto);
		return new ProjetoDTO(atualizado);
	}
	
	@Transactional
	public void deletar(Long id) {
		if(!projetoRepository.existsById(id)) {
			throw new EntityNotFoundException("Projeto não encontrado com id: " + id);
			
		}
		projetoRepository.deleteById(id);
	}
}


