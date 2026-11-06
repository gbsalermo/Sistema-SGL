package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Estagiario;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Approved interns have a row in usuarios but none in estagiarios until
 * management creates the internship link; these tests pin the query that finds them.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UsuarioEstagiarioSemVinculoRepositoryTest {

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

    private Usuario criarUsuario(Unidade unidade, String email, Perfil perfil,
            SituacaoUsuario situacao, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Pessoa " + email);
        usuario.setEmail(email);
        usuario.setSenha("senha-123");
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setSituacao(situacao);
        usuario.setAtivo(ativo);
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void listaEstagiarioAprovadoSemVinculo() {
        Unidade unidade = criarUnidade("U1");
        Usuario aprovado = criarUsuario(unidade, "a@x.br", Perfil.ESTAGIARIO, SituacaoUsuario.ATIVO, true);

        List<Usuario> resultado = usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId());

        assertEquals(1, resultado.size());
        assertEquals(aprovado.getId(), resultado.get(0).getId());
    }

    @Test
    void naoListaQuemJaTemVinculo() {
        Unidade unidade = criarUnidade("U1");
        Estagiario estagiario = new Estagiario();
        estagiario.setNome("Com vinculo");
        estagiario.setEmail("v@x.br");
        estagiario.setSenha("senha-123");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.of(2026, 1, 1));
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        estagiario.setSituacaoEstagio(SituacaoEstagio.EM_ANDAMENTO);
        entityManager.persistAndFlush(estagiario);

        assertTrue(usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId()).isEmpty());
    }

    @Test
    void naoListaPendenteNemInativo() {
        Unidade unidade = criarUnidade("U1");
        criarUsuario(unidade, "p@x.br", Perfil.ESTAGIARIO, SituacaoUsuario.PENDENTE_VINCULACAO, true);
        criarUsuario(unidade, "i@x.br", Perfil.ESTAGIARIO, SituacaoUsuario.ATIVO, false);

        assertTrue(usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId()).isEmpty());
    }

    @Test
    void naoListaOutraUnidade() {
        Unidade unidade = criarUnidade("U1");
        Unidade outra = criarUnidade("U2");
        criarUsuario(outra, "o@x.br", Perfil.ESTAGIARIO, SituacaoUsuario.ATIVO, true);

        assertTrue(usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId()).isEmpty());
    }

    @Test
    void naoListaOutrosPerfis() {
        Unidade unidade = criarUnidade("U1");
        criarUsuario(unidade, "an@x.br", Perfil.ANALISTA, SituacaoUsuario.ATIVO, true);

        assertTrue(usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId()).isEmpty());
    }
}
