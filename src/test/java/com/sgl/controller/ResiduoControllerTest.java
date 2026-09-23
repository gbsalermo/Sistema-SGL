package com.sgl.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import com.sgl.dto.request.AdministrarResiduoRequestDTO;
import com.sgl.dto.request.AnalisarResiduoRequestDTO;
import com.sgl.dto.request.ArmazenarResiduoRequestDTO;
import com.sgl.dto.request.ComponenteResiduoRequestDTO;
import com.sgl.dto.request.CriarResiduoRequestDTO;
import com.sgl.dto.request.DespacharResiduoRequestDTO;
import com.sgl.dto.request.ReceberResiduoRequestDTO;
import com.sgl.dto.response.HistoricoResiduoResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.dto.response.RotuloResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.Laboratorio;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.AcaoAdministrativaResiduo;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.service.ResiduoService;

/**
 * Teste de fatia web ({@code @WebMvcTest}) de {@link ResiduoController}.
 *
 * Segue o template corrigido para Spring Boot 4.1 (ver
 * controllers-context.md): {@code @MockitoBean} no lugar de
 * {@code @MockBean}, {@code ObjectMapper} próprio via
 * {@code @TestConfiguration} e {@code @Import(SecurityConfig.class)} para
 * reproduzir o "anyRequest().permitAll()" real.
 *
 * {@link ResiduoResponseDTO}, {@link RotuloResiduoResponseDTO} e
 * {@link HistoricoResiduoResponseDTO} só têm construtor que recebe a
 * entidade (campos {@code final}, sem setters) — por isso este teste monta
 * uma entidade {@link Residuo}/{@link HistoricoResiduo} mínima e válida
 * (mesmo padrão de fixture usado em ResiduoServiceTest) em vez de tentar
 * instanciar o DTO diretamente.
 *
 * Toda a árvore de validação de negócio da máquina de estados (INFORMADO →
 * EM_ANALISE → LIBERADO_PARA_ARMAZENAMENTO → ARMAZENADO_TEMPORARIAMENTE →
 * DESPACHADO) já está coberta em ResiduoServiceTest; aqui o foco é só
 * roteamento HTTP, serialização JSON e validação de request (@Valid).
 */
