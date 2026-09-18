package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.AnalisarResiduoRequestDTO;
import com.sgl.dto.request.ArmazenarResiduoRequestDTO;
import com.sgl.dto.request.ComponenteResiduoRequestDTO;
import com.sgl.dto.request.CriarResiduoRequestDTO;
import com.sgl.dto.request.DespacharResiduoRequestDTO;
import com.sgl.dto.request.ReceberResiduoRequestDTO;
import com.sgl.dto.response.HistoricoResiduoResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.dto.response.RotuloResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.Laboratorio;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.HistoricoResiduoRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link ResiduoService}.
 *
 * É o service mais complexo do sistema: uma máquina de estados
 * (INFORMADO → EM_ANALISE → LIBERADO_PARA_ARMAZENAMENTO →
 * ARMAZENADO_TEMPORARIAMENTE → DESPACHADO, controlada por
 * {@code requireStatus()} em {@link Residuo}) somada a um bom número de
 * validações de negócio em {@code criar()} (gerador/laboratório/projeto,
 * tratamento, classificação e segurança).
 *
 * Nota sobre o padrão adotado aqui: como o teste é puramente unitário (sem
 * JPA de verdade), os callbacks {@code @PrePersist} de {@link Residuo}
 * (que geram {@code publicId} e {@code status} default) nunca disparam.
 * Para simular o comportamento real do banco — que atribui o {@code id}
 * (auto incremento) e o {@code publicId} assim que a entidade é
 * persistida —, o mock de {@code residuoRepository.save(...)} devolve o
 * mesmo objeto recebido, preenchendo esses dois campos quando ainda
 * estiverem nulos. Sem isso, {@code gerarCodigoRastreio()} formataria o
 * código com "id=null".
 */
