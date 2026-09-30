package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.service.VinculoEstagioService;

@WebMvcTest(VinculoEstagioController.class)
@Import(SecurityConfig.class)
class VinculoEstagioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }

    private static final UUID ESTAGIARIO_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ORIENTADOR_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final UUID VINCULO_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID PARTICIPACAO_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000005");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VinculoEstagioService service;

    private NovoVinculoEstagioRequestDTO montarRequest() {
        NovoVinculoEstagioRequestDTO dto =
                new NovoVinculoEstagioRequestDTO();
        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicio(LocalDate.of(2026, 10, 1));
        dto.setDataFimPrevista(LocalDate.of(2027, 3, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setObservacao("Novo período");
        return dto;
    }

    private VinculoEstagioResponseDTO montarResponse() {
        Estagiario estagiario = new Estagiario();
        estagiario.setPublicId(ESTAGIARIO_ID);

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 10, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2027, 3, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        Atividade atividade = new Atividade();
        atividade.setPublicId(ATIVIDADE_ID);
        atividade.setNome("Atividade inicial");
        atividade.setCodigoSeg("ATV-TESTE");

        VinculoEstagioAtividade participacao =
                new VinculoEstagioAtividade();
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(
                LocalDate.of(2026, 10, 1));

        return new VinculoEstagioResponseDTO(
                vinculo,
                List.of(participacao));
    }

    @Test
    void deveCriarNovoVinculoERetornar201()
            throws Exception {

        NovoVinculoEstagioRequestDTO dto = montarRequest();

        when(service.criar(
                eq(ESTAGIARIO_ID),
                any(NovoVinculoEstagioRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/estagiarios/{estagiarioId}",
                        ESTAGIARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(VINCULO_ID.toString()))
                .andExpect(jsonPath("$.situacao")
                        .value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.participacoesAtividade")
                        .isArray())
                .andExpect(jsonPath(
                        "$.participacoesAtividade[0].atividadeId")
                        .value(ATIVIDADE_ID.toString()));
    }

    @Test
    void deveRetornar400SemAtividadeInicial()
            throws Exception {

        NovoVinculoEstagioRequestDTO dto = montarRequest();
        dto.setAtividadeId(null);

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/estagiarios/{estagiarioId}",
                        ESTAGIARIO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
