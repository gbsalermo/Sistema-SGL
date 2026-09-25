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
import com.sgl.dto.request.AtividadeRequestDTO;
import com.sgl.dto.request.ProrrogacaoRequestDTO;
import com.sgl.dto.response.AtividadeResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.AtividadeService;
import com.sgl.service.ProrrogacaoService;

@WebMvcTest(AtividadeController.class)
@Import(SecurityConfig.class)
class AtividadeControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    private static final String BASE_URL = "/api/v1/atividades";

    private static final UUID PROJETO_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID SCI_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000402");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000403");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AtividadeService atividadeService;

    @MockitoBean
    private ProrrogacaoService prorrogacaoService;

    private AtividadeRequestDTO montarRequest() {
        AtividadeRequestDTO dto = new AtividadeRequestDTO();
        dto.setSciId(SCI_ID);
        dto.setCodigoSeg("95.95.95.001.01.01.001");
        dto.setNome("Atividade Teste");
        dto.setResponsavel("Pesquisador");
        dto.setDataInicio(LocalDate.of(2026, 3, 1));
        dto.setDataFim(LocalDate.of(2026, 6, 30));
        dto.setAtivo(true);
        return dto;
    }

    private AtividadeResponseDTO montarResponse() {
        Projeto projeto = Projeto.builder()
                .publicId(PROJETO_ID)
                .nome("Projeto Pai")
                .codigoSeg("95.95.95.001.01.00")
                .build();

        Sci sci = Sci.builder()
                .publicId(SCI_ID)
                .projeto(projeto)
                .codigoSeg("95.95.95.001.01.01")
                .nome("SCI Pai")
                .build();

        Atividade atividade = Atividade.builder()
                .publicId(ATIVIDADE_ID)
                .sci(sci)
                .codigoSeg("95.95.95.001.01.01.001")
                .nome("Atividade Teste")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 3, 1))
                .dataFim(LocalDate.of(2026, 6, 30))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build();

        return new AtividadeResponseDTO(atividade);
    }

    @Test
    void deveCriarAtividadeERetornar201() throws Exception {
        when(atividadeService.criar(any(AtividadeRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ATIVIDADE_ID.toString()))
                .andExpect(jsonPath("$.sciId").value(SCI_ID.toString()))
                .andExpect(jsonPath("$.projetoId").value(PROJETO_ID.toString()))
                .andExpect(jsonPath("$.codigoSeg").value("95.95.95.001.01.01.001"));
    }

    @Test
    void deveRetornar400AoCriarAtividadeComCorpoInvalido() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarAtividadesERetornar200() throws Exception {
        when(atividadeService.listarTodos())
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(ATIVIDADE_ID.toString()));
    }

    @Test
    void deveBuscarAtividadePorIdERetornar200() throws Exception {
        when(atividadeService.buscarPorId(ATIVIDADE_ID))
                .thenReturn(montarResponse());

        mockMvc.perform(get(BASE_URL + "/{id}", ATIVIDADE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Atividade Teste"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.situacaoExecucao").value("EM_ANDAMENTO_NO_PRAZO"));
    }

    @Test
    void deveRetornar404QuandoAtividadeNaoEncontrada() throws Exception {
        when(atividadeService.buscarPorId(ATIVIDADE_ID))
                .thenThrow(new ResourceNotFoundException("Atividade", ATIVIDADE_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", ATIVIDADE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarAtividadesPorSciERetornar200() throws Exception {
        when(atividadeService.listarPorSci(SCI_ID))
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL + "/por-sci")
                        .param("sciId", SCI_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sciId").value(SCI_ID.toString()));
    }

    @Test
    void deveListarAtividadesPorProjetoERetornar200() throws Exception {
        when(atividadeService.listarPorProjeto(PROJETO_ID))
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL + "/por-projeto")
                        .param("projetoId", PROJETO_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projetoId").value(PROJETO_ID.toString()));
    }

    @Test
    void deveListarAtividadesAtivasPorSciERetornar200() throws Exception {
        when(atividadeService.listarAtivosPorSci(SCI_ID))
                .thenReturn(List.of(montarResponse()));

        mockMvc.perform(get(BASE_URL + "/por-sci/ativos")
                        .param("sciId", SCI_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveAtualizarAtividadeERetornar200() throws Exception {
        when(atividadeService.atualizar(eq(ATIVIDADE_ID), any(AtividadeRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(put(BASE_URL + "/{id}", ATIVIDADE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ATIVIDADE_ID.toString()));
    }

    @Test
    void deveDesativarAtividadeERetornar204() throws Exception {
        doNothing().when(atividadeService).deletar(ATIVIDADE_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", ATIVIDADE_ID))
                .andExpect(status().isNoContent());

        verify(atividadeService).deletar(ATIVIDADE_ID);
    }

    @Test
    void deveProrrogarAtividadeERetornar201() throws Exception {
        String body = """
                {
                  "usuarioId": "00000000-0000-0000-0000-000000000499",
                  "novaDataFim": "2026-07-31",
                  "justificativa": "Prorrogação de teste"
                }
                """;

        when(prorrogacaoService.prorrogarAtividade(
                eq(ATIVIDADE_ID), any(ProrrogacaoRequestDTO.class)))
                .thenReturn(null);

        mockMvc.perform(post(BASE_URL + "/{id}/prorrogacoes", ATIVIDADE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void deveListarProrrogacoesDaAtividadeERetornar200() throws Exception {
        when(prorrogacaoService.listarHistoricoAtividade(ATIVIDADE_ID))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/{id}/prorrogacoes", ATIVIDADE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

}
