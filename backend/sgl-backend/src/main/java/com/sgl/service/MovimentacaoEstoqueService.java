package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EntradaLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

/**
 * Centraliza as operações físicas que alteram o estoque.
 *
 * MovimentacaoEstoque permanece como trilha de auditoria. Quando uma operação
 * utiliza mais de um lote, é criada uma movimentação para cada lote afetado.
 */
@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarTodos() {
        return movimentacaoRepository.findAll().stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovimentacaoEstoqueDTO buscarPorId(Long id) {
        return movimentacaoRepository.findById(id)
                .map(MovimentacaoEstoqueDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação", id));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorProduto(Long produtoId) {
        return movimentacaoRepository.findByProdutoId(produtoId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorLaboratorio(Long laboratorioId) {
        return movimentacaoRepository.findByLaboratorioId(laboratorioId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorUsuario(Long usuarioId) {
        return movimentacaoRepository.findByUsuarioId(usuarioId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorPedido(Long pedidoId) {
        return movimentacaoRepository.findByPedidoId(pedidoId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorTipo(TipoMovimentacao tipo) {
        return movimentacaoRepository.findByTipoMovimentacao(tipo).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional
    public LoteDTO registrarEntradaLote(
            Long estoqueId,
            EntradaLoteDTO dto,
            Usuario usuario) {

        validarUsuarioResponsavel(usuario);

        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
        validarEntradaLote(estoque.getProduto(), dto);

        if (loteRepository.existsByEstoqueCentralIdAndNumeroLote(
                estoqueId,
                dto.getNumeroLote())) {
            throw new BusinessRuleException(
                    "Já existe lote com esse número neste estoque."
            );
        }

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual = quantidadeAnterior + dto.getQuantidade();

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.setNumeroLote(dto.getNumeroLote());
        lote.setQuantidadeInicial(dto.getQuantidade());
        lote.setQuantidadeDisponivel(dto.getQuantidade());
        lote.setDataEntrada(LocalDate.now());
        lote.setDataValidade(dto.getDataValidade());
        lote.setAtivo(true);
        loteRepository.save(lote);

        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        registrarMovimentacao(
                estoque,
                lote,
                usuario,
                null,
                null,
                TipoMovimentacao.ENTRADA,
                dto.getOrigem(),
                dto.getQuantidade(),
                quantidadeAnterior,
                quantidadeAtual,
                dto.getObservacao()
        );

        return new LoteDTO(lote);
    }

    /**
     * Retira uma quantidade do estoque consumindo lotes por FEFO para produtos
     * perecíveis e FIFO para produtos não perecíveis.
     */
    @Transactional
    public List<MovimentacaoEstoqueDTO> registrarSaida(
            Long estoqueId,
            Integer quantidade,
            Usuario usuario,
            OrigemMovimentacao origem,
            Pedido pedido,
            Laboratorio laboratorio,
            String observacao) {

        validarQuantidade(quantidade);
        validarUsuarioResponsavel(usuario);

        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
        List<Lote> lotes = buscarLotesParaSaida(estoque);

        int saldoUtilizavel = lotes.stream()
                .mapToInt(Lote::getQuantidadeDisponivel)
                .sum();

        if (saldoUtilizavel < quantidade) {
            throw new BusinessRuleException(
                    "Estoque utilizável insuficiente. Disponível nos lotes válidos: "
                            + saldoUtilizavel + ", solicitado: " + quantidade
            );
        }

        List<MovimentacaoEstoqueDTO> movimentacoes = new ArrayList<>();
        int restante = quantidade;

        for (Lote lote : lotes) {
            if (restante == 0) {
                break;
            }

            int consumido = Math.min(restante, lote.getQuantidadeDisponivel());
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior - consumido;

            lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel() - consumido);
            estoque.setQuantidadeAtual(saldoAtual);

            loteRepository.save(lote);
            estoqueCentralRepository.save(estoque);

            MovimentacaoEstoque movimentacao = registrarMovimentacao(
                    estoque,
                    lote,
                    usuario,
                    pedido,
                    laboratorio,
                    TipoMovimentacao.SAIDA,
                    origem,
                    consumido,
                    saldoAnterior,
                    saldoAtual,
                    observacao
            );

            movimentacoes.add(new MovimentacaoEstoqueDTO(movimentacao));
            restante -= consumido;
        }

        return movimentacoes;
    }

    /**
     * Descarta somente lotes efetivamente vencidos. Se a quantidade ultrapassar
     * um lote, o descarte continua nos próximos lotes vencidos.
     */
    @Transactional
    public List<MovimentacaoEstoqueDTO> registrarDescarteVencimento(
            Long estoqueId,
            Integer quantidade,
            String justificativa,
            Usuario usuario) {

        validarQuantidade(quantidade);
        validarUsuarioResponsavel(usuario);

        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);

        if (!Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
            throw new BusinessRuleException(
                    "Somente produtos perecíveis podem ser descartados por vencimento."
            );
        }

        List<Lote> lotesVencidos = loteRepository.buscarVencidosComBloqueio(
                estoqueId,
                LocalDate.now()
        );

        int saldoVencido = lotesVencidos.stream()
                .mapToInt(Lote::getQuantidadeDisponivel)
                .sum();

        if (saldoVencido < quantidade) {
            throw new BusinessRuleException(
                    "Quantidade de descarte maior que o saldo vencido disponível. "
                            + "Disponível: " + saldoVencido
            );
        }

        List<MovimentacaoEstoqueDTO> movimentacoes = new ArrayList<>();
        int restante = quantidade;

        for (Lote lote : lotesVencidos) {
            if (restante == 0) {
                break;
            }

            int descartado = Math.min(restante, lote.getQuantidadeDisponivel());
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior - descartado;

            lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel() - descartado);
            estoque.setQuantidadeAtual(saldoAtual);

            loteRepository.save(lote);
            estoqueCentralRepository.save(estoque);

            MovimentacaoEstoque movimentacao = registrarMovimentacao(
                    estoque,
                    lote,
                    usuario,
                    null,
                    null,
                    TipoMovimentacao.DESCARTE_VENCIMENTO,
                    OrigemMovimentacao.DESCARTE,
                    descartado,
                    saldoAnterior,
                    saldoAtual,
                    justificativa
            );

            movimentacoes.add(new MovimentacaoEstoqueDTO(movimentacao));
            restante -= descartado;
        }

        return movimentacoes;
    }

    /**
     * Restaura exatamente os lotes consumidos pelas saídas de um pedido.
     *
     * O usuário responsável pode ficar ausente nesta etapa apenas enquanto o
     * contexto de autenticação local ainda não estiver integrado. Nesse caso a
     * restauração física é feita, mas a movimentação DEVOLUCAO não é criada.
     */
    @Transactional
    public void devolverSaidasDoPedido(
            Pedido pedido,
            Usuario usuarioResponsavel,
            String observacao) {

        List<MovimentacaoEstoque> saidas = movimentacaoRepository
                .findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
                        pedido.getId(),
                        TipoMovimentacao.SAIDA
                )
                .stream()
                .filter(m -> m.getLote() != null)
                .sorted(Comparator
                        .comparing((MovimentacaoEstoque m) -> m.getEstoqueCentral().getId())
                        .thenComparing(m -> m.getLote().getId()))
                .toList();

        if (saidas.isEmpty()) {
            throw new BusinessRuleException(
                    "Não foram encontradas saídas por lote para devolver este pedido."
            );
        }

        for (MovimentacaoEstoque saida : saidas) {
            EstoqueCentral estoque = estoqueCentralRepository
                    .buscarPorIdComBloqueio(saida.getEstoqueCentral().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estoque central",
                            saida.getEstoqueCentral().getId()
                    ));

            Lote lote = loteRepository.buscarPorIdComBloqueio(saida.getLote().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lote",
                            saida.getLote().getId()
                    ));

            int quantidade = saida.getQuantidadeMovimentada();
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior + quantidade;

            if (lote.getQuantidadeDisponivel() + quantidade > lote.getQuantidadeInicial()) {
                throw new BusinessRuleException(
                        "A devolução ultrapassaria a quantidade inicial do lote "
                                + lote.getNumeroLote() + "."
                );
            }

            lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel() + quantidade);
            lote.setAtivo(true);
            estoque.setQuantidadeAtual(saldoAtual);

            loteRepository.save(lote);
            estoqueCentralRepository.save(estoque);

            if (usuarioResponsavel != null) {
                validarUsuarioResponsavel(usuarioResponsavel);
                registrarMovimentacao(
                        estoque,
                        lote,
                        usuarioResponsavel,
                        pedido,
                        pedido.getLaboratorio(),
                        TipoMovimentacao.DEVOLUCAO,
                        OrigemMovimentacao.DEVOLUCAO,
                        quantidade,
                        saldoAnterior,
                        saldoAtual,
                        observacao
                );
            }
        }
    }

    /** Apenas para consulta/diagnóstico da política de seleção. */
    @Transactional
    public List<LoteDTO> listarLotesOrdenadosParaSaida(Long estoqueId) {
        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
        return buscarLotesParaSaida(estoque).stream()
                .map(LoteDTO::new)
                .toList();
    }

    private List<Lote> buscarLotesParaSaida(EstoqueCentral estoque) {
        if (Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
            return loteRepository.buscarDisponiveisPorFefoComBloqueio(
                    estoque.getId(),
                    LocalDate.now()
            );
        }

        return loteRepository.buscarDisponiveisPorEntradaComBloqueio(
                estoque.getId()
        );
    }

    private EstoqueCentral buscarEstoqueAtivoComBloqueio(Long estoqueId) {
        EstoqueCentral estoque = estoqueCentralRepository
                .buscarPorIdComBloqueio(estoqueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Estoque central", estoqueId));

        if (!Boolean.TRUE.equals(estoque.getAtivo())) {
            throw new BusinessRuleException("O estoque informado está inativo.");
        }

        return estoque;
    }

    private MovimentacaoEstoque registrarMovimentacao(
            EstoqueCentral estoque,
            Lote lote,
            Usuario usuario,
            Pedido pedido,
            Laboratorio laboratorio,
            TipoMovimentacao tipo,
            OrigemMovimentacao origem,
            Integer quantidade,
            Integer quantidadeAnterior,
            Integer quantidadeAtual,
            String observacao) {

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(estoque.getProduto())
                .estoqueCentral(estoque)
                .lote(lote)
                .usuario(usuario)
                .pedido(pedido)
                .laboratorio(laboratorio)
                .tipoMovimentacao(tipo)
                .origem(origem)
                .quantidadeMovimentada(quantidade)
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeAtual)
                .dataMovimentacao(LocalDateTime.now())
                .observacao(observacao)
                .build();

        return movimentacaoRepository.save(movimentacao);
    }

    private void validarEntradaLote(Produto produto, EntradaLoteDTO dto) {
        validarQuantidade(dto.getQuantidade());

        if (dto.getOrigem() == null) {
            throw new BusinessRuleException("Origem da entrada é obrigatória.");
        }

        if (Boolean.TRUE.equals(produto.getPerecivel())) {
            if (dto.getDataValidade() == null) {
                throw new BusinessRuleException(
                        "Data de validade é obrigatória para produto perecível."
                );
            }

            if (dto.getDataValidade().isBefore(LocalDate.now())) {
                throw new BusinessRuleException(
                        "Não é possível registrar entrada de lote já vencido."
                );
            }
        } else if (dto.getDataValidade() != null) {
            throw new BusinessRuleException(
                    "Produto não perecível não deve possuir data de validade no lote."
            );
        }
    }

    private void validarUsuarioResponsavel(Usuario usuario) {
        if (usuario == null) {
            throw new BusinessRuleException("Usuário responsável é obrigatório.");
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException(
                    "O usuário responsável pela movimentação está inativo."
            );
        }
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new BusinessRuleException("A quantidade deve ser maior que zero.");
        }
    }
}
