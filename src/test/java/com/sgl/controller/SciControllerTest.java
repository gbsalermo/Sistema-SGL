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
import com.sgl.config.SecurityConfig;
import com.sgl.dto.request.CorrecaoCodigoSegRequestDTO;
import com.sgl.dto.request.ProrrogacaoRequestDTO;
import com.sgl.dto.request.SciRequestDTO;
import com.sgl.dto.response.SciResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.SciService;
import com.sgl.service.CorrecaoCodigoSegService;
import com.sgl.service.ProrrogacaoService;

@WebMvcTest(SciController.class)
@Import(SecurityConfig.class)
class SciControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    private static final String BASE_URL = "/api/v1/scis";

    private static final UUID PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SciService sciService;

    @MockitoBean
    private ProrrogacaoService prorrogacaoService;

    @MockitoBean
    private CorrecaoCodigoSegService correcaoCodigoSegService;

    private SciRequestDTO montarRequest() {
        SciRequestDTO dto = new SciRequestDTO();
        dto.setProjetoId(PROJETO_ID);
        dto.setCodigoSeg("95.95.95.001.01.01");
        dto.setNome("SCI Teste");
        dto.setResponsavel("Pesquisador");
        dto.setDataInicio(LocalDate.of(2026, 2, 1));
        dto.setDataFim(LocalDate.of(2026, 6, 30));
        dto.setAtivo(true);
        return dto;
    }

    private SciResponseDTO montarResponse() {
        Projeto projeto = Projeto.builder()
                .publicId(PROJETO_ID)
                .nome("Projeto Pai")
                .codigoSeg("95.95.95.001.01.00")
                .build();

        Sci sci = Sci.builder()
                .publicId(SCI_ID)
                .projeto(projeto)
                .codigoSeg("95.95.95.001.01.01")
                .nome("SCI Teste")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 6, 30))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();

        return new SciResponseDTO(sci);
    }

    @Test
    void deveCriarSciERetornar201() throws Exception {
        when(sciService.criar(any(SciRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SCI_ID.toString()))
                .andExpect(jsonPath("$.projetoId").value(PROJETO_ID.toString()))
                .andExpect(jsonPath("$.codigoSeg").value("95.95.95.001.01.01"));
    }

    @Test
    void deveRetornar400AoCriarSciComCorpoInvalido() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarSciERetornar200() throws Exception {
        when(sciService.listarTodos())
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(SCI_ID.toString()));
    }

    @Test
    void deveBuscarSciPorIdERetornar200() throws Exception {
        when(sciService.buscarPorId(SCI_ID))
                .thenReturn(montarResponse());

        mockMvc.perform(get(BASE_URL + "/{id}", SCI_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("SCI Teste"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.situacaoExecucao").value("EM_ANDAMENTO_NO_PRAZO"));
    }

    @Test
    void deveRetornar404QuandoSciNaoEncontrado() throws Exception {
        when(sciService.buscarPorId(SCI_ID))
                .thenThrow(new ResourceNotFoundException("SCI", SCI_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", SCI_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarSciPorProjetoERetornar200() throws Exception {
        when(sciService.listarPorProjeto(PROJETO_ID))
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL + "/por-projeto")
                        .param("projetoId", PROJETO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projetoId").value(PROJETO_ID.toString()));
    }

    @Test
    void deveListarSciAtivosPorProjetoERetornar200() throws Exception {
        when(sciService.listarAtivosPorProjeto(PROJETO_ID))
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL + "/por-projeto/ativos")
                        .param("projetoId", PROJETO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarSciERetornar200() throws Exception {
        when(sciService.atualizar(eq(SCI_ID), any(SciRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(put(BASE_URL + "/{id}", SCI_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SCI_ID.toString()));
    }

    @Test
    void deveDesativarSciERetornar204() throws Exception {
        doNothing().when(sciService).deletar(SCI_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", SCI_ID))
                .andExpect(status().isNoContent());

        verify(sciService).deletar(SCI_ID);
    }

    @Test
    void deveProrrogarSciERetornar201() throws Exception {
        String body = """
                {
                  "usuarioId": "00000000-0000-0000-0000-000000000299",
                  "novaDataFim": "2026-07-31",
                  "justificativa": "Prorrogação de teste"
                }
                """;

        when(prorrogacaoService.prorrogarSci(
                eq(SCI_ID), any(ProrrogacaoRequestDTO.class)))
                .thenReturn(null);

        mockMvc.perform(post(BASE_URL + "/{id}/prorrogacoes", SCI_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void deveListarProrrogacoesDoSciERetornar200() throws Exception {
        when(prorrogacaoService.listarHistoricoSci(SCI_ID))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/{id}/prorrogacoes", SCI_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    void deveCorrigirCodigoSegDoSciERetornar201() throws Exception {
        String body = """
                {
                  "usuarioId": "00000000-0000-0000-0000-000000000299",
                  "novoCodigoSeg": "95.95.95.001.01.02",
                  "justificativa": "Correção de digitação"
                }
                """;

        when(correcaoCodigoSegService.corrigirSci(
                eq(SCI_ID),
                any(CorrecaoCodigoSegRequestDTO.class)))
                .thenReturn(List.of());

        mockMvc.perform(post(BASE_URL + "/{id}/correcoes-codigo-seg", SCI_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveListarCorrecoesCodigoSegDoSciERetornar200() throws Exception {
        when(correcaoCodigoSegService.listarHistoricoSci(SCI_ID))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/{id}/correcoes-codigo-seg", SCI_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

}
