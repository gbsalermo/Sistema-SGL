package com.sgl.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ConsumoProdutoLaboratorioDTO;
import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
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
        validarProduto(produtoId);

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
     * Calcula indicadores de consumo efetivo de um produto por laboratório.
     *
     * A fonte é HistoricoLaboratorio, portanto entram no cálculo apenas itens
     * efetivamente entregues. A média mensal considera todos os meses do
     * intervalo, inclusive meses sem recebimento. A quantidade mínima sugerida
     * é a média mensal arredondada para cima e serve somente como referência;
     * o método não altera automaticamente o EstoqueCentral.
     */
    @Transactional(readOnly = true)
    public ConsumoProdutoLaboratorioDTO calcularConsumoProduto(
            Long laboratorioId,
            Long produtoId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarPeriodo(dataInicio, dataFim);

        Laboratorio laboratorio = laboratorioRepository.findById(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        List<HistoricoLaboratorio> registros = historicoLaboratorioRepository
                .findByLaboratorioProdutoEPeriodo(
                        laboratorioId,
                        produtoId,
                        dataInicio,
                        dataFim
                );

        int quantidadeTotal = registros.stream()
                .mapToInt(HistoricoLaboratorio::getQuantidade)
                .sum();

        long quantidadePedidos = registros.stream()
                .map(HistoricoLaboratorio::getPedido)
                .filter(pedido -> pedido != null && pedido.getId() != null)
                .map(pedido -> pedido.getId())
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
                laboratorio.getId(),
                laboratorio.getNome(),
                produto.getId(),
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

    private void validarProduto(Long produtoId) {
        if (!produtoRepository.existsById(produtoId)) {
            throw new ResourceNotFoundException("Produto", produtoId);
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
