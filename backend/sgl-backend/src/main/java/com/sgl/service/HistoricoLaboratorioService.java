package com.sgl.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ConsumoProdutoLaboratorioDTO;
import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoricoLaboratorioService {

    private final HistoricoLaboratorioRepository historicoLaboratorioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final ProjetoRepository projetoRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarTodos() {
        return historicoLaboratorioRepository.findAll().stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoricoLaboratorioDTO buscarPorId(UUID id) {
        return historicoLaboratorioRepository.findByPublicId(id)
                .map(HistoricoLaboratorioDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Histórico de laboratório", id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);

        return historicoLaboratorioRepository.findByLaboratorioId(laboratorio.getId()).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorProduto(UUID produtoId) {
        Produto produto = buscarProduto(produtoId);

        return historicoLaboratorioRepository.findByProdutoId(produto.getId()).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorPedido(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findByPublicId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));

        return historicoLaboratorioRepository.findByPedidoId(pedido.getId()).stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorPeriodo(
            UUID laboratorioId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
        validarPeriodo(dataInicio, dataFim);

        return historicoLaboratorioRepository
                .findByLaboratorioIdAndPeriodo(laboratorio.getId(), dataInicio, dataFim)
                .stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    // Includes zero-consumption months when calculating the monthly average.
    @Transactional(readOnly = true)
    public ConsumoProdutoLaboratorioDTO calcularConsumoProduto(
            UUID laboratorioId,
            UUID produtoId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarPeriodo(dataInicio, dataFim);

        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
        Produto produto = buscarProduto(produtoId);

        List<HistoricoLaboratorio> registros = historicoLaboratorioRepository
                .findByLaboratorioProdutoEPeriodo(
                        laboratorio.getId(),
                        produto.getId(),
                        dataInicio,
                        dataFim
                );

        int quantidadeTotal = registros.stream()
                .mapToInt(HistoricoLaboratorio::getQuantidade)
                .sum();

        long quantidadePedidos = registros.stream()
                .map(HistoricoLaboratorio::getPedido)
                .filter(pedido -> pedido != null && pedido.getId() != null)
                .map(Pedido::getId)
                .distinct()
                .count();

        int mesesConsiderados = Math.toIntExact(
                ChronoUnit.MONTHS.between(
                        YearMonth.from(dataInicio),
                        YearMonth.from(dataFim)
                ) + 1
        );

        BigDecimal mediaQuantidadePorPedido = quantidadePedidos == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(quantidadeTotal)
                        .divide(BigDecimal.valueOf(quantidadePedidos), 2, RoundingMode.HALF_UP);

        BigDecimal mediaConsumoMensal = BigDecimal.valueOf(quantidadeTotal)
                .divide(BigDecimal.valueOf(mesesConsiderados), 2, RoundingMode.HALF_UP);

        int quantidadeMinimaSugerida = mediaConsumoMensal
                .setScale(0, RoundingMode.CEILING)
                .intValue();

        return new ConsumoProdutoLaboratorioDTO(
                laboratorio.getPublicId(),
                laboratorio.getNome(),
                produto.getPublicId(),
                produto.getNome(),
                produto.getUnidadeArmazenamento(),
                dataInicio,
                dataFim,
                quantidadePedidos,
                quantidadeTotal,
                mediaQuantidadePorPedido,
                mesesConsiderados,
                mediaConsumoMensal,
                quantidadeMinimaSugerida
        );
    }

    // Uses delivered material history, not merely created requests.
    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioDTO> listarPorProjetoEPeriodo(
            UUID laboratorioId,
            UUID projetoId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
        Projeto projeto = buscarProjetoDoLaboratorio(projetoId, laboratorio);
        validarPeriodo(dataInicio, dataFim);

        return historicoLaboratorioRepository
                .findByLaboratorioProjetoEPeriodo(
                        laboratorio.getId(),
                        projeto.getId(),
                        dataInicio,
                        dataFim
                )
                .stream()
                .map(HistoricoLaboratorioDTO::new)
                .toList();
    }

    private Laboratorio buscarLaboratorio(UUID laboratorioId) {
        return laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
    }

    private Produto buscarProduto(UUID produtoId) {
        return produtoRepository.findByPublicId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));
    }

    private Projeto buscarProjetoDoLaboratorio(UUID projetoId, Laboratorio laboratorio) {
        Projeto projeto = projetoRepository.findByPublicId(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));

        if (projeto.getLaboratorio() == null
                || !projeto.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException(
                    "O projeto informado não pertence ao laboratório informado."
            );
        }

        return projeto;
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
