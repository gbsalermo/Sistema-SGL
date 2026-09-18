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

import com.sgl.dto.request.ProdutoRequestDTO;
import com.sgl.dto.response.ProdutoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ProdutoRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link ProdutoService}.
 *
 * Particularidade importante desta classe: {@code Produto} é tratado como
 * catálogo COMPARTILHADO entre unidades. {@code listarTodos()},
 * {@code listarPorRisco()}, {@code listarPereciveis()} e
 * {@code buscarPorNome()} passam por {@code produtosVisiveis()}, que só
 * filtra por unidade quando existe tenant ativo (via
 * {@code findDisponiveisNaUnidade}); sem tenant, devolve o catálogo inteiro
 * (via {@code findAll}). Isso é comportamento intencional (produtos ainda não
 * vinculados a nenhum estoque de unidade precisam aparecer para quem for
 * cadastrar um novo estoque), não um vazamento a corrigir — por isso os
 * testes abaixo de listagem não definem {@code TenantContext}, exercitando o
 * caminho "sem tenant" (findAll). {@code buscarPorId()}/{@code atualizar()}/
 * {@code deletar()} usam {@code buscarProdutoNoTenant()}, que só valida a
 * unidade quando há tenant definido (mesmo padrão "fail closed apenas se
 * ativo" herdado de {@code TenantContext.pertence}).
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setPublicId(PRODUTO_PUBLIC_ID);
        produto.setNome("Ácido Sulfúrico");
        produto.setDescricao("Reagente de laboratório.");
        produto.setCodigoReferencia("AS-98");
        produto.setUnidadeMedida(UnidadeMedida.L);
        produto.setLocalizacaoFisica("Armário de ácidos");
        produto.setRisco(NivelRisco.ALTO);
        produto.setTipoRisco(TipoRisco.CORROSIVO);
        produto.setDescricaoRisco("Corrosivo, causa queimaduras graves.");
        produto.setPerecivel(false);
        produto.setAtivo(true);
    }

    @AfterEach
    void tearDown() {
        // Evita que um TenantContext definido num teste vaze para o próximo
        // (ThreadLocal compartilhado entre execuções na mesma thread).
        TenantContext.limpar();
    }

    @Test
    void deveCriarProdutoComDadosValidos() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO();
        dto.setNome("Álcool Etílico 70%");
        dto.setDescricao("Álcool para assepsia.");
        dto.setCodigoReferencia("ALC-70");
        dto.setUnidadeMedida(UnidadeMedida.L);
        dto.setLocalizacaoFisica("Armário B");
        dto.setRisco(NivelRisco.NENHUM);
        dto.setPerecivel(false);
        dto.setFiscalizado(false);

        when(produtoRepository.existsByCodigoReferencia("ALC-70")).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
            Produto salvo = invocation.getArgument(0);
            salvo.setId(2L);
            salvo.setPublicId(PRODUTO_PUBLIC_ID);
            return salvo;
        });

        ProdutoResponseDTO resultado = produtoService.criar(dto);

        assertEquals(PRODUTO_PUBLIC_ID, resultado.getId());
        assertEquals("Álcool Etílico 70%", resultado.getNome());
        assertEquals(NivelRisco.NENHUM, resultado.getRisco());
        assertFalse(resultado.getPerecivel());
    }

    @Test
    void deveRejeitarCriacaoQuandoCodigoReferenciaJaExiste() {
        // Única BusinessRuleException lançada diretamente pelo Service (as
        // demais regras de risco/perecibilidade/fiscalização vivem na
        // entidade Produto). Cobrindo aqui a duplicidade de código de
        // referência, checada antes do save.
        ProdutoRequestDTO dto = new ProdutoRequestDTO();
        dto.setNome("Álcool Etílico 70%");
        dto.setCodigoReferencia("AS-98");
        dto.setUnidadeMedida(UnidadeMedida.L);
        dto.setRisco(NivelRisco.NENHUM);
        dto.setPerecivel(false);

        when(produtoRepository.existsByCodigoReferencia("AS-98")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> produtoService.criar(dto));

        assertEquals("Já existe um produto com este código de referência.", ex.getMessage());
    }

    @Test
    void deveListarTodosOsProdutos() {
        when(produtoRepository.findAll()).thenReturn(List.of(produto));

        List<ProdutoResponseDTO> resultado = produtoService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(PRODUTO_PUBLIC_ID, resultado.get(0).getId());
    }

    @Test
    void deveListarProdutosPorRisco() {
        Produto produtoBaixoRisco = new Produto();
        produtoBaixoRisco.setId(2L);
        produtoBaixoRisco.setPublicId(UUID.randomUUID());
        produtoBaixoRisco.setNome("Sabão neutro");
        produtoBaixoRisco.setUnidadeMedida(UnidadeMedida.UNIDADE);
        produtoBaixoRisco.setRisco(NivelRisco.BAIXO);
        produtoBaixoRisco.setPerecivel(false);

        when(produtoRepository.findAll()).thenReturn(List.of(produto, produtoBaixoRisco));

        List<ProdutoResponseDTO> resultado = produtoService.listarPorRisco(NivelRisco.ALTO);

        assertEquals(1, resultado.size());
        assertEquals(NivelRisco.ALTO, resultado.get(0).getRisco());
    }

    @Test
    void deveListarProdutosPereciveis() {
        Produto produtoPerecivel = new Produto();
        produtoPerecivel.setId(3L);
        produtoPerecivel.setPublicId(UUID.randomUUID());
        produtoPerecivel.setNome("Meio de cultura");
        produtoPerecivel.setUnidadeMedida(UnidadeMedida.UNIDADE);
        produtoPerecivel.setRisco(NivelRisco.NENHUM);
        produtoPerecivel.setPerecivel(true);

        when(produtoRepository.findAll()).thenReturn(List.of(produto, produtoPerecivel));

        List<ProdutoResponseDTO> resultado = produtoService.listarPereciveis();

        assertEquals(1, resultado.size());
        assertEquals("Meio de cultura", resultado.get(0).getNome());
    }

    @Test
    void deveBuscarProdutosPorNome() {
        Produto outroProduto = new Produto();
        outroProduto.setId(4L);
        outroProduto.setPublicId(UUID.randomUUID());
        outroProduto.setNome("Etanol absoluto");
        outroProduto.setUnidadeMedida(UnidadeMedida.L);
        outroProduto.setRisco(NivelRisco.NENHUM);
        outroProduto.setPerecivel(false);

        when(produtoRepository.findAll()).thenReturn(List.of(produto, outroProduto));

        // Busca é case-insensitive e por substring (contains).
        List<ProdutoResponseDTO> resultado = produtoService.buscarPorNome("sulfú");

        assertEquals(1, resultado.size());
        assertEquals("Ácido Sulfúrico", resultado.get(0).getNome());
    }

    @Test
    void deveBuscarProdutoPorId() {
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));

        ProdutoResponseDTO resultado = produtoService.buscarPorId(PRODUTO_PUBLIC_ID);

        assertEquals(PRODUTO_PUBLIC_ID, resultado.getId());
        assertEquals("Ácido Sulfúrico", resultado.getNome());
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(produtoRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> produtoService.buscarPorId(idInexistente));

        assertEquals("Produto não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveAtualizarProduto() {
        ProdutoRequestDTO dto = new ProdutoRequestDTO();
        dto.setNome("Ácido Sulfúrico P.A.");
        dto.setDescricao("Reagente de laboratório, grau analítico.");
        dto.setCodigoReferencia("AS-98");
        dto.setUnidadeMedida(UnidadeMedida.L);
        dto.setRisco(NivelRisco.ALTO);
        dto.setTipoRisco(TipoRisco.CORROSIVO);
        dto.setPerecivel(false);

        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        // produto.getId() != null aqui, então a checagem de duplicidade usa a
        // variante "AndIdNot" (permite manter o próprio código de referência).
        when(produtoRepository.existsByCodigoReferenciaAndIdNot("AS-98", 1L)).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);

        ProdutoResponseDTO resultado = produtoService.atualizar(PRODUTO_PUBLIC_ID, dto);

        assertEquals("Ácido Sulfúrico P.A.", resultado.getNome());
        assertEquals("Reagente de laboratório, grau analítico.", resultado.getDescricao());
    }

    @Test
    void deveDeletarProduto() {
        // deletar() faz soft-delete: só marca ativo=false, sem chamar save()
        // explicitamente (a alteração é persistida pelo dirty checking da
        // transação — fora do escopo deste teste unitário puro).
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));

        produtoService.deletar(PRODUTO_PUBLIC_ID);

        assertFalse(produto.getAtivo());
    }
}