@WebMvcTest(ResiduoController.class)
@Import(SecurityConfig.class)
class ResiduoControllerTest {

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            // AnalisarResiduoRequestDTO tem campo dataPrevistaDespacho
            // (LocalDate) - registra o JavaTimeModule pelo mesmo motivo do
            // MovimentacaoEstoqueControllerTest.
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }

    private static final String BASE_URL = "/api/v1/residuos";

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID GERADOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID GESTOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID RESIDUO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID CLASSE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID HISTORICO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ResiduoService residuoService;

    // Residuo/HistoricoResiduo entities: usados só para construir os DTOs de
    // resposta (que têm construtor "por entidade"), nunca são persistidos de
    // verdade - o Service está totalmente mockado.
    private Unidade montarUnidade() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setNome("Embrapa Mandioca e Fruticultura");
        unidade.setSigla("CNPMF");
        return unidade;
    }

    private Laboratorio montarLaboratorio() {
        return Laboratorio.builder()
                .id(10L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .unidade(montarUnidade())
                .nome("Laboratório de Química")
                .ativo(true)
                .build();
    }

    private Usuario montarGerador() {
        Usuario gerador = new Usuario();
        gerador.setId(20L);
        gerador.setPublicId(GERADOR_PUBLIC_ID);
        gerador.setNome("Gerador Teste");
        gerador.setEmail("gerador@teste.com");
        gerador.setSenha("senha");
        gerador.setPerfil(Perfil.PESQUISADOR);
        gerador.setUnidade(montarUnidade());
        gerador.setAtivo(true);
        return gerador;
    }

    private Residuo montarResiduo() {
        Laboratorio laboratorio = montarLaboratorio();
        Residuo residuo = Residuo.builder()
                .id(100L)
                .publicId(RESIDUO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .unidadeIdSnapshot(UNIDADE_PUBLIC_ID)
                .unidadeNomeSnapshot("Embrapa Mandioca e Fruticultura")
                .unidadeSiglaSnapshot("CNPMF")
                .gerador(montarGerador())
                .descricao("Resíduo líquido do processo de extração de DNA")
                .processoOrigem("Extração de DNA vegetal")
                .estadoFisico(EstadoFisicoResiduo.LIQUIDO)
                .tratamentoRealizado(false)
                .recipiente("Bombona plástica de 5 L")
                .quantidade(new BigDecimal("2.5"))
                .unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.BAIXO)
                .riscosInformados(new LinkedHashSet<>(Set.of(TipoRisco.IRRITANTE)))
                .status(StatusResiduo.INFORMADO)
                .dataInformacao(LocalDateTime.now())
                .codigoRastreio("SGL-RES-2026-000100")
                .qrCodeConteudo("SGL-RESIDUO:" + RESIDUO_PUBLIC_ID)
                .build();
        return residuo;
    }

    private HistoricoResiduo montarHistorico() {
        return HistoricoResiduo.builder()
                .id(200L)
                .publicId(HISTORICO_PUBLIC_ID)
                .residuo(montarResiduo())
                .usuario(montarGerador())
                .status(StatusResiduo.INFORMADO)
                .acao("RESIDUO_INFORMADO")
                .observacao("Material entregue conforme protocolo.")
                .dataHora(LocalDateTime.now())
                .build();
    }

    private CriarResiduoRequestDTO montarCriarResiduoRequestDTO() {
        ComponenteResiduoRequestDTO componente = new ComponenteResiduoRequestDTO();
        componente.setNomeComponente("Acetona");
        componente.setPrincipal(true);
        componente.setConcentracaoOuQuantidade("aprox. 70%");

        CriarResiduoRequestDTO dto = new CriarResiduoRequestDTO();
        dto.setUsuarioGeradorId(GERADOR_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setDescricao("Resíduo líquido do processo de extração de DNA");
        dto.setProcessoOrigem("Extração de DNA vegetal");
        dto.setEstadoFisico(EstadoFisicoResiduo.LIQUIDO);
        dto.setTratamentoRealizado(false);
        dto.setRecipiente("Bombona plástica de 5 L");
        dto.setQuantidade(new BigDecimal("2.5"));
        dto.setUnidadeMedida(UnidadeMedida.L);
        dto.setNivelRiscoInformado(NivelRisco.BAIXO);
        dto.setRiscosInformados(Set.of(TipoRisco.IRRITANTE));
        dto.setObservacaoGerador("Material recebido conforme protocolo.");
        dto.setComponentes(List.of(componente));
        dto.setClassesInformadasIds(Set.of(CLASSE_PUBLIC_ID));
        dto.setMedidasSegurancaInformadas(Set.of(MedidaSeguranca.LUVAS));
        return dto;
    }

    private ReceberResiduoRequestDTO montarReceberRequestDTO() {
        ReceberResiduoRequestDTO dto = new ReceberResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setObservacao("Recebido para conferência.");
        return dto;
    }

    private AnalisarResiduoRequestDTO montarAnalisarRequestDTO() {
        AnalisarResiduoRequestDTO dto = new AnalisarResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setNivelRiscoConfirmado(NivelRisco.BAIXO);
        dto.setRiscosConfirmados(Set.of(TipoRisco.IRRITANTE));
        dto.setLocalArmazenamentoTemporario("Abrigo de resíduos - setor químico A");
        dto.setDestinoFinalPrevisto("Empresa licenciada para tratamento de resíduos químicos");
        dto.setClassesConfirmadasIds(Set.of(CLASSE_PUBLIC_ID));
        dto.setMedidasSegurancaConfirmadas(Set.of(MedidaSeguranca.LUVAS));
        return dto;
    }

    private ArmazenarResiduoRequestDTO montarArmazenarRequestDTO() {
        ArmazenarResiduoRequestDTO dto = new ArmazenarResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setLocalArmazenamentoTemporario("Abrigo de resíduos - setor químico A");
        return dto;
    }

    private DespacharResiduoRequestDTO montarDespacharRequestDTO() {
        DespacharResiduoRequestDTO dto = new DespacharResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setDestinoFinalConfirmado("Empresa licenciada para tratamento de resíduos químicos");
        return dto;
    }

    private AdministrarResiduoRequestDTO montarAdministrarRequestDTO() {
        return new AdministrarResiduoRequestDTO(
                GESTOR_PUBLIC_ID,
                AcaoAdministrativaResiduo.CANCELAR,
                "Registro realizado por engano"
        );
    }

    @Test
    void deveInformarResiduoERetornar201() throws Exception {
        when(residuoService.criar(any(CriarResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(montarResiduo()));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarCriarResiduoRequestDTO())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RESIDUO_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.status").value("INFORMADO"));
    }

    @Test
    void deveRetornar400AoInformarResiduoComCorpoInvalido() throws Exception {
        // Corpo vazio viola praticamente todos os @NotNull/@NotBlank/@NotEmpty
        // de CriarResiduoRequestDTO (usuarioGeradorId, laboratorioId,
        // descricao, processoOrigem, estadoFisico, tratamentoRealizado,
        // recipiente, quantidade, unidadeMedida, nivelRiscoInformado,
        // riscosInformados, componentes, classesInformadasIds,
        // medidasSegurancaInformadas).
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarTodosERetornar200() throws Exception {
        when(residuoService.listarTodos()).thenReturn(List.of(new ResiduoResponseDTO(montarResiduo())));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(RESIDUO_PUBLIC_ID.toString()));
    }

    @Test
    void deveBuscarPorIdERetornar200() throws Exception {
        when(residuoService.buscarPorId(RESIDUO_PUBLIC_ID)).thenReturn(new ResiduoResponseDTO(montarResiduo()));

        mockMvc.perform(get(BASE_URL + "/{id}", RESIDUO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Resíduo líquido do processo de extração de DNA"));
    }

    @Test
    void deveRetornar404QuandoResiduoNaoEncontrado() throws Exception {
        when(residuoService.buscarPorId(RESIDUO_PUBLIC_ID))
                .thenThrow(new ResourceNotFoundException("Resíduo", RESIDUO_PUBLIC_ID));

        mockMvc.perform(get(BASE_URL + "/{id}", RESIDUO_PUBLIC_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarPorStatusERetornar200() throws Exception {
        when(residuoService.listarPorStatus(StatusResiduo.INFORMADO))
                .thenReturn(List.of(new ResiduoResponseDTO(montarResiduo())));

        mockMvc.perform(get(BASE_URL + "/por-status").param("status", "INFORMADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INFORMADO"));
    }

    @Test
    void deveListarPorLaboratorioERetornar200() throws Exception {
        when(residuoService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(new ResiduoResponseDTO(montarResiduo())));

        mockMvc.perform(get(BASE_URL + "/por-laboratorio").param("laboratorioId", LABORATORIO_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].laboratorioId").value(LABORATORIO_PUBLIC_ID.toString()));
    }

    @Test
    void deveListarPorGeradorERetornar200() throws Exception {
        when(residuoService.listarPorGerador(GERADOR_PUBLIC_ID))
                .thenReturn(List.of(new ResiduoResponseDTO(montarResiduo())));

        mockMvc.perform(get(BASE_URL + "/por-gerador").param("usuarioGeradorId", GERADOR_PUBLIC_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioGeradorId").value(GERADOR_PUBLIC_ID.toString()));
    }

    @Test
    void deveReceberERetornar200() throws Exception {
        Residuo residuoEmAnalise = montarResiduo();
        residuoEmAnalise.setStatus(StatusResiduo.EM_ANALISE);
        when(residuoService.receber(eq(RESIDUO_PUBLIC_ID), any(ReceberResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(residuoEmAnalise));

        mockMvc.perform(put(BASE_URL + "/{id}/receber", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarReceberRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }

    @Test
    void deveAnalisarELiberarERetornar200() throws Exception {
        Residuo residuoLiberado = montarResiduo();
        residuoLiberado.setStatus(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO);
        when(residuoService.analisarELiberar(eq(RESIDUO_PUBLIC_ID), any(AnalisarResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(residuoLiberado));

        mockMvc.perform(put(BASE_URL + "/{id}/analisar-liberar", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarAnalisarRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIBERADO_PARA_ARMAZENAMENTO"));
    }

    @Test
    void deveConfirmarArmazenamentoERetornar200() throws Exception {
        Residuo residuoArmazenado = montarResiduo();
        residuoArmazenado.setStatus(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE);
        when(residuoService.confirmarArmazenamento(eq(RESIDUO_PUBLIC_ID), any(ArmazenarResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(residuoArmazenado));

        mockMvc.perform(put(BASE_URL + "/{id}/armazenar", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarArmazenarRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARMAZENADO_TEMPORARIAMENTE"));
    }

    @Test
    void deveDespacharERetornar200() throws Exception {
        Residuo residuoDespachado = montarResiduo();
        residuoDespachado.setStatus(StatusResiduo.DESPACHADO);
        when(residuoService.despachar(eq(RESIDUO_PUBLIC_ID), any(DespacharResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(residuoDespachado));

        mockMvc.perform(put(BASE_URL + "/{id}/despachar", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarDespacharRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESPACHADO"));
    }

    @Test
    void deveAdministrarCicloERetornar200() throws Exception {
        Residuo residuoCancelado = montarResiduo();
        residuoCancelado.setStatus(StatusResiduo.CANCELADO);

        when(residuoService.administrarCiclo(
                eq(RESIDUO_PUBLIC_ID),
                any(AdministrarResiduoRequestDTO.class)))
                .thenReturn(new ResiduoResponseDTO(residuoCancelado));

        mockMvc.perform(put(BASE_URL + "/{id}/administrar", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarAdministrarRequestDTO())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void deveRetornar400AoAdministrarSemJustificativa() throws Exception {
        AdministrarResiduoRequestDTO dto = montarAdministrarRequestDTO();
        dto.setJustificativa(" ");

        mockMvc.perform(put(BASE_URL + "/{id}/administrar", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornar400AoTransicionarStatusInvalido() throws Exception {
        // Delega a exceção que Residuo.receber() lançaria de verdade se o
        // resíduo não estivesse mais em INFORMADO (máquina de estados
        // documentada em ResiduoServiceTest) - aqui só confirma que o
        // Controller propaga BusinessRuleException como 400.
        when(residuoService.receber(eq(RESIDUO_PUBLIC_ID), any(ReceberResiduoRequestDTO.class)))
                .thenThrow(new BusinessRuleException(
                        "O resíduo só pode ser recebido para análise quando estiver em INFORMADO. Status atual: EM_ANALISE"));

        mockMvc.perform(put(BASE_URL + "/{id}/receber", RESIDUO_PUBLIC_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(montarReceberRequestDTO())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveGerarRotuloERetornar200() throws Exception {
        when(residuoService.gerarDadosRotulo(RESIDUO_PUBLIC_ID))
                .thenReturn(new RotuloResiduoResponseDTO(montarResiduo()));

        mockMvc.perform(get(BASE_URL + "/{id}/rotulo", RESIDUO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residuoId").value(RESIDUO_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.impressaoPermitida").value(false));
    }

    @Test
    void deveBuscarHistoricoERetornar200() throws Exception {
        when(residuoService.buscarHistorico(RESIDUO_PUBLIC_ID))
                .thenReturn(List.of(new HistoricoResiduoResponseDTO(montarHistorico())));

        mockMvc.perform(get(BASE_URL + "/{id}/historico", RESIDUO_PUBLIC_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(HISTORICO_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$[0].acao").value("RESIDUO_INFORMADO"));
    }
}
