package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.EncerrarVinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeCulturasRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioAtividadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Cultura;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.VinculoEstagioAtividadeCultura;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.CulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeCulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class VinculoEstagioAtividadeServiceTest {

    private static final UUID UNIDADE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID VINCULO_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ATIVIDADE_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID PARTICIPACAO_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID CULTURA_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000005");

    @Mock
    private VinculoEstagioAtividadeRepository participacaoRepository;

    @Mock
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Mock
    private CulturaRepository culturaRepository;

    @Mock
    private VinculoEstagioAtividadeCulturaRepository participacaoCulturaRepository;

    @InjectMocks
    private VinculoEstagioAtividadeService service;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Projeto projeto;
    private Sci sci;
    private Atividade atividade;
    private Estagiario estagiario;
    private VinculoEstagio vinculo;
    private VinculoEstagioAtividade participacao;

    @BeforeEach
    void setUp() {
        unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_ID)
                .nome("Unidade Teste")
                .sigla("UT")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .publicId(UUID.randomUUID())
                .unidade(unidade)
                .nome("Laboratório Teste")
                .ativo(true)
                .build();

        projeto = Projeto.builder()
                .id(3L)
                .publicId(UUID.randomUUID())
                .laboratorio(laboratorio)
                .nome("Projeto Teste")
                .codigoSeg("PRJ-TESTE")
                .ativo(true)
                .build();

        sci = Sci.builder()
                .id(4L)
                .publicId(UUID.randomUUID())
                .projeto(projeto)
                .codigoSeg("SCI-TESTE")
                .nome("SCI Teste")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .build();

        atividade = Atividade.builder()
                .id(5L)
                .publicId(ATIVIDADE_ID)
                .sci(sci)
                .codigoSeg("ATV-TESTE")
                .nome("Atividade Teste")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .ativo(true)
                .build();

        estagiario = new Estagiario();
        estagiario.setId(6L);
        estagiario.setPublicId(UUID.randomUUID());
        estagiario.setUnidade(unidade);
        estagiario.setAtivo(true);

        vinculo = new VinculoEstagio();
        vinculo.setId(7L);
        vinculo.setPublicId(VINCULO_ID);
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 2, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 11, 30));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        participacao = new VinculoEstagioAtividade();
        participacao.setId(8L);
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));

        TenantContext.definir(UNIDADE_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    private void mockarVinculo() {
        when(vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(vinculo));
    }

    private void mockarAtividade() {
        when(atividadeRepository
                .findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        ATIVIDADE_ID, UNIDADE_ID))
                .thenReturn(Optional.of(atividade));
    }

    @Test
    void deveAdicionarParticipacaoValida() {
        mockarVinculo();
        mockarAtividade();

        when(participacaoRepository
                .existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
                        vinculo.getId(), atividade.getId()))
                .thenReturn(false);
        when(participacaoRepository.save(any(VinculoEstagioAtividade.class)))
                .thenAnswer(invocation -> {
                    VinculoEstagioAtividade salva = invocation.getArgument(0);
                    salva.setId(9L);
                    salva.setPublicId(PARTICIPACAO_ID);
                    return salva;
                });

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));
        dto.setObservacao("  participação inicial  ");

        VinculoEstagioAtividadeResponseDTO resultado =
                service.adicionar(VINCULO_ID, dto);

        assertEquals(PARTICIPACAO_ID, resultado.getId());
        assertEquals(ATIVIDADE_ID, resultado.getAtividadeId());
        assertEquals("participação inicial", resultado.getObservacao());
        assertTrue(resultado.getAtiva());
        verify(participacaoRepository)
                .save(any(VinculoEstagioAtividade.class));
    }

    @Test
    void deveBloquearParticipacaoDuplicadaAberta() {
        mockarVinculo();
        mockarAtividade();

        when(participacaoRepository
                .existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
                        vinculo.getId(), atividade.getId()))
                .thenReturn(true);

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.adicionar(VINCULO_ID, dto));

        assertEquals(
                "O vínculo de estágio já possui participação ativa nesta Atividade.",
                ex.getMessage());
    }

    @Test
    void deveBloquearAdicaoEmVinculoFinalizado() {
        vinculo.setSituacao(SituacaoEstagio.FINALIZADO);
        mockarVinculo();

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.adicionar(VINCULO_ID, dto));

        assertTrue(ex.getMessage().contains("vínculo de estágio finalizado"));
    }

    @Test
    void deveBloquearParticipacaoAntesDoInicioDoVinculo() {
        mockarVinculo();
        mockarAtividade();

        VinculoEstagioAtividadeRequestDTO dto =
                new VinculoEstagioAtividadeRequestDTO();
        dto.setAtividadeId(ATIVIDADE_ID);
        dto.setDataInicioParticipacao(LocalDate.of(2026, 1, 15));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.adicionar(VINCULO_ID, dto));

        assertEquals(
                "A participação não pode começar antes do vínculo de estágio.",
                ex.getMessage());
    }

    @Test
    void deveListarHistoricoDoVinculo() {
        mockarVinculo();

        when(participacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                        VINCULO_ID, UNIDADE_ID))
                .thenReturn(List.of(participacao));

        List<VinculoEstagioAtividadeResponseDTO> resultado =
                service.listarPorVinculo(VINCULO_ID);

        assertEquals(1, resultado.size());
        assertEquals(PARTICIPACAO_ID, resultado.get(0).getId());
    }

    @Test
    void deveEncerrarParticipacaoQuandoHaOutraAberta() {
        when(participacaoRepository
                .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(participacao));
        when(participacaoRepository
                .countByVinculoEstagioIdAndDataFimParticipacaoIsNull(
                        vinculo.getId()))
                .thenReturn(2L);
        when(participacaoRepository.save(participacao))
                .thenReturn(participacao);

        EncerrarVinculoEstagioAtividadeRequestDTO dto =
                new EncerrarVinculoEstagioAtividadeRequestDTO();
        dto.setDataFimParticipacao(LocalDate.of(2026, 6, 30));

        VinculoEstagioAtividadeResponseDTO resultado =
                service.encerrar(PARTICIPACAO_ID, dto);

        assertEquals(LocalDate.of(2026, 6, 30),
                resultado.getDataFimParticipacao());
        assertFalse(resultado.getAtiva());
    }

    @Test
    void deveBloquearEncerramentoDaUltimaParticipacaoAberta() {
        when(participacaoRepository
                .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(participacao));
        when(participacaoRepository
                .countByVinculoEstagioIdAndDataFimParticipacaoIsNull(
                        vinculo.getId()))
                .thenReturn(1L);

        EncerrarVinculoEstagioAtividadeRequestDTO dto =
                new EncerrarVinculoEstagioAtividadeRequestDTO();
        dto.setDataFimParticipacao(LocalDate.of(2026, 6, 30));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.encerrar(PARTICIPACAO_ID, dto));

        assertTrue(ex.getMessage().contains("última participação ativa"));
    }

    @Test
    void deveAtualizarCulturasDaParticipacao() {
        when(participacaoRepository
                .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(Optional.of(participacao));

        Cultura cultura = Cultura.builder()
                .id(20L)
                .publicId(CULTURA_ID)
                .unidade(unidade)
                .nome("Mandioca")
                .ativo(true)
                .build();

        when(culturaRepository
                .findByPublicIdInAndUnidadePublicId(
                        Set.of(CULTURA_ID), UNIDADE_ID))
                .thenReturn(List.of(cultura));

        VinculoEstagioAtividadeCultura associacao =
                new VinculoEstagioAtividadeCultura();
        associacao.setId(21L);
        associacao.setPublicId(UUID.randomUUID());
        associacao.setParticipacao(participacao);
        associacao.setCultura(cultura);

        when(participacaoCulturaRepository
                .findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
                        PARTICIPACAO_ID, UNIDADE_ID))
                .thenReturn(List.of())
                .thenReturn(List.of(associacao));

        VinculoEstagioAtividadeCulturasRequestDTO dto =
                new VinculoEstagioAtividadeCulturasRequestDTO();
        dto.setCulturaIds(Set.of(CULTURA_ID));

        VinculoEstagioAtividadeResponseDTO resultado =
                service.atualizarCulturas(PARTICIPACAO_ID, dto);

        assertEquals(1, resultado.getCulturas().size());
        assertEquals(CULTURA_ID, resultado.getCulturas().get(0).getId());
        verify(participacaoCulturaRepository)
                .saveAll(any());
    }

    @Test
    void deveExigirTenantAtivo() {
        TenantContext.limpar();

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.listarPorVinculo(VINCULO_ID));

        assertEquals(
                "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
                ex.getMessage());
    }
}
