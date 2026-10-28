package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.tenant.TenantProvider;

/**
 * Cobre os campos de identidade corporativa de Usuario/Unidade com H2 real:
 * a unicidade e a aceitação de nulos replicam o que a V45 garante no PostgreSQL.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UsuarioIdentidadeRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    private Usuario corporativo(String login, String matricula, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario " + login);
        usuario.setPerfil(Perfil.PESQUISADOR);
        usuario.setLogin(login);
        usuario.setMatricula(matricula);
        usuario.setCpf(cpf);
        usuario.setOrigem(OrigemUsuario.CORPORATIVO);
        usuario.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);
        return usuario;
    }

    @Test
    void findByMatricula_eFindByCpf_localizamUsuarioCorporativo() {
        usuarioRepository.saveAndFlush(corporativo("m344136", "344136", "12345678909"));

        assertTrue(usuarioRepository.findByMatricula("344136").isPresent());
        assertTrue(usuarioRepository.findByCpf("12345678909").isPresent());
        assertTrue(usuarioRepository.findByLogin("m344136").isPresent());
    }

    @Test
    void matriculaDuplicada_violaUnicidade() {
        usuarioRepository.saveAndFlush(corporativo("m1", "344136", "11111111111"));

        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(corporativo("m2", "344136", "22222222222")));
    }

    @Test
    void usuarioCorporativo_semSenhaESemEmail_ehPersistido() {
        Usuario usuario = corporativo("m9", "9", "33333333333");
        usuario.setSenha(null);
        usuario.setEmail(null);

        Usuario salvo = usuarioRepository.saveAndFlush(usuario);

        assertTrue(salvo.getId() != null);
    }

    @Test
    void unidade_findByCodigoCorporativo() {
        Unidade unidade = new Unidade();
        unidade.setNome("Centro");
        unidade.setSigla("CNPMF");
        unidade.setCodigoCorporativo(7);
        unidadeRepository.saveAndFlush(unidade);

        assertEquals("CNPMF", unidadeRepository.findByCodigoCorporativo(7).orElseThrow().getSigla());
    }
}
