package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class ContextoOperacionalConsultaServiceTest {

    private static final UUID UNIDADE = UUID.fromString("11000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE = UUID.fromString("11000000-0000-0000-0000-000000000002");
    private static final UUID USUARIO_ID = UUID.fromString("11000000-0000-0000-0000-000000000003");
    private static final UUID PROJETO_ID = UUID.fromString("11000000-0000-0000-0000-000000000004");
    private static final UUID ATIVIDADE_ID = UUID.fromString("11000000-0000-0000-0000-000000000005");
    private static final UUID PARTICIPACAO_ID = UUID.fromString("11000000-0000-0000-0000-000000000006");

    @Mock
    private VinculoEstagioAtividadeRepository participacoes;
    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private AtividadeRepository atividades;
    @Mock
    private ProjetoRepository projetos;
    @InjectMocks
    private ContextoOperacionalConsultaService service;

    private Estagiario estagiario;
    private Projeto projeto;
    private Atividade atividade;
    private VinculoEstagioAtividade participacao;

    @BeforeEach
    void preparar() {
        TenantContext.definir(UNIDADE);
        LocalDate hoje = LocalDate.now();
        Unidade unidade = Unidade.builder().publicId(UNIDADE).nome("IB").build();
        Laboratorio laboratorio = Laboratorio.builder().publicId(UUID.randomUUID())
                .unidade(unidade).nome("Laboratorio A").ativo(true).build();
        projeto = Projeto.builder().publicId(PROJETO_ID).laboratorio(laboratorio)
                .nome("Projeto A").ativo(true).status(StatusProjeto.ATIVO)
                .dataInicio(hoje.minusDays(90)).dataFim(hoje.plusDays(90)).build();
        Sci sci = Sci.builder().publicId(UUID.randomUUID()).projeto(projeto)
                .nome("SCI A").status(StatusProjeto.ATIVO).ativo(true)
                .dataInicio(hoje.minusDays(80)).build();
        atividade = Atividade.builder().publicId(ATIVIDADE_ID).sci(sci)
                .nome("Atividade A").status(StatusProjeto.ATIVO).ativo(true)
                .dataInicio(hoje.minusDays(70)).build();

        estagiario = new Estagiario();
        estagiario.setPublicId(USUARIO_ID);
        estagiario.setNome("Estagiario Teste");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setAtivo(true);
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(null);

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setPublicId(UUID.randomUUID());
        vinculo.setEstagiario(estagiario);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setDataInicio(hoje.minusDays(60));
        vinculo.setDataFimPrevista(hoje.plusDays(60));

        participacao = new VinculoEstagioAtividade();
        participacao.setPublicId(PARTICIPACAO_ID);
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(hoje.minusDays(50));
    }

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void listaContextosElegiveisDoEstagiarioSemConsultarLaboratorioLegado() {
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO_ID, UNIDADE))
                .thenReturn(Optional.of(estagiario));
        when(participacoes.listarContextosDoUsuario(USUARIO_ID, UNIDADE))
                .thenReturn(List.of(participacao));

        var resultado = service.listarDoEstagiario(USUARIO_ID);

        assertEquals(1, resultado.size());
        var contexto = resultado.get(0);
        assertEquals("PARTICIPACAO_ESTAGIO", contexto.origem());
        assertEquals(USUARIO_ID, contexto.usuarioId());
        assertEquals(PARTICIPACAO_ID, contexto.participacaoId());
        assertEquals(ATIVIDADE_ID, contexto.atividadeId());
        assertEquals(PROJETO_ID, contexto.projetoId());
        assertEquals(UNIDADE, contexto.unidadeId());
        assertTrue(contexto.aberta());
        assertTrue(contexto.elegivel());
        assertNull(contexto.motivoInelegibilidade());
    }

    @Test
    void devolveMotivoParaParticipacaoEncerradaNoHistorico() {
        participacao.setDataFimParticipacao(LocalDate.now().minusDays(1));
        when(atividades.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(ATIVIDADE_ID, UNIDADE))
                .thenReturn(Optional.of(atividade));
        when(participacoes.listarParticipacoesDaAtividade(ATIVIDADE_ID, UNIDADE))
                .thenReturn(List.of(participacao));

        var contexto = service.listarDaAtividade(ATIVIDADE_ID).get(0);

        assertFalse(contexto.aberta());
        assertFalse(contexto.elegivel());
        assertEquals("PARTICIPACAO_ENCERRADA", contexto.motivoInelegibilidade());
    }

    @Test
    void consultaParticipantesPorProjetoEmUmaBuscaLote() {
        when(projetos.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE))
                .thenReturn(Optional.of(projeto));
        when(participacoes.listarParticipacoesDoProjeto(PROJETO_ID, UNIDADE))
                .thenReturn(List.of(participacao));

        var resultado = service.listarDoProjeto(PROJETO_ID);

        assertEquals(1, resultado.size());
        assertEquals("Atividade A", resultado.get(0).atividadeNome());
        verify(participacoes).listarParticipacoesDoProjeto(PROJETO_ID, UNIDADE);
    }

    @Test
    void permiteEstagiarioSemParticipacoesEDevolveListaVazia() {
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO_ID, UNIDADE))
                .thenReturn(Optional.of(estagiario));
        when(participacoes.listarContextosDoUsuario(USUARIO_ID, UNIDADE))
                .thenReturn(List.of());

        assertTrue(service.listarDoEstagiario(USUARIO_ID).isEmpty());
    }

    @Test
    void bloqueiaPerfilQueNaoEEstagiario() {
        Usuario naoEstagiario = new Usuario();
        naoEstagiario.setPerfil(Perfil.PESQUISADOR);
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO_ID, UNIDADE))
                .thenReturn(Optional.of(naoEstagiario));

        assertThrows(BusinessRuleException.class,
                () -> service.listarDoEstagiario(USUARIO_ID));
        verifyNoInteractions(participacoes);
    }

    @Test
    void naoPermiteBuscarUsuarioDeOutraUnidade() {
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO_ID, UNIDADE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarDoEstagiario(USUARIO_ID));
        verifyNoInteractions(participacoes);
    }

    @Test
    void naoPermiteBuscarProjetoDeOutraUnidade() {
        when(projetos.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, UNIDADE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarDoProjeto(PROJETO_ID));
        verifyNoInteractions(participacoes);
    }

    @Test
    void naoPermiteBuscarAtividadeDeOutraUnidade() {
        when(atividades.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(ATIVIDADE_ID, UNIDADE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarDaAtividade(ATIVIDADE_ID));
        verifyNoInteractions(participacoes);
    }

    @Test
    void falhaFechadoQuandoHeaderDeTenantAusente() {
        TenantContext.limpar();
        assertThrows(BusinessRuleException.class,
                () -> service.listarDoEstagiario(USUARIO_ID));
        assertThrows(BusinessRuleException.class,
                () -> service.listarDoProjeto(PROJETO_ID));
        assertThrows(BusinessRuleException.class,
                () -> service.listarDaAtividade(ATIVIDADE_ID));
        verifyNoInteractions(participacoes, usuarios, projetos, atividades);
    }

    @Test
    void tenantAlteradoNaoReaproveitaContextoDeUnidadeAnterior() {
        TenantContext.definir(OUTRA_UNIDADE);
        when(projetos.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_ID, OUTRA_UNIDADE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarDoProjeto(PROJETO_ID));
        verifyNoInteractions(participacoes);
    }
}
