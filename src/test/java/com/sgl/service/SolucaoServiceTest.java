package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.sgl.dto.request.SolucaoComponenteRequestDTO;
import com.sgl.dto.request.SolucaoRequestDTO;
import com.sgl.dto.response.SolucaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Produto;
import com.sgl.model.Solucao;
import com.sgl.model.SolucaoComponente;
import com.sgl.model.Unidade;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.SolucaoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class SolucaoServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_ID =
            UUID.fromString("70000000-0000-0000-0000-000000000002");
    private static final UUID PRODUTO_ID =
            UUID.fromString("70000000-0000-0000-0000-000000000003");
    private static final UUID SOLUCAO_ID =
            UUID.fromString("70000000-0000-0000-0000-000000000004");
    private static final UUID COMPONENTE_ID =
            UUID.fromString("70000000-0000-0000-0000-000000000005");

    @Mock
    private SolucaoRepository solucaoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private SolucaoService service;

    private Unidade unidade;
    private Produto produto;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        produto = Produto.builder()
                .id(2L)
                .publicId(PRODUTO_ID)
                .nome("Etanol")
                .codigoReferencia("ET-001")
                .unidadeMedida(UnidadeMedida.L)
                .ativo(true)
                .build();

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarSolucaoConvertendoComponenteParaUnidadeCanonicaNaResposta() {
        SolucaoRequestDTO dto = requestValido();

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(solucaoRepository.existsByUnidadeIdAndNomeIgnoreCase(
                unidade.getId(), "Solução Etanólica"))
                .thenReturn(false);
        when(produtoRepository.findByPublicId(PRODUTO_ID))
                .thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_ID, UNIDADE_ID))
                .thenReturn(true);
        when(solucaoRepository.save(any(Solucao.class)))
                .thenAnswer(invocation -> {
                    Solucao solucao = invocation.getArgument(0);
                    solucao.setId(10L);
                    solucao.setPublicId(SOLUCAO_ID);
                    SolucaoComponente componente = solucao.getComponentes().get(0);
                    componente.setId(11L);
                    componente.setPublicId(COMPONENTE_ID);
                    return solucao;
                });

        SolucaoResponseDTO resultado = service.criar(dto);

        assertEquals(SOLUCAO_ID, resultado.getId());
        assertEquals("Solução Etanólica", resultado.getNome());
        assertEquals(1, resultado.getComponentes().size());
        assertEquals(
                0,
                resultado.getComponentes().get(0).getQuantidadeCanonica()
                        .compareTo(new BigDecimal("0.500"))
        );
        assertEquals(UnidadeMedida.L, resultado.getComponentes().get(0).getUnidadeCanonica());
    }

    @Test
    void deveBloquearComponenteComUnidadeIncompativel() {
        SolucaoRequestDTO dto = requestValido();
        dto.getComponentes().get(0).setUnidadeMedida(UnidadeMedida.G);

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(solucaoRepository.existsByUnidadeIdAndNomeIgnoreCase(
                unidade.getId(), "Solução Etanólica"))
                .thenReturn(false);
        when(produtoRepository.findByPublicId(PRODUTO_ID))
                .thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_ID, UNIDADE_ID))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto)
        );

        assertEquals(
                "A unidade G não é compatível com a unidade canônica L do produto Etanol.",
                ex.getMessage()
        );
    }

    @Test
    void deveBloquearProdutoDuplicadoNaComposicao() {
        SolucaoRequestDTO dto = requestValido();

        SolucaoComponenteRequestDTO repetido = new SolucaoComponenteRequestDTO();
        repetido.setProdutoId(PRODUTO_ID);
        repetido.setQuantidade(new BigDecimal("0.250"));
        repetido.setUnidadeMedida(UnidadeMedida.L);
        dto.setComponentes(List.of(dto.getComponentes().get(0), repetido));

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(solucaoRepository.existsByUnidadeIdAndNomeIgnoreCase(
                unidade.getId(), "Solução Etanólica"))
                .thenReturn(false);
        when(produtoRepository.findByPublicId(PRODUTO_ID))
                .thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_ID, UNIDADE_ID))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto)
        );

        assertEquals(
                "O mesmo produto não pode aparecer mais de uma vez na composição da solução.",
                ex.getMessage()
        );
    }

    @Test
    void deveBloquearCriacaoEmOutraUnidade() {
        SolucaoRequestDTO dto = requestValido();
        dto.setUnidadeId(OUTRA_UNIDADE_ID);

        Unidade outraUnidade = Unidade.builder()
                .id(9L)
                .publicId(OUTRA_UNIDADE_ID)
                .nome("Outra Unidade")
                .sigla("OU")
                .build();

        when(unidadeRepository.findByPublicId(OUTRA_UNIDADE_ID))
                .thenReturn(Optional.of(outraUnidade));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto)
        );

        assertEquals(
                "A operação não pode acessar dados de outra unidade.",
                ex.getMessage()
        );
    }

    @Test
    void deveListarSomenteSolucoesAtivasDaUnidadeAtual() {
        Solucao solucao = solucaoPersistida();

        when(solucaoRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UNIDADE_ID))
                .thenReturn(List.of(solucao));

        List<SolucaoResponseDTO> resultado = service.listarAtivas();

        assertEquals(1, resultado.size());
        assertEquals(SOLUCAO_ID, resultado.get(0).getId());
    }

    @Test
    void deveExigirTenantAtivo() {
        TenantContext.limpar();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.listarTodos()
        );

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage()
        );
    }

    private SolucaoRequestDTO requestValido() {
        SolucaoComponenteRequestDTO componente = new SolucaoComponenteRequestDTO();
        componente.setProdutoId(PRODUTO_ID);
        componente.setQuantidade(new BigDecimal("500.000"));
        componente.setUnidadeMedida(UnidadeMedida.ML);

        SolucaoRequestDTO dto = new SolucaoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("  Solução Etanólica  ");
        dto.setDescricao("Solução de teste");
        dto.setRendimentoQuantidade(new BigDecimal("1.000"));
        dto.setRendimentoUnidade(UnidadeMedida.L);
        dto.setConcentracao("50%");
        dto.setInstrucoesPreparo("Misturar os componentes.");
        dto.setComponentes(List.of(componente));
        return dto;
    }

    private Solucao solucaoPersistida() {
        Solucao solucao = Solucao.builder()
                .id(10L)
                .publicId(SOLUCAO_ID)
                .unidade(unidade)
                .nome("Solução Etanólica")
                .rendimentoQuantidade(new BigDecimal("1.000"))
                .rendimentoUnidade(UnidadeMedida.L)
                .ativo(true)
                .build();

        SolucaoComponente componente = SolucaoComponente.builder()
                .id(11L)
                .publicId(COMPONENTE_ID)
                .solucao(solucao)
                .produto(produto)
                .quantidade(new BigDecimal("500.000"))
                .unidadeMedida(UnidadeMedida.ML)
                .build();

        solucao.getComponentes().add(componente);
        return solucao;
    }
}
