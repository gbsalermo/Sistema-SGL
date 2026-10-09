package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ModeloSolucaoServiceTest {
    static final UUID UNIDADE = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID OUTRA = UUID.fromString("00000000-0000-0000-0000-000000000202");
    static final UUID PRODUTO = UUID.fromString("00000000-0000-0000-0000-000000000303");
    @Mock ModeloSolucaoRepository repository;
    @Mock UnidadeRepository unidadeRepository;
    @Mock ProdutoRepository produtoRepository;
    @InjectMocks ModeloSolucaoService service;
    Unidade unidade;
    Produto produto;

    @BeforeEach
    void preparar() {
        TenantContext.definir(UNIDADE);
        unidade = Unidade.builder().id(1L).publicId(UNIDADE).nome("Unidade").sigla("UNI").build();
        produto = Produto.builder().id(3L).publicId(PRODUTO).nome("Água")
            .unidadeMedida(UnidadeMedida.L).ativo(true).build();
    }
    @AfterEach void limpar() { TenantContext.limpar(); }

    private ModeloSolucaoRequestDTO dto(UnidadeMedida medida) {
        return new ModeloSolucaoRequestDTO(UNIDADE, "Solução padrão", "Exemplo", "Preparar no laboratório",
            List.of(new ComponenteModeloSolucaoRequestDTO(PRODUTO, new BigDecimal("500"), medida)), true);
    }
    private void baseCriacao() {
        when(unidadeRepository.findByPublicId(UNIDADE)).thenReturn(Optional.of(unidade));
        when(produtoRepository.pertenceAUnidade(PRODUTO, UNIDADE)).thenReturn(true);
        when(produtoRepository.findByPublicId(PRODUTO)).thenReturn(Optional.of(produto));
    }

    @Test
    void criaCatalogoEConverteQuantidadeParaCanonica() {
        baseCriacao();
        when(repository.save(any(ModeloSolucao.class))).thenAnswer(inv -> inv.getArgument(0));
        var resposta = service.criar(dto(UnidadeMedida.ML));
        assertEquals("Solução padrão", resposta.getNome());
        assertEquals(UNIDADE, resposta.getUnidadeId());
        assertEquals(1, resposta.getComponentes().size());
        assertEquals(0, resposta.getComponentes().get(0).getQuantidadeCanonica()
            .compareTo(new BigDecimal("0.5")));
        verify(repository).save(any(ModeloSolucao.class));
    }

    @Test
    void bloqueiaMisturaDeDimensoesIncompativeis() {
        baseCriacao();
        assertThrows(BusinessRuleException.class, () -> service.criar(dto(UnidadeMedida.G)));
        verify(repository, never()).save(any(ModeloSolucao.class));
    }

    @Test
    void impedeCatalogoDeOutraUnidade() {
        ModeloSolucaoRequestDTO dto = dto(UnidadeMedida.ML);
        dto.setUnidadeId(OUTRA);
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verifyNoInteractions(repository, unidadeRepository, produtoRepository);
    }

    @Test
    void exigeTenantAtivo() {
        TenantContext.limpar();
        assertThrows(BusinessRuleException.class, () -> service.criar(dto(UnidadeMedida.ML)));
        verifyNoInteractions(repository, unidadeRepository, produtoRepository);
    }

    @Test
    void bloqueiaProdutosRepetidos() {
        ModeloSolucaoRequestDTO dto = dto(UnidadeMedida.ML);
        dto.setComponentes(List.of(dto.getComponentes().get(0), dto.getComponentes().get(0)));
        baseCriacao();
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verify(repository, never()).save(any(ModeloSolucao.class));
    }

    @Test
    void inativaSemApagarCatalogo() {
        UUID id = UUID.randomUUID();
        ModeloSolucao modelo = ModeloSolucao.builder().id(6L).publicId(id)
            .unidade(unidade).nome("Solução padrão").ativo(true).build();
        when(repository.findByPublicIdAndUnidadePublicId(id, UNIDADE)).thenReturn(Optional.of(modelo));
        service.inativar(id);
        assertEquals(false, modelo.getAtivo());
        verify(repository, never()).delete(any(ModeloSolucao.class));
    }
}
