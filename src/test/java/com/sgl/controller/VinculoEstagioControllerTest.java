package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.request.ProrrogarBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovaBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.SincronizacaoVinculoEstagioRequestDTO;
import com.sgl.dto.response.HistoricoSincronizacaoVinculoEstagioResponseDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.service.SincronizacaoVinculoEstagioService;
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

    @MockitoBean
    private SincronizacaoVinculoEstagioService sincronizacaoService;

    private NovoVinculoEstagioRequestDTO montarRequest() {
        NovoVinculoEstagioRequestDTO dto =
                new NovoVinculoEstagioRequestDTO();
        dto.setOrientadorId(ORIENTADOR_ID);
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicio(LocalDate.of(2026, 10, 1));
        dto.setDataFimPrevista(LocalDate.of(2027, 3, 31));
        dto.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        dto.setFormacao(FormacaoEstagiario.GRADUACAO);
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

    @Test
    void deveSincronizarVinculoInstitucional()
            throws Exception {

        SincronizacaoVinculoEstagioRequestDTO dto =
                new SincronizacaoVinculoEstagioRequestDTO();

        dto.setOrigem(
                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL);
        dto.setReferenciaEvento("EVT-001");
        dto.setReferenciaInstitucional("BOLSA-001");
        dto.setSituacao(SituacaoEstagio.PRORROGADO);
        dto.setDataFimPrevista(LocalDate.of(2027, 6, 30));

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setPublicId(VINCULO_ID);

        HistoricoSincronizacaoVinculoEstagio historico =
                HistoricoSincronizacaoVinculoEstagio.builder()
                        .publicId(UUID.randomUUID())
                        .vinculoEstagio(vinculo)
                        .tipoEvento(
                                TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO)
                        .origem(
                                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL)
                        .referenciaEvento("EVT-001")
                        .situacaoAnterior(SituacaoEstagio.EM_ANDAMENTO)
                        .situacaoNova(SituacaoEstagio.PRORROGADO)
                        .dataFimPrevistaAnterior(
                                LocalDate.of(2027, 3, 31))
                        .dataFimPrevistaNova(
                                LocalDate.of(2027, 6, 30))
                        .build();

        when(sincronizacaoService.sincronizar(
                eq(VINCULO_ID),
                any(SincronizacaoVinculoEstagioRequestDTO.class)))
                .thenReturn(
                        new HistoricoSincronizacaoVinculoEstagioResponseDTO(
                                historico));

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/sincronizacoes-institucionais",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vinculoId")
                        .value(VINCULO_ID.toString()))
                .andExpect(jsonPath("$.tipoEvento")
                        .value("PRORROGACAO"))
                .andExpect(jsonPath("$.situacaoNova")
                        .value("PRORROGADO"))
                .andExpect(jsonPath("$.referenciaEvento")
                        .value("EVT-001"));
    }

    @Test
    void deveRetornar400QuandoSituacaoDaSincronizacaoNaoForInformada()
            throws Exception {

        SincronizacaoVinculoEstagioRequestDTO dto =
                new SincronizacaoVinculoEstagioRequestDTO();

        dto.setOrigem(
                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL);
        dto.setDataFimPrevista(LocalDate.of(2027, 6, 30));

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/sincronizacoes-institucionais",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveProrrogarBolsaERetornar200()
            throws Exception {

        ProrrogarBolsaVinculoEstagioRequestDTO dto =
                new ProrrogarBolsaVinculoEstagioRequestDTO();
        dto.setNovaDataFimPrevista(LocalDate.of(2027, 6, 30));

        when(service.prorrogarBolsaLocal(
                eq(VINCULO_ID),
                any(ProrrogarBolsaVinculoEstagioRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(put(
                        "/api/v1/vinculos-estagio/{vinculoId}/prorrogar-bolsa",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(VINCULO_ID.toString()));
    }

    @Test
    void deveRegistrarNovaBolsaERetornar201()
            throws Exception {

        NovaBolsaVinculoEstagioRequestDTO dto =
                new NovaBolsaVinculoEstagioRequestDTO();
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);
        dto.setDataInicio(LocalDate.of(2026, 10, 2));
        dto.setDataFimPrevista(LocalDate.of(2027, 3, 31));

        when(service.registrarNovaBolsaLocal(
                eq(VINCULO_ID),
                any(NovaBolsaVinculoEstagioRequestDTO.class)))
                .thenReturn(montarResponse());

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/nova-bolsa",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value(VINCULO_ID.toString()));
    }

    @Test
    void deveRetornar400AoProrrogarBolsaSemNovaData()
            throws Exception {

        ProrrogarBolsaVinculoEstagioRequestDTO dto =
                new ProrrogarBolsaVinculoEstagioRequestDTO();

        mockMvc.perform(put(
                        "/api/v1/vinculos-estagio/{vinculoId}/prorrogar-bolsa",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoRegistrarNovaBolsaSemPeriodo()
            throws Exception {

        NovaBolsaVinculoEstagioRequestDTO dto =
                new NovaBolsaVinculoEstagioRequestDTO();
        dto.setTipoBolsa(TipoBolsa.BOLSA_CAPES);

        mockMvc.perform(post(
                        "/api/v1/vinculos-estagio/{vinculoId}/nova-bolsa",
                        VINCULO_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

}
