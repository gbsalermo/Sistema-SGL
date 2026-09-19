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

import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do UsuarioRepository usando @DataJpaTest com H2 real
 * (sem mocks). Segue o mesmo padrão estabelecido em UnidadeRepositoryTest:
 * - @ActiveProfiles("test") para carregar application-test.properties (H2),
 *   já que o profile padrão da aplicação ("dev") usa PostgreSQL real.
 * - @Import(TenantProvider.class) para disponibilizar o bean "tenantProvider"
 *   usado pela expressão SpEL do findAll() sobrescrito.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

    private Usuario criarUsuario(String email, Unidade unidade, Laboratorio laboratorio) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("senha-123");
        usuario.setPerfil(Perfil.TECNICO);
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);
        usuario.setAtivo(true);
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void deveEncontrarPorPublicId() {
        Usuario usuario = criarUsuario("ana@exemplo.com", null, null);

        Optional<Usuario> resultado = usuarioRepository.findByPublicId(usuario.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals("ana@exemplo.com", resultado.get().getEmail());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Usuario> resultado = usuarioRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorPublicIdEUnidadePublicIdQuandoUsuarioPertenceAUnidade() {
        Unidade unidade = criarUnidade("EU1");
        Usuario usuario = criarUsuario("bruno@exemplo.com", unidade, null);

        Optional<Usuario> resultado = usuarioRepository
                .findByPublicIdAndUnidadePublicId(usuario.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdEUnidadePublicIdQuandoUsuarioPertenceAOutraUnidade() {
        Unidade unidadeDoUsuario = criarUnidade("EU2");
        Unidade outraUnidade = criarUnidade("EU3");
        Usuario usuario = criarUsuario("carla@exemplo.com", unidadeDoUsuario, null);

        Optional<Usuario> resultado = usuarioRepository
                .findByPublicIdAndUnidadePublicId(usuario.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorEmail() {
        criarUsuario("daniel@exemplo.com", null, null);

        Optional<Usuario> resultado = usuarioRepository.findByEmail("daniel@exemplo.com");

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorEmailInexistente() {
        Optional<Usuario> resultado = usuarioRepository.findByEmail("naoexiste@exemplo.com");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorLaboratorioId() {
        Unidade unidade = criarUnidade("EL1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório de Solos");
        criarUsuario("eduardo@exemplo.com", unidade, laboratorio);

        List<Usuario> resultado = usuarioRepository.findByLaboratorioId(laboratorio.getId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioIdNaoTemUsuarios() {
        Unidade unidade = criarUnidade("EL2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório vazio");

        List<Usuario> resultado = usuarioRepository.findByLaboratorioId(laboratorio.getId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveEncontrarPorUnidadePublicId() {
        Unidade unidade = criarUnidade("EU4");
        criarUsuario("fabio@exemplo.com", unidade, null);

        List<Usuario> resultado = usuarioRepository.findByUnidadePublicId(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoCasa() {
        List<Usuario> resultado = usuarioRepository.findByUnidadePublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExistirPorEmail() {
        criarUsuario("gabriela@exemplo.com", null, null);

        assertTrue(usuarioRepository.existsByEmail("gabriela@exemplo.com"));
    }

    @Test
    void naoDeveExistirPorEmailInexistente() {
        assertFalse(usuarioRepository.existsByEmail("ninguem@exemplo.com"));
    }

    @Test
    void deveExistirPorEmailComIdDiferenteQuandoOutroUsuarioUsaOMesmoEmail() {
        Usuario usuario = criarUsuario("helena@exemplo.com", null, null);

        boolean existe = usuarioRepository.existsByEmailAndIdNot("helena@exemplo.com", usuario.getId() + 1);

        assertTrue(existe);
    }

    @Test
    void naoDeveExistirPorEmailComIdNotQuandoOEmailPertenceAoProprioId() {
        Usuario usuario = criarUsuario("igor@exemplo.com", null, null);

        boolean existe = usuarioRepository.existsByEmailAndIdNot("igor@exemplo.com", usuario.getId());

        assertFalse(existe);
    }

    @Test
    void findAllDeveRetornarTodosOsUsuariosQuandoNenhumTenantEstaDefinido() {
        // NOTA: a query customizada de findAll() navega "usuario.unidade.publicId" no
        // WHERE, o que o Hibernate traduz para INNER JOIN com unidades (join, não
        // left join). Por isso, um usuário sem unidade associada é sempre excluído do
        // findAll(), mesmo com nenhum tenant definido (comportamento pré-existente da
        // query em produção — não alterado aqui, apenas documentado via este teste
        // usando usuários COM unidade).
        Unidade unidade = criarUnidade("TN1");
        criarUsuario("julia@exemplo.com", unidade, null);
        criarUsuario("kaue@exemplo.com", unidade, null);

        List<Usuario> resultado = usuarioRepository.findAll();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllDeveFiltrarApenasUsuariosDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("TA1");
        Unidade unidadeB = criarUnidade("TB1");
        criarUsuario("leo@exemplo.com", unidadeA, null);
        criarUsuario("marcia@exemplo.com", unidadeB, null);

        TenantContext.definir(unidadeA.getPublicId());

        List<Usuario> resultado = usuarioRepository.findAll();

        assertEquals(1, resultado.size());
        assertEquals("leo@exemplo.com", resultado.get(0).getEmail());
    }
}
