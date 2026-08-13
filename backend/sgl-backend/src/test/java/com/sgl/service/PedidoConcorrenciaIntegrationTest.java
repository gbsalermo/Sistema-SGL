package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.dto.AprovarPedidoDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
class PedidoConcorrenciaIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueCentralRepository estoqueCentralRepository;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    private ExecutorService executor;

    @AfterEach
    void encerrarExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void deveImpedirDoisPedidosConcorrentesDeConsumiremOMesmoSaldo() throws Exception {
        Unidade unidade = unidadeRepository.saveAndFlush(
                Unidade.builder()
                        .nome("Unidade Concorrência")
                        .sigla("CONC")
                        .build()
        );

        Laboratorio laboratorio = laboratorioRepository.saveAndFlush(
                Laboratorio.builder()
                        .unidade(unidade)
                        .nome("Laboratório Concorrência")
                        .descricao("Cenário de teste concorrente")
                        .ativo(true)
                        .build()
        );

        Usuario usuario = new Usuario();
        usuario.setNome("Usuário Concorrência");
        usuario.setEmail("concorrencia@sgl.test");
        usuario.setSenha("senha-teste");
        usuario.setPerfil(Perfil.GESTOR);
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);
        usuario.setAtivo(true);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Produto produto = produtoRepository.saveAndFlush(
                Produto.builder()
                        .nome("Produto Concorrência")
                        .codigoReferencia("CONC-001")
                        .unidadeMedida(UnidadeMedida.UNIDADE)
                        .risco(NivelRisco.NENHUM)
                        .perecivel(false)
                        .unidadeArmazenamento("unidade")
                        .ativo(true)
                        .build()
        );

        EstoqueCentral estoque = estoqueCentralRepository.saveAndFlush(
                EstoqueCentral.builder()
                        .unidade(unidade)
                        .produto(produto)
                        .quantidadeAtual(10)
                        .quantidadeMinima(2)
                        .ativo(true)
                        .build()
        );

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.setNumeroLote("CONC-LOTE-001");
        lote.setQuantidadeInicial(10);
        lote.setQuantidadeDisponivel(10);
        lote.setDataEntrada(LocalDate.now());
        lote.setDataValidade(null);
        lote.setAtivo(true);
        lote = loteRepository.saveAndFlush(lote);

        Pedido pedidoA = criarPedido(usuario, laboratorio, produto, 7, "Pedido concorrente A");
        Pedido pedidoB = criarPedido(usuario, laboratorio, produto, 7, "Pedido concorrente B");

        Long pedidoAId = pedidoA.getId();
        Long pedidoBId = pedidoB.getId();
        Long itemAId = pedidoA.getItens().get(0).getId();
        Long itemBId = pedidoB.getItens().get(0).getId();
        Long estoqueId = estoque.getId();
        Long loteId = lote.getId();
        Long usuarioId = usuario.getId();

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        executor = Executors.newFixedThreadPool(2);

        Future<Boolean> resultadoA = executor.submit(() -> aprovarConcorrentemente(
                pedidoAId,
                itemAId,
                usuarioId,
                prontas,
                iniciar
        ));

        Future<Boolean> resultadoB = executor.submit(() -> aprovarConcorrentemente(
                pedidoBId,
                itemBId,
                usuarioId,
                prontas,
                iniciar
        ));

        assertTrue(prontas.await(5, TimeUnit.SECONDS), "As duas aprovações deveriam estar prontas para iniciar.");
        iniciar.countDown();

        boolean aprovadoA = resultadoA.get(10, TimeUnit.SECONDS);
        boolean aprovadoB = resultadoB.get(10, TimeUnit.SECONDS);

        assertEquals(1, (aprovadoA ? 1 : 0) + (aprovadoB ? 1 : 0),
                "Exatamente um dos pedidos deve ser aprovado.");

        Pedido pedidoAAtual = pedidoRepository.findById(pedidoAId).orElseThrow();
        Pedido pedidoBAtual = pedidoRepository.findById(pedidoBId).orElseThrow();

        long aprovados = List.of(pedidoAAtual, pedidoBAtual).stream()
                .filter(p -> p.getStatus() == StatusPedido.APROVADO)
                .count();
        long pendentes = List.of(pedidoAAtual, pedidoBAtual).stream()
                .filter(p -> p.getStatus() == StatusPedido.PENDENTE)
                .count();

        assertEquals(1, aprovados, "Somente um pedido pode terminar APROVADO.");
        assertEquals(1, pendentes, "O pedido sem saldo deve permanecer PENDENTE.");

        EstoqueCentral estoqueAtual = estoqueCentralRepository.findById(estoqueId).orElseThrow();
        Lote loteAtual = loteRepository.findById(loteId).orElseThrow();

        assertEquals(3, estoqueAtual.getQuantidadeAtual(),
                "O estoque agregado deve terminar com 3 unidades.");
        assertEquals(3, loteAtual.getQuantidadeDisponivel(),
                "O lote deve terminar com 3 unidades.");
        assertTrue(estoqueAtual.getQuantidadeAtual() >= 0, "O estoque nunca pode ficar negativo.");
        assertTrue(loteAtual.getQuantidadeDisponivel() >= 0, "O lote nunca pode ficar negativo.");

        List<MovimentacaoEstoque> saidas = movimentacaoEstoqueRepository
                .findByTipoMovimentacao(TipoMovimentacao.SAIDA);

        int totalSaidas = saidas.stream()
                .filter(m -> m.getEstoqueCentral().getId().equals(estoqueId))
                .mapToInt(MovimentacaoEstoque::getQuantidadeMovimentada)
                .sum();

        long quantidadeMovimentacoesSaida = saidas.stream()
                .filter(m -> m.getEstoqueCentral().getId().equals(estoqueId))
                .count();

        assertEquals(7, totalSaidas,
                "Somente 7 unidades podem ser registradas como SAIDA.");
        assertEquals(1, quantidadeMovimentacoesSaida,
                "Com um único lote, deve existir somente uma SAIDA para o pedido vencedor.");
    }

    private Pedido criarPedido(
            Usuario usuario,
            Laboratorio laboratorio,
            Produto produto,
            int quantidade,
            String observacao) {

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .dataSolicitacao(LocalDateTime.now())
                .status(StatusPedido.PENDENTE)
                .observacao(observacao)
                .build();

        ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(quantidade)
                .build();

        pedido.getItens().add(item);
        return pedidoRepository.saveAndFlush(pedido);
    }

    private boolean aprovarConcorrentemente(
            Long pedidoId,
            Long itemId,
            Long usuarioAprovadorId,
            CountDownLatch prontas,
            CountDownLatch iniciar) throws InterruptedException {

        prontas.countDown();
        iniciar.await();

        AprovarPedidoDTO dto = new AprovarPedidoDTO();
        dto.setUsuarioAprovadorId(usuarioAprovadorId);
        dto.setObservacao("Teste de concorrência");
        dto.setItens(List.of(new AprovarPedidoDTO.ItemAprovacaoDTO(itemId, 7)));

        try {
            pedidoService.aprovar(pedidoId, dto);
            return true;
        } catch (BusinessRuleException exception) {
            assertTrue(
                    exception.getMessage().startsWith("Estoque utilizável insuficiente."),
                    "A única falha de negócio esperada é estoque utilizável insuficiente."
            );
            return false;
        }
    }
}
