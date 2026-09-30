package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EncerrarVinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioAtividadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VinculoEstagioAtividadeService {

    private final VinculoEstagioAtividadeRepository participacaoRepository;
    private final VinculoEstagioRepository vinculoEstagioRepository;
    private final AtividadeRepository atividadeRepository;

    @Transactional
    public VinculoEstagioAtividadeResponseDTO adicionar(
            UUID vinculoId,
            VinculoEstagioAtividadeRequestDTO dto) {

        VinculoEstagio vinculo =
                buscarVinculoNoTenant(vinculoId);

        validarVinculoOperacional(vinculo);

        Atividade atividade =
                buscarAtividadeNoTenant(dto.getAtividadeId());

        validarAtividadeOperacional(atividade);

        validarMesmaUnidade(vinculo, atividade);

        validarPeriodo(
                vinculo,
                atividade,
                dto.getDataInicioParticipacao(),
                null
        );

        boolean jaPossuiParticipacaoAberta =
                participacaoRepository
                        .existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
                                vinculo.getId(),
                                atividade.getId()
                        );

        if (jaPossuiParticipacaoAberta) {
            throw new BusinessRuleException(
                    "O vínculo de estágio já possui participação ativa nesta Atividade."
            );
        }

        VinculoEstagioAtividade participacao =
                new VinculoEstagioAtividade();

        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);

        participacao.setDataInicioParticipacao(
                dto.getDataInicioParticipacao()
        );

        participacao.setObservacao(
                normalizarObservacao(dto.getObservacao())
        );

        participacao =
                participacaoRepository.save(participacao);

        return new VinculoEstagioAtividadeResponseDTO(
                participacao
        );
    }

    @Transactional(readOnly = true)
    public List<VinculoEstagioAtividadeResponseDTO>
            listarPorVinculo(UUID vinculoId) {

        VinculoEstagio vinculo =
                buscarVinculoNoTenant(vinculoId);

        UUID unidadeId =
                TenantContext.unidadeAtual()
                        .orElseThrow();

        return participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        vinculo.getPublicId(),
                        unidadeId
                )
                .stream()
                .map(VinculoEstagioAtividadeResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoEstagioAtividadeResponseDTO>
            listarAtivasPorVinculo(UUID vinculoId) {

        VinculoEstagio vinculo =
                buscarVinculoNoTenant(vinculoId);

        UUID unidadeId =
                TenantContext.unidadeAtual()
                        .orElseThrow();

        return participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdAndDataFimParticipacaoIsNull(
                        vinculo.getPublicId(),
                        unidadeId
                )
                .stream()
                .map(VinculoEstagioAtividadeResponseDTO::new)
                .toList();
    }

    @Transactional
    public VinculoEstagioAtividadeResponseDTO encerrar(
            UUID participacaoId,
            EncerrarVinculoEstagioAtividadeRequestDTO dto) {

        VinculoEstagioAtividade participacao =
                buscarParticipacaoNoTenant(participacaoId);

        if (participacao.getDataFimParticipacao() != null) {
            throw new BusinessRuleException(
                    "A participação nesta Atividade já está encerrada."
            );
        }

        VinculoEstagio vinculo =
                participacao.getVinculoEstagio();

        validarVinculoOperacional(vinculo);

        validarPeriodo(
                vinculo,
                participacao.getAtividade(),
                participacao.getDataInicioParticipacao(),
                dto.getDataFimParticipacao()
        );

        participacao.setDataFimParticipacao(
                dto.getDataFimParticipacao()
        );

        return new VinculoEstagioAtividadeResponseDTO(
                participacaoRepository.save(participacao)
        );
    }

    private VinculoEstagio buscarVinculoNoTenant(
            UUID vinculoId) {

        exigirTenantAtivo();

        UUID unidadeId =
                TenantContext.unidadeAtual()
                        .orElseThrow();

        return vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        vinculoId,
                        unidadeId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vínculo de estágio",
                                vinculoId
                        )
                );
    }

    private Atividade buscarAtividadeNoTenant(
            UUID atividadeId) {

        exigirTenantAtivo();

        UUID unidadeId =
                TenantContext.unidadeAtual()
                        .orElseThrow();

        return atividadeRepository
                .findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        atividadeId,
                        unidadeId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Atividade",
                                atividadeId
                        )
                );
    }

    private VinculoEstagioAtividade buscarParticipacaoNoTenant(
            UUID participacaoId) {

        exigirTenantAtivo();

        UUID unidadeId =
                TenantContext.unidadeAtual()
                        .orElseThrow();

        return participacaoRepository
                .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                        participacaoId,
                        unidadeId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Participação em Atividade",
                                participacaoId
                        )
                );
    }

    private void validarVinculoOperacional(
            VinculoEstagio vinculo) {

        if (!Boolean.TRUE.equals(
                vinculo.getEstagiario().getAtivo())) {

            throw new BusinessRuleException(
                    "O usuário do vínculo de estágio está inativo."
            );
        }

        if (vinculo.getSituacao()
                == SituacaoEstagio.FINALIZADO) {

            throw new BusinessRuleException(
                    "Não é possível alterar Atividades de um vínculo de estágio finalizado."
            );
        }
    }

    private void validarAtividadeOperacional(
            Atividade atividade) {

        atividade.validateActive();

        Sci sci = atividade.getSci();

        if (sci == null) {
            throw new BusinessRuleException(
                    "A Atividade informada não possui SCI válido."
            );
        }

        sci.validateActive();

        Projeto projeto = sci.getProjeto();

        if (projeto == null) {
            throw new BusinessRuleException(
                    "A Atividade informada não possui Projeto válido."
            );
        }

        projeto.validateActive();

        Laboratorio laboratorio =
                projeto.getLaboratorio();

        if (laboratorio == null) {
            throw new BusinessRuleException(
                    "A Atividade informada não possui Laboratório válido."
            );
        }

        laboratorio.validateActive();
    }

    private void validarMesmaUnidade(
            VinculoEstagio vinculo,
            Atividade atividade) {

        UUID unidadeEstagiario =
                vinculo.getEstagiario()
                        .getUnidade()
                        .getPublicId();

        UUID unidadeAtividade =
                atividade.getSci()
                        .getProjeto()
                        .getLaboratorio()
                        .getUnidade()
                        .getPublicId();

        if (!unidadeEstagiario.equals(
                unidadeAtividade)) {

            throw new BusinessRuleException(
                    "O Estagiário e a Atividade devem pertencer à mesma Unidade."
            );
        }
    }

    private void validarPeriodo(
            VinculoEstagio vinculo,
            Atividade atividade,
            LocalDate inicio,
            LocalDate fim) {

        if (inicio == null) {
            throw new BusinessRuleException(
                    "Data de início da participação é obrigatória."
            );
        }

        if (fim != null && fim.isBefore(inicio)) {
            throw new BusinessRuleException(
                    "Data de fim da participação não pode ser anterior à data de início."
            );
        }

        if (inicio.isBefore(
                vinculo.getDataInicio())) {

            throw new BusinessRuleException(
                    "A participação não pode começar antes do vínculo de estágio."
            );
        }

        if (vinculo.getDataFimPrevista() != null
                && inicio.isAfter(
                        vinculo.getDataFimPrevista())) {

            throw new BusinessRuleException(
                    "A participação não pode começar após o fim previsto do vínculo de estágio."
            );
        }

        if (atividade.getDataInicio() != null
                && inicio.isBefore(
                        atividade.getDataInicio())) {

            throw new BusinessRuleException(
                    "A participação não pode começar antes da Atividade."
            );
        }

        if (atividade.getDataFim() != null
                && inicio.isAfter(
                        atividade.getDataFim())) {

            throw new BusinessRuleException(
                    "A participação não pode começar após o fim da Atividade."
            );
        }

        if (fim != null) {

            if (vinculo.getDataFimPrevista() != null
                    && fim.isAfter(
                            vinculo.getDataFimPrevista())) {

                throw new BusinessRuleException(
                        "A participação não pode terminar após o fim previsto do vínculo de estágio."
                );
            }

            if (atividade.getDataFim() != null
                    && fim.isAfter(
                            atividade.getDataFim())) {

                throw new BusinessRuleException(
                        "A participação não pode terminar após o fim da Atividade."
                );
            }
        }
    }

    private String normalizarObservacao(
            String observacao) {

        if (observacao == null
                || observacao.isBlank()) {
            return null;
        }

        return observacao.trim();
    }

    private void exigirTenantAtivo() {

        if (!TenantContext.ativo()) {

            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação."
            );
        }
    }
}