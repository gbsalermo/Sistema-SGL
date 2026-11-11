package com.sgl.test;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds three test users per {@link Perfil} so the developer login screen always has someone to sign in as.
 *
 * <p>Runs after the other seeders (which are ordered lower) so units and laboratories already exist.</p>
 */
@Profile({"dev", "demo"})
@Component
@Order(3000)
@RequiredArgsConstructor
public class PerfisDeTesteInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PerfisDeTesteInitializer.class);

    private static final int USUARIOS_POR_PERFIL = 3;
    private static final String SENHA_TESTE = "123456";

    // Same labels as ROTULOS_PERFIL in the web app, so names match what the UI shows.
    private static final Map<Perfil, String> ROTULOS = Map.of(
            Perfil.ADMINISTRADOR_GERAL, "Administrador geral",
            Perfil.ADMINISTRADOR, "Administrador",
            Perfil.GESTOR, "Gestor",
            Perfil.TECNICO, "Técnico",
            Perfil.ANALISTA, "Analista",
            Perfil.PESQUISADOR, "Pesquisador",
            Perfil.ESTAGIARIO, "Estagiário");

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstagiarioRepository estagiarioRepository;
    private final VinculoEstagioRepository vinculoEstagioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        criarUsuariosDeTeste();
    }

    static String loginDeTeste(Perfil perfil, int n) {
        return "teste." + perfil.name().toLowerCase(Locale.ROOT).replace('_', '-') + "." + n;
    }

    @Transactional
    public void criarUsuariosDeTeste() {
        List<Unidade> unidades = unidadeRepository.findAllSemFiltroDeTenant();
        if (unidades.isEmpty()) {
            log.warn("Nenhuma unidade cadastrada; usuários de teste por perfil não foram criados.");
            return;
        }

        // Enum order matters: researchers must exist before interns reference them as advisors.
        for (Perfil perfil : Perfil.values()) {
            for (int n = 1; n <= USUARIOS_POR_PERFIL; n++) {
                criarUsuario(perfil, n, unidades);
            }
        }
    }

    private void criarUsuario(Perfil perfil, int n, List<Unidade> unidades) {
        String login = loginDeTeste(perfil, n);
        if (usuarioRepository.findByLogin(login).isPresent()) {
            return;
        }

        Unidade unidade = unidades.get((n - 1) % unidades.size());
        List<Laboratorio> laboratorios = laboratorioRepository.findByUnidadePublicId(unidade.getPublicId());
        // Round-robin inside the unit: the k-th user landing in a unit takes its k-th laboratory.
        Laboratorio laboratorio = laboratorios.isEmpty() ? null : laboratorios.get(((n - 1) / unidades.size()) % laboratorios.size());

        String nome = ROTULOS.get(perfil) + " de Teste " + n;
        String email = login + "@sgl.local";
        String senha = passwordEncoder.encode(SENHA_TESTE);

        if (perfil == Perfil.ESTAGIARIO) {
            criarEstagiario(nome, email, senha, login, n, unidade, laboratorio);
            return;
        }

        Usuario usuario = new Usuario(null, null, nome, email, senha, perfil, unidade, laboratorio, true);
        usuarioRepository.save(prepararUsuarioDeTeste(usuario, login));
    }

    private void criarEstagiario(String nome, String email, String senha, String login, int n,
            Unidade unidade, Laboratorio laboratorio) {
        Estagiario estagiario = new Estagiario();
        estagiario.setNome(nome);
        estagiario.setEmail(email);
        estagiario.setSenha(senha);
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        if (laboratorio != null) {
            estagiario.substituirLaboratorios(List.of(laboratorio));
        }
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.now().minusMonths(2));
        estagiario.setDataFimEstagio(LocalDate.now().plusMonths(10));
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_INSTITUCIONAL);
        estagiario.setSituacaoEstagio(SituacaoEstagio.EM_ANDAMENTO);
        estagiario = estagiarioRepository.save(prepararUsuarioDeTeste(estagiario, login));

        // Researcher n lives in the same unit as intern n, by the shared distribution rule.
        Usuario orientador = usuarioRepository.findByLogin(loginDeTeste(Perfil.PESQUISADOR, n)).orElse(null);
        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setEstagiario(estagiario);
        vinculo.setOrientador(orientador);
        vinculo.setDataInicio(estagiario.getDataInicioEstagio());
        vinculo.setDataFimPrevista(estagiario.getDataFimEstagio());
        vinculo.setTipoBolsa(estagiario.getTipoBolsa());
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setTreinamentoSegurancaConcluido(false);
        vinculoEstagioRepository.save(vinculo);
    }

    private <T extends Usuario> T prepararUsuarioDeTeste(T usuario, String login) {
        usuario.setLogin(login);
        usuario.setOrigem(OrigemUsuario.DEV);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setAtivo(true);
        return usuario;
    }
}
