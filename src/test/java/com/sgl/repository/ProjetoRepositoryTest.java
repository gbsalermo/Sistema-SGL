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
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do ProjetoRepository usando @DataJpaTest com H2 real
 * (sem mocks). Segue o mesmo padrão de UnidadeRepositoryTest/UsuarioRepositoryTest:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL do findAll() sobrescrito.
 *
 * Diferente de UsuarioRepository, aqui Projeto.laboratorio é @JoinColumn(nullable
 * = false) e Laboratorio.unidade também é nullable = false — ou seja, o INNER
 * JOIN implícito gerado pelo JPQL (projeto.laboratorio.unidade.publicId) nunca
 * exclui projetos "órfãos" sem laboratório/unidade, porque essa combinação não é
 * permitida pelo modelo.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class ProjetoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjetoRepository projetoRepository;

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

    private Projeto criarProjeto(Laboratorio laboratorio, String nome, boolean ativo) {
        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .nome(nome)
                .ativo(ativo)
                .build();
        return entityManager.persistAndFlush(projeto);
    }

    @Test
    void devePersistirCamposExpandidosDoProjeto() {
        Unidade unidade = criarUnidade("PX1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Expandido");

        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .nome("Projeto Expandido")
                .codigoSeg("94.94.94.001.01.00")
                .status(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO)
                .possuiRecursoExterno(true)
                .empresaRecursoExterno("Empresa Teste")
                .ativo(true)
                .build();

        entityManager.persistAndFlush(projeto);
        entityManager.clear();

        Projeto resultado = projetoRepository
                .findByPublicId(projeto.getPublicId())
                .orElseThrow();

        assertEquals("94.94.94.001.01.00", resultado.getCodigoSeg());
        assertEquals(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE, resultado.getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO, resultado.getSituacaoExecucao());
        assertTrue(resultado.getPossuiRecursoExterno());
        assertEquals("Empresa Teste", resultado.getEmpresaRecursoExterno());
    }

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("PU1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório A");
        Projeto projeto = criarProjeto(laboratorio, "Projeto A", true);

        Optional<Projeto> resultado = projetoRepository.findByPublicId(projeto.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("Projeto A", resultado.get().getNome());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Projeto> resultado = projetoRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoLaboratorioQuandoPertenceAUnidade() {
        Unidade unidade = criarUnidade("PU2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório B");
        Projeto projeto = criarProjeto(laboratorio, "Projeto B", true);

        Optional<Projeto> resultado = projetoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(projeto.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdEUnidadeDoLaboratorioQuandoPertenceAOutraUnidade() {
        Unidade unidadeDoProjeto = criarUnidade("PU3");
        Unidade outraUnidade = criarUnidade("PU4");
        Laboratorio laboratorio = criarLaboratorio(unidadeDoProjeto, "Laboratório C");
        Projeto projeto = criarProjeto(laboratorio, "Projeto C", true);

        Optional<Projeto> resultado = projetoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(projeto.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorLaboratorioId() {
        Unidade unidade = criarUnidade("PL1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório D");
        criarProjeto(laboratorio, "Projeto D", true);

        List<Projeto> resultado = projetoRepository.findByLaboratorioId(laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemProjetos() {
        Unidade unidade = criarUnidade("PL2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório vazio");

        List<Projeto> resultado = projetoRepository.findByLaboratorioId(laboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicIdDoLaboratorio() {
        Unidade unidade = criarUnidade("PU5");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório E");
        criarProjeto(laboratorio, "Projeto E", true);

        List<Projeto> resultado = projetoRepository.findByLaboratorioUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoCasaComLaboratorio() {
        List<Projeto> resultado = projetoRepository.findByLaboratorioUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarSomenteProjetosAtivos() {
        Unidade unidade = criarUnidade("PA1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório F");
        criarProjeto(laboratorio, "Projeto Ativo", true);
        criarProjeto(laboratorio, "Projeto Inativo", false);

        List<Projeto> resultado = projetoRepository.findByAtivoTrue();

        assertEquals(1, resultado.size());
        assertEquals("Projeto Ativo", resultado.get(0).getNome());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaProjetosAtivos() {
        Unidade unidade = criarUnidade("PA2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório G");
        criarProjeto(laboratorio, "Projeto Inativo", false);

        List<Projeto> resultado = projetoRepository.findByAtivoTrue();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicIdEAtivoTrue() {
        Unidade unidade = criarUnidade("PA3");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório H");
        criarProjeto(laboratorio, "Projeto Ativo H", true);
        criarProjeto(laboratorio, "Projeto Inativo H", false);

        List<Projeto> resultado = projetoRepository
                .findByLaboratorioUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertEquals(1, resultado.size());
        assertEquals("Projeto Ativo H", resultado.get(0).getNome());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadeNaoTemProjetosAtivos() {
        Unidade unidade = criarUnidade("PA4");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório I");
        criarProjeto(laboratorio, "Projeto Inativo I", false);

        List<Projeto> resultado = projetoRepository
                .findByLaboratorioUnidadePublicIdAndAtivoTrue(unidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void findAllDeveRetornarTodosOsProjetosQuandoNenhumTenantEstaDefinido() {
        Unidade unidade = criarUnidade("PF1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório J");
        criarProjeto(laboratorio, "Projeto J1", true);
        criarProjeto(laboratorio, "Projeto J2", true);

        List<Projeto> resultado = projetoRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasProjetosDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("PF2");
        Unidade unidadeB = criarUnidade("PF3");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório K");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório L");
        criarProjeto(laboratorioA, "Projeto K", true);
        criarProjeto(laboratorioB, "Projeto L", true);

        TenantContext.definir(unidadeA.getPublicId());

        List<Projeto> resultado = projetoRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals("Projeto K", resultado.get(0).getNome());
    }
}
