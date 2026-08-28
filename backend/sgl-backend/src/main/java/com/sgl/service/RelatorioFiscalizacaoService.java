package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO.MovimentacaoFiscalizadaItem;
import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO.ProdutoFiscalizadoItem;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioFiscalizacaoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public RelatorioFiscalizacaoResponseDTO gerar(
            UUID produtoId,
            OrgaoFiscalizador orgaoFiscalizador,
            UUID unidadeId,
            LocalDate dataInicio,
            LocalDate dataFim,
            Integer diasVencimento) {

        validarPeriodo(dataInicio, dataFim);
        int janelaVencimento = diasVencimento == null ? 30 : diasVencimento;
        if (janelaVencimento < 1 || janelaVencimento > 365) {
            throw new BusinessRuleException("A janela de vencimento deve estar entre 1 e 365 dias.");
        }

        Long unidadeInternaId = null;
        if (unidadeId != null) {
            Unidade unidade = unidadeRepository.findByPublicId(unidadeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada."));
            unidadeInternaId = unidade.getId();
        }

        final Long filtroUnidadeId = unidadeInternaId;
        LocalDate hoje = LocalDate.now();
        LocalDate limiteVencimento = hoje.plusDays(janelaVencimento);
        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        List<Produto> produtosFiscalizados = produtoRepository.findAll().stream()
                .filter(produto -> Boolean.TRUE.equals(produto.getFiscalizado()))
                .filter(produto -> produtoId == null || produto.getPublicId().equals(produtoId))
                .filter(produto -> orgaoFiscalizador == null || produto.getOrgaosFiscalizadores().contains(orgaoFiscalizador))
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (produtoId != null && produtosFiscalizados.isEmpty()) {
            Produto produto = produtoRepository.findByPublicId(produtoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
            if (!Boolean.TRUE.equals(produto.getFiscalizado())) {
                throw new BusinessRuleException("O produto informado não está classificado como fiscalizado.");
            }
        }

        Set<Long> produtosInternos = produtosFiscalizados.stream().map(Produto::getId).collect(java.util.stream.Collectors.toSet());

        List<EstoqueCentral> estoques = estoqueCentralRepository.findAll().stream()
                .filter(estoque -> produtosInternos.contains(estoque.getProduto().getId()))
                .filter(estoque -> filtroUnidadeId == null || estoque.getUnidade().getId().equals(filtroUnidadeId))
                .toList();

        Set<Long> estoquesInternos = estoques.stream().map(EstoqueCentral::getId).collect(java.util.stream.Collectors.toSet());

        List<Lote> lotes = loteRepository.findAll().stream()
                .filter(lote -> estoquesInternos.contains(lote.getEstoqueCentral().getId()))
                .toList();

        List<MovimentacaoEstoque> movimentacoes = movimentacaoRepository.findAll().stream()
                .filter(mov -> produtosInternos.contains(mov.getProduto().getId()))
                .filter(mov -> filtroUnidadeId == null || mov.getEstoqueCentral().getUnidade().getId().equals(filtroUnidadeId))
                .filter(mov -> inicio == null || !mov.getDataMovimentacao().isBefore(inicio))
                .filter(mov -> fim == null || !mov.getDataMovimentacao().isAfter(fim))
                .sorted(Comparator.comparing(MovimentacaoEstoque::getDataMovimentacao).reversed())
                .toList();

        List<ProdutoFiscalizadoItem> produtos = produtosFiscalizados.stream()
                .map(produto -> montarProduto(produto, estoques, lotes, movimentacoes, hoje, limiteVencimento))
                .toList();

        List<MovimentacaoFiscalizadaItem> trilha = movimentacoes.stream()
                .map(this::montarMovimentacao)
                .toList();

        int lotesAtivos = (int) lotes.stream().filter(lote -> Boolean.TRUE.equals(lote.getAtivo()) && lote.getQuantidadeDisponivel() > 0).count();
        int lotesVencidos = (int) lotes.stream().filter(lote -> loteVencido(lote, hoje)).count();
        int lotesProximos = (int) lotes.stream().filter(lote -> loteProximoVencimento(lote, hoje, limiteVencimento)).count();

        return RelatorioFiscalizacaoResponseDTO.builder()
                .geradoEm(LocalDateTime.now())
                .totalProdutosFiscalizados(produtosFiscalizados.size())
                .saldoAtualTotal(estoques.stream().mapToInt(EstoqueCentral::getQuantidadeAtual).sum())
                .lotesAtivos(lotesAtivos)
                .lotesVencidos(lotesVencidos)
                .lotesProximosVencimento(lotesProximos)
                .quantidadeEntradas(somarTipo(movimentacoes, TipoMovimentacao.ENTRADA))
                .quantidadeSaidas(somarTipo(movimentacoes, TipoMovimentacao.SAIDA))
                .produtos(produtos)
                .movimentacoes(trilha)
                .build();
    }

    private ProdutoFiscalizadoItem montarProduto(
            Produto produto,
            List<EstoqueCentral> estoques,
            List<Lote> lotes,
            List<MovimentacaoEstoque> movimentacoes,
            LocalDate hoje,
            LocalDate limiteVencimento) {

        List<EstoqueCentral> estoquesProduto = estoques.stream()
                .filter(estoque -> estoque.getProduto().getId().equals(produto.getId()))
                .toList();
        Set<Long> estoquesProdutoIds = estoquesProduto.stream().map(EstoqueCentral::getId).collect(java.util.stream.Collectors.toSet());
        List<Lote> lotesProduto = lotes.stream()
                .filter(lote -> estoquesProdutoIds.contains(lote.getEstoqueCentral().getId()))
                .toList();
        List<MovimentacaoEstoque> movimentosProduto = movimentacoes.stream()
                .filter(mov -> mov.getProduto().getId().equals(produto.getId()))
                .toList();

        LocalDate proximoVencimento = lotesProduto.stream()
                .filter(lote -> Boolean.TRUE.equals(lote.getAtivo()))
                .filter(lote -> lote.getQuantidadeDisponivel() > 0)
                .map(Lote::getDataValidade)
                .filter(data -> data != null && !data.isBefore(hoje))
                .min(LocalDate::compareTo)
                .orElse(null);

        return ProdutoFiscalizadoItem.builder()
                .produtoId(produto.getPublicId())
                .produtoNome(produto.getNome())
                .codigoReferencia(produto.getCodigoReferencia())
                .orgaosFiscalizadores(Set.copyOf(produto.getOrgaosFiscalizadores()))
                .observacaoFiscalizacao(produto.getObservacaoFiscalizacao())
                .saldoAtual(estoquesProduto.stream().mapToInt(EstoqueCentral::getQuantidadeAtual).sum())
                .lotesAtivos((int) lotesProduto.stream().filter(lote -> Boolean.TRUE.equals(lote.getAtivo()) && lote.getQuantidadeDisponivel() > 0).count())
                .lotesVencidos((int) lotesProduto.stream().filter(lote -> loteVencido(lote, hoje)).count())
                .lotesProximosVencimento((int) lotesProduto.stream().filter(lote -> loteProximoVencimento(lote, hoje, limiteVencimento)).count())
                .proximoVencimento(proximoVencimento)
                .quantidadeEntradas(somarTipo(movimentosProduto, TipoMovimentacao.ENTRADA))
                .quantidadeSaidas(somarTipo(movimentosProduto, TipoMovimentacao.SAIDA))
                .build();
    }

    private MovimentacaoFiscalizadaItem montarMovimentacao(MovimentacaoEstoque mov) {
        var pedido = mov.getPedido();
        var lote = mov.getLote();

        return MovimentacaoFiscalizadaItem.builder()
                .movimentacaoId(mov.getPublicId())
                .dataMovimentacao(mov.getDataMovimentacao())
                .produtoId(mov.getProduto().getPublicId())
                .produtoNome(mov.getProduto().getNome())
                .tipoMovimentacao(mov.getTipoMovimentacao())
                .quantidadeMovimentada(mov.getQuantidadeMovimentada())
                .loteId(lote != null ? lote.getPublicId() : null)
                .codigoInternoLote(lote != null ? lote.getCodigoInterno() : null)
                .numeroLote(lote != null ? lote.getNumeroLote() : null)
                .dataValidadeLote(lote != null ? lote.getDataValidade() : null)
                .laboratorioId(mov.getLaboratorio() != null ? mov.getLaboratorio().getPublicId() : null)
                .laboratorioNome(mov.getLaboratorio() != null ? mov.getLaboratorio().getNome() : null)
                .projetoId(pedido != null && pedido.getProjeto() != null ? pedido.getProjeto().getPublicId() : null)
                .projetoNome(pedido != null && pedido.getProjeto() != null ? pedido.getProjeto().getNome() : null)
                .solicitanteId(pedido != null ? pedido.getUsuario().getPublicId() : null)
                .solicitanteNome(pedido != null ? pedido.getUsuario().getNome() : null)
                .pedidoId(pedido != null ? pedido.getPublicId() : null)
                .responsavelId(mov.getUsuario().getPublicId())
                .responsavelNome(mov.getUsuario().getNome())
                .saldoAposMovimentacao(mov.getQuantidadeAtual())
                .build();
    }

    private int somarTipo(List<MovimentacaoEstoque> movimentacoes, TipoMovimentacao tipo) {
        return movimentacoes.stream()
                .filter(mov -> mov.getTipoMovimentacao() == tipo)
                .mapToInt(MovimentacaoEstoque::getQuantidadeMovimentada)
                .sum();
    }

    private boolean loteVencido(Lote lote, LocalDate hoje) {
        return Boolean.TRUE.equals(lote.getAtivo())
                && lote.getQuantidadeDisponivel() > 0
                && lote.getDataValidade() != null
                && lote.getDataValidade().isBefore(hoje);
    }

    private boolean loteProximoVencimento(Lote lote, LocalDate hoje, LocalDate limite) {
        return Boolean.TRUE.equals(lote.getAtivo())
                && lote.getQuantidadeDisponivel() > 0
                && lote.getDataValidade() != null
                && !lote.getDataValidade().isBefore(hoje)
                && !lote.getDataValidade().isAfter(limite);
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if ((dataInicio == null) != (dataFim == null)) {
            throw new BusinessRuleException("Para filtrar por período, informe dataInicio e dataFim.");
        }
        if (dataInicio != null && dataInicio.isAfter(dataFim)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
        }
    }
}
