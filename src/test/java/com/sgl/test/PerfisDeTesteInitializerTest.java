package com.sgl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioRepository;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@Transactional
class PerfisDeTesteInitializerTest {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;
    @Autowired
    private UnidadeRepository unidadeRepository;
    @Autowired
    private LaboratorioRepository laboratorioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VinculoEstagioRepository vinculoEstagioRepository;

    private Unidade u1;
    private Unidade u2;
    private Laboratorio u1Lab1;
    private Laboratorio u1Lab2;

    @BeforeEach
    void limparEPopular() {
        // The database is shared per context; start every test from an empty one.
        vinculoEstagioRepository.deleteAll();
        usuarioRepository.deleteAll();
        laboratorioRepository.deleteAll();
        unidadeRepository.deleteAll();
    }

    private void cadastrarUnidades() {
        // Names/siglas sort U1 before U2, which the distribution rule depends on.
        u1 = unidadeRepository.save(new Unidade(null, null, "Unidade 1", "U1", null));
        u2 = unidadeRepository.save(new Unidade(null, null, "Unidade 2", "U2", null));
        u1Lab1 = laboratorioRepository.save(new Laboratorio(null, null, u1, "Lab A", "A", null, true));
        u1Lab2 = laboratorioRepository.save(new Laboratorio(null, null, u1, "Lab B", "B", null, true));
        laboratorioRepository.save(new Laboratorio(null, null, u2, "Lab C", "C", null, true));
    }

    private PerfisDeTesteInitializer initializer() {
        return beanFactory.createBean(PerfisDeTesteInitializer.class);
    }

    private List<Usuario> usuariosDeTeste() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getLogin() != null && u.getLogin().startsWith("teste."))
                .toList();
    }

    private Usuario porLogin(String login) {
        return usuarioRepository.findByLogin(login).orElseThrow();
    }

    @Test
    void criaTresPorPerfil() {
        cadastrarUnidades();
        initializer().criarUsuariosDeTeste();

        List<Usuario> usuarios = usuariosDeTeste().stream()
                .filter(u -> !u.getLogin().startsWith("teste.gestor-responsavel"))
                .toList();
        assertEquals(21, usuarios.size());
        Map<Perfil, Long> porPerfil = usuarios.stream()
                .collect(Collectors.groupingBy(u -> Perfil.legadoDe(u.getPapeis(), u.getCategoriaCargo()), Collectors.counting()));
        for (Perfil perfil : Perfil.values()) {
            assertEquals(3L, porPerfil.get(perfil), perfil.name());
        }
        assertTrue(usuarios.stream().allMatch(u -> u.getOrigem() == OrigemUsuario.DEV
                && Boolean.TRUE.equals(u.getAtivo()) && u.getSituacao() == SituacaoUsuario.ATIVO));
    }

    // Roles accumulate: one test user is GESTOR and also the responsável of a laboratory.
    @Test
    void criaGestorQueTambemEhResponsavelDeLaboratorio() {
        cadastrarUnidades();
        initializer().criarUsuariosDeTeste();

        Usuario usuario = porLogin("teste.gestor-responsavel.1");
        assertEquals(java.util.Set.of(com.sgl.model.enums.PapelSistema.GESTOR), usuario.getPapeis());
        assertEquals(com.sgl.model.enums.CategoriaCargo.PESQUISADOR, usuario.getCategoriaCargo());
        Laboratorio laboratorio = laboratorioRepository.findById(u1Lab1.getId()).orElseThrow();
        assertEquals(usuario.getId(), laboratorio.getResponsavel().getId());
    }

    @Test
    void distribuiEntreUnidadesELaboratorios() {
        cadastrarUnidades();
        initializer().criarUsuariosDeTeste();

        Usuario g1 = porLogin("teste.gestor.1");
        assertEquals(u1.getId(), g1.getUnidade().getId());
        assertEquals(List.of(u1Lab1.getId()), g1.getLaboratorios().stream().map(Laboratorio::getId).toList());

        assertEquals(u2.getId(), porLogin("teste.gestor.2").getUnidade().getId());

        Usuario g3 = porLogin("teste.gestor.3");
        assertEquals(u1.getId(), g3.getUnidade().getId());
        assertEquals(List.of(u1Lab2.getId()), g3.getLaboratorios().stream().map(Laboratorio::getId).toList());
    }

    @Test
    void estagiariosTemVinculoComPesquisadorDeTeste() {
        cadastrarUnidades();
        initializer().criarUsuariosDeTeste();

        Usuario estagiario = porLogin("teste.estagiario.1");
        assertInstanceOf(Estagiario.class, estagiario);
        VinculoEstagio vinculo = vinculoEstagioRepository.findAll().stream()
                .filter(v -> v.getEstagiario().getId().equals(estagiario.getId()))
                .findFirst().orElseThrow();
        assertNotNull(vinculo.getOrientador());
        assertEquals("teste.pesquisador.1", vinculo.getOrientador().getLogin());
    }

    @Test
    void idempotente() {
        cadastrarUnidades();
        PerfisDeTesteInitializer initializer = initializer();
        initializer.criarUsuariosDeTeste();
        initializer.criarUsuariosDeTeste();

        // 3 per former profile plus the single "gestor e responsável".
        assertEquals(22, usuariosDeTeste().size());
    }

    @Test
    void semUnidadesNaoFalha() {
        initializer().criarUsuariosDeTeste();

        assertEquals(0, usuariosDeTeste().size());
    }

    @Test
    void loginDeTeste_formato() {
        assertEquals("teste.administrador-geral.2",
                PerfisDeTesteInitializer.loginDeTeste(Perfil.ADMINISTRADOR_GERAL, 2));
    }
}
