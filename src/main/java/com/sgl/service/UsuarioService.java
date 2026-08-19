package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UnidadeRepository unidadeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EstagiarioRepository estagiarioRepository;

    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado: " + dto.getEmail());
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new BusinessRuleException("Senha é obrigatória na criação do usuário.");
        }

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Laboratorio laboratorio = buscarLaboratorioCompativel(dto.getLaboratorioId(), unidade);

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(dto.getPerfil());
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);
        usuario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        return new UsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        return usuarioRepository.findByLaboratorioId(laboratorio.getId()).stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(UUID id) {
        Usuario usuario = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        return new UsuarioResponseDTO(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(UUID id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (usuarioRepository.existsByEmailAndIdNot(dto.getEmail(), usuario.getId())) {
            throw new BusinessRuleException("Já existe um usuário com este email.");
        }

        if (usuario.getPerfil() == Perfil.ESTAGIARIO
                && dto.getPerfil() != Perfil.ESTAGIARIO
                && estagiarioRepository.existsByIdAndAtivoTrue(usuario.getId())) {
            throw new BusinessRuleException(
                    "Finalize o estágio antes de alterar o perfil do usuário."
            );
        }

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Laboratorio laboratorio = buscarLaboratorioCompativel(dto.getLaboratorioId(), unidade);

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setPerfil(dto.getPerfil());
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        return new UsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void Inativar(UUID id) {
        Usuario usuario = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException("O usuário já está inativo.");
        }

        usuario.setAtivo(false);
    }

    private Laboratorio buscarLaboratorioCompativel(UUID laboratorioId, Unidade unidade) {
        if (laboratorioId == null) {
            return null;
        }

        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        if (laboratorio.getUnidade() == null
                || !laboratorio.getUnidade().getId().equals(unidade.getId())) {
            throw new BusinessRuleException(
                    "O laboratório informado não pertence à unidade do usuário."
            );
        }

        return laboratorio;
    }
}
