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

import com.sgl.dto.response.ConsumoProdutoLaboratorioResponseDTO;
import com.sgl.dto.response.HistoricoLaboratorioResponseDTO;
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
import com.sgl.tenant.TenantContext;

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
    public List<HistoricoLaboratorioResponseDTO> listarTodos() {
        // Correção de segurança: este método fazia "findAll()" sem
        // NENHUM filtro de unidade — diferente dos outros services do
        // sistema, este nunca tinha sido adaptado para multitenancy.
        // Qualquer chamada devolvia o histórico de recebimento de material
        // de todos os laboratórios de todas as unidades.
        exigirTenantAtivo();

        return historicoLaboratorioRepository
                .findByLaboratorioUnidadePublicId(TenantContext.unidadeAtual().orElseThrow())
                .stream()
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoricoLaboratorioResponseDTO buscarPorId(UUID id) {
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return historicoLaboratorioRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId)
                .map(HistoricoLaboratorioResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Histórico de laboratório", id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);

        return historicoLaboratorioRepository.findByLaboratorioId(laboratorio.getId()).stream()
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioResponseDTO> listarPorProduto(UUID produtoId) {
        // Produto é um catálogo compartilhado entre unidades (ver
        // ProdutoService), então buscarProduto() continua sem filtro de
        // unidade. Mas o HISTÓRICO em si pertence a um laboratório de uma
        // unidade específica, então a consulta abaixo precisa do tenant.
        Produto produto = buscarProduto(produtoId);
        exigirTenantAtivo();

        return historicoLaboratorioRepository
                .findByProdutoIdAndLaboratorioUnidadePublicId(produto.getId(), TenantContext.unidadeAtual().orElseThrow())
                .stream()
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioResponseDTO> listarPorPedido(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findByPublicId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
        exigirTenantAtivo();

        return historicoLaboratorioRepository
                .findByPedidoIdAndLaboratorioUnidadePublicId(pedido.getId(), TenantContext.unidadeAtual().orElseThrow())
                .stream()
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioResponseDTO> listarPorPeriodo(
            UUID laboratorioId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
        validarPeriodo(dataInicio, dataFim);

        return historicoLaboratorioRepository
                .findByLaboratorioIdAndPeriodo(laboratorio.getId(), dataInicio, dataFim)
                .stream()
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    // Inclui meses sem consumo no cálculo da média mensal.
    @Transactional(readOnly = true)
    public ConsumoProdutoLaboratorioResponseDTO calcularConsumoProduto(
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

        return new ConsumoProdutoLaboratorioResponseDTO(
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

    // Usa materiais efetivamente entregues, e não apenas pedidos criados.
    @Transactional(readOnly = true)
    public List<HistoricoLaboratorioResponseDTO> listarPorProjetoEPeriodo(
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
                .map(HistoricoLaboratorioResponseDTO::new)
                .toList();
    }

    private Laboratorio buscarLaboratorio(UUID laboratorioId) {
        // Correção de segurança: antes buscava o laboratório sem checar se
        // ele pertence à unidade de quem está chamando, permitindo consultar
        // o histórico de qualquer laboratório de outra unidade só sabendo o
        // id dele.
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return laboratorioRepository.findByPublicIdAndUnidadePublicId(laboratorioId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
    }

    /**
     * Garante que existe uma unidade (tenant) definida para a requisição
     * atual. Ver o mesmo método em EstoqueCentralService para a explicação
     * completa do porquê essa checagem existe.
     */
    private void exigirTenantAtivo() {
        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        }
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
