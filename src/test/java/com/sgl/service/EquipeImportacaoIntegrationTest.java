package com.sgl.service;

import static com.sgl.corporate.CorporateGatewayStubs.colaboradorItem;
import static com.sgl.corporate.CorporateGatewayStubs.empregadoItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.JwtService;

/** The responsável registers many people from the Embrapa base at once, straight into the team (spec fase 2, 7.2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class EquipeImportacaoIntegrationTest {

    private static final int CODIGO = 77;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private PessoaCorporativaService pessoaService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Unidade unidade;
    private Laboratorio lab;
    private Laboratorio outroLab;
    private Usuario responsavel;

    @BeforeEach
    void setUp() {
        gateway.reset();
        pessoaService.clearCache();
        unidade = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade importação")
                .sigla("IM-" + UUID.randomUUID().toString().substring(0, 6)).codigoCorporativo(CODIGO).build());
        responsavel = novoUsuario(Perfil.PESQUISADOR, null);
        lab = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Lab").ativo(true).responsavel(responsavel).build());
        outroLab = laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome("Outro").ativo(true).build());
    }

    @AfterEach
    void tearDown() {
        pessoaService.clearCache();
        List<Laboratorio> labs = laboratorioRepository.findByUnidadePublicId(unidade.getPublicId());
        labs.forEach(l -> l.setResponsavel(null));
        laboratorioRepository.saveAllAndFlush(labs);
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(unidade.getPublicId()));
        laboratorioRepository.deleteAll(labs);
        unidadeRepository.delete(unidade);
    }

    @Test
    void loteComUmJaCadastrado_cadastraOsOutrosNaEquipeSemPapeis() throws Exception {
        novoUsuario(Perfil.TECNICO, "1002");
        gateway.empregadoPorMatricula("1001", "Joao Novo", "11111111111", "Analista A", CODIGO, true);
        gateway.empregadoPorMatricula("1002", "Maria Antiga", "22222222222", "Tecnico A", CODIGO, true);
        gateway.colaboradorPorId(55, colaboradorItem(55, "33333333333", "Bia Bolsista", CODIGO));

        mockMvc.perform(importar(lab, responsavel, """
                        [{"tipo":"EMPREGADO","referencia":"1001"},
                         {"tipo":"EMPREGADO","referencia":"1002"},
                         {"tipo":"COLABORADOR","referencia":"55","categoriaCargo":"ESTAGIARIO"}]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].resultado").value("CADASTRADA"))
                .andExpect(jsonPath("$[1].resultado").value("JA_CADASTRADA"))
                .andExpect(jsonPath("$[2].resultado").value("CADASTRADA"));

        Usuario joao = comLaboratorios(usuarioRepository.findByMatricula("1001").orElseThrow());
        assertThat(joao.getSituacao()).isEqualTo(SituacaoUsuario.ATIVO);
        assertThat(joao.getPapeis()).isEmpty();
        assertThat(joao.getCategoriaCargo()).isEqualTo(CategoriaCargo.ANALISTA);
        assertThat(joao.getLaboratorios()).extracting(Laboratorio::getPublicId).containsExactly(lab.getPublicId());
        Usuario bia = comLaboratorios(usuarioRepository.findByCpf("33333333333").orElseThrow());
        assertThat(bia.getCategoriaCargo()).isEqualTo(CategoriaCargo.ESTAGIARIO);
        assertThat(bia.getLaboratorios()).extracting(Laboratorio::getPublicId).containsExactly(lab.getPublicId());
    }

    @Test
    void mesmaPessoaDuasVezesNoLote_segundaVemComoJaCadastrada() throws Exception {
        gateway.empregadoPorMatricula("1001", "Joao Novo", "11111111111", "Analista A", CODIGO, true);
        gateway.empregadoPorMatricula("1001", "Joao Novo", "11111111111", "Analista A", CODIGO, true);

        mockMvc.perform(importar(lab, responsavel, """
                        [{"tipo":"EMPREGADO","referencia":"1001"},{"tipo":"EMPREGADO","referencia":"1001"}]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultado").value("CADASTRADA"))
                .andExpect(jsonPath("$[1].resultado").value("JA_CADASTRADA"));
    }

    @Test
    void pessoaQueNaoExisteNaBase_vemComoNaoEncontradaSemDerrubarOLote() throws Exception {
        gateway.empregadoPorMatriculaStatus("9999", HttpStatus.NOT_FOUND);
        gateway.empregadoPorMatricula("1001", "Joao Novo", "11111111111", "Analista A", CODIGO, true);

        mockMvc.perform(importar(lab, responsavel, """
                        [{"tipo":"EMPREGADO","referencia":"9999"},{"tipo":"EMPREGADO","referencia":"1001"}]"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultado").value("NAO_ENCONTRADA"))
                .andExpect(jsonPath("$[1].resultado").value("CADASTRADA"));
    }

    @Test
    void loteComMaisDe50_400() throws Exception {
        String itens = IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "{\"tipo\":\"EMPREGADO\",\"referencia\":\"" + (2000 + i) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));

        mockMvc.perform(importar(lab, responsavel, itens)).andExpect(status().isBadRequest());
    }

    @Test
    void importarNoLaboratorioDeOutro_403() throws Exception {
        mockMvc.perform(importar(outroLab, responsavel, "[{\"tipo\":\"EMPREGADO\",\"referencia\":\"1001\"}]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorSozinhoNaoImporta_403() throws Exception {
        Usuario gestor = novoUsuario(Perfil.GESTOR, null);

        mockMvc.perform(importar(lab, gestor, "[{\"tipo\":\"EMPREGADO\",\"referencia\":\"1001\"}]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidatosNaoIncluemQuemJaEstaNoSgl() throws Exception {
        novoUsuario(Perfil.TECNICO, "1002");
        gateway.empregadosDaUnidade(CODIGO,
                empregadoItem("1001", "Ana Nova", "11111111111", "Analista A", CODIGO),
                empregadoItem("1002", "Ana Antiga", "22222222222", "Tecnico A", CODIGO));

        mockMvc.perform(get(equipe(lab) + "/candidatos-embrapa").param("tipo", "EMPREGADO").param("termo", "ana")
                        .header(HttpHeaders.AUTHORIZATION, bearer(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referencia").value("1001"))
                .andExpect(jsonPath("$[0].categoriaSugerida").value("ANALISTA"));
    }

    @Test
    void candidatosComMenosDeTresLetras_400() throws Exception {
        mockMvc.perform(get(equipe(lab) + "/candidatos-embrapa").param("tipo", "EMPREGADO").param("termo", "an")
                        .header(HttpHeaders.AUTHORIZATION, bearer(responsavel)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidatosDeOutroLaboratorio_403() throws Exception {
        mockMvc.perform(get(equipe(outroLab) + "/candidatos-embrapa").param("tipo", "EMPREGADO").param("termo", "ana")
                        .header(HttpHeaders.AUTHORIZATION, bearer(responsavel)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadastroEhAuditadoComOLaboratorio(CapturedOutput output) throws Exception {
        gateway.empregadoPorMatricula("1001", "Joao Novo", "11111111111", "Analista A", CODIGO, true);

        mockMvc.perform(importar(lab, responsavel, "[{\"tipo\":\"EMPREGADO\",\"referencia\":\"1001\"}]"))
                .andExpect(status().isOk());

        assertThat(output.getOut()).contains("acao=USUARIO_IMPORTADO ator=" + responsavel.getPublicId())
                .contains("laboratorio=" + lab.getPublicId());
    }

    private static String equipe(Laboratorio laboratorio) {
        return "/api/v1/laboratorios/" + laboratorio.getPublicId() + "/equipe";
    }

    private RequestBuilder importar(Laboratorio laboratorio, Usuario ator, String itensJson) {
        return post(equipe(laboratorio) + "/importar")
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\":" + itensJson + "}");
    }

    private Usuario comLaboratorios(Usuario usuario) {
        return usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(usuario.getPublicId()).orElseThrow();
    }

    private Usuario novoUsuario(Perfil perfil, String matricula) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setMatricula(matricula);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
