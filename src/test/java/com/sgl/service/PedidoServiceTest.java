package com.sgl.service;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.request.ItemPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.model.enums.TipoPedido;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;
import com.sgl.security.SessaoDeTeste;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SOLICITANTE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID APROVADOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ESTOQUE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID PEDIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID ITEM_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID SEGUNDO_LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

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
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Mock
    private VinculoOperacionalUsuarioService vinculoOperacionalService;

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
    // Client-supplied acting users must match the session user (CurrentUser.exigirProprioUsuario).
    @BeforeEach
    void sessaoDoOperador() {
        SessaoDeTeste.entrar(SOLICITANTE_PUBLIC_ID, Perfil.TECNICO);
    }

    @AfterEach
    void encerrarSessao() {
        SessaoDeTeste.sair();
    }


    @BeforeEach
    void prepararCenario() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_PUBLIC_ID)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .nome("Laboratório de Química")
                .unidade(unidade)
                .ativo(true)
                .build();

        solicitante = new Usuario();
        solicitante.setId(3L);
        solicitante.setPublicId(SOLICITANTE_PUBLIC_ID);
        solicitante.setNome("Solicitante");
        solicitante.setUnidade(unidade);
        solicitante.substituirLaboratorios(List.of(laboratorio));
        solicitante.setAtivo(true);

        aprovador = new Usuario();
        aprovador.setId(4L);
        aprovador.setPublicId(APROVADOR_PUBLIC_ID);
        aprovador.setNome("Aprovador");
        aprovador.setUnidade(unidade);
        aprovador.setAtivo(true);

        produto = Produto.builder()
                .id(5L)
                .publicId(PRODUTO_PUBLIC_ID)
                .nome("Álcool 70%")
                .perecivel(false)
                .ativo(true)
                .build();

        estoque = EstoqueCentral.builder()
                .id(6L)
                .publicId(ESTOQUE_PUBLIC_ID)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(BigDecimal.valueOf(10))
                .quantidadeMinima(BigDecimal.valueOf(2))
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(7L)
                .publicId(PEDIDO_PUBLIC_ID)
                .usuario(solicitante)
                .laboratorio(laboratorio)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .itens(new ArrayList<>())
                .build();

        item = ItemPedido.builder()
                .id(8L)
                .publicId(ITEM_PUBLIC_ID)
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(BigDecimal.valueOf(5))
                // Sem isso, tipoEmbalagemSolicitada fica nulo (!= UNITARIO),
                // e PedidoService.aprovar() tenta validar o multiplicador de
                // embalagem do item — que também não foi definido aqui. Os
                // testes deste arquivo não testam essa regra de embalagem,
                // então marcamos como UNITARIO para não cair nela.
                .tipoEmbalagemSolicitada(TipoEmbalagem.UNITARIO)
                .build();
        pedido.getItens().add(item);

        // A partir da correção de segurança em TenantContext.pertence()
        // (fail-closed), os métodos do service passaram a exigir que exista
        // um tenant (unidade) ativo — como aconteceria numa requisição HTTP
        // real, onde o TenantRequestFilter já define isso antes de chamar o
        // service. Aqui simulamos a mesma coisa manualmente.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Test
    void deveRejeitarPedidoEmNomeDeOutroUsuario() {
        // The requester comes from the session; a body naming someone else is refused.
        SessaoDeTeste.entrar(UUID.randomUUID(), Perfil.TECNICO);
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(SOLICITANTE_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);

        assertThrows(AccessDeniedException.class, () -> pedidoService.criar(dto));
    }

    @Test
    void deveAprovarPedidoDelegandoSaidaAoMovimentacaoEstoqueService() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto);

        assertEquals(StatusPedido.APROVADO, resultado.getStatus());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.getId());
        assertEquals(BigDecimal.valueOf(3), item.getQuantidadeAprovada());
        assertEquals("Aprovação parcial", pedido.getObservacao());

        // PedidoService.aprovar() sempre chama a sobrecarga de 9 argumentos
        // de registrarSaida (inclui tipo de embalagem e multiplicador do
        // item aprovado). Este teste estava verificando a sobrecarga antiga
        // de 7 argumentos, que não existe mais nesse ponto do código — o
        // Mockito não reconhecia a chamada real e o teste passava só porque
        // uma NullPointerException (bug corrigido em outro ponto) interrompia
        // o método antes de chegar aqui.
        verify(movimentacaoEstoqueService).registrarSaida(
                6L,
                BigDecimal.valueOf(3),
                aprovador,
                OrigemMovimentacao.PEDIDO,
                pedido,
                laboratorio,
                "Aprovação parcial",
                TipoEmbalagem.UNITARIO,
                null
        );
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveAprovarQuantidadeFisicaDecimalSemExigirMultiploDeEmbalagem() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        produto.setUnidadeMedida(UnidadeMedida.ML);
        pedido.setTipo(TipoPedido.PRODUTOS);
        item.setUnidadeMedidaSolicitada(UnidadeMedida.ML);
        item.setTipoEmbalagemSolicitada(null);
        item.setQuantidadeEmbalagensSolicitada(null);
        item.setMultiplicadorSolicitado(null);
        item.setQuantidadeSolicitada(new BigDecimal("0.700"));

        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(1);
        dto.getItens().get(0).setQuantidadeAprovada(new BigDecimal("0.200"));

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto);

        assertEquals(StatusPedido.APROVADO, resultado.getStatus());
        assertEquals(0, new BigDecimal("0.200").compareTo(item.getQuantidadeAprovada()));
        verify(movimentacaoEstoqueService).registrarSaida(
                6L, new BigDecimal("0.200"), aprovador,
                OrigemMovimentacao.PEDIDO, pedido, laboratorio, "Aprovação parcial",
                null, null);
    }

    @Test
    void administradorGeral_passaNaChecagemDeLaboratorioComoGestor() {
        aprovador.setPerfil(Perfil.ADMINISTRADOR_GERAL);
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(APROVADOR_PUBLIC_ID);
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, aprovador.getPerfil());
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(aprovador));
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));

        // Passou da checagem de vínculo com o laboratório; falha só por não haver itens.
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> pedidoService.criar(dto));

        assertEquals("O pedido precisa de pelo menos um Produto.", exception.getMessage());
    }

    // Pins the current rule: with several laboratories the request must say which one is meant.
    @Test
    void pedidoSemLaboratorioNemProjetoDePessoaComDoisLaboratoriosNaoDeterminaOLaboratorio() {
        Laboratorio segundo = new Laboratorio();
        segundo.setId(30L);
        segundo.setPublicId(SEGUNDO_LABORATORIO_PUBLIC_ID);
        segundo.setUnidade(unidade);
        segundo.setAtivo(true);
        solicitante.substituirLaboratorios(List.of(laboratorio, segundo));
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(SOLICITANTE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(SOLICITANTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(solicitante));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> pedidoService.criar(dto));

        assertEquals("Não foi possível determinar o Laboratório do pedido.", exception.getMessage());
    }

    @Test
    void deveImpedirAprovacaoQuandoPedidoNaoEstiverPendente() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        pedido.setStatus(StatusPedido.APROVADO);
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
        );

        assertEquals(
                "Apenas pedidos PENDENTES podem ser aprovados. Status atual: APROVADO",
                exception.getMessage()
        );
        verifyNoInteractions(movimentacaoEstoqueService, estoqueCentralRepository);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirQuantidadeAprovadaMaiorQueSolicitada() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(6);

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
        );

        assertEquals(
                "Quantidade aprovada deve ser maior que zero e não pode ser maior que a solicitada. Solicitada: 5, aprovada: 6",
                exception.getMessage()
        );
        assertEquals(null, item.getQuantidadeAprovada());
        verifyNoInteractions(movimentacaoEstoqueService, estoqueCentralRepository);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void devePropagarFalhaQuandoNaoHaLotesValidosSuficientes() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);

        when(usuarioRepository.findByPublicIdAndUnidadePublicId(APROVADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(aprovador));
        prepararBuscaPedidoComBloqueio();
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L))
                .thenReturn(Optional.of(estoque));

        // Mesmo ajuste de assinatura explicado no teste
        // deveAprovarPedidoDelegandoSaidaAoMovimentacaoEstoqueService acima.
        when(movimentacaoEstoqueService.registrarSaida(
                6L,
                BigDecimal.valueOf(3),
                aprovador,
                OrigemMovimentacao.PEDIDO,
                pedido,
                laboratorio,
                "Aprovação parcial",
                TipoEmbalagem.UNITARIO,
                null
        )).thenThrow(new BusinessRuleException(
                "Estoque utilizável insuficiente para a forma de retirada UNITARIO. Disponível nos lotes compatíveis: 2, solicitado: 3"
        ));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
        );

        assertEquals(
                "Estoque utilizável insuficiente para a forma de retirada UNITARIO. Disponível nos lotes compatíveis: 2, solicitado: 3",
                exception.getMessage()
        );
        assertEquals(null, item.getQuantidadeAprovada());
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveExigirUsuarioAprovador() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        AprovarPedidoRequestDTO dto = criarAprovacaoDTO(3);
        dto.setUsuarioAprovadorId(null);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.aprovar(PEDIDO_PUBLIC_ID, dto)
        );

        assertEquals("O usuário aprovador é obrigatório.", exception.getMessage());
        verifyNoInteractions(
                usuarioRepository,
                pedidoRepository,
                estoqueCentralRepository,
                movimentacaoEstoqueService
        );
    }

    @Test
    void deveRestaurarLotesAoCancelarPedidoAprovado() {
        SessaoDeTeste.entrar(APROVADOR_PUBLIC_ID, Perfil.GESTOR);
        pedido.setStatus(StatusPedido.APROVADO);

        prepararBuscaPedidoComBloqueio();
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.cancelar(PEDIDO_PUBLIC_ID, "Cancelado pelo gestor");

        verify(movimentacaoEstoqueService).devolverSaidasDoPedido(
                pedido,
                null,
                "Cancelado pelo gestor"
        );
        assertEquals(StatusPedido.CANCELADO, resultado.getStatus());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveListarPedidosDoProjetoNoLaboratorioEPeriodo() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .publicId(PROJETO_PUBLIC_ID)
                .unidade(laboratorio.getUnidade())
                .laboratorios(new LinkedHashSet<>(Set.of(laboratorio)))
                .nome("Projeto 1")
                .ativo(true)
                .build();
        pedido.setProjeto(projeto);
        pedido.setDataSolicitacao(LocalDateTime.of(2026, 6, 15, 14, 30));

        LocalDate dataInicio = LocalDate.of(2026, 6, 1);
        LocalDate dataFim = LocalDate.of(2026, 6, 30);
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));
        when(pedidoRepository.findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim))
                .thenReturn(List.of(pedido));

        List<PedidoResponseDTO> resultado = pedidoService.listarPorProjetoEPeriodo(
                LABORATORIO_PUBLIC_ID,
                PROJETO_PUBLIC_ID,
                dataInicio,
                dataFim
        );

        assertEquals(1, resultado.size());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.get(0).getId());
        assertEquals(PROJETO_PUBLIC_ID, resultado.get(0).getProjetoId());
        verify(pedidoRepository).findByLaboratorioProjetoEPeriodo(2L, 9L, inicio, fim);
    }

    @Test
    void deveImpedirConsultaDePedidosQuandoProjetoNaoPertenceAoLaboratorio() {
        Laboratorio outroLaboratorio = Laboratorio.builder()
                .id(99L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .nome("Outro laboratório")
                .unidade(unidade)
                .ativo(true)
                .build();

        Projeto projeto = Projeto.builder()
                .id(9L)
                .publicId(PROJETO_PUBLIC_ID)
                .unidade(outroLaboratorio.getUnidade())
                .laboratorios(new LinkedHashSet<>(Set.of(outroLaboratorio)))
                .nome("Projeto externo")
                .ativo(true)
                .build();

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                )
        );

        assertEquals(
                "O projeto informado não pertence ao laboratório informado.",
                exception.getMessage()
        );
        verify(pedidoRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }

    @Test
    void deveImpedirConsultaDePedidosComPeriodoInvertido() {
        Projeto projeto = Projeto.builder()
                .id(9L)
                .publicId(PROJETO_PUBLIC_ID)
                .unidade(laboratorio.getUnidade())
                .laboratorios(new LinkedHashSet<>(Set.of(laboratorio)))
                .nome("Projeto 1")
                .ativo(true)
                .build();

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> pedidoService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 6, 1)
                )
        );

        assertEquals(
                "A data inicial não pode ser posterior à data final.",
                exception.getMessage()
        );
        verify(pedidoRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }

    @Test
    void pedidoComProjetoDeDoisLaboratoriosExigeLaboratorio() {
        Projeto projeto = projetoNosLaboratorios(laboratorio, segundoLaboratorio(true));
        solicitante.setPerfil(Perfil.TECNICO);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(SOLICITANTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(solicitante));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> pedidoService.criar(pedidoDoProjeto(null)));

        assertEquals("Informe o laboratório: o projeto está em mais de um laboratório.", exception.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void pedidoComLaboratorioDoProjetoEhAceito() {
        Laboratorio segundo = segundoLaboratorio(true);
        Projeto projeto = projetoNosLaboratorios(laboratorio, segundo);
        solicitante.setPerfil(Perfil.TECNICO);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(SOLICITANTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(solicitante));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicId(SEGUNDO_LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(segundo));
        // The requester's base laboratory is the first one; an operational link authorizes the second.
        when(vinculoOperacionalService.vinculadoLaboratorio(solicitante, segundo, LocalDate.now())).thenReturn(true);
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L)).thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.criar(pedidoDoProjeto(SEGUNDO_LABORATORIO_PUBLIC_ID));

        assertEquals(SEGUNDO_LABORATORIO_PUBLIC_ID, resultado.getLaboratorioId());
        assertEquals(PROJETO_PUBLIC_ID, resultado.getProjetoId());
    }

    @Test
    void pedidoComProjetoSemLaboratorioAtivoRecebe400() {
        laboratorio.setAtivo(false);
        Projeto projeto = projetoNosLaboratorios(laboratorio);
        solicitante.setPerfil(Perfil.TECNICO);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(SOLICITANTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(solicitante));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> pedidoService.criar(pedidoDoProjeto(null)));

        assertEquals("O projeto não possui laboratório ativo.", exception.getMessage());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void empregadoPedeEmLaboratorioForaDaListaEMarca() {
        Laboratorio outro = segundoLaboratorio(true);
        PedidoResponseDTO resposta = criarComoTecnico(outro, false);

        assertEquals(SEGUNDO_LABORATORIO_PUBLIC_ID, resposta.getLaboratorioId());
        assertTrue(resposta.getLaboratorioForaDoSolicitante());
    }

    @Test
    void empregadoNaListaNaoMarca() {
        PedidoResponseDTO resposta = criarComoTecnico(laboratorio, false);

        assertFalse(resposta.getLaboratorioForaDoSolicitante());
    }

    @Test
    void empregadoComVinculoVigenteNaoMarca() {
        PedidoResponseDTO resposta = criarComoTecnico(segundoLaboratorio(true), true);

        assertFalse(resposta.getLaboratorioForaDoSolicitante());
    }

    @Test
    void gestaoNuncaMarca() {
        Laboratorio outro = segundoLaboratorio(true);
        solicitante.setPerfil(Perfil.GESTOR);
        PedidoResponseDTO resposta = criarComo(outro);

        assertFalse(resposta.getLaboratorioForaDoSolicitante());
    }

    @Test
    void laboratorioInativoContinuaRecusado() {
        Laboratorio inativo = segundoLaboratorio(false);
        solicitante.setPerfil(Perfil.TECNICO);
        stubSolicitanteELaboratorio(inativo);

        assertThrows(BusinessRuleException.class, () -> pedidoService.criar(pedidoSimples(inativo)));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void laboratorioDeOutraUnidadeContinuaRecusado() {
        Unidade outraUnidade = Unidade.builder().id(99L).publicId(UUID.randomUUID()).nome("Outra").sigla("OU").build();
        Laboratorio deFora = segundoLaboratorio(true);
        deFora.setUnidade(outraUnidade);
        solicitante.setPerfil(Perfil.TECNICO);
        stubSolicitanteELaboratorio(deFora);

        assertThrows(BusinessRuleException.class, () -> pedidoService.criar(pedidoSimples(deFora)));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    private PedidoResponseDTO criarComoTecnico(Laboratorio alvo, boolean comVinculoVigente) {
        solicitante.setPerfil(Perfil.TECNICO);
        if (comVinculoVigente) {
            when(vinculoOperacionalService.vinculadoLaboratorio(solicitante, alvo, LocalDate.now())).thenReturn(true);
        }
        return criarComo(alvo);
    }

    private PedidoResponseDTO criarComo(Laboratorio alvo) {
        stubSolicitanteELaboratorio(alvo);
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 5L)).thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return pedidoService.criar(pedidoSimples(alvo));
    }

    private void stubSolicitanteELaboratorio(Laboratorio alvo) {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(SOLICITANTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(solicitante));
        when(laboratorioRepository.findByPublicId(alvo.getPublicId())).thenReturn(Optional.of(alvo));
    }

    private PedidoRequestDTO pedidoSimples(Laboratorio alvo) {
        PedidoRequestDTO dto = pedidoDoProjeto(alvo.getPublicId());
        dto.setProjetoId(null);
        return dto;
    }

    private Laboratorio segundoLaboratorio(boolean ativo) {
        return Laboratorio.builder()
                .id(12L)
                .publicId(SEGUNDO_LABORATORIO_PUBLIC_ID)
                .nome("Laboratório de Biologia")
                .unidade(unidade)
                .ativo(ativo)
                .build();
    }

    private Projeto projetoNosLaboratorios(Laboratorio... laboratorios) {
        return Projeto.builder()
                .id(10L)
                .publicId(PROJETO_PUBLIC_ID)
                .nome("Projeto multi-laboratório")
                .unidade(unidade)
                .laboratorios(new LinkedHashSet<>(List.of(laboratorios)))
                .ativo(true)
                .build();
    }

    private PedidoRequestDTO pedidoDoProjeto(UUID laboratorioId) {
        ItemPedidoRequestDTO itemDTO = new ItemPedidoRequestDTO();
        itemDTO.setProdutoId(PRODUTO_PUBLIC_ID);
        itemDTO.setQuantidadeSolicitada(BigDecimal.valueOf(2));
        itemDTO.setTipoEmbalagemSolicitada(TipoEmbalagem.UNITARIO);
        itemDTO.setQuantidadeEmbalagensSolicitada(2);
        itemDTO.setMultiplicadorSolicitado(BigDecimal.ONE);

        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(SOLICITANTE_PUBLIC_ID);
        dto.setProjetoId(PROJETO_PUBLIC_ID);
        dto.setLaboratorioId(laboratorioId);
        dto.setItens(List.of(itemDTO));
        return dto;
    }

    private void prepararBuscaPedidoComBloqueio() {
        when(pedidoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PEDIDO_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.buscarPorIdComBloqueio(7L)).thenReturn(Optional.of(pedido));
    }

    private AprovarPedidoRequestDTO criarAprovacaoDTO(Integer quantidadeAprovada) {
        AprovarPedidoRequestDTO.ItemAprovacaoDTO itemDTO =
                new AprovarPedidoRequestDTO.ItemAprovacaoDTO(ITEM_PUBLIC_ID, quantidadeAprovada == null ? null : BigDecimal.valueOf(quantidadeAprovada));

        AprovarPedidoRequestDTO dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(APROVADOR_PUBLIC_ID);
        dto.setObservacao("Aprovação parcial");
        dto.setItens(List.of(itemDTO));
        return dto;
    }
}
