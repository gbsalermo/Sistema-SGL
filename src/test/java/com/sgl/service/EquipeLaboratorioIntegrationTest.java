package com.sgl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;
import com.sgl.security.JwtService;

/**
 * The team of a laboratory is managed by its responsável, or by an administrator of the Unidade;
 * a GESTOR alone no longer manages teams (spec fase 2, 7.1 and 7.2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class EquipeLaboratorioIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private LaboratorioRepository laboratorioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VinculoOperacionalUsuarioRepository vinculoRepository;

    private Unidade unidade;
    private Laboratorio labA;
    private Laboratorio labB;
    private Usuario responsavelA;
    private Usuario responsavelB;
    private final List<Unidade> unidades = new ArrayList<>();

    @BeforeEach
    void setUp() {
        unidade = novaUnidade();
        responsavelA = novoUsuario(Perfil.PESQUISADOR);
        responsavelB = novoUsuario(Perfil.PESQUISADOR);
        labA = novoLaboratorio("Lab A", responsavelA);
        labB = novoLaboratorio("Lab B", responsavelB);
        responsavelA.substituirLaboratorios(List.of(labA));
        responsavelA = usuarioRepository.saveAndFlush(responsavelA);
    }

    @AfterEach
    void tearDown() {
        for (Unidade u : unidades) {
            vinculoRepository.deleteAll(vinculoRepository.findByUsuarioUnidadePublicId(u.getPublicId()));
            List<Laboratorio> labs = laboratorioRepository.findByUnidadePublicId(u.getPublicId());
            labs.forEach(l -> l.setResponsavel(null));
            laboratorioRepository.saveAllAndFlush(labs);
            usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(u.getPublicId()));
            laboratorioRepository.deleteAll(labs);
            unidadeRepository.delete(u);
        }
    }

    @Test
    void responsavelListaIncluiERetiraNoProprioLaboratorio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labA, tecnico, responsavelA)).andExpect(status().isOk());
        mockMvc.perform(get(equipe(labA)).header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.usuarioId == '%s')].permanente".formatted(tecnico.getPublicId())).value(true))
                .andExpect(jsonPath("$[?(@.usuarioId == '%s')].responsavel".formatted(responsavelA.getPublicId())).value(true));
        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNoContent());

        assertThat(laboratoriosDe(tecnico)).isEmpty();
    }

    @Test
    void responsavelNaoMexeNaEquipeDeOutroLaboratorio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labB, tecnico, responsavelA)).andExpect(status().isForbidden());
        mockMvc.perform(get(equipe(labB)).header(HttpHeaders.AUTHORIZATION, bearer(responsavelA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestorSozinhoNaoGereEquipe() throws Exception {
        Usuario gestor = novoUsuario(Perfil.GESTOR);
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labA, tecnico, gestor)).andExpect(status().isForbidden());
    }

    @Test
    void gestorQueEhResponsavelGereSoOSeuLaboratorio() throws Exception {
        Usuario gestor = novoUsuario(Perfil.GESTOR);
        Laboratorio labDoGestor = novoLaboratorio("Lab do Gestor", gestor);
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labDoGestor, tecnico, gestor)).andExpect(status().isOk());
        mockMvc.perform(incluir(labA, tecnico, gestor)).andExpect(status().isForbidden());
    }

    @Test
    void solicitanteQueNaoEhResponsavelRecebe403() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        Usuario outro = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labA, outro, tecnico)).andExpect(status().isForbidden());
        mockMvc.perform(get(equipe(labA)).header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorLocalGereQualquerLaboratorioDaUnidade() throws Exception {
        Usuario admin = novoUsuario(Perfil.ADMINISTRADOR);
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labB, tecnico, admin)).andExpect(status().isOk());
        assertThat(laboratoriosDe(tecnico)).containsExactly(labB.getPublicId());
    }

    @Test
    void ninguemSeIncluiNemSeRetira() throws Exception {
        Usuario admin = novoUsuario(Perfil.ADMINISTRADOR);

        mockMvc.perform(incluir(labA, admin, admin)).andExpect(status().isForbidden());
        mockMvc.perform(retirar(labA, responsavelA, responsavelA)).andExpect(status().isForbidden());
    }

    @Test
    void naoRetiraOResponsavelDoLaboratorio() throws Exception {
        Usuario admin = novoUsuario(Perfil.ADMINISTRADOR);

        mockMvc.perform(retirar(labA, responsavelA, admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESPONSAVEL_NA_EQUIPE"));
    }

    @Test
    void retirarOUltimoLaboratorioDeixaAPessoaAtivaSemLaboratorio() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        tecnico.substituirLaboratorios(List.of(labA));
        tecnico = usuarioRepository.saveAndFlush(tecnico);

        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNoContent());

        Usuario salvo = usuarioRepository.findByPublicId(tecnico.getPublicId()).orElseThrow();
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(laboratoriosDe(tecnico)).isEmpty();
    }

    @Test
    void retirarTiraALaboratorioEEncerraOAcessoTemporarioMasMantemOutrosLaboratorios() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        tecnico.substituirLaboratorios(List.of(labA, labB));
        tecnico = usuarioRepository.saveAndFlush(tecnico);
        VinculoOperacionalUsuario vinculo = novoVinculo(tecnico, labA);

        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNoContent());

        assertThat(laboratoriosDe(tecnico)).containsExactly(labB.getPublicId());
        VinculoOperacionalUsuario encerrado = vinculoRepository.findById(vinculo.getId()).orElseThrow();
        assertThat(encerrado.getEncerrado()).isTrue();
        assertThat(encerrado.getDataFim()).isEqualTo(LocalDate.now());
    }

    @Test
    void retirarQuemSoTemAcessoTemporarioEncerraOVinculo() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);
        VinculoOperacionalUsuario vinculo = novoVinculo(tecnico, labA);

        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNoContent());

        assertThat(vinculoRepository.findById(vinculo.getId()).orElseThrow().getEncerrado()).isTrue();
    }

    @Test
    void retirarQuemNaoEhDaEquipeDa404() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNotFound());
    }

    @Test
    void incluirEhIdempotente() throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labA, tecnico, responsavelA)).andExpect(status().isOk());
        mockMvc.perform(incluir(labA, tecnico, responsavelA)).andExpect(status().isOk());

        assertThat(laboratoriosDe(tecnico)).containsExactly(labA.getPublicId());
    }

    @Test
    void responsavelIncluiUmGestorNaEquipe() throws Exception {
        Usuario gestor = novoUsuario(Perfil.GESTOR);

        mockMvc.perform(incluir(labA, gestor, responsavelA)).andExpect(status().isOk());
        assertThat(laboratoriosDe(gestor)).containsExactly(labA.getPublicId());
    }

    @Test
    void naoIncluiPendente() throws Exception {
        Usuario pendente = novoUsuario(Perfil.ESTAGIARIO);
        pendente.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);
        pendente = usuarioRepository.saveAndFlush(pendente);

        mockMvc.perform(incluir(labA, pendente, responsavelA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("USUARIO_PENDENTE"));
    }

    @Test
    void inclusaoERetiradaSaoAuditadas(CapturedOutput output) throws Exception {
        Usuario tecnico = novoUsuario(Perfil.TECNICO);

        mockMvc.perform(incluir(labA, tecnico, responsavelA)).andExpect(status().isOk());
        mockMvc.perform(retirar(labA, tecnico, responsavelA)).andExpect(status().isNoContent());

        assertThat(output.getOut())
                .contains("acao=EQUIPE_INCLUSAO ator=" + responsavelA.getPublicId() + " alvo=" + tecnico.getPublicId())
                .contains("acao=EQUIPE_RETIRADA ator=" + responsavelA.getPublicId() + " alvo=" + tecnico.getPublicId());
    }

    private static String equipe(Laboratorio lab) {
        return "/api/v1/laboratorios/" + lab.getPublicId() + "/equipe";
    }

    private RequestBuilder incluir(Laboratorio lab, Usuario alvo, Usuario ator) {
        return post(equipe(lab) + "/" + alvo.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(ator));
    }

    private RequestBuilder retirar(Laboratorio lab, Usuario alvo, Usuario ator) {
        return delete(equipe(lab) + "/" + alvo.getPublicId()).header(HttpHeaders.AUTHORIZATION, bearer(ator));
    }

    private List<UUID> laboratoriosDe(Usuario usuario) {
        return usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(usuario.getPublicId()).orElseThrow()
                .getLaboratorios().stream().map(Laboratorio::getPublicId).toList();
    }

    private Unidade novaUnidade() {
        Unidade nova = unidadeRepository.saveAndFlush(Unidade.builder().nome("Unidade equipe")
                .sigla("EQ-" + UUID.randomUUID().toString().substring(0, 6)).build());
        unidades.add(nova);
        return nova;
    }

    private Laboratorio novoLaboratorio(String nome, Usuario responsavel) {
        return laboratorioRepository.saveAndFlush(Laboratorio.builder()
                .unidade(unidade).nome(nome).ativo(true).responsavel(responsavel).build());
    }

    private Usuario novoUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private VinculoOperacionalUsuario novoVinculo(Usuario usuario, Laboratorio lab) {
        VinculoOperacionalUsuario vinculo = new VinculoOperacionalUsuario();
        vinculo.setUsuario(usuario);
        vinculo.setLaboratorio(lab);
        vinculo.setDataInicio(LocalDate.now().minusDays(1));
        vinculo.setDataFim(LocalDate.now().plusDays(30));
        return vinculoRepository.saveAndFlush(vinculo);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
