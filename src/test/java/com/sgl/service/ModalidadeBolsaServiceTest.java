package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.sgl.dto.request.ModalidadeBolsaRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ModalidadeBolsa;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.ModalidadeBolsaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ModalidadeBolsaServiceTest {

    private static final UUID UNIDADE_ID = UUID.fromString("a2000000-0000-0000-0000-000000000001");
    private static final UUID MODALIDADE_ID = UUID.fromString("a2000000-0000-0000-0000-000000000002");

    @Mock private ModalidadeBolsaRepository repository;
    @Mock private UnidadeRepository unidadeRepository;
    @InjectMocks private ModalidadeBolsaService service;

    private Unidade unidade;
    private ModalidadeBolsa modalidade;

    @BeforeEach
    void preparar() {
        unidade = Unidade.builder().id(1L).publicId(UNIDADE_ID).nome("CNPMF").sigla("CNPMF").build();
        modalidade = ModalidadeBolsa.builder()
                .id(3L).publicId(MODALIDADE_ID).unidade(unidade)
                .nome("Bolsa FAPESB").tipoBase(TipoBolsa.BOLSA_INSTITUCIONAL)
                .ativo(true).build();
        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void cadastraModalidadeNaUnidadeEReutilizaTipoBase() {
        ModalidadeBolsaRequestDTO dto = new ModalidadeBolsaRequestDTO();
        dto.setNome("  Bolsa FAPESB  ");
        dto.setTipoBase(TipoBolsa.BOLSA_INSTITUCIONAL);
        when(unidadeRepository.findByPublicId(UNIDADE_ID)).thenReturn(Optional.of(unidade));
        when(repository.save(any(ModalidadeBolsa.class))).thenAnswer(inv -> {
            ModalidadeBolsa entity = inv.getArgument(0);
            entity.setPublicId(MODALIDADE_ID);
            return entity;
        });

        var response = service.criar(dto);

        assertEquals("Bolsa FAPESB", response.getNome());
        assertEquals(UNIDADE_ID, response.getUnidadeId());
        assertEquals(TipoBolsa.BOLSA_INSTITUCIONAL, response.getTipoBase());
        assertTrue(response.getAtivo());
    }

    @Test
    void naoDuplicaNomeMesmoComCaixaDiferente() {
        ModalidadeBolsaRequestDTO dto = new ModalidadeBolsaRequestDTO();
        dto.setNome("bolsa fapesb");
        dto.setTipoBase(TipoBolsa.BOLSA_INSTITUCIONAL);
        when(unidadeRepository.findByPublicId(UNIDADE_ID)).thenReturn(Optional.of(unidade));
        when(repository.existsByUnidadeIdAndNomeIgnoreCase(1L, "bolsa fapesb")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verify(repository, never()).save(any(ModalidadeBolsa.class));
    }

    @Test
    void listaApenasAtivasDaUnidade() {
        when(repository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UNIDADE_ID))
                .thenReturn(List.of(modalidade));
        var lista = service.listarAtivas();
        assertEquals(1, lista.size());
        assertEquals(MODALIDADE_ID, lista.get(0).getId());
    }

    @Test
    void recusaModalidadeDeOutraUnidade() {
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.validarParaVinculo(MODALIDADE_ID, TipoBolsa.BOLSA_INSTITUCIONAL));
    }

    @Test
    void recusaModalidadeInativa() {
        modalidade.setAtivo(false);
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(modalidade));
        assertThrows(BusinessRuleException.class,
                () -> service.validarParaVinculo(MODALIDADE_ID, TipoBolsa.BOLSA_INSTITUCIONAL));
    }

    @Test
    void recusaClassificacaoBaseDiferente() {
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(modalidade));
        assertThrows(BusinessRuleException.class,
                () -> service.validarParaVinculo(MODALIDADE_ID, TipoBolsa.BOLSA_CAPES));
    }

    @Test
    void inativaSemApagarModalidadeUtilizada() {
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(modalidade));
        service.inativar(MODALIDADE_ID);
        assertFalse(modalidade.getAtivo());
        verify(repository, never()).delete(any(ModalidadeBolsa.class));
    }

    @Test
    void exigeUnidadeEmOperacao() {
        TenantContext.limpar();
        assertThrows(BusinessRuleException.class, service::listarAtivas);
    }

    @Test
    void impedeAlterarTipoBaseDaModalidade() {
        ModalidadeBolsaRequestDTO dto = new ModalidadeBolsaRequestDTO();
        dto.setNome("Bolsa FAPESB");
        dto.setTipoBase(TipoBolsa.BOLSA_CAPES);
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(modalidade));
        assertThrows(BusinessRuleException.class, () -> service.atualizar(MODALIDADE_ID, dto));
    }

    @Test
    void permiteReativarSemModificarVinculosHistoricos() {
        modalidade.setAtivo(false);
        ModalidadeBolsaRequestDTO dto = new ModalidadeBolsaRequestDTO();
        dto.setNome("Bolsa FAPESB");
        dto.setTipoBase(TipoBolsa.BOLSA_INSTITUCIONAL);
        dto.setAtivo(true);
        when(repository.findByPublicIdAndUnidadePublicId(MODALIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(modalidade));
        when(repository.save(any(ModalidadeBolsa.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.atualizar(MODALIDADE_ID, dto).getAtivo());
    }
}
