package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

import com.sgl.dto.request.ItemPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.*;
import com.sgl.model.enums.*;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;

/**
 * Etapa 8: exercita PedidoService.criar() pelo contrato de contexto operacional.
 *
 * A elegibilidade pura ja tem sua propria suite. Aqui se testa que a escrita
 * busca a participacao do usuario/tenant, aplica a politica e fixa a origem
 * historica sem interferir nas regras de Estoque e Solucoes.
 *
 * O header DEV X-SGL-Unidade-Id NAO autentica o solicitante; esses testes
 * nao substituem os testes de autorizacao com identidade real.
 */
@ExtendWith(MockitoExtension.class)
class PedidoContextoOperacionalServiceTest {

    private static final UUID UNIDADE = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE = UUID.fromString("80000000-0000-0000-0000-000000000002");
    private static final UUID USUARIO = UUID.fromString("80000000-0000-0000-0000-000000000003");
    private static final UUID LAB = UUID.fromString("80000000-0000-0000-0000-000000000004");
    private static final UUID LAB_BASE = UUID.fromString("80000000-0000-0000-0000-000000000005");
    private static final UUID PROJETO = UUID.fromString("80000000-0000-0000-0000-000000000006");
    private static final UUID SCI = UUID.fromString("80000000-0000-0000-0000-000000000007");
    private static final UUID ATIVIDADE = UUID.fromString("80000000-0000-0000-0000-000000000008");
    private static final UUID PARTICIPACAO = UUID.fromString("80000000-0000-0000-0000-000000000009");
    private static final UUID SEGUNDA_PARTICIPACAO = UUID.fromString("80000000-0000-0000-0000-000000000010");
    private static final UUID SEGUNDA_ATIVIDADE = UUID.fromString("80000000-0000-0000-0000-000000000011");
    private static final UUID PRODUTO = UUID.fromString("80000000-0000-0000-0000-000000000012");

    @Mock PedidoRepository pedidoRepository;
    @Mock EstoqueCentralRepository estoqueCentralRepository;
    @Mock HistoricoLaboratorioRepository historicoLaboratorioRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock LaboratorioRepository laboratorioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProjetoRepository projetoRepository;
    @Mock MovimentacaoEstoqueService movimentacaoEstoqueService;
    @Mock ModeloSolucaoRepository modeloSolucaoRepository;
    @Mock VinculoEstagioAtividadeRepository participacaoRepository;
    @Mock VinculoOperacionalUsuarioService vinculoOperacionalService;

    @InjectMocks PedidoService service;

    private Unidade unidade;
    private Laboratorio laboratorio;
    private Laboratorio laboratorioBase;
    private Projeto projeto;
    private Sci sci;
    private Atividade atividade;
    private Estagiario estagiario;
    private VinculoEstagio vinculo;
    private VinculoEstagioAtividade participacao;
    private Produto produto;
    private EstoqueCentral estoque;

    @BeforeEach
    void preparar() {
        TenantContext.definir(UNIDADE);
        LocalDate hoje = LocalDate.now();

        unidade = Unidade.builder().id(1L).publicId(UNIDADE)
                .nome("Unidade A").sigla("UA").build();
        laboratorio = Laboratorio.builder().id(2L).publicId(LAB)
                .unidade(unidade).nome("Laboratorio da atividade").ativo(true).build();
        laboratorioBase = Laboratorio.builder().id(20L).publicId(LAB_BASE)
                .unidade(unidade).nome("Laboratorio base do usuario").ativo(true).build();

        projeto = Projeto.builder().id(3L).publicId(PROJETO).laboratorio(laboratorio)
                .nome("Projeto original").ativo(true)
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .dataInicio(hoje.minusMonths(4)).dataFim(hoje.plusMonths(4)).build();
        sci = Sci.builder().id(4L).publicId(SCI).projeto(projeto)
                .nome("SCI original").ativo(true)
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .dataInicio(hoje.minusMonths(3)).dataFim(hoje.plusMonths(3)).build();
        atividade = Atividade.builder().id(5L).publicId(ATIVIDADE).sci(sci)
                .nome("Atividade original").ativo(true)
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .dataInicio(hoje.minusMonths(2)).dataFim(hoje.plusMonths(2)).build();

        estagiario = new Estagiario();
        estagiario.setId(6L);
        estagiario.setPublicId(USUARIO);
        estagiario.setNome("Estagiario A");
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorioBase); // diferente da atividade
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setAtivo(true);

        vinculo = new VinculoEstagio();
        vinculo.setId(7L);
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(hoje.minusMonths(4));
        vinculo.setDataFimPrevista(hoje.plusMonths(4));
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        participacao = novaParticipacao(PARTICIPACAO, atividade);

