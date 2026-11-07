package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.dto.request.EditarVinculoOperacionalRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.*;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.*;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class VinculoOperacionalUsuarioServiceTest {
    private static final UUID UNIDADE = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID LAB = UUID.randomUUID();
    private static final UUID PROJETO = UUID.randomUUID();

    @Mock VinculoOperacionalUsuarioRepository vinculos;
    @Mock UsuarioRepository usuarios;
    @Mock LaboratorioRepository laboratorios;
    @Mock ProjetoRepository projetos;
    @Mock SciRepository scis;
    @Mock AtividadeRepository atividades;
    VinculoOperacionalUsuarioService service;

    // Every mutation requires a gestão session; reads and lookups do not.
    @BeforeEach void sessaoDeGestor() {
        // The real validator over the mocked repository: the rules under test live in it.
        service = new VinculoOperacionalUsuarioService(vinculos, usuarios, laboratorios, projetos, scis, atividades,
            new LaboratoriosDaPessoa(laboratorios));
        AuthenticatedUser gestor = new AuthenticatedUser(UUID.randomUUID(), Perfil.GESTOR, SituacaoUsuario.ATIVO, UNIDADE);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(gestor, null, List.of()));
    }
    @AfterEach void limpar() { TenantContext.limpar(); SecurityContextHolder.clearContext(); }

    private Unidade unidade() {
        Unidade u = new Unidade();
        u.setId(1L); u.setPublicId(UNIDADE); return u;
    }
    private Usuario analista() {
        Usuario u = new Usuario();
        u.setId(2L); u.setPublicId(USUARIO); u.setUnidade(unidade());
        u.setPerfil(Perfil.ANALISTA); u.setAtivo(true);
        return u;
    }
    private Laboratorio laboratorio() {
        Laboratorio lab = new Laboratorio();
        lab.setId(3L); lab.setPublicId(LAB);
        lab.setUnidade(unidade()); lab.setAtivo(true);
        return lab;
    }

    private Laboratorio outroLaboratorio(String nome) {
        Laboratorio lab = laboratorio();
        lab.setId(30L + nome.hashCode() % 1000); lab.setPublicId(UUID.randomUUID()); lab.setNome(nome);
        when(laboratorios.findByPublicIdAndUnidadePublicId(lab.getPublicId(), UNIDADE)).thenReturn(Optional.of(lab));
        return lab;
    }

    @Test void definirLaboratoriosSubstituiALista() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio antigo = laboratorio();
        usuario.substituirLaboratorios(List.of(antigo));
        Laboratorio a = outroLaboratorio("A");
        Laboratorio b = outroLaboratorio("B");
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        service.definirLaboratorios(USUARIO, List.of(a.getPublicId(), b.getPublicId()));
        assertEquals(List.of(a.getPublicId(), b.getPublicId()),
            usuario.laboratoriosOrdenados().stream().map(Laboratorio::getPublicId).toList());
    }

    @Test void definirLaboratoriosValeParaEstagiario() {
        TenantContext.definir(UNIDADE);
        Usuario estagiario = analista();
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        Laboratorio a = outroLaboratorio("A");
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(estagiario));
        service.definirLaboratorios(USUARIO, List.of(a.getPublicId()));
        assertTrue(estagiario.atuaNoLaboratorio(a));
    }

    @Test void definirLaboratoriosVaziaParaOperacionalDa400() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        usuario.substituirLaboratorios(List.of(lab));
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
            () -> service.definirLaboratorios(USUARIO, List.of()));
        assertEquals("Informe ao menos um laboratório.", e.getMessage());
        assertTrue(usuario.atuaNoLaboratorio(lab));
    }

    @Test void criarVinculoGenericoDa400() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO); dto.setLaboratorioId(LAB); dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        assertEquals("Use a lista de laboratórios da pessoa para acesso sem prazo; o vínculo adicional é para acesso temporário ou restrito a projeto, SCI ou atividade.", e.getMessage());
        verify(vinculos, never()).save(any(VinculoOperacionalUsuario.class));
    }

    @Test void criarVinculoComPrazoNoMesmoLaboratorioDaListaEhPermitido() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        usuario.substituirLaboratorios(List.of(lab));
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO); dto.setLaboratorioId(LAB);
        dto.setDataInicio(LocalDate.now()); dto.setDataFim(LocalDate.now().plusDays(30));
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        when(vinculos.save(any(VinculoOperacionalUsuario.class))).thenAnswer(inv -> {
            VinculoOperacionalUsuario v = inv.getArgument(0);
            v.setPublicId(UUID.randomUUID());
            return v;
        });
        service.criar(dto);
        verify(vinculos).save(any(VinculoOperacionalUsuario.class));
    }

    // "Salvar laboratórios" num usuário pendente não o libera (só Aprovar faz isso); recusar evita a falsa impressão.
    @Test void laboratorioBaseDeUsuarioPendenteExigeAprovacao() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        usuario.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));

        com.sgl.exception.UsuarioRejectedException e = assertThrows(com.sgl.exception.UsuarioRejectedException.class,
            () -> service.definirLaboratorios(USUARIO, List.of(LAB)));
        assertEquals("USUARIO_PENDENTE", e.getCodigo());
        assertTrue(usuario.getLaboratorios().isEmpty());
    }

    @Test void deveBloquearAtribuicaoDeLaboratorioDeOutraUnidade() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.definirLaboratorios(USUARIO, List.of(LAB)));
        assertTrue(usuario.getLaboratorios().isEmpty());
    }

    @Test void devePermitirLaboratorioAdicionalVigente() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setUsuario(usuario); v.setLaboratorio(lab);
        v.setDataInicio(LocalDate.now().minusDays(5));
        when(vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(List.of(v));
        assertTrue(service.vinculadoLaboratorio(usuario, lab, LocalDate.now()));
    }

    @Test void deveIgnorarVinculoEncerrado() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setUsuario(usuario); v.setLaboratorio(lab);
        v.setDataInicio(LocalDate.now().minusDays(9));
        v.setDataFim(LocalDate.now().minusDays(1));
        when(vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(USUARIO, UNIDADE))
            .thenReturn(List.of(v));
        assertFalse(service.vinculadoLaboratorio(usuario, lab, LocalDate.now()));
    }

    @Test void vinculadoQuandoLaboratorioEstaNaLista() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio a = laboratorio();
        Laboratorio b = laboratorio();
        b.setId(30L); b.setPublicId(UUID.randomUUID());
        usuario.substituirLaboratorios(List.of(a, b));
        assertTrue(service.vinculadoLaboratorio(usuario, b, LocalDate.now()));
        // The list alone grants access: no vínculo lookup is needed.
        verifyNoInteractions(vinculos);
    }

    @Test void naoVinculadoForaDaListaSemVinculo() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio a = laboratorio();
        Laboratorio fora = laboratorio();
        fora.setId(31L); fora.setPublicId(UUID.randomUUID());
        usuario.substituirLaboratorios(List.of(a));
        when(vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(USUARIO, UNIDADE)).thenReturn(List.of());
        assertFalse(service.vinculadoLaboratorio(usuario, fora, LocalDate.now()));
    }

    @Test void deveRejeitarCriacaoDeVinculoParaEstagiario() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        usuario.setPerfil(Perfil.ESTAGIARIO);
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO); dto.setLaboratorioId(LAB);
        dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verifyNoInteractions(laboratorios, projetos, scis, atividades);
    }

    @Test void editarVinculoAlteraObservacaoSemMudarContexto() {
        TenantContext.definir(UNIDADE);
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setPublicId(UUID.randomUUID());
        v.setUsuario(analista());
        v.setLaboratorio(laboratorio());
        v.setDataInicio(LocalDate.now().minusDays(3));
        EditarVinculoOperacionalRequestDTO dto = new EditarVinculoOperacionalRequestDTO();
        dto.setObservacao("Vínculo conferido pela Gestão");
        dto.setDataFim(LocalDate.now().plusDays(15));
        when(vinculos.findByPublicIdAndUsuarioUnidadePublicId(v.getPublicId(), UNIDADE))
            .thenReturn(Optional.of(v));
        service.editar(v.getPublicId(), dto);
        assertEquals("Vínculo conferido pela Gestão", v.getObservacao());
        assertEquals(LocalDate.now().plusDays(15), v.getDataFim());
        assertEquals(LAB, v.getLaboratorio().getPublicId());
    }

    @Test void editarVinculoDeLaboratorioSemDataFimDa400() {
        TenantContext.definir(UNIDADE);
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setPublicId(UUID.randomUUID());
        v.setUsuario(analista());
        v.setLaboratorio(laboratorio());
        v.setDataInicio(LocalDate.now().minusDays(3));
        v.setDataFim(LocalDate.now().plusDays(10));
        when(vinculos.findByPublicIdAndUsuarioUnidadePublicId(v.getPublicId(), UNIDADE))
            .thenReturn(Optional.of(v));
        BusinessRuleException e = assertThrows(BusinessRuleException.class, () ->
            service.editar(v.getPublicId(), new EditarVinculoOperacionalRequestDTO()));
        assertEquals("Use a lista de laboratórios da pessoa para acesso sem prazo; o vínculo adicional é para acesso temporário ou restrito a projeto, SCI ou atividade.", e.getMessage());
        assertEquals(LocalDate.now().plusDays(10), v.getDataFim());
    }

    @Test void editarVinculoEncerradoEProibido() {
        TenantContext.definir(UNIDADE);
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setPublicId(UUID.randomUUID());
        v.setUsuario(analista());
        v.setEncerrado(true);
        when(vinculos.findByPublicIdAndUsuarioUnidadePublicId(v.getPublicId(), UNIDADE))
            .thenReturn(Optional.of(v));
        assertThrows(BusinessRuleException.class, () ->
            service.editar(v.getPublicId(), new EditarVinculoOperacionalRequestDTO()));
    }

    @Test void deveEncerrarVinculoImediatamenteSemApagarHistorico() {
        TenantContext.definir(UNIDADE);
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setPublicId(UUID.randomUUID());
        v.setUsuario(analista());
        v.setLaboratorio(laboratorio());
        v.setDataInicio(LocalDate.now().minusDays(1));
        when(vinculos.findByPublicIdAndUsuarioUnidadePublicId(v.getPublicId(), UNIDADE))
            .thenReturn(Optional.of(v));
        service.encerrar(v.getPublicId());
        assertTrue(v.getEncerrado());
        assertFalse(v.vigente(LocalDate.now()));
        assertEquals(LocalDate.now(), v.getDataFim());
        verify(vinculos, never()).delete(any(VinculoOperacionalUsuario.class));
    }


    @Test void podeVincularProjetoEspecificoDentroDoLaboratorios() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        usuario.substituirLaboratorios(List.of(lab));
        Projeto p = new Projeto();
        p.setId(5L);
        p.setPublicId(PROJETO);
        p.setLaboratorios(new LinkedHashSet<>(Set.of(lab)));
        p.setAtivo(true);
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO);
        dto.setLaboratorioId(LAB);
        dto.setProjetoId(PROJETO);
        dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        when(projetos.findByPublicId(PROJETO)).thenReturn(Optional.of(p));
        when(vinculos.save(any(VinculoOperacionalUsuario.class))).thenAnswer(inv -> {
            VinculoOperacionalUsuario v = inv.getArgument(0);
            v.setPublicId(UUID.randomUUID());
            return v;
        });
        assertEquals(PROJETO, service.criar(dto).getProjetoId());
        verify(vinculos).save(any(VinculoOperacionalUsuario.class));
    }

    @Test void deveBloquearProjetoDeOutroLaboratorio() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        Laboratorio outro = laboratorio();
        outro.setId(44L);
        Projeto p = new Projeto();
        p.setPublicId(PROJETO); p.setLaboratorios(new LinkedHashSet<>(Set.of(outro))); p.setAtivo(true);
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO); dto.setLaboratorioId(LAB); dto.setProjetoId(PROJETO);
        dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        when(projetos.findByPublicId(PROJETO)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verify(vinculos, never()).save(any(VinculoOperacionalUsuario.class));
    }

    // Vínculos definem onde a pessoa opera: mexer neles segue as regras da troca de perfil.
    @Test void gestorNaoAlteraLaboratoriosDeAdministrador() {
        TenantContext.definir(UNIDADE);
        Usuario admin = analista();
        admin.setPerfil(Perfil.ADMINISTRADOR);
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(admin));
        assertThrows(AccessDeniedException.class, () -> service.definirLaboratorios(USUARIO, List.of(LAB)));
        assertTrue(admin.getLaboratorios().isEmpty());
    }

    @Test void gestorNaoEncerraVinculoDeOutroGestor() {
        TenantContext.definir(UNIDADE);
        Usuario outroGestor = analista();
        outroGestor.setPerfil(Perfil.GESTOR);
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setPublicId(UUID.randomUUID());
        v.setUsuario(outroGestor);
        v.setLaboratorio(laboratorio());
        v.setDataInicio(LocalDate.now().minusDays(1));
        when(vinculos.findByPublicIdAndUsuarioUnidadePublicId(v.getPublicId(), UNIDADE)).thenReturn(Optional.of(v));
        assertThrows(AccessDeniedException.class, () -> service.encerrar(v.getPublicId()));
        assertNotEquals(Boolean.TRUE, v.getEncerrado());
    }

    @Test void ninguemCriaVinculoParaSiMesmo() {
        TenantContext.definir(UNIDADE);
        Usuario proprio = analista();
        proprio.setPublicId(CurrentUser.get().publicId());
        proprio.setPerfil(Perfil.ADMINISTRADOR);
        AuthenticatedUser admin = new AuthenticatedUser(proprio.getPublicId(), Perfil.ADMINISTRADOR, SituacaoUsuario.ATIVO, UNIDADE);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(proprio.getPublicId()); dto.setLaboratorioId(LAB); dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(proprio.getPublicId(), UNIDADE)).thenReturn(Optional.of(proprio));
        assertThrows(AccessDeniedException.class, () -> service.criar(dto));
        verify(vinculos, never()).save(any(VinculoOperacionalUsuario.class));
    }
}
