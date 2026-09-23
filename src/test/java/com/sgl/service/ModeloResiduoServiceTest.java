package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.ComponenteModeloResiduoRequestDTO;
import com.sgl.dto.request.ModeloResiduoRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.ModeloResiduo;
import com.sgl.model.Unidade;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.ModeloResiduoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ModeloResiduoServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000101");

    private static final UUID OUTRA_UNIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");

    private static final UUID CLASSE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000110");

    private static final UUID MODELO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000130");

    @Mock
    private ModeloResiduoRepository modeloResiduoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private ClasseResiduoRepository classeResiduoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ModeloResiduoService service;

    private Unidade unidade;
    private ClasseResiduo classe;

    @BeforeEach
    void preparar() {

        TenantContext.definir(UNIDADE_ID);

        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("CNPMF")
                .sigla("CNPMF")
                .build();

        classe = ClasseResiduo.builder()
                .id(10L)
                .publicId(CLASSE_ID)
                .unidade(unidade)
                .codigo("A")
                .descricao("Classe A")
                .ativo(true)
                .build();
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarModeloNaUnidadeDoTenant() {

        ModeloResiduoRequestDTO dto = dtoValido();

        prepararCriacaoValida(dto);

        when(modeloResiduoRepository.save(any(ModeloResiduo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.criar(dto);

        assertEquals("Resíduo padrão", response.getNome());
        assertEquals(UNIDADE_ID, response.getUnidadeId());
        assertEquals(1, response.getClasses().size());
        assertEquals(1, response.getComponentes().size());
        assertEquals(
                "Solução tampão",
                response.getComponentes().get(0).getNomeComponente()
        );
        assertTrue(response.getAtivo());

        verify(modeloResiduoRepository)
                .save(any(ModeloResiduo.class));
    }

    @Test
    void deveExigirTenantParaCriarModelo() {

        TenantContext.limpar();

        ModeloResiduoRequestDTO dto = dtoValido();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("X-SGL-Unidade-Id")
        );

        verify(unidadeRepository, never())
                .findByPublicId(any());
    }

    @Test
    void deveRejeitarCriacaoEmOutraUnidade() {

        ModeloResiduoRequestDTO dto = dtoValido();
        dto.setUnidadeId(OUTRA_UNIDADE_ID);

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("outra unidade")
        );

        verify(unidadeRepository, never())
                .findByPublicId(any());
    }

    @Test
    void listarTodosDeveExigirTenant() {

        TenantContext.limpar();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.listarTodos()
                );

        assertTrue(
                erro.getMessage()
                        .contains("X-SGL-Unidade-Id")
        );
    }

    @Test
    void listarTodosDeveConsultarSomenteTenantAtual() {

        when(
                modeloResiduoRepository
                        .findByUnidadePublicIdOrderByNomeAsc(
                                UNIDADE_ID
                        )
        ).thenReturn(List.of());

        var resultado = service.listarTodos();

        assertTrue(resultado.isEmpty());

        verify(modeloResiduoRepository)
                .findByUnidadePublicIdOrderByNomeAsc(
                        UNIDADE_ID
                );
    }

    @Test
    void buscarModeloDeOutraUnidadeDeveParecerInexistente() {

        when(
                modeloResiduoRepository
                        .findByPublicIdAndUnidadePublicId(
                                MODELO_ID,
                                UNIDADE_ID
                        )
        ).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.buscarPorId(MODELO_ID)
        );
    }

    @Test
    void deveRejeitarNomeDuplicadoNaMesmaUnidade() {

        ModeloResiduoRequestDTO dto = dtoValido();

        when(
                unidadeRepository
                        .findByPublicId(UNIDADE_ID)
        ).thenReturn(Optional.of(unidade));

        when(
                modeloResiduoRepository
                        .existsByUnidadeIdAndNomeIgnoreCase(
                                unidade.getId(),
                                "Resíduo padrão"
                        )
        ).thenReturn(true);

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("Já existe")
        );
    }

    @Test
    void deveExigirDescricaoQuandoTratamentoForRealizado() {

        ModeloResiduoRequestDTO dto = dtoValido();

        dto.setTratamentoRealizado(true);
        dto.setDescricaoTratamento(" ");

        prepararUnidadeENome();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("descrição do tratamento")
        );
    }

    @Test
    void deveRejeitarRiscosVazios() {

        ModeloResiduoRequestDTO dto = dtoValido();
        dto.setRiscos(Set.of());

        prepararUnidadeENome();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("risco")
        );
    }

    @Test
    void deveRejeitarSegurancaVazia() {

        ModeloResiduoRequestDTO dto = dtoValido();
        dto.setMedidasSeguranca(Set.of());

        prepararUnidadeENome();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("medida de segurança")
        );
    }

    @Test
    void deveExigirObservacaoQuandoSegurancaContemOutro() {

        ModeloResiduoRequestDTO dto = dtoValido();

        dto.setMedidasSeguranca(
                Set.of(MedidaSeguranca.OUTRO)
        );

        dto.setObservacaoSeguranca(null);

        prepararUnidadeENome();

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("OUTRO")
        );
    }

    @Test
    void deveRejeitarClasseDeOutraUnidade() {

        ModeloResiduoRequestDTO dto = dtoValido();

        prepararUnidadeENome();

        when(
                classeResiduoRepository
                        .findByPublicIdInAndUnidadePublicId(
                                dto.getClassesIds(),
                                UNIDADE_ID
                        )
        ).thenReturn(List.of());

        BusinessRuleException erro =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.criar(dto)
                );

        assertTrue(
                erro.getMessage()
                        .contains("inválidas para esta unidade")
        );
    }

    @Test
    void deveRejeitarClasseInativa() {

        ModeloResiduoRequestDTO dto = dtoValido();

        classe.setAtivo(false);

        prepararUnidadeENome();

        when(
                classeResiduoRepository
                        .findByPublicIdInAndUnidadePublicId(
                                dto.getClassesIds(),
                                UNIDADE_ID
                        )
        ).thenReturn(List.of(classe));

        assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto)
        );
    }

    @Test
    void deletarDeveSomenteInativarModelo() {

        ModeloResiduo modelo =
                ModeloResiduo.builder()
                        .id(30L)
                        .publicId(MODELO_ID)
                        .unidade(unidade)
                        .nome("Modelo")
                        .ativo(true)
                        .build();

        when(
                modeloResiduoRepository
                        .findByPublicIdAndUnidadePublicId(
                                MODELO_ID,
                                UNIDADE_ID
                        )
        ).thenReturn(Optional.of(modelo));

        service.deletar(MODELO_ID);

        assertFalse(modelo.getAtivo());
    }

    private void prepararCriacaoValida(
            ModeloResiduoRequestDTO dto) {

        prepararUnidadeENome();

        when(
                classeResiduoRepository
                        .findByPublicIdInAndUnidadePublicId(
                                dto.getClassesIds(),
                                UNIDADE_ID
                        )
        ).thenReturn(List.of(classe));
    }

    private void prepararUnidadeENome() {

        when(
                unidadeRepository
                        .findByPublicId(UNIDADE_ID)
        ).thenReturn(Optional.of(unidade));

        when(
                modeloResiduoRepository
                        .existsByUnidadeIdAndNomeIgnoreCase(
                                unidade.getId(),
                                "Resíduo padrão"
                        )
        ).thenReturn(false);
    }

    private ModeloResiduoRequestDTO dtoValido() {

        ModeloResiduoRequestDTO dto =
                new ModeloResiduoRequestDTO();

        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("  Resíduo padrão  ");
        dto.setDescricao("Modelo reutilizável");
        dto.setProcessoOrigem("Processo laboratorial");
        dto.setEstadoFisico(EstadoFisicoResiduo.LIQUIDO);
        dto.setTratamentoRealizado(false);
        dto.setDescricaoTratamento(null);
        dto.setRecipiente("Bombona");
        dto.setUnidadeMedida(UnidadeMedida.ML);
        dto.setNivelRisco(NivelRisco.BAIXO);

        dto.setRiscos(
                Set.of(TipoRisco.IRRITANTE)
        );

        dto.setClassesIds(
                Set.of(CLASSE_ID)
        );

        dto.setMedidasSeguranca(
                Set.of(MedidaSeguranca.LUVAS)
        );

        dto.setObservacaoSeguranca(null);
        dto.setAtivo(true);

        ComponenteModeloResiduoRequestDTO componente =
                new ComponenteModeloResiduoRequestDTO();

        componente.setNomeComponente("Solução tampão");
        componente.setPrincipal(true);

        componente.setConcentracaoOuQuantidade(
                "100 mL"
        );

        dto.setComponentes(
                List.of(componente)
        );

        return dto;
    }
}
