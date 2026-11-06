package com.sgl.service;

import static com.sgl.corporate.CorporateGatewayStubs.colaboradorItem;
import static com.sgl.corporate.CorporateGatewayStubs.empregadoItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.tenant.TenantContext;
import com.sgl.corporate.CorporateApiClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import com.sgl.security.JwtService;

/**
 * Search and import of corporate people through the real security chain, service and H2, with only
 * the gateway faked. Not @Transactional (the filters read outside the test's persistence context);
 * the context is shared, so everything created here is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class PessoaCorporativaIntegrationTest {

    private static final int CODIGO = 7;
    private static final int OUTRA_UNIDADE_CORPORATIVA = 99;
    private static final String CPF_JOAO = "12345678909";
    private static final String CPF_MARIA = "98765432100";
    private static final String CPF_ESTAGIARIA = "11122233396";
    private static final String BUSCA = "/api/v1/pessoas-corporativas";
    private static final String IMPORTAR = BUSCA + "/importar";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private PessoaCorporativaService pessoaService;
    @Autowired private CorporateApiClient corporateApiClient;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Usuario gestor;
    private final List<Unidade> outrasUnidades = new ArrayList<>();

    @BeforeEach
    void setUp() {
        gateway.reset();
        pessoaService.clearCache();
        unidade = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("PC-" + java.util.UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CODIGO)
                .build());
        laboratorio = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab Base").ativo(true).build());
        gestor = novoUsuario(Perfil.GESTOR, null, null);
    }

    @AfterEach
    void tearDown() {
        pessoaService.clearCache();
        SecurityContextHolder.clearContext();
        TenantContext.limpar();
        List<Unidade> todas = new ArrayList<>(outrasUnidades);
        todas.add(unidade);
        for (Unidade u : todas) {
            usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(u.getPublicId()));
            laboratorioRepository.deleteAll(laboratorioRepository.findByUnidadePublicId(u.getPublicId()));
            unidadeRepository.delete(u);
        }
    }

    @Test
    void buscar_marcaJaCadastradoPorMatriculaEPorCpf() throws Exception {
        novoUsuario(Perfil.TECNICO, "344136", null);
        novoUsuario(Perfil.PESQUISADOR, null, CPF_MARIA);
        gateway.empregadosDaUnidade(CODIGO,
                empregadoItem("344136", "Joao Silva", CPF_JOAO, "Analista A", CODIGO),
                empregadoItem("555555", "Joao Pereira", CPF_MARIA, "Tecnico", CODIGO),
                empregadoItem("777777", "Joao Novo", "55566677788", "Pesquisador A", CODIGO));

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].jaCadastrado").value(true))
                .andExpect(jsonPath("$[0].email").value("344136@embrapa.br"))
                .andExpect(jsonPath("$[0].situacao").value("ATIVO"))
                .andExpect(jsonPath("$[1].jaCadastrado").value(true))
                .andExpect(jsonPath("$[2].jaCadastrado").value(false))
                .andExpect(jsonPath("$[2].situacao").doesNotExist())
                .andExpect(jsonPath("$[2].perfilSugerido").value("PESQUISADOR"))
                .andExpect(jsonPath("$[2].referencia").value("777777"));
    }

    @Test
    void buscar_porMatricula_usaEndpointDireto() throws Exception {
        gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", CODIGO, true);

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "344136").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].matricula").value("344136"))
                .andExpect(jsonPath("$[0].nome").value("Joao Silva"));
        gateway.verify();
    }

    @Test
    void buscar_filtraPorNomeSemAcento() throws Exception {
        gateway.empregadosDaUnidade(CODIGO,
                empregadoItem("1", "João da Silva", CPF_JOAO, "Analista", CODIGO),
                empregadoItem("2", "Maria Souza", CPF_MARIA, "Analista", CODIGO));

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "JOAO").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("João da Silva"));
    }

    @Test
    void buscar_termoCurto_retorna400() throws Exception {
        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "jo").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isBadRequest());
        gateway.verify();
    }

    @Test
    void buscar_naoRetornaCpf() throws Exception {
        gateway.colaboradoresDaUnidade(CODIGO, colaboradorItem(42, CPF_ESTAGIARIA, "Ana Estagiaria", CODIGO));

        MvcResult result = mockMvc.perform(get(BUSCA).param("tipo", "COLABORADOR").param("termo", "ana").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referencia").value("42"))
                .andExpect(jsonPath("$[0].perfilSugerido").value("ESTAGIARIO"))
                .andExpect(jsonPath("$[0].email").value(nullValue()))
                .andReturn();

        String raw = result.getResponse().getContentAsString();
        assertThat(raw).doesNotContain(CPF_ESTAGIARIA).doesNotContainIgnoringCase("cpf");
    }

    @Test
    void buscar_tecnico_retorna403() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO, null, null);

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isForbidden());
    }

    @Test
    void importar_criaAtivoComLaboratorioEPerfil() throws Exception {
        gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", CODIGO, true);

        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "TECNICO", laboratorio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("TECNICO"))
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.laboratorios[0].id").value(laboratorio.getPublicId().toString()));

        Usuario criado = comRelacionamentos(usuarioRepository.findByMatricula("344136").orElseThrow());
        assertThat(criado.getCpf()).isEqualTo(CPF_JOAO);
        assertThat(criado.getOrigem()).isEqualTo(OrigemUsuario.CORPORATIVO);
        assertThat(criado.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(criado.getAtivo()).isTrue();
        assertThat(criado.getCargo()).isEqualTo("Analista A");
        assertThat(criado.getSublotacao()).isEqualTo("Sublotação 344136");
        assertThat(criado.getUnidade().getPublicId()).isEqualTo(unidade.getPublicId());
        assertThat(criado.getSenha()).isNull();
    }

    @Test
    void importar_pessoaDeOutraUnidade_retorna403() throws Exception {
        gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", OUTRA_UNIDADE_CORPORATIVA, true);

        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "TECNICO", laboratorio)))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByMatricula("344136")).isEmpty();
    }

    @Test
    void importar_perfilAcimaDoPermitido_retorna403() throws Exception {
        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "ADMINISTRADOR", laboratorio)))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByMatricula("344136")).isEmpty();
    }

    @Test
    void importar_jaCadastrado_retorna409() throws Exception {
        novoUsuario(Perfil.TECNICO, null, CPF_JOAO);
        gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", CODIGO, true);

        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "TECNICO", laboratorio)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("JA_CADASTRADO"));
    }

    @Test
    void importar_laboratorioDeOutraUnidade_retorna400() throws Exception {
        Unidade outra = unidadeRepository.saveAndFlush(Unidade.builder().nome("Outra")
                .sigla("OT-" + java.util.UUID.randomUUID().toString().substring(0, 6)).codigoCorporativo(98).build());
        Laboratorio labAlheio = laboratorioRepository.saveAndFlush(
                Laboratorio.builder().unidade(outra).nome("Alheio").ativo(true).build());
        try {
            gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", CODIGO, true);

            mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("EMPREGADO", "344136", "TECNICO", labAlheio)))
                    .andExpect(status().isBadRequest());
            assertThat(usuarioRepository.findByMatricula("344136")).isEmpty();
        } finally {
            laboratorioRepository.delete(labAlheio);
            unidadeRepository.delete(outra);
        }
    }

    @Test
    void importar_colaborador_semEmail_ehAceito() throws Exception {
        gateway.colaboradorPorId(42, colaboradorItem(42, CPF_ESTAGIARIA, "Ana Estagiaria", CODIGO));

        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("COLABORADOR", "42", "ESTAGIARIO", laboratorio)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ESTAGIARIO"))
                .andExpect(jsonPath("$.email").doesNotExist());

        Usuario criado = comRelacionamentos(usuarioRepository.findByCpf(CPF_ESTAGIARIA).orElseThrow());
        assertThat(criado.getMatricula()).isNull();
        assertThat(criado.getEmail()).isNull();
        assertThat(criado.getLaboratorios()).extracting(Laboratorio::getPublicId).containsExactly(laboratorio.getPublicId());
    }

    @Test
    void cache_segundaBuscaNaMesmaUnidade_naoChamaGateway() throws Exception {
        // A single expectation: a second listing call would fail the mock server.
        gateway.empregadosDaUnidade(CODIGO, empregadoItem("1", "Joao Silva", CPF_JOAO, "Analista", CODIGO));

        for (String termo : new String[] {"joao", "silva"}) {
            mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", termo).header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
        gateway.verify();
    }

    @Test
    void importar_administradorGeralComHeaderDeOutraUnidade_importaNaquelaUnidade() throws Exception {
        Unidade outra = novaUnidade(98);
        Laboratorio labOutra = laboratorioRepository.saveAndFlush(
                Laboratorio.builder().unidade(outra).nome("Lab Outra").ativo(true).build());
        Usuario geral = novoUsuario(Perfil.ADMINISTRADOR_GERAL, null, null);
        gateway.empregadoPorMatricula("344136", "Joao Silva", CPF_JOAO, "Analista A", 98, true);

        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(geral))
                        .header(TenantContext.HEADER_UNIDADE, outra.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "GESTOR", labOutra)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidadeId").value(outra.getPublicId().toString()));
    }

    @Test
    void importar_gestorConcedendoGestor_retorna403() throws Exception {
        mockMvc.perform(post(IMPORTAR).header(HttpHeaders.AUTHORIZATION, bearer(gestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("EMPREGADO", "344136", "GESTOR", laboratorio)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cache_naoMisturaListagensDeUnidadesDiferentes() throws Exception {
        Unidade outra = novaUnidade(98);
        Usuario geral = novoUsuario(Perfil.ADMINISTRADOR_GERAL, null, null);
        gateway.empregadosDaUnidade(CODIGO, empregadoItem("1", "Joao da A", CPF_JOAO, "Analista", CODIGO));
        gateway.empregadosDaUnidade(98, empregadoItem("2", "Joao da B", CPF_MARIA, "Analista", 98));

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(geral)))
                .andExpect(jsonPath("$[0].nome").value("Joao da A"));
        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(geral))
                        .header(TenantContext.HEADER_UNIDADE, outra.getPublicId().toString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Joao da B"));
        gateway.verify();
    }

    @Test
    void cache_falhaDoGateway_naoEhCacheada() throws Exception {
        gateway.empregadosDaUnidadeStatus(CODIGO, HttpStatus.INTERNAL_SERVER_ERROR);
        gateway.empregadosDaUnidade(CODIGO, empregadoItem("1", "Joao Silva", CPF_JOAO, "Analista", CODIGO));

        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get(BUSCA).param("tipo", "EMPREGADO").param("termo", "joao").header(HttpHeaders.AUTHORIZATION, bearer(gestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        gateway.verify();
    }

    @Test
    void cache_expiraAposCincoMinutos() {
        MutableClock clock = new MutableClock();
        PessoaCorporativaService service = new PessoaCorporativaService(corporateApiClient, usuarioRepository,
                unidadeRepository, laboratorioRepository, clock);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                AuthenticatedUser.from(gestor), null, List.of()));
        TenantContext.definir(unidade.getPublicId());
        gateway.empregadosDaUnidade(CODIGO, empregadoItem("1", "Joao Silva", CPF_JOAO, "Analista", CODIGO));
        gateway.empregadosDaUnidade(CODIGO, empregadoItem("1", "Joao Silva", CPF_JOAO, "Analista", CODIGO));

        service.buscar(com.sgl.corporate.TipoPessoaCorporativa.EMPREGADO, "joao");
        clock.advance(Duration.ofMinutes(4));
        service.buscar(com.sgl.corporate.TipoPessoaCorporativa.EMPREGADO, "joao"); // still cached
        clock.advance(Duration.ofMinutes(2));
        service.buscar(com.sgl.corporate.TipoPessoaCorporativa.EMPREGADO, "joao"); // expired: second listing

        gateway.verify();
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-27T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public java.time.ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private Unidade novaUnidade(int codigo) {
        Unidade nova = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade " + codigo)
                .sigla("U" + codigo + "-" + java.util.UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(codigo).build());
        outrasUnidades.add(nova);
        return nova;
    }

    private Usuario comRelacionamentos(Usuario usuario) {
        return usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(usuario.getPublicId()).orElseThrow();
    }

    private String corpo(String tipo, String referencia, String perfil, Laboratorio lab) {
        return """
            {"tipo":"%s","referencia":"%s","perfil":"%s","laboratorioBaseId":"%s"}
            """.formatted(tipo, referencia, perfil, lab.getPublicId());
    }

    private Usuario novoUsuario(Perfil perfil, String matricula, String cpf) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setMatricula(matricula);
        usuario.setCpf(cpf);
        Usuario salvo = usuarioRepository.saveAndFlush(usuario);
        return salvo;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
