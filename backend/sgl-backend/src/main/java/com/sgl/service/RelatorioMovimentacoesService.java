package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioMovimentacoesService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    @Transactional(readOnly = true)
    public RelatorioMovimentacoesResponseDTO gerar(
            TipoMovimentacao tipo,
            OrigemMovimentacao origem,
            UUID produtoId,
            UUID laboratorioId,
            UUID usuarioId,
            UUID loteId,
            LocalDate dataInicio,
            LocalDate dataFim) {

        validarPeriodoOpcional(dataInicio, dataFim);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        List<MovimentacaoEstoque> filtradas = movimentacaoRepository.findAll().stream()
                .filter(item -> tipo == null || item.getTipoMovimentacao() == tipo)
                .filter(item -> origem == null || item.getOrigem() == origem)
                .filter(item -> produtoId == null || (item.getProduto() != null && produtoId.equals(item.getProduto().getPublicId())))
                .filter(item -> laboratorioId == null || (item.getLaboratorio() != null && laboratorioId.equals(item.getLaboratorio().getPublicId())))
                .filter(item -> usuarioId == null || (item.getUsuario() != null && usuarioId.equals(item.getUsuario().getPublicId())))
                .filter(item -> loteId == null || (item.getLote() != null && loteId.equals(item.getLote().getPublicId())))
                .filter(item -> inicio == null || !item.getDataMovimentacao().isBefore(inicio))
                .filter(item -> fim == null || !item.getDataMovimentacao().isAfter(fim))
                .sorted(Comparator.comparing(MovimentacaoEstoque::getDataMovimentacao).reversed())
                .toList();

        int entradas = somar(filtradas, TipoMovimentacao.ENTRADA);
        int saidas = somar(filtradas, TipoMovimentacao.SAIDA);
        int ajustes = somar(filtradas, TipoMovimentacao.AJUSTE);
        int devolucoes = somar(filtradas, TipoMovimentacao.DEVOLUCAO);
        int descartes = somar(filtradas, TipoMovimentacao.DESCARTE_VENCIMENTO);

        List<MovimentacaoEstoqueResponseDTO> itens = filtradas.stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();

        return new RelatorioMovimentacoesResponseDTO(
                LocalDateTime.now(),
                filtradas.size(),
                entradas,
                saidas,
                ajustes,
                devolucoes,
                descartes,
                itens
        );
    }

    private int somar(List<MovimentacaoEstoque> movimentacoes, TipoMovimentacao tipo) {
        return movimentacoes.stream()
                .filter(item -> item.getTipoMovimentacao() == tipo)
                .map(MovimentacaoEstoque::getQuantidadeMovimentada)
                .filter(valor -> valor != null)
                .mapToInt(Integer::intValue)
                .sum();
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
