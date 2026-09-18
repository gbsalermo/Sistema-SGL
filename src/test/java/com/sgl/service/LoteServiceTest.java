package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.sgl.dto.request.AtualizarLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.tenant.TenantContext;

/**
 * Testes unitários de {@link LoteService}.
 *
 * O foco pedido é o {@code LoteService} isolado (a lógica de escolha
 * FIFO/FEFO em si já é exercitada indiretamente via
 * {@code MovimentacaoEstoqueServiceTest}) — aqui cobrimos {@code atualizar()}
 * e {@code inativar()}, além das consultas simples.
 *
 * Nota: {@code atualizar()} no fonte atual tem CINCO validações de negócio em
 * sequência (número duplicado, data de validade vs. perecibilidade do
 * produto, saldo ao inativar, imutabilidade do tipo de embalagem e — uma
 * regra adicionada depois que o brief original foi escrito —
 * "fracionamento não pode ser revogado"). O brief mencionava só quatro
 * ("linhas 82/93/101/107" de uma versão anterior do arquivo, hoje com 184
 * linhas em vez de 166); a quinta regra foi incluída abaixo para manter a
 * cobertura completa de toda BusinessRuleException lançada pelo método,
 * como pedem os Global Constraints do plano.
 */
@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTOQUE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LOTE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private EstoqueCentralRepository estoqueCentralRepository;

    @InjectMocks
    private LoteService loteService;

    private Unidade unidade;
    private Produto produto;
    private EstoqueCentral estoque;
    private Lote lote;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setPublicId(UNIDADE_PUBLIC_ID);
        unidade.setSigla("CNPMF");
        unidade.setNome("Embrapa Mandioca e Fruticultura");

        produto = new Produto();
        produto.setId(10L);
        produto.setPublicId(PRODUTO_PUBLIC_ID);
        produto.setNome("Ágar Nutriente");
        produto.setUnidadeMedida(UnidadeMedida.KG);
        produto.setPerecivel(false);
        produto.setAtivo(true);

        estoque = EstoqueCentral.builder()
                .id(20L)
                .publicId(ESTOQUE_PUBLIC_ID)
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(2)
                .quantidadeMinima(1)
                .ativo(true)
                .build();

        lote = new Lote();
        lote.setId(30L);
        lote.setPublicId(LOTE_PUBLIC_ID);
        lote.setEstoqueCentral(estoque);
        // codigoInterno/sequencialInterno só podem ser definidos uma vez
        // (imutáveis) — ver Lote.definirCodigoInterno().
        lote.definirCodigoInterno("LOT-AGAR-001", 1);
        lote.setNumeroLote("FAB-2026-001");
        lote.setTipoEmbalagem(TipoEmbalagem.UNITARIO);
        lote.setApresentacao("Frasco de 1 kg");
        lote.setQuantidadeApresentacoes(2);
        lote.setConteudoPorApresentacao(1);
        lote.setFracionavel(true);
        lote.setObservacao("Lote de teste");
        lote.setQuantidadeInicial(2);
        lote.setQuantidadeDisponivel(2);
        lote.setDataEntrada(LocalDate.of(2026, 1, 10));
        lote.setDataValidade(null);
        lote.setAtivo(true);
    }

    @AfterEach
    void tearDown() {
        TenantContext.limpar();
    }

    @Test
    void deveListarTodosOsLotes() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByEstoqueCentralUnidadePublicId(UNIDADE_PUBLIC_ID)).thenReturn(List.of(lote));

        List<LoteResponseDTO> resultado = loteService.listarTodos();

        assertEquals(1, resultado.size());
        assertEquals(LOTE_PUBLIC_ID, resultado.get(0).getId());
    }

    @Test
    void deveBuscarLotePorId() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));

        LoteResponseDTO resultado = loteService.buscarPorId(LOTE_PUBLIC_ID);

        assertEquals(LOTE_PUBLIC_ID, resultado.getId());
        assertEquals("LOT-AGAR-001", resultado.getCodigoInterno());
    }

    @Test
    void deveListarLotesPorEstoque() {
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(ESTOQUE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(estoque));
        when(loteRepository.findByEstoqueCentralId(estoque.getId())).thenReturn(List.of(lote));

        List<LoteResponseDTO> resultado = loteService.listarPorEstoque(ESTOQUE_PUBLIC_ID);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveListarLotesVencidos() {
        Lote loteVencidoComSaldo = new Lote();
        loteVencidoComSaldo.setId(31L);
        loteVencidoComSaldo.setPublicId(UUID.randomUUID());
        loteVencidoComSaldo.setEstoqueCentral(estoque);
        loteVencidoComSaldo.definirCodigoInterno("LOT-AGAR-002", 2);
        loteVencidoComSaldo.setNumeroLote("FAB-2025-999");
        loteVencidoComSaldo.setFracionavel(true);
        loteVencidoComSaldo.setQuantidadeInicial(1);
        loteVencidoComSaldo.setQuantidadeDisponivel(1);
        loteVencidoComSaldo.setDataEntrada(LocalDate.of(2025, 1, 1));
        loteVencidoComSaldo.setDataValidade(LocalDate.now().minusDays(1));
        loteVencidoComSaldo.setAtivo(true);

        // A query do repositório já filtra por ativo=true e
        // dataValidade < hoje; o filtro extra "quantidadeDisponivel > 0" é
        // feito em memória pelo Service. Incluímos um lote com saldo
        // zerado na resposta simulada do mock para provar que esse filtro
        // adicional realmente remove esse lote do resultado.
        Lote loteVencidoSemSaldo = new Lote();
        loteVencidoSemSaldo.setId(32L);
        loteVencidoSemSaldo.setPublicId(UUID.randomUUID());
        loteVencidoSemSaldo.setEstoqueCentral(estoque);
        loteVencidoSemSaldo.definirCodigoInterno("LOT-AGAR-003", 3);
        loteVencidoSemSaldo.setNumeroLote("FAB-2025-998");
        loteVencidoSemSaldo.setFracionavel(true);
        loteVencidoSemSaldo.setQuantidadeInicial(1);
        loteVencidoSemSaldo.setQuantidadeDisponivel(0);
        loteVencidoSemSaldo.setDataEntrada(LocalDate.of(2025, 1, 1));
        loteVencidoSemSaldo.setDataValidade(LocalDate.now().minusDays(1));
        loteVencidoSemSaldo.setAtivo(true);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByEstoqueCentralUnidadePublicIdAndDataValidadeBeforeAndAtivoTrue(
                UNIDADE_PUBLIC_ID, LocalDate.now()))
                .thenReturn(List.of(loteVencidoComSaldo, loteVencidoSemSaldo));

        List<LoteResponseDTO> resultado = loteService.listarVencidos();

        assertEquals(1, resultado.size());
        assertEquals("FAB-2025-999", resultado.get(0).getNumeroLote());
    }

    @Test
    void deveAtualizarLote() {
        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setApresentacao("Frasco de 1 kg - reembalado");
        dto.setObservacao("Observação atualizada.");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote()))
                .thenReturn(false);
        when(loteRepository.save(lote)).thenReturn(lote);

        LoteResponseDTO resultado = loteService.atualizar(LOTE_PUBLIC_ID, dto);

        assertEquals("Frasco de 1 kg - reembalado", resultado.getApresentacao());
        assertEquals("Observação atualizada.", resultado.getObservacao());
        assertEquals(dto.getNumeroLote(), resultado.getNumeroLote());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoNumeroDeLoteJaExisteNoEstoque() {
        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setNumeroLote("FAB-2026-002");

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), "FAB-2026-002"))
                .thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.atualizar(LOTE_PUBLIC_ID, dto));

        assertEquals("Já existe lote com esse número neste estoque.", ex.getMessage());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoDataDeValidadeIncompativelComPerecibilidadeDoProduto() {
        // Regra vive na entidade Produto (validateLotExpirationDate), mas
        // quem a aciona é o LoteService.atualizar(). Produto perecível
        // exige data de validade; aqui o dto chega sem.
        produto.setPerecivel(true);

        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setDataValidade(null);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote()))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.atualizar(LOTE_PUBLIC_ID, dto));

        assertEquals("Data de validade é obrigatória para produto perecível.", ex.getMessage());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoInativarLoteComSaldoDisponivel() {
        // lote.quantidadeDisponivel = 2 (definido no setUp) > 0.
        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setAtivo(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote()))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.atualizar(LOTE_PUBLIC_ID, dto));

        assertEquals("Lote com saldo disponível não pode ser inativado diretamente.", ex.getMessage());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoTipoDeEmbalagemForAlterado() {
        // lote.tipoEmbalagem = UNITARIO (setUp); tentativa de trocar para KIT.
        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setTipoEmbalagem(TipoEmbalagem.KIT);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote()))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.atualizar(LOTE_PUBLIC_ID, dto));

        assertEquals("O tipo de embalagem original do lote não pode ser alterado.", ex.getMessage());
    }

    @Test
    void deveRejeitarAtualizacaoQuandoFracionamentoDeixaDeSerPermitido() {
        // lote.fracionavel = true (setUp) => permiteFracionamento() = true.
        // dto tenta revogar isso, o que a regra proíbe (evita que retiradas
        // já liberadas como unitárias voltem a exigir embalagem completa).
        AtualizarLoteRequestDTO dto = dtoValido();
        dto.setFracionavel(false);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));
        when(loteRepository.existsByEstoqueCentralIdAndNumeroLote(estoque.getId(), dto.getNumeroLote()))
                .thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.atualizar(LOTE_PUBLIC_ID, dto));

        assertEquals(
                "Um lote liberado para retirada unitária não pode voltar a exigir embalagem completa.",
                ex.getMessage());
    }

    @Test
    void deveInativarLote() {
        lote.setQuantidadeDisponivel(0);

        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));

        loteService.inativar(LOTE_PUBLIC_ID);

        assertFalse(lote.getAtivo());
    }

    @Test
    void deveRejeitarInativacaoQuandoLoteAindaTemSaldoDisponivel() {
        // lote.quantidadeDisponivel = 2 (setUp) > 0.
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(LOTE_PUBLIC_ID, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.of(lote));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.inativar(LOTE_PUBLIC_ID));

        assertEquals("Lote com saldo disponível não pode ser inativado diretamente.", ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoLoteNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(loteRepository.findByPublicIdAndEstoqueCentralUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> loteService.buscarPorId(idInexistente));

        assertEquals("Lote não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveLancarExcecaoQuandoEstoqueCentralNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        TenantContext.definir(UNIDADE_PUBLIC_ID);
        when(estoqueCentralRepository.findByPublicIdAndUnidadePublicId(idInexistente, UNIDADE_PUBLIC_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> loteService.listarPorEstoque(idInexistente));

        assertEquals("Estoque central não encontrado com id: " + idInexistente, ex.getMessage());
    }

    @Test
    void deveRejeitarOperacaoQuandoTenantNaoDefinido() {
        // exigirTenantAtivo(): nenhum TenantContext.definir() foi chamado
        // neste teste, simulando requisição sem o header X-SGL-Unidade-Id.
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> loteService.listarTodos());

        assertEquals("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.", ex.getMessage());
    }

    private AtualizarLoteRequestDTO dtoValido() {
        AtualizarLoteRequestDTO dto = new AtualizarLoteRequestDTO();
        dto.setNumeroLote(lote.getNumeroLote());
        dto.setTipoEmbalagem(lote.getTipoEmbalagem());
        dto.setApresentacao(lote.getApresentacao());
        dto.setFracionavel(true);
        dto.setObservacao(lote.getObservacao());
        dto.setDataValidade(null);
        dto.setAtivo(true);
        return dto;
    }
}