        produto = Produto.builder().id(8L).publicId(PRODUTO)
                .nome("Produto de teste").unidadeMedida(UnidadeMedida.ML)
                .perecivel(false).ativo(true).build();
        estoque = EstoqueCentral.builder().id(9L).unidade(unidade)
                .produto(produto).quantidadeAtual(new BigDecimal("100"))
                .ativo(true).build();
    }

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    private VinculoEstagioAtividade novaParticipacao(UUID id, Atividade origem) {
        VinculoEstagioAtividade nova = new VinculoEstagioAtividade();
        nova.setId(id.equals(PARTICIPACAO) ? 10L : 11L);
        nova.setPublicId(id);
        nova.setVinculoEstagio(vinculo);
        nova.setAtividade(origem);
        nova.setDataInicioParticipacao(LocalDate.now().minusMonths(1));
        return nova;
    }

    private PedidoRequestDTO pedido(UUID participacaoId) {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(USUARIO);
        dto.setParticipacaoId(participacaoId);
        dto.setTipo(TipoPedido.PRODUTOS);
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setProdutoId(PRODUTO);
        item.setQuantidadeSolicitada(new BigDecimal("12.500"));
        item.setUnidadeMedidaSolicitada(UnidadeMedida.ML);
        dto.setItens(List.of(item));
        return dto;
    }

    private void prepararSolicitante(Usuario usuario) {
        when(usuarioRepository.findByPublicIdAndUnidadePublicId(USUARIO, UNIDADE))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararParticipacao(UUID id, VinculoEstagioAtividade origem) {
        when(participacaoRepository.buscarParticipacaoParaPedido(id, USUARIO, UNIDADE))
                .thenReturn(Optional.of(origem));
    }

    private void prepararProdutoESalvamento() {
        when(produtoRepository.findByPublicId(PRODUTO)).thenReturn(Optional.of(produto));
        when(produtoRepository.pertenceAUnidade(PRODUTO, UNIDADE)).thenReturn(true);
        when(estoqueCentralRepository.findByUnidadeIdAndProdutoId(1L, 8L))
                .thenReturn(Optional.of(estoque));
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void confirmarSemGravacao() {
        verify(pedidoRepository, never()).save(any(Pedido.class));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void estagiarioCriaProdutoPelaParticipacaoSemLaboratorioOuProjetoNoPayload() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        prepararProdutoESalvamento();

        PedidoResponseDTO resultado = service.criar(pedido(PARTICIPACAO));

        assertEquals(PARTICIPACAO, resultado.getParticipacaoId());
        assertEquals(ATIVIDADE, resultado.getAtividadeOrigemId());
        assertEquals(SCI, resultado.getSciOrigemId());
        assertEquals(PROJETO, resultado.getProjetoId());
        assertEquals(LAB, resultado.getLaboratorioId());
        assertEquals("Atividade original", resultado.getAtividadeOrigemNome());
        assertEquals("SCI original", resultado.getSciOrigemNome());
        assertEquals("Projeto original", resultado.getProjetoOrigemNome());
        assertEquals("Laboratorio da atividade", resultado.getLaboratorioOrigemNome());
        assertEquals(StatusPedido.PENDENTE, resultado.getStatus());
        assertEquals(0, new BigDecimal("12.500")
                .compareTo(resultado.getItens().get(0).getQuantidadeSolicitada()));
        verify(pedidoRepository).save(any(Pedido.class));
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void duasParticipacoesNoMesmoProjetoMantemOrigensDistintas() {
        Atividade outraAtividade = Atividade.builder().id(15L).publicId(SEGUNDA_ATIVIDADE)
                .nome("Segunda atividade").sci(sci).ativo(true)
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .dataInicio(LocalDate.now().minusMonths(1)).build();
        VinculoEstagioAtividade outra = novaParticipacao(SEGUNDA_PARTICIPACAO, outraAtividade);

        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        prepararParticipacao(SEGUNDA_PARTICIPACAO, outra);
        prepararProdutoESalvamento();

        PedidoResponseDTO primeiro = service.criar(pedido(PARTICIPACAO));
        PedidoResponseDTO segundo = service.criar(pedido(SEGUNDA_PARTICIPACAO));

        assertEquals(PROJETO, primeiro.getProjetoId());
        assertEquals(PROJETO, segundo.getProjetoId());
        assertEquals(ATIVIDADE, primeiro.getAtividadeOrigemId());
        assertEquals(SEGUNDA_ATIVIDADE, segundo.getAtividadeOrigemId());
        assertNotEquals(primeiro.getParticipacaoId(), segundo.getParticipacaoId());
        verify(pedidoRepository, times(2)).save(any(Pedido.class));
    }

    @Test
    void ausenciaDeParticipacaoBloqueiaEstagiarioSemFallbackAoLaboratorioBase() {
        prepararSolicitante(estagiario);

        assertThrows(BusinessRuleException.class, () -> service.criar(pedido(null)));

        confirmarSemGravacao();
        verifyNoInteractions(participacaoRepository, laboratorioRepository);
    }

    @Test
    void participacaoDeOutroUsuarioNaoPodeSerUtilizada() {
        prepararSolicitante(estagiario);
        when(participacaoRepository.buscarParticipacaoParaPedido(PARTICIPACAO, USUARIO, UNIDADE))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        verify(participacaoRepository).buscarParticipacaoParaPedido(PARTICIPACAO, USUARIO, UNIDADE);
        confirmarSemGravacao();
    }

    @Test
    void unidadeDivergenteBloqueiaMesmoQuandoRepositorioRetornaParticipacao() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        Unidade outra = Unidade.builder().id(77L).publicId(OUTRA_UNIDADE).nome("Outra").build();
        laboratorio.setUnidade(outra);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        assertTrue(erro.getMessage().contains("UNIDADE_DIVERGENTE"));
        confirmarSemGravacao();
    }

    @Test
    void participacaoEncerradaBloqueiaNovoPedido() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        participacao.setDataFimParticipacao(LocalDate.now().minusDays(1));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        assertTrue(erro.getMessage().contains("PARTICIPACAO_ENCERRADA"));
        confirmarSemGravacao();
    }

    @Test
    void participacaoFuturaBloqueiaNovoPedido() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        participacao.setDataInicioParticipacao(LocalDate.now().plusDays(1));

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        assertTrue(erro.getMessage().contains("PARTICIPACAO_FORA_DA_VIGENCIA"));
        confirmarSemGravacao();
    }

    @Test
    void vinculoFinalizadoBloqueiaNovoPedido() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        vinculo.setSituacao(SituacaoEstagio.FINALIZADO);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        assertTrue(erro.getMessage().contains("VINCULO_FINALIZADO"));
        confirmarSemGravacao();
    }

    @Test
    void sciInativoBloqueiaNovoPedido() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        sci.setAtivo(false);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        assertTrue(erro.getMessage().contains("HIERARQUIA_INATIVA"));
        confirmarSemGravacao();
    }

    @Test
    void projetoDivergenteDaParticipacaoEhRejeitado() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        PedidoRequestDTO dto = pedido(PARTICIPACAO);
        dto.setProjetoId(UUID.randomUUID());

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(dto));

        assertTrue(erro.getMessage().contains("Projeto informado diverge"));
        confirmarSemGravacao();
    }

    @Test
    void laboratorioDivergenteDaParticipacaoEhRejeitado() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        PedidoRequestDTO dto = pedido(PARTICIPACAO);
        dto.setLaboratorioId(LAB_BASE);

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(dto));

        assertTrue(erro.getMessage().contains("Laboratório informado diverge"));
        confirmarSemGravacao();
    }

    @Test
    void estagiarioSolicitaSolucaoComProjetoDerivado() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        prepararProdutoESalvamento();

        PedidoRequestDTO dto = pedido(PARTICIPACAO);
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setNomeSolucao("Solucao de teste");

        PedidoResponseDTO resultado = service.criar(dto);

        assertEquals(TipoPedido.SOLUCAO, resultado.getTipo());
        assertEquals(PROJETO, resultado.getProjetoId());
        assertEquals(LAB, resultado.getLaboratorioId());
        assertEquals(PARTICIPACAO, resultado.getParticipacaoId());
        assertEquals("Solucao de teste", resultado.getNomeSolucao());
        verifyNoInteractions(movimentacaoEstoqueService);
    }

    @Test
    void perfilDiferenteDeEstagiarioNaoPodeEnviarParticipacao() {
        Usuario tecnico = usuarioDePerfil(Perfil.TECNICO, laboratorio);
        prepararSolicitante(tecnico);

        assertThrows(BusinessRuleException.class,
                () -> service.criar(pedido(PARTICIPACAO)));

        confirmarSemGravacao();
        verifyNoInteractions(participacaoRepository);
    }

    @Test
    void tecnicoPodeCriarPedidoSemProjetoUsandoLaboratorioBase() {
        Usuario tecnico = usuarioDePerfil(Perfil.TECNICO, laboratorio);
        prepararSolicitante(tecnico);
        prepararProdutoESalvamento();

        PedidoResponseDTO resultado = service.criar(pedido(null));

        assertEquals(LAB, resultado.getLaboratorioId());
        assertNull(resultado.getProjetoId());
        assertNull(resultado.getParticipacaoId());
        assertNull(resultado.getAtividadeOrigemId());
        verifyNoInteractions(participacaoRepository);
    }

    @Test
    void tecnicoComProjetoDeterminaLaboratorioSemEnviaLoSeparadamente() {
        Usuario tecnico = usuarioDePerfil(Perfil.TECNICO, laboratorio);
        prepararSolicitante(tecnico);
        when(projetoRepository.findByPublicId(PROJETO)).thenReturn(Optional.of(projeto));
        prepararProdutoESalvamento();
        PedidoRequestDTO dto = pedido(null);
        dto.setProjetoId(PROJETO);

        PedidoResponseDTO resultado = service.criar(dto);

        assertEquals(LAB, resultado.getLaboratorioId());
        assertEquals(PROJETO, resultado.getProjetoId());
        assertNull(resultado.getParticipacaoId());
    }

    @Test
    void gestorPodeEscolherLaboratorioDaSuaUnidade() {
        Usuario gestor = usuarioDePerfil(Perfil.GESTOR, null);
        prepararSolicitante(gestor);
        when(laboratorioRepository.findByPublicId(LAB)).thenReturn(Optional.of(laboratorio));
        prepararProdutoESalvamento();
        PedidoRequestDTO dto = pedido(null);
        dto.setLaboratorioId(LAB);

        PedidoResponseDTO resultado = service.criar(dto);

        assertEquals(LAB, resultado.getLaboratorioId());
        assertNull(resultado.getParticipacaoId());
    }

    @Test
    void solucaoDeTecnicoContinuaExigindoProjeto() {
        Usuario tecnico = usuarioDePerfil(Perfil.TECNICO, laboratorio);
        prepararSolicitante(tecnico);
        PedidoRequestDTO dto = pedido(null);
        dto.setTipo(TipoPedido.SOLUCAO);
        dto.setNomeSolucao("Solucao sem projeto");

        BusinessRuleException erro = assertThrows(BusinessRuleException.class,
                () -> service.criar(dto));

        assertTrue(erro.getMessage().contains("Projeto é obrigatório"));
        confirmarSemGravacao();
    }

    @Test
    void participacaoAlteradaNaoReescreveContextoHistoricoDoPedido() {
        prepararSolicitante(estagiario);
        prepararParticipacao(PARTICIPACAO, participacao);
        prepararProdutoESalvamento();

        final Pedido[] salvo = new Pedido[1];
        // Substituir o retorno padrao por captura: a resposta inicial ja
        // permite demonstrar que os dados historicos foram gravados no pedido.
        PedidoResponseDTO original = service.criar(pedido(PARTICIPACAO));
        org.mockito.ArgumentCaptor<Pedido> captor = org.mockito.ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        salvo[0] = captor.getValue();

        Atividade novaAtividade = Atividade.builder()
                .id(22L).publicId(SEGUNDA_ATIVIDADE).sci(sci)
                .nome("Outra origem").build();
        participacao.setAtividade(novaAtividade);
        atividade.setNome("Atividade renomeada");
        sci.setNome("SCI renomeado");
        projeto.setNome("Projeto renomeado");
        laboratorio.setNome("Laboratorio renomeado");

        PedidoResponseDTO historico = new PedidoResponseDTO(salvo[0]);

        assertEquals(original.getParticipacaoId(), historico.getParticipacaoId());
        assertEquals(ATIVIDADE, historico.getAtividadeOrigemId());
        assertEquals(SCI, historico.getSciOrigemId());
        assertEquals(PROJETO, historico.getProjetoId());
        assertEquals(LAB, historico.getLaboratorioId());
        assertEquals("Atividade original", historico.getAtividadeOrigemNome());
        assertEquals("SCI original", historico.getSciOrigemNome());
        assertEquals("Projeto original", historico.getProjetoNome());
        assertEquals("Laboratorio da atividade", historico.getLaboratorioNome());
    }

    @Test
    void pedidoLegadoSemParticipacaoMantemRespostaCompativel() {
        Pedido legado = Pedido.builder()
                .usuario(estagiario).laboratorio(laboratorio)
                .projeto(projeto).itens(new ArrayList<>()).build();

        PedidoResponseDTO resposta = new PedidoResponseDTO(legado);

        assertNull(resposta.getParticipacaoId());
        assertNull(resposta.getAtividadeOrigemId());
        assertNull(resposta.getSciOrigemId());
        assertEquals("Projeto original", resposta.getProjetoNome());
        assertEquals("Laboratorio da atividade", resposta.getLaboratorioNome());
    }

    private Usuario usuarioDePerfil(Perfil perfil, Laboratorio labBase) {
        Usuario usuario = new Usuario();
        usuario.setId(6L);
        usuario.setPublicId(USUARIO);
        usuario.setNome("Usuario nao estagiario");
        usuario.setUnidade(unidade);
        usuario.setLaboratorio(labBase);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }
}
