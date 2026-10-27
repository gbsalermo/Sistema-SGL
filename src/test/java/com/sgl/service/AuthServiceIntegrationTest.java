package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
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

    private Unidade cnpmf;

    @BeforeEach
    void setUp() {
        gateway.reset();
        authProperties.setPrimeiroLoginAdmin(false);
        cnpmf = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("CNPMF-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CNPMF)
                .build());
    }

    @AfterEach
    void tearDown() {
        authProperties.setPrimeiroLoginAdmin(false);
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId()));
        unidadeRepository.delete(cnpmf);
    }

    @Test
    void login_empregadoNovo_criaPendenteComDadosCorporativos() {
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Fulano de Tal", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login(" M0344136 ", "senha", IP);

        Usuario usuario = usuarioRepository.findWithUnidadeByPublicId(resposta.getUsuario().getId()).orElseThrow();
        assertThat(usuario.getMatricula()).isEqualTo("344136");
        assertThat(usuario.getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
        assertThat(usuario.getOrigem()).isEqualTo(OrigemUsuario.CORPORATIVO);
        assertThat(usuario.getUnidade().getPublicId()).isEqualTo(cnpmf.getPublicId());
        assertThat(usuario.getCargo()).isEqualTo("Analista A");
        assertThat(usuario.getSublotacao()).isEqualTo("Sublotação 344136");
        assertThat(usuario.getCpf()).isEqualTo(CPF);
        assertThat(usuario.getLogin()).isEqualTo("m344136");
        assertThat(usuario.getPerfil()).isEqualTo(Perfil.ANALISTA); // provisional suggestion only
        assertThat(usuario.getUltimoLoginEm()).isNotNull();
        assertThat(usuario.getSincronizadoEm()).isNotNull();
        assertThat(jwtService.parse(resposta.getToken())).contains(usuario.getPublicId());
        assertThat(resposta.getExpiraEm()).isNotNull();
        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
        gateway.verify();
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
        Usuario importado = salvar(usuario("Empregado", "344136", CPF, Perfil.TECNICO, SituacaoUsuario.ATIVO));
        // Newer employees log in with the CPF and the gateway reports the CPF itself as "matricula".
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpf(CPF, CPF, "Empregado", "Técnico A", CNPMF);

        LoginResponseDTO resposta = authService.login(CPF, "senha", IP);

        assertThat(resposta.getUsuario().getId()).isEqualTo(importado.getPublicId());
        assertThat(usuarioRepository.findByPublicId(importado.getPublicId()).orElseThrow().getMatricula())
                .isEqualTo("344136");
        assertThat(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId())).hasSize(1);
    }

    @Test
    void login_novoComMatriculaDeOnzeDigitos_naoGravaCpfComoMatricula() {
        gateway.loginOk(CPF, CPF, 512);
        gateway.empregadoPorCpf(CPF, CPF, "Empregado Novo", "Técnico A", CNPMF);

        authService.login(CPF, "senha", IP);

        Usuario criado = usuarioRepository.findByCpf(CPF).orElseThrow();
        assertThat(criado.getMatricula()).isNull();
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
        assertThat(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL)).isFalse();
        authProperties.setPrimeiroLoginAdmin(true);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Analista A", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getPerfil()).isEqualTo(Perfil.ADMINISTRADOR_GERAL);
        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
    }

    @Test
    void login_segundoComFlagLigada_ficaPendente() {
        authProperties.setPrimeiroLoginAdmin(true);
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Analista A", CNPMF, true);
        gateway.loginOk("m500", "500", 512);
        gateway.empregadoPorMatricula("500", "Segundo", OUTRO_CPF, "Pesquisador A", CNPMF, true);

        authService.login("m344136", "senha", IP);
        LoginResponseDTO segundo = authService.login("m500", "senha", IP);

        assertThat(segundo.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
        assertThat(segundo.getUsuario().getPerfil()).isEqualTo(Perfil.PESQUISADOR);
    }

    @Test
    void login_flagDesligada_ficaPendente() {
        assertThat(usuarioRepository.existsByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL)).isFalse();
        gateway.loginOk("m344136", "344136", 512);
        gateway.empregadoPorMatricula("344136", "Primeiro", CPF, "Cargo Desconhecido", CNPMF, true);

        LoginResponseDTO resposta = authService.login("m344136", "senha", IP);

        assertThat(resposta.getUsuario().getSituacao()).isEqualTo(SituacaoUsuario.PENDENTE_VINCULACAO);
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
