package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.response.RelatorioProjetosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class RelatorioProjetosServiceTest {

    @Mock ProjetoRepository projetoRepository;
    @Mock SciRepository sciRepository;
    @Mock AtividadeRepository atividadeRepository;
    @Mock VinculoEstagioAtividadeRepository participacaoRepository;
    @InjectMocks RelatorioProjetosService service;

    private final UUID unidadeId = UUID.randomUUID();

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Projeto projeto(String nome) {
        Laboratorio laboratorio = Laboratorio.builder()
                .publicId(UUID.randomUUID())
                .nome("Laboratorio " + nome)
                .build();
        return Projeto.builder()
                .publicId(UUID.randomUUID())
                .nome(nome)
                .laboratorio(laboratorio)
                .ativo(true)
                .build();
    }

    @Test
    void exigeTenantAntesDeConsultarRepositories() {
        assertThrows(BusinessRuleException.class,
                () -> service.gerar(null, null, null, null, null, null, null));
        verifyNoInteractions(projetoRepository, sciRepository, atividadeRepository, participacaoRepository);
    }

    @Test
    void rejeitaIntervaloInvertidoAntesDeConsultarBanco() {
        TenantContext.definir(unidadeId);
        assertThrows(BusinessRuleException.class,
                () -> service.gerar(null, null, null, null, null,
                        LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)));
        verifyNoInteractions(projetoRepository, sciRepository, atividadeRepository, participacaoRepository);
    }

    @Test
    void semProjetosRetornaZerosESemConsultasDeAgregacao() {
        TenantContext.definir(unidadeId);
        when(projetoRepository.buscarParaRelatorio(eq(unidadeId), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(List.of());

        RelatorioProjetosResponseDTO r = service.gerar(null, null, null, null, null, null, null);
        assertEquals(0L, r.totalProjetos());
        assertEquals(0L, r.totalScis());
        assertEquals(0L, r.totalAtividades());
        assertEquals(0L, r.totalParticipacoes());
        assertEquals(0L, r.totalEstagiariosDistintos());
        assertTrue(r.projetos().isEmpty());
        verifyNoInteractions(sciRepository, atividadeRepository, participacaoRepository);
    }

    @Test
    void repassaFiltrosEPreencheZerosQuandoNaoHaVinculos() {
        TenantContext.definir(unidadeId);
        Projeto p = projeto("A");
        UUID laboratorioId = p.getLaboratorio().getPublicId();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);
        when(projetoRepository.buscarParaRelatorio(unidadeId, laboratorioId, StatusProjeto.ATIVO,
                null, true, false, inicio, fim)).thenReturn(List.of(p));
        when(sciRepository.contarScisPorProjetos(eq(unidadeId), anyList())).thenReturn(List.of());
        when(atividadeRepository.contarAtividadesPorProjetos(eq(unidadeId), anyList())).thenReturn(List.of());
        when(participacaoRepository.contarParticipacoesPorProjetos(eq(unidadeId), anyList())).thenReturn(List.of());

        RelatorioProjetosResponseDTO r = service.gerar(laboratorioId, StatusProjeto.ATIVO,
                null, true, false, inicio, fim);
        assertEquals(1L, r.totalProjetos());
        assertEquals(0L, r.totalScis());
        assertEquals(0L, r.totalAtividades());
        assertEquals(0L, r.totalParticipacoes());
        assertEquals(0L, r.totalEstagiariosDistintos());
        assertEquals(0L, r.projetos().get(0).totalEstagiarios());
        verify(participacaoRepository).contarEstagiariosDistintosNosProjetos(unidadeId, List.of(p.getPublicId()));
    }

    @Test
    void somaContagensPorProjetoMasUsaDistintosGlobaisDoRepository() {
        TenantContext.definir(unidadeId);
        Projeto p1 = projeto("A");
        Projeto p2 = projeto("B");
        List<UUID> ids = List.of(p1.getPublicId(), p2.getPublicId());
        when(projetoRepository.buscarParaRelatorio(eq(unidadeId), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(p1, p2));

        SciRepository.TotalScisPorProjeto s1 = mock(SciRepository.TotalScisPorProjeto.class);
        when(s1.getProjetoId()).thenReturn(ids.get(0));
        when(s1.getTotalScis()).thenReturn(2L);
        SciRepository.TotalScisPorProjeto s2 = mock(SciRepository.TotalScisPorProjeto.class);
        when(s2.getProjetoId()).thenReturn(ids.get(1));
        when(s2.getTotalScis()).thenReturn(1L);
        when(sciRepository.contarScisPorProjetos(unidadeId, ids)).thenReturn(List.of(s1, s2));

        AtividadeRepository.TotalAtividadesPorProjeto a1 = mock(AtividadeRepository.TotalAtividadesPorProjeto.class);
        when(a1.getProjetoId()).thenReturn(ids.get(0));
        when(a1.getTotalAtividades()).thenReturn(3L);
        when(atividadeRepository.contarAtividadesPorProjetos(unidadeId, ids)).thenReturn(List.of(a1));

        VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto v1 =
                mock(VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto.class);
        when(v1.getProjetoId()).thenReturn(ids.get(0));
        when(v1.getTotalParticipacoes()).thenReturn(4L);
        when(v1.getTotalEstagiarios()).thenReturn(2L);
        VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto v2 =
                mock(VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto.class);
        when(v2.getProjetoId()).thenReturn(ids.get(1));
        when(v2.getTotalParticipacoes()).thenReturn(2L);
        when(v2.getTotalEstagiarios()).thenReturn(2L);
        when(participacaoRepository.contarParticipacoesPorProjetos(unidadeId, ids))
                .thenReturn(List.of(v1, v2));
        when(participacaoRepository.contarEstagiariosDistintosNosProjetos(unidadeId, ids))
                .thenReturn(3L);

        RelatorioProjetosResponseDTO r = service.gerar(null, null, null, null, null, null, null);
        assertEquals(2L, r.totalProjetos());
        assertEquals(3L, r.totalScis());
        assertEquals(3L, r.totalAtividades());
        assertEquals(6L, r.totalParticipacoes());
        assertEquals(3L, r.totalEstagiariosDistintos());
        assertEquals(2L, r.projetos().get(0).totalEstagiarios());
        assertEquals(2L, r.projetos().get(1).totalEstagiarios());
        assertEquals(0L, r.projetos().get(1).totalAtividades());
        verify(sciRepository).contarScisPorProjetos(unidadeId, ids);
        verify(atividadeRepository).contarAtividadesPorProjetos(unidadeId, ids);
        verify(participacaoRepository).contarParticipacoesPorProjetos(unidadeId, ids);
    }
}
