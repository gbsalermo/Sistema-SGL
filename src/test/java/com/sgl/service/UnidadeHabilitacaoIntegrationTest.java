package com.sgl.service;

import static com.sgl.corporate.CorporateGatewayStubs.unidadeItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.sgl.corporate.CorporateGatewayStubs;
import com.sgl.corporate.CorporateGatewayTestConfig;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.JwtService;

/**
 * Enabling Unidades from the corporate list, through the real security chain and H2 with only the
 * gateway faked. Not @Transactional; everything created here is deleted afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CorporateGatewayTestConfig.class)
class UnidadeHabilitacaoIntegrationTest {

    private static final int CNPMF = 7;
    private static final int NOVA = 4321;
    private static final String CORPORATIVAS = "/api/v1/unidades/corporativas";
    private static final String HABILITAR = "/api/v1/unidades/habilitar";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private CorporateGatewayStubs gateway;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    private Unidade cnpmf;
    private Usuario administradorGeral;
    private final List<UUID> criadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        gateway.reset();
        cnpmf = unidadeRepository.saveAndFlush(Unidade.builder()
                .nome("Embrapa Mandioca e Fruticultura")
                .sigla("UH-" + UUID.randomUUID().toString().substring(0, 6))
                .codigoCorporativo(CNPMF)
                .build());
        administradorGeral = novoUsuario(Perfil.ADMINISTRADOR_GERAL);
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll(usuarioRepository.findByUnidadePublicId(cnpmf.getPublicId()));
        criadas.forEach(id -> unidadeRepository.findByPublicId(id).ifPresent(unidadeRepository::delete));
        unidadeRepository.findByCodigoCorporativo(NOVA).ifPresent(unidadeRepository::delete);
        unidadeRepository.delete(cnpmf);
    }

    @Test
    void listarCorporativas_marcaHabilitadas() throws Exception {
        gateway.unidadesEmbrapa(unidadeItem(CNPMF, "CNPMF", "Embrapa Mandioca e Fruticultura"),
                unidadeItem(NOVA, "CNPX", "Embrapa Nova"));

        mockMvc.perform(get(CORPORATIVAS).header(HttpHeaders.AUTHORIZATION, bearer(administradorGeral)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoCorporativo").value(CNPMF))
                .andExpect(jsonPath("$[0].habilitada").value(true))
                .andExpect(jsonPath("$[1].codigoCorporativo").value(NOVA))
                .andExpect(jsonPath("$[1].sigla").value("CNPX"))
                .andExpect(jsonPath("$[1].nome").value("Embrapa Nova"))
                .andExpect(jsonPath("$[1].habilitada").value(false));
    }

    @Test
    void habilitar_criaUnidadeComCodigo() throws Exception {
        gateway.unidadesEmbrapa(unidadeItem(NOVA, "CNPX-" + NOVA, "Embrapa Nova"));

        mockMvc.perform(habilitar(NOVA, administradorGeral))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sigla").value("CNPX-" + NOVA))
                .andExpect(jsonPath("$.nome").value("Embrapa Nova"));

        Unidade criada = unidadeRepository.findByCodigoCorporativo(NOVA).orElseThrow();
        criadas.add(criada.getPublicId());
        assertThat(criada.getSigla()).isEqualTo("CNPX-" + NOVA);
    }

    @Test
    void habilitar_repetida_409() throws Exception {
        mockMvc.perform(habilitar(CNPMF, administradorGeral))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("UNIDADE_JA_HABILITADA"));
    }

    @Test
    void habilitar_codigoForaDaListaCorporativa_404() throws Exception {
        gateway.unidadesEmbrapa(unidadeItem(CNPMF, "CNPMF", "Embrapa Mandioca e Fruticultura"));

        mockMvc.perform(habilitar(NOVA, administradorGeral))
                .andExpect(status().isNotFound());
        assertThat(unidadeRepository.findByCodigoCorporativo(NOVA)).isEmpty();
    }

    @Test
    void habilitar_administradorLocal_403() throws Exception {
        Usuario administrador = novoUsuario(Perfil.ADMINISTRADOR);

        mockMvc.perform(habilitar(NOVA, administrador))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(CORPORATIVAS).header(HttpHeaders.AUTHORIZATION, bearer(administrador)))
                .andExpect(status().isForbidden());
    }

    private RequestBuilder habilitar(int codigo, Usuario ator) {
        return post(HABILITAR)
                .header(HttpHeaders.AUTHORIZATION, bearer(ator))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoCorporativo\":%d}".formatted(codigo));
    }

    private Usuario novoUsuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setPerfil(perfil);
        usuario.setUnidade(cnpmf);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.generate(usuario.getPublicId());
    }
}
