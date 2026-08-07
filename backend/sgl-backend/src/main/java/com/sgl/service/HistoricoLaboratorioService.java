package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Projeto;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoricoLaboratorioService {

    private final HistoricoLaboratorioRepository historicoLaboratorioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final ProjetoRepository projetoRepository;

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarTodos() {
        return historicoLaboratorioRepository.findAll().stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoricoLaboratorioDTO buscarPorId(Long id) {
        return historicoLaboratorioRepository.findById(id)
                .map(HistoricoLaboratorioDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Histórico de laboratório", id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorLaboratorio(Long laboratorioId) {
        validarLaboratorio(laboratorioId);

        return historicoLaboratorioRepository.findByLaboratorioId(laboratorioId).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorProduto(Long produtoId) {
        return historicoLaboratorioRepository.findByProdutoId(produtoId).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorPedido(Long pedidoId) {
        return historicoLaboratorioRepository.findByPedidoId(pedidoId).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorPeriodo(
            Long laboratorioId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarLaboratorio(laboratorioId);
        validarPeriodo(dataInicio, dataFim);

        return historicoLaboratorioRepository
                .findByLaboratorioIdAndPeriodo(laboratorioId, dataInicio, dataFim)
                .stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    /**
     * Materiais efetivamente recebidos pelo projeto dentro do laboratório e do
     * período informados. Esta consulta representa consumo/recebimento, não
     * apenas solicitações criadas.
     */
    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorProjetoEPeriodo(
            Long laboratorioId,
            Long projetoId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarLaboratorio(laboratorioId);
        validarProjetoDoLaboratorio(projetoId, laboratorioId);
        validarPeriodo(dataInicio, dataFim);

        return historicoLaboratorioRepository
                .findByLaboratorioProjetoEPeriodo(
                        laboratorioId,
                        projetoId,
                        dataInicio,
                        dataFim
                )
                .stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    private void validarLaboratorio(Long laboratorioId) {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new ResourceNotFoundException("Laboratório", laboratorioId);
        }
    }

    private void validarProjetoDoLaboratorio(Long projetoId, Long laboratorioId) {
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));

        if (projeto.getLaboratorio() == null
                || !projeto.getLaboratorio().getId().equals(laboratorioId)) {
            throw new BusinessRuleException(
                    "O projeto informado não pertence ao laboratório informado."
            );
        }
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) {
            throw new BusinessRuleException("Data inicial e data final são obrigatórias.");
        }

        if (dataInicio.isAfter(dataFim)) {
            throw new BusinessRuleException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }
}
