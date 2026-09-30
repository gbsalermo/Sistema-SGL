package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.service.EstagiarioService;

/**
 * Teste de fatia web do controller de Estagiários.
 *
 * As regras de vínculo institucional ficam cobertas em EstagiarioServiceTest;
 * aqui validamos contrato HTTP, serialização e validação do request.
 */
@WebMvcTest(EstagiarioController.class)
@Import(SecurityConfig.class)
class EstagiarioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
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
    private static final UUID ORIENTADOR_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID VINCULO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ATIVIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstagiarioService estagiarioService;

    private EstagiarioRequestDTO montarRequestDTO() {
        EstagiarioRequestDTO dto = new EstagiarioRequestDTO();
        dto.setUsuarioId(ESTAGIARIO_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDataInicioEstagio(LocalDate.of(2026, 8, 1));
        dto.setDataFimEstagio(LocalDate.of(2027, 1, 31));
        dto.setTipoBolsa(TipoBolsa.CONTRATUAL);
        dto.setObservacao("Estágio vinculado ao projeto de síntese.");
        dto.setOrientadorId(ORIENTADOR_PUBLIC_ID);
        dto.setAtividadeId(ATIVIDADE_PUBLIC_ID);
        return dto;
    }

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
        estagiario.setAtivo(true);

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setPublicId(VINCULO_PUBLIC_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 8, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2027, 1, 31));
        vinculo.setTipoBolsa(TipoBolsa.CONTRATUAL);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setObservacao("Estágio vinculado ao projeto de síntese.");

        return new EstagiarioResponseDTO(
                estagiario,
                List.of(vinculo));
    }

    @Test
    void deveListarTodosOsEstagiariosERetornar200() throws Exception {
        when(estagiarioService.listarTodos())
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id")
                        .value(ESTAGIARIO_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$[0].vinculos").isArray())
                .andExpect(jsonPath("$[0].vinculos[0].situacao")
                        .value("EM_ANDAMENTO"));
    }

    @Test
    void deveBuscarEstagiarioPorIdERetornar200() throws Exception {
        when(estagiarioService.buscarPorId(ESTAGIARIO_PUBLIC_ID))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioNome")
                        .value("Maria Oliveira"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRetornar404QuandoEstagiarioNaoEncontrado() throws Exception {
        when(estagiarioService.buscarPorId(ESTAGIARIO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException(
                        "Estagiário",
                        ESTAGIARIO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorLaboratorioERetornar200() throws Exception {
        when(estagiarioService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-laboratorio")
                        .param(
                                "laboratorioId",
                                LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratorioId")
                        .value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarAtivosERetornar200() throws Exception {
        when(estagiarioService.listarAtivos())
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveCriarEstagiarioERetornar201() throws Exception {
        EstagiarioRequestDTO dto = montarRequestDTO();

        when(estagiarioService.criar(any(EstagiarioRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioNome")
                        .value("Maria Oliveira"))
                .andExpect(jsonPath("$.vinculos[0].situacao")
                        .value("EM_ANDAMENTO"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoAtualizarDiretamente() throws Exception {
        EstagiarioRequestDTO dto = montarRequestDTO();

        when(estagiarioService.atualizar(
                eq(ESTAGIARIO_PUBLIC_ID),
                any(EstagiarioRequestDTO.class)))
                .thenThrow(new BusinessRuleException(
                        "A atualização direta do estágio foi substituída "
                                + "pelo gerenciamento de vínculos institucionais."));

        mockMvc.perform(put(BASE_URL + "/{id}", ESTAGIARIO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoDeletarDiretamente() throws Exception {
        doThrow(new BusinessRuleException(
                "Estagiários não podem ser excluídos diretamente. "
                        + "O histórico institucional deve ser preservado."))
                .when(estagiarioService)
                .deletar(ESTAGIARIO_PUBLIC_ID);

        mockMvc.perform(delete(
                        BASE_URL + "/{id}",
                        ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isBadRequest());

        verify(estagiarioService)
                .deletar(ESTAGIARIO_PUBLIC_ID);
    }

    @Test
    void deveRetornar400AoEncerrarPeloFluxoLegado() throws Exception {
        when(estagiarioService.encerrarEstagio(ESTAGIARIO_PUBLIC_ID))
                .thenThrow(new BusinessRuleException(
                        "O encerramento direto do estágio foi substituído "
                                + "pelo fluxo de encerramento do vínculo institucional."));

        mockMvc.perform(put(
                        BASE_URL + "/{id}/encerrar",
                        ESTAGIARIO_PUBLIC_ID))
                .andExpect(status().isBadRequest());
    }
}
