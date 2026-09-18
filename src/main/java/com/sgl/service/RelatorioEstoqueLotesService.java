package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioEstoqueLotesService {

    private static final int DIAS_VENCIMENTO_PADRAO = 30;

    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public RelatorioEstoqueLotesResponseDTO gerar(
            UUID unidadeId,
            UUID produtoId,
            Boolean ativoEstoque,
            Boolean abaixoMinimo,
            Boolean ativoLote,
            String validade,
            Integer diasVencimento) {

        int dias = diasVencimento == null ? DIAS_VENCIMENTO_PADRAO : diasVencimento;
        if (dias < 1 || dias > 365) {
            throw new BusinessRuleException("O período de vencimento deve estar entre 1 e 365 dias.");
        }

        String validadeNormalizada = normalizarValidade(validade);
        LocalDate hoje = LocalDate.now();
        LocalDate limiteVencimento = hoje.plusDays(dias);

        List<EstoqueCentral> estoques = estoqueCentralRepository.findAll().stream()
                .filter(estoque -> unidadeId == null
                        || estoque.getUnidade().getPublicId().equals(unidadeId))
                .filter(estoque -> produtoId == null
                        || estoque.getProduto().getPublicId().equals(produtoId))
                .filter(estoque -> ativoEstoque == null
                        || Boolean.TRUE.equals(estoque.getAtivo()) == ativoEstoque)
                .filter(estoque -> abaixoMinimo == null
                        || estaAbaixoMinimo(estoque) == abaixoMinimo)
                .toList();

        Set<Long> estoqueIds = estoques.stream()
                .map(EstoqueCentral::getId)
                .collect(Collectors.toSet());

        List<Lote> lotes = loteRepository.findAll().stream()
                .filter(lote -> estoqueIds.contains(lote.getEstoqueCentral().getId()))
                .filter(lote -> ativoLote == null
                        || Boolean.TRUE.equals(lote.getAtivo()) == ativoLote)
                .filter(lote -> validadeNormalizada == null
                        || classificarLote(lote, hoje, limiteVencimento).equals(validadeNormalizada))
                .toList();

        List<RelatorioEstoqueLotesResponseDTO.EstoqueItem> itensEstoque = estoques.stream()
                .map(estoque -> mapearEstoque(estoque, lotes, hoje, limiteVencimento))
                .toList();

        List<RelatorioEstoqueLotesResponseDTO.LoteItem> itensLote = lotes.stream()
                .sorted((a, b) -> {
                    LocalDate validadeA = a.getDataValidade();
                    LocalDate validadeB = b.getDataValidade();
                    if (validadeA == null && validadeB == null) {
                        return a.getCodigoInterno().compareToIgnoreCase(b.getCodigoInterno());
                    }
                    if (validadeA == null) return 1;
                    if (validadeB == null) return -1;
                    return validadeA.compareTo(validadeB);
                })
                .map(lote -> mapearLote(lote, hoje, limiteVencimento))
                .toList();

        return RelatorioEstoqueLotesResponseDTO.builder()
                .geradoEm(LocalDateTime.now())
                .totalEstoques(estoques.size())
                .estoquesAtivos((int) estoques.stream().filter(e -> Boolean.TRUE.equals(e.getAtivo())).count())
                .estoquesAbaixoMinimo((int) estoques.stream().filter(this::estaAbaixoMinimo).count())
                .quantidadeTotalEstoque(estoques.stream()
                        .mapToLong(e -> e.getQuantidadeAtual() == null ? 0L : e.getQuantidadeAtual())
                        .sum())
                .totalLotes(lotes.size())
                .lotesAtivos((int) lotes.stream().filter(this::loteDisponivelAtivo).count())
                .lotesVencidos((int) lotes.stream()
                        .filter(lote -> "VENCIDO".equals(classificarLote(lote, hoje, limiteVencimento)))
                        .count())
                .lotesProximosVencimento((int) lotes.stream()
                        .filter(lote -> "PROXIMO_VENCIMENTO".equals(classificarLote(lote, hoje, limiteVencimento)))
                        .count())
                .lotesEsgotados((int) lotes.stream()
                        .filter(lote -> "ESGOTADO".equals(classificarLote(lote, hoje, limiteVencimento)))
                        .count())
                .estoques(itensEstoque)
                .lotes(itensLote)
                .build();
    }

    private RelatorioEstoqueLotesResponseDTO.EstoqueItem mapearEstoque(
            EstoqueCentral estoque,
            List<Lote> lotesFiltrados,
            LocalDate hoje,
            LocalDate limiteVencimento) {

        List<Lote> lotesDoEstoque = lotesFiltrados.stream()
                .filter(lote -> lote.getEstoqueCentral().getId().equals(estoque.getId()))
                .toList();

        return RelatorioEstoqueLotesResponseDTO.EstoqueItem.builder()
                .estoqueId(estoque.getPublicId())
                .unidadeId(estoque.getUnidade().getPublicId())
                .unidadeNome(estoque.getUnidade().getNome())
                .unidadeSigla(estoque.getUnidade().getSigla())
                .produtoId(estoque.getProduto().getPublicId())
                .produtoNome(estoque.getProduto().getNome())
                .codigoReferencia(estoque.getProduto().getCodigoReferencia())
                .unidadeMedida(estoque.getProduto().getUnidadeMedida().name())
                .quantidadeAtual(estoque.getQuantidadeAtual())
                .quantidadeMinima(estoque.getQuantidadeMinima())
                .abaixoMinimo(estaAbaixoMinimo(estoque))
                .ativo(Boolean.TRUE.equals(estoque.getAtivo()))
                .totalLotes(lotesDoEstoque.size())
                .lotesAtivos((int) lotesDoEstoque.stream().filter(this::loteDisponivelAtivo).count())
                .lotesVencidos((int) lotesDoEstoque.stream()
                        .filter(lote -> "VENCIDO".equals(classificarLote(lote, hoje, limiteVencimento)))
                        .count())
                .lotesProximosVencimento((int) lotesDoEstoque.stream()
                        .filter(lote -> "PROXIMO_VENCIMENTO".equals(classificarLote(lote, hoje, limiteVencimento)))
                        .count())
                .build();
    }

    private RelatorioEstoqueLotesResponseDTO.LoteItem mapearLote(
            Lote lote,
            LocalDate hoje,
            LocalDate limiteVencimento) {

        EstoqueCentral estoque = lote.getEstoqueCentral();
        return RelatorioEstoqueLotesResponseDTO.LoteItem.builder()
                .loteId(lote.getPublicId())
                .estoqueId(estoque.getPublicId())
                .unidadeId(estoque.getUnidade().getPublicId())
                .unidadeNome(estoque.getUnidade().getNome())
                .produtoId(estoque.getProduto().getPublicId())
                .produtoNome(estoque.getProduto().getNome())
                .codigoInterno(lote.getCodigoInterno())
                .numeroLote(lote.getNumeroLote())
                .quantidadeInicial(lote.getQuantidadeInicial())
                .quantidadeDisponivel(lote.getQuantidadeDisponivel())
                .dataEntrada(lote.getDataEntrada())
                .dataValidade(lote.getDataValidade())
                .ativo(Boolean.TRUE.equals(lote.getAtivo()))
                .situacao(classificarLote(lote, hoje, limiteVencimento))
                .build();
    }

    private boolean estaAbaixoMinimo(EstoqueCentral estoque) {
        int atual = estoque.getQuantidadeAtual() == null ? 0 : estoque.getQuantidadeAtual();
        int minimo = estoque.getQuantidadeMinima() == null ? 0 : estoque.getQuantidadeMinima();
        return atual <= minimo;
    }

    private boolean loteDisponivelAtivo(Lote lote) {
        return Boolean.TRUE.equals(lote.getAtivo())
                && lote.getQuantidadeDisponivel() != null
                && lote.getQuantidadeDisponivel() > 0;
    }

    private String classificarLote(Lote lote, LocalDate hoje, LocalDate limiteVencimento) {
        if (!Boolean.TRUE.equals(lote.getAtivo())) {
            return "INATIVO";
        }
        if (lote.getQuantidadeDisponivel() == null || lote.getQuantidadeDisponivel() <= 0) {
            return "ESGOTADO";
        }
        if (lote.getDataValidade() == null) {
            return "SEM_VALIDADE";
        }
        if (lote.getDataValidade().isBefore(hoje)) {
            return "VENCIDO";
        }
        if (!lote.getDataValidade().isAfter(limiteVencimento)) {
            return "PROXIMO_VENCIMENTO";
        }
        return "VALIDO";
    }

    private String normalizarValidade(String validade) {
        if (validade == null || validade.isBlank()) {
            return null;
        }

        String valor = validade.trim().toUpperCase(Locale.ROOT);
        Set<String> permitidos = Set.of(
                "VALIDO",
                "PROXIMO_VENCIMENTO",
                "VENCIDO",
                "SEM_VALIDADE",
                "ESGOTADO",
                "INATIVO"
        );

        if (!permitidos.contains(valor)) {
            throw new BusinessRuleException("Situação de lote inválida para o relatório.");
        }
        return valor;
    }
}
