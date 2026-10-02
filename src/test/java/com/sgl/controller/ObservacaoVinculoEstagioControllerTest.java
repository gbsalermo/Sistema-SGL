package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
import com.sgl.dto.request.ObservacaoVinculoEstagioRequestDTO;
import com.sgl.dto.request.TreinamentoSegurancaVinculoRequestDTO;
import com.sgl.dto.response.ObservacaoVinculoEstagioResponseDTO;
import com.sgl.model.ObservacaoVinculoEstagio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.EventoObservacaoVinculoEstagio;
import com.sgl.model.enums.TipoObservacaoVinculoEstagio;
import com.sgl.service.ObservacaoVinculoEstagioService;

@WebMvcTest(ObservacaoVinculoEstagioController.class)
@Import(SecurityConfig.class)
class ObservacaoVinculoEstagioControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }

    private static final UUID VINCULO_ID =
            UUID.fromString("73000000-0000-0000-0000-000000000001");
    private static final UUID OPERADOR_ID =
            UUID.fromString("73000000-0000-0000-0000-000000000002");
    private static final UUID OBSERVACAO_ID =
            UUID.fromString("73000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ObservacaoVinculoEstagioService service;

    private ObservacaoVinculoEstagioResponseDTO montarResponse(
            TipoObservacaoVinculoEstagio tipo,
            EventoObservacaoVinculoEstagio evento,
            String texto) {

        Usuario operador = new Usuario();
        operador.setPublicId(OPERADOR_ID);
        operador.setNome("Gestor Teste");

        ObservacaoVinculoEstagio observacao =
                ObservacaoVinculoEstagio.builder()
                        .publicId(OBSERVACAO_ID)
                        .tipo(tipo)
                        .evento(evento)
                        .texto(texto)
                        .usuario(operador)
                        .dataHora(LocalDateTime.of(
                                2026, 10, 2, 18, 30))
                        .build();

        return new ObservacaoVinculoEstagioResponseDTO(observacao);
    }

    @Test
    void deveAdicionarObservacaoERetornar201()
            throws Exception {

        ObservacaoVinculoEstagioRequestDTO dto =
                new ObservacaoVinculoEstagioRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setTexto("Acompanhamento operacional");

        when(service.adicionarOperacional(
                eq(VINCULO_ID),
                any(ObservacaoVinculoEstagioRequestDTO.class)))
                .thenReturn(montarResponse(
                        TipoObservacaoVinculoEstagio.OPERACIONAL,
                        EventoObservacaoVinculoEstagio.OBSERVACAO,
                        "Acompanhamento operacional"));

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/observacoes",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(OBSERVACAO_ID.toString()))
                .andExpect(jsonPath("$.evento")
                        .value("OBSERVACAO"));
    }

    @Test
    void deveAlterarTreinamentoERetornar200()
            throws Exception {

        TreinamentoSegurancaVinculoRequestDTO dto =
                new TreinamentoSegurancaVinculoRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setConcluido(true);
        dto.setObservacao("Treinamento concluído");

        when(service.alterarTreinamento(
                eq(VINCULO_ID),
                any(TreinamentoSegurancaVinculoRequestDTO.class)))
                .thenReturn(montarResponse(
                        TipoObservacaoVinculoEstagio.TREINAMENTO_SEGURANCA,
                        EventoObservacaoVinculoEstagio.TREINAMENTO_CONCLUIDO,
                        "Treinamento concluído"));

        mockMvc.perform(put(
                        "/api/v1/vinculos-estagio/{vinculoId}/treinamento-seguranca",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evento")
                        .value("TREINAMENTO_CONCLUIDO"));
    }

    @Test
    void deveListarObservacoesERetornar200()
            throws Exception {

        when(service.listar(VINCULO_ID))
                .thenReturn(List.of(montarResponse(
                        TipoObservacaoVinculoEstagio.OPERACIONAL,
                        EventoObservacaoVinculoEstagio.OBSERVACAO,
                        "Registro")));

        mockMvc.perform(get(
                        "/api/v1/vinculos-estagio/{vinculoId}/observacoes",
                        VINCULO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id")
                        .value(OBSERVACAO_ID.toString()));
    }

    @Test
    void deveRetornar400AoAdicionarObservacaoSemOperador()
            throws Exception {

        ObservacaoVinculoEstagioRequestDTO dto =
                new ObservacaoVinculoEstagioRequestDTO();
        dto.setTexto("Registro sem operador");

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/observacoes",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
