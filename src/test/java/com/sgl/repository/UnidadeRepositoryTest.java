package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.sgl.model.Unidade;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do UnidadeRepository usando @DataJpaTest com H2 real
 * (sem mocks) — persiste entidades via TestEntityManager e valida os métodos
 * customizados (findByPublicId, existsBySigla, existsBySiglaAndIdNot e o
 * findAll() sobrescrito que filtra por tenant).
 *
 * IMPORTANTE: o profile padrão da aplicação é "dev" (PostgreSQL real), então
 * é obrigatório usar @ActiveProfiles("test") para carregar
 * application-test.properties (H2 em memória) — sem isso o teste tentaria
 * conectar num PostgreSQL inexistente no ambiente de CI/local.
 *
 * O método findAll() do repositório usa uma expressão SpEL
 * (:#{@tenantProvider.unidadeId}) que referencia o bean "tenantProvider".
 * O @DataJpaTest, por padrão, NÃO faz component scan de beans comuns
 * (@Component/@Service), apenas de @Entity e repositórios Spring Data — por
 * isso é necessário @Import(TenantProvider.class) para que o bean exista no
 * contexto de teste e a SpEL possa ser resolvida.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UnidadeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UnidadeRepository unidadeRepository;

    // Garante que nenhum teste "vaze" um tenant definido em ThreadLocal para o próximo.
    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String nome, String sigla) {
        Unidade unidade = Unidade.builder()
                .nome(nome)
                .sigla(sigla)
                .build();
        return entityManager.persistAndFlush(unidade);
    }

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("Instituto de Química", "IQ");

        Optional<Unidade> resultado = unidadeRepository.findByPublicId(unidade.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("IQ", resultado.get().getSigla());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Unidade> resultado = unidadeRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExistirPorSigla() {
        criarUnidade("Instituto de Física", "IF");

        assertTrue(unidadeRepository.existsBySigla("IF"));
    }

    @Test
    void naoDeveExistirPorSiglaInexistente() {
        assertFalse(unidadeRepository.existsBySigla("XYZ"));
    }

    @Test
    void deveExistirPorSiglaComIdDiferenteQuandoOutraUnidadeUsaAMesmaSigla() {
        Unidade outraUnidade = criarUnidade("Instituto de Biologia", "IB");

        // Simula a checagem de duplicidade ao editar uma unidade diferente da
        // que já possui a sigla "IB".
        boolean existe = unidadeRepository.existsBySiglaAndIdNot("IB", outraUnidade.getId() + 1);

        assertTrue(existe);
    }

    @Test
    void naoDeveExistirPorSiglaComIdNotQuandoASiglaPertenceAoProprioId() {
        Unidade unidade = criarUnidade("Instituto de Matemática", "IM");

        // O próprio registro que possui a sigla é excluído da busca (caso de edição sem conflito).
        boolean existe = unidadeRepository.existsBySiglaAndIdNot("IM", unidade.getId());

        assertFalse(existe);
    }

    @Test
    void findAllDeveRetornarTodasAsUnidadesQuandoNenhumTenantEstaDefinido() {
        criarUnidade("Instituto de Química", "IQ2");
        criarUnidade("Instituto de Física", "IF2");
        TenantContext.limpar();

        List<Unidade> resultado = unidadeRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasAUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("Instituto de Química", "IQ3");
        criarUnidade("Instituto de Física", "IF3");

        TenantContext.definir(unidadeA.getPublicId());

        List<Unidade> resultado = unidadeRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals("IQ3", resultado.get(0).getSigla());
    }
}
