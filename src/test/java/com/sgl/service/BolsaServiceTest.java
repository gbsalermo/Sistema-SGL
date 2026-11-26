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

import com.sgl.dto.request.BolsaRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Bolsa;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.BolsaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class BolsaServiceTest {

    private static final UUID UNIDADE_ID = UUID.fromString("a2000000-0000-0000-0000-000000000001");
    private static final UUID BOLSA_ID = UUID.fromString("a2000000-0000-0000-0000-000000000002");

    @Mock private BolsaRepository repository;
    @Mock private UnidadeRepository unidadeRepository;
    @InjectMocks private BolsaService service;

    private Unidade unidade;
    private Bolsa bolsa;

    @BeforeEach
    void preparar() {
        unidade = Unidade.builder().id(1L).publicId(UNIDADE_ID).nome("CNPMF").sigla("CNPMF").build();
        bolsa = Bolsa.builder().id(3L).publicId(BOLSA_ID)
                .unidade(unidade).nome("Bolsa FAPESB").ativo(true).build();
        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void limpar() { TenantContext.limpar(); }

    @Test
    void cadastraBolsaSemClassificacaoEnum() {
        BolsaRequestDTO dto = new BolsaRequestDTO();
        dto.setNome("  Bolsa FAPESB  ");
        when(unidadeRepository.findByPublicId(UNIDADE_ID)).thenReturn(Optional.of(unidade));
        when(repository.save(any(Bolsa.class))).thenAnswer(inv -> {
            Bolsa entity = inv.getArgument(0);
            entity.setPublicId(BOLSA_ID);
            return entity;
        });

        var response = service.criar(dto);
        assertEquals("Bolsa FAPESB", response.getNome());
        assertEquals(UNIDADE_ID, response.getUnidadeId());
        assertTrue(response.getAtivo());
    }

    @Test
    void rejeitaDuplicidadeDeNomeNaMesmaUnidade() {
        BolsaRequestDTO dto = new BolsaRequestDTO();
        dto.setNome("bolsa fapesb");
        when(unidadeRepository.findByPublicId(UNIDADE_ID)).thenReturn(Optional.of(unidade));
        when(repository.existsByUnidadeIdAndNomeIgnoreCase(1L, "bolsa fapesb")).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verify(repository, never()).save(any(Bolsa.class));
    }

    @Test
    void listaAtivasDaUnidade() {
        when(repository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(UNIDADE_ID))
                .thenReturn(List.of(bolsa));
        assertEquals(BOLSA_ID, service.listarAtivas().get(0).getId());
    }

    @Test
    void recusaBolsaDeOutraUnidade() {
        when(repository.findByPublicIdAndUnidadePublicId(BOLSA_ID, UNIDADE_ID))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.validarEscolha(null, BOLSA_ID));
    }

    @Test
    void recusaBolsaInativa() {
        bolsa.setAtivo(false);
        when(repository.findByPublicIdAndUnidadePublicId(BOLSA_ID, UNIDADE_ID))
                .thenReturn(Optional.of(bolsa));
        assertThrows(BusinessRuleException.class,
                () -> service.validarEscolha(null, BOLSA_ID));
    }

    @Test
    void aceitaBolsaIndependenteSemEnum() {
        when(repository.findByPublicIdAndUnidadePublicId(BOLSA_ID, UNIDADE_ID))
                .thenReturn(Optional.of(bolsa));
        assertEquals(bolsa, service.validarEscolha(null, BOLSA_ID));
    }

    @Test
    void aceitaContratoLegadoSemBolsaCadastrada() {
        assertNull(service.validarEscolha(TipoBolsa.BOLSA_CNPQ, null));
    }

    @Test
    void exigeUmaUnicaOrigem() {
        assertThrows(BusinessRuleException.class, () -> service.validarEscolha(null, null));
        assertThrows(BusinessRuleException.class,
                () -> service.validarEscolha(TipoBolsa.BOLSA_CAPES, BOLSA_ID));
        verify(repository, never()).findByPublicIdAndUnidadePublicId(any(), any());
    }

    @Test
    void renomeiaEReativaSemApagarHistorico() {
        bolsa.setAtivo(false);
        BolsaRequestDTO dto = new BolsaRequestDTO();
        dto.setNome("Nova Bolsa FAPESB");
        dto.setAtivo(true);
        when(repository.findByPublicIdAndUnidadePublicId(BOLSA_ID, UNIDADE_ID))
                .thenReturn(Optional.of(bolsa));
        when(repository.save(any(Bolsa.class))).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.atualizar(BOLSA_ID, dto).getAtivo());
        assertEquals("Nova Bolsa FAPESB", bolsa.getNome());
    }

    @Test
    void inativaSemExcluirRegistro() {
        when(repository.findByPublicIdAndUnidadePublicId(BOLSA_ID, UNIDADE_ID))
                .thenReturn(Optional.of(bolsa));
        service.inativar(BOLSA_ID);
        assertFalse(bolsa.getAtivo());
        verify(repository, never()).delete(any(Bolsa.class));
    }
}
