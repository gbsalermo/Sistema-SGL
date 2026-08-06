package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.AprovarPedidoDTO;
import com.sgl.dto.PedidoDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
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

/**
 * Testes unitários das regras de aprovação de pedidos.
 *
 * <p>O PedidoService é executado de verdade, porém todos os repositories são
 * mocks. Portanto, estes testes não iniciam o Spring, não executam o
 * DataInitializer e não acessam banco de dados.</p>
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    /* Dependências falsas utilizadas pelo PedidoService durante os testes. */
    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private HistoricoLaboratorioRepository historicoLaboratorioRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    /* Service real recebendo os repositories falsos acima. */
    @InjectMocks
    private PedidoService pedidoService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario solicitante;
    private Usuario aprovador;
    private Produto produto;
    private EstoqueCentral estoque;
    private ItemPedido item;
    private Pedido pedido;

    /**
     * Cria um cenário novo antes de cada teste.
     *
     * <p>O pedido começa PENDENTE, solicita cinco unidades e o estoque possui
     * dez. O aprovador está ativo e pertence à mesma unidade.</p>
     */
    @BeforeEach
    void prepararCenario() {

        unidade = Unidade.builder()
                .id(1L)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .nome("Laboratório de Química")
                .unidade(unidade)
                .ativo(true)
                .build();

        solicitante = new Usuario();
        solicitante.setId(3L);
        solicitante.setNome("Usuário Solicitante");
        solicitante.setUnidade(unidade);
        solicitante.setLaboratorio(laboratorio);
        solicitante.setAtivo(true);

        aprovador = new Usuario();
        aprovador.setId(4L);
        aprovador.setNome("Usuário Aprovador");
        aprovador.setUnidade(unidade);
        aprovador.setAtivo(true);

        produto = Produto.builder()
                .id(5L)
                .nome("Álcool 70%")
                .perecivel(false)
                .unidadeArmazenamento("Frasco de 1 L")
                .ativo(true)
                .build();

        estoque = EstoqueCentral.builder()
                .id(6L)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(7L)
                .usuario(solicitante)
                .laboratorio(laboratorio)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .itens(new java.util.ArrayList<>())
                .build();

        item = ItemPedido.builder()
                .id(8L)
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(5)
                .build();

        pedido.getItens().add(item);
    }

    /**
     * Cenário feliz de aprovação parcial.
     *
     * <p>O pedido solicita cinco unidades, mas apenas três são aprovadas. O
     * saldo deve cair de dez para sete, o item deve guardar a quantidade três,
     * o pedido deve se tornar APROVADO e uma movimentação SAIDA/PEDIDO deve ser
     * criada.</p>
     */
    @Test
    void deveAprovarParcialmentePedidoEReduzirEstoque() {

        // ARRANGE: cria o DTO aprovando três das cinco unidades solicitadas.
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L))
                .thenReturn(Optional.of(aprovador));

        when(pedidoRepository.findById(7L))
                .thenReturn(Optional.of(pedido));

        when(estoqueCentralRepository
                .buscarPorUnidadeEProdutoComBloqueio(1L, 5L))
                .thenReturn(Optional.of(estoque));

        /*
         * Como PedidoDTO é criado com o retorno do save, fazemos o mock devolver
         * o mesmo Pedido que recebeu. Isso simula o comportamento básico do JPA.
         */
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        // ACT: executa a aprovação real do Service.
        PedidoDTO resultado = pedidoService.aprovar(7L, dto);

        // ASSERT: verifica as alterações principais do domínio.
        assertEquals(StatusPedido.APROVADO, pedido.getStatus());
        assertEquals(StatusPedido.APROVADO, resultado.getStatus());
        assertEquals(3, item.getQuantidadeAprovada());
        assertEquals(7, estoque.getQuantidadeAtual());
        assertEquals("Aprovação parcial para teste", pedido.getObservacao());

        verify(estoqueCentralRepository).save(estoque);
        verify(pedidoRepository).save(pedido);

        // Captura a movimentação criada internamente para verificar seus dados.
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque movimentacao = captor.getValue();

        assertEquals(TipoMovimentacao.SAIDA, movimentacao.getTipoMovimentacao());
        assertEquals(OrigemMovimentacao.PEDIDO, movimentacao.getOrigem());
        assertEquals(3, movimentacao.getQuantidadeMovimentada());
        assertEquals(10, movimentacao.getQuantidadeAnterior());
        assertEquals(7, movimentacao.getQuantidadeAtual());
        assertEquals(aprovador, movimentacao.getUsuario());
        assertEquals(pedido, movimentacao.getPedido());
        assertEquals(laboratorio, movimentacao.getLaboratorio());
        assertEquals(estoque, movimentacao.getEstoqueCentral());
    }

    /**
     * Apenas pedidos PENDENTES podem ser aprovados.
     *
     * <p>O teste muda o pedido para APROVADO antes da chamada e confirma que o
     * fluxo para antes de consultar estoque ou realizar qualquer persistência.</p>
     */
    @Test
    void deveImpedirAprovacaoQuandoPedidoNaoEstiverPendente() {

        pedido.setStatus(StatusPedido.APROVADO);
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L))
                .thenReturn(Optional.of(aprovador));

        when(pedidoRepository.findById(7L))
                .thenReturn(Optional.of(pedido));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Apenas pedidos PENDENTES podem ser aprovados. Status atual: APROVADO",
                exception.getMessage()
        );

        assertEquals(10, estoque.getQuantidadeAtual());
        verifyNoInteractions(estoqueCentralRepository, movimentacaoEstoqueRepository);
        verify(pedidoRepository, never()).save(any());
    }

    /**
     * A quantidade aprovada não pode ultrapassar a solicitada.
     *
     * <p>Como o item solicita cinco e o DTO tenta aprovar seis, a validação deve
     * ocorrer antes da busca pelo estoque.</p>
     */
    @Test
    void deveImpedirQuantidadeAprovadaMaiorQueSolicitada() {

        AprovarPedidoDTO dto = criarAprovacaoDTO(6);

        when(usuarioRepository.findById(4L))
                .thenReturn(Optional.of(aprovador));

        when(pedidoRepository.findById(7L))
                .thenReturn(Optional.of(pedido));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Quantidade aprovada deve ser maior que zero e não pode ser maior "
                        + "que a solicitada. Solicitada: 5, aprovada: 6",
                exception.getMessage()
        );

        assertEquals(null, item.getQuantidadeAprovada());
        assertEquals(10, estoque.getQuantidadeAtual());
        verifyNoInteractions(estoqueCentralRepository, movimentacaoEstoqueRepository);
        verify(pedidoRepository, never()).save(any());
    }

    /**
     * A aprovação deve ser bloqueada quando não houver saldo suficiente.
     *
     * <p>Nesse cenário o estoque tem apenas duas unidades e o DTO tenta aprovar
     * três. Nenhum save ou registro de movimentação pode acontecer.</p>
     */
    @Test
    void deveImpedirAprovacaoQuandoEstoqueForInsuficiente() {

        estoque.setQuantidadeAtual(2);
        AprovarPedidoDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findById(4L))
                .thenReturn(Optional.of(aprovador));

        when(pedidoRepository.findById(7L))
                .thenReturn(Optional.of(pedido));

        when(estoqueCentralRepository
                .buscarPorUnidadeEProdutoComBloqueio(1L, 5L))
                .thenReturn(Optional.of(estoque));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals(
                "Estoque insuficiente para o produto: Álcool 70%. Disponível: 2, solicitado: 3",
                exception.getMessage()
        );

        assertEquals(2, estoque.getQuantidadeAtual());
        assertEquals(null, item.getQuantidadeAprovada());
        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    /**
     * O aprovador é obrigatório na implementação atual.
     *
     * <p>A validação ocorre logo no início do método, portanto nenhum repository
     * deve ser consultado quando o ID estiver ausente.</p>
     */
    @Test
    void deveExigirUsuarioAprovador() {

        AprovarPedidoDTO dto = criarAprovacaoDTO(3);
        dto.setUsuarioAprovadorId(null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(7L, dto)
        );

        assertEquals("O usuário aprovador é obrigatório.", exception.getMessage());

        verifyNoInteractions(
                usuarioRepository,
                pedidoRepository,
                estoqueCentralRepository,
                movimentacaoEstoqueRepository
        );
    }

    /** Cria um DTO de aprovação para o único item existente no cenário. */
    private AprovarPedidoDTO criarAprovacaoDTO(Integer quantidadeAprovada) {

        AprovarPedidoDTO.ItemAprovacaoDTO itemDTO =
                new AprovarPedidoDTO.ItemAprovacaoDTO(8L, quantidadeAprovada);

        AprovarPedidoDTO dto = new AprovarPedidoDTO();
        dto.setUsuarioAprovadorId(4L);
        dto.setObservacao("Aprovação parcial para teste");
        dto.setAutorizarProdutoVencido(false);
        dto.setItens(List.of(itemDTO));

        return dto;
    }
}
