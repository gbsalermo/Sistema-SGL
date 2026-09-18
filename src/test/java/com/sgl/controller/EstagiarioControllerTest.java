package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.service.EstagiarioService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link EstagiarioController}. Segue o
 * mesmo padrão fixado em UnidadeControllerTest/LaboratorioControllerTest/
 * ProjetoControllerTest (ver notas de configuração lá): bean de {@link ObjectMapper}
 * via {@code @TestConfiguration} e {@code @Import(SecurityConfig.class)} para
 * reproduzir o "anyRequest().permitAll()" real em vez do HTTP Basic gerado por
 * padrão pelo slice.
 *
 * {@link EstagiarioService} é mockado - toda a regra de tenant/negócio (já coberta em
 * EstagiarioServiceTest) não é exercitada aqui, só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(EstagiarioController.class)
@Import(SecurityConfig.class)
class EstagiarioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // Diferente de UnidadeRequestDTO/LaboratorioRequestDTO (sem campos de
            // data), EstagiarioRequestDTO tem LocalDate - o ObjectMapper "cru" do
            // slice não vem com o JavaTimeModule registrado (isso é feito pela
            // JacksonAutoConfiguration real, que este @WebMvcTest não sobe), então
            // é preciso registrar explicitamente aqui para serializar/desserializar
            // LocalDate no corpo da requisição de teste.
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/estagiarios";

    private static final UUID ESTAGIARIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstagiarioService estagiarioService;

    private EstagiarioRequestDTO montarRequestDTO() {
        return new EstagiarioRequestDTO(
                ESTAGIARIO_PUBLIC_ID,
                LABORATORIO_PUBLIC_ID,
                LocalDate.of(2026, 8, 1),
                null,
                TipoBolsa.CONTRATUAL,
                "Estágio vinculado ao projeto de síntese.",
                true);
    }

    // EstagiarioResponseDTO só tem construtor a partir da entidade Estagiario
    // (que estende Usuario) - por isso montamos a entidade real via setters (igual
    // ao padrão de herança JOINED usado pelo EstagiarioService) em vez de mockar o
    // próprio DTO.
    private EstagiarioResponseDTO montarResponseDTO() {
        Unidade unidade = Unidade.builder()
                .publicId(UNIDADE_PUBLIC_ID)
                .nome("Instituto de Química")
                .sigla("IQ")
                .build();

        Laboratorio laboratorio = Laboratorio.builder()
                .publicId(LABORATORIO_PUBLIC_ID)
                .nome("Laboratório de Química Orgânica")
                .ativo(true)
                .build();

        Estagiario estagiario = new Estagiario();
        estagiario.setPublicId(ESTAGIARIO_PUBLIC_ID);
        estagiario.setNome("Maria Oliveira");
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorio);
        estagiario.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        estagiario.setTipoBolsa(TipoBolsa.CONTRATUAL);
        estagiario.setObservacao("Estágio vinculado ao projeto de síntese.");
        estagiario.setAtivo(true);

        return new EstagiarioResponseDTO(estagiario);
    }

    @Test
    void deveListarTodosOsEstagiariosERetornar200() throws Exception {
        when(estagiarioService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(ESTAGIARIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarEstagiarioPorIdERetornar200() throws Exception {
        when(estagiarioService.buscarPorId(ESTAGIARIO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome").value("Maria Oliveira"));
    }

    @Test
    void deveRetornar404QuandoEstagiarioNaoEncontrado() throws Exception {
        when(estagiarioService.buscarPorId(ESTAGIARIO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Estagiário", ESTAGIARIO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorLaboratorioERetornar200() throws Exception {
        when(estagiarioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-laboratorio")
                        .param("laboratorioId", LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarAtivosERetornar200() throws Exception {
        when(estagiarioService.listarAtivos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveCriarEstagiarioERetornar201() throws Exception {
        EstagiarioRequestDTO dto = montarRequestDTO();
        when(estagiarioService.criar(any(EstagiarioRequestDTO.class))).thenReturn(montarResponseDTO());

        // O controller usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (EstagiarioController.java linha 93) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioNome").value("Maria Oliveira"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // EstagiarioRequestDTO exige usuarioId, laboratorioId, dataInicioEstagio e
        // tipoBolsa (todos @NotNull) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarEstagiarioERetornar200() throws Exception {
        EstagiarioRequestDTO dto = montarRequestDTO();
        when(estagiarioService.atualizar(eq(ESTAGIARIO_PUBLIC_ID), any(EstagiarioRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome").value("Maria Oliveira"));
    }

    @Test
    void deveDeletarEstagiarioERetornar204() throws Exception {
        doNothing().when(estagiarioService).deletar(ESTAGIARIO_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(estagiarioService).deletar(ESTAGIARIO_PUBLIC_ID);
    }

    @Test
    void deveEncerrarEstagioERetornar200() throws Exception {
        when(estagiarioService.encerrarEstagio(ESTAGIARIO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}/encerrar", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome").value("Maria Oliveira"));
    }

    @Test
    void deveRetornar400AoEncerrarEstagioJaEncerrado() throws Exception {
        // EstagiarioService.encerrarEstagio lança BusinessRuleException quando o
        // estágio já está encerrado (EstagiarioService.java linha 244) - o
        // RestExceptionHandler mapeia essa exceção para 400.
        when(estagiarioService.encerrarEstagio(ESTAGIARIO_PUBLIC_ID))
                .thenThrow(new BusinessRuleException("O estágio já está encerrado."));

        mockMvc.perform(put(BASE_URL + "/{id}/encerrar", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isBadRequest());
    }
}
