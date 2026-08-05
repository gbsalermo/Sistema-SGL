package com.sgl.service;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.UsuarioDTO;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UsuarioService {
	
	private final UsuarioRepository usuarioRepository;
	private final LaboratorioRepository laboratorioRepository;
	private final BCryptPasswordEncoder passwordEncoder; //Para Criptografia da senha
	private final EstagiarioRepository estagiarioRepository;
	
	//Criar
	@Transactional
	public UsuarioDTO criar(UsuarioDTO dto) {
		
		//Verifico Se o usuario Existe
		if (usuarioRepository.existsByEmail(dto.getEmail())) {
			throw new RuntimeException("Email já cadastrado: " + dto.getEmail());
		}
		
		Usuario usuario = new Usuario();
		usuario.setNome(dto.getNome());
		usuario.setEmail(dto.getEmail());
		usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
		usuario.setPerfil(dto.getPerfil());
		usuario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
		
		if(dto.getLaboratorioId() != null) {
			Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
					.orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));
			usuario.setLaboratorio(laboratorio);
		}
		
		Usuario salvo = usuarioRepository.save(usuario);
		return new UsuarioDTO(salvo);
	}
	
	
	
	//Listar todos
	@Transactional
	public List<UsuarioDTO> listarTodos() {
		return usuarioRepository.findAll()
				.stream()
				.map(UsuarioDTO::new)
				.toList();
	}
	
	//Listar Por Laboratorio
	@Transactional(readOnly = true)
	public List<UsuarioDTO> listarPorLaboratorio(Long laboratotioId){
		return usuarioRepository.findByLaboratorioId(laboratotioId)
				.stream()
				.map(UsuarioDTO::new)
				.toList();
	}
	
	//Buscar Por ID
	@Transactional(readOnly = true)
	public UsuarioDTO buscarPorId(Long id) {
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado com id: " + id));
		return new UsuarioDTO(usuario);
	}
	
	//Atualizar 
	@Transactional
	public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
		
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com id"));
		
		if (usuarioRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
	        throw new IllegalArgumentException(
	                "Já existe um usuário com este email.");
	    }
		
		//Impede trocar de perfil enquanto estagio está ativo
		if (usuario.getPerfil() == Perfil.ESTAGIARIO
		        && dto.getPerfil() != Perfil.ESTAGIARIO
		        && estagiarioRepository.existsByIdAndAtivoTrue(usuario.getId())) {

		    throw new IllegalArgumentException(
		        "Finalize o estágio antes de alterar o perfil do usuário.");
		}
		
		usuario.setNome(dto.getNome());
		usuario.setEmail(dto.getEmail());
		usuario.setPerfil(dto.getPerfil());
		usuario.setAtivo(dto.getAtivo());
		if (dto.getSenha() != null
		        && !dto.getSenha().isBlank()) {

		    usuario.setSenha(
		            passwordEncoder.encode(dto.getSenha())
		    );
		}
		
		
		if(dto.getLaboratorioId() != null) {
			Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
					.orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado"));
			usuario.setLaboratorio(laboratorio);
		} else {
			usuario.setLaboratorio(null);
		}
		
		Usuario atualizado = usuarioRepository.save(usuario);
		return new UsuarioDTO(atualizado);
	}
	
	
	//Deletar
	@Transactional
	public void Inativar(Long id) {

	    Usuario usuario = usuarioRepository.findById(id)
	            .orElseThrow(() -> new EntityNotFoundException(
	                    "Usuário não encontrado com id: " + id
	            ));

	    if (!Boolean.TRUE.equals(usuario.getAtivo())) {
	        throw new IllegalArgumentException(
	                "O usuário já está inativo."
	        );
	    }

	    usuario.setAtivo(false);
	}

}
