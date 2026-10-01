package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import com.sgl.dto.request.CulturaRequestDTO;
import com.sgl.dto.response.CulturaResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Cultura;
import com.sgl.model.Unidade;
import com.sgl.repository.CulturaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class CulturaServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID CULTURA_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000003");

    @Mock
    private CulturaRepository culturaRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private CulturaService service;

    private Unidade unidade;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarCulturaNaUnidadeAtual() {
        CulturaRequestDTO dto = new CulturaRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("  Mandioca  ");

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(culturaRepository
                .existsByUnidadeIdAndNomeIgnoreCase(
                        unidade.getId(), "Mandioca"))
                .thenReturn(false);
        when(culturaRepository.save(any(Cultura.class)))
                .thenAnswer(invocation -> {
                    Cultura cultura = invocation.getArgument(0);
                    cultura.setId(2L);
                    cultura.setPublicId(CULTURA_ID);
                    return cultura;
                });

        CulturaResponseDTO resultado = service.criar(dto);

        assertEquals(CULTURA_ID, resultado.getId());
        assertEquals("Mandioca", resultado.getNome());
        assertEquals(Boolean.TRUE, resultado.getAtivo());
        verify(culturaRepository).save(any(Cultura.class));
    }

    @Test
    void deveListarSomenteCulturasAtivasDaUnidade() {
        Cultura cultura = Cultura.builder()
                .id(2L)
                .publicId(CULTURA_ID)
                .unidade(unidade)
                .nome("Citros")
                .ativo(true)
                .build();

        when(culturaRepository
                .findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(
                        UNIDADE_ID))
                .thenReturn(List.of(cultura));

        List<CulturaResponseDTO> resultado = service.listarAtivos();

        assertEquals(1, resultado.size());
        assertEquals("Citros", resultado.get(0).getNome());
    }

    @Test
    void deveBloquearCriacaoEmOutraUnidade() {
        CulturaRequestDTO dto = new CulturaRequestDTO();
        dto.setUnidadeId(OUTRA_UNIDADE_ID);
        dto.setNome("Banana");

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
                () -> service.criar(dto));

        assertEquals(
                "A operação não pode acessar dados de outra unidade.",
                ex.getMessage());
    }
}
