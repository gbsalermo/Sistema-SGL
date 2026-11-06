package com.sgl.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sgl.config.SecurityConfig;
import com.sgl.service.VinculoOperacionalUsuarioService;

@WebMvcTest(VinculoOperacionalUsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class VinculoOperacionalUsuarioControllerTest {

    private static final UUID USUARIO = UUID.fromString("74000000-0000-0000-0000-000000000001");
    private static final UUID LAB_A = UUID.fromString("74000000-0000-0000-0000-000000000002");
    private static final UUID LAB_B = UUID.fromString("74000000-0000-0000-0000-000000000003");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VinculoOperacionalUsuarioService service;

    @Test
    void putLaboratoriosRecebeLista() throws Exception {
        mockMvc.perform(put("/api/v1/vinculos-operacionais/usuarios/{id}/laboratorios", USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioIds\":[\"%s\",\"%s\"]}".formatted(LAB_A, LAB_B)))
                .andExpect(status().isOk());

        verify(service).definirLaboratorios(USUARIO, List.of(LAB_A, LAB_B));
    }

    @Test
    void putLaboratoriosComIdNuloNaListaRetorna400() throws Exception {
        mockMvc.perform(put("/api/v1/vinculos-operacionais/usuarios/{id}/laboratorios", USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioIds\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotaAntigaLaboratorioBaseNaoExisteMais() throws Exception {
        mockMvc.perform(put("/api/v1/vinculos-operacionais/usuarios/{id}/laboratorio-base", USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratorioId\":\"%s\"}".formatted(LAB_A)))
                .andExpect(status().is4xxClientError());
    }
}
