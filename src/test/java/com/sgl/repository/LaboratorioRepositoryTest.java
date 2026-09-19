package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do LaboratorioRepository usando @DataJpaTest com H2
 * real (sem mocks) — persiste entidades via TestEntityManager e valida os
 * métodos customizados (findByPublicId, findByPublicIdAndUnidadePublicId,
 * findByUnidadeId, findByUnidadePublicId e o findAll() sobrescrito que
 * filtra por tenant).
 *
 * Segue o mesmo padrão do UnidadeRepositoryTest e do PedidoRepositoryTest
 * (Batch R1):
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito — o @DataJpaTest não faz
 *   component scan de @Component comuns, só de @Entity/repositórios.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class LaboratorioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    // Garante que nenhum teste "vaze" um tenant definido em ThreadLocal para o próximo.
    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        Unidade unidade = Unidade.builder()
                .nome("Unidade " + sigla)
                .sigla(sigla)
                .build();
        return entityManager.persistAndFlush(unidade);
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        Laboratorio laboratorio = Laboratorio.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(laboratorio);
    }

    // --- findByPublicId -----------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("LB1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório de Química");

        Optional<Laboratorio> resultado = laboratorioRepository.findByPublicId(laboratorio.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("Laboratório de Química", resultado.get().getNome());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Laboratorio> resultado = laboratorioRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndUnidadePublicId ------------------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadePublicId() {
        Unidade unidade = criarUnidade("LB2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório de Física");

        Optional<Laboratorio> resultado = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(laboratorio.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComALaboratorio() {
        Unidade unidade = criarUnidade("LB3");
        Unidade outraUnidade = criarUnidade("LB4");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório de Biologia");

        Optional<Laboratorio> resultado = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(laboratorio.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant ---------------------------------------

    @Test
    void findAllDeveRetornarTodosOsLaboratoriosQuandoNenhumTenantEstaDefinido() {
        Unidade unidadeA = criarUnidade("LB5");
        Unidade unidadeB = criarUnidade("LB6");
        criarLaboratorio(unidadeA, "Laboratório A");
        criarLaboratorio(unidadeB, "Laboratório B");

        List<Laboratorio> resultado = laboratorioRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasOLaboratorioDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("LB7");
        Unidade unidadeB = criarUnidade("LB8");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório A");
        criarLaboratorio(unidadeB, "Laboratório B");

        TenantContext.definir(unidadeA.getPublicId());

        List<Laboratorio> resultado = laboratorioRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(laboratorioA.getId(), resultado.get(0).getId());
    }

    // --- findByUnidadeId -------------------------------------------------------

    @Test
    void deveEncontrarPorUnidadeId() {
        Unidade unidade = criarUnidade("LB9");
        criarLaboratorio(unidade, "Laboratório de Genética");

        List<Laboratorio> resultado = laboratorioRepository.findByUnidadeId(unidade.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeIdNaoTemLaboratorios() {
        Unidade unidade = criarUnidade("LB10");

        List<Laboratorio> resultado = laboratorioRepository.findByUnidadeId(unidade.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByUnidadePublicId --------------------------------------------------

    @Test
    void deveEncontrarPorUnidadePublicId() {
        Unidade unidade = criarUnidade("LB11");
        criarLaboratorio(unidade, "Laboratório de Microbiologia");

        List<Laboratorio> resultado = laboratorioRepository.findByUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemLaboratorios() {
        List<Laboratorio> resultado = laboratorioRepository.findByUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }
}
