package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioResumoOperacionalService {

    private static final int LIMITE_PADRAO = 5;
    private static final int LIMITE_MAXIMO = 50;

    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    @Transactional(readOnly = true)
    public RelatorioResumoOperacionalResponseDTO gerar(
            UUID produtoId,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer limite) {

        validarPeriodoOpcional(dataInicio, dataFim);
        int limiteEfetivo = validarLimite(limite);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        List<MovimentacaoEstoque> filtradas = movimentacaoRepository.findAll().stream()
                .filter(item -> produtoId == null
                        || (item.getProduto() != null && produtoId.equals(item.getProduto().getPublicId())))
                .filter(item -> inicio == null || !item.getDataMovimentacao().isBefore(inicio))
                .filter(item -> fim == null || !item.getDataMovimentacao().isAfter(fim))
                .toList();

        Map<UUID, ProdutoAcumulado> entradasPorProduto = new HashMap<>();
        Map<UUID, ProdutoAcumulado> saidasPorProduto = new HashMap<>();
        Map<UUID, LoteAcumulado> movimentacaoPorLote = new HashMap<>();

        int quantidadeEntradas = 0;
        int quantidadeSaidas = 0;
        int quantidadeDescartes = 0;

        for (MovimentacaoEstoque item : filtradas) {
            int quantidade = item.getQuantidadeMovimentada() == null ? 0 : item.getQuantidadeMovimentada();
            Produto produto = item.getProduto();

            if (item.getTipoMovimentacao() == TipoMovimentacao.ENTRADA) {
                quantidadeEntradas += quantidade;
                acumularProduto(entradasPorProduto, produto, quantidade);
            } else if (item.getTipoMovimentacao() == TipoMovimentacao.SAIDA) {
                quantidadeSaidas += quantidade;
                acumularProduto(saidasPorProduto, produto, quantidade);
            } else if (item.getTipoMovimentacao() == TipoMovimentacao.DESCARTE_VENCIMENTO) {
                quantidadeDescartes += quantidade;
            }

            if (item.getLote() != null) {
                acumularLote(movimentacaoPorLote, item.getLote(), item.getTipoMovimentacao(), quantidade);
            }
        }

        List<RelatorioResumoOperacionalResponseDTO.ProdutoRanking> principaisEntradas = entradasPorProduto.values().stream()
                .sorted(Comparator.comparingInt(ProdutoAcumulado::getQuantidade).reversed()
                        .thenComparing(ProdutoAcumulado::getNome, String.CASE_INSENSITIVE_ORDER))
                .limit(limiteEfetivo)
                .map(ProdutoAcumulado::toDto)
                .toList();

        List<RelatorioResumoOperacionalResponseDTO.ProdutoRanking> principaisSaidas = saidasPorProduto.values().stream()
                .sorted(Comparator.comparingInt(ProdutoAcumulado::getQuantidade).reversed()
                        .thenComparing(ProdutoAcumulado::getNome, String.CASE_INSENSITIVE_ORDER))
                .limit(limiteEfetivo)
                .map(ProdutoAcumulado::toDto)
                .toList();

        List<RelatorioResumoOperacionalResponseDTO.LoteRanking> lotesMaisMovimentados = movimentacaoPorLote.values().stream()
                .sorted(Comparator.comparingInt(LoteAcumulado::getQuantidadeMovimentada).reversed()
                        .thenComparing(LoteAcumulado::getCodigoInterno, String.CASE_INSENSITIVE_ORDER))
                .limit(limiteEfetivo)
                .map(LoteAcumulado::toDto)
                .toList();

        int produtosMovimentados = (int) filtradas.stream()
                .map(MovimentacaoEstoque::getProduto)
                .filter(java.util.Objects::nonNull)
                .map(Produto::getPublicId)
                .distinct()
                .count();

        return new RelatorioResumoOperacionalResponseDTO(
                LocalDateTime.now(),
                filtradas.size(),
                quantidadeEntradas,
                quantidadeSaidas,
                quantidadeDescartes,
                produtosMovimentados,
                movimentacaoPorLote.size(),
                principaisEntradas,
                principaisSaidas,
                lotesMaisMovimentados
        );
    }

    private void acumularProduto(Map<UUID, ProdutoAcumulado> destino, Produto produto, int quantidade) {
        if (produto == null || produto.getPublicId() == null) {
            return;
        }

        ProdutoAcumulado acumulado = destino.computeIfAbsent(
                produto.getPublicId(),
                chave -> new ProdutoAcumulado(produto.getPublicId(), produto.getNome())
        );
        acumulado.adicionar(quantidade);
    }

    private void acumularLote(
            Map<UUID, LoteAcumulado> destino,
            Lote lote,
            TipoMovimentacao tipo,
            int quantidade) {

        if (lote.getPublicId() == null) {
            return;
        }

        LoteAcumulado acumulado = destino.computeIfAbsent(
                lote.getPublicId(),
                chave -> new LoteAcumulado(lote)
        );
        acumulado.adicionar(tipo, quantidade);
    }

    private int validarLimite(Integer limite) {
        int valor = limite == null ? LIMITE_PADRAO : limite;
        if (valor <= 0 || valor > LIMITE_MAXIMO) {
            throw new BusinessRuleException("O limite do ranking deve estar entre 1 e 50.");
        }
        return valor;
    }

    private void validarPeriodoOpcional(LocalDate dataInicio, LocalDate dataFim) {
        if ((dataInicio == null) != (dataFim == null)) {
            throw new BusinessRuleException("Para filtrar por período, informe dataInicio e dataFim.");
        }
        if (dataInicio != null && dataInicio.isAfter(dataFim)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
        }
    }

    private static class ProdutoAcumulado {
        private final UUID id;
        private final String nome;
        private int quantidade;
        private int movimentacoes;

        ProdutoAcumulado(UUID id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        void adicionar(int valor) {
            quantidade += valor;
            movimentacoes++;
        }

        int getQuantidade() {
            return quantidade;
        }

        String getNome() {
            return nome == null ? "" : nome;
        }

        RelatorioResumoOperacionalResponseDTO.ProdutoRanking toDto() {
            return new RelatorioResumoOperacionalResponseDTO.ProdutoRanking(
                    id,
                    nome,
                    quantidade,
                    movimentacoes
            );
        }
    }

    private static class LoteAcumulado {
        private final Lote lote;
        private int quantidadeMovimentada;
        private int movimentacoes;
        private int entradas;
        private int saidas;

        LoteAcumulado(Lote lote) {
            this.lote = lote;
        }

        void adicionar(TipoMovimentacao tipo, int quantidade) {
            quantidadeMovimentada += quantidade;
            movimentacoes++;
            if (tipo == TipoMovimentacao.ENTRADA) {
                entradas += quantidade;
            } else if (tipo == TipoMovimentacao.SAIDA) {
                saidas += quantidade;
            }
        }

        int getQuantidadeMovimentada() {
            return quantidadeMovimentada;
        }

        String getCodigoInterno() {
            return lote.getCodigoInterno() == null ? "" : lote.getCodigoInterno();
        }

        RelatorioResumoOperacionalResponseDTO.LoteRanking toDto() {
            Produto produto = lote.getEstoqueCentral().getProduto();
            return new RelatorioResumoOperacionalResponseDTO.LoteRanking(
                    lote.getPublicId(),
                    lote.getCodigoInterno(),
                    lote.getNumeroLote(),
                    produto != null ? produto.getPublicId() : null,
                    produto != null ? produto.getNome() : null,
                    quantidadeMovimentada,
                    movimentacoes,
                    entradas,
                    saidas,
                    lote.getQuantidadeDisponivel(),
                    lote.getDataValidade()
            );
        }
    }
}
