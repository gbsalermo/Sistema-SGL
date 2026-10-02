package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.SincronizacaoVinculoEstagioRequestDTO;
import com.sgl.dto.response.HistoricoSincronizacaoVinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.HistoricoSincronizacaoVinculoEstagio;
import com.sgl.model.Unidade;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;
import com.sgl.repository.HistoricoSincronizacaoVinculoEstagioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class SincronizacaoVinculoEstagioServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID VINCULO_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID OUTRO_VINCULO_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000003");

    @Mock
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Mock
    private VinculoEstagioAtividadeRepository participacaoRepository;

    @Mock
    private HistoricoSincronizacaoVinculoEstagioRepository historicoRepository;

    @InjectMocks
    private SincronizacaoVinculoEstagioService service;

    private VinculoEstagio vinculo;
    private VinculoEstagioAtividade participacao;
    private Atividade atividade;

    @BeforeEach
    void setUp() {

        Unidade unidade = Unidade.builder()
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
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        atividade = Atividade.builder()
                .id(4L)
                .publicId(UUID.randomUUID())
                .nome("Atividade Teste")
                .codigoSeg("ATV-SYNC")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2027, 12, 31))
                .ativo(true)
                .build();

        participacao = new VinculoEstagioAtividade();
        participacao.setId(5L);
        participacao.setPublicId(UUID.randomUUID());
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private SincronizacaoVinculoEstagioRequestDTO montarDto(
            SituacaoEstagio situacao,
            LocalDate dataFimPrevista,
            LocalDate dataFimEfetiva,
            String referenciaEvento) {

        SincronizacaoVinculoEstagioRequestDTO dto =
                new SincronizacaoVinculoEstagioRequestDTO();

        dto.setOrigem(
                OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL);
        dto.setReferenciaEvento(referenciaEvento);
        dto.setReferenciaInstitucional("BOLSA-001");
        dto.setSituacao(situacao);
        dto.setDataFimPrevista(dataFimPrevista);
        dto.setDataFimEfetiva(dataFimEfetiva);

        return dto;
    }

    private void mockarFluxoNovoEvento() {

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(historicoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-001"))
                .thenReturn(Optional.empty());

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        lenient().when(
                vinculoEstagioRepository.save(any(VinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(historicoRepository
                .save(any(HistoricoSincronizacaoVinculoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveRegistrarProrrogacaoQuandoPrazoAumenta() {

        mockarFluxoNovoEvento();

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null,
                        "EVT-001");

        HistoricoSincronizacaoVinculoEstagioResponseDTO resultado =
                service.sincronizar(VINCULO_ID, dto);

        assertEquals(
                TipoEventoSincronizacaoVinculoEstagio.PRORROGACAO,
                resultado.getTipoEvento());
        assertEquals(
                SituacaoEstagio.PRORROGADO,
                vinculo.getSituacao());
        assertEquals(
                LocalDate.of(2027, 3, 31),
                vinculo.getDataFimPrevista());
        assertEquals(
                "BOLSA-001",
                vinculo.getReferenciaInstitucional());

        verify(vinculoEstagioRepository)
                .save(vinculo);
        verify(historicoRepository)
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
        verify(participacaoRepository, never())
                .saveAll(any());
    }

    @Test
    void deveFinalizarVinculoEEncerrarParticipacaoAberta() {

        mockarFluxoNovoEvento();

        LocalDate dataFimEfetiva =
                LocalDate.of(2026, 11, 30);

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.FINALIZADO,
                        LocalDate.of(2026, 12, 31),
                        dataFimEfetiva,
                        "EVT-001");

        HistoricoSincronizacaoVinculoEstagioResponseDTO resultado =
                service.sincronizar(VINCULO_ID, dto);

        assertEquals(
                TipoEventoSincronizacaoVinculoEstagio.FINALIZACAO,
                resultado.getTipoEvento());
        assertEquals(
                SituacaoEstagio.FINALIZADO,
                vinculo.getSituacao());
        assertEquals(
                dataFimEfetiva,
                vinculo.getDataFimEfetiva());
        assertEquals(
                dataFimEfetiva,
                participacao.getDataFimParticipacao());

        verify(participacaoRepository)
                .saveAll(any());
        verify(historicoRepository)
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveSerIdempotenteQuandoMesmoEventoChegaComMesmoEstado() {

        vinculo.setReferenciaInstitucional("BOLSA-001");
        vinculo.setSituacao(SituacaoEstagio.PRORROGADO);
        vinculo.setDataFimPrevista(LocalDate.of(2027, 3, 31));

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null,
                        "EVT-001");

        HistoricoSincronizacaoVinculoEstagio historico =
                montarHistoricoProcessado(
                        vinculo,
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null);

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(historicoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-001"))
                .thenReturn(Optional.of(historico));

        HistoricoSincronizacaoVinculoEstagioResponseDTO resultado =
                service.sincronizar(VINCULO_ID, dto);

        assertEquals(
                historico.getPublicId(),
                resultado.getId());

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
        verify(historicoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
        verify(participacaoRepository, never())
                .saveAll(any());
    }

    @Test
    void deveBloquearReplayDoMesmoEventoComDadosDiferentes() {

        vinculo.setReferenciaInstitucional("BOLSA-001");

        HistoricoSincronizacaoVinculoEstagio historico =
                montarHistoricoProcessado(
                        vinculo,
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null);

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(historicoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-001"))
                .thenReturn(Optional.of(historico));

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 4, 30),
                        null,
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertTrue(
                ex.getMessage().contains(
                        "dados institucionais diferentes"));

        verify(historicoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearReativacaoDeVinculoFinalizado() {

        vinculo.setSituacao(SituacaoEstagio.FINALIZADO);
        vinculo.setDataFimEfetiva(LocalDate.of(2026, 11, 30));

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(historicoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-001"))
                .thenReturn(Optional.empty());

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of());

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.EM_ANDAMENTO,
                        LocalDate.of(2026, 12, 31),
                        null,
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertTrue(
                ex.getMessage().contains(
                        "não pode ser reativado"));
    }

    @Test
    void deveBloquearFinalizacaoDepoisDoFimDaAtividadeAberta() {

        atividade.setDataFim(LocalDate.of(2026, 10, 31));

        mockarFluxoNovoEvento();

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.FINALIZADO,
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2026, 11, 30),
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertTrue(
                ex.getMessage().contains(
                        "Atividade já terminou anteriormente"));

        verify(vinculoEstagioRepository, never())
                .save(any(VinculoEstagio.class));
        verify(historicoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearReferenciaInstitucionalDiferente() {

        vinculo.setReferenciaInstitucional("BOLSA-ANTIGA");

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null,
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertTrue(
                ex.getMessage().contains(
                        "referência institucional"));

        verify(historicoRepository, never())
                .save(any(HistoricoSincronizacaoVinculoEstagio.class));
    }

    @Test
    void deveBloquearEventoReutilizadoEmOutroVinculo() {

        VinculoEstagio outroVinculo = new VinculoEstagio();
        outroVinculo.setPublicId(OUTRO_VINCULO_ID);

        HistoricoSincronizacaoVinculoEstagio historico =
                montarHistoricoProcessado(
                        outroVinculo,
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null);

        when(vinculoEstagioRepository
                .buscarPorPublicIdETenantComBloqueio(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));

        when(historicoRepository
                .findByOrigemAndReferenciaEvento(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL,
                        "EVT-001"))
                .thenReturn(Optional.of(historico));

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null,
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertTrue(
                ex.getMessage().contains(
                        "outro vínculo de estágio"));
    }

    @Test
    void deveExigirTenantAtivo() {

        TenantContext.limpar();

        SincronizacaoVinculoEstagioRequestDTO dto =
                montarDto(
                        SituacaoEstagio.PRORROGADO,
                        LocalDate.of(2027, 3, 31),
                        null,
                        "EVT-001");

        BusinessRuleException ex =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.sincronizar(VINCULO_ID, dto));

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());

        verify(vinculoEstagioRepository, never())
                .buscarPorPublicIdETenantComBloqueio(any(), any());
    }

    private HistoricoSincronizacaoVinculoEstagio montarHistoricoProcessado(
            VinculoEstagio vinculoHistorico,
            SituacaoEstagio situacaoNova,
            LocalDate dataFimPrevistaNova,
            LocalDate dataFimEfetivaNova) {

        return HistoricoSincronizacaoVinculoEstagio.builder()
                .publicId(UUID.randomUUID())
                .vinculoEstagio(vinculoHistorico)
                .tipoEvento(
                        TipoEventoSincronizacaoVinculoEstagio.ATUALIZACAO)
                .origem(
                        OrigemSincronizacaoVinculoEstagio.AMBIENTE_INSTITUCIONAL)
                .referenciaEvento("EVT-001")
                .situacaoAnterior(SituacaoEstagio.EM_ANDAMENTO)
                .situacaoNova(situacaoNova)
                .dataFimPrevistaAnterior(LocalDate.of(2026, 12, 31))
                .dataFimPrevistaNova(dataFimPrevistaNova)
                .dataFimEfetivaNova(dataFimEfetivaNova)
                .build();
    }
}
