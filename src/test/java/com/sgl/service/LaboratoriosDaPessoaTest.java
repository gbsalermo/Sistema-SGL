package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.LaboratorioRepository;

class LaboratoriosDaPessoaTest {

    private final LaboratorioRepository repository = mock(LaboratorioRepository.class);
    private final LaboratoriosDaPessoa resolvedor = new LaboratoriosDaPessoa(repository);
    private final Unidade unidade = unidade();

    private static Unidade unidade() {
        Unidade u = new Unidade();
        u.setId(1L);
        u.setPublicId(UUID.randomUUID());
        return u;
    }

    private Laboratorio laboratorio(String nome, boolean ativo) {
        Laboratorio l = new Laboratorio();
        l.setPublicId(UUID.randomUUID());
        l.setNome(nome);
        l.setAtivo(ativo);
        l.setUnidade(unidade);
        when(repository.findByPublicIdAndUnidadePublicId(l.getPublicId(), unidade.getPublicId()))
                .thenReturn(Optional.of(l));
        return l;
    }

    @Test
    void listaVaziaParaPerfilOperacionalDa400() {
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> resolvedor.resolver(List.of(), unidade, Perfil.TECNICO, Set.of()));
        assertEquals("Informe ao menos um laboratório.", e.getMessage());
        assertThrows(BusinessRuleException.class,
                () -> resolvedor.resolver(null, unidade, Perfil.ESTAGIARIO, Set.of()));
    }

    @Test
    void gestaoPodeFicarSemLaboratorio() {
        for (Perfil perfil : List.of(Perfil.GESTOR, Perfil.ADMINISTRADOR, Perfil.ADMINISTRADOR_GERAL)) {
            assertTrue(resolvedor.resolver(null, unidade, perfil, Set.of()).isEmpty());
            assertTrue(resolvedor.resolver(List.of(), unidade, perfil, Set.of()).isEmpty());
        }
    }

    @Test
    void repetidoDa400() {
        Laboratorio a = laboratorio("A", true);
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> resolvedor.resolver(List.of(a.getPublicId(), a.getPublicId()), unidade,
                        Perfil.TECNICO, Set.of()));
        assertEquals("Laboratório repetido na lista.", e.getMessage());
    }

    @Test
    void outraUnidadeDa404() {
        UUID desconhecido = UUID.randomUUID();
        when(repository.findByPublicIdAndUnidadePublicId(desconhecido, unidade.getPublicId()))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> resolvedor.resolver(List.of(desconhecido), unidade, Perfil.TECNICO, Set.of()));
    }

    @Test
    void acrescentarInativoDa400() {
        Laboratorio inativo = laboratorio("Lab Velho", false);
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> resolvedor.resolver(List.of(inativo.getPublicId()), unidade, Perfil.TECNICO, Set.of()));
        assertEquals("O laboratório Lab Velho está inativo.", e.getMessage());
    }

    @Test
    void manterInativoJaPresenteEhPermitido() {
        Laboratorio inativo = laboratorio("Lab Velho", false);
        Set<Laboratorio> resolvidos = resolvedor.resolver(List.of(inativo.getPublicId()), unidade,
                Perfil.TECNICO, Set.of(inativo));
        assertEquals(Set.of(inativo), resolvidos);
    }

    @Test
    void devolveNaOrdemRecebida() {
        Laboratorio a = laboratorio("A", true);
        Laboratorio b = laboratorio("B", true);
        Laboratorio c = laboratorio("C", true);
        Set<Laboratorio> resolvidos = resolvedor.resolver(
                List.of(c.getPublicId(), a.getPublicId(), b.getPublicId()), unidade, Perfil.TECNICO, Set.of());
        assertInstanceOf(LinkedHashSet.class, resolvidos);
        assertEquals(List.of(c, a, b), List.copyOf(resolvidos));
    }
}
