package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import com.sgl.dto.request.EncerrarVinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioAtividadeResponseDTO;
import com.sgl.model.Atividade;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.service.VinculoEstagioAtividadeService;

@WebMvcTest(VinculoEstagioAtividadeController.class)
@Import(SecurityConfig.class)
class VinculoEstagioAtividadeControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }

    private static final UUID VINCULO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID PARTICIPACAO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VinculoEstagioAtividadeService service;

    private VinculoEstagioAtividadeResponseDTO montarResponse(
            boolean encerrada) {

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setPublicId(VINCULO_ID);

        Atividade atividade = new Atividade();
        atividade.setPublicId(ATIVIDADE_ID);
        atividade.setNome("Atividade Teste");
        atividade.setCodigoSeg("ATV-TESTE");

        VinculoEstagioAtividade participacao =
                new VinculoEstagioAtividade();
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(
                LocalDate.of(2026, 3, 1));

        if (encerrada) {
            participacao.setDataFimParticipacao(
                    LocalDate.of(2026, 6, 30));
        }

        return new VinculoEstagioAtividadeResponseDTO(
                participacao);
    }

    @Test
    void deveAdicionarAtividadeAoVinculoERetornar201()
            throws Exception {

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(
                LocalDate.of(2026, 3, 1));

        when(service.adicionar(
                eq(VINCULO_ID),
                any(VinculoEstagioAtividadeRequestDTO.class)))
                .thenReturn(montarResponse(false));

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/atividades",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(PARTICIPACAO_ID.toString()))
                .andExpect(jsonPath("$.atividadeId")
                        .value(ATIVIDADE_ID.toString()))
                .andExpect(jsonPath("$.ativa").value(true));
    }

    @Test
    void deveListarHistoricoDeAtividadesERetornar200()
            throws Exception {

        when(service.listarPorVinculo(VINCULO_ID))
                .thenReturn(List.of(montarResponse(false)));

        mockMvc.perform(get(
                        "/api/v1/vinculos-estagio/{vinculoId}/atividades",
                        VINCULO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id")
                        .value(PARTICIPACAO_ID.toString()));
    }

    @Test
    void deveListarAtividadesAtivasERetornar200()
            throws Exception {

        when(service.listarAtivasPorVinculo(VINCULO_ID))
                .thenReturn(List.of(montarResponse(false)));

        mockMvc.perform(get(
                        "/api/v1/vinculos-estagio/{vinculoId}/atividades/ativas",
                        VINCULO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ativa").value(true));
    }

    @Test
    void deveEncerrarParticipacaoERetornar200()
            throws Exception {

        EncerrarVinculoEstagioAtividadeRequestDTO dto =
                new EncerrarVinculoEstagioAtividadeRequestDTO();
        dto.setDataFimParticipacao(
                LocalDate.of(2026, 6, 30));

        when(service.encerrar(
                eq(PARTICIPACAO_ID),
                any(EncerrarVinculoEstagioAtividadeRequestDTO.class)))
                .thenReturn(montarResponse(true));

        mockMvc.perform(put(
                        "/api/v1/vinculos-estagio/participacoes/{participacaoId}/encerrar",
                        PARTICIPACAO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false))
                .andExpect(jsonPath("$.dataFimParticipacao")
                        .value("2026-06-30"));
    }

    @Test
    void deveRetornar400AoAdicionarSemAtividade()
            throws Exception {

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setDataInicioParticipacao(
                LocalDate.of(2026, 3, 1));

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/atividades",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveEditarParticipacaoERetornar200()
            throws Exception {

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(
                LocalDate.of(2026, 4, 1));
        dto.setObservacao("Correção administrativa");

        when(service.atualizar(
                eq(PARTICIPACAO_ID),
                any(VinculoEstagioAtividadeRequestDTO.class)))
                .thenReturn(montarResponse(false));

        mockMvc.perform(put(
                        "/api/v1/vinculos-estagio/participacoes/{participacaoId}",
                        PARTICIPACAO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(PARTICIPACAO_ID.toString()))
                .andExpect(jsonPath("$.ativa").value(true));
    }

}
