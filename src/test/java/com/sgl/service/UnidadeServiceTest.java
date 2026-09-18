package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;

/**
 * Testes unitários de {@link UnidadeService}. O repositório é mockado (Mockito puro,
 * sem subir contexto Spring) porque a regra de negócio de Unidade não depende de nada
 * além do UnidadeRepository - segue o padrão já usado em PedidoServiceTest/MovimentacaoEstoqueServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private UnidadeService unidadeService;

    private Unidade unidade;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");
    }

    @Test
    void deveCriarUnidadeQuandoSiglaNaoExiste() {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        dto.setSigla("CNPMF");
        dto.setNome("Embrapa Mandioca e Fruticultura");

        when(unidadeRepository.existsBySigla("CNPMF")).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

        UnidadeResponseDTO resultado = unidadeService.criar(dto);

        assertEquals("CNPMF", resultado.getSigla());
        verify(unidadeRepository).save(any(Unidade.class));
    }

    @Test
    void deveRejeitarCriacaoQuandoSiglaJaExiste() {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        dto.setSigla("CNPMF");

        when(unidadeRepository.existsBySigla("CNPMF")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> unidadeService.criar(dto));

        assertEquals("Já existe uma unidade com esta sigla.", ex.getMessage());
        // Garante que, ao barrar a duplicidade, o service nunca chega a persistir nada.
        verify(unidadeRepository, never()).save(any());
    }

    @Test
    void deveListarTodasAsUnidades() {
        when(unidadeRepository.findAll()).thenReturn(List.of(unidade));

        List<UnidadeResponseDTO> resultado = unidadeService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarUnidadePorId() {
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));

        UnidadeResponseDTO resultado = unidadeService.buscarPorId(UNIDADE_PUBLIC_ID);

        // UnidadeResponseDTO expõe o publicId da entidade através do getter "id" (não "publicId").
        assertEquals(UNIDADE_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoUnidadeNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        when(unidadeRepository.findByPublicId(idInexistente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> unidadeService.buscarPorId(idInexistente));
    }

    @Test
    void deveAtualizarUnidadeQuandoNovaSiglaNaoConflita() {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        dto.setSigla("CNPMF2");
        dto.setNome("Nome Atualizado");

        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        // atualizar() checa unicidade com existsBySiglaAndIdNot (exclui a própria unidade da checagem),
        // diferente de criar() que usa existsBySigla simples.
        when(unidadeRepository.existsBySiglaAndIdNot("CNPMF2", unidade.getId())).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

        unidadeService.atualizar(UNIDADE_PUBLIC_ID, dto);

        verify(unidadeRepository).save(any(Unidade.class));
    }

    @Test
    void deveRejeitarAtualizacaoQuandoNovaSiglaJaExisteEmOutraUnidade() {
        UnidadeRequestDTO dto = new UnidadeRequestDTO();
        dto.setSigla("OUTRA");

        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(unidadeRepository.existsBySiglaAndIdNot("OUTRA", unidade.getId())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> unidadeService.atualizar(UNIDADE_PUBLIC_ID, dto));

        assertEquals("Já existe uma unidade com esta sigla.", ex.getMessage());
    }

    @Test
    void deveDeletarUnidadeExistente() {
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));

        unidadeService.deletar(UNIDADE_PUBLIC_ID);

        verify(unidadeRepository).delete(unidade);
    }
}
