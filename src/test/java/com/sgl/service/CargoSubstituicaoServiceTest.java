package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.dto.response.CargoSubstituicaoDTO;
import com.sgl.dto.response.CargoSubstituicaoDTO.TipoCargo;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.security.SessaoDeTeste;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testa {@link CargoSubstituicaoService} com banco H2 de verdade: a regra está nas consultas
 * dos repositórios, então mockar os repositórios não provaria nada.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ TenantProvider.class, CargoSubstituicaoService.class })
class CargoSubstituicaoServiceTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private CargoSubstituicaoService service;

    @BeforeEach
    void entrarComoGestor() {
        SessaoDeTeste.entrar(UUID.randomUUID(), Perfil.GESTOR);
    }

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
        SessaoDeTeste.sair();
    }

    private Unidade unidade(String sigla) {
        return em.persistAndFlush(Unidade.builder().nome("Unidade " + sigla).sigla(sigla).build());
    }

    private Laboratorio laboratorio(Unidade unidade, String nome, Usuario responsavel) {
        return em.persistAndFlush(Laboratorio.builder().unidade(unidade).nome(nome)
                .responsavel(responsavel).ativo(true).build());
    }

    private Usuario usuario(String nome, Unidade unidade, Perfil perfil, boolean ativo, SituacaoUsuario acesso) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(nome.toLowerCase().replace(' ', '.') + "@exemplo.com");
        u.setPerfil(perfil);
        u.setUnidade(unidade);
        u.setAtivo(ativo);
        u.setSituacao(acesso);
        return em.persistAndFlush(u);
    }

    private Usuario usuarioOk(String nome, Unidade unidade, Perfil perfil) {
        return usuario(nome, unidade, perfil, true, SituacaoUsuario.ATIVO);
    }

    private VinculoEstagio vinculo(Unidade unidade, Usuario orientador, SituacaoEstagio situacao) {
        Estagiario e = new Estagiario();
        e.setNome("Estagiário " + orientador.getNome());
        e.setEmail("est." + orientador.getEmail());
        e.setPerfil(Perfil.ESTAGIARIO);
        e.setUnidade(unidade);
        e.setAtivo(true);
        e.setDataInicioEstagio(LocalDate.of(2026, 1, 1));
        e.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        e.setSituacaoEstagio(SituacaoEstagio.EM_ANDAMENTO);
        em.persistAndFlush(e);

        VinculoEstagio v = new VinculoEstagio();
        v.setEstagiario(e);
        v.setOrientador(orientador);
        v.setDataInicio(LocalDate.of(2026, 1, 1));
        v.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        v.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        v.setSituacao(situacao);
        return em.persistAndFlush(v);
    }

    private Residuo residuo(Laboratorio lab, Usuario gerador, Usuario gestor, StatusResiduo status) {
        return em.persistAndFlush(Residuo.builder().laboratorio(lab).gerador(gerador)
                .gestorRecebedorInicial(gestor).descricao("Solvente").processoOrigem("Processo")
                .recipiente("Frasco").quantidade(new BigDecimal("1.0")).unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.BAIXO).status(status).build());
    }

    @Test
    void cargosListaResponsavelDeLaboratorioQueMudouDeUnidade() {
        Unidade antiga = unidade("OLD");
        Unidade nova = unidade("NEW");
        // Quem mudou de lotação já está na unidade nova, aguardando aprovação.
        Usuario quemMudou = usuario("Quem Mudou", nova, Perfil.TECNICO, true, SituacaoUsuario.PENDENTE_VINCULACAO);
        Usuario quemFicou = usuarioOk("Quem Ficou", antiga, Perfil.GESTOR);
        laboratorio(antiga, "Lab sem dono", quemMudou);
        laboratorio(antiga, "Lab ok", quemFicou);
        TenantContext.definir(antiga.getPublicId());

        List<CargoSubstituicaoDTO> cargos = service.listar();

        assertEquals(1, cargos.size());
        assertEquals(TipoCargo.RESPONSAVEL_LABORATORIO, cargos.get(0).tipo());
        assertEquals(quemMudou.getPublicId(), cargos.get(0).ocupanteId());
        assertEquals("Quem Mudou", cargos.get(0).ocupanteNome());
    }

    @Test
    void cargosListaOrientadorInativoPrimeiro() {
        Unidade u = unidade("UNI");
        Usuario inativo = usuario("Orientador Inativo", u, Perfil.PESQUISADOR, false, SituacaoUsuario.ATIVO);
        Usuario respInativo = usuario("Resp Inativo", u, Perfil.GESTOR, false, SituacaoUsuario.ATIVO);
        Laboratorio lab = laboratorio(u, "Lab", respInativo);
        vinculo(u, inativo, SituacaoEstagio.EM_ANDAMENTO);
        residuo(lab, respInativo, respInativo, StatusResiduo.EM_ANALISE);
        TenantContext.definir(u.getPublicId());

        List<CargoSubstituicaoDTO> cargos = service.listar();

        assertEquals(3, cargos.size());
        assertEquals(TipoCargo.ORIENTADOR_ESTAGIO, cargos.get(0).tipo());
        assertEquals(TipoCargo.RESPONSAVEL_LABORATORIO, cargos.get(1).tipo());
        assertEquals(TipoCargo.GESTOR_RECEBEDOR_RESIDUO, cargos.get(2).tipo());
    }

    @Test
    void cargosIgnoraVinculoFinalizado() {
        Unidade u = unidade("UNI");
        Usuario inativo = usuario("Orientador Inativo", u, Perfil.PESQUISADOR, false, SituacaoUsuario.ATIVO);
        vinculo(u, inativo, SituacaoEstagio.FINALIZADO);
        TenantContext.definir(u.getPublicId());

        assertTrue(service.listar().isEmpty());
    }

    @Test
    void cargosIgnoraResiduoJaDespachadoOuCancelado() {
        Unidade u = unidade("UNI");
        Usuario inativo = usuario("Gestor Inativo", u, Perfil.GESTOR, false, SituacaoUsuario.ATIVO);
        Laboratorio lab = laboratorio(u, "Lab", null);
        residuo(lab, inativo, inativo, StatusResiduo.DESPACHADO);
        residuo(lab, inativo, inativo, StatusResiduo.CANCELADO);
        TenantContext.definir(u.getPublicId());

        assertTrue(service.listar().isEmpty());
    }

    @Test
    void cargosDeOutraUnidadeNaoAparecem() {
        Unidade a = unidade("AAA");
        Unidade b = unidade("BBB");
        Usuario inativo = usuario("Inativo", b, Perfil.GESTOR, false, SituacaoUsuario.ATIVO);
        laboratorio(b, "Lab de B", inativo);
        TenantContext.definir(a.getPublicId());

        assertTrue(service.listar().isEmpty());
    }

    @Test
    void semTenantNegaAcesso() {
        assertThrows(AccessDeniedException.class, () -> service.listar());
    }

    @Test
    void foraDaGestaoNegaAcesso() {
        Unidade u = unidade("UNI");
        TenantContext.definir(u.getPublicId());
        SessaoDeTeste.entrar(UUID.randomUUID(), Perfil.TECNICO);

        assertThrows(AccessDeniedException.class, () -> service.listar());
    }

    @Test
    void administradorGeralVeAUnidadeEmOperacao() {
        Unidade u = unidade("UNI");
        Usuario inativo = usuario("Resp Inativo", u, Perfil.GESTOR, false, SituacaoUsuario.ATIVO);
        laboratorio(u, "Lab", inativo);
        TenantContext.definir(u.getPublicId());
        SessaoDeTeste.entrar(UUID.randomUUID(), Perfil.ADMINISTRADOR_GERAL);

        assertEquals(1, service.listar().size());
    }
}
