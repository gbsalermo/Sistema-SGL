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
import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.service.UnidadeService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link UnidadeController}. Sobe só a
 * camada MVC (roteamento, serialização JSON, {@code @Valid}) - o {@link UnidadeService}
 * é mockado, então nenhuma regra de negócio real (já coberta em UnidadeServiceTest) é
 * exercitada aqui. Primeiro teste de Controller do projeto: fixa o padrão para os
 * próximos batches (ver controllers-context.md).
 *
 * NOTA DE CONFIGURAÇÃO (projeto usa Spring Boot 4.1.0 modularizado): diferente de
 * versões anteriores do Spring Boot, o slice {@code @WebMvcTest} aqui NÃO traz um bean
 * de {@link ObjectMapper} pronto no contexto (JacksonAutoConfiguration virou um módulo
 * separado e não é importado por padrão pelo slice nesta versão). Por isso este teste
 * declara o bean explicitamente via {@code @TestConfiguration} abaixo - sem isso,
 * {@code @Autowired ObjectMapper} falha com NoSuchBeanDefinitionException. Replicar
 * este bloco {@code @TestConfiguration} em todos os próximos ControllerTests do plano.
 *
 * NOTA 2: sem {@code @Import(SecurityConfig.class)}, o slice sobe a autoconfiguração
 * padrão do Spring Security (usuário gerado, HTTP Basic) em vez do
 * {@link SecurityConfig} real da aplicação (que faz {@code anyRequest().permitAll()}
 * porque a autenticação ainda não foi implementada) - as requisições do teste voltavam
 * 401/403 mesmo com o Service mockado corretamente. Importar o SecurityConfig real
 * replica o comportamento de produção. Replicar também nos próximos ControllerTests.
 */
@WebMvcTest(UnidadeController.class)
@Import(SecurityConfig.class)
class UnidadeControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/unidades";

    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnidadeService unidadeService;

    private UnidadeResponseDTO montarResponseDTO() {
        return new UnidadeResponseDTO(UNIDADE_PUBLIC_ID, "Instituto de Química", "IQ");
    }

    @Test
    void deveListarTodasAsUnidadesERetornar200() throws Exception {
        when(unidadeService.listarTodos()).thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(UNIDADE_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$[0].sigla").value("IQ"));
    }

    @Test
    void deveBuscarUnidadePorIdERetornar200() throws Exception {
        when(unidadeService.buscarPorId(UNIDADE_PUBLIC_ID)).thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", UNIDADE_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Instituto de Química"));
    }

    @Test
    void deveRetornar404QuandoUnidadeNaoEncontrada() throws Exception {
        when(unidadeService.buscarPorId(UNIDADE_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Unidade", UNIDADE_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", UNIDADE_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveCriarUnidadeERetornar201() throws Exception {
        UnidadeRequestDTO dto = new UnidadeRequestDTO("Instituto de Química", "IQ");
        when(unidadeService.criar(any(UnidadeRequestDTO.class))).thenReturn(montarResponseDTO());

        // O controller usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (UnidadeController.java linha 54) - por isso o teste espera 201, não 200.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sigla").value("IQ"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // UnidadeRequestDTO exige nome e sigla via @NotBlank - corpo vazio viola ambos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveAtualizarUnidadeERetornar200() throws Exception {
        UnidadeRequestDTO dto = new UnidadeRequestDTO("Instituto de Química Atualizado", "IQ2");
        when(unidadeService.atualizar(eq(UNIDADE_PUBLIC_ID), any(UnidadeRequestDTO.class)))
                .thenReturn(new UnidadeResponseDTO(UNIDADE_PUBLIC_ID, "Instituto de Química Atualizado", "IQ2"));

        mockMvc.perform(put(BASE_URL + "/{id}", UNIDADE_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sigla").value("IQ2"));
    }

    @Test
    void deveDeletarUnidadeERetornar204() throws Exception {
        doNothing().when(unidadeService).deletar(UNIDADE_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", UNIDADE_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(unidadeService).deletar(UNIDADE_PUBLIC_ID);
    }
}
