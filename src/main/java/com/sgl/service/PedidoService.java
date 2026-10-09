package com.sgl.service;

import java.math.BigDecimal;
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
import com.sgl.model.ModeloSolucao;
import com.sgl.model.ComponenteModeloSolucao;
import com.sgl.model.medida.ConversorUnidadeMedida;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoPedido;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ModeloSolucaoRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

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
	private final ModeloSolucaoRepository modeloSolucaoRepository;

	@Transactional
	public PedidoResponseDTO criar(PedidoRequestDTO dto) {
		Usuario usuario = buscarUsuarioNoTenant(dto.getUsuarioId());

		Laboratorio laboratorio = laboratorioRepository.findByPublicId(dto.getLaboratorioId())
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", dto.getLaboratorioId()));
		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

		Projeto projeto = null;
		if (dto.getProjetoId() != null) {
			projeto = projetoRepository.findByPublicId(dto.getProjetoId())
					.orElseThrow(() -> new ResourceNotFoundException("Projeto", dto.getProjetoId()));
			if (projeto.getLaboratorio() != null && projeto.getLaboratorio().getUnidade() != null) {
				validarTenantUnidade(projeto.getLaboratorio().getUnidade().getPublicId());
			}
		}

		validarConsistenciaPedido(usuario, laboratorio, projeto);
		usuario.validateActive();
		laboratorio.validateActive();
		if (projeto != null)
			projeto.validateActive();

        TipoPedido tipo = dto.getTipo() == null ? TipoPedido.PRODUTOS : dto.getTipo();
        ModeloSolucao modelo = null;
        String nomeSolucao = null;
        List<ItemPedidoRequestDTO> itensSolicitados = dto.getItens();

        if (tipo == TipoPedido.SOLUCAO) {
            if (projeto == null)
                throw new BusinessRuleException("Projeto é obrigatório para pedir uma Solução.");
            if (dto.getModeloSolucaoId() != null) {
                if (itensSolicitados != null && !itensSolicitados.isEmpty())
                    throw new BusinessRuleException("Ao selecionar um modelo, os itens são gerados pelo servidor.");
                UUID unidade = laboratorio.getUnidade().getPublicId();
                modelo = modeloSolucaoRepository.findByPublicIdAndUnidadePublicId(dto.getModeloSolucaoId(), unidade)
                    .orElseThrow(() -> new ResourceNotFoundException("Modelo de Solução", dto.getModeloSolucaoId()));
                if (!Boolean.TRUE.equals(modelo.getAtivo()))
                    throw new BusinessRuleException("O modelo de Solução está inativo.");
                nomeSolucao = modelo.getNome();
                itensSolicitados = modelo.getComponentes().stream()
                    .sorted(java.util.Comparator.comparing(ComponenteModeloSolucao::getOrdem))
                    .map(c -> {
                        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
                        item.setProdutoId(c.getProduto().getPublicId());
                        item.setQuantidadeSolicitada(c.getQuantidade());
                        item.setUnidadeMedidaSolicitada(c.getUnidadeMedida());
                        return item;
                    }).toList();
            } else {
                nomeSolucao = dto.getNomeSolucao();
                if (nomeSolucao == null || nomeSolucao.isBlank())
                    throw new BusinessRuleException("Informe o nome da Solução personalizada.");
                nomeSolucao = nomeSolucao.trim();
            }
        } else if (dto.getModeloSolucaoId() != null || dto.getNomeSolucao() != null) {
            throw new BusinessRuleException("Modelo e nome de Solução só são permitidos em pedidos de Solução.");
        }
        if (itensSolicitados == null || itensSolicitados.isEmpty())
            throw new BusinessRuleException("O pedido precisa de pelo menos um Produto.");

		boolean urgente = Boolean.TRUE.equals(dto.getUrgente());
		String motivoUrgencia = normalizarTexto(dto.getMotivoUrgencia());
		if (!urgente)
			motivoUrgencia = null;

		Pedido pedido = Pedido.builder().usuario(usuario).laboratorio(laboratorio).projeto(projeto)
				.dataSolicitacao(LocalDateTime.now()).status(StatusPedido.PENDENTE).tipo(tipo)
                .nomeSolucao(nomeSolucao).modeloSolucao(modelo).urgente(urgente)
				.motivoUrgencia(motivoUrgencia).observacao(dto.getObservacao())
				.arquivoDocumento(dto.getArquivoDocumento()).itens(new ArrayList<>()).build();

		Set<Long> produtosAdicionados = new HashSet<>();

		for (ItemPedidoRequestDTO itemDTO : itensSolicitados) {
            if (itemDTO == null || itemDTO.getProdutoId() == null)
                throw new BusinessRuleException("Produto obrigatório na composição.");
			Produto produto = produtoRepository.findByPublicId(itemDTO.getProdutoId())
					.orElseThrow(() -> new ResourceNotFoundException("Produto", itemDTO.getProdutoId()));

			// Correção de bug: essa validação checa se o produto realmente
			// tem estoque cadastrado na unidade do laboratório do pedido —
			// isso não depende de "quem está fazendo a chamada" (tenant),
			// e sim do laboratório já resolvido acima. O "if
			// (TenantContext.ativo() && ...)" antigo pulava essa checagem
			// por completo quando a requisição não enviava o header de
			// tenant, permitindo criar pedido com produto de outra unidade.
			// A validação agora roda sempre.
			if (!produtoRepository.pertenceAUnidade(produto.getPublicId(), laboratorio.getUnidade().getPublicId())) {
				throw new ResourceNotFoundException("Produto", itemDTO.getProdutoId());
			}

			if (!produtosAdicionados.add(produto.getId())) {
				throw new BusinessRuleException(
						"O produto '" + produto.getNome() + "' foi informado mais de uma vez no pedido.");
			}

			produto.validateActive();
			Long unidadeId = laboratorio.getUnidade().getId();
			EstoqueCentral estoque = estoqueCentralRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Estoque do produto '" + produto.getNome()
							+ "' na unidade " + laboratorio.getUnidade().getNome()));
			estoque.validateActive();

            BigDecimal quantidadeCanonica = itemDTO.getQuantidadeSolicitada();
            UnidadeMedida unidadeInformada = null;
            if (tipo == TipoPedido.SOLUCAO) {
                unidadeInformada = itemDTO.getUnidadeMedidaSolicitada();
                if (unidadeInformada == null || quantidadeCanonica == null
                        || quantidadeCanonica.compareTo(BigDecimal.ZERO) <= 0)
                    throw new BusinessRuleException("Cada componente exige quantidade positiva e unidade física.");
                quantidadeCanonica = ConversorUnidadeMedida.converterParaCanonica(
                    quantidadeCanonica, unidadeInformada, produto.getUnidadeMedida());
                if (quantidadeCanonica.stripTrailingZeros().scale() > 6)
                    throw new BusinessRuleException("Quantidade convertida excede 6 casas decimais.");
            } else {
                validarFormaRetirada(itemDTO);
            }

            ItemPedido item = ItemPedido.builder().pedido(pedido).produto(produto)
                    .quantidadeSolicitada(quantidadeCanonica)
                    .unidadeMedidaSolicitada(unidadeInformada)
                    .tipoEmbalagemSolicitada(tipo == TipoPedido.PRODUTOS ? itemDTO.getTipoEmbalagemSolicitada() : null)
                    .quantidadeEmbalagensSolicitada(tipo == TipoPedido.PRODUTOS ? itemDTO.getQuantidadeEmbalagensSolicitada() : null)
                    .multiplicadorSolicitado(tipo == TipoPedido.PRODUTOS ? itemDTO.getMultiplicadorSolicitado() : null)
                    .build();
			pedido.getItens().add(item);
		}

		return new PedidoResponseDTO(pedidoRepository.save(pedido));
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> listarTodos() {
		// Correção de segurança: sem tenant ativo, caía num "findAll" que
		// devolvia pedidos de todas as unidades. Agora o header
		// X-SGL-Unidade-Id é exigido também para listar.
		exigirTenantAtivo();

		List<Pedido> pedidos = pedidoRepository
				.findByLaboratorioUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());
		return pedidos.stream().map(PedidoResponseDTO::new).toList();
	}

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarSolucoes() {
        exigirTenantAtivo();
        return pedidoRepository.findByLaboratorioUnidadePublicIdAndTipo(
                TenantContext.unidadeAtual().orElseThrow(), TipoPedido.SOLUCAO)
                .stream().map(PedidoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarSolucao(UUID id) {
        Pedido pedido = buscarPedidoNoTenant(id);
        exigirTipoSolucao(pedido);
        return new PedidoResponseDTO(pedido);
    }

	@Transactional(readOnly = true)
	public PedidoResponseDTO buscarPorId(UUID id) {
		return new PedidoResponseDTO(buscarPedidoNoTenant(id));
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> listarPorUsuario(UUID usuarioId) {
		Usuario usuario = buscarUsuarioNoTenant(usuarioId);
		exigirTenantAtivo();

		List<Pedido> pedidos = pedidoRepository.findByUsuarioIdAndLaboratorioUnidadePublicId(usuario.getId(),
				TenantContext.unidadeAtual().orElseThrow());
		return pedidos.stream().map(PedidoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> listarPorStatus(StatusPedido status) {
		exigirTenantAtivo();

		List<Pedido> pedidos = pedidoRepository
				.findByLaboratorioUnidadePublicIdAndStatus(TenantContext.unidadeAtual().orElseThrow(), status);
		return pedidos.stream().map(PedidoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> listarPorUrgencia(Boolean urgente) {
		exigirTenantAtivo();

		List<Pedido> pedidos = pedidoRepository
				.findByLaboratorioUnidadePublicIdAndUrgente(TenantContext.unidadeAtual().orElseThrow(), urgente);
		return pedidos.stream().map(PedidoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<PedidoResponseDTO> listarPorProjetoEPeriodo(UUID laboratorioId, UUID projetoId, LocalDate dataInicio,
			LocalDate dataFim) {
		Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

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
		if (aprovadorId == null)
			throw new BusinessRuleException("O usuário aprovador é obrigatório.");

		Usuario usuarioAprovador = buscarUsuarioNoTenant(aprovadorId);
		usuarioAprovador.validateActive();

		Pedido pedido = buscarPedidoComBloqueio(id);
		if (pedido.getStatus() != StatusPedido.PENDENTE) {
			throw new BusinessRuleException(
					"Apenas pedidos PENDENTES podem ser aprovados. Status atual: " + pedido.getStatus());
		}

        if (pedido.getTipo() == TipoPedido.SOLUCAO) {
            if (dto.getItens() == null || dto.getItens().size() != pedido.getItens().size())
                throw new BusinessRuleException("Todos os componentes da Solução devem ser aprovados.");
            Set<UUID> componentes = new HashSet<>();
            for (AprovarPedidoRequestDTO.ItemAprovacaoDTO item : dto.getItens()) {
                if (item == null || item.getItemId() == null || !componentes.add(item.getItemId()))
                    throw new BusinessRuleException("Não repita componentes da Solução.");
            }
        }

		for (AprovarPedidoRequestDTO.ItemAprovacaoDTO itemAprovacao : dto.getItens()) {
			ItemPedido item = pedido.getItens().stream().filter(i -> i.getPublicId().equals(itemAprovacao.getItemId()))
					.findFirst()
					.orElseThrow(() -> new ResourceNotFoundException("Item do pedido", itemAprovacao.getItemId()));

			BigDecimal quantidadeAprovada = itemAprovacao.getQuantidadeAprovada();

			if (quantidadeAprovada == null || quantidadeAprovada.compareTo(BigDecimal.ZERO) <= 0
					|| quantidadeAprovada.compareTo(item.getQuantidadeSolicitada()) > 0) {
				throw new BusinessRuleException(
						"Quantidade aprovada deve ser maior que zero e não pode ser maior que a solicitada. Solicitada: "
								+ item.getQuantidadeSolicitada() + ", aprovada: " + quantidadeAprovada);
			}

            if (pedido.getTipo() == TipoPedido.SOLUCAO
                    && quantidadeAprovada.compareTo(item.getQuantidadeSolicitada()) != 0)
                throw new BusinessRuleException("Para alterar a composição, rejeite o pedido e solicite nova receita.");

			if (pedido.getTipo() != TipoPedido.SOLUCAO
                    && item.getTipoEmbalagemSolicitada() != TipoEmbalagem.UNITARIO) {
				// Correção de bug: "multiplicadorSolicitado" é um Integer
				// (objeto), que pode ser nulo — por exemplo, num item que
				// foi persistido antes desse campo existir, ou criado sem
				// passar pelo @PrePersist que normalmente preenche o valor
				// padrão. Antes, "quantidadeAprovada % null" desembrulhava
				// o Integer nulo e lançava NullPointerException, devolvendo
				// um erro 500 (interno) em vez de um erro de negócio 400
				// claro para quem está aprovando o pedido.

				BigDecimal multiplicador = item.getMultiplicadorSolicitado();

				if (multiplicador == null || multiplicador.compareTo(BigDecimal.ZERO) <= 0) {
					throw new BusinessRuleException("O item de embalagem " + item.getTipoEmbalagemSolicitada()
							+ " não possui um multiplicador de embalagem válido e não pode ser aprovado.");
				}

				if (quantidadeAprovada.remainder(multiplicador).compareTo(BigDecimal.ZERO) != 0) {
					throw new BusinessRuleException("A quantidade aprovada deve respeitar a embalagem solicitada. "
							+ item.getTipoEmbalagemSolicitada() + " = " + multiplicador + " unit.");
				}
			}

			Produto produto = item.getProduto();
			Long unidadeId = pedido.getLaboratorio().getUnidade().getId();
			EstoqueCentral estoque = estoqueCentralRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Estoque do produto '" + produto.getNome()
							+ "' na unidade " + pedido.getLaboratorio().getUnidade().getNome()));

            movimentacaoEstoqueService.registrarSaida(estoque.getId(), quantidadeAprovada, usuarioAprovador,
                    OrigemMovimentacao.PEDIDO, pedido, pedido.getLaboratorio(), dto.getObservacao(),
                    pedido.getTipo() == TipoPedido.SOLUCAO ? null : item.getTipoEmbalagemSolicitada(),
                    pedido.getTipo() == TipoPedido.SOLUCAO ? null : item.getMultiplicadorSolicitado());

			item.setQuantidadeAprovada(quantidadeAprovada);
		}

        pedido.setStatus(pedido.getTipo() == TipoPedido.SOLUCAO
                ? StatusPedido.EM_PREPARACAO : StatusPedido.APROVADO);
		pedido.setObservacao(dto.getObservacao());
		return new PedidoResponseDTO(pedidoRepository.save(pedido));
	}

	@Transactional
	public PedidoResponseDTO rejeitar(UUID id, String observacao) {
		Pedido pedido = buscarPedidoComBloqueio(id);
		if (pedido.getStatus() != StatusPedido.PENDENTE) {
			throw new BusinessRuleException(
					"Apenas pedidos PENDENTES podem ser rejeitados. Status atual: " + pedido.getStatus());
		}
		pedido.setStatus(StatusPedido.REJEITADO);
		pedido.setObservacao(observacao);
		return new PedidoResponseDTO(pedidoRepository.save(pedido));
	}

	@Transactional
	public PedidoResponseDTO entregar(UUID id) {
		Pedido pedido = buscarPedidoComBloqueio(id);
        boolean solucaoEmPreparo = pedido.getTipo() == TipoPedido.SOLUCAO
                && pedido.getStatus() == StatusPedido.EM_PREPARACAO;
        if (!solucaoEmPreparo && pedido.getStatus() != StatusPedido.APROVADO) {
            throw new BusinessRuleException("Pedido deve estar aprovado para entrega. Status atual: " + pedido.getStatus());
        }

		for (ItemPedido item : pedido.getItens()) {
			if (item.getQuantidadeAprovada() != null && item.getQuantidadeAprovada().compareTo(BigDecimal.ZERO) > 0) {
				HistoricoLaboratorio historico = HistoricoLaboratorio.builder().laboratorio(pedido.getLaboratorio())
						.produto(item.getProduto()).quantidade(item.getQuantidadeAprovada())
						.dataRecebimento(LocalDate.now()).pedido(pedido).ativo(true).build();
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
        if (pedido.getTipo() == TipoPedido.SOLUCAO)
            throw new BusinessRuleException("Use o cancelamento específico de Soluções com confirmação de preparo.");
		if (pedido.getStatus() == StatusPedido.REJEITADO)
			throw new BusinessRuleException("Pedidos REJEITADOS já estão encerrados e não podem ser cancelados.");
		if (pedido.getStatus() == StatusPedido.ENTREGUE)
			throw new BusinessRuleException("Pedidos ENTREGUES não podem ser cancelados.");
		if (pedido.getStatus() == StatusPedido.CANCELADO)
			throw new BusinessRuleException("O pedido já está cancelado.");

		if (pedido.getStatus() == StatusPedido.APROVADO) {
			movimentacaoEstoqueService.devolverSaidasDoPedido(pedido, null, observacao);
		}
		pedido.setStatus(StatusPedido.CANCELADO);
		pedido.setObservacao(observacao);
		return new PedidoResponseDTO(pedidoRepository.save(pedido));
	}

    @Transactional
    public PedidoResponseDTO cancelarSolucao(UUID id, Boolean preparada, String justificativa) {
        Pedido pedido = buscarPedidoComBloqueio(id);
        exigirTipoSolucao(pedido);
        if (justificativa == null || justificativa.isBlank())
            throw new BusinessRuleException("Informe a justificativa de cancelamento da Solução.");
        if (pedido.getStatus() == StatusPedido.ENTREGUE || pedido.getStatus() == StatusPedido.REJEITADO
                || pedido.getStatus() == StatusPedido.CANCELADO)
            throw new BusinessRuleException("A Solução já está encerrada.");
        if (pedido.getStatus() == StatusPedido.EM_PREPARACAO) {
            if (preparada == null)
                throw new BusinessRuleException("Confirme se a Solução já foi preparada fisicamente.");
            if (!preparada)
                movimentacaoEstoqueService.devolverSaidasDoPedido(pedido, null, justificativa);
            pedido.setSolucaoPreparadaNoCancelamento(preparada);
            pedido.setEstoqueRevertidoNoCancelamento(!preparada);
        } else if (pedido.getStatus() == StatusPedido.PENDENTE) {
            pedido.setSolucaoPreparadaNoCancelamento(false);
            pedido.setEstoqueRevertidoNoCancelamento(false); // Nao existiram saidas.
        } else {
            throw new BusinessRuleException("A Solução só pode ser cancelada quando pendente ou em preparação.");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setObservacao(justificativa.trim());
        return new PedidoResponseDTO(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO aprovarSolucao(UUID id, AprovarPedidoRequestDTO dto) {
        exigirTipoSolucao(buscarPedidoNoTenant(id));
        return aprovar(id, dto);
    }

    @Transactional
    public PedidoResponseDTO entregarSolucao(UUID id) {
        exigirTipoSolucao(buscarPedidoNoTenant(id));
        return entregar(id);
    }

    private void exigirTipoSolucao(Pedido pedido) {
        if (pedido.getTipo() != TipoPedido.SOLUCAO)
            throw new BusinessRuleException("Pedido informado não é uma Solução.");
    }

	private Pedido buscarPedidoNoTenant(UUID id) {
		// Correção de segurança: antes, sem tenant ativo, buscava sem
		// filtro de unidade (findByPublicId), vazando o pedido de outra
		// unidade para quem não enviasse o header.
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return pedidoRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
	}

	private Usuario buscarUsuarioNoTenant(UUID id) {
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return usuarioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
	}

	private Pedido buscarPedidoComBloqueio(UUID publicId) {
		Pedido referencia = buscarPedidoNoTenant(publicId);
		return pedidoRepository.buscarPorIdComBloqueio(referencia.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Pedido", publicId));
	}

	private void validarTenantUnidade(UUID unidadeId) {
		if (!TenantContext.pertence(unidadeId)) {
			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	/**
	 * Garante que existe uma unidade (tenant) definida para a requisição atual. Ver
	 * o mesmo método em EstoqueCentralService para a explicação completa do porquê
	 * essa checagem existe.
	 */
	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
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
		validarTenantUnidade(usuario.getUnidade().getPublicId());
		if (projeto != null && (projeto.getLaboratorio() == null
				|| !projeto.getLaboratorio().getId().equals(laboratorio.getId()))) {
			throw new BusinessRuleException("O projeto informado não pertence ao laboratório do pedido.");
		}
	}

	private void validarFormaRetirada(ItemPedidoRequestDTO itemDTO) {
		if (itemDTO.getTipoEmbalagemSolicitada() == null || itemDTO.getQuantidadeEmbalagensSolicitada() == null
				|| itemDTO.getMultiplicadorSolicitado() == null) {
			throw new BusinessRuleException("Forma de retirada, quantidade e multiplicador são obrigatórios.");
		}
		if (itemDTO.getQuantidadeEmbalagensSolicitada() <= 0
				|| itemDTO.getMultiplicadorSolicitado().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException(
					"Quantidade da forma de retirada e multiplicador devem ser maiores que zero.");
		}
		if (itemDTO.getTipoEmbalagemSolicitada() == TipoEmbalagem.UNITARIO
				&& itemDTO.getMultiplicadorSolicitado().compareTo(BigDecimal.ONE) != 0) {
			throw new BusinessRuleException("Retirada unitária deve usar multiplicador 1.");
		}
		BigDecimal esperado = BigDecimal.valueOf(itemDTO.getQuantidadeEmbalagensSolicitada())
				.multiply(itemDTO.getMultiplicadorSolicitado());
		if (esperado.compareTo(itemDTO.getQuantidadeSolicitada()) != 0) {
			throw new BusinessRuleException("Quantidade total inconsistente com a forma de retirada escolhida.");
		}
	}

	private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
		if (dataInicio == null || dataFim == null)
			throw new BusinessRuleException("Data inicial e data final são obrigatórias.");
		if (dataInicio.isAfter(dataFim))
			throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
	}

	private String normalizarTexto(String valor) {
		if (valor == null)
			return null;
		String normalizado = valor.trim();
		return normalizado.isEmpty() ? null : normalizado;
	}
}
