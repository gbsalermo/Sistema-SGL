package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;

class ProjetoTest {

    @Test
    void pertenceAoLaboratorioComparaPorId() {
        Laboratorio a = laboratorio(1L, "A", true);
        Projeto projeto = projeto(a);

        assertTrue(projeto.pertenceAoLaboratorio(laboratorio(1L, "Outra instância de A", true)));
        assertFalse(projeto.pertenceAoLaboratorio(laboratorio(2L, "B", true)));
        assertFalse(projeto.pertenceAoLaboratorio(null));
    }

    @Test
    void resolveUnicoLaboratorioQuandoNaoInformado() {
        Laboratorio a = laboratorio(1L, "A", true);

        assertSame(a, projeto(a).resolverLaboratorioOperacional(null));
    }

    @Test
    void exigeEscolhaQuandoHaVariosLaboratorios() {
        Projeto projeto = projeto(laboratorio(1L, "A", true), laboratorio(2L, "B", true));

        BusinessRuleException erro = assertThrows(
                BusinessRuleException.class,
                () -> projeto.resolverLaboratorioOperacional(null)
        );

        assertEquals("Informe o laboratório: o projeto está em mais de um laboratório.", erro.getMessage());
    }

    @Test
    void aceitaLaboratorioInformadoDoProjeto() {
        Laboratorio b = laboratorio(2L, "B", true);
        Projeto projeto = projeto(laboratorio(1L, "A", true), b);

        assertSame(b, projeto.resolverLaboratorioOperacional(b.getPublicId()));
    }

    @Test
    void recusaLaboratorioForaDoProjeto() {
        Projeto projeto = projeto(laboratorio(1L, "A", true));
        Laboratorio c = laboratorio(3L, "C", true);

        BusinessRuleException erro = assertThrows(
                BusinessRuleException.class,
                () -> projeto.resolverLaboratorioOperacional(c.getPublicId())
        );

        assertEquals("O laboratório informado não pertence ao projeto.", erro.getMessage());
    }

    @Test
    void recusaLaboratorioInformadoInativo() {
        Laboratorio b = laboratorio(2L, "B", false);
        Projeto projeto = projeto(laboratorio(1L, "A", true), b);

        assertThrows(
                BusinessRuleException.class,
                () -> projeto.resolverLaboratorioOperacional(b.getPublicId())
        );
    }

    @Test
    void ignoraInativoNaInferencia() {
        Laboratorio a = laboratorio(1L, "A", true);
        Projeto projeto = projeto(a, laboratorio(2L, "B", false));

        assertSame(a, projeto.resolverLaboratorioOperacional(null));
    }

    @Test
    void laboratoriosOrdenadosPorNome() {
        Projeto projeto = projeto(laboratorio(1L, "Zeta", true), laboratorio(2L, "Alfa", true));

        List<String> nomes = projeto.laboratoriosOrdenados().stream()
                .map(Laboratorio::getNome)
                .toList();

        assertEquals(List.of("Alfa", "Zeta"), nomes);
    }

    @Test
    void possuiLaboratorioAtivoBastaUm() {
        assertTrue(projeto(laboratorio(1L, "A", false), laboratorio(2L, "B", true)).possuiLaboratorioAtivo());
        assertFalse(projeto(laboratorio(1L, "A", false), laboratorio(2L, "B", false)).possuiLaboratorioAtivo());
        assertFalse(projeto().possuiLaboratorioAtivo());
    }

    private static Laboratorio laboratorio(Long id, String nome, boolean ativo) {
        return Laboratorio.builder()
                .id(id)
                .publicId(UUID.randomUUID())
                .nome(nome)
                .ativo(ativo)
                .build();
    }

    private static Projeto projeto(Laboratorio... laboratorios) {
        return Projeto.builder()
                .nome("Projeto")
                .laboratorios(new LinkedHashSet<>(Set.of(laboratorios)))
                .build();
    }
}
