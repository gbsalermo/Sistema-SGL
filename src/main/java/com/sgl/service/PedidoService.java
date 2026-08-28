package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.request.ItemPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
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
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Transactional
    public PedidoResponseDTO criar(PedidoRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByPublicId(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));

        Laboratorio laboratorio = laboratorioRepository.findByPublicId(dto.getLaboratorioId())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", dto.getLaboratorioId()));

        Projeto projeto = null;
        if (dto.getProjetoId() != null) {
            projeto = projetoRepository.findByPublicId(dto.getProjetoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto", dto.getProjetoId()));
        }

        validarConsistenciaPedido(usuario, laboratorio, projeto);
        usuario.validateActive();
        laboratorio.validateActive();
        if (projeto != null) projeto.validateActive();

        boolean urgente = Boolean.TRUE.equals(dto.getUrgente());
        String motivoUrgencia = normalizarTexto(dto.getMotivoUrgencia());
        if (!urgente) motivoUrgencia = null;

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .urgente(urgente)
                .motivoUrgencia(motivoUrgencia)
                .observacao(dto.getObservacao())
                .arquivoDocumento(dto.getArquivoDocumento())
                .itens(new ArrayList<>())
                .build();

        Set<Long> produtosAdicionados = new HashSet<>();

        for (ItemPedidoRequestDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findByPublicId(itemDTO.getProdutoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", itemDTO.getProdutoId()));

            if (!produtosAdicionados.add(produto.getId())) {
                throw new BusinessRuleException("O produto '" + produto.getNome() + "' foi informado mais de uma vez no pedido.");
            }

            produto.validateActive();
            Long unidadeId = laboratorio.getUnidade().getId();
            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estoque do produto '" + produto.getNome() + "' na unidade " + laboratorio.getUnidade().getNome()));
            estoque.validateActive();

            validarFormaRetirada(itemDTO);

            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .quantidadeSolicitada(itemDTO.getQuantidadeSolicitada())
                    .tipoEmbalagemSolicitada(itemDTO.getTipoEmbalagemSolicitada())
                    .quantidadeEmbalagensSolicitada(itemDTO.getQuantidadeEmbalagensSolicitada())
                    .multiplicadorSolicitado(itemDTO.getMultiplicadorSolicitado())
                    .build();
            pedido.getItens().add(item);
        }

        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(UUID id) {
        Pedido pedido = pedidoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
        return new PedidoResponseDTO(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
        return pedidoRepository.findByUsuarioId(usuario.getId()).stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status).stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorUrgencia(Boolean urgente) {
        return pedidoRepository.findByUrgente(urgente).stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPorProjetoEPeriodo(UUID laboratorioId, UUID projetoId, LocalDate dataInicio, LocalDate dataFim) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
        Projeto projeto = projetoRepository.findByPublicId(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));

        if (projeto.getLaboratorio() == null || !projeto.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException("O projeto informado não pertence ao laboratório informado.");
        }

        validarPeriodo(dataInicio, dataFim);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);
        return pedidoRepository.findByLaboratorioProjetoEPeriodo(laboratorio.getId(), projeto.getId(), inicio, fim)
                .stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional
    public PedidoResponseDTO aprovar(UUID id, AprovarPedidoRequestDTO dto) {
        UUID aprovadorId = dto.getUsuarioAprovadorId();
        if (aprovadorId == null) throw new BusinessRuleException("O usuário aprovador é obrigatório.");

        Usuario usuarioAprovador = usuarioRepository.findByPublicId(aprovadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário aprovador", aprovadorId));
        usuarioAprovador.validateActive();

        Pedido pedido = buscarPedidoComBloqueio(id);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new BusinessRuleException("Apenas pedidos PENDENTES podem ser aprovados. Status atual: " + pedido.getStatus());
        }

        for (AprovarPedidoRequestDTO.ItemAprovacaoDTO itemAprovacao : dto.getItens()) {
            ItemPedido item = pedido.getItens().stream()
                    .filter(i -> i.getPublicId().equals(itemAprovacao.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Item do pedido", itemAprovacao.getItemId()));

            Integer quantidadeAprovada = itemAprovacao.getQuantidadeAprovada();
            if (quantidadeAprovada == null || quantidadeAprovada <= 0 || quantidadeAprovada > item.getQuantidadeSolicitada()) {
                throw new BusinessRuleException("Quantidade aprovada deve ser maior que zero e não pode ser maior que a solicitada. Solicitada: "
                        + item.getQuantidadeSolicitada() + ", aprovada: " + quantidadeAprovada);
            }

            if (item.getTipoEmbalagemSolicitada() != TipoEmbalagem.UNITARIO
                    && quantidadeAprovada % item.getMultiplicadorSolicitado() != 0) {
                throw new BusinessRuleException("A quantidade aprovada deve respeitar a embalagem solicitada. "
                        + item.getTipoEmbalagemSolicitada() + " = " + item.getMultiplicadorSolicitado() + " unit.");
            }

            Produto produto = item.getProduto();
            Long unidadeId = pedido.getLaboratorio().getUnidade().getId();
            EstoqueCentral estoque = estoqueCentralRepository
                    .findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Estoque do produto '" + produto.getNome() + "' na unidade " + pedido.getLaboratorio().getUnidade().getNome()));

            movimentacaoEstoqueService.registrarSaida(
                    estoque.getId(),
                    quantidadeAprovada,
                    usuarioAprovador,
                    OrigemMovimentacao.PEDIDO,
                    pedido,
                    pedido.getLaboratorio(),
                    dto.getObservacao(),
                    item.getTipoEmbalagemSolicitada(),
                    item.getMultiplicadorSolicitado()
            );

            item.setQuantidadeAprovada(quantidadeAprovada);
        }

        pedido.setStatus(StatusPedido.APROVADO);
        pedido.setObservacao(dto.getObservacao());
        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO rejeitar(UUID id, String observacao) {
        Pedido pedido = buscarPedidoComBloqueio(id);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new BusinessRuleException("Apenas pedidos PENDENTES podem ser rejeitados. Status atual: " + pedido.getStatus());
        }
        pedido.setStatus(StatusPedido.REJEITADO);
        pedido.setObservacao(observacao);
        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO entregar(UUID id) {
        Pedido pedido = buscarPedidoComBloqueio(id);
        if (pedido.getStatus() != StatusPedido.APROVADO) {
            throw new BusinessRuleException("Apenas pedidos APROVADOS podem ser entregues. Status atual: " + pedido.getStatus());
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
        pedido.setDataEntrega(LocalDateTime.now());
        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO cancelar(UUID id, String observacao) {
        Pedido pedido = buscarPedidoComBloqueio(id);
        if (pedido.getStatus() == StatusPedido.REJEITADO) throw new BusinessRuleException("Pedidos REJEITADOS já estão encerrados e não podem ser cancelados.");
        if (pedido.getStatus() == StatusPedido.ENTREGUE) throw new BusinessRuleException("Pedidos ENTREGUES não podem ser cancelados.");
        if (pedido.getStatus() == StatusPedido.CANCELADO) throw new BusinessRuleException("O pedido já está cancelado.");

        if (pedido.getStatus() == StatusPedido.APROVADO) {
            movimentacaoEstoqueService.devolverSaidasDoPedido(pedido, null, observacao);
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setObservacao(observacao);
        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    private void validarFormaRetirada(ItemPedidoRequestDTO itemDTO) {
        if (itemDTO.getTipoEmbalagemSolicitada() == null
                || itemDTO.getQuantidadeEmbalagensSolicitada() == null
                || itemDTO.getMultiplicadorSolicitado() == null) {
            throw new BusinessRuleException("Forma de retirada, quantidade e multiplicador são obrigatórios.");
        }
        if (itemDTO.getQuantidadeEmbalagensSolicitada() <= 0 || itemDTO.getMultiplicadorSolicitado() <= 0) {
            throw new BusinessRuleException("Quantidade da forma de retirada e multiplicador devem ser maiores que zero.");
        }
        if (itemDTO.getTipoEmbalagemSolicitada() == TipoEmbalagem.UNITARIO && itemDTO.getMultiplicadorSolicitado() != 1) {
            throw new BusinessRuleException("Retirada unitária deve usar multiplicador 1.");
        }
        int esperado;
        try {
            esperado = Math.multiplyExact(itemDTO.getQuantidadeEmbalagensSolicitada(), itemDTO.getMultiplicadorSolicitado());
        } catch (ArithmeticException ex) {
            throw new BusinessRuleException("Quantidade total solicitada excede o limite suportado.");
        }
        if (esperado != itemDTO.getQuantidadeSolicitada()) {
            throw new BusinessRuleException("Quantidade total inconsistente com a forma de retirada escolhida.");
        }
    }

    private Pedido buscarPedidoComBloqueio(UUID publicId) {
        Pedido referencia = pedidoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", publicId));
        return pedidoRepository.buscarPorIdComBloqueio(referencia.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", publicId));
    }

    private void validarConsistenciaPedido(Usuario usuario, Laboratorio laboratorio, Projeto projeto) {
        if (usuario.getLaboratorio() == null || !usuario.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException("O usuário não pertence ao laboratório informado.");
        }
        if (usuario.getUnidade() == null || laboratorio.getUnidade() == null) {
            throw new BusinessRuleException("Usuário e laboratório devem possuir uma unidade vinculada.");
        }
        if (!usuario.getUnidade().getId().equals(laboratorio.getUnidade().getId())) {
            throw new BusinessRuleException("O usuário e o laboratório pertencem a unidades diferentes.");
        }
        if (projeto != null && (projeto.getLaboratorio() == null || !projeto.getLaboratorio().getId().equals(laboratorio.getId()))) {
            throw new BusinessRuleException("O projeto informado não pertence ao laboratório do pedido.");
        }
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null) throw new BusinessRuleException("Data inicial e data final são obrigatórias.");
        if (dataInicio.isAfter(dataFim)) throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
    }

    private String normalizarTexto(String valor) {
        if (valor == null) return null;
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
