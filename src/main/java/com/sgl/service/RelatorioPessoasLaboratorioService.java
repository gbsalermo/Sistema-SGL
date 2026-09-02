package com.sgl.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioPessoaLaboratorioItemDTO;
import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioPessoasLaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstagiarioRepository estagiarioRepository;

    @Transactional(readOnly = true)
    public RelatorioPessoasLaboratorioResponseDTO gerar(
            java.util.UUID laboratorioId,
            Perfil perfil,
            Boolean ativo) {

        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        List<Usuario> vinculados = new ArrayList<>(usuarioRepository.findByLaboratorioId(laboratorio.getId()));
        Usuario responsavel = laboratorio.getResponsavel();

        if (responsavel != null && vinculados.stream().noneMatch(usuario -> usuario.getId().equals(responsavel.getId()))) {
            vinculados.add(responsavel);
        }

        Map<Long, Estagiario> estagiariosPorUsuario = estagiarioRepository.findByLaboratorioId(laboratorio.getId())
                .stream()
                .collect(Collectors.toMap(Estagiario::getId, Function.identity(), (a, b) -> a));

        List<RelatorioPessoaLaboratorioItemDTO> pessoas = vinculados.stream()
                .filter(usuario -> perfil == null || usuario.getPerfil() == perfil)
                .filter(usuario -> ativo == null || Boolean.TRUE.equals(usuario.getAtivo()) == ativo)
                .map(usuario -> mapear(usuario, responsavel, estagiariosPorUsuario.get(usuario.getId())))
                .sorted(Comparator
                        .comparing(RelatorioPessoaLaboratorioItemDTO::getResponsavelLaboratorio).reversed()
                        .thenComparing(item -> item.getPerfil().name())
                        .thenComparing(RelatorioPessoaLaboratorioItemDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Perfil, Long> porPerfil = pessoas.stream()
                .collect(Collectors.groupingBy(
                        RelatorioPessoaLaboratorioItemDTO::getPerfil,
                        LinkedHashMap::new,
                        Collectors.counting()));

        int ativos = (int) pessoas.stream().filter(item -> Boolean.TRUE.equals(item.getAtivo())).count();

        return new RelatorioPessoasLaboratorioResponseDTO(
                LocalDateTime.now(),
                laboratorio.getPublicId(),
                laboratorio.getNome(),
                laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null,
                laboratorio.getUnidade() != null ? laboratorio.getUnidade().getNome() : null,
                responsavel != null ? responsavel.getPublicId() : null,
                responsavel != null ? responsavel.getNome() : null,
                responsavel != null ? responsavel.getEmail() : null,
                pessoas.size(),
                ativos,
                pessoas.size() - ativos,
                porPerfil,
                pessoas
        );
    }

    private RelatorioPessoaLaboratorioItemDTO mapear(
            Usuario usuario,
            Usuario responsavel,
            Estagiario estagiario) {

        return new RelatorioPessoaLaboratorioItemDTO(
                usuario.getPublicId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getAtivo(),
                responsavel != null && responsavel.getId().equals(usuario.getId()),
                estagiario != null ? estagiario.getTipoBolsa() : null,
                estagiario != null ? estagiario.getDataInicioEstagio() : null,
                estagiario != null ? estagiario.getDataFimEstagio() : null
        );
    }
}
