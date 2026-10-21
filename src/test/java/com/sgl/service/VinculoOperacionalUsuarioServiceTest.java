package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.*;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.*;
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
    @InjectMocks VinculoOperacionalUsuarioService service;

    @AfterEach void limpar() { TenantContext.limpar(); }

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

    @Test void deveSalvarLaboratorioBaseSemDuplicarCadastro() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        service.definirLaboratorioBase(USUARIO, LAB);
        assertEquals(LAB, usuario.getLaboratorio().getPublicId());
    }

    @Test void deveBloquearAtribuicaoDeLaboratorioDeOutraUnidade() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.definirLaboratorioBase(USUARIO, LAB));
        assertNull(usuario.getLaboratorio());
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

    @Test void deveBloquearProjetoDeOutroLaboratorio() {
        TenantContext.definir(UNIDADE);
        Usuario usuario = analista();
        Laboratorio lab = laboratorio();
        Laboratorio outro = laboratorio();
        outro.setId(44L);
        Projeto p = new Projeto();
        p.setPublicId(PROJETO); p.setLaboratorio(outro); p.setAtivo(true);
        VinculoOperacionalUsuarioRequestDTO dto = new VinculoOperacionalUsuarioRequestDTO();
        dto.setUsuarioId(USUARIO); dto.setLaboratorioId(LAB); dto.setProjetoId(PROJETO);
        dto.setDataInicio(LocalDate.now());
        when(usuarios.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE)).thenReturn(Optional.of(usuario));
        when(laboratorios.findByPublicIdAndUnidadePublicId(LAB, UNIDADE)).thenReturn(Optional.of(lab));
        when(projetos.findByPublicId(PROJETO)).thenReturn(Optional.of(p));
        assertThrows(BusinessRuleException.class, () -> service.criar(dto));
        verify(vinculos, never()).save(any(VinculoOperacionalUsuario.class));
    }
}
