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
import com.sgl.dto.request.ClasseResiduoRequestDTO;
import com.sgl.dto.response.ClasseResiduoResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.Unidade;
import com.sgl.service.ClasseResiduoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link ClasseResiduoController}. Sobe só
 * a camada MVC (roteamento, serialização JSON, {@code @Valid}) - o
 * {@link ClasseResiduoService} é mockado, então nenhuma regra de negócio real (fail-open
 * de tenant, normalização de código, duplicidade etc., já cobertas em
 * ClasseResiduoServiceTest) é exercitada aqui. Último batch de Controller do plano
 * (Batch C7) - segue o mesmo template corrigido para Spring Boot 4.1 já usado em
 * UnidadeControllerTest (pacote novo de {@code @WebMvcTest}, {@code @MockitoBean} no
 * lugar de {@code @MockBean}, {@code @TestConfiguration} para o {@link ObjectMapper} e
 * {@code @Import(SecurityConfig.class)} para não cair na autoconfiguração padrão do
 * Spring Security, que devolveria 401/403 mesmo com o Service mockado certo).
 */
@WebMvcTest(ClasseResiduoController.class)
@Import(SecurityConfig.class)
class ClasseResiduoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final String BASE_URL = "/api/v1/classes-residuo";

    private static final UUID UNIDADE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID CLASSE_PUBLIC_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClasseResiduoService classeResiduoService;

    // ClasseResiduoResponseDTO só tem construtor que recebe a entidade
    // (converte a partir dela), então montamos a entidade completa aqui
    // - igual ao setUp() de ClasseResiduoServiceTest - em vez de tentar
    // instanciar o DTO de resposta diretamente.
    private ClasseResiduoResponseDTO montarResponseDTO() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        ClasseResiduo classe = ClasseResiduo.builder()
                .id(10L)
                .publicId(CLASSE_PUBLIC_ID)
                .unidade(unidade)
                .codigo("CL-01")
                .descricao("Resíduo químico perigoso")
                .ativo(true)
                .build();

        return new ClasseResiduoResponseDTO(classe);
    }

    private ClasseResiduoRequestDTO montarRequestDTO() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-01");
        dto.setDescricao("Resíduo químico perigoso");
        dto.setAtivo(true);
        return dto;
    }

    @Test
    void deveCriarClasseERetornar201() throws Exception {
        ClasseResiduoRequestDTO dto = montarRequestDTO();
        when(classeResiduoService.criar(any(ClasseResiduoRequestDTO.class)))
                .thenReturn(montarResponseDTO());

        // O controller usa ResponseEntity.status(HttpStatus.CREATED) explicitamente
        // (ClasseResiduoController.java linha 35-37) - por isso o teste espera 201.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CLASSE_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.codigo").value("CL-01"));
    }

    @Test
    void deveRetornar400AoCriarComCorpoInvalido() throws Exception {
        // ClasseResiduoRequestDTO exige unidadeId (@NotNull), codigo e descricao
        // (@NotBlank) - corpo vazio viola todos.
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTodasERetornar200() throws Exception {
        when(classeResiduoService.listarTodos())
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(CLASSE_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$[0].codigo").value("CL-01"));
    }

    @Test
    void deveListarAtivasERetornar200() throws Exception {
        when(classeResiduoService.listarAtivos())
                .thenReturn(List.of(montarResponseDTO()));

        mockMvc.perform(get(BASE_URL + "/ativos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ativo").value(true));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(classeResiduoService.buscarPorId(CLASSE_PUBLIC_ID))
                .thenReturn(montarResponseDTO());

        mockMvc.perform(get(BASE_URL + "/{id}", CLASSE_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Resíduo químico perigoso"));
    }

    @Test
    void deveRetornar404QuandoClasseNaoEncontrada() throws Exception {
        when(classeResiduoService.buscarPorId(CLASSE_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Classe de resíduo", CLASSE_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", CLASSE_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarClasseERetornar200() throws Exception {
        ClasseResiduoRequestDTO dto = montarRequestDTO();
        dto.setCodigo("CL-02");
        dto.setDescricao("Descrição atualizada");

        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        ClasseResiduo classeAtualizada = ClasseResiduo.builder()
                .id(10L)
                .publicId(CLASSE_PUBLIC_ID)
                .unidade(unidade)
                .codigo("CL-02")
                .descricao("Descrição atualizada")
                .ativo(true)
                .build();

        when(classeResiduoService.atualizar(eq(CLASSE_PUBLIC_ID), any(ClasseResiduoRequestDTO.class)))
                .thenReturn(new ClasseResiduoResponseDTO(classeAtualizada));

        mockMvc.perform(put(BASE_URL + "/{id}", CLASSE_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CL-02"))
                .andExpect(jsonPath("$.descricao").value("Descrição atualizada"));
    }

    @Test
    void deveDeletarClasseERetornar204() throws Exception {
        doNothing().when(classeResiduoService).deletar(CLASSE_PUBLIC_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", CLASSE_PUBLIC_ID))
                .andExpect(status().isNoContent());

        verify(classeResiduoService).deletar(CLASSE_PUBLIC_ID);
    }
}
