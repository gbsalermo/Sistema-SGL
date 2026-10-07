package com.sgl.service;

import java.math.BigDecimal;
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
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

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
		// Correção de segurança: o repositório já filtra por tenant nas
		// suas consultas (via "@tenantProvider.unidadeId"), mas só quando
		// existe um tenant ativo — a query em si permite tudo quando esse
		// valor é nulo. Exigir o tenant aqui garante que essa "porta aberta"
		// não seja alcançada por uma chamada sem o header de unidade.
		exigirTenantAtivo();
		return movimentacaoRepository.findAll().stream().map(MovimentacaoEstoqueResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public MovimentacaoEstoqueResponseDTO buscarPorId(UUID id) {
		// Correção de segurança (vazamento entre unidades): este método
		// usava "findByPublicId", uma busca SEM filtro de unidade, mesmo
		// já existindo no repositório o método correto
		// "findByPublicIdAndEstoqueCentralUnidadePublicId" (usado em outros
		// pontos do sistema). Ou seja, qualquer um podia ler os detalhes de
		// uma movimentação de estoque de OUTRA unidade só sabendo o id.
		exigirTenantAtivo();
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return movimentacaoRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(id, unidadeId)
				.map(MovimentacaoEstoqueResponseDTO::new)
				.orElseThrow(() -> new ResourceNotFoundException("Movimentação", id));
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorProduto(UUID produtoId) {
		exigirTenantAtivo();
		Produto produto = produtoRepository.findByPublicId(produtoId)
				.orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));
		return movimentacaoRepository.findByProdutoId(produto.getId()).stream().map(MovimentacaoEstoqueResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
		exigirTenantAtivo();
		Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
		return movimentacaoRepository.findByLaboratorioId(laboratorio.getId()).stream()
				.map(MovimentacaoEstoqueResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorUsuario(UUID usuarioId) {
		exigirTenantAtivo();
		Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
		return movimentacaoRepository.findByUsuarioId(usuario.getId()).stream().map(MovimentacaoEstoqueResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorPedido(UUID pedidoId) {
		exigirTenantAtivo();
		Pedido pedido = pedidoRepository.findByPublicId(pedidoId)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido", pedidoId));
		return movimentacaoRepository.findByPedidoId(pedido.getId()).stream().map(MovimentacaoEstoqueResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorLote(UUID loteId) {
		exigirTenantAtivo();
		Lote lote = loteRepository.findByPublicId(loteId)
				.orElseThrow(() -> new ResourceNotFoundException("Lote", loteId));
		return movimentacaoRepository.findByLoteIdOrderByDataMovimentacaoDesc(lote.getId()).stream()
				.map(MovimentacaoEstoqueResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<MovimentacaoEstoqueResponseDTO> listarPorTipo(TipoMovimentacao tipo) {
		exigirTenantAtivo();
		return movimentacaoRepository.findByTipoMovimentacao(tipo).stream().map(MovimentacaoEstoqueResponseDTO::new)
				.toList();
	}

	@Transactional
	public LoteResponseDTO registrarEntradaLote(UUID estoqueId, EntradaLoteRequestDTO dto, Usuario usuario) {
		validarUsuarioResponsavel(usuario);
		EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
		validarEntradaLote(estoque.getProduto(), dto);

		if (loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote())) {
			throw new BusinessRuleException("Já existe lote com esse número de fornecedor neste estoque.");
		}

		Produto produtoBloqueado = produtoRepository.buscarPorIdComBloqueio(estoque.getProduto().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Produto", estoque.getProduto().getId()));

		BigDecimal multiplicador = dto.getConteudoPorApresentacao() == null ? BigDecimal.ONE
				: dto.getConteudoPorApresentacao();

		BigDecimal quantidadeUnitaria = BigDecimal.valueOf(dto.getQuantidade()).multiply(multiplicador);

		BigDecimal quantidadeAnterior = estoque.getQuantidadeAtual();
		BigDecimal quantidadeAtual = quantidadeAnterior.add(quantidadeUnitaria);
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
				dto.getObservacao() == null || dto.getObservacao().isBlank() ? null : dto.getObservacao().trim());
		lote.setQuantidadeInicial(quantidadeUnitaria);
		lote.setQuantidadeDisponivel(quantidadeUnitaria);
		lote.setDataEntrada(LocalDate.now());
		lote.setDataValidade(dto.getDataValidade());
		lote.setAtivo(true);
		loteRepository.save(lote);

		estoque.setQuantidadeAtual(quantidadeAtual);
		estoqueCentralRepository.save(estoque);
		registrarMovimentacao(estoque, lote, usuario, null, null, TipoMovimentacao.ENTRADA, dto.getOrigem(),
				quantidadeUnitaria, quantidadeAnterior, quantidadeAtual, dto.getObservacao());
		return new LoteResponseDTO(lote);
	}

	@Transactional
	public List<MovimentacaoEstoqueResponseDTO> registrarSaida(Long estoqueId, BigDecimal quantidade, Usuario usuario,
			OrigemMovimentacao origem, Pedido pedido, Laboratorio laboratorio, String observacao) {
		return registrarSaida(estoqueId, quantidade, usuario, origem, pedido, laboratorio, observacao, null, null);
	}

	@Transactional
	public List<MovimentacaoEstoqueResponseDTO> registrarSaida(Long estoqueId, BigDecimal quantidade, Usuario usuario,
			OrigemMovimentacao origem, Pedido pedido, Laboratorio laboratorio, String observacao,
			TipoEmbalagem tipoEmbalagemSolicitada, Integer multiplicadorSolicitado) {

		validarQuantidade(quantidade);
		validarUsuarioResponsavel(usuario);
		EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
		List<Lote> lotes = buscarLotesParaSaida(estoque).stream().filter(
				lote -> loteCompativelComFormaSolicitada(lote, tipoEmbalagemSolicitada, multiplicadorSolicitado))
				.toList();

		BigDecimal saldoUtilizavel = lotes.stream().map(Lote::getQuantidadeDisponivel)
				.filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (saldoUtilizavel.compareTo(quantidade) < 0) {
			String forma = tipoEmbalagemSolicitada == null ? "selecionada" : tipoEmbalagemSolicitada.name();
			throw new BusinessRuleException("Estoque utilizável insuficiente para a forma de retirada " + forma
					+ ". Disponível nos lotes compatíveis: " + saldoUtilizavel + ", solicitado: " + quantidade);
		}

		List<MovimentacaoEstoqueResponseDTO> movimentacoes = new ArrayList<>();
		BigDecimal restante = quantidade;
		for (Lote lote : lotes) {
			if (restante.compareTo(BigDecimal.ZERO) == 0)
				break;
			BigDecimal consumido = calcularQuantidadeCompativel(lote, restante);

			if (consumido.compareTo(BigDecimal.ZERO) <= 0)
				continue;

			BigDecimal saldoAnterior = estoque.getQuantidadeAtual();
			BigDecimal saldoAtual = saldoAnterior.subtract(consumido);

			lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel().subtract(consumido));

			restante = restante.subtract(consumido);
		}

		if (restante.compareTo(BigDecimal.ZERO) > 0) {
			throw new BusinessRuleException(
					"A quantidade solicitada não pode ser atendida com a forma de retirada escolhida sem fracionar uma embalagem fechada.");
		}
		return movimentacoes;
	}

	@Transactional
	public List<MovimentacaoEstoqueResponseDTO> registrarDescarteVencimento(UUID estoqueId, BigDecimal quantidade,
			String justificativa, Usuario usuario) {
		validarQuantidade(quantidade);
		validarUsuarioResponsavel(usuario);
		EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);

		if (!Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
			throw new BusinessRuleException("Somente produtos perecíveis podem ser descartados por vencimento.");
		}

		List<Lote> lotesVencidos = loteRepository.buscarVencidosComBloqueio(estoque.getId(), LocalDate.now());
		BigDecimal saldoVencido = lotesVencidos.stream().map(Lote::getQuantidadeDisponivel)
				.filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (saldoVencido.compareTo(quantidade) < 0) {
			throw new BusinessRuleException(
					"Quantidade de descarte maior que o saldo vencido disponível. Disponível: " + saldoVencido);
		}

		List<MovimentacaoEstoqueResponseDTO> movimentacoes = new ArrayList<>();
		BigDecimal restante = quantidade;
		for (Lote lote : lotesVencidos) {
			if (restante.compareTo(BigDecimal.ZERO) == 0)
				break;
			BigDecimal descartado = calcularQuantidadeCompativel(lote, restante);
			if (descartado.compareTo(BigDecimal.ZERO) <= 0)
				continue;

			BigDecimal saldoAnterior = estoque.getQuantidadeAtual();

			BigDecimal saldoAtual = saldoAnterior.subtract(descartado);

			lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel().subtract(descartado));

			estoque.setQuantidadeAtual(saldoAtual);

			loteRepository.save(lote);
			estoqueCentralRepository.save(estoque);

			MovimentacaoEstoque movimentacao = registrarMovimentacao(estoque, lote, usuario, null, null,
					TipoMovimentacao.DESCARTE_VENCIMENTO, OrigemMovimentacao.DESCARTE, descartado, saldoAnterior,
					saldoAtual, justificativa);

			movimentacoes.add(new MovimentacaoEstoqueResponseDTO(movimentacao));

			restante = restante.subtract(descartado);
		}

		if (restante.compareTo(BigDecimal.ZERO) > 0) {
			throw new BusinessRuleException(
					"A quantidade informada não pode ser descartada sem fracionar uma embalagem fechada. Informe uma quantidade compatível com os lotes vencidos.");
		}
		return movimentacoes;
	}

	@Transactional
	public void devolverSaidasDoPedido(Pedido pedido, Usuario usuarioResponsavel, String observacao) {
		List<MovimentacaoEstoque> saidas = movimentacaoRepository
				.findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(pedido.getId(), TipoMovimentacao.SAIDA).stream()
				.filter(m -> m.getLote() != null)
				.sorted(Comparator.comparing((MovimentacaoEstoque m) -> m.getEstoqueCentral().getId())
						.thenComparing(m -> m.getLote().getId()))
				.toList();

		if (saidas.isEmpty())
			throw new BusinessRuleException("Não foram encontradas saídas por lote para devolver este pedido.");

		for (MovimentacaoEstoque saida : saidas) {
			EstoqueCentral estoque = estoqueCentralRepository.buscarPorIdComBloqueio(saida.getEstoqueCentral().getId())
					.orElseThrow(
							() -> new ResourceNotFoundException("Estoque central", saida.getEstoqueCentral().getId()));
			Lote lote = loteRepository.buscarPorIdComBloqueio(saida.getLote().getId())
					.orElseThrow(() -> new ResourceNotFoundException("Lote", saida.getLote().getId()));

			BigDecimal quantidade = saida.getQuantidadeMovimentada();
			BigDecimal saldoAnterior = estoque.getQuantidadeAtual();
			BigDecimal saldoAtual = saldoAnterior.add(quantidade);
			if (lote.getQuantidadeDisponivel().add(quantidade).compareTo(lote.getQuantidadeInicial()) > 0) {
				throw new BusinessRuleException(
						"A devolução ultrapassaria a quantidade inicial do lote " + lote.getCodigoInterno() + ".");
			}

			lote.setQuantidadeDisponivel(lote.getQuantidadeDisponivel().add(quantidade));
			lote.setAtivo(true);
			estoque.setQuantidadeAtual(saldoAtual);
			loteRepository.save(lote);
			estoqueCentralRepository.save(estoque);

			if (usuarioResponsavel != null) {
				validarUsuarioResponsavel(usuarioResponsavel);
				registrarMovimentacao(estoque, lote, usuarioResponsavel, pedido, pedido.getLaboratorio(),
						TipoMovimentacao.DEVOLUCAO, OrigemMovimentacao.DEVOLUCAO, quantidade, saldoAnterior, saldoAtual,
						observacao);
			}
		}
	}

	@Transactional
	public List<LoteResponseDTO> listarLotesOrdenadosParaSaida(Long estoqueId) {
		EstoqueCentral estoque = buscarEstoqueAtivoComBloqueio(estoqueId);
		return buscarLotesParaSaida(estoque).stream().map(LoteResponseDTO::new).toList();
	}

	private boolean loteCompativelComFormaSolicitada(Lote lote, TipoEmbalagem tipo, BigDecimal multiplicador) {
		if (tipo == null)
			return true;
		if (tipo == TipoEmbalagem.UNITARIO) {
			return lote.getTipoEmbalagem() == TipoEmbalagem.UNITARIO || lote.permiteFracionamento();
		}
		BigDecimal fator = multiplicador == null || multiplicador.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE
				: multiplicador;

		return lote.getTipoEmbalagem() == tipo && lote.fatorApresentacao().compareTo(fator) == 0;
	}

	private BigDecimal calcularQuantidadeCompativel(Lote lote, BigDecimal restante) {

		BigDecimal disponivel = lote.getQuantidadeDisponivel() == null ? BigDecimal.ZERO
				: lote.getQuantidadeDisponivel().max(BigDecimal.ZERO);

		BigDecimal limite = restante.min(disponivel);

		if (lote.permiteFracionamento()) {
			return limite;
		}

		BigDecimal multiplicador = lote.fatorApresentacao();

		return limite.divideToIntegralValue(multiplicador).multiply(multiplicador);
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
		if (origem == null || origem.isBlank())
			origem = "PRD-" + produto.getId();
		String sigla = origem.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
		return sigla.isBlank() ? "PRD-" + produto.getId() : sigla;
	}

	private String formatarCodigoLote(String sigla, int sequencial) {
		return "LOT-" + sigla + "-" + String.format(Locale.ROOT, "%03d", sequencial);
	}

	private record CodigoLoteGerado(String codigo, int sequencial) {
	}

	private List<Lote> buscarLotesParaSaida(EstoqueCentral estoque) {
		if (Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
			return loteRepository.buscarDisponiveisPorFefoComBloqueio(estoque.getId(), LocalDate.now());
		}
		return loteRepository.buscarDisponiveisPorEntradaComBloqueio(estoque.getId());
	}

	private EstoqueCentral buscarEstoqueAtivoComBloqueio(UUID estoquePublicId) {
		EstoqueCentral referencia = estoqueCentralRepository.findByPublicId(estoquePublicId)
				.orElseThrow(() -> new ResourceNotFoundException("Estoque central", estoquePublicId));
		return buscarEstoqueAtivoComBloqueio(referencia.getId());
	}

	private EstoqueCentral buscarEstoqueAtivoComBloqueio(Long estoqueId) {
		EstoqueCentral estoque = estoqueCentralRepository.buscarPorIdComBloqueio(estoqueId)
				.orElseThrow(() -> new ResourceNotFoundException("Estoque central", estoqueId));
		estoque.validateActive();
		return estoque;
	}

	private MovimentacaoEstoque registrarMovimentacao(EstoqueCentral estoque, Lote lote, Usuario usuario, Pedido pedido,
			Laboratorio laboratorio, TipoMovimentacao tipo, OrigemMovimentacao origem, BigDecimal quantidade,
			BigDecimal quantidadeAnterior, BigDecimal quantidadeAtual, String observacao) {
		MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder().produto(estoque.getProduto())
				.estoqueCentral(estoque).lote(lote).usuario(usuario).pedido(pedido).laboratorio(laboratorio)
				.tipoMovimentacao(tipo).origem(origem).quantidadeMovimentada(quantidade)
				.quantidadeAnterior(quantidadeAnterior).quantidadeAtual(quantidadeAtual)
				.dataMovimentacao(LocalDateTime.now()).observacao(observacao).build();
		return movimentacaoRepository.save(movimentacao);
	}

	private void validarEntradaLote(Produto produto, EntradaLoteRequestDTO dto) {
		if (dto.getQuantidade() == null || dto.getQuantidade() <= 0) {
		    throw new BusinessRuleException(
		            "A quantidade de apresentações deve ser maior que zero."
		    );
		}
		if (dto.getTipoEmbalagem() == null)
			throw new BusinessRuleException("Tipo de embalagem é obrigatório.");
		if (dto.getConteudoPorApresentacao() != null
				&& dto.getConteudoPorApresentacao().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Multiplicador deve ser maior que zero.");
		}
		if (dto.getOrigem() == null)
			throw new BusinessRuleException("Origem da entrada é obrigatória.");
		produto.validateLotExpirationDate(dto.getDataValidade());
		if (dto.getDataValidade() != null && dto.getDataValidade().isBefore(LocalDate.now())) {
			throw new BusinessRuleException("Não é possível registrar entrada de lote já vencido.");
		}
	}

	private String normalizarApresentacao(Produto produto, String apresentacao) {
		if (apresentacao != null && !apresentacao.isBlank())
			return apresentacao.trim();
		if (produto.getUnidadeArmazenamento() != null && !produto.getUnidadeArmazenamento().isBlank())
			return produto.getUnidadeArmazenamento().trim();
		return produto.getUnidadeMedida().name();
	}

	private void validarUsuarioResponsavel(Usuario usuario) {
		if (usuario == null)
			throw new BusinessRuleException("Usuário responsável é obrigatório.");
		usuario.validateActive();
	}

	private void validarQuantidade(BigDecimal quantidade) {
	    if (quantidade == null
	            || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
	        throw new BusinessRuleException(
	                "A quantidade deve ser maior que zero."
	        );
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
}
