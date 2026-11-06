package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.LinkedHashSet;
import java.util.Set;
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
import com.sgl.security.SessaoDeTeste;

@ExtendWith(MockitoExtension.class)
class PedidoSolucaoServiceTest {
    static final UUID UNIDADE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID LAB = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID PRODUTO = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID PROJETO = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final UUID PEDIDO = UUID.fromString("00000000-0000-0000-0000-000000000006");
    static final UUID ITEM = UUID.fromString("00000000-0000-0000-0000-000000000007");
    static final UUID LAB_SEGUNDO = UUID.fromString("00000000-0000-0000-0000-000000000008");
    static final UUID LAB_FORA = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Mock PedidoRepository pedidoRepository;
    @Mock EstoqueCentralRepository estoqueCentralRepository;
    @Mock HistoricoLaboratorioRepository historicoLaboratorioRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock LaboratorioRepository laboratorioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProjetoRepository projetoRepository;
    @Mock MovimentacaoEstoqueService movimentacaoEstoqueService;
    @Mock ModeloSolucaoRepository modeloSolucaoRepository;
    @Mock VinculoOperacionalUsuarioService vinculoOperacionalService;
    @InjectMocks PedidoService service;

    Unidade unidade;
    Usuario usuario;
    Laboratorio lab;
    Projeto projeto;
    Produto produto;
    EstoqueCentral estoque;
    Pedido pedido;
    ItemPedido item;
    // Client-supplied acting users must match the session user (CurrentUser.exigirProprioUsuario).
    @BeforeEach
    void sessaoDoOperador() {
        SessaoDeTeste.entrar(USUARIO, Perfil.TECNICO);
    }

    @AfterEach
    void encerrarSessao() {
        SessaoDeTeste.sair();
    }


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
        usuario.substituirLaboratorios(List.of(lab));
        usuario.setNome("Usuário");
        usuario.setAtivo(true);
        projeto = Projeto.builder().id(5L).publicId(PROJETO)
                .unidade(lab.getUnidade())
                .laboratorios(new LinkedHashSet<>(Set.of(lab)))
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
    void solucaoComProjetoDeDoisLaboratoriosExigeLaboratorio() {
        adicionarSegundoLaboratorioAoProjeto();
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(projetoRepository.findByPublicId(PROJETO)).thenReturn(Optional.of(projeto));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
            () -> service.criar(solucaoDoProjeto(null)));

        assertEquals("Informe o laboratório: o projeto está em mais de um laboratório.", erro.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void solucaoComLaboratorioForaDoProjetoEhRecusada() {
        adicionarSegundoLaboratorioAoProjeto();
        Laboratorio foraDoProjeto = Laboratorio.builder().id(13L).publicId(LAB_FORA).unidade(unidade)
            .nome("Laboratório fora do projeto").ativo(true).build();
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(projetoRepository.findByPublicId(PROJETO)).thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicId(LAB_FORA)).thenReturn(Optional.of(foraDoProjeto));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
            () -> service.criar(solucaoDoProjeto(LAB_FORA)));

        assertEquals("O Projeto não pertence ao Laboratório informado.", erro.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void solucaoComLaboratorioDoProjetoEhAceita() {
        Laboratorio segundo = adicionarSegundoLaboratorioAoProjeto();
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(projetoRepository.findByPublicId(PROJETO)).thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicId(LAB_SEGUNDO)).thenReturn(Optional.of(segundo));
        // The requester's base laboratory is the first one; an operational link authorizes the second.
        when(vinculoOperacionalService.vinculadoLaboratorio(eq(usuario), eq(segundo), any())).thenReturn(true);
        when(produtoRepository.findByPublicId(PRODUTO)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO, UNIDADE)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 4L))
            .thenReturn(Optional.of(estoque));
        stubSalvar();

        var resultado = service.criar(solucaoDoProjeto(LAB_SEGUNDO));

        assertEquals(LAB_SEGUNDO, resultado.getLaboratorioId());
        assertEquals(PROJETO, resultado.getProjetoId());
        assertEquals(TipoPedido.SOLUCAO, resultado.getTipo());
    }

    private Laboratorio adicionarSegundoLaboratorioAoProjeto() {
        Laboratorio segundo = Laboratorio.builder().id(12L).publicId(LAB_SEGUNDO).unidade(unidade)
            .nome("Segundo laboratório").ativo(true).build();
        projeto.getLaboratorios().add(segundo);
        return segundo;
    }

    private PedidoRequestDTO solucaoDoProjeto(UUID laboratorioId) {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setUsuarioId(USUARIO);
        dto.setProjetoId(PROJETO);
        dto.setLaboratorioId(laboratorioId);
        dto.setNomeSolucao("Solução do projeto");
        ItemPedidoRequestDTO componente = new ItemPedidoRequestDTO();
        componente.setProdutoId(PRODUTO);
        componente.setQuantidadeSolicitada(new BigDecimal("10"));
        componente.setUnidadeMedidaSolicitada(UnidadeMedida.ML);
        dto.setItens(List.of(componente));
        return dto;
    }

    @Test
    void permiteSolucaoSemProjetoQuandoLaboratorioEstaAutorizado() {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(Optional.of(usuario));
        when(laboratorioRepository.findByPublicId(LAB)).thenReturn(Optional.of(lab));
        when(produtoRepository.findByPublicId(PRODUTO)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO, UNIDADE)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 4L))
            .thenReturn(Optional.of(estoque));
        stubSalvar();

        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setUsuarioId(USUARIO);
        dto.setLaboratorioId(LAB);
        dto.setNomeSolucao("Solução sem projeto");
        ItemPedidoRequestDTO componente = new ItemPedidoRequestDTO();
        componente.setProdutoId(PRODUTO);
        componente.setQuantidadeSolicitada(new BigDecimal("10"));
        componente.setUnidadeMedidaSolicitada(UnidadeMedida.ML);
        dto.setItens(List.of(componente));

        var resposta = service.criar(dto);
        assertEquals(TipoPedido.SOLUCAO, resposta.getTipo());
        assertEquals(StatusPedido.PENDENTE, resposta.getStatus());
        assertNull(resposta.getProjetoId());
        verify(projetoRepository, never()).findByPublicId(any());
    }

    @Test
    void aprovaSolucaoComBaixaUnicaSemExigirEmbalagem() {
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
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
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
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
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
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
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
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
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
        pedido.setStatus(StatusPedido.EM_PREPARACAO);
        stubPedido();
        assertThrows(BusinessRuleException.class, () -> service.cancelarSolucao(PEDIDO, null, "Cancelamento"));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void entregaNaoEfetuaSegundaBaixa() {
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
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
        SessaoDeTeste.entrar(USUARIO, Perfil.GESTOR);
        stubPedido();
        assertThrows(BusinessRuleException.class, () -> service.cancelar(PEDIDO, "Antigo endpoint"));
        verifyNoInteractions(movimentacaoEstoqueService);
    }
}
