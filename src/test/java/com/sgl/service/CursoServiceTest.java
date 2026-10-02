package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import com.sgl.dto.request.CursoRequestDTO;
import com.sgl.dto.response.CursoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Curso;
import com.sgl.model.Unidade;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("72000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_ID =
            UUID.fromString("72000000-0000-0000-0000-000000000002");
    private static final UUID CURSO_ID =
            UUID.fromString("72000000-0000-0000-0000-000000000003");

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private CursoService service;

    private Unidade unidade;
    private Curso curso;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        curso = Curso.builder()
                .id(2L)
                .publicId(CURSO_ID)
                .unidade(unidade)
                .nome("Engenharia Agronômica")
                .ativo(true)
                .build();

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarCursoNoCatalogoDaUnidade() {
        CursoRequestDTO dto = new CursoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("  Engenharia Ambiental  ");

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(cursoRepository
                .existsByUnidadeIdAndNomeIgnoreCase(
                        unidade.getId(), "Engenharia Ambiental"))
                .thenReturn(false);
        when(cursoRepository.save(any(Curso.class)))
                .thenAnswer(invocation -> {
                    Curso salvo = invocation.getArgument(0);
                    salvo.setId(3L);
                    salvo.setPublicId(CURSO_ID);
                    return salvo;
                });

        CursoResponseDTO resultado = service.criar(dto);

        assertEquals(CURSO_ID, resultado.getId());
        assertEquals("Engenharia Ambiental", resultado.getNome());
        assertEquals(UNIDADE_ID, resultado.getUnidadeId());
        assertEquals(true, resultado.getAtivo());
    }

    @Test
    void deveBloquearNomeDuplicadoNaMesmaUnidade() {
        CursoRequestDTO dto = new CursoRequestDTO();
        dto.setUnidadeId(UNIDADE_ID);
        dto.setNome("Engenharia Agronômica");

        when(unidadeRepository.findByPublicId(UNIDADE_ID))
                .thenReturn(Optional.of(unidade));
        when(cursoRepository
                .existsByUnidadeIdAndNomeIgnoreCase(
                        unidade.getId(), "Engenharia Agronômica"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto));

        assertEquals(
                "Já existe um curso com este nome na unidade.",
                ex.getMessage());

        verify(cursoRepository, never()).save(any(Curso.class));
    }

    @Test
    void deveBloquearCriacaoDeCursoEmOutraUnidade() {
        Unidade outra = Unidade.builder()
                .id(10L)
                .publicId(OUTRA_UNIDADE_ID)
                .nome("Outra Unidade")
                .sigla("OU")
                .build();

        CursoRequestDTO dto = new CursoRequestDTO();
        dto.setUnidadeId(OUTRA_UNIDADE_ID);
        dto.setNome("Biologia");

        when(unidadeRepository.findByPublicId(OUTRA_UNIDADE_ID))
                .thenReturn(Optional.of(outra));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.criar(dto));

        assertEquals(
                "A operação não pode acessar dados de outra unidade.",
                ex.getMessage());
    }

    @Test
    void deveListarSomenteCursosAtivosDaUnidadeAtual() {
        when(cursoRepository
                .findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(
                        UNIDADE_ID))
                .thenReturn(List.of(curso));

        List<CursoResponseDTO> resultado = service.listarAtivos();

        assertEquals(1, resultado.size());
        assertEquals(CURSO_ID, resultado.get(0).getId());
    }

    @Test
    void deveInativarCursoSemExcluirHistorico() {
        when(cursoRepository
                .findByPublicIdAndUnidadePublicId(
                        CURSO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(curso));

        service.deletar(CURSO_ID);

        assertFalse(curso.getAtivo());
        verify(cursoRepository, never()).delete(any(Curso.class));
    }

    @Test
    void deveExigirTenantAtivo() {
        TenantContext.limpar();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                service::listarAtivos);

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());
    }
}
