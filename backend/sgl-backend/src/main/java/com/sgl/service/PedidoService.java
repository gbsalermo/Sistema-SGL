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
import com.sgl.model.EstoqueCentral;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.StatusPedido;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
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

    @Transactional
    public PedidoDTO criar(PedidoDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()));

        Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Laboratório não encontrado com id: " + dto.getLaboratorioId()));

        Projeto projeto = null;
        if (dto.getProjetoId() != null) {
            projeto = projetoRepository.findById(dto.getProjetoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Projeto não encontrado com id: " + dto.getProjetoId()));
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
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Produto não encontrado com id: " + itemDTO.getProdutoId()));

            if (!produtosAdicionados.add(produto.getId())) {
                throw new IllegalArgumentException(
                        "O produto '" + produto.getNome()
                                + "' foi informado mais de uma vez no pedido.");
            }

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new IllegalArgumentException(
                        "O produto '" + produto.getNome() + "' está inativo.");
            }

            Long unidadeId = laboratorio.getUnidade().getId();

            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O produto '" + produto.getNome()
                                    + "' não possui estoque cadastrado na unidade "
                                    + laboratorio.getUnidade().getNome()));

            if (!Boolean.TRUE.equals(estoque.getAtivo())) {
                throw new IllegalArgumentException(
                        "O estoque central do produto '"
                                + produto.getNome() + "' está inativo.");
            }

            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .quantidadeSolicitada(itemDTO.getQuantidadeSolicitada())
                    .build();

            pedido.getItens().add(item);
        }

        Pedido salvo = pedidoRepository.save(pedido);
        return new PedidoDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(PedidoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com id: " + id));
        return new PedidoDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(PedidoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status)
                .stream()
                .map(PedidoDTO::new)
                .toList();
    }

    // Apenas os itens informados serão aprovados.
    // Os demais permanecerão com quantidadeAprovada = null,
    // caracterizando uma aprovação parcial do pedido.
    @Transactional
    public PedidoDTO aprovar(Long id, AprovarPedidoDTO dto) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException(
                    "Apenas pedidos PENDENTES podem ser aprovados. Status atual: " + pedido.getStatus());
        }

        for (AprovarPedidoDTO.ItemAprovacaoDTO itemAprovacao : dto.getItens()) {
            ItemPedido item = pedido.getItens().stream()
                    .filter(i -> i.getId().equals(itemAprovacao.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Item não encontrado com id: " + itemAprovacao.getItemId()));

            if (itemAprovacao.getQuantidadeAprovada() > item.getQuantidadeSolicitada()
                    || itemAprovacao.getQuantidadeAprovada() <= 0) {
                throw new IllegalArgumentException(
                        "Quantidade aprovada não pode ser maior que a solicitada ou menor/igual a zero. "
                                + "Solicitada: " + item.getQuantidadeSolicitada()
                                + ", Aprovada: " + itemAprovacao.getQuantidadeAprovada());
            }

            Long unidadeId = pedido.getLaboratorio().getUnidade().getId();

            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, item.getProduto().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Estoque não encontrado para o produto '"
                                    + item.getProduto().getNome()
                                    + "' na unidade "
                                    + pedido.getLaboratorio().getUnidade().getNome()));

            if (estoque.getQuantidadeAtual() < itemAprovacao.getQuantidadeAprovada()) {
                throw new IllegalArgumentException(
                        "Estoque insuficiente para o produto: " + item.getProduto().getNome()
                                + ". Disponível: " + estoque.getQuantidadeAtual()
                                + ", solicitado: " + itemAprovacao.getQuantidadeAprovada());
            }

            estoque.setQuantidadeAtual(
                    estoque.getQuantidadeAtual() - itemAprovacao.getQuantidadeAprovada());
            estoqueCentralRepository.save(estoque);

            item.setQuantidadeAprovada(itemAprovacao.getQuantidadeAprovada());
        }

        pedido.setStatus(StatusPedido.APROVADO);
        pedido.setObservacao(dto.getObservacao());

        Pedido atualizado = pedidoRepository.save(pedido);
        return new PedidoDTO(atualizado);
    }

    @Transactional
    public PedidoDTO rejeitar(Long id, String observacao) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException(
                    "Apenas pedidos PENDENTES podem ser rejeitados. Status atual: " + pedido.getStatus());
        }

        pedido.setStatus(StatusPedido.REJEITADO);
        pedido.setObservacao(observacao);

        Pedido atualizado = pedidoRepository.save(pedido);
        return new PedidoDTO(atualizado);
    }

    @Transactional
    public PedidoDTO entregar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pedido não encontrado com id: " + id));

        if (pedido.getStatus() != StatusPedido.APROVADO) {
            throw new IllegalArgumentException(
                    "Apenas pedidos APROVADOS podem ser entregues. Status atual: "
                            + pedido.getStatus());
        }

        for (ItemPedido item : pedido.getItens()) {
            if (item.getQuantidadeAprovada() != null
                    && item.getQuantidadeAprovada() > 0) {

                HistoricoLaboratorio estoqueLab = historicoLaboratorioRepository
                        .findByLaboratorioIdAndProdutoId(
                                pedido.getLaboratorio().getId(),
                                item.getProduto().getId())
                        .orElse(null);

                if (estoqueLab == null) {
                    estoqueLab = HistoricoLaboratorio.builder()
                            .laboratorio(pedido.getLaboratorio())
                            .produto(item.getProduto())
                            .quantidade(item.getQuantidadeAprovada())
                            .dataRecebimento(LocalDate.now())
                            .pedido(pedido)
                            .ativo(true)
                            .build();
                } else {
                    estoqueLab.setQuantidade(
                            estoqueLab.getQuantidade() + item.getQuantidadeAprovada());
                    estoqueLab.setDataRecebimento(LocalDate.now());
                    estoqueLab.setPedido(pedido);
                    estoqueLab.setAtivo(true);
                }

                historicoLaboratorioRepository.save(estoqueLab);

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

        Pedido atualizado = pedidoRepository.save(pedido);
        return new PedidoDTO(atualizado);
    }

    @Transactional
    public PedidoDTO cancelar(Long id, String observacao) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com id: " + id));

        if (pedido.getStatus() == StatusPedido.REJEITADO) {
            throw new IllegalArgumentException(
                    "Pedidos REJEITADOS já estão encerrados e não podem ser cancelados."
            );
        }
        if (pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new IllegalArgumentException("Pedidos ENTREGUES não podem ser cancelados");
        }

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("O pedido já está cancelado");
        }

        if (pedido.getStatus() == StatusPedido.APROVADO) {
            for (ItemPedido item : pedido.getItens()) {
                if (item.getQuantidadeAprovada() != null && item.getQuantidadeAprovada() > 0) {
                    Long unidadeId = pedido.getLaboratorio().getUnidade().getId();

                    EstoqueCentral estoque = estoqueCentralRepository
                            .findByUnidadeIdAndProdutoId(
                                    unidadeId,
                                    item.getProduto().getId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Estoque não encontrado para o produto '"
                                            + item.getProduto().getNome()
                                            + "' na unidade "
                                            + pedido.getLaboratorio().getUnidade().getNome()));

                    estoque.setQuantidadeAtual(
                            estoque.getQuantidadeAtual() + item.getQuantidadeAprovada());
                    estoqueCentralRepository.save(estoque);
                }
            }
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setObservacao(observacao);

        Pedido atualizado = pedidoRepository.save(pedido);
        return new PedidoDTO(atualizado);
    }

    private void validarConsistenciaPedido(
            Usuario usuario,
            Laboratorio laboratorio,
            Projeto projeto) {

        if (usuario.getLaboratorio() == null
                || !usuario.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new IllegalArgumentException(
                    "O usuário não pertence ao laboratório informado.");
        }

        if (usuario.getUnidade() == null || laboratorio.getUnidade() == null) {
            throw new IllegalArgumentException(
                    "Usuário e laboratório devem possuir uma unidade vinculada.");
        }

        if (!usuario.getUnidade().getId().equals(laboratorio.getUnidade().getId())) {
            throw new IllegalArgumentException(
                    "O usuário e o laboratório pertencem a unidades diferentes.");
        }

        if (projeto != null
                && (projeto.getLaboratorio() == null
                        || !projeto.getLaboratorio().getId().equals(laboratorio.getId()))) {
            throw new IllegalArgumentException(
                    "O projeto informado não pertence ao laboratório do pedido.");
        }
    }

    private void validarEntidadesAtivas(
            Usuario usuario,
            Laboratorio laboratorio,
            Projeto projeto) {

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException(
                    "O usuário informado está inativo.");
        }

        if (!Boolean.TRUE.equals(laboratorio.getAtivo())) {
            throw new IllegalArgumentException(
                    "O laboratório informado está inativo.");
        }

        if (projeto != null && !Boolean.TRUE.equals(projeto.getAtivo())) {
            throw new IllegalArgumentException(
                    "O projeto informado está inativo.");
        }
    }
}
