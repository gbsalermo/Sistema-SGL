package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
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

    @Mock
    private SciRepository sciRepository;

    @Mock
    private AtividadeRepository atividadeRepository;

    @Spy
    private CodigoSegValidator codigoSegValidator = new CodigoSegValidator();

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
                .codigoSeg("96.96.96.001.01.00")
                .status(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO)
                .possuiRecursoExterno(true)
                .empresaRecursoExterno("Empresa Atual")
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
    void deveAplicarDefaultsAoCriarProjetoComContratoAntigo() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto legado");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projetoService.criar(dto);

        ArgumentCaptor<Projeto> captor = ArgumentCaptor.forClass(Projeto.class);
        verify(projetoRepository).save(captor.capture());

        Projeto salvo = captor.getValue();
        assertNull(salvo.getCodigoSeg());
        assertEquals(StatusProjeto.ATIVO, salvo.getStatus());
        assertEquals(SituacaoExecucaoProjeto.NAO_INFORMADO, salvo.getSituacaoExecucao());
        assertFalse(salvo.getPossuiRecursoExterno());
        assertNull(salvo.getEmpresaRecursoExterno());
        assertTrue(salvo.getAtivo());
    }

    @Test
    void deveCriarProjetoComNovosCampos() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto expandido");
        dto.setCodigoSeg(" 95.95.95.001.01.00 ");
        dto.setStatus(StatusProjeto.CONCLUIDO);
        dto.setSituacaoExecucao(SituacaoExecucaoProjeto.EXECUCAO_CANCELADA);
        dto.setPossuiRecursoExterno(true);
        dto.setEmpresaRecursoExterno(" Empresa Parceira ");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projetoService.criar(dto);

        ArgumentCaptor<Projeto> captor = ArgumentCaptor.forClass(Projeto.class);
        verify(projetoRepository).save(captor.capture());

        Projeto salvo = captor.getValue();
        assertEquals("95.95.95.001.01.00", salvo.getCodigoSeg());
        assertEquals(StatusProjeto.CONCLUIDO, salvo.getStatus());
        assertEquals(SituacaoExecucaoProjeto.EXECUCAO_CANCELADA, salvo.getSituacaoExecucao());
        assertTrue(salvo.getPossuiRecursoExterno());
        assertEquals("Empresa Parceira", salvo.getEmpresaRecursoExterno());
    }

    @Test
    void deveRejeitarRecursoExternoSemEmpresa() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto com recurso externo");
        dto.setPossuiRecursoExterno(true);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.criar(dto)
        );

        assertEquals(
                "A empresa é obrigatória quando o projeto possui recurso externo.",
                ex.getMessage()
        );
    }

    @Test
    void devePreservarNovosCamposAoAtualizarComContratoAntigo() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projetoService.atualizar(PROJETO_PUBLIC_ID, dto);

        assertEquals("96.96.96.001.01.00", projeto.getCodigoSeg());
        assertEquals(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE, projeto.getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO, projeto.getSituacaoExecucao());
        assertTrue(projeto.getPossuiRecursoExterno());
        assertEquals("Empresa Atual", projeto.getEmpresaRecursoExterno());
    }

    @Test
    void deveRemoverEmpresaAoDesativarRecursoExterno() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");
        dto.setPossuiRecursoExterno(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        projetoService.atualizar(PROJETO_PUBLIC_ID, dto);

        assertFalse(projeto.getPossuiRecursoExterno());
        assertNull(projeto.getEmpresaRecursoExterno());
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

    @Test
    void deveRejeitarAlteracaoDaDataInicioDoProjeto() {
        projeto.setDataInicio(LocalDate.of(2026, 1, 1));
        projeto.setDataFim(LocalDate.of(2026, 12, 31));

        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");
        dto.setDataInicio(LocalDate.of(2026, 1, 2));
        dto.setDataFim(LocalDate.of(2026, 12, 31));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.atualizar(PROJETO_PUBLIC_ID, dto)
        );

        assertEquals(
                "A data de início do projeto não pode ser alterada após a criação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarAmpliacaoDaDataFimDoProjetoNoPutComum() {
        projeto.setDataInicio(LocalDate.of(2026, 1, 1));
        projeto.setDataFim(LocalDate.of(2026, 12, 31));

        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");
        dto.setDataInicio(LocalDate.of(2026, 1, 1));
        dto.setDataFim(LocalDate.of(2027, 1, 31));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.atualizar(PROJETO_PUBLIC_ID, dto)
        );

        assertEquals(
                "A ampliação da data final deve ser realizada pelo fluxo de prorrogação.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarReducaoDoProjetoQueDeixaSciForaDoPeriodo() {
        projeto.setDataInicio(LocalDate.of(2026, 1, 1));
        projeto.setDataFim(LocalDate.of(2026, 12, 31));

        Sci sciFilho = Sci.builder()
                .publicId(UUID.randomUUID())
                .projeto(projeto)
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 8, 31))
                .build();

        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");
        dto.setDataInicio(LocalDate.of(2026, 1, 1));
        dto.setDataFim(LocalDate.of(2026, 6, 30));

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(sciFilho));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.atualizar(PROJETO_PUBLIC_ID, dto)
        );

        assertEquals(
                "A nova data de fim do projeto deixaria um SCI fora do período do projeto.",
                ex.getMessage()
        );
    }


    @Test
    void devePermitirPrimeiraDefinicaoDeCodigoSegEmProjetoLegado() {
        projeto.setCodigoSeg(null);

        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto legado corrigido");
        dto.setCodigoSeg("95.95.95.001.01.00");

        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        when(projetoRepository.save(any(Projeto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjetoResponseDTO resultado =
                projetoService.atualizar(PROJETO_PUBLIC_ID, dto);

        assertEquals("95.95.95.001.01.00", resultado.getCodigoSeg());
    }

    @Test
    void deveRejeitarAlteracaoDoCodigoSegDoProjetoNoPutComum() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto atualizado");
        dto.setCodigoSeg("95.95.95.001.01.00");

        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
                PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.atualizar(PROJETO_PUBLIC_ID, dto)
        );

        assertEquals(
                "O Código SEG do Projeto não pode ser alterado pelo fluxo comum de atualização. Use o fluxo administrativo de correção.",
                ex.getMessage()
        );
    }

    @Test
    void deveRejeitarCodigoSegGlobalmenteDuplicadoNaCriacaoDoProjeto() {
        ProjetoRequestDTO dto = new ProjetoRequestDTO();
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setNome("Projeto duplicado");
        dto.setCodigoSeg("95.95.95.001.01.00");

        TenantContext.definir(UNIDADE_PUBLIC_ID);

        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(
                LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));

        when(projetoRepository.existsByCodigoSeg(
                "95.95.95.001.01.00"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> projetoService.criar(dto)
        );

        assertEquals(
                "Já existe um Projeto com este Código SEG.",
                ex.getMessage()
        );
    }

}
