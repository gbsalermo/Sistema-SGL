package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.repository.HistoricoLaboratorioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoricoLaboratorioService {

    private final HistoricoLaboratorioRepository historicoLaboratorioRepository;

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

        return historicoLaboratorioRepository
                .findByLaboratorioIdAndPeriodo(laboratorioId, dataInicio, dataFim)
                .stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }
}
