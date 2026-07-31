package com.sgl.service;

import java.util.List;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProjetoDTO;
import com.sgl.model.Laboratorio;
import com.sgl.model.Produto;
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
				.build();
		preencherProjeto(projeto, dto);
		
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
	   //Aplicando novo laboratorio caso precise
	    Laboratorio novoLaboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
	                .orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado com id: "
	                        + dto.getLaboratorioId()));

	    projeto.setLaboratorio(novoLaboratorio);
	    preencherProjeto(projeto, dto);

	    Projeto atualizado = projetoRepository.save(projeto);
	    return new ProjetoDTO(atualizado);
	}
	
	@Transactional
	public void deletar(Long id) {

		Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Produto não encontrado com o id: " + id));

		projeto.setAtivo(false);
	}
	
	//LISTAR APENAS PROJETOS ATIVOS
	@Transactional(readOnly = true)
	public List<ProjetoDTO> listarAtivos() {
	    return projetoRepository.findByAtivoTrue()
	            .stream()
	            .map(ProjetoDTO::new)
	            .toList();
	}

	//Metodo para preencher dados ao Criar e Atualizar
	private void preencherProjeto(Projeto projeto, ProjetoDTO dto) {
		validarDatas(dto.getDataInicio(), dto.getDataFim());

		projeto.setNome(dto.getNome());
		projeto.setDescricao(dto.getDescricao());
		projeto.setDataInicio(dto.getDataInicio());
		projeto.setDataFim(dto.getDataFim());
		projeto.setResponsavel(dto.getResponsavel());
		projeto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
	}

	private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
		if (dataInicio == null && dataFim != null) {
			throw new IllegalArgumentException("dataInicio é obrigatória quando dataFim for informada.");
		}

		if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
			throw new IllegalArgumentException("dataInicio não pode ser maior que dataFim.");
		}
	}
}
