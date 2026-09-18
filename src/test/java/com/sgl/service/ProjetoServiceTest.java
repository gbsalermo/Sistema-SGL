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

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link ProjetoService}.
 *
 * Segue o mesmo padrão de {@code LaboratorioServiceTest}/{@code UsuarioServiceTest}:
 * quase toda operação passa por {@code TenantContext.ativo()}/{@code pertence(...)}
 * antes de tocar o repositório, então cada teste "caminho feliz" precisa
 * definir o tenant coincidindo com a unidade do laboratório/projeto envolvido,
 * e o {@code @AfterEach} limpa o ThreadLocal para não vazar estado entre testes.
 */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @InjectMocks
    private ProjetoService projetoService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Projeto projeto;

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
        laboratorio.setNome("Laboratório de Química Orgânica");
        laboratorio.setAtivo(true);
        laboratorio.setUnidade(unidade);

        projeto = Projeto.builder()
                .id(20L)
                .publicId(PROJETO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .nome("Síntese de Novos Compostos")
                .descricao("Desenvolvimento de novos compostos orgânicos")
                .responsavel("Maria Oliveira")
                .ativo(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        // Evita que o tenant definido em um teste vaze para o próximo (ThreadLocal).
        TenantContext.limpar();
    }

    @Test
    void deveCriarProjetoComDadosValidos() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Síntese de Novos Compostos");
        dto.setDescricao("Desenvolvimento de novos compostos orgânicos");
        dto.setResponsavel("Maria Oliveira");
        dto.setAtivo(true);

        // O tenant ativo precisa ser a mesma unidade do laboratório, senão
        // validarTenantUnidade barra a operação.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(projeto);

        ProjetoResponseDTO resultado = projetoService.criar(dto);

        assertEquals(PROJETO_PUBLIC_ID, resultado.getId());
        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    void deveListarTodosOsProjetos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByLaboratorioUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(projeto));

        List<ProjetoResponseDTO> resultado = projetoService.listarTodos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveBuscarProjetoPorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        ProjetoResponseDTO resultado = projetoService.buscarPorId(PROJETO_PUBLIC_ID);

        assertEquals(PROJETO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoProjetoNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> projetoService.buscarPorId(idInexistente));

        assertEquals("Projeto não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveListarProjetosPorLaboratorio() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByLaboratorioId(laboratorio.getId())).thenReturn(List.of(projeto));

        List<ProjetoResponseDTO> resultado = projetoService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveLancarExcecaoQuandoLaboratorioNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> projetoService.listarPorLaboratorio(idInexistente));

        assertEquals("Laboratório não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveAtualizarProjeto() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Síntese de Novos Compostos - Atualizado");
        dto.setAtivo(true);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class))).thenReturn(projeto);

        projetoService.atualizar(PROJETO_PUBLIC_ID, dto);

        verify(projetoRepository).save(any(Projeto.class));
    }

    @Test
    void deveDeletarProjeto() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        projetoService.deletar(PROJETO_PUBLIC_ID);

        // deletar() é soft-delete: apenas marca ativo=false na entidade
        // gerenciada pelo JPA (dirty checking dentro da transação), sem
        // chamar projetoRepository.save() explicitamente — diferente de
        // LaboratorioService.deletar(), que salva de forma explícita.
        assertFalse(projeto.getAtivo());
    }

    @Test
    void deveListarProjetosAtivos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByLaboratorioUnidadePublicIdAndAtivoTrue(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(projeto));

        List<ProjetoResponseDTO> resultado = projetoService.listarAtivos();

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRejeitarOperacaoQuandoLaboratorioDeOutroTenant() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Novo Projeto");

        Unidade outraUnidade = new Unidade();
        outraUnidade.setId(99L);
        outraUnidade.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);

        Laboratorio laboratorioDeOutraUnidade = new Laboratorio();
        laboratorioDeOutraUnidade.setId(30L);
        laboratorioDeOutraUnidade.setPublicId(LABORATORIO_PUBLIC_ID);
        laboratorioDeOutraUnidade.setAtivo(true);
        laboratorioDeOutraUnidade.setUnidade(outraUnidade);

        // buscarLaboratorio() já filtra pela unidade do tenant ativo na
        // própria consulta (findByPublicIdAndUnidadePublicId), então em
        // produção esse cenário de inconsistência não deveria surgir — mas o
        // service tem uma segunda checagem explícita (validarTenantUnidade)
        // como defesa em profundidade. Aqui simulamos o mock devolvendo um
        // laboratório cuja unidade não bate com o tenant ativo, só para
        // exercitar essa segunda camada de proteção.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorioDeOutraUnidade));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> projetoService.criar(dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
    }
}
