package com.sgl.service;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.UsuarioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EstagiarioRepository estagiarioRepository;

    @Transactional
    public UsuarioDTO criar(UsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado: " + dto.getEmail());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(dto.getPerfil());
        usuario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        if (dto.getLaboratorioId() != null) {
            Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Laboratório",
                            dto.getLaboratorioId()
                    ));
            usuario.setLaboratorio(laboratorio);
        }

        return new UsuarioDTO(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarPorLaboratorio(Long laboratorioId) {
        return usuarioRepository.findByLaboratorioId(laboratorioId).stream()
                .map(UsuarioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return new UsuarioDTO(usuario);
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (usuarioRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new BusinessRuleException("Já existe um usuário com este email.");
        }

        if (usuario.getPerfil() == Perfil.ESTAGIARIO
                && dto.getPerfil() != Perfil.ESTAGIARIO
                && estagiarioRepository.existsByIdAndAtivoTrue(usuario.getId())) {
            throw new BusinessRuleException(
                    "Finalize o estágio antes de alterar o perfil do usuário."
            );
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setPerfil(dto.getPerfil());
        usuario.setAtivo(dto.getAtivo());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        if (dto.getLaboratorioId() != null) {
            Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Laboratório",
                            dto.getLaboratorioId()
                    ));
            usuario.setLaboratorio(laboratorio);
        } else {
            usuario.setLaboratorio(null);
        }

        return new UsuarioDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void Inativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException("O usuário já está inativo.");
        }

        usuario.setAtivo(false);
    }
}
