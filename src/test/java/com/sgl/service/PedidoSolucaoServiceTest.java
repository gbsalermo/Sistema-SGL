package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.*;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.*;
import com.sgl.model.enums.*;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class PedidoSolucaoServiceTest {
    static final UUID UNIDADE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID LAB = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PRODUTO = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID PROJETO = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final UUID PEDIDO = UUID.fromString("00000000-0000-0000-0000-000000000006");
    static final UUID ITEM = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock PedidoRepository pedidoRepository;
    @Mock EstoqueCentralRepository estoqueCentralRepository;
    @Mock HistoricoLaboratorioRepository historicoLaboratorioRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock LaboratorioRepository laboratorioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProjetoRepository projetoRepository;
    @Mock MovimentacaoEstoqueService movimentacaoEstoqueService;
    @Mock ModeloSolucaoRepository modeloSolucaoRepository;
    @InjectMocks PedidoService service;

    Unidade unidade;
    Usuario usuario;
    Laboratorio lab;
    Projeto projeto;
    Produto produto;
    EstoqueCentral estoque;
    Pedido pedido;
    ItemPedido item;

    @BeforeEach
    void preparar() {
        TenantContext.definir(UNIDADE);
        unidade = Unidade.builder().id(1L).publicId(UNIDADE).nome("Unidade").sigla("UNI").build();
        lab = Laboratorio.builder().id(2L).publicId(LAB).unidade(unidade).nome("Laboratório")
            .ativo(true).build();
        usuario = new Usuario();
        usuario.setId(3L);
        usuario.setPublicId(USUARIO);
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(lab);
        usuario.setNome("Usuário");
        usuario.setAtivo(true);
        projeto = Projeto.builder().id(5L).publicId(PROJETO).laboratorio(lab)
            .nome("Projeto").ativo(true).build();
        produto = Produto.builder().id(4L).publicId(PRODUTO).nome("Água")
            .unidadeMedida(UnidadeMedida.L).ativo(true).build();
        estoque = EstoqueCentral.builder().id(8L).unidade(unidade).produto(produto)
            .quantidadeAtual(new BigDecimal("2")).ativo(true).build();

        pedido = Pedido.builder().id(9L).publicId(PEDIDO).tipo(TipoPedido.SOLUCAO)
            .nomeSolucao("Solução A").usuario(usuario).laboratorio(lab).projeto(projeto)
            .status(StatusPedido.PENDENTE).itens(new ArrayList<>()).build();
        item = ItemPedido.builder().id(10L).publicId(ITEM).pedido(pedido).produto(produto)
            .quantidadeSolicitada(new BigDecimal("0.010"))
            .unidadeMedidaSolicitada(UnidadeMedida.ML).build();
        pedido.getItens().add(item);
    }
    @AfterEach void limpar() { TenantContext.limpar(); }

    private void stubPedido() {
        when(pedidoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PEDIDO, UNIDADE))
            .thenReturn(Optional.of(pedido));
        when(pedidoRepository.buscarPorIdComBloqueio(9L)).thenReturn(Optional.of(pedido));
    }
    private void stubSalvar() {
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }
    private void stubCriacao() {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LAB)).thenReturn(Optional.of(lab));
        when(projetoRepository.findByPublicId(PROJETO)).thenReturn(Optional.of(projeto));
        when(produtoRepository.findByPublicId(PRODUTO)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO, UNIDADE)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 4L))
            .thenReturn(Optional.of(estoque));
        stubSalvar();
    }

    @Test
    void pedidoPersonalizadoConverteParaUnidadeCanonica() {
        stubCriacao();
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setUsuarioId(USUARIO);
        dto.setLaboratorioId(LAB);
        dto.setProjetoId(PROJETO);
        dto.setNomeSolucao("Minha solução");
        ItemPedidoRequestDTO i = new ItemPedidoRequestDTO();
        i.setProdutoId(PRODUTO);
        i.setQuantidadeSolicitada(new BigDecimal("10"));
        i.setUnidadeMedidaSolicitada(UnidadeMedida.ML);
        dto.setItens(List.of(i));
        var resultado = service.criar(dto);
        assertEquals(TipoPedido.SOLUCAO, resultado.getTipo());
        assertEquals(StatusPedido.PENDENTE, resultado.getStatus());
        assertEquals("Minha solução", resultado.getNomeSolucao());
        assertEquals(0, resultado.getItens().get(0).getQuantidadeSolicitada()
            .compareTo(new BigDecimal("0.010")));
        assertEquals(UnidadeMedida.ML, resultado.getItens().get(0).getUnidadeMedidaSolicitada());
    }

    @Test
    void aprovaSolucaoComBaixaUnicaSemExigirEmbalagem() {
        stubPedido();
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 4L))
            .thenReturn(Optional.of(estoque));
        stubSalvar();
        var dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(USUARIO);
        dto.setItens(List.of(new AprovarPedidoRequestDTO.ItemAprovacaoDTO(
            ITEM, new BigDecimal("0.010"))));
        var resposta = service.aprovar(PEDIDO, dto);
        assertEquals(StatusPedido.EM_PREPARACAO, resposta.getStatus());
        verify(movimentacaoEstoqueService).registrarSaida(eq(8L),
            eq(new BigDecimal("0.010")), eq(usuario), eq(OrigemMovimentacao.PEDIDO),
            eq(pedido), eq(lab), isNull(), isNull(), isNull());
    }

    @Test
    void proibeAlterarQuantidadeDaReceitaNaAprovacao() {
        stubPedido();
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        var dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(USUARIO);
        dto.setItens(List.of(new AprovarPedidoRequestDTO.ItemAprovacaoDTO(
            ITEM, new BigDecimal("0.005"))));
        assertThrows(BusinessRuleException.class, () -> service.aprovar(PEDIDO, dto));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void cancelarSolucaoNaoPreparadaReverteEstoque() {
        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        stubPedido();
        stubSalvar();
        var r = service.cancelarSolucao(PEDIDO, false, "Desistência");
        assertEquals(StatusPedido.CANCELADO, r.getStatus());
        assertEquals(false, r.getSolucaoPreparadaNoCancelamento());
        assertEquals(true, r.getEstoqueRevertidoNoCancelamento());
        verify(movimentacaoEstoqueService).devolverSaidasDoPedido(pedido, null, "Desistência");
    }

    @Test
    void cancelarSolucaoPreparadaMantemBaixa() {
        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        stubPedido();
        stubSalvar();
        var r = service.cancelarSolucao(PEDIDO, true, "Cancelou após preparo");
        assertEquals(StatusPedido.CANCELADO, r.getStatus());
        assertEquals(true, r.getSolucaoPreparadaNoCancelamento());
        assertEquals(false, r.getEstoqueRevertidoNoCancelamento());
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void exigeConfirmacaoParaCancelarSolucaoEmPreparo() {
        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        stubPedido();
        assertThrows(BusinessRuleException.class, () -> service.cancelarSolucao(PEDIDO, null, "Cancelamento"));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void entregaNaoEfetuaSegundaBaixa() {
        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        item.setQuantidadeAprovada(new BigDecimal("0.010"));
        stubPedido();
        stubSalvar();
        var r = service.entregar(PEDIDO);
        assertEquals(StatusPedido.ENTREGUE, r.getStatus());
        verify(historicoLaboratorioRepository).save(any(HistoricoLaboratorio.class));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void bloqueiaCancelamentoAntigoSemConfirmacaoDePreparo() {
        stubPedido();
        assertThrows(BusinessRuleException.class, () -> service.cancelar(PEDIDO, "Antigo endpoint"));
        verifyNoInteractions(movimentacaoEstoqueService);
    }
}
