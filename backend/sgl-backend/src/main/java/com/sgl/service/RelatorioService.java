package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final EstagiarioRepository estagiarioRepository;
    private final LaboratorioRepository laboratorioRepository;

    @Transactional(readOnly = true)
    public RelatorioEstagiariosResponseDTO gerarRelatorioEstagiarios(
            Boolean ativo,
            UUID laboratorioId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarPeriodoOpcional(dataInicio, dataFim);

        Long laboratorioInternoId = null;
        if (laboratorioId != null) {
            Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
            laboratorioInternoId = laboratorio.getId();
        }

        final Long filtroLaboratorioId = laboratorioInternoId;

        List<Estagiario> filtrados = estagiarioRepository.findAll().stream()
                .filter(estagiario -> ativo == null || Boolean.TRUE.equals(estagiario.getAtivo()) == ativo)
                .filter(estagiario -> filtroLaboratorioId == null
                        || (estagiario.getLaboratorio() != null
                        && filtroLaboratorioId.equals(estagiario.getLaboratorio().getId())))
                .filter(estagiario -> dentroDoPeriodo(estagiario, dataInicio, dataFim))
                .sorted(Comparator.comparing(Estagiario::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int ativos = (int) filtrados.stream()
                .filter(estagiario -> Boolean.TRUE.equals(estagiario.getAtivo()))
                .count();
        int inativos = filtrados.size() - ativos;

        List<RelatorioEstagiariosResponseDTO.Item> itens = filtrados.stream()
                .map(RelatorioEstagiariosResponseDTO.Item::new)
                .toList();

        return new RelatorioEstagiariosResponseDTO(
                LocalDateTime.now(),
                filtrados.size(),
                ativos,
                inativos,
                itens
        );
    }

    private boolean dentroDoPeriodo(Estagiario estagiario, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) {
            return true;
        }

        LocalDate inicioEstagio = estagiario.getDataInicioEstagio();
        LocalDate fimEstagio = estagiario.getDataFimEstagio();

        boolean iniciouAteOFimDoPeriodo = inicioEstagio == null || !inicioEstagio.isAfter(dataFim);
        boolean naoTerminouAntesDoPeriodo = fimEstagio == null || !fimEstagio.isBefore(dataInicio);

        return iniciouAteOFimDoPeriodo && naoTerminouAntesDoPeriodo;
    }

    private void validarPeriodoOpcional(LocalDate dataInicio, LocalDate dataFim) {
        if ((dataInicio == null) != (dataFim == null)) {
            throw new BusinessRuleException(
                    "Para filtrar por período, informe dataInicio e dataFim."
            );
        }

        if (dataInicio != null && dataInicio.isAfter(dataFim)) {
            throw new BusinessRuleException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }
}
