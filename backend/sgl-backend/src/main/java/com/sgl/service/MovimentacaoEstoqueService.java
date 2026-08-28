package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
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
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;
    private final ProdutoRepository produtoRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarTodos() {
        return movimentacaoRepository.findAll().stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovimentacaoEstoqueResponseDTO buscarPorId(UUID id) {
        return movimentacaoRepository.findByPublicId(id)
                .map(MovimentacaoEstoqueResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação", id));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorProduto(UUID produtoId) {
        Produto produto = produtoRepository.findByPublicId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        return movimentacaoRepository.findByProdutoId(produto.getId()).stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        return movimentacaoRepository.findByLaboratorioId(laboratorio.getId()).stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));

        return movimentacaoRepository.findByUsuarioId(usuario.getId()).stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorPedido(UUID pedidoId) {
        Pedido pedido = pedidoRepository.findByPublicId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));

        return movimentacaoRepository.findByPedidoId(pedido.getId()).stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarPorTipo(TipoMovimentacao tipo) {
        return movimentacaoRepository.findByTipoMovimentacao(tipo).stream()
                .map(MovimentacaoEstoqueResponseDTO::new)
                .toList();
    }

    @Transactional
    public LoteResponseDTO registrarEntradaLote(
            UUID estoqueId,
            EntradaLoteRequestDTO dto,
            Usuario usuario) {

        validarUsuarioResponsavel(usuario);

        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
        validarEntradaLote(estoque.getProduto(), dto);

        if (loteRepository.existsByEstoqueCentralIdAndNumeroLote(
                estoque.getId(),
                dto.getNumeroLote())) {
            throw new BusinessRuleException(
                    "Já existe lote com esse número de fornecedor neste estoque."
            );
        }

        Produto produtoBloqueado = produtoRepository.buscarPorIdComBloqueio(estoque.getProduto().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", estoque.getProduto().getId()));

        int multiplicador = dto.getConteudoPorApresentacao() == null
                ? 1
                : dto.getConteudoPorApresentacao();

        int quantidadeUnitaria;
        try {
            quantidadeUnitaria = Math.multiplyExact(dto.getQuantidade(), multiplicador);
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("Quantidade total do lote excede o limite suportado.");
        }

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual = quantidadeAnterior + quantidadeUnitaria;

        CodigoLoteGerado codigoGerado = gerarCodigoInternoLote(produtoBloqueado);

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno(codigoGerado.codigo(), codigoGerado.sequencial());
        lote.setNumeroLote(dto.getNumeroLote().trim());
        lote.setTipoEmbalagem(dto.getTipoEmbalagem());
        lote.setApresentacao(normalizarApresentacao(produtoBloqueado, dto.getApresentacao()));
        lote.setQuantidadeApresentacoes(dto.getQuantidade());
        lote.setConteudoPorApresentacao(multiplicador);
        lote.setFracionavel(dto.getFracionavel() == null ? true : dto.getFracionavel());
        lote.setObservacao(
                dto.getObservacao() == null || dto.getObservacao().isBlank()
                        ? null
                        : dto.getObservacao().trim()
        );
        lote.setQuantidadeInicial(quantidadeUnitaria);
        lote.setQuantidadeDisponivel(quantidadeUnitaria);
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
                quantidadeUnitaria,
                quantidadeAnterior,
                quantidadeAtual,
                dto.getObservacao()
        );

        return new LoteResponseDTO(lote);
    }

    @Transactional
    public List<MovimentacaoEstoqueResponseDTO> registrarSaida(
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

        List<MovimentacaoEstoqueResponseDTO> movimentacoes = new ArrayList<>();
        int restante = quantidade;

        for (Lote lote : lotes) {
            if (restante == 0) break;

            int consumido = Math.min(restante, lote.getQuantidadeDisponivel());
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior - consumido;

            lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel() - consumido);
            estoque.setQuantidadeAtual(saldoAtual);

            loteRepository.save(lote);
            estoqueCentralRepository.save(estoque);

            MovimentacaoEstoque movimentacao = registrarMovimentacao(
                    estoque, lote, usuario, pedido, laboratorio,
                    TipoMovimentacao.SAIDA, origem, consumido,
                    saldoAnterior, saldoAtual, observacao
            );

            movimentacoes.add(new MovimentacaoEstoqueResponseDTO(movimentacao));
            restante -= consumido;
        }

        return movimentacoes;
    }

    @Transactional
    public List<MovimentacaoEstoqueResponseDTO> registrarDescarteVencimento(
            UUID estoqueId,
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
                estoque.getId(), LocalDate.now()
        );

        int saldoVencido = lotesVencidos.stream()
                .mapToInt(Lote::getQuantidadeDisponivel)
                .sum();

        if (saldoVencido < quantidade) {
            throw new BusinessRuleException(
                    "Quantidade de descarte maior que o saldo vencido disponível. Disponível: " + saldoVencido
            );
        }

        List<MovimentacaoEstoqueResponseDTO> movimentacoes = new ArrayList<>();
        int restante = quantidade;

        for (Lote lote : lotesVencidos) {
            if (restante == 0) break;

            int descartado = Math.min(restante, lote.getQuantidadeDisponivel());
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior - descartado;

            lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel() - descartado);
            estoque.setQuantidadeAtual(saldoAtual);

            loteRepository.save(lote);
            estoqueCentralRepository.save(estoque);

            MovimentacaoEstoque movimentacao = registrarMovimentacao(
                    estoque, lote, usuario, null, null,
                    TipoMovimentacao.DESCARTE_VENCIMENTO, OrigemMovimentacao.DESCARTE,
                    descartado, saldoAnterior, saldoAtual, justificativa
            );

            movimentacoes.add(new MovimentacaoEstoqueResponseDTO(movimentacao));
            restante -= descartado;
        }

        return movimentacoes;
    }

    @Transactional
    public void devolverSaidasDoPedido(
            Pedido pedido,
            Usuario usuarioResponsavel,
            String observacao) {

        List<MovimentacaoEstoque> saidas = movimentacaoRepository
                .findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
                        pedido.getId(), TipoMovimentacao.SAIDA
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
                            "Estoque central", saida.getEstoqueCentral().getId()
                    ));

            Lote lote = loteRepository.buscarPorIdComBloqueio(saida.getLote().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lote", saida.getLote().getId()
                    ));

            int quantidade = saida.getQuantidadeMovimentada();
            int saldoAnterior = estoque.getQuantidadeAtual();
            int saldoAtual = saldoAnterior + quantidade;

            if (lote.getQuantidadeDisponivel() + quantidade > lote.getQuantidadeInicial()) {
                throw new BusinessRuleException(
                        "A devolução ultrapassaria a quantidade inicial do lote " + lote.getCodigoInterno() + "."
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
                        estoque, lote, usuarioResponsavel, pedido, pedido.getLaboratorio(),
                        TipoMovimentacao.DEVOLUCAO, OrigemMovimentacao.DEVOLUCAO,
                        quantidade, saldoAnterior, saldoAtual, observacao
                );
            }
        }
    }

    @Transactional
    public List<LoteResponseDTO> listarLotesOrdenadosParaSaida(Long estoqueId) {
        EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
        return buscarLotesParaSaida(estoque).stream()
                .map(LoteResponseDTO::new)
                .toList();
    }

    private CodigoLoteGerado gerarCodigoInternoLote(Produto produto) {
        Integer maiorSequencial = loteRepository.buscarMaiorSequencialInternoPorProduto(produto.getId());
        int sequencial = (maiorSequencial == null ? 0 : maiorSequencial) + 1;
        String sigla = gerarSiglaProduto(produto);
        String codigo = formatarCodigoLote(sigla, sequencial);

        while (loteRepository.existsByCodigoInterno(codigo)) {
            sequencial++;
            codigo = formatarCodigoLote(sigla, sequencial);
        }

        return new CodigoLoteGerado(codigo, sequencial);
    }

    private String gerarSiglaProduto(Produto produto) {
        String origem = produto.getCodigoReferencia();
        if (origem == null || origem.isBlank()) {
            origem = "PRD-" + produto.getId();
        }

        String sigla = origem
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return sigla.isBlank() ? "PRD-" + produto.getId() : sigla;
    }

    private String formatarCodigoLote(String sigla, int sequencial) {
        return "LOT-" + sigla + "-" + String.format(Locale.ROOT, "%03d", sequencial);
    }

    private record CodigoLoteGerado(String codigo, int sequencial) {}

    private List<Lote> buscarLotesParaSaida(EstoqueCentral estoque) {
        if (Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
            return loteRepository.buscarDisponiveisPorFefoComBloqueio(
                    estoque.getId(), LocalDate.now()
            );
        }
        return loteRepository.buscarDisponiveisPorEntradaComBloqueio(estoque.getId());
    }

    private EstoqueCentral buscarEstoqueAtivoComBloqueio(UUID estoquePublicId) {
        EstoqueCentral referencia = estoqueCentralRepository.findByPublicId(estoquePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", estoquePublicId));
        return buscarEstoqueAtivoComBloqueio(referencia.getId());
    }

    private EstoqueCentral buscarEstoqueAtivoComBloqueio(Long estoqueId) {
        EstoqueCentral estoque = estoqueCentralRepository
                .buscarPorIdComBloqueio(estoqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", estoqueId));
        estoque.validateActive();
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

    private void validarEntradaLote(Produto produto, EntradaLoteRequestDTO dto) {
        validarQuantidade(dto.getQuantidade());

        if (dto.getTipoEmbalagem() == null) {
            throw new BusinessRuleException("Tipo de embalagem é obrigatório.");
        }

        if (dto.getConteudoPorApresentacao() != null && dto.getConteudoPorApresentacao() <= 0) {
            throw new BusinessRuleException("Multiplicador deve ser maior que zero.");
        }

        if (dto.getOrigem() == null) {
            throw new BusinessRuleException("Origem da entrada é obrigatória.");
        }

        produto.validateLotExpirationDate(dto.getDataValidade());

        if (dto.getDataValidade() != null && dto.getDataValidade().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Não é possível registrar entrada de lote já vencido.");
        }
    }

    private String normalizarApresentacao(Produto produto, String apresentacao) {
        if (apresentacao != null && !apresentacao.isBlank()) return apresentacao.trim();
        if (produto.getUnidadeArmazenamento() != null && !produto.getUnidadeArmazenamento().isBlank()) {
            return produto.getUnidadeArmazenamento().trim();
        }
        return produto.getUnidadeMedida().name();
    }

    private void validarUsuarioResponsavel(Usuario usuario) {
        if (usuario == null) {
            throw new BusinessRuleException("Usuário responsável é obrigatório.");
        }
        usuario.validateActive();
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new BusinessRuleException("A quantidade deve ser maior que zero.");
        }
    }
}
