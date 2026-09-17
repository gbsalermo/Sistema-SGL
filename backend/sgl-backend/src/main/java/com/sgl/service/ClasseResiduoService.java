package com.sgl.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ClasseResiduoRequestDTO;
import com.sgl.dto.response.ClasseResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.Unidade;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClasseResiduoService {

    private final ClasseResiduoRepository classeResiduoRepository;
    private final UnidadeRepository unidadeRepository;

    @Transactional
    public ClasseResiduoResponseDTO criar(
            ClasseResiduoRequestDTO dto) {

        Unidade unidade = buscarUnidade(dto.getUnidadeId());

        validarTenantUnidade(unidade.getPublicId());

        String codigo = normalizarCodigo(dto.getCodigo());

        validarCodigoDuplicado(
                unidade,
                codigo,
                null
        );

        ClasseResiduo classe = ClasseResiduo.builder()
                .unidade(unidade)
                .codigo(codigo)
                .descricao(dto.getDescricao().trim())
                .ativo(dto.getAtivo() != null
                        ? dto.getAtivo()
                        : true)
                .build();

        return new ClasseResiduoResponseDTO(
                classeResiduoRepository.save(classe)
        );
    }

    @Transactional(readOnly = true)
    public List<ClasseResiduoResponseDTO> listarTodos() {

        List<ClasseResiduo> classes =
                TenantContext.unidadeAtual()
                        .map(classeResiduoRepository::
                                findByUnidadePublicIdOrderByCodigoAsc)
                        .orElseGet(classeResiduoRepository::findAll);

        return classes.stream()
                .map(ClasseResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClasseResiduoResponseDTO> listarAtivos() {

        List<ClasseResiduo> classes =
                TenantContext.unidadeAtual()
                        .map(classeResiduoRepository::
                                findByUnidadePublicIdAndAtivoTrueOrderByCodigoAsc)
                        .orElseGet(classeResiduoRepository::
                                findByAtivoTrueOrderByCodigoAsc);

        return classes.stream()
                .map(ClasseResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClasseResiduoResponseDTO buscarPorId(UUID id) {
        return new ClasseResiduoResponseDTO(
                buscarClasseNoTenant(id)
        );
    }

    @Transactional
    public ClasseResiduoResponseDTO atualizar(
            UUID id,
            ClasseResiduoRequestDTO dto) {

        ClasseResiduo classe =
                buscarClasseNoTenant(id);

        validarTenantUnidade(
                classe.getUnidade().getPublicId()
        );

        if (!classe.getUnidade()
                .getPublicId()
                .equals(dto.getUnidadeId())) {

            throw new BusinessRuleException(
                    "A classe de resíduo não pode ser transferida para outra unidade."
            );
        }

        String codigo =
                normalizarCodigo(dto.getCodigo());

        validarCodigoDuplicado(
                classe.getUnidade(),
                codigo,
                classe.getId()
        );

        classe.setCodigo(codigo);
        classe.setDescricao(
                dto.getDescricao().trim()
        );

        if (dto.getAtivo() != null) {
            classe.setAtivo(dto.getAtivo());
        }

        return new ClasseResiduoResponseDTO(
                classeResiduoRepository.save(classe)
        );
    }

    @Transactional
    public void deletar(UUID id) {
        ClasseResiduo classe =
                buscarClasseNoTenant(id);

        classe.setAtivo(false);
    }

    private ClasseResiduo buscarClasseNoTenant(UUID id) {

        return TenantContext.unidadeAtual()
                .flatMap(unidadeId ->
                        classeResiduoRepository
                                .findByPublicIdAndUnidadePublicId(
                                        id,
                                        unidadeId
                                )
                )
                .orElseGet(() -> {

                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException(
                                "Classe de resíduo",
                                id
                        );
                    }

                    return classeResiduoRepository
                            .findByPublicId(id)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Classe de resíduo",
                                            id
                                    )
                            );
                });
    }

    private Unidade buscarUnidade(UUID unidadeId) {

        return unidadeRepository
                .findByPublicId(unidadeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Unidade",
                                unidadeId
                        )
                );
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException(
                    "A operação não pode acessar dados de outra unidade."
            );
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private void validarCodigoDuplicado(
            Unidade unidade,
            String codigo,
            Long idAtual) {

        boolean duplicado;

        if (idAtual == null) {

            duplicado =
                    classeResiduoRepository
                            .existsByUnidadeIdAndCodigoIgnoreCase(
                                    unidade.getId(),
                                    codigo
                            );

        } else {

            duplicado =
                    classeResiduoRepository
                            .existsByUnidadeIdAndCodigoIgnoreCaseAndIdNot(
                                    unidade.getId(),
                                    codigo,
                                    idAtual
                            );
        }

        if (duplicado) {
            throw new BusinessRuleException(
                    "Já existe uma classe de resíduo com este código na unidade."
            );
        }
    }
}