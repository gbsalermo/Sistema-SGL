package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.EstoqueCentralRequestDTO;
import com.sgl.dto.response.EstoqueCentralResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link EstoqueCentralService}.
 *
 * Nota sobre {@code docs/CODIGOS_REFERENCIA_TESTES.md} (mencionado no brief
 * como possível referência): o rascunho lá documentado é de uma versão bem
 * mais antiga do Service, que tinha métodos {@code entrada()}/{@code saida()}
 * com registro de {@code MovimentacaoEstoque} embutido. Essa lógica hoje mora
 * em {@code MovimentacaoEstoqueService} (já coberto por
 * {@code MovimentacaoEstoqueServiceTest}); o {@code EstoqueCentralService}
 * atual só cuida do CRUD do registro de estoque em si. O rascunho não foi
 * reaproveitado — os testes abaixo seguem o fonte atual.
 *
 * Duas regras de tenant coexistem aqui (mesmo padrão de
 * {@code LoteService}): {@code exigirTenantAtivo()} (nenhum header
 * X-SGL-Unidade-Id enviado => acesso negado) e
 * {@code validarTenantUnidade(unidadeId)} (header enviado, mas apontando para
 * uma unidade diferente da informada na operação).
 */
@ExtendWith(MockitoExtension.class)
class EstoqueCentralServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ESTOQUE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private EstoqueCentralService estoqueCentralService;

    private Unidade unidade;
    private Produto produto;
    private EstoqueCentral estoque;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        produto = new Produto();
        produto.setId(10L);
        produto.setPublicId(PRODUTO_PUBLIC_ID);
        produto.setNome("Ágar Nutriente");
        produto.setUnidadeMedida(UnidadeMedida.KG);
        produto.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(20L)
                .publicId(ESTOQUE_PUBLIC_ID)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(5)
                .quantidadeMinima(2)
                .ativo(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarEstoqueComDadosValidos() {
        EstoqueCentralRequestDTO dto = new EstoqueCentralRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setQuantidadeMinima(3);
        dto.setAtivo(true);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()))
                .thenReturn(false);
        when(estoqueCentralRepository.save(any(EstoqueCentral.class))).thenReturn(estoque);

        EstoqueCentralResponseDTO resultado = estoqueCentralService.criar(dto);

        assertEquals(ESTOQUE_PUBLIC_ID, resultado.getId());
        assertEquals(UNIDADE_PUBLIC_ID, resultado.getUnidadeId());
        assertEquals(PRODUTO_PUBLIC_ID, resultado.getProdutoId());
    }

    @Test
    void deveRejeitarCriacaoQuandoJaExisteEstoqueParaProdutoNaUnidade() {
        EstoqueCentralRequestDTO dto = new EstoqueCentralRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setQuantidadeMinima(3);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estoqueCentralService.criar(dto));

        assertEquals("Já existe estoque para esse produto nesta unidade.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoParaProdutoInativo() {
        produto.setAtivo(false);

        EstoqueCentralRequestDTO dto = new EstoqueCentralRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setQuantidadeMinima(3);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estoqueCentralService.criar(dto));

        assertEquals("Não é possível criar estoque para produto inativo.", ex.getMessage());
    }

    @Test
    void deveListarTodosOsEstoques() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(estoque));

        List<EstoqueCentralResponseDTO> resultado = estoqueCentralService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarEstoquePorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(ESTOQUE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));

        EstoqueCentralResponseDTO resultado = estoqueCentralService.buscarPorId(ESTOQUE_PUBLIC_ID);

        assertEquals(ESTOQUE_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveBuscarEstoquePorUnidadeEProduto() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()))
                .thenReturn(Optional.of(estoque));

        EstoqueCentralResponseDTO resultado =
                estoqueCentralService.buscarPorUnidadeEProduto(UNIDADE_PUBLIC_ID, PRODUTO_PUBLIC_ID);

        assertEquals(ESTOQUE_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveListarEstoquesPorUnidade() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(estoque));

        List<EstoqueCentralResponseDTO> resultado = estoqueCentralService.listarPorUnidade(UNIDADE_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveAtualizarEstoque() {
        EstoqueCentralRequestDTO dto = new EstoqueCentralRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setQuantidadeMinima(8);
        dto.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(ESTOQUE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(estoqueCentralRepository.save(any(EstoqueCentral.class))).thenReturn(estoque);

        EstoqueCentralResponseDTO resultado = estoqueCentralService.atualizar(ESTOQUE_PUBLIC_ID, dto);

        assertEquals(8, resultado.getQuantidadeMinima());
        assertFalse(resultado.getAtivo());
    }

    @Test
    void deveListarEstoqueBaixoPorUnidade() {
        EstoqueCentral estoqueBaixo = EstoqueCentral.builder()
                .id(21L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(1)
                .quantidadeMinima(5)
                .ativo(true)
                .build();

        EstoqueCentral estoqueOk = EstoqueCentral.builder()
                .id(22L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(10)
                .quantidadeMinima(5)
                .ativo(true)
                .build();

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByUnidadePublicIdAndAtivoTrue(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(estoqueBaixo, estoqueOk));

        List<EstoqueCentralResponseDTO> resultado = estoqueCentralService.listarEstoqueBaixoPorUnidade(UNIDADE_PUBLIC_ID);

        assertEquals(1, resultado.size());
        assertEquals(1, resultado.get(0).getQuantidadeAtual());
    }

    @Test
    void deveDeletarEstoque() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(ESTOQUE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));

        estoqueCentralService.deletar(ESTOQUE_PUBLIC_ID);

        assertFalse(estoque.getAtivo());
    }

    @Test
    void deveRejeitarDelecaoQuandoJaInativo() {
        estoque.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(ESTOQUE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estoqueCentralService.deletar(ESTOQUE_PUBLIC_ID));

        assertEquals("O estoque central já está inativo.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEstoqueNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> estoqueCentralService.buscarPorId(idInexistente));

        assertEquals("Estoque central não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarOperacaoQuandoUnidadeDeOutroTenant() {
        // validarTenantUnidade(): o tenant ativo é UNIDADE_PUBLIC_ID, mas a
        // operação tenta criar estoque para OUTRA_UNIDADE_PUBLIC_ID.
        EstoqueCentralRequestDTO dto = new EstoqueCentralRequestDTO();
        dto.setUnidadeId(OUTRA_UNIDADE_PUBLIC_ID);
        dto.setProdutoId(PRODUTO_PUBLIC_ID);
        dto.setQuantidadeMinima(3);

        TenantContext.definir(UNIDADE_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estoqueCentralService.criar(dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
    }

    @Test
    void deveRejeitarOperacaoQuandoTenantNaoDefinido() {
        // exigirTenantAtivo(): diferente de validarTenantUnidade() (acima),
        // esta regra dispara quando NENHUM header X-SGL-Unidade-Id foi
        // enviado (TenantContext nunca definido nesta thread), e não quando
        // o header aponta para uma unidade errada. Usada por listarTodos(),
        // buscarEstoqueNoTenant() (buscarPorId/atualizar/deletar).
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> estoqueCentralService.listarTodos());

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }
}
