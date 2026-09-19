package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Laboratorio;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.tenant.TenantContext;
import com.sgl.tenant.TenantProvider;

/**
 * Testes de integração do ResiduoRepository usando @DataJpaTest com H2 real
 * (sem mocks) — persiste entidades via TestEntityManager e valida todos os
 * métodos customizados do repositório.
 *
 * Segue o mesmo padrão dos demais Repositories desta fase:
 * - @ActiveProfiles("test") ativa application-test.properties (H2), já que o
 *   profile padrão da aplicação ("dev") usaria PostgreSQL real.
 * - @Import(TenantProvider.class) disponibiliza o bean "tenantProvider" usado
 *   pela expressão SpEL de findAllByOrderByDataInformacaoDesc() e
 *   findByStatusOrderByDataInformacaoDesc().
 *
 * Residuo é a entidade mais complexa do sistema (classificações via
 * ClasseResiduo/ResiduoClasse, medidas de segurança, componentes etc.), mas
 * todas essas coleções (classificacoes, componentes,
 * riscosInformados/Confirmados, medidasSegurancaInformadas/Confirmadas) têm
 * @Builder.Default com coleção vazia, então não é necessário populá-las para
 * persistir um Resíduo válido — essas regras de negócio já são cobertas em
 * ResiduoServiceTest. Aqui montamos apenas o mínimo necessário
 * (Unidade -> Laboratorio -> Usuario "gerador") para exercitar os métodos do
 * repositório.
 *
 * ATENÇÃO (não é bug a corrigir, apenas observação): "status" e
 * "dataInformacao" são preenchidos automaticamente pelo @PrePersist
 * generateDefaults() quando estão nulos — por isso os testes que não
 * dependem de um status/data específicos deixam esses campos de fora do
 * builder, e os que precisam de valores determinísticos (ex.: para testar
 * ordenação) os setam explicitamente (o @PrePersist só age quando o campo
 * está null).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class ResiduoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ResiduoRepository residuoRepository;

    @AfterEach
    void limparTenant() {
        TenantContext.limpar();
    }

    private Unidade criarUnidade(String sigla) {
        Unidade unidade = Unidade.builder()
                .nome("Unidade " + sigla)
                .sigla(sigla)
                .build();
        return entityManager.persistAndFlush(unidade);
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        Laboratorio laboratorio = Laboratorio.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(true)
                .build();
        return entityManager.persistAndFlush(laboratorio);
    }

    private Usuario criarUsuario(Unidade unidade, Laboratorio laboratorio, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + email);
        usuario.setEmail(email);
        usuario.setSenha("senha-123");
        usuario.setPerfil(Perfil.PESQUISADOR);
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(laboratorio);
        usuario.setAtivo(true);
        return entityManager.persistAndFlush(usuario);
    }

    private Residuo criarResiduo(Laboratorio laboratorio, Usuario gerador, StatusResiduo status,
            LocalDateTime dataInformacao) {
        Residuo residuo = Residuo.builder()
                .laboratorio(laboratorio)
                .gerador(gerador)
                .descricao("Descrição do resíduo")
                .processoOrigem("Processo de origem")
                .recipiente("Frasco de vidro 500 mL")
                .quantidade(new BigDecimal("1.500"))
                .unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.BAIXO)
                .status(status)
                .dataInformacao(dataInformacao)
                .build();
        return entityManager.persistAndFlush(residuo);
    }

    // --- findByPublicId -------------------------------------------------------

    @Test
    void deveEncontrarPorPublicId() {
        Unidade unidade = criarUnidade("R1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R1");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r1@exemplo.com");
        Residuo residuo = criarResiduo(laboratorio, gerador, null, null);

        Optional<Residuo> resultado = residuoRepository.findByPublicId(residuo.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarPorPublicIdInexistente() {
        Optional<Residuo> resultado = residuoRepository.findByPublicId(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByPublicIdAndLaboratorioUnidadePublicId -----------------------------

    @Test
    void deveEncontrarPorPublicIdEUnidadeDoLaboratorio() {
        Unidade unidade = criarUnidade("R2");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R2");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r2@exemplo.com");
        Residuo residuo = criarResiduo(laboratorio, gerador, null, null);

        Optional<Residuo> resultado = residuoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(residuo.getPublicId(), unidade.getPublicId());

        assertTrue(resultado.isPresent());
    }

    @Test
    void naoDeveEncontrarQuandoUnidadeInformadaNaoCasaComOLaboratorioDoResiduo() {
        Unidade unidade = criarUnidade("R3");
        Unidade outraUnidade = criarUnidade("R4");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R3");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r3@exemplo.com");
        Residuo residuo = criarResiduo(laboratorio, gerador, null, null);

        Optional<Residuo> resultado = residuoRepository
                .findByPublicIdAndLaboratorioUnidadePublicId(residuo.getPublicId(), outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    // --- findAllByOrderByDataInformacaoDesc (filtro de tenant via SpEL) ----------

    @Test
    void findAllByOrderByDataInformacaoDescDeveRetornarTodosQuandoNenhumTenantEstaDefinido() {
        Unidade unidadeA = criarUnidade("R5");
        Unidade unidadeB = criarUnidade("R6");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório R5");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório R6");
        Usuario geradorA = criarUsuario(unidadeA, laboratorioA, "gerador-r5@exemplo.com");
        Usuario geradorB = criarUsuario(unidadeB, laboratorioB, "gerador-r6@exemplo.com");
        criarResiduo(laboratorioA, geradorA, null, null);
        criarResiduo(laboratorioB, geradorB, null, null);

        List<Residuo> resultado = residuoRepository.findAllByOrderByDataInformacaoDesc();

        assertEquals(2, resultado.size());
    }

    @Test
    void findAllByOrderByDataInformacaoDescDeveFiltrarApenasResiduoDaUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("R7");
        Unidade unidadeB = criarUnidade("R8");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório R7");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório R8");
        Usuario geradorA = criarUsuario(unidadeA, laboratorioA, "gerador-r7@exemplo.com");
        Usuario geradorB = criarUsuario(unidadeB, laboratorioB, "gerador-r8@exemplo.com");
        Residuo residuoA = criarResiduo(laboratorioA, geradorA, null, null);
        criarResiduo(laboratorioB, geradorB, null, null);

        TenantContext.definir(unidadeA.getPublicId());

        List<Residuo> resultado = residuoRepository.findAllByOrderByDataInformacaoDesc();

        assertEquals(1, resultado.size());
        assertEquals(residuoA.getId(), resultado.get(0).getId());
    }

    @Test
    void findAllByOrderByDataInformacaoDescDeveOrdenarPorDataInformacaoDescendente() {
        Unidade unidade = criarUnidade("R9");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R9");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r9@exemplo.com");
        LocalDateTime maisAntiga = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime maisRecente = LocalDateTime.of(2026, 6, 1, 10, 0);
        // Insere primeiro a mais antiga para garantir que a ordenação não é por ordem de inserção/id.
        Residuo residuoAntigo = criarResiduo(laboratorio, gerador, StatusResiduo.INFORMADO, maisAntiga);
        Residuo residuoRecente = criarResiduo(laboratorio, gerador, StatusResiduo.INFORMADO, maisRecente);

        List<Residuo> resultado = residuoRepository.findAllByOrderByDataInformacaoDesc();

        assertEquals(2, resultado.size());
        assertEquals(residuoRecente.getId(), resultado.get(0).getId());
        assertEquals(residuoAntigo.getId(), resultado.get(1).getId());
    }

    // --- findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc (derivado) ---

    @Test
    void deveEncontrarPorUnidadePublicIdDoLaboratorio() {
        Unidade unidade = criarUnidade("R10");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R10");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r10@exemplo.com");
        criarResiduo(laboratorio, gerador, null, null);

        List<Residuo> resultado = residuoRepository
                .findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc(unidade.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoUnidadePublicIdNaoTemResiduo() {
        List<Residuo> resultado = residuoRepository
                .findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByStatusOrderByDataInformacaoDesc (filtro de tenant via SpEL) ------

    @Test
    void deveEncontrarPorStatus() {
        Unidade unidade = criarUnidade("R11");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R11");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r11@exemplo.com");
        criarResiduo(laboratorio, gerador, StatusResiduo.EM_ANALISE, null);

        List<Residuo> resultado = residuoRepository.findByStatusOrderByDataInformacaoDesc(StatusResiduo.EM_ANALISE);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoStatusNaoCasa() {
        Unidade unidade = criarUnidade("R12");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R12");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r12@exemplo.com");
        criarResiduo(laboratorio, gerador, StatusResiduo.EM_ANALISE, null);

        List<Residuo> resultado = residuoRepository.findByStatusOrderByDataInformacaoDesc(StatusResiduo.DESPACHADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveFiltrarPorStatusEUnidadeDoTenantAtual() {
        Unidade unidadeA = criarUnidade("R13");
        Unidade unidadeB = criarUnidade("R14");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório R13");
        Laboratorio laboratorioB = criarLaboratorio(unidadeB, "Laboratório R14");
        Usuario geradorA = criarUsuario(unidadeA, laboratorioA, "gerador-r13@exemplo.com");
        Usuario geradorB = criarUsuario(unidadeB, laboratorioB, "gerador-r14@exemplo.com");
        criarResiduo(laboratorioA, geradorA, StatusResiduo.EM_ANALISE, null);
        criarResiduo(laboratorioB, geradorB, StatusResiduo.EM_ANALISE, null);

        TenantContext.definir(unidadeA.getPublicId());

        List<Residuo> resultado = residuoRepository.findByStatusOrderByDataInformacaoDesc(StatusResiduo.EM_ANALISE);

        assertEquals(1, resultado.size());
    }

    // --- findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc -----

    @Test
    void deveEncontrarPorUnidadePublicIdDoLaboratorioEStatus() {
        Unidade unidade = criarUnidade("R15");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R15");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r15@exemplo.com");
        criarResiduo(laboratorio, gerador, StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, null);

        List<Residuo> resultado = residuoRepository.findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc(
                unidade.getPublicId(), StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoStatusNaoCasaComUnidadeDoLaboratorio() {
        Unidade unidade = criarUnidade("R16");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R16");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r16@exemplo.com");
        criarResiduo(laboratorio, gerador, StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, null);

        List<Residuo> resultado = residuoRepository.findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc(
                unidade.getPublicId(), StatusResiduo.DESPACHADO);

        assertTrue(resultado.isEmpty());
    }

    // --- findByLaboratorioPublicIdOrderByDataInformacaoDesc (derivado) -----------

    @Test
    void deveEncontrarPorLaboratorioPublicId() {
        Unidade unidade = criarUnidade("R17");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R17");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r17@exemplo.com");
        criarResiduo(laboratorio, gerador, null, null);

        List<Residuo> resultado = residuoRepository
                .findByLaboratorioPublicIdOrderByDataInformacaoDesc(laboratorio.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoLaboratorioPublicIdNaoTemResiduo() {
        List<Residuo> resultado = residuoRepository
                .findByLaboratorioPublicIdOrderByDataInformacaoDesc(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }

    // --- findByGeradorPublicIdOrderByDataInformacaoDesc (derivado) ---------------

    @Test
    void deveEncontrarPorGeradorPublicId() {
        Unidade unidade = criarUnidade("R18");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório R18");
        Usuario gerador = criarUsuario(unidade, laboratorio, "gerador-r18@exemplo.com");
        criarResiduo(laboratorio, gerador, null, null);

        List<Residuo> resultado = residuoRepository.findByGeradorPublicIdOrderByDataInformacaoDesc(gerador.getPublicId());

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRetornarListaVaziaQuandoGeradorPublicIdNaoTemResiduo() {
        List<Residuo> resultado = residuoRepository.findByGeradorPublicIdOrderByDataInformacaoDesc(UUID.randomUUID());

        assertTrue(resultado.isEmpty());
    }
}
