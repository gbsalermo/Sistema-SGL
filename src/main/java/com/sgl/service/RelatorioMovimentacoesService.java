package com.sgl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import com.sgl.model.MovimentacaoRecipiente;
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
        return gerar(tipo, origem, produtoId, laboratorioId, usuarioId, loteId,
                dataInicio, dataFim, tipoEmbalagem, projetoId, sciId, atividadeId, null);
    }

    @Transactional(readOnly = true)
    public RelatorioMovimentacoesResponseDTO gerar(TipoMovimentacao tipo, OrigemMovimentacao origem, UUID produtoId,
            UUID laboratorioId, UUID usuarioId, UUID loteId, LocalDate dataInicio, LocalDate dataFim,
            TipoEmbalagem tipoEmbalagem, UUID projetoId, UUID sciId, UUID atividadeId, String formaConsumo) {
        if (formaConsumo != null && !Set.of("COMPLETA", "POR_MEDIDA", "MISTA", "NAO_DETERMINADA").contains(formaConsumo)) {
            throw new BusinessRuleException("Forma de consumo inválida.");
        }
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

        // Classificação informativa por parcela histórica: somente uma retirada de
        // toda a capacidade, a partir do recipiente cheio, é considerada completa.
        // Sem detalhe confiável, não inferir a apresentação entregue.
        UUID unidade = TenantContext.unidadeAtual().orElse(null);
        Map<UUID, List<MovimentacaoRecipiente>> detalhes = Map.of();
        if (unidade != null && !filtradas.isEmpty()) {
            detalhes = movimentacaoRecipienteRepository
                    .listarDetalhesParaRelatorio(unidade,
                            filtradas.stream().map(MovimentacaoEstoque::getPublicId).toList())
                    .stream().collect(Collectors.groupingBy(
                            detalhe -> detalhe.getMovimentacaoEstoque().getPublicId()));
        }
        final Map<UUID, List<MovimentacaoRecipiente>> detalhesPorMovimentacao = detalhes;
        List<MovimentacaoEstoqueResponseDTO> itens = filtradas.stream().map(movimentacao -> {
            var dto = new MovimentacaoEstoqueResponseDTO(movimentacao);
            List<MovimentacaoRecipiente> parcelas =
                    detalhesPorMovimentacao.getOrDefault(movimentacao.getPublicId(), List.of());
            if (tipoEmbalagem != null) {
                parcelas = parcelas.stream().filter(d -> d.getRecipienteEstoque().getTipoEmbalagem() == tipoEmbalagem).toList();
                dto.setTipoEmbalagemConsultada(tipoEmbalagem.name());
                dto.setRecipientesUtilizados((long) parcelas.size());
            }
            dto.setFormaConsumoIdentificada(classificarParcelas(parcelas));
            return dto;
        }).filter(dto -> formaConsumo == null || formaConsumo.equals(dto.getFormaConsumoIdentificada()))
                .toList();


        BigDecimal entradas = somarItens(itens, TipoMovimentacao.ENTRADA);
        BigDecimal saidas = somarItens(itens, TipoMovimentacao.SAIDA);
        BigDecimal ajustes = somarItens(itens, TipoMovimentacao.AJUSTE_ENTRADA)
                .add(somarItens(itens, TipoMovimentacao.AJUSTE_SAIDA));
        BigDecimal devolucoes = somarItens(itens, TipoMovimentacao.DEVOLUCAO);
        BigDecimal descartes = somarItens(itens, TipoMovimentacao.DESCARTE_VENCIMENTO);

		return new RelatorioMovimentacoesResponseDTO(LocalDateTime.now(), itens.size(), entradas, saidas, ajustes,
				devolucoes, descartes, itens);
	}

    private String classificarParcelas(List<MovimentacaoRecipiente> parcelas) {
        if (parcelas.isEmpty()) return "NAO_DETERMINADA";
        boolean completa = false;
        boolean medida = false;
        for (var parcela : parcelas) {
            var capacidade = parcela.getRecipienteEstoque().getCapacidadeInicial();
            var anterior = parcela.getQuantidadeAnterior();
            var retirada = parcela.getQuantidadeMovimentada();
            if (capacidade == null || anterior == null || retirada == null) return "NAO_DETERMINADA";
            if (anterior.compareTo(capacidade) == 0 && retirada.compareTo(capacidade) == 0) {
                completa = true;
            } else {
                medida = true;
            }
        }
        if (completa && medida) return "MISTA";
        return completa ? "COMPLETA" : "POR_MEDIDA";
    }

    private BigDecimal somarItens(List<MovimentacaoEstoqueResponseDTO> itens, TipoMovimentacao tipo) {
        return itens.stream().filter(item -> item.getTipoMovimentacao() == tipo)
                .map(MovimentacaoEstoqueResponseDTO::getQuantidadeMovimentada)
                .filter(java.util.Objects::nonNull)
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
