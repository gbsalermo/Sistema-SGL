package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
		Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
				.orElseThrow(() -> new EntityNotFoundException("Laboratório não encontrado com id: " + dto.getLaboratorioId()));
		
		Pedido pedido = Pedido.builder()
				.usuario(usuarioRepository.findById(dto.getUsuarioId())
				.orElseThrow(() -> new EntityNotFoundException("Usuario não encontrado com id: " + dto.getUsuarioId())))
				.laboratorio(laboratorio)
				.projeto(dto.getProjetoId() != null ?
						projetoRepository.findById(dto.getProjetoId())
								.orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + dto.getProjetoId())) : null)
				.dataSolicitacao(LocalDateTime.now())
				.status(StatusPedido.PENDENTE)
				.observacao(dto.getObservacao())
				.arquivoDocumento(dto.getArquivoDocumento())
				.itens(new ArrayList<>())
				.build();
		
		for(ItemPedidoDTO itemDTO : dto.getItens()) {
			Produto produto = produtoRepository.findById(itemDTO.getProdutoId())
					.orElseThrow(() -> new EntityNotFoundException("Produto não Encontrado com id: " + itemDTO.getProdutoId()));
					
			EstoqueCentral estoque = estoqueCentralRepository.findByProdutoId(produto.getId())
			        .orElseThrow(() -> new IllegalArgumentException(
			                "Produto não possui estoque central registrado: " + produto.getNome()));

			if (!estoque.getAtivo()) {
			    throw new IllegalArgumentException(
			            "O estoque central do produto '" + produto.getNome() + "' está inativo.");
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
	public List<PedidoDTO> listarTodos(){
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
	public List<PedidoDTO> listarPorUsuario(Long usuarioId){
		return pedidoRepository.findByUsuarioId(usuarioId)
				.stream()
				.map(PedidoDTO::new)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<PedidoDTO> listarPorStatus(StatusPedido status){
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
		
		if(pedido.getStatus() != StatusPedido.PENDENTE) {
			throw new IllegalArgumentException("Apenas pedidos PENDENTES podem ser aprovados. Status atual: " + pedido.getStatus());		
		}
		
		for (AprovarPedidoDTO.ItemAprovacaoDTO itemAprovacao : dto.getItens()) {
			ItemPedido item = pedido.getItens().stream()
					.filter(i -> i.getId().equals(itemAprovacao.getItemId()))
					.findFirst()
					.orElseThrow(() -> new EntityNotFoundException("Item não encontrado com id: " + itemAprovacao.getItemId()));		
		
			if(itemAprovacao.getQuantidadeAprovada() > item.getQuantidadeSolicitada() && itemAprovacao.getQuantidadeAprovada() <= 0) {
				throw new IllegalArgumentException("Quantidade aprovada não pode ser maior que a solicitada ou menor/igual a zero. "
						+ "Solicitadada: " + item.getQuantidadeSolicitada()
						+ ", Aprovada: " + itemAprovacao.getQuantidadeAprovada());
			}
			
			EstoqueCentral estoque = estoqueCentralRepository.findByProdutoId(item.getProduto().getId())
					.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado para o produto " + item.getProduto().getNome()));
			
			if(estoque.getQuantidadeAtual() < itemAprovacao.getQuantidadeAprovada()) {
				throw new IllegalArgumentException("Estoque insuficiente para o produto: " + item.getProduto().getNome()
						+ ". Disponivel: " + estoque.getQuantidadeAtual()
						+ ", Solicitado: " + itemAprovacao.getQuantidadeAprovada());
			}
			
			estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() - itemAprovacao.getQuantidadeAprovada());
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
		
		if(pedido.getStatus() != StatusPedido.PENDENTE) {
			throw new IllegalArgumentException("Apenas pedidos PENDENTES podem ser rejeitados. Status atual: " + pedido.getStatus());
			
		}
			pedido.setStatus(StatusPedido.REJEITADO);
			pedido.setObservacao(observacao);
			
			Pedido atualizado = pedidoRepository.save(pedido);
			return new PedidoDTO(atualizado);
		}
	
	@Transactional
	public PedidoDTO entregar(Long id) {

	    // Busca o pedido pelo id
	    Pedido pedido = pedidoRepository.findById(id)
	            .orElseThrow(() ->
	                    new EntityNotFoundException(
	                            "Pedido não encontrado com id: " + id));

	    // Apenas pedidos APROVADOS podem ser entregues
	    if (pedido.getStatus() != StatusPedido.APROVADO) {
	        throw new IllegalArgumentException(
	                "Apenas pedidos APROVADOS podem ser entregues. Status atual: "
	                        + pedido.getStatus());
	    }

	    // Percorre todos os itens do pedido
	    for (ItemPedido item : pedido.getItens()) {

	        // Ignora itens que não tiveram quantidade aprovada
	        if (item.getQuantidadeAprovada() != null
	                && item.getQuantidadeAprovada() > 0) {

	            // Procura se o laboratório já possui esse produto em estoque
	            HistoricoLaboratorio estoqueLab =
	            		historicoLaboratorioRepository
	                            .findByLaboratorioIdAndProdutoId(
	                                    pedido.getLaboratorio().getId(),
	                                    item.getProduto().getId())
	                            .orElse(null);

	            // Caso NÃO exista, cria um novo estoque
	            if (estoqueLab == null) {

	                estoqueLab = HistoricoLaboratorio.builder()
	                        .laboratorio(pedido.getLaboratorio())
	                        .produto(item.getProduto())
	                        .quantidade(item.getQuantidadeAprovada())
	                        .dataRecebimento(LocalDate.now())
	                        .pedido(pedido)
	                        .ativo(true)
	                        .build();

	            }
	            // Caso já exista, apenas soma a quantidade recebida
	            else {

	                estoqueLab.setQuantidade(
	                        estoqueLab.getQuantidade()
	                                + item.getQuantidadeAprovada());

	                // Atualiza a data da última entrada
	                estoqueLab.setDataRecebimento(LocalDate.now());

	                // Atualiza o último pedido responsável pela entrada
	                estoqueLab.setPedido(pedido);

	                // Garante que o estoque permaneça ativo
	                estoqueLab.setAtivo(true);
	            }

	            // Salva o estoque atualizado
	            historicoLaboratorioRepository.save(estoqueLab);

	            // ======================================================
	            // REGISTRA O HISTÓRICO DA ENTREGA
	            // ======================================================

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

	    // Após entregar todos os itens, altera o status do pedido
	    pedido.setStatus(StatusPedido.ENTREGUE);

	    Pedido atualizado = pedidoRepository.save(pedido);

	    return new PedidoDTO(atualizado);
	}
	
	@Transactional
	public PedidoDTO cancelar(Long id, String observacao) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado com id: " + id));
		
		if(pedido.getStatus() == StatusPedido.ENTREGUE) {
			throw new IllegalArgumentException("Pedidos ENTREGUES não podem ser cancelados");
		}
		
		if(pedido.getStatus() == StatusPedido.CANCELADO) {
			throw new IllegalArgumentException("Pedidos ja está cancelado");
		}
		
		if(pedido.getStatus() == StatusPedido.APROVADO) {
			for(ItemPedido item : pedido.getItens()) {
				if(item.getQuantidadeAprovada() != null && item.getQuantidadeAprovada() > 0) {
					EstoqueCentral estoque = estoqueCentralRepository.findByProdutoId(item.getProduto().getId())
							.orElseThrow(() -> new EntityNotFoundException("Estoque central não encontrado para o produto: " + item.getProduto().getNome()));
					
					estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() 
							+ item.getQuantidadeAprovada());
					estoqueCentralRepository.save(estoque);
				}
			}
		}
		
		pedido.setStatus(StatusPedido.CANCELADO);
		pedido.setObservacao(observacao);
		
		Pedido atualizado = pedidoRepository.save(pedido);
		return new PedidoDTO(atualizado);
	}
}