@ExtendWith(MockitoExtension.class)
class ResiduoServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LABORATORIO_OUTRO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID GERADOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID GESTOR_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID GESTOR_OUTRO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID GESTOR_PERFIL_INVALIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID CLASSE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESIDUO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Mock
    private ResiduoRepository residuoRepository;

    @Mock
    private HistoricoResiduoRepository historicoResiduoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ClasseResiduoRepository classeResiduoRepository;

    @InjectMocks
    private ResiduoService residuoService;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Laboratorio laboratorioOutro;
    private Usuario gerador;
    private Usuario gestor;
    private Usuario gestorOutro;
    private Usuario gestorPerfilInvalido;
    private Projeto projeto;
    private Produto produto;
    private ClasseResiduo classe;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setNome("Embrapa Mandioca e Fruticultura");
        unidade.setSigla("CNPMF");

        laboratorio = Laboratorio.builder()
                .id(10L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .unidade(unidade)
                .nome("Laboratório de Química")
                .ativo(true)
                .build();

        laboratorioOutro = Laboratorio.builder()
                .id(11L)
                .publicId(LABORATORIO_OUTRO_PUBLIC_ID)
                .unidade(unidade)
                .nome("Laboratório de Biologia")
                .ativo(true)
                .build();

        gerador = new Usuario();
        gerador.setId(20L);
        gerador.setPublicId(GERADOR_PUBLIC_ID);
        gerador.setNome("Gerador Teste");
        gerador.setEmail("gerador@teste.com");
        gerador.setSenha("senha");
        gerador.setPerfil(Perfil.PESQUISADOR);
        gerador.setUnidade(unidade);
        gerador.setLaboratorio(laboratorio);
        gerador.setAtivo(true);

        gestor = new Usuario();
        gestor.setId(21L);
        gestor.setPublicId(GESTOR_PUBLIC_ID);
        gestor.setNome("Gestor Um");
        gestor.setEmail("gestor@teste.com");
        gestor.setSenha("senha");
        gestor.setPerfil(Perfil.GESTOR);
        gestor.setUnidade(unidade);
        gestor.setAtivo(true);

        gestorOutro = new Usuario();
        gestorOutro.setId(22L);
        gestorOutro.setPublicId(GESTOR_OUTRO_PUBLIC_ID);
        gestorOutro.setNome("Gestor Dois");
        gestorOutro.setEmail("gestor2@teste.com");
        gestorOutro.setSenha("senha");
        gestorOutro.setPerfil(Perfil.GESTOR);
        gestorOutro.setUnidade(unidade);
        gestorOutro.setAtivo(true);

        gestorPerfilInvalido = new Usuario();
        gestorPerfilInvalido.setId(23L);
        gestorPerfilInvalido.setPublicId(GESTOR_PERFIL_INVALIDO_PUBLIC_ID);
        gestorPerfilInvalido.setNome("Técnico Sem Permissão");
        gestorPerfilInvalido.setEmail("tecnico@teste.com");
        gestorPerfilInvalido.setSenha("senha");
        gestorPerfilInvalido.setPerfil(Perfil.TECNICO);
        gestorPerfilInvalido.setUnidade(unidade);
        gestorPerfilInvalido.setAtivo(true);

        projeto = Projeto.builder()
                .id(30L)
                .publicId(PROJETO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .nome("Projeto de Extração de DNA")
                .ativo(true)
                .build();

        produto = Produto.builder()
                .id(40L)
                .publicId(PRODUTO_PUBLIC_ID)
                .nome("Acetona")
                .unidadeMedida(UnidadeMedida.ML)
                .ativo(true)
                .build();

        classe = ClasseResiduo.builder()
                .id(50L)
                .publicId(CLASSE_PUBLIC_ID)
                .unidade(unidade)
                .codigo("A")
                .descricao("Solventes sem halogênios")
                .ativo(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    void deveCriarResiduoComDadosValidos() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();
        stubSalvarComIdGerado();

        CriarResiduoRequestDTO dto = dtoValidoCriar();

        ResiduoResponseDTO resultado = residuoService.criar(dto);

        assertEquals(StatusResiduo.INFORMADO, resultado.getStatus());
        assertEquals(LABORATORIO_PUBLIC_ID, resultado.getLaboratorioId());
        assertEquals(GERADOR_PUBLIC_ID, resultado.getUsuarioGeradorId());
        assertNull(resultado.getProjetoId());
        assertEquals("Resíduo líquido do processo de extração de DNA", resultado.getDescricao());
        assertEquals(1, resultado.getComponentes().size());
        assertEquals(1, resultado.getClassesInformadas().size());
        assertTrue(resultado.getMedidasSegurancaInformadas().contains(MedidaSeguranca.LUVAS));

        verify(historicoResiduoRepository, times(1)).save(any(HistoricoResiduo.class));
    }

    @Test
    void deveGravarSnapshotDeUnidadeAoCriarResiduo() {
        // Achado #6 (correção de segurança): o rótulo não pode mudar de
        // unidade sozinho se o laboratório for movido depois. Este teste
        // confirma que os três campos de snapshot são preenchidos com os
        // dados da unidade do laboratório NO MOMENTO da criação.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();
        stubSalvarComIdGerado();

        residuoService.criar(dtoValidoCriar());

        ArgumentCaptor<Residuo> captor = ArgumentCaptor.forClass(Residuo.class);
        verify(residuoRepository, times(2)).save(captor.capture());

        Residuo primeiroSalvamento = captor.getAllValues().get(0);
        assertEquals(UNIDADE_PUBLIC_ID, primeiroSalvamento.getUnidadeIdSnapshot());
        assertEquals("Embrapa Mandioca e Fruticultura", primeiroSalvamento.getUnidadeNomeSnapshot());
        assertEquals("CNPMF", primeiroSalvamento.getUnidadeSiglaSnapshot());
    }

    @Test
    void deveRejeitarCriacaoQuandoUsuarioGeradorNaoEncontrado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("Usuário não encontrado com id: " + GERADOR_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoUsuarioGeradorInativo() {
        gerador.setAtivo(false);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gerador));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("O usuário está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoLaboratorioNaoEncontrado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gerador));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("Laboratório não encontrado com id: " + LABORATORIO_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoLaboratorioInativo() {
        laboratorio.setAtivo(false);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("O laboratório informado está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoGeradorNaoPertenceAoLaboratorio() {
        gerador.setLaboratorio(laboratorioOutro);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("O usuário gerador não pertence ao laboratório informado.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoProjetoNaoEncontrado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setProjetoId(PROJETO_PUBLIC_ID);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.criar(dto));

        assertEquals("Projeto não encontrado com id: " + PROJETO_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoProjetoInativo() {
        projeto.setAtivo(false);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setProjetoId(PROJETO_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("O projeto informado está inativo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoProjetoNaoPertenceAoLaboratorio() {
        projeto.setLaboratorio(laboratorioOutro);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(PROJETO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(projeto));

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setProjetoId(PROJETO_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("O projeto informado não pertence ao laboratório gerador do resíduo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoTratamentoRealizadoNaoInformado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setTratamentoRealizado(null);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("informe se o resíduo recebeu tratamento", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoDescricaoTratamentoAusenteComTratamentoRealizado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setTratamentoRealizado(true);
        dto.setDescricaoTratamento(null);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("A descrição do tratamento é obrigatória quando o resíduo já foi tratado", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoNenhumaClasseDeResiduoInformada() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setClassesInformadasIds(Set.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("Informe pelo menos uma classe de resíduo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoClasseDeResiduoEstaInativa() {
        classe.setAtivo(false);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dtoValidoCriar()));

        assertEquals("A classe de resíduo informada está inativa.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoMedidasSegurancaInformadasNulas() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setMedidasSegurancaInformadas(null);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("Informe as medidas de segurança do resíduo.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoMedidaSegurancaOutraSemDescricao() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setMedidasSegurancaInformadas(new LinkedHashSet<>(Set.of(MedidaSeguranca.OUTRO)));
        dto.setObservacaoSegurancaInformada(null);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("Descreva a medida de segurança marcada como OUTRO.", ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoProdutoDoComponenteNaoEncontrado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.empty());

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setComponentes(List.of(componenteComProduto(PRODUTO_PUBLIC_ID)));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.criar(dto));

        assertEquals("Produto não encontrado com id: " + PRODUTO_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoProdutoDoComponenteNaoPertenceAUnidade() {
        // Correção de segurança em criarComponente(): antes, sem tenant
        // ativo, o ".orElse(false)" simplesmente ignorava essa checagem.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO_PUBLIC_ID, UNIDADE_PUBLIC_ID)).thenReturn(false);

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setComponentes(List.of(componenteComProduto(PRODUTO_PUBLIC_ID)));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.criar(dto));

        assertEquals("Produto não encontrado com id: " + PRODUTO_PUBLIC_ID, ex.getMessage());
    }

    @Test
    void deveRejeitarCriacaoQuandoComponenteSemNomeNemProduto() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        stubGeradorELaboratorioAtivos();
        stubClasseValida();

        ComponenteResiduoRequestDTO componenteInvalido = new ComponenteResiduoRequestDTO();
        componenteInvalido.setProdutoId(null);
        componenteInvalido.setNomeComponente(null);

        CriarResiduoRequestDTO dto = dtoValidoCriar();
        dto.setComponentes(List.of(componenteInvalido));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.criar(dto));

        assertEquals("O componente do resíduo deve possuir nome ou referência de produto.", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // receber()
    // ---------------------------------------------------------------

    @Test
    void deveReceberResiduoComSucesso() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubSalvarComIdGerado();

        ReceberResiduoRequestDTO dto = new ReceberResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setObservacao("Recebido no almoxarifado.");

        ResiduoResponseDTO resultado = residuoService.receber(RESIDUO_PUBLIC_ID, dto);

        assertEquals(StatusResiduo.EM_ANALISE, resultado.getStatus());
        assertEquals(GESTOR_PUBLIC_ID, resultado.getGestorRecebedorInicialId());
        verify(historicoResiduoRepository, times(1)).save(any(HistoricoResiduo.class));
    }

    @Test
    void deveRejeitarRecebimentoQuandoStatusInvalido() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));

        ReceberResiduoRequestDTO dto = new ReceberResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.receber(RESIDUO_PUBLIC_ID, dto));

        assertEquals(
                "O resíduo só pode ser recebido para análise quando estiver em INFORMADO. Status atual: EM_ANALISE",
                ex.getMessage());
    }

    @Test
    void deveRejeitarRecebimentoQuandoUsuarioGestorNaoTemPerfilAdequado() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PERFIL_INVALIDO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestorPerfilInvalido));

        ReceberResiduoRequestDTO dto = new ReceberResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PERFIL_INVALIDO_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.receber(RESIDUO_PUBLIC_ID, dto));

        assertEquals("A operação de gestão de resíduos exige perfil GESTOR ou ADMINISTRADOR.", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // analisarELiberar()
    // ---------------------------------------------------------------

    @Test
    void deveAnalisarELiberarResiduoComSucesso() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubClasseValida();
        stubSalvarComIdGerado();

        AnalisarResiduoRequestDTO dto = dtoValidoAnalisar();

        ResiduoResponseDTO resultado = residuoService.analisarELiberar(RESIDUO_PUBLIC_ID, dto);

        assertEquals(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, resultado.getStatus());
        assertEquals(NivelRisco.MEDIO, resultado.getNivelRiscoConfirmado());
        assertEquals("Abrigo de resíduos - setor químico A", resultado.getLocalArmazenamentoTemporario());
        assertEquals(1, resultado.getClassesConfirmadas().size());
        verify(historicoResiduoRepository, times(1)).save(any(HistoricoResiduo.class));
    }

    @Test
    void deveRejeitarAnaliseQuandoStatusInvalido() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubClasseValida();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.analisarELiberar(RESIDUO_PUBLIC_ID, dtoValidoAnalisar()));

        assertEquals(
                "O resíduo só pode ser liberado para armazenamento quando estiver em EM_ANALISE. Status atual: INFORMADO",
                ex.getMessage());
    }

    @Test
    void deveRejeitarAnaliseQuandoGestorDiferenteDoRecebedorInicial() {
        // Regra nova (achado da reconciliação): só quem recebeu o resíduo
        // inicialmente pode fazer a análise/liberação.
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_OUTRO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestorOutro));
        stubClasseValida();

        AnalisarResiduoRequestDTO dto = dtoValidoAnalisar();
        dto.setUsuarioGestorId(GESTOR_OUTRO_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.analisarELiberar(RESIDUO_PUBLIC_ID, dto));

        assertEquals("A análise deve ser realizada pelo gestor que recebeu inicialmente o resíduo", ex.getMessage());
    }

    @Test
    void deveRejeitarAnaliseQuandoResiduoNaoTemGestorRecebedorInicial() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubClasseValida();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.analisarELiberar(RESIDUO_PUBLIC_ID, dtoValidoAnalisar()));

        assertEquals("O resíduo não possui gestor de recebimento inicial", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // confirmarArmazenamento()
    // ---------------------------------------------------------------

    @Test
    void deveConfirmarArmazenamentoComSucesso() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubSalvarComIdGerado();

        ArmazenarResiduoRequestDTO dto = new ArmazenarResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setLocalArmazenamentoTemporario("Abrigo de resíduos - setor B (corrigido)");

        ResiduoResponseDTO resultado = residuoService.confirmarArmazenamento(RESIDUO_PUBLIC_ID, dto);

        assertEquals(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, resultado.getStatus());
        assertEquals("Abrigo de resíduos - setor B (corrigido)", resultado.getLocalArmazenamentoTemporario());
        verify(historicoResiduoRepository, times(1)).save(any(HistoricoResiduo.class));
    }

    @Test
    void deveRejeitarArmazenamentoQuandoStatusInvalido() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));

        ArmazenarResiduoRequestDTO dto = new ArmazenarResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.confirmarArmazenamento(RESIDUO_PUBLIC_ID, dto));

        assertEquals(
                "O resíduo só pode ser armazenado temporariamente quando estiver em LIBERADO_PARA_ARMAZENAMENTO. Status atual: EM_ANALISE",
                ex.getMessage());
    }

    // ---------------------------------------------------------------
    // despachar()
    // ---------------------------------------------------------------

    @Test
    void deveDespacharResiduoComSucesso() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));
        stubSalvarComIdGerado();

        DespacharResiduoRequestDTO dto = new DespacharResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setDestinoFinalConfirmado("Empresa Licenciada XYZ");
        dto.setObservacao("Despachado conforme manifesto 123.");

        ResiduoResponseDTO resultado = residuoService.despachar(RESIDUO_PUBLIC_ID, dto);

        assertEquals(StatusResiduo.DESPACHADO, resultado.getStatus());
        assertEquals("Empresa Licenciada XYZ", resultado.getDestinoFinalConfirmado());
        verify(historicoResiduoRepository, times(1)).save(any(HistoricoResiduo.class));
    }

    @Test
    void deveRejeitarDespachoQuandoStatusInvalido() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GESTOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gestor));

        DespacharResiduoRequestDTO dto = new DespacharResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setDestinoFinalConfirmado("Empresa Licenciada XYZ");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.despachar(RESIDUO_PUBLIC_ID, dto));

        assertEquals(
                "O resíduo só pode ser despachado quando estiver em ARMAZENADO_TEMPORARIAMENTE. Status atual: LIBERADO_PARA_ARMAZENAMENTO",
                ex.getMessage());
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    void deveBuscarResiduoPorId() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));

        ResiduoResponseDTO resultado = residuoService.buscarPorId(RESIDUO_PUBLIC_ID);

        assertEquals(RESIDUO_PUBLIC_ID, resultado.getId());
    }

    @Test
    void deveLancarExcecaoQuandoResiduoNaoEncontrado() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> residuoService.buscarPorId(RESIDUO_PUBLIC_ID));

        assertEquals("Resíduo não encontrado com id: " + RESIDUO_PUBLIC_ID, ex.getMessage());
    }

    // ---------------------------------------------------------------
    // listarTodos() / exigirTenantAtivo()
    // ---------------------------------------------------------------

    @Test
    void deveListarTodosOsResiduos() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByLaboratorioUnidadePublicIdOrderByDataInformacaoDesc(UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(residuo));

        List<ResiduoResponseDTO> resultado = residuoService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(RESIDUO_PUBLIC_ID, resultado.get(0).getId());
    }

    @Test
    void deveRejeitarOperacaoQuandoTenantNaoDefinido() {
        // exigirTenantAtivo(): nenhum TenantContext.definir() foi chamado,
        // simulando requisição sem o header X-SGL-Unidade-Id.
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.listarTodos());

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // listarPorStatus()
    // ---------------------------------------------------------------

    @Test
    void deveListarResiduosPorStatus() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByLaboratorioUnidadePublicIdAndStatusOrderByDataInformacaoDesc(
                UNIDADE_PUBLIC_ID, StatusResiduo.EM_ANALISE))
                .thenReturn(List.of(residuo));

        List<ResiduoResponseDTO> resultado = residuoService.listarPorStatus(StatusResiduo.EM_ANALISE);

        assertEquals(1, resultado.size());
        assertEquals(StatusResiduo.EM_ANALISE, resultado.get(0).getStatus());
    }

    // ---------------------------------------------------------------
    // listarPorLaboratorio()
    // ---------------------------------------------------------------

    @Test
    void deveListarResiduosPorLaboratorio() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
        when(residuoRepository.findByLaboratorioPublicIdOrderByDataInformacaoDesc(LABORATORIO_PUBLIC_ID))
                .thenReturn(List.of(residuo));

        List<ResiduoResponseDTO> resultado = residuoService.listarPorLaboratorio(LABORATORIO_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    // ---------------------------------------------------------------
    // listarPorGerador()
    // ---------------------------------------------------------------

    @Test
    void deveListarResiduosPorGerador() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gerador));
        when(residuoRepository.findByGeradorPublicIdOrderByDataInformacaoDesc(GERADOR_PUBLIC_ID))
                .thenReturn(List.of(residuo));

        List<ResiduoResponseDTO> resultado = residuoService.listarPorGerador(GERADOR_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveRejeitarListagemPorGeradorQuandoGeradorInativo() {
        gerador.setAtivo(false);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gerador));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> residuoService.listarPorGerador(GERADOR_PUBLIC_ID));

        assertEquals("O usuário está inativo.", ex.getMessage());
    }

    // ---------------------------------------------------------------
    // buscarHistorico()
    // ---------------------------------------------------------------

    @Test
    void deveBuscarHistoricoDeResiduo() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.EM_ANALISE, gestor);
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));

        HistoricoResiduo evento = HistoricoResiduo.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .residuo(residuo)
                .usuario(gestor)
                .status(StatusResiduo.EM_ANALISE)
                .acao("RECEBIDO_PELA_GESTAO")
                .observacao("Recebido para conferência.")
                .dataHora(LocalDateTime.now())
                .build();

        when(historicoResiduoRepository.findByResiduoIdOrderByDataHoraAsc(residuo.getId()))
                .thenReturn(List.of(evento));

        List<HistoricoResiduoResponseDTO> resultado = residuoService.buscarHistorico(RESIDUO_PUBLIC_ID);

        assertEquals(1, resultado.size());
        assertEquals("RECEBIDO_PELA_GESTAO", resultado.get(0).getAcao());
        assertEquals(GESTOR_PUBLIC_ID, resultado.get(0).getUsuarioId());
    }

    // ---------------------------------------------------------------
    // gerarDadosRotulo()
    // ---------------------------------------------------------------

    @Test
    void deveGerarDadosDeRotulo() {
        Residuo residuo = construirResiduoExistente(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, gestor);
        // Já possui código de rastreio e QR code (fluxo normal, gerado em
        // criar()/analisarELiberar()) — não deve disparar um novo save().
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));

        RotuloResiduoResponseDTO resultado = residuoService.gerarDadosRotulo(RESIDUO_PUBLIC_ID);

        assertEquals(residuo.getCodigoRastreio(), resultado.getCodigoRastreio());
        assertEquals(residuo.getQrCodeConteudo(), resultado.getQrCodeConteudo());
        assertTrue(resultado.isImpressaoPermitida());
        assertEquals(UNIDADE_PUBLIC_ID, resultado.getUnidadeId());
        verify(residuoRepository, never()).save(any(Residuo.class));
    }

    @Test
    void deveGerarCodigoEQrCodeQuandoAusentesAoGerarRotulo() {
        // Compatibilidade com resíduos antigos criados antes da correção
        // que passou a gravar codigoRastreio/qrCodeConteudo (ver comentário
        // de assegurarIdentificacaoRotulo() em ResiduoService).
        Residuo residuo = construirResiduoExistente(StatusResiduo.INFORMADO, null);
        residuo.setCodigoRastreio(null);
        residuo.setQrCodeConteudo(null);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(residuoRepository.findByPublicIdAndLaboratorioUnidadePublicId(RESIDUO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(residuo));
        stubSalvarComIdGerado();

        RotuloResiduoResponseDTO resultado = residuoService.gerarDadosRotulo(RESIDUO_PUBLIC_ID);

        assertEquals("SGL-RESIDUO:" + RESIDUO_PUBLIC_ID, resultado.getQrCodeConteudo());
        assertTrue(resultado.getCodigoRastreio().startsWith("SGL-RES-"));
        assertFalse(resultado.isImpressaoPermitida());
        verify(residuoRepository, times(1)).save(any(Residuo.class));
    }

    // ---------------------------------------------------------------
    // Helpers de mock/dados
    // ---------------------------------------------------------------

    private void stubGeradorELaboratorioAtivos() {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(GERADOR_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(gerador));
        when(laboratorioRepository.findByPublicIdAndUnidadePublicId(LABORATORIO_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(laboratorio));
    }

    private void stubClasseValida() {
        when(classeResiduoRepository.findByPublicIdInAndUnidadePublicId(Set.of(CLASSE_PUBLIC_ID), UNIDADE_PUBLIC_ID))
                .thenReturn(List.of(classe));
    }

    private void stubSalvarComIdGerado() {
        when(residuoRepository.save(any(Residuo.class))).thenAnswer(invocation -> {
            Residuo r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(999L);
            }
            if (r.getPublicId() == null) {
                r.setPublicId(RESIDUO_PUBLIC_ID);
            }
            return r;
        });
    }

    private CriarResiduoRequestDTO dtoValidoCriar() {
        CriarResiduoRequestDTO dto = new CriarResiduoRequestDTO();
        dto.setUsuarioGeradorId(GERADOR_PUBLIC_ID);
        dto.setLaboratorioId(LABORATORIO_PUBLIC_ID);
        dto.setProjetoId(null);
        dto.setDescricao("Resíduo líquido do processo de extração de DNA");
        dto.setProcessoOrigem("Extração de DNA vegetal");
        dto.setEstadoFisico(EstadoFisicoResiduo.LIQUIDO);
        dto.setTratamentoRealizado(false);
        dto.setDescricaoTratamento(null);
        dto.setRecipiente("Bombona plástica de 5 L");
        dto.setQuantidade(new BigDecimal("2.500"));
        dto.setUnidadeMedida(UnidadeMedida.L);
        dto.setNivelRiscoInformado(NivelRisco.BAIXO);
        dto.setRiscosInformados(new LinkedHashSet<>(Set.of(TipoRisco.IRRITANTE)));
        dto.setObservacaoGerador("Observação do gerador.");

        ComponenteResiduoRequestDTO componente = new ComponenteResiduoRequestDTO();
        componente.setNomeComponente("Acetona residual");
        componente.setPrincipal(true);
        componente.setConcentracaoOuQuantidade("aprox. 70%");
        dto.setComponentes(List.of(componente));

        dto.setClassesInformadasIds(new LinkedHashSet<>(Set.of(CLASSE_PUBLIC_ID)));
        dto.setMedidasSegurancaInformadas(new LinkedHashSet<>(Set.of(MedidaSeguranca.LUVAS)));
        dto.setObservacaoSegurancaInformada(null);
        return dto;
    }

    private ComponenteResiduoRequestDTO componenteComProduto(UUID produtoId) {
        ComponenteResiduoRequestDTO componente = new ComponenteResiduoRequestDTO();
        componente.setProdutoId(produtoId);
        return componente;
    }

    private AnalisarResiduoRequestDTO dtoValidoAnalisar() {
        AnalisarResiduoRequestDTO dto = new AnalisarResiduoRequestDTO();
        dto.setUsuarioGestorId(GESTOR_PUBLIC_ID);
        dto.setNivelRiscoConfirmado(NivelRisco.MEDIO);
        dto.setRiscosConfirmados(new LinkedHashSet<>(Set.of(TipoRisco.IRRITANTE)));
        dto.setLocalArmazenamentoTemporario("Abrigo de resíduos - setor químico A");
        dto.setDestinoFinalPrevisto("Empresa licenciada para tratamento de resíduos químicos");
        dto.setDataPrevistaDespacho(LocalDate.now().plusDays(10));
        dto.setObservacaoGestor("Conferido conforme laudo.");
        dto.setClassesConfirmadasIds(new LinkedHashSet<>(Set.of(CLASSE_PUBLIC_ID)));
        dto.setMedidasSegurancaConfirmadas(new LinkedHashSet<>(Set.of(MedidaSeguranca.LUVAS)));
        dto.setObservacaoSegurancaConfirmada(null);
        return dto;
    }

    private Residuo construirResiduoExistente(StatusResiduo status, Usuario gestorRecebedorInicial) {
        return Residuo.builder()
                .id(700L)
                .publicId(RESIDUO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .gerador(gerador)
                .unidadeIdSnapshot(UNIDADE_PUBLIC_ID)
                .unidadeNomeSnapshot(unidade.getNome())
                .unidadeSiglaSnapshot(unidade.getSigla())
                .descricao("Resíduo de teste")
                .processoOrigem("Processo de teste")
                .estadoFisico(EstadoFisicoResiduo.LIQUIDO)
                .tratamentoRealizado(false)
                .recipiente("Frasco")
                .quantidade(new BigDecimal("1.000"))
                .unidadeMedida(UnidadeMedida.L)
                .nivelRiscoInformado(NivelRisco.BAIXO)
                .status(status)
                .dataInformacao(LocalDateTime.now())
                .codigoRastreio("SGL-RES-2026-000700")
                .qrCodeConteudo("SGL-RESIDUO:" + RESIDUO_PUBLIC_ID)
                .gestorRecebedorInicial(gestorRecebedorInicial)
                .build();
    }
}
