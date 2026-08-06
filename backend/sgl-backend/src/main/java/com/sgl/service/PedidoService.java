package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.AprovarPedidoDTO;
import com.sgl.dto.ItemPedidoDTO;
import com.sgl.dto.PedidoDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final HistoricoLaboratorioRepository historicoLaboratorioRepository;
    private final ProdutoRepository produtoRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public PedidoDTO criar(PedidoDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));

        Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Laboratório",
                        dto.getLaboratorioId()
                ));

        Projeto projeto = null;
        if (dto.getProjetoId() != null) {
            projeto = projetoRepository.findById(dto.getProjetoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Projeto",
                            dto.getProjetoId()
                    ));
        }

        validarConsistenciaPedido(usuario, laboratorio, projeto);
        validarEntidadesAtivas(usuario, laboratorio, projeto);

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .observacao(dto.getObservacao())
                .arquivoDocumento(dto.getArquivoDocumento())
                .itens(new ArrayList<>())
                .build();

        Set<Long> produtosAdicionados = new HashSet<>();

        for (ItemPedidoDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Produto",
                            itemDTO.getProdutoId()
                    ));

            if (!produtosAdicionados.add(produto.getId())) {
                throw new BusinessRuleException(
                        "O produto '" + produto.getNome()
                                + "' foi informado mais de uma vez no pedido."
                );
            }

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new BusinessRuleException(
                        "O produto '" + produto.getNome() + "' está inativo."
                );
            }

            Long unidadeId = laboratorio.getUnidade().getId();
            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estoque do produto '" + produto.getNome()
                                    + "' na unidade " + laboratorio.getUnidade().getNome()
                    ));

            if (!Boolean.TRUE.equals(estoque.getAtivo())) {
                throw new BusinessRuleException(
                        "O estoque central do produto '"
                                + produto.getNome() + "' está inativo."
                );
            }

            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .quantidadeSolicitada(itemDTO.getQuantidadeSolicitada())
                    .build();

            pedido.getItens().add(item);
        }

        return new PedidoDTO(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll().stream().map(PedidoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
        return new PedidoDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId).stream().map(PedidoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream().map(PedidoDTO::new).toList();
    }

    @Transactional
    public PedidoDTO aprovar(Long id, AprovarPedidoDTO dto) {
        Long aprovadorId = dto.getUsuarioAprovadorId();

        if (aprovadorId == null) {
            throw new BusinessRuleException("O usuário aprovador é obrigatório.");
        }

        Usuario usuarioAprovador = usuarioRepository.findById(aprovadorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário aprovador",
                        aprovadorId
                ));

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new BusinessRuleException(
                    "Apenas pedidos PENDENTES podem ser aprovados. Status atual: "
                            + pedido.getStatus()
            );
        }

        for (AprovarPedidoDTO.ItemAprovacaoDTO itemAprovacao : dto.getItens()) {
            ItemPedido item = pedido.getItens().stream()
                    .filter(i -> i.getId().equals(itemAprovacao.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Item do pedido",
                            itemAprovacao.getItemId()
                    ));

            Produto produto = item.getProduto();
            boolean produtoVencido = Boolean.TRUE.equals(produto.getPerecivel())
                    && produto.getDataValidade() != null
                    && produto.getDataValidade().isBefore(LocalDate.now());

            if (produtoVencido && !Boolean.TRUE.equals(dto.getAutorizarProdutoVencido())) {
                throw new BusinessRuleException(
                        "O produto '" + produto.getNome()
                                + "' está vencido. Confirme a autorização para continuar."
                );
            }

            Integer quantidadeAprovada = itemAprovacao.getQuantidadeAprovada();
            if (quantidadeAprovada == null
                    || quantidadeAprovada <= 0
                    || quantidadeAprovada > item.getQuantidadeSolicitada()) {
                throw new BusinessRuleException(
                        "Quantidade aprovada deve ser maior que zero e não pode ser maior "
                                + "que a solicitada. Solicitada: "
                                + item.getQuantidadeSolicitada()
                                + ", aprovada: " + quantidadeAprovada
                );
            }

            Long unidadeId = pedido.getLaboratorio().getUnidade().getId();
            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estoque do produto '" + produto.getNome()
                                    + "' na unidade "
                                    + pedido.getLaboratorio().getUnidade().getNome()
                    ));

            if (estoque.getQuantidadeAtual() < quantidadeAprovada) {
                throw new BusinessRuleException(
                        "Estoque insuficiente para o produto: " + produto.getNome()
                                + ". Disponível: " + estoque.getQuantidadeAtual()
                                + ", solicitado: " + quantidadeAprovada
                );
            }

            int quantidadeAnterior = estoque.getQuantidadeAtual();
            int quantidadeAtual = quantidadeAnterior - quantidadeAprovada;

            estoque.setQuantidadeAtual(quantidadeAtual);
            estoqueCentralRepository.save(estoque);
            item.setQuantidadeAprovada(quantidadeAprovada);

            MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                    .produto(produto)
                    .laboratorio(pedido.getLaboratorio())
                    .usuario(usuarioAprovador)
                    .pedido(pedido)
                    .tipoMovimentacao(TipoMovimentacao.SAIDA)
                    .origem(OrigemMovimentacao.PEDIDO)
                    .quantidadeMovimentada(quantidadeAprovada)
                    .quantidadeAnterior(quantidadeAnterior)
                    .quantidadeAtual(quantidadeAtual)
                    .dataMovimentacao(LocalDateTime.now())
                    .observacao(dto.getObservacao())
                    .estoqueCentral(estoque)
                    .build();

            movimentacaoEstoqueRepository.save(movimentacao);
        }

        pedido.setStatus(StatusPedido.APROVADO);
        pedido.setObservacao(dto.getObservacao());
        return new PedidoDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoDTO rejeitar(Long id, String observacao) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new BusinessRuleException(
                    "Apenas pedidos PENDENTES podem ser rejeitados. Status atual: "
                            + pedido.getStatus()
            );
        }

        pedido.setStatus(StatusPedido.REJEITADO);
        pedido.setObservacao(observacao);
        return new PedidoDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoDTO entregar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.getStatus() != StatusPedido.APROVADO) {
            throw new BusinessRuleException(
                    "Apenas pedidos APROVADOS podem ser entregues. Status atual: "
                            + pedido.getStatus()
            );
        }

        for (ItemPedido item : pedido.getItens()) {
            if (item.getQuantidadeAprovada() != null && item.getQuantidadeAprovada() > 0) {
                HistoricoLaboratorio historico = HistoricoLaboratorio.builder()
                        .laboratorio(pedido.getLaboratorio())
                        .produto(item.getProduto())
                        .quantidade(item.getQuantidadeAprovada())
                        .dataRecebimento(LocalDate.now())
                        .pedido(pedido)
                        .ativo(true)
                        .build();
                historicoLaboratorioRepository.save(historico);
            }
        }

        pedido.setStatus(StatusPedido.ENTREGUE);
        return new PedidoDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoDTO cancelar(Long id, String observacao) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));

        if (pedido.getStatus() == StatusPedido.REJEITADO) {
            throw new BusinessRuleException(
                    "Pedidos REJEITADOS já estão encerrados e não podem ser cancelados."
            );
        }
        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessRuleException("Pedidos ENTREGUES não podem ser cancelados.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new BusinessRuleException("O pedido já está cancelado.");
        }

        if (pedido.getStatus() == StatusPedido.APROVADO) {
            for (ItemPedido item : pedido.getItens()) {
                if (item.getQuantidadeAprovada() != null && item.getQuantidadeAprovada() > 0) {
                    Long unidadeId = pedido.getLaboratorio().getUnidade().getId();
                    EstoqueCentral estoque = estoqueCentralRepository
                            .findByUnidadeIdAndProdutoId(unidadeId, item.getProduto().getId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Estoque do produto '" + item.getProduto().getNome()
                                            + "' na unidade "
                                            + pedido.getLaboratorio().getUnidade().getNome()
                            ));

                    estoque.setQuantidadeAtual(
                            estoque.getQuantidadeAtual() + item.getQuantidadeAprovada()
                    );
                    estoqueCentralRepository.save(estoque);
                }
            }
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setObservacao(observacao);
        return new PedidoDTO(pedidoRepository.save(pedido));
    }

    private void validarConsistenciaPedido(
            Usuario usuario,
            Laboratorio laboratorio,
            Projeto projeto) {

        if (usuario.getLaboratorio() == null
                || !usuario.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException(
                    "O usuário não pertence ao laboratório informado."
            );
        }
        if (usuario.getUnidade() == null || laboratorio.getUnidade() == null) {
            throw new BusinessRuleException(
                    "Usuário e laboratório devem possuir uma unidade vinculada."
            );
        }
        if (!usuario.getUnidade().getId().equals(laboratorio.getUnidade().getId())) {
            throw new BusinessRuleException(
                    "O usuário e o laboratório pertencem a unidades diferentes."
            );
        }
        if (projeto != null
                && (projeto.getLaboratorio() == null
                || !projeto.getLaboratorio().getId().equals(laboratorio.getId()))) {
            throw new BusinessRuleException(
                    "O projeto informado não pertence ao laboratório do pedido."
            );
        }
    }

    private void validarEntidadesAtivas(
            Usuario usuario,
            Laboratorio laboratorio,
            Projeto projeto) {

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException("O usuário informado está inativo.");
        }
        if (!Boolean.TRUE.equals(laboratorio.getAtivo())) {
            throw new BusinessRuleException("O laboratório informado está inativo.");
        }
        if (projeto != null && !Boolean.TRUE.equals(projeto.getAtivo())) {
            throw new BusinessRuleException("O projeto informado está inativo.");
        }
    }
}
