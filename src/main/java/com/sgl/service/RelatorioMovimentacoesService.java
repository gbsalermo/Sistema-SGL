package com.sgl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.repository.MovimentacaoRecipienteRepository;
import com.sgl.tenant.TenantContext;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioMovimentacoesService {

	private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final MovimentacaoRecipienteRepository movimentacaoRecipienteRepository;

	@Transactional(readOnly = true)
	public RelatorioMovimentacoesResponseDTO gerar(TipoMovimentacao tipo, OrigemMovimentacao origem, UUID produtoId,
			UUID laboratorioId, UUID usuarioId, UUID loteId, LocalDate dataInicio, LocalDate dataFim) {

        return gerar(tipo, origem, produtoId, laboratorioId, usuarioId, loteId,
                dataInicio, dataFim, null);
    }

    @Transactional(readOnly = true)
    public List<TipoEmbalagem> embalagensDoProduto(UUID produtoId) {
        UUID unidadeId = TenantContext.unidadeAtual()
                .orElseThrow(() -> new BusinessRuleException("A Unidade é obrigatória."));
        return movimentacaoRecipienteRepository.listarEmbalagensMovimentadasPorProduto(unidadeId, produtoId);
    }

    @Transactional(readOnly = true)
    public RelatorioMovimentacoesResponseDTO gerar(TipoMovimentacao tipo, OrigemMovimentacao origem, UUID produtoId,
            UUID laboratorioId, UUID usuarioId, UUID loteId, LocalDate dataInicio, LocalDate dataFim,
            TipoEmbalagem tipoEmbalagem) {
        return gerar(tipo, origem, produtoId, laboratorioId, usuarioId, loteId,
                dataInicio, dataFim, tipoEmbalagem, null, null, null);
    }

    @Transactional(readOnly = true)
    public RelatorioMovimentacoesResponseDTO gerar(TipoMovimentacao tipo, OrigemMovimentacao origem, UUID produtoId,
            UUID laboratorioId, UUID usuarioId, UUID loteId, LocalDate dataInicio, LocalDate dataFim,
            TipoEmbalagem tipoEmbalagem, UUID projetoId, UUID sciId, UUID atividadeId) {
        Set<UUID> idsEmbalagem = null;
        if (tipoEmbalagem != null) {
            UUID unidadeId = TenantContext.unidadeAtual()
                    .orElseThrow(() -> new BusinessRuleException("A Unidade é obrigatória para filtrar por embalagem."));
            idsEmbalagem = new HashSet<>(movimentacaoRecipienteRepository
                    .buscarMovimentacoesPorTipoEmbalagem(unidadeId, tipoEmbalagem));
        }
        final Set<UUID> movimentacoesDaEmbalagem = idsEmbalagem;
		validarPeriodoOpcional(dataInicio, dataFim);

		LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
		LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

		List<MovimentacaoEstoque> filtradas = movimentacaoRepository.findAll().stream()
				.filter(item -> movimentacoesDaEmbalagem == null || movimentacoesDaEmbalagem.contains(item.getPublicId()))
                .filter(item -> projetoId == null || (item.getPedido() != null
                        && item.getPedido().getProjeto() != null
                        && projetoId.equals(item.getPedido().getProjeto().getPublicId())))
                .filter(item -> sciId == null || (item.getPedido() != null
                        && item.getPedido().getSciOrigem() != null
                        && sciId.equals(item.getPedido().getSciOrigem().getPublicId())))
                .filter(item -> atividadeId == null || (item.getPedido() != null
                        && item.getPedido().getAtividadeOrigem() != null
                        && atividadeId.equals(item.getPedido().getAtividadeOrigem().getPublicId())))
                .filter(item -> tipo == null || item.getTipoMovimentacao() == tipo)
				.filter(item -> origem == null || item.getOrigem() == origem)
				.filter(item -> produtoId == null
						|| (item.getProduto() != null && produtoId.equals(item.getProduto().getPublicId())))
				.filter(item -> laboratorioId == null
						|| (item.getLaboratorio() != null && laboratorioId.equals(item.getLaboratorio().getPublicId())))
				.filter(item -> usuarioId == null
						|| (item.getUsuario() != null && usuarioId.equals(item.getUsuario().getPublicId())))
				.filter(item -> loteId == null
						|| (item.getLote() != null && loteId.equals(item.getLote().getPublicId())))
				.filter(item -> inicio == null || !item.getDataMovimentacao().isBefore(inicio))
				.filter(item -> fim == null || !item.getDataMovimentacao().isAfter(fim))
				.sorted(Comparator.comparing(MovimentacaoEstoque::getDataMovimentacao).reversed()).toList();

		BigDecimal entradas = somar(filtradas, TipoMovimentacao.ENTRADA);
		BigDecimal saidas = somar(filtradas, TipoMovimentacao.SAIDA);
		BigDecimal ajustes = somar(filtradas, TipoMovimentacao.AJUSTE_ENTRADA)
				.add(somar(filtradas, TipoMovimentacao.AJUSTE_SAIDA));
		BigDecimal devolucoes = somar(filtradas, TipoMovimentacao.DEVOLUCAO);
		BigDecimal descartes = somar(filtradas, TipoMovimentacao.DESCARTE_VENCIMENTO);

		List<MovimentacaoEstoqueResponseDTO> itens = filtradas.stream().map(MovimentacaoEstoqueResponseDTO::new)
				.toList();

		return new RelatorioMovimentacoesResponseDTO(LocalDateTime.now(), filtradas.size(), entradas, saidas, ajustes,
				devolucoes, descartes, itens);
	}

	private BigDecimal somar(List<MovimentacaoEstoque> movimentacoes, TipoMovimentacao tipo) {

		return movimentacoes.stream().filter(item -> item.getTipoMovimentacao() == tipo)
				.map(MovimentacaoEstoque::getQuantidadeMovimentada).filter(java.util.Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private void validarPeriodoOpcional(LocalDate dataInicio, LocalDate dataFim) {
		if ((dataInicio == null) != (dataFim == null)) {
			throw new BusinessRuleException("Para filtrar por período, informe dataInicio e dataFim.");
		}
		if (dataInicio != null && dataInicio.isAfter(dataFim)) {
			throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
		}
	}
}
