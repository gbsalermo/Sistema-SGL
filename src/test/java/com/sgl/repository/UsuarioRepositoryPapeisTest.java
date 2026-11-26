package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.PapelSistema;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UsuarioRepositoryPapeisTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        return entityManager.persistAndFlush(Unidade.builder().nome("Unidade " + sigla).sigla(sigla).build());
    }

    private Usuario criarUsuario(String email, Unidade unidade, boolean ativo, PapelSistema... papeis) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setUnidade(unidade);
        usuario.setAtivo(ativo);
        usuario.substituirPapeis(List.of(papeis));
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void contaSoAdministradoresLocaisAtivosDaUnidade() {
        Unidade a = criarUnidade("UA");
        Unidade b = criarUnidade("UB");
        criarUsuario("a1@sgl.local", a, true, PapelSistema.ADMINISTRADOR_LOCAL);
        criarUsuario("a2@sgl.local", a, true, PapelSistema.ADMINISTRADOR_LOCAL, PapelSistema.GESTOR);
        criarUsuario("a3@sgl.local", a, false, PapelSistema.ADMINISTRADOR_LOCAL);
        criarUsuario("b1@sgl.local", b, true, PapelSistema.ADMINISTRADOR_LOCAL);
        criarUsuario("a4@sgl.local", a, true, PapelSistema.GESTOR);

        assertEquals(2, usuarioRepository.contarAtivosComPapelNaUnidade(PapelSistema.ADMINISTRADOR_LOCAL, a.getPublicId()));
        assertEquals(3, usuarioRepository.contarAtivosComPapel(PapelSistema.ADMINISTRADOR_LOCAL));
    }

    @Test
    void detectaSeJaExisteAdministradorGeral() {
        Unidade a = criarUnidade("UC");
        assertFalse(usuarioRepository.existeComPapel(PapelSistema.ADMINISTRADOR_GERAL));

        criarUsuario("geral@sgl.local", a, false, PapelSistema.ADMINISTRADOR_GERAL);

        // Even an inactive one counts: the first-login bootstrap must not run twice.
        assertTrue(usuarioRepository.existeComPapel(PapelSistema.ADMINISTRADOR_GERAL));
    }
}
