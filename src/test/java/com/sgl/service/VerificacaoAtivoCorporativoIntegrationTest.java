package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.SessaoDeTeste;
import com.sgl.tenant.TenantContext;

/**
 * Corporate active-check against the real services, repositories and H2, with only the gateway faked.
 * Not @Transactional: the deactivation must be committed on its own and survive the rollback of
 * EstagiarioService.criar, which only real transactions can show. The context (and its H2) is shared,
 * so everything created here is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class VerificacaoAtivoCorporativoIntegrationTest {

    private static final int CNPMF = 7;
    private static final String CPF_ESTAGIARIO = "12345678909";
    private static final String MATRICULA_ORIENTADOR = "344136";

    @Autowired private VerificacaoAtivoCorporativoService verificacao;
    @Autowired private EstagiarioService estagiarioService;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private EstagiarioRepository estagiarioRepository;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario estagiario;
    private Usuario orientador;

    @BeforeEach
    void setUp() {
        gateway.reset();
        unidade = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("CNPMF-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CNPMF)
                .build());
        laboratorio = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Laboratório de Fitopatologia").ativo(true).build());
        estagiario = salvar(usuario("Amanda Estagiária", null, CPF_ESTAGIARIO, Perfil.ESTAGIARIO));
        orientador = salvar(usuario("Dra. Ana Orientadora", MATRICULA_ORIENTADOR, "98765432100", Perfil.PESQUISADOR));
    }

    @AfterEach
    void tearDown() {
        SessaoDeTeste.sair();
        TenantContext.limpar();
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(unidade.getPublicId()));
        laboratorioRepository.delete(laboratorio);
        unidadeRepository.delete(unidade);
    }

    @Test
    void ambosAtivosPassa() {
        gateway.colaboradorPorCpf(CPF_ESTAGIARIO, "Amanda Estagiária", CNPMF, true);
        gateway.empregadoPorMatricula(MATRICULA_ORIENTADOR, "Dra. Ana Orientadora", "98765432100",
                "Pesquisador A", CNPMF, true);

        assertThatCode(() -> verificacao.exigirAtivos(estagiario, orientador)).doesNotThrowAnyException();

        gateway.verify();
        assertThat(ativo(estagiario)).isTrue();
        assertThat(ativo(orientador)).isTrue();
    }

    @Test
    void estagiarioInativoRecusaEDesativa() {
        gateway.colaboradorPorCpf(CPF_ESTAGIARIO, "Amanda Estagiária", CNPMF, false);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O estagiário Amanda Estagiária está inativo na base corporativa.");

        gateway.verify();
        assertThat(ativo(estagiario)).isFalse();
        assertThat(ativo(orientador)).isTrue();
    }

    @Test
    void orientadorInativoRecusaEDesativa() {
        gateway.colaboradorPorCpf(CPF_ESTAGIARIO, "Amanda Estagiária", CNPMF, true);
        gateway.empregadoPorMatricula(MATRICULA_ORIENTADOR, "Dra. Ana Orientadora", "98765432100",
                "Pesquisador A", CNPMF, false);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O orientador Dra. Ana Orientadora está inativo na base corporativa.");

        gateway.verify();
        assertThat(ativo(estagiario)).isTrue();
        assertThat(ativo(orientador)).isFalse();
    }

    @Test
    void pessoaNaoEncontradaRecusaEDesativa() {
        gateway.colaboradorPorCpfNaoEncontrado(CPF_ESTAGIARIO);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O estagiário Amanda Estagiária está inativo na base corporativa.");

        gateway.verify();
        assertThat(ativo(estagiario)).isFalse();
    }

    @Test
    void baseIndisponivelRecusaSemDesativar() {
        gateway.colaboradorPorCpf(CPF_ESTAGIARIO, "Amanda Estagiária", CNPMF, true);
        gateway.empregadoPorMatriculaStatus(MATRICULA_ORIENTADOR, HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não foi possível confirmar na base corporativa se o estagiário e o orientador "
                        + "estão ativos. Tente novamente.");

        gateway.verify();
        assertThat(ativo(estagiario)).isTrue();
        assertThat(ativo(orientador)).isTrue();
    }

    // A 400 from the gateway does not prove the person is absent (e.g. invalid pagination), so it must
    // not be read as "not found" and deactivate anyone.
    @Test
    void respostaInesperadaRecusaSemDesativar() {
        gateway.colaboradorPorCpfStatus(CPF_ESTAGIARIO, HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não foi possível confirmar na base corporativa se o estagiário e o orientador "
                        + "estão ativos. Tente novamente.");

        gateway.verify();
        assertThat(ativo(estagiario)).isTrue();
        assertThat(ativo(orientador)).isTrue();
    }

    @Test
    void semMatriculaESemCpfRecusa() {
        estagiario.setCpf(null);
        estagiario = salvar(estagiario);

        assertThatThrownBy(() -> verificacao.exigirAtivos(estagiario, orientador))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Não é possível conferir na base corporativa o estagiário Amanda Estagiária: "
                        + "cadastro sem matrícula e sem CPF.");

        gateway.verify();
        assertThat(ativo(estagiario)).isTrue();
    }

    @Test
    void inativoFicaDesativadoMesmoComCadastroRecusado() {
        SessaoDeTeste.entrar(UUID.randomUUID(), Perfil.GESTOR);
        TenantContext.definir(unidade.getPublicId());
        gateway.colaboradorPorCpf(CPF_ESTAGIARIO, "Amanda Estagiária", CNPMF, false);

        assertThatThrownBy(() -> estagiarioService.criar(requisicao()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O estagiário Amanda Estagiária está inativo na base corporativa.");

        gateway.verify();
        assertThat(ativo(estagiario)).isFalse();
        assertThat(estagiarioRepository.existsById(estagiario.getId())).isFalse();
    }

    private EstagiarioRequestDTO requisicao() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(estagiario.getPublicId());
        dto.setLaboratorioId(laboratorio.getPublicId());
        dto.setOrientadorId(orientador.getPublicId());
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);
        dto.setAtividadeId(UUID.randomUUID());
        return dto;
    }

    private boolean ativo(Usuario usuario) {
        return Boolean.TRUE.equals(usuarioRepository.findById(usuario.getId()).orElseThrow().getAtivo());
    }

    private Usuario usuario(String nome, String matricula, String cpf, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setMatricula(matricula);
        usuario.setCpf(cpf);
        usuario.setPerfil(perfil);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setOrigem(OrigemUsuario.CORPORATIVO);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario salvar(Usuario usuario) {
        return usuarioRepository.saveAndFlush(usuario);
    }
}
