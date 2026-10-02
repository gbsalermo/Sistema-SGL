package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.ObservacaoVinculoEstagioRequestDTO;
import com.sgl.dto.request.TreinamentoSegurancaVinculoRequestDTO;
import com.sgl.dto.response.ObservacaoVinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Estagiario;
import com.sgl.model.ObservacaoVinculoEstagio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.EventoObservacaoVinculoEstagio;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoObservacaoVinculoEstagio;
import com.sgl.repository.ObservacaoVinculoEstagioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ObservacaoVinculoEstagioServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000001");
    private static final UUID VINCULO_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000002");
    private static final UUID OPERADOR_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000003");
    private static final UUID OBSERVACAO_ID =
            UUID.fromString("71000000-0000-0000-0000-000000000004");

    @Mock
    private ObservacaoVinculoEstagioRepository observacaoRepository;

    @Mock
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ObservacaoVinculoEstagioService service;

    private Unidade unidade;
    private VinculoEstagio vinculo;
    private Usuario operador;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        Estagiario estagiario = new Estagiario();
        estagiario.setId(2L);
        estagiario.setPublicId(UUID.randomUUID());
        estagiario.setUnidade(unidade);
        estagiario.setAtivo(true);

        vinculo = new VinculoEstagio();
        vinculo.setId(3L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setTreinamentoSegurancaConcluido(false);

        operador = new Usuario();
        operador.setId(4L);
        operador.setPublicId(OPERADOR_ID);
        operador.setNome("Gestor Teste");
        operador.setPerfil(Perfil.GESTOR);
        operador.setUnidade(unidade);
        operador.setAtivo(true);

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private void mockarVinculoEOperador() {
        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(usuarioRepository
                .findByPublicIdAndUnidadePublicId(
                        OPERADOR_ID, UNIDADE_ID))
                .thenReturn(Optional.of(operador));
    }

    private void mockarPersistenciaObservacao() {
        when(observacaoRepository.save(any(ObservacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> {
                    ObservacaoVinculoEstagio observacao =
                            invocation.getArgument(0);

                    if (observacao.getPublicId() == null) {
                        observacao.setPublicId(OBSERVACAO_ID);
                    }

                    if (observacao.getDataHora() == null) {
                        observacao.setDataHora(LocalDateTime.of(
                                2026, 10, 2, 18, 0));
                    }

                    return observacao;
                });
    }

    @Test
    void deveAdicionarObservacaoOperacionalAuditavel() {
        mockarVinculoEOperador();
        mockarPersistenciaObservacao();

        ObservacaoVinculoEstagioRequestDTO dto =
                new ObservacaoVinculoEstagioRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setTexto("  acompanhamento do gestor  ");

        ObservacaoVinculoEstagioResponseDTO resultado =
                service.adicionarOperacional(VINCULO_ID, dto);

        assertEquals(OBSERVACAO_ID, resultado.getId());
        assertEquals(TipoObservacaoVinculoEstagio.OPERACIONAL,
                resultado.getTipo());
        assertEquals(EventoObservacaoVinculoEstagio.OBSERVACAO,
                resultado.getEvento());
        assertEquals("acompanhamento do gestor",
                resultado.getTexto());
        assertEquals(OPERADOR_ID, resultado.getUsuarioId());
    }

    @Test
    void deveConcluirTreinamentoERegistrarEventoAuditavel() {
        mockarVinculoEOperador();
        mockarPersistenciaObservacao();

        when(vinculoEstagioRepository.save(vinculo))
                .thenReturn(vinculo);

        TreinamentoSegurancaVinculoRequestDTO dto =
                new TreinamentoSegurancaVinculoRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setConcluido(true);
        dto.setObservacao("  treinamento conferido  ");

        ObservacaoVinculoEstagioResponseDTO resultado =
                service.alterarTreinamento(VINCULO_ID, dto);

        assertTrue(vinculo.getTreinamentoSegurancaConcluido());
        assertEquals(TipoObservacaoVinculoEstagio.TREINAMENTO_SEGURANCA,
                resultado.getTipo());
        assertEquals(
                EventoObservacaoVinculoEstagio.TREINAMENTO_CONCLUIDO,
                resultado.getEvento());
        assertEquals("treinamento conferido", resultado.getTexto());

        verify(vinculoEstagioRepository).save(vinculo);
    }

    @Test
    void deveReverterTreinamentoERegistrarEventoAuditavel() {
        vinculo.setTreinamentoSegurancaConcluido(true);

        mockarVinculoEOperador();
        mockarPersistenciaObservacao();

        when(vinculoEstagioRepository.save(vinculo))
                .thenReturn(vinculo);

        TreinamentoSegurancaVinculoRequestDTO dto =
                new TreinamentoSegurancaVinculoRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setConcluido(false);
        dto.setObservacao("revisar comprovação");

        ObservacaoVinculoEstagioResponseDTO resultado =
                service.alterarTreinamento(VINCULO_ID, dto);

        assertFalse(vinculo.getTreinamentoSegurancaConcluido());
        assertEquals(
                EventoObservacaoVinculoEstagio.TREINAMENTO_REVERTIDO,
                resultado.getEvento());
        assertEquals("revisar comprovação", resultado.getTexto());
    }

    @Test
    void deveBloquearTreinamentoQuandoEstadoNaoMuda() {
        mockarVinculoEOperador();

        TreinamentoSegurancaVinculoRequestDTO dto =
                new TreinamentoSegurancaVinculoRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setConcluido(false);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.alterarTreinamento(VINCULO_ID, dto));

        assertEquals(
                "O treinamento de segurança já está pendente.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
        verify(observacaoRepository, never())
                .save(any(ObservacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearOperadorSemPerfilDeGestao() {
        operador.setPerfil(Perfil.PESQUISADOR);
        mockarVinculoEOperador();

        ObservacaoVinculoEstagioRequestDTO dto =
                new ObservacaoVinculoEstagioRequestDTO();
        dto.setUsuarioId(OPERADOR_ID);
        dto.setTexto("observação");

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.adicionarOperacional(VINCULO_ID, dto));

        assertEquals(
                "A operação exige perfil GESTOR ou ADMINISTRADOR.",
                ex.getMessage());
    }

    @Test
    void deveExigirTenantParaObservacoes() {
        TenantContext.limpar();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.listar(VINCULO_ID));

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());
    }
}
