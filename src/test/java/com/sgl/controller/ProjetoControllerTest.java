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
import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.ProjetoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link ProjetoController}. Segue o mesmo
 * padrão fixado em UnidadeControllerTest/LaboratorioControllerTest (ver notas de
 * configuração lá): bean de {@link ObjectMapper} via {@code @TestConfiguration} e
 * {@code @Import(SecurityConfig.class)} para reproduzir o "anyRequest().permitAll()"
 * real em vez do HTTP Basic gerado por padrão pelo slice.
 *
 * {@link ProjetoService} é mockado - toda a regra de tenant/negócio (já coberta em
 * ProjetoServiceTest) não é exercitada aqui, só roteamento HTTP, serialização e
 * validação de request.
 */
@WebMvcTest(ProjetoController.class)
@Import(SecurityConfig.class)
class ProjetoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/projetos";

    private static final UUID PROJETO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjetoService projetoService;

    private ProjetoRequestDTO montarRequestDTO() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Síntese de Novos Compostos");
        dto.setDescricao("Desenvolvimento de novos compostos orgânicos para catálise.");
        dto.setResponsavel("Maria Oliveira");
        dto.setAtivo(true);
        return dto;
    }

    // ProjetoResponseDTO só tem construtor a partir da entidade Projeto (campos
    // "final", sem setters) - por isso montamos um Projeto/Laboratorio reais via
    // builder (igual ao que ProjetoService faz) em vez de mockar o próprio DTO.
    private ProjetoResponseDTO montarResponseDTO() {
        Laboratorio laboratorio = Laboratorio.builder()
                .publicId(LABORATORIO_PUBLIC_ID)
                .nome("Laboratório de Química Orgânica")
                .ativo(true)
                .build();

        Projeto projeto = Projeto.builder()
                .publicId(PROJETO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .nome("Síntese de Novos Compostos")
                .descricao("Desenvolvimento de novos compostos orgânicos para catálise.")
                .responsavel("Maria Oliveira")
                .codigoSeg("95.95.95.001.01.00")
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .possuiRecursoExterno(true)
                .empresaRecursoExterno("Empresa Teste")
                .ativo(true)
                .build();

        return new ProjetoResponseDTO(projeto);
    }

    @Test
    void deveListarTodosOsProjetosERetornar200() throws Exception {
        when(projetoService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(PROJETO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarProjetoPorIdERetornar200() throws Exception {
        when(projetoService.buscarPorId(PROJETO_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", PROJETO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Síntese de Novos Compostos"))
                .andExpect(jsonPath("$.codigoSeg").value("95.95.95.001.01.00"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.situacaoExecucao").value("EM_ANDAMENTO_NO_PRAZO"))
                .andExpect(jsonPath("$.possuiRecursoExterno").value(true))
                .andExpect(jsonPath("$.empresaRecursoExterno").value("Empresa Teste"));
    }

    @Test
    void deveRetornar404QuandoProjetoNaoEncontrado() throws Exception {
        when(projetoService.buscarPorId(PROJETO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Projeto", PROJETO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", PROJETO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarProjetosPorLaboratorioERetornar200() throws Exception {
        when(projetoService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/por-laboratorio")
                        .param("laboratorioId", LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveCriarProjetoERetornar201() throws Exception {
        ProjetoRequestDTO dto = montarRequestDTO();
        when(projetoService.criar(any(ProjetoRequestDTO.class))).thenReturn(montarResponseDTO());

        // O controller usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (ProjetoController.java linha 45) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Síntese de Novos Compostos"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // ProjetoRequestDTO exige laboratorioId (@NotNull) e nome (@NotBlank) - corpo
        // vazio viola ambos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarProjetoERetornar200() throws Exception {
        ProjetoRequestDTO dto = montarRequestDTO();
        when(projetoService.atualizar(eq(PROJETO_PUBLIC_ID), any(ProjetoRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(put(BASE_URL + "/{id}", PROJETO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Síntese de Novos Compostos"));
    }

    @Test
    void deveDeletarProjetoERetornar204() throws Exception {
        doNothing().when(projetoService).deletar(PROJETO_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", PROJETO_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(projetoService).deletar(PROJETO_PUBLIC_ID);
    }

    @Test
    void deveListarProjetosAtivosERetornar200() throws Exception {
        when(projetoService.listarAtivos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }
}
