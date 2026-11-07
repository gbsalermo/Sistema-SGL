package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Queries over the usuario_laboratorios association, against real H2
 * (same setup as ProjetoRepositoryTest).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class UsuarioLaboratoriosRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        return entityManager.persistAndFlush(Unidade.builder().nome("Unidade " + sigla).sigla(sigla).build());
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        return entityManager.persistAndFlush(Laboratorio.builder().unidade(unidade).nome(nome).ativo(true).build());
    }

    private Usuario criarUsuario(String nome, Perfil perfil, Unidade unidade, Laboratorio... laboratorios) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setPerfil(perfil);
        usuario.setUnidade(unidade);
        usuario.setAtivo(true);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.substituirLaboratorios(List.of(laboratorios));
        return entityManager.persistAndFlush(usuario);
    }

    @Test
    void findByLaboratoriosIdTrazPessoaComDoisLaboratoriosEmCadaUm() {
        Unidade unidade = criarUnidade("UL1");
        Laboratorio solos = criarLaboratorio(unidade, "Solos");
        Laboratorio virologia = criarLaboratorio(unidade, "Virologia");
        Laboratorio outro = criarLaboratorio(unidade, "Outro");
        Usuario pessoa = criarUsuario("Pessoa Dois Labs", Perfil.TECNICO, unidade, solos, virologia);
        entityManager.clear();

        List<Usuario> emSolos = usuarioRepository.findByLaboratoriosId(solos.getId());
        List<Usuario> emVirologia = usuarioRepository.findByLaboratoriosId(virologia.getId());

        assertEquals(List.of(pessoa.getId()), emSolos.stream().map(Usuario::getId).toList());
        assertEquals(List.of(pessoa.getId()), emVirologia.stream().map(Usuario::getId).toList());
        assertTrue(usuarioRepository.findByLaboratoriosId(outro.getId()).isEmpty());
    }

    @Test
    void findEstagiariosSemVinculoCarregaLaboratorios() {
        Unidade unidade = criarUnidade("UL2");
        Laboratorio solos = criarLaboratorio(unidade, "Solos");
        Laboratorio virologia = criarLaboratorio(unidade, "Virologia");
        Usuario estagiario = criarUsuario("Estagiária Pendente", Perfil.ESTAGIARIO, unidade, solos, virologia);
        entityManager.clear();

        List<Usuario> semVinculo = usuarioRepository.findEstagiariosSemVinculo(unidade.getPublicId());

        assertEquals(1, semVinculo.size(), "A pessoa com dois laboratórios aparece uma vez");
        Usuario encontrado = semVinculo.get(0);
        assertEquals(estagiario.getId(), encontrado.getId());
        // The DTO is built after the query; the collection must not depend on an open session.
        assertTrue(Hibernate.isInitialized(encontrado.getLaboratorios()));
        assertEquals(List.of("Solos", "Virologia"),
                encontrado.laboratoriosOrdenados().stream().map(Laboratorio::getNome).toList());
    }
}
