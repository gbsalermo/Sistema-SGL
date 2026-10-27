package com.sgl.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.config.SecurityConfig;
import com.sgl.dto.response.RelatorioProjetosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.RelatorioProjetosService;

@WebMvcTest(RelatorioProjetosController.class)
@Import(SecurityConfig.class)
class RelatorioProjetosControllerTest {

    private static final String URL = "/api/v1/relatorios/projetos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioProjetosService service;

    private RelatorioProjetosResponseDTO relatorio() {
        return new RelatorioProjetosResponseDTO(
                LocalDateTime.of(2026, 10, 26, 15, 0),
                1, 2, 3, 4, 2,
                List.of(new RelatorioProjetosResponseDTO.ProjetoItem(
                        UUID.randomUUID(), "10.25.00.085.00.00",
                        "Projeto Teste", UUID.randomUUID(), "Laboratório",
                        "Responsável", LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31), StatusProjeto.ATIVO,
                        SituacaoExecucaoProjeto.NAO_INFORMADO,
                        true, false, null, 2, 3, 4, 2)));
    }

    @Test
    void deveRetornarRelatorioSemFiltros() throws Exception {
        when(service.gerar(isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull())).thenReturn(relatorio());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjetos").value(1))
                .andExpect(jsonPath("$.totalScis").value(2))
                .andExpect(jsonPath("$.totalEstagiariosDistintos").value(2))
                .andExpect(jsonPath("$.projetos[0].nome").value("Projeto Teste"));
    }

    @Test
    void deveRepassarTodosOsFiltrosAoService() throws Exception {
        UUID laboratorioId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);
        when(service.gerar(eq(laboratorioId), eq(StatusProjeto.ATIVO),
                eq(SituacaoExecucaoProjeto.NAO_INFORMADO),
                eq(true), eq(false), eq(inicio), eq(fim))).thenReturn(relatorio());

        mockMvc.perform(get(URL)
                        .param("laboratorioId", laboratorioId.toString())
                        .param("status", "ATIVO")
                        .param("situacaoExecucao", "NAO_INFORMADO")
                        .param("ativo", "true")
                        .param("recursoExterno", "false")
                        .param("dataInicio", inicio.toString())
                        .param("dataFim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtividades").value(3));

        verify(service).gerar(laboratorioId, StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.NAO_INFORMADO,
                true, false, inicio, fim);
    }

    @Test
    void deveRetornar400QuandoPeriodoInvalido() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 12, 31);
        LocalDate fim = LocalDate.of(2026, 1, 1);
        when(service.gerar(isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(inicio), eq(fim)))
                .thenThrow(new BusinessRuleException(
                        "A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get(URL)
                        .param("dataInicio", inicio.toString())
                        .param("dataFim", fim.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400ParaUuidDeLaboratorioInvalido() throws Exception {
        mockMvc.perform(get(URL).param("laboratorioId", "nao-e-uuid"))
                .andExpect(status().isBadRequest());
    }
}
