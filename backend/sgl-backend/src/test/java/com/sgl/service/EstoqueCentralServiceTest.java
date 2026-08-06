package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

/**
 * Testes unitários das regras de movimentação do estoque central.
 *
 * <p>Nesta classe o Spring e o banco de dados não são iniciados. O Service é
 * executado de verdade, mas seus repositories são substituídos por mocks do
 * Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class EstoqueCentralServiceTest {

    /*
     * Cada @Mock cria uma implementação falsa do repository.
     * Esses objetos não acessam o banco de dados.
     */
    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    /*
     * O @InjectMocks cria o EstoqueCentralService real e injeta nele todos os
     * repositories falsos declarados acima.
     */
    @InjectMocks
    private EstoqueCentralService estoqueCentralService;

    /* Objetos reutilizados pelos diferentes cenários de teste. */
    private Unidade unidade;
    private Produto produto;
    private Usuario usuario;
    private EstoqueCentral estoque;

    /**
     * Executado antes de cada teste.
     *
     * <p>Cada método começa com um cenário novo: saldo 10, usuário ativo e
     * estoque ativo. Dessa forma, um teste não altera os dados do próximo.</p>
     */
    @BeforeEach
    void prepararCenario() {

        unidade = Unidade.builder()
                .id(1L)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        produto = Produto.builder()
                .id(10L)
                .nome("Álcool 70%")
                .unidadeArmazenamento("Frasco de 1 L")
                .ativo(true)
                .build();

        usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNome("Usuário de Teste");
        usuario.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(30L)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(2)
                .ativo(true)
                .build();
    }

    /**
     * Cenário feliz da entrada.
     *
     * <p>Partindo de saldo 10 e recebendo uma entrada de 5, o saldo deve passar
     * para 15. Além disso, uma movimentação do tipo ENTRADA deve ser registrada
     * com os saldos anterior e atual corretos.</p>
     */
    @Test
    void deveAumentarSaldoERegistrarMovimentacaoAoRealizarEntrada() {

        // ARRANGE: cria o DTO que representa uma entrada de cinco unidades.
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(5);

        // Programa os mocks para devolver o estoque e o usuário procurados.
        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        // ACT: executa a regra verdadeira do Service.
        EstoqueCentralDTO resultado = estoqueCentralService.entrada(30L, dto);

        // ASSERT: confirma que 10 + 5 resultou em saldo 15.
        assertEquals(15, estoque.getQuantidadeAtual());
        assertEquals(15, resultado.getQuantidadeAtual());

        // Confirma que o Service solicitou a persistência do estoque atualizado.
        verify(estoqueCentralRepository).save(estoque);

        /*
         * Captura a movimentação criada internamente pelo Service e enviada ao
         * repository, permitindo verificar todos os campos importantes.
         */
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque movimentacao = captor.getValue();

        assertEquals(TipoMovimentacao.ENTRADA, movimentacao.getTipoMovimentacao());
        assertEquals(OrigemMovimentacao.COMPRA, movimentacao.getOrigem());
        assertEquals(5, movimentacao.getQuantidadeMovimentada());
        assertEquals(10, movimentacao.getQuantidadeAnterior());
        assertEquals(15, movimentacao.getQuantidadeAtual());
        assertEquals(produto, movimentacao.getProduto());
        assertEquals(usuario, movimentacao.getUsuario());
        assertEquals(estoque, movimentacao.getEstoqueCentral());
    }

    /**
     * Cenário feliz da saída.
     *
     * <p>Partindo de saldo 10 e retirando 4 unidades, o saldo deve passar para
     * 6 e uma movimentação do tipo SAIDA deve ser registrada.</p>
     */
    @Test
    void deveReduzirSaldoERegistrarMovimentacaoAoRealizarSaida() {

        // ARRANGE: cria uma movimentação de saída de quatro unidades.
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(4);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        // ACT: executa a saída.
        EstoqueCentralDTO resultado = estoqueCentralService.saida(30L, dto);

        // ASSERT: confirma que 10 - 4 resultou em saldo 6.
        assertEquals(6, estoque.getQuantidadeAtual());
        assertEquals(6, resultado.getQuantidadeAtual());

        verify(estoqueCentralRepository).save(estoque);

        // Captura e verifica a movimentação gerada pela saída.
        ArgumentCaptor<MovimentacaoEstoque> captor =
                ArgumentCaptor.forClass(MovimentacaoEstoque.class);

        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque movimentacao = captor.getValue();

        assertEquals(TipoMovimentacao.SAIDA, movimentacao.getTipoMovimentacao());
        assertEquals(OrigemMovimentacao.COMPRA, movimentacao.getOrigem());
        assertEquals(4, movimentacao.getQuantidadeMovimentada());
        assertEquals(10, movimentacao.getQuantidadeAnterior());
        assertEquals(6, movimentacao.getQuantidadeAtual());
    }

    /**
     * Regra de proteção contra saldo negativo.
     *
     * <p>O estoque possui 10 unidades, portanto uma tentativa de retirar 15
     * deve lançar BusinessRuleException. Nenhuma alteração ou movimentação pode
     * ser salva.</p>
     */
    @Test
    void deveImpedirSaidaQuandoEstoqueForInsuficiente() {

        // ARRANGE: solicita uma quantidade maior que o saldo disponível.
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(15);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        // ACT + ASSERT: confirma que a regra lança a exceção de domínio correta.
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> estoqueCentralService.saida(30L, dto)
        );

        assertEquals(
                "Estoque insuficiente. Disponível: 10, solicitado: 15",
                exception.getMessage()
        );

        // O saldo deve continuar intacto após a tentativa inválida.
        assertEquals(10, estoque.getQuantidadeAtual());

        // Nenhuma persistência pode ocorrer nesse cenário.
        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    /**
     * Regra que exige uma quantidade positiva.
     *
     * <p>A validação ocorre antes das consultas aos repositories. Por isso,
     * além da exceção, verificamos que nenhum mock foi utilizado.</p>
     */
    @Test
    void deveRejeitarQuantidadeZeroAntesDeConsultarRepositories() {

        // ARRANGE: cria uma movimentação com quantidade inválida.
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(0);

        // ACT + ASSERT: espera a exceção antes de qualquer consulta ao banco.
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> estoqueCentralService.saida(30L, dto)
        );

        assertEquals(
                "A quantidade deve ser maior que zero.",
                exception.getMessage()
        );

        // Nenhum repository deveria ser chamado porque a quantidade falhou primeiro.
        verifyNoInteractions(
                estoqueCentralRepository,
                usuarioRepository,
                movimentacaoEstoqueRepository
        );
    }

    /**
     * Regra que impede uma saída realizada por usuário inativo.
     *
     * <p>O estoque e o usuário são encontrados, mas a operação deve parar antes
     * de alterar o saldo ou registrar a movimentação.</p>
     */
    @Test
    void deveImpedirSaidaQuandoUsuarioEstiverInativo() {

        // ARRANGE: altera apenas o estado do usuário para inativo.
        usuario.setAtivo(false);
        MovimentacaoEstoqueDTO dto = criarMovimentacaoDTO(2);

        when(estoqueCentralRepository.findById(30L))
                .thenReturn(Optional.of(estoque));

        when(usuarioRepository.findById(20L))
                .thenReturn(Optional.of(usuario));

        // ACT + ASSERT: espera a exceção da regra de usuário inativo.
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> estoqueCentralService.saida(30L, dto)
        );

        assertEquals(
                "O usuário responsável pela movimentação está inativo.",
                exception.getMessage()
        );

        // O saldo permanece igual a 10 e nada é salvo.
        assertEquals(10, estoque.getQuantidadeAtual());
        verify(estoqueCentralRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    /**
     * Método auxiliar para evitar repetição na criação dos DTOs usados pelos
     * testes.
     */
    private MovimentacaoEstoqueDTO criarMovimentacaoDTO(Integer quantidade) {

        MovimentacaoEstoqueDTO dto = new MovimentacaoEstoqueDTO();
        dto.setUsuarioId(20L);
        dto.setQuantidadeMovimentada(quantidade);
        dto.setOrigem(OrigemMovimentacao.COMPRA);
        dto.setObservacao("Movimentação criada durante teste");

        return dto;
    }
}
