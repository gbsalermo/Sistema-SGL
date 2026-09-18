package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link RelatorioPessoasLaboratorioService}.
 *
 * O comentário no fonte explica a origem da validação de tenant: antes desta
 * correção de segurança, trocar o {@code laboratorioId} na URL bastava para
 * ler o relatório de pessoas de um laboratório de OUTRA unidade — por isso
 * {@code exigirTenantAtivo} (via {@code TenantContext.ativo()}) é a primeira
 * checagem do método, mesmo não estando no grep original de {@code throw}
 * usado para montar o brief deste batch.
 *
 * O responsável do laboratório pode não estar na lista de usuários
 * vinculados por {@code laboratorioId} (ele só é "vinculado" via o campo
 * {@code responsavel} de {@link Laboratorio}) — o setUp explora justamente
 * esse caso, incluindo-o na resposta esperada.
 */
@ExtendWith(MockitoExtension.class)
class RelatorioPessoasLaboratorioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EstagiarioRepository estagiarioRepository;

    @InjectMocks
    private RelatorioPessoasLaboratorioService relatorioPessoasLaboratorioService;

    private Laboratorio laboratorio;
    private Usuario gestorResponsavel;
    private Usuario tecnicoAtivo;
    private Estagiario estagiarioInativo;

    @BeforeEach
    void setUp() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        gestorResponsavel = new Usuario();
        gestorResponsavel.setId(20L);
        gestorResponsavel.setPublicId(UUID.randomUUID());
        gestorResponsavel.setNome("Gestor Responsável");
        gestorResponsavel.setEmail("gestor@embrapa.br");
        gestorResponsavel.setPerfil(Perfil.GESTOR);
        gestorResponsavel.setAtivo(true);

        laboratorio = Laboratorio.builder()
                .id(10L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .unidade(unidade)
                .nome("Laboratório de Química")
                .ativo(true)
                .responsavel(gestorResponsavel)
                .build();

        tecnicoAtivo = new Usuario();
        tecnicoAtivo.setId(21L);
        tecnicoAtivo.setPublicId(UUID.randomUUID());
        tecnicoAtivo.setNome("Técnico Ana");
        tecnicoAtivo.setEmail("ana@embrapa.br");
        tecnicoAtivo.setPerfil(Perfil.TECNICO);
        tecnicoAtivo.setAtivo(true);

        estagiarioInativo = new Estagiario();
        estagiarioInativo.setId(22L);
        estagiarioInativo.setPublicId(UUID.randomUUID());
        estagiarioInativo.setNome("Estagiário Bruno");
        estagiarioInativo.setEmail("bruno@embrapa.br");
        estagiarioInativo.setPerfil(Perfil.ESTAGIARIO);
        estagiarioInativo.setAtivo(false);
        estagiarioInativo.setLaboratorio(laboratorio);
        estagiarioInativo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        estagiarioInativo.setDataInicioEstagio(LocalDate.of(2025, 3, 1));
        estagiarioInativo.setDataFimEstagio(LocalDate.of(2025, 9, 1));
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveGerarRelatorioDePessoasPorLaboratorioComSucesso() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(usuarioRepository.findByLaboratorioId(laboratorio.getId()))
                .thenReturn(List.of(tecnicoAtivo, estagiarioInativo));
        when(estagiarioRepository.findByLaboratorioId(laboratorio.getId()))
                .thenReturn(List.of(estagiarioInativo));

        RelatorioPessoasLaboratorioResponseDTO resultado = relatorioPessoasLaboratorioService.gerar(
                LABORATORIO_PUBLIC_ID, null, null);

        assertEquals(LABORATORIO_PUBLIC_ID, resultado.getLaboratorioId());
        assertEquals(gestorResponsavel.getPublicId(), resultado.getResponsavelId());
        // O responsável não veio de findByLaboratorioId() (só é vínculo via
        // Laboratorio.responsavel) — mesmo assim precisa entrar na lista.
        assertEquals(3, resultado.getTotalPessoas());
        assertEquals(2, resultado.getAtivos());
        assertEquals(1, resultado.getInativos());

        assertEquals(1L, resultado.getPorPerfil().get(Perfil.GESTOR));
        assertEquals(1L, resultado.getPorPerfil().get(Perfil.TECNICO));
        assertEquals(1L, resultado.getPorPerfil().get(Perfil.ESTAGIARIO));

        // Ordenação: responsável do laboratório sempre primeiro.
        assertEquals("Gestor Responsável", resultado.getPessoas().get(0).getNome());
        assertTrue(resultado.getPessoas().get(0).getResponsavelLaboratorio());
        // Entre os demais (não-responsáveis), ordena por perfil (nome do
        // enum) e depois por nome: ESTAGIARIO vem antes de TECNICO.
        assertEquals("Estagiário Bruno", resultado.getPessoas().get(1).getNome());
        assertEquals(TipoBolsa.BOLSA_CNPQ, resultado.getPessoas().get(1).getTipoVinculoEstagio());
        assertEquals("Técnico Ana", resultado.getPessoas().get(2).getNome());
    }

    @Test
    void deveFiltrarPorPerfilEAtivo() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(usuarioRepository.findByLaboratorioId(laboratorio.getId()))
                .thenReturn(List.of(tecnicoAtivo, estagiarioInativo));
        when(estagiarioRepository.findByLaboratorioId(laboratorio.getId()))
                .thenReturn(List.of(estagiarioInativo));

        RelatorioPessoasLaboratorioResponseDTO resultado = relatorioPessoasLaboratorioService.gerar(
                LABORATORIO_PUBLIC_ID, Perfil.TECNICO, true);

        assertEquals(1, resultado.getTotalPessoas());
        assertEquals("Técnico Ana", resultado.getPessoas().get(0).getNome());
    }

    @Test
    void deveLancarExcecaoQuandoLaboratorioNaoEncontradoNaUnidadeDoTenant() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> relatorioPessoasLaboratorioService.gerar(idInexistente, null, null));

        assertEquals("Laboratório não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarQuandoTenantNaoDefinido() {
        // Correção de segurança citada no comentário do fonte: sem
        // TenantContext.definir(), a operação deve ser recusada — e não
        // tratada como "sem restrição de unidade".
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> relatorioPessoasLaboratorioService.gerar(LABORATORIO_PUBLIC_ID, null, null));

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }
}
