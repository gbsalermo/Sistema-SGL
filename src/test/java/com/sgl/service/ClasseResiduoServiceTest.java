package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.ClasseResiduoRequestDTO;
import com.sgl.dto.response.ClasseResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.Unidade;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link ClasseResiduoService}. Módulo novo (classificação de
 * resíduo), trazido pela reconciliação de histórico de 17/09, já nasceu com a
 * correção de fail-open de tenant aplicada (mesmo padrão {@code exigirTenantAtivo()}
 * usado em {@code EstoqueCentralService}/{@code UnidadeService}): sem o header
 * X-SGL-Unidade-Id ativo (via {@code TenantContext.definir}), qualquer leitura ou
 * escrita é barrada antes de tocar o repositório. Por isso quase todo teste aqui
 * precisa simular o tenant ativo no @BeforeEach/dentro do próprio teste, e o
 * @AfterEach limpa o ThreadLocal para não vazar estado entre testes.
 */
@ExtendWith(MockitoExtension.class)
class ClasseResiduoServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CLASSE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private ClasseResiduoRepository classeResiduoRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private ClasseResiduoService classeResiduoService;

    private Unidade unidade;
    private Unidade outraUnidade;
    private ClasseResiduo classe;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        outraUnidade = new Unidade();
        outraUnidade.setId(2L);
        outraUnidade.setPublicId(OUTRA_UNIDADE_PUBLIC_ID);
        outraUnidade.setSigla("CPATSA");
        outraUnidade.setNome("Embrapa Semiárido");

        classe = ClasseResiduo.builder()
                .id(10L)
                .publicId(CLASSE_PUBLIC_ID)
                .unidade(unidade)
                .codigo("CL-01")
                .descricao("Resíduo químico perigoso")
                .ativo(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveCriarClasseComDadosValidos() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-01");
        dto.setDescricao("Resíduo químico perigoso");
        dto.setAtivo(true);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCase(unidade.getId(), "CL-01"))
                .thenReturn(false);
        when(classeResiduoRepository.save(any(ClasseResiduo.class))).thenReturn(classe);

        ClasseResiduoResponseDTO resultado = classeResiduoService.criar(dto);

        assertEquals(CLASSE_PUBLIC_ID, resultado.getId());
        assertEquals(UNIDADE_PUBLIC_ID, resultado.getUnidadeId());
        assertEquals("CL-01", resultado.getCodigo());
        verify(classeResiduoRepository).save(any(ClasseResiduo.class));
    }

    @Test
    void deveRejeitarCriacaoQuandoUnidadeDeOutroTenant() {
        // validarTenantUnidade(): o tenant ativo é UNIDADE_PUBLIC_ID, mas o DTO
        // pede a criação da classe na unidade de outro tenant.
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(OUTRA_UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-01");
        dto.setDescricao("Resíduo químico perigoso");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(OUTRA_UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(outraUnidade));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.criar(dto));

        assertEquals("A operação não pode acessar dados de outra unidade.", ex.getMessage());
        verify(classeResiduoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarCriacaoComCodigoDuplicadoNaUnidade() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-01");
        dto.setDescricao("Resíduo químico perigoso");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCase(unidade.getId(), "CL-01"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.criar(dto));

        assertEquals("Já existe uma classe de resíduo com este código na unidade.", ex.getMessage());
        verify(classeResiduoRepository, never()).save(any());
    }

    @Test
    void deveNormalizarCodigoParaMaiusculas() {
        // normalizarCodigo() faz trim + toUpperCase antes de checar duplicidade
        // e de persistir - o DTO chega com espaços e minúsculas de propósito.
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("  cl-01  ");
        dto.setDescricao("  Resíduo químico perigoso  ");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(unidadeRepository.findByPublicId(UNIDADE_PUBLIC_ID)).thenReturn(Optional.of(unidade));
        when(classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCase(unidade.getId(), "CL-01"))
                .thenReturn(false);
        when(classeResiduoRepository.save(any(ClasseResiduo.class))).thenReturn(classe);

        classeResiduoService.criar(dto);

        ArgumentCaptor<ClasseResiduo> captor = ArgumentCaptor.forClass(ClasseResiduo.class);
        verify(classeResiduoRepository).save(captor.capture());

        assertEquals("CL-01", captor.getValue().getCodigo());
        assertEquals("Resíduo químico perigoso", captor.getValue().getDescricao());
    }

    @Test
    void deveListarTodasAsClassesDaUnidade() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByUnidadePublicIdOrderByCodigoAsc(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(classe));

        List<ClasseResiduoResponseDTO> resultado = classeResiduoService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(CLASSE_PUBLIC_ID, resultado.get(0).getId());
    }

    @Test
    void deveRejeitarListagemSemTenantAtivo() {
        // exigirTenantAtivo(): correção de segurança do módulo - sem o header
        // X-SGL-Unidade-Id, listarTodos() não pode devolver classes de todas
        // as unidades (fail-closed, não fail-open).
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.listarTodos());

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }

    @Test
    void deveListarApenasClassesAtivas() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByUnidadePublicIdAndAtivoTrueOrderByCodigoAsc(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(classe));

        List<ClasseResiduoResponseDTO> resultado = classeResiduoService.listarAtivos();

        assertEquals(1, resultado.size());
        assertEquals(CLASSE_PUBLIC_ID, resultado.get(0).getId());
    }

    @Test
    void deveRejeitarListagemDeAtivosSemTenantAtivo() {
        // O brief original (escrito antes da correção de fail-open) só cita o
        // cenário de rejeição para listarTodos(); listarAtivos() chama a mesma
        // exigirTenantAtivo() e também precisa ficar fail-closed sem o header.
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.listarAtivos());

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }

    @Test
    void deveBuscarClassePorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(CLASSE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(classe));

        ClasseResiduoResponseDTO resultado = classeResiduoService.buscarPorId(CLASSE_PUBLIC_ID);

        assertEquals(CLASSE_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoClasseNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> classeResiduoService.buscarPorId(idInexistente));

        assertEquals("Classe de resíduo não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveAtualizarClasseComDadosValidos() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-02");
        dto.setDescricao("Descrição atualizada");
        dto.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(CLASSE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(classe));
        when(classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCaseAndIdNot(
                unidade.getId(), "CL-02", classe.getId())).thenReturn(false);
        when(classeResiduoRepository.save(any(ClasseResiduo.class))).thenReturn(classe);

        ClasseResiduoResponseDTO resultado = classeResiduoService.atualizar(CLASSE_PUBLIC_ID, dto);

        assertEquals("CL-02", resultado.getCodigo());
        assertEquals("Descrição atualizada", resultado.getDescricao());
        assertFalse(resultado.getAtivo());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoTentaTransferirDeUnidade() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(OUTRA_UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-02");
        dto.setDescricao("Descrição atualizada");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(CLASSE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(classe));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.atualizar(CLASSE_PUBLIC_ID, dto));

        assertEquals("A classe de resíduo não pode ser transferida para outra unidade.", ex.getMessage());
        verify(classeResiduoRepository, never()).save(any());
    }

    @Test
    void deveRejeitarAtualizacaoComCodigoDuplicado() {
        ClasseResiduoRequestDTO dto = new ClasseResiduoRequestDTO();
        dto.setUnidadeId(UNIDADE_PUBLIC_ID);
        dto.setCodigo("CL-02");
        dto.setDescricao("Descrição atualizada");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(CLASSE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(classe));
        when(classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCaseAndIdNot(
                unidade.getId(), "CL-02", classe.getId())).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> classeResiduoService.atualizar(CLASSE_PUBLIC_ID, dto));

        assertEquals("Já existe uma classe de resíduo com este código na unidade.", ex.getMessage());
        verify(classeResiduoRepository, never()).save(any());
    }

    @Test
    void deveDeletarClasse() {
        // deletar() é soft-delete: apenas marca ativo=false, sem chamar
        // nenhum método de exclusão física no repositório.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(classeResiduoRepository.findByPublicIdAndUnidadePublicId(CLASSE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(classe));

        classeResiduoService.deletar(CLASSE_PUBLIC_ID);

        assertFalse(classe.getAtivo());
    }
}
