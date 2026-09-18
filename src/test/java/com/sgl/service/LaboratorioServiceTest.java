package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.sgl.dto.request.LaboratorioRequestDTO;
import com.sgl.dto.response.LaboratorioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link LaboratorioService}.
 *
 * Importante: quase todo método deste service passa por
 * {@code TenantContext.pertence(...)} ou {@code TenantContext.ativo()} antes
 * de tocar o repositório (regra de isolamento entre unidades). Como
 * {@code TenantContext} guarda o tenant atual em ThreadLocal, cada teste que
 * simula um cenário "com tenant" precisa chamar {@code TenantContext.definir(...)}
 * e o {@code @AfterEach} sempre limpa esse estado para não vazar entre testes.
 */
@ExtendWith(MockitoExtension.class)
class LaboratorioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESPONSAVEL_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LaboratorioService laboratorioService;

    private Unidade unidade;
    private Laboratorio laboratorio;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        laboratorio = new Laboratorio();
        laboratorio.setId(10L);
        laboratorio.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorio.setNome("Laboratório de Solos");
        laboratorio.setDescricao("Análises de solo");
        laboratorio.setAtivo(true);
        laboratorio.setUnidade(unidade);
    }

    @AfterEach
    void tearDown() {
        // Evita que o tenant definido em um teste vaze para o próximo (ThreadLocal).
        TenantContext.limpar();
    }

    @Test
    void deveCriarLaboratorioComDadosValidos() {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Laboratório de Solos");
        dto.setDescricao("Análises de solo");
        dto.setAtivo(true);

        // O tenant ativo precisa ser a mesma unidade do DTO, senão validarTenantUnidade barra a operação.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(laboratorioRepository.save(any(Laboratorio.class))).thenReturn(laboratorio);

        LaboratorioResponseDTO resultado = laboratorioService.criar(dto);

        assertEquals(LABORATORIO_PUBLIC_ID, resultado.getId());
        verify(laboratorioRepository).save(any(Laboratorio.class));
    }

    @Test
    void deveRejeitarCriacaoQuandoResponsavelNaoEncontrado() {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Laboratório de Solos");
        dto.setResponsavelId(RESPONSAVEL_PUBLIC_ID);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(RESPONSAVEL_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> laboratorioService.criar(dto));

        assertEquals("Usuário responsável não encontrado com id: " + RESPONSAVEL_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoResponsavelDeOutraUnidade() {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Laboratório de Solos");
        dto.setResponsavelId(RESPONSAVEL_PUBLIC_ID);

        // Responsável existe e pertence ao tenant (por isso a busca o encontra),
        // mas está vinculado a uma unidade diferente da unidade do laboratório sendo criado.
        Unidade unidadeDoResponsavel = new Unidade();
        unidadeDoResponsavel.setId(99L);
        unidadeDoResponsavel.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);

        Usuario responsavel = new Usuario();
        responsavel.setPublicId(RESPONSAVEL_PUBLIC_ID);
        responsavel.setUnidade(unidadeDoResponsavel);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(RESPONSAVEL_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(responsavel));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> laboratorioService.criar(dto));

        assertEquals("O responsável deve pertencer à mesma unidade do laboratório.", ex.getMessage());
    }

    @Test
    void deveListarTodosOsLaboratorios() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(laboratorio));

        List<LaboratorioResponseDTO> resultado = laboratorioService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveListarLaboratoriosPorUnidade() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(laboratorio));

        List<LaboratorioResponseDTO> resultado = laboratorioService.listarPorUnidade(UNIDADE_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarLaboratorioPorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        LaboratorioResponseDTO resultado = laboratorioService.buscarPorId(LABORATORIO_PUBLIC_ID);

        assertEquals(LABORATORIO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoLaboratorioNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> laboratorioService.buscarPorId(idInexistente));

        assertEquals("Laboratório não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveAtualizarLaboratorio() {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Laboratório de Solos - Atualizado");
        dto.setDescricao("Nova descrição");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(laboratorioRepository.save(any(Laboratorio.class))).thenReturn(laboratorio);

        laboratorioService.atualizar(LABORATORIO_PUBLIC_ID, dto);

        verify(laboratorioRepository).save(any(Laboratorio.class));
    }

    @Test
    void deveDeletarLaboratorio() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(laboratorioRepository.save(any(Laboratorio.class))).thenReturn(laboratorio);

        laboratorioService.deletar(LABORATORIO_PUBLIC_ID);

        // deletar() é soft-delete: não remove a linha, apenas marca ativo=false e salva.
        assertFalse(laboratorio.getAtivo());
        verify(laboratorioRepository).save(laboratorio);
    }

    @Test
    void deveRejeitarOperacaoQuandoUnidadeDeOutroTenant() {
        LaboratorioRequestDTO dto = new LaboratorioRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setNome("Laboratório de Solos");

        // Tenant ativo é uma unidade diferente da unidade informada no DTO:
        // TenantContext.pertence() falha fechado e a operação deve ser barrada.
        TenantContext.definir(OUTRA_UNIDADE_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> laboratorioService.criar(dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
    }
}
