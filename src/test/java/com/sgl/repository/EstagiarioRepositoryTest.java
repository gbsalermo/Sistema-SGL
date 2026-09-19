package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
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

import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do EstagiarioRepository usando @DataJpaTest com H2
 * real (sem mocks) — persiste entidades via TestEntityManager e valida
 * todos os métodos customizados do repositório.
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * Estagiario extends Usuario (herança JOINED — @Inheritance(JOINED) em
 * Usuario, cada Estagiario grava uma linha em "usuarios" e outra em
 * "estagiarios"). Diferente de Produto/Pedido/etc, Estagiario NÃO usa
 * Lombok @Builder — é criado com o construtor padrão + setters (herdados de
 * Usuario), então os inicializadores de campo Java (ex.: "ativo = true" em
 * Usuario) são aplicados normalmente.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class EstagiarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EstagiarioRepository estagiarioRepository;

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

    private Estagiario criarEstagiario(Unidade unidade, Laboratorio laboratorio, String email, boolean ativo) {
        Estagiario estagiario = new Estagiario();
        estagiario.setNome("Estagiário " + email);
        estagiario.setEmail(email);
        estagiario.setSenha("senha-123");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorio);
        estagiario.setAtivo(ativo);
        estagiario.setDataInicioEstagio(LocalDate.of(2026, 1, 1));
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        return entityManager.persistAndFlush(estagiario);
    }

    // --- findByPublicId ---------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("ES1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "estagiario1@exemplo.com", true);

        Optional<Estagiario> resultado = estagiarioRepository.findByPublicId(estagiario.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("estagiario1@exemplo.com", resultado.get().getEmail());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Estagiario> resultado = estagiarioRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findById (sobrescrito) ---------------------------------------------------

    @Test
    void deveEncontrarPorId() {
        Unidade unidade = criarUnidade("ES2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "estagiario2@exemplo.com", true);

        Optional<Estagiario> resultado = estagiarioRepository.findById(estagiario.getId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorIdInexistente() {
        Optional<Estagiario> resultado = estagiarioRepository.findById(-1L);

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndUnidadePublicId -------------------------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadePublicId() {
        Unidade unidade = criarUnidade("ES3");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "estagiario3@exemplo.com", true);

        Optional<Estagiario> resultado = estagiarioRepository
                .findByPublicIdAndUnidadePublicId(estagiario.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComOEstagiario() {
        Unidade unidade = criarUnidade("ES4");
        Unidade outraUnidade = criarUnidade("ES5");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "estagiario4@exemplo.com", true);

        Optional<Estagiario> resultado = estagiarioRepository
                .findByPublicIdAndUnidadePublicId(estagiario.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAll() com filtro de tenant -------------------------------------------------

    @Test
    void findAllDeveRetornarTodosOsEstagiariosQuandoNenhumTenantEstaDefinido() {
        Unidade unidadeA = criarUnidade("ES6");
        Unidade unidadeB = criarUnidade("ES7");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório A");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório B");
        criarEstagiario(unidadeA, laboratorioA, "a@exemplo.com", true);
        criarEstagiario(unidadeB, laboratorioB, "b@exemplo.com", true);

        List<Estagiario> resultado = estagiarioRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasOEstagiarioDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("ES8");
        Unidade unidadeB = criarUnidade("ES9");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório A");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório B");
        Estagiario estagiarioA = criarEstagiario(unidadeA, laboratorioA, "a2@exemplo.com", true);
        criarEstagiario(unidadeB, laboratorioB, "b2@exemplo.com", true);

        TenantContext.definir(unidadeA.getPublicId());

        List<Estagiario> resultado = estagiarioRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals(estagiarioA.getId(), resultado.get(0).getId());
    }

    // --- findByLaboratorioId -----------------------------------------------------------

    @Test
    void deveEncontrarPorLaboratorioId() {
        Unidade unidade = criarUnidade("ES10");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "lab1@exemplo.com", true);

        List<Estagiario> resultado = estagiarioRepository.findByLaboratorioId(laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemEstagiarios() {
        Unidade unidade = criarUnidade("ES11");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório sem estagiários");

        List<Estagiario> resultado = estagiarioRepository.findByLaboratorioId(laboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByUnidadePublicId -----------------------------------------------------------

    @Test
    void deveEncontrarPorUnidadePublicId() {
        Unidade unidade = criarUnidade("ES12");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "unid1@exemplo.com", true);

        List<Estagiario> resultado = estagiarioRepository.findByUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemEstagiarios() {
        List<Estagiario> resultado = estagiarioRepository.findByUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByUnidadePublicIdAndAtivoTrue -----------------------------------------------

    @Test
    void deveEncontrarPorUnidadePublicIdEAtivoTrue() {
        Unidade unidade = criarUnidade("ES13");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "ativo1@exemplo.com", true);
        criarEstagiario(unidade, laboratorio, "inativo1@exemplo.com", false);

        List<Estagiario> resultado = estagiarioRepository.findByUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertEquals(1, resultado.size());
        assertEquals("ativo1@exemplo.com", resultado.get(0).getEmail());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeCasaMasTodosOsEstagiariosEstaoInativos() {
        Unidade unidade = criarUnidade("ES14");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "inativo2@exemplo.com", false);

        List<Estagiario> resultado = estagiarioRepository.findByUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findByAtivoTrue -----------------------------------------------------------------

    @Test
    void deveEncontrarEstagiariosAtivos() {
        Unidade unidade = criarUnidade("ES15");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "ativo2@exemplo.com", true);
        criarEstagiario(unidade, laboratorio, "inativo3@exemplo.com", false);

        List<Estagiario> resultado = estagiarioRepository.findByAtivoTrue();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getAtivo());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaEstagiariosAtivos() {
        Unidade unidade = criarUnidade("ES16");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        criarEstagiario(unidade, laboratorio, "inativo4@exemplo.com", false);

        List<Estagiario> resultado = estagiarioRepository.findByAtivoTrue();

        assertTrue(resultado.isEmpty());
    }

    // --- existsByIdAndAtivoTrue --------------------------------------------------------

    @Test
    void deveExistirPorIdEAtivoTrue() {
        Unidade unidade = criarUnidade("ES17");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "existeativo@exemplo.com", true);

        assertTrue(estagiarioRepository.existsByIdAndAtivoTrue(estagiario.getId()));
    }

    @Test
    void naoDeveExistirPorIdEAtivoTrueQuandoEstagiarioEstaInativo() {
        Unidade unidade = criarUnidade("ES18");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Base");
        Estagiario estagiario = criarEstagiario(unidade, laboratorio, "existeinativo@exemplo.com", false);

        assertFalse(estagiarioRepository.existsByIdAndAtivoTrue(estagiario.getId()));
    }
}
