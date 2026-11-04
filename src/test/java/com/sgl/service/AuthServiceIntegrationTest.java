package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.config.AuthProperties;
import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.exception.LoginRejectedException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;
import com.sgl.security.JwtService;

/**
 * Corporate login against the real service, repositories and H2, with only the gateway faked.
 * Not @Transactional: login deliberately commits the "deactivated" mark before refusing, and the
 * context (and its H2) is shared, so everything created here is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class AuthServiceIntegrationTest {

    private static final int CNPMF = 7;
    private static final String CPF = "12345678909";
    private static final String OUTRO_CPF = "98765432100";
    private static final String IP = "10.0.0.1";

    @Autowired private AuthService authService;
    @Autowired private AuthProperties authProperties;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private JwtService jwtService;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private VinculoOperacionalUsuarioRepository vinculoRepository;

    private Unidade cnpmf;
    private Unidade outra;

    @BeforeEach
    void setUp() {
        gateway.reset();
        authProperties.setPrimeiroLoginAdmin(false);
        cnpmf = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("CNPMF-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CNPMF)
                .build());
        outra = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Outra Unidade")
                .sigla("OUT-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(8)
                .build());
    }

    @AfterEach
    void tearDown() {
        authProperties.setPrimeiroLoginAdmin(false);
        vinculoRepository.deleteAll();
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId()));
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(outra.getPublicId()));
        laboratorioRepository.deleteAll(laboratorioRepository.findAll().stream()
                .filter(l -> l.getUnidade().getId().equals(outra.getId())).toList());
        unidadeRepository.delete(cnpmf);
        unidadeRepository.delete(outra);
    }

    @Test
    void login_empregadoNovo_entraAtivoComPerfilSugeridoEDadosCorporativos() {
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Fulano de Tal", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login(" M0344136 ", "senha", IP);

        Usuario usuario = usuarioRepository.findWithUnidadeByPublicId(resposta.getUsuario().getId()).orElseThrow();
        assertThat(usuario.getMatricula()).isEqualTo("344136");
        // Empregado não espera aprovação: entra ativo com o perfil sugerido pelo cargo.
        assertThat(usuario.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(usuario.getOrigem()).isEqualTo(OrigemUsuario.CORPORATIVO);
        assertThat(usuario.getUnidade().getPublicId()).isEqualTo(cnpmf.getPublicId());
        assertThat(usuario.getCargo()).isEqualTo("Analista A");
        assertThat(usuario.getSublotacao()).isEqualTo("Sublotação 344136");
        assertThat(usuario.getCpf()).isEqualTo(CPF);
        assertThat(usuario.getLogin()).isEqualTo("m344136");
        assertThat(usuario.getPerfil()).isEqualTo(Perfil.ANALISTA);
        assertThat(usuario.getUltimoLoginEm()).isNotNull();
        assertThat(usuario.getSincronizadoEm()).isNotNull();
        assertThat(jwtService.parse(resposta.getToken())).contains(usuario.getPublicId());
        assertThat(resposta.getExpiraEm()).isNotNull();
        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        gateway.verify();
    }

    // Colaborador (estagiário, bolsista) continua passando pela aprovação de um gestor.
    @Test
    void login_colaboradorNovo_ficaPendenteComoEstagiario() {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfNaoEncontrado(CPF);
        gateway.colaboradorPorCpf(CPF, "Bolsista Nova", CNPMF, true);

        LoginResponseDTO resposta = authService.login(CPF, "senha", IP);

        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.ESTAGIARIO);
    }

    // Quem ficou pendente pela regra antiga e é empregado é liberado no próximo login, sem perder o perfil.
    @Test
    void login_empregadoJaPendente_eLiberadoNoProximoLogin() {
        Usuario pendente = salvar(usuario("Pendente", "344136", CPF, Perfil.ANALISTA, SituacaoUsuario.PENDENTE_VINCULACAO));
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Pendente", CPF, "Analista A", CNPMF, true);

        authService.login("m344136", "senha", IP);

        Usuario atualizado = usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow();
        assertThat(atualizado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(atualizado.getPerfil()).isEqualTo(Perfil.ANALISTA);
    }

    @Test
    void login_colaboradorJaPendente_continuaPendente() {
        Usuario pendente = salvar(usuario("Estagiário", null, CPF, Perfil.ESTAGIARIO, SituacaoUsuario.PENDENTE_VINCULACAO));
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfNaoEncontrado(CPF);
        gateway.colaboradorPorCpf(CPF, "Estagiário", CNPMF, true);

        authService.login(CPF, "senha", IP);

        assertThat(usuarioRepository.findByPublicId(pendente.getPublicId()).orElseThrow().getSituacao())
                .isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
    }

    @Test
    void login_usuarioImportado_entraAtivoEAtualizaNome() {
        Usuario importado = salvar(usuario("Nome Antigo", "344136", CPF, Perfil.PESQUISADOR, SituacaoUsuario.ATIVO));
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Nome Novo", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        Usuario atualizado = usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Nome Novo");
        assertThat(atualizado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        // Synchronization never touches the profile granted inside the SGL.
        assertThat(atualizado.getPerfil()).isEqualTo(Perfil.PESQUISADOR);
        assertThat(atualizado.getCargo()).isEqualTo("Analista A");
        assertThat(atualizado.getLogin()).isEqualTo("m344136");
        assertThat(resposta.getUsuario().getId()).isEqualTo(importado.getPublicId());
        assertThat(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId())).hasSize(1);
    }

    // Mudança de lotação: a Embrapa passou a pessoa para outra Unidade. Perfil, laboratório e vínculos
    // eram da Unidade antiga; na nova ela recomeça como num primeiro login (empregado: ativo com o
    // perfil sugerido pelo cargo).
    @Test
    void login_empregadoMudouDeUnidade_recomecaSemLaboratorioEEncerraVinculos() {
        Laboratorio laboratorio = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(outra).nome("Lab da outra").ativo(true).build());
        Usuario gestor = usuario("Gestor Transferido", "344136", CPF, Perfil.GESTOR, SituacaoUsuario.ATIVO);
        gestor.setUnidade(outra);
        gestor.setLaboratorio(laboratorio);
        gestor = salvar(gestor);
        VinculoOperacionalUsuario vinculo = new VinculoOperacionalUsuario();
        vinculo.setUsuario(gestor);
        vinculo.setLaboratorio(laboratorio);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1));
        vinculo = vinculoRepository.saveAndFlush(vinculo);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Gestor Transferido", CPF, "Analista A", CNPMF, true);

        authService.login("m344136", "senha", IP);

        Usuario atualizado = usuarioRepository.findWithUnidadeAndLaboratorioByPublicId(gestor.getPublicId()).orElseThrow();
        assertThat(atualizado.getUnidade().getPublicId()).isEqualTo(cnpmf.getPublicId());
        assertThat(atualizado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(atualizado.getLaboratorio()).isNull();
        assertThat(atualizado.getPerfil()).isEqualTo(Perfil.ANALISTA); // provisional suggestion, as for a new user
        VinculoOperacionalUsuario encerrado = vinculoRepository.findById(vinculo.getId()).orElseThrow();
        assertThat(encerrado.getEncerrado()).isTrue();
        assertThat(encerrado.getDataFim()).isNotNull();
    }

    @Test
    void login_administradorGeralMudouDeUnidade_soAcompanhaAUnidade() {
        Usuario admin = usuario("Admin Geral", "344136", CPF, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);
        admin.setUnidade(outra);
        admin = salvar(admin);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Admin Geral", CPF, "Analista A", CNPMF, true);

        authService.login("m344136", "senha", IP);

        Usuario atualizado = usuarioRepository.findWithUnidadeByPublicId(admin.getPublicId()).orElseThrow();
        assertThat(atualizado.getUnidade().getPublicId()).isEqualTo(cnpmf.getPublicId());
        assertThat(atualizado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(atualizado.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR_GERAL);
    }

    @Test
    void login_localizaPorCpfQuandoMatriculaAusente() {
        Usuario importado = salvar(usuario("Estagiária", null, CPF, Perfil.ESTAGIARIO, SituacaoUsuario.ATIVO));
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpfNaoEncontrado(CPF);
        gateway.colaboradorPorCpf(CPF, "Estagiária Atualizada", CNPMF, true);

        LoginResponseDTO resposta = authService.login("123.456.789-09", "senha", IP);

        assertThat(resposta.getUsuario().getId()).isEqualTo(importado.getPublicId());
        Usuario atualizado = usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Estagiária Atualizada");
        assertThat(atualizado.getMatricula()).isNull();
        assertThat(atualizado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId())).hasSize(1);
    }

    @Test
    void login_naoSubstituiMatriculaPorCpf() {
        Usuario empregado = usuario("Empregado", "344136", CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO);
        empregado.setLogin("m344136");
        Usuario importado = salvar(empregado);
        // Newer employees log in with the CPF and the gateway reports the CPF itself as "matricula".
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpf(CPF, CPF, "Empregado", "Técnico A", CNPMF);

        LoginResponseDTO resposta = authService.login(CPF, "senha", IP);

        assertThat(resposta.getUsuario().getId()).isEqualTo(importado.getPublicId());
        Usuario atualizado = usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow();
        assertThat(atualizado.getMatricula()).isEqualTo("344136");
        // A CPF login must not replace the stored network login (nor ever be stored as one).
        assertThat(atualizado.getLogin()).isEqualTo("m344136");
        assertThat(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId())).hasSize(1);
    }

    @Test
    void login_importadoPorCpfDepoisEntraPorMatricula_mesmoUsuarioComMatriculaPreenchida() {
        Usuario importado = salvar(usuario("Empregado", null, CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO));
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Empregado", CPF, "Técnico A", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getId()).isEqualTo(importado.getPublicId());
        Usuario atualizado = usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow();
        assertThat(atualizado.getMatricula()).isEqualTo("344136");
        assertThat(atualizado.getLogin()).isEqualTo("m344136");
        assertThat(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId())).hasSize(1);
    }

    @Test
    void login_novoComMatriculaDeOnzeDigitos_naoGravaCpfComoMatricula() {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpf(CPF, CPF, "Empregado Novo", "Técnico A", CNPMF);

        authService.login(CPF, "senha", IP);

        Usuario criado = usuarioRepository.findByCpf(CPF).orElseThrow();
        assertThat(criado.getMatricula()).isNull();
        // The CPF already lives in the cpf column; login must not become a second copy of it.
        assertThat(criado.getLogin()).isNull();
    }

    @Test
    void login_inativoNaBase_403_eMarcaUsuarioInativo() {
        Usuario importado = salvar(usuario("Aposentado", "344136", CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO));
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Aposentado", CPF, "Técnico A", CNPMF, false);

        assertThatThrownBy(() -> authService.login("m344136", "senha", IP))
                .isInstanceOfSatisfying(LoginRejectedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCodigo()).isEqualTo("CONTA_DESATIVADA");
                });

        Usuario desativado = usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow();
        assertThat(desativado.getAtivo()).isFalse();
        // Deactivation keeps the SGL data so an admin can restore the person later.
        assertThat(desativado.getPerfil()).isEqualTo(Perfil.TECNICO);
    }

    @Test
    void login_contaDesabilitada_marcaUsuarioExistenteInativo() {
        Usuario importado = salvar(usuario("Desligado", "344136", CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO));
        gateway.loginOk("m344136", "344136", 514);

        assertThatThrownBy(() -> authService.login("m344136", "senha", IP))
                .isInstanceOfSatisfying(LoginRejectedException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONTA_DESATIVADA"));

        assertThat(usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow().getAtivo()).isFalse();
        gateway.verify();
    }

    @Test
    void login_usuarioDesativadoNoSgl_recusaSemReativar() {
        Usuario desativado = usuario("Bloqueado", "344136", CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO);
        desativado.setAtivo(false);
        salvar(desativado);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Bloqueado", CPF, "Técnico A", CNPMF, true);

        assertThatThrownBy(() -> authService.login("m344136", "senha", IP))
                .isInstanceOfSatisfying(LoginRejectedException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CONTA_DESATIVADA"));

        assertThat(usuarioRepository.findByPublicId(desativado.getPublicId()).orElseThrow().getAtivo()).isFalse();
    }

    @Test
    void login_primeiroComFlagLigada_viraAdministradorGeral() {
        assertThat(usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR_GERAL)).isFalse();
        authProperties.setPrimeiroLoginAdmin(true);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.ADMINISTRADOR_GERAL);
        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    void login_segundoComFlagLigada_naoViraAdministradorGeral() {
        authProperties.setPrimeiroLoginAdmin(true);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Analista A", CNPMF, true);
        gateway.loginOk("m500", "500", 512);
        gateway.empregadoPorMatricula("500", "Segundo", OUTRO_CPF, "Pesquisador A", CNPMF, true);

        authService.login("m344136", "senha", IP);
        LoginResponseDTO segundo = authService.login("m500", "senha", IP);

        assertThat(segundo.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(segundo.getUsuario().getPerfil()).isEqualTo(Perfil.PESQUISADOR);
    }

    @Test
    void login_flagLigadaComAdministradorGeralJaDesativado_naoViraAdministradorGeral() {
        // The bootstrap is single-use: once any ADMINISTRADOR_GERAL ever existed, deactivating it
        // must not reopen the door for whoever logs in next.
        Usuario antigo = usuario("Admin Geral Antigo", "100", OUTRO_CPF, Perfil.ADMINISTRADOR_GERAL, SituacaoUsuario.ATIVO);
        antigo.setAtivo(false);
        salvar(antigo);
        authProperties.setPrimeiroLoginAdmin(true);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Recém-chegado", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.ANALISTA);
    }

    @Test
    void login_flagDesligada_empregadoComCargoDesconhecidoEntraComoTecnico() {
        assertThat(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL)).isFalse();
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Cargo Desconhecido", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        // An unrecognized cargo falls back to the least privileged operational profile.
        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.TECNICO);
    }

    @Test
    void devLogin_usuarioDev_emiteToken() {
        Usuario dev = usuario("Gestor DEV", null, null, Perfil.GESTOR, SituacaoUsuario.ATIVO);
        dev.setOrigem(OrigemUsuario.DEV);
        dev.setLogin("gestor.teste");
        salvar(dev);

        LoginResponseDTO resposta = authService.devLogin("gestor.teste");

        assertThat(jwtService.parse(resposta.getToken())).contains(dev.getPublicId());
        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.GESTOR);
    }

    @Test
    void devLogin_usuarioCorporativo_401() {
        Usuario corporativo = usuario("Corporativo", "344136", CPF, Perfil.GESTOR, SituacaoUsuario.ATIVO);
        corporativo.setLogin("m344136");
        salvar(corporativo);

        // The password-less DEV path must never open a corporate account.
        assertThatThrownBy(() -> authService.devLogin("m344136"))
                .isInstanceOfSatisfying(LoginRejectedException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCodigo()).isEqualTo("CREDENCIAIS_INVALIDAS");
                });
    }

    @Test
    void devLogin_inexistente_401() {
        assertThatThrownBy(() -> authService.devLogin("ninguem"))
                .isInstanceOfSatisfying(LoginRejectedException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private Usuario usuario(String nome, String matricula, String cpf, Perfil perfil, SituacaoUsuario situacao) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setMatricula(matricula);
        usuario.setCpf(cpf);
        usuario.setPerfil(perfil);
        usuario.setSituacao(situacao);
        usuario.setOrigem(OrigemUsuario.CORPORATIVO);
        usuario.setUnidade(cnpmf);
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario salvar(Usuario usuario) {
        return usuarioRepository.saveAndFlush(usuario);
    }
}
