package com.sgl.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.dto.request.AprovarPedidoRequestDTO;
import com.sgl.dto.request.ItemPedidoRequestDTO;
import com.sgl.dto.request.PedidoRequestDTO;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.*;
import com.sgl.model.enums.*;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;

/**
 * Etapa 7.6-B: contrato Pedido convencional -> baixa física efetiva.
 * Não duplica os testes unitários do algoritmo FEFO/FIFO.
 */
@SpringBootTest
@ActiveProfiles("test")
class PedidoEstoqueFisicoIntegrationTest {
    @Autowired private PedidoService pedidos;
    @Autowired private UnidadeRepository unidades;
    @Autowired private LaboratorioRepository laboratorios;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProdutoRepository produtos;
    @Autowired private EstoqueCentralRepository estoques;
    @Autowired private LoteRepository lotes;
    @Autowired private RecipienteEstoqueRepository recipientes;
    @Autowired private MovimentacaoEstoqueRepository movimentacoes;

    @AfterEach void limparContexto() { TenantContext.limpar(); }

    private record Cenario(Unidade unidade, Laboratorio lab, Usuario gestor,
                           Produto produto, EstoqueCentral estoque) {}

    private Cenario cenario(boolean perecivel) {
        String chave = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        Unidade u = unidades.saveAndFlush(Unidade.builder()
                .nome("Unidade 76B " + chave).sigla("U" + chave).build());
        Laboratorio lab = laboratorios.saveAndFlush(Laboratorio.builder()
                .unidade(u).nome("Laboratorio 76B " + chave).ativo(true).build());
        Usuario gestor = new Usuario();
        gestor.setNome("Gestor 76B");
        gestor.setEmail("gestor76b-" + chave.toLowerCase() + "@sgl.test");
        gestor.setSenha("senha-teste");
        gestor.setPerfil(Perfil.GESTOR);
        gestor.setUnidade(u);
        gestor.setLaboratorio(lab);
        gestor.setAtivo(true);
        gestor = usuarios.saveAndFlush(gestor);
        Produto produto = produtos.saveAndFlush(Produto.builder()
                .nome("Etanol 76B " + chave).codigoReferencia("P76B" + chave)
                .unidadeMedida(UnidadeMedida.ML).risco(NivelRisco.NENHUM)
                .perecivel(perecivel).unidadeArmazenamento("mL").ativo(true).build());
        EstoqueCentral estoque = estoques.saveAndFlush(EstoqueCentral.builder()
                .unidade(u).produto(produto).quantidadeAtual(BigDecimal.ZERO)
                .quantidadeMinima(BigDecimal.ZERO).ativo(true).build());
        TenantContext.definir(u.getPublicId());
        return new Cenario(u, lab, gestor, produto, estoque);
    }

    private Lote lote(Cenario c, int ordem, LocalDate validade, boolean fracionavel,
                      BigDecimal... saldos) {
        String codigo = "LOT-76B-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal total = java.util.Arrays.stream(saldos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Lote lote = new Lote();
        lote.setEstoqueCentral(c.estoque());
        lote.definirCodigoInterno(codigo, ordem);
        lote.setNumeroLote("FORN-" + codigo);
        lote.setTipoEmbalagem(TipoEmbalagem.FRASCO);
        lote.setApresentacao("frasco de 500 mL");
        lote.setQuantidadeApresentacoes(saldos.length);
        lote.setConteudoPorApresentacao(new BigDecimal("500"));
        lote.setFracionavel(fracionavel);
        lote.setQuantidadeInicial(total);
        lote.setQuantidadeDisponivel(total);
        lote.setDataEntrada(LocalDate.now().minusDays(20L - ordem));
        lote.setDataValidade(validade);
        lote.setAtivo(true);
        lote = lotes.saveAndFlush(lote);
        int seq = 0;
        for (BigDecimal saldo : saldos) {
            RecipienteEstoque r = new RecipienteEstoque();
            r.setLote(lote);
            r.definirIdentificacao(codigo + "-R" + (++seq), seq);
            r.setTipoEmbalagem(TipoEmbalagem.FRASCO);
            r.setCapacidadeInicial(new BigDecimal("500"));
            r.setQuantidadeDisponivel(saldo);
            r.setUnidadeMedida(UnidadeMedida.ML);
            r.setEstado(saldo.compareTo(new BigDecimal("500")) == 0
                    ? EstadoRecipienteEstoque.FECHADO : EstadoRecipienteEstoque.ABERTO);
            if (r.getEstado() == EstadoRecipienteEstoque.ABERTO)
                r.setDataAbertura(java.time.LocalDateTime.now().minusDays(1));
            recipientes.save(r);
        }
        recipientes.flush();
        c.estoque().setQuantidadeAtual(c.estoque().getQuantidadeAtual().add(total));
        estoques.saveAndFlush(c.estoque());
        return lote;
    }

    private PedidoResponseDTO criar(Cenario c, String quantidade, UnidadeMedida unidade) {
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setProdutoId(c.produto().getPublicId());
        item.setQuantidadeSolicitada(new BigDecimal(quantidade));
        item.setUnidadeMedidaSolicitada(unidade);
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setUsuarioId(c.gestor().getPublicId());
        dto.setLaboratorioId(c.lab().getPublicId());
        dto.setItens(List.of(item));
        return pedidos.criar(dto);
    }

    private PedidoResponseDTO aprovar(Cenario c, PedidoResponseDTO criado, String aprovado) {
        AprovarPedidoRequestDTO dto = new AprovarPedidoRequestDTO();
        dto.setUsuarioAprovadorId(c.gestor().getPublicId());
        dto.setItens(List.of(new AprovarPedidoRequestDTO.ItemAprovacaoDTO(
                criado.getItens().get(0).getId(), new BigDecimal(aprovado))));
        return pedidos.aprovar(criado.getId(), dto);
    }

    private void saldo(Lote l, String esperado) {
        Lote atual = lotes.findById(l.getId()).orElseThrow();
        assertEquals(0, atual.getQuantidadeDisponivel().compareTo(new BigDecimal(esperado)));
        BigDecimal soma = recipientes.findByLoteIdOrderBySequencialAsc(l.getId()).stream()
                .map(RecipienteEstoque::getQuantidadeDisponivel)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, soma.compareTo(new BigDecimal(esperado)),
                "Soma dos recipientes deve conferir com saldo do lote");
    }

    @Test
    void retirada200ConsomeApenasAberto() {
        Cenario c = cenario(false);
        Lote l = lote(c, 1, null, true, new BigDecimal("300"), new BigDecimal("500"));
        PedidoResponseDTO criado = criar(c, "200", UnidadeMedida.ML);
        aprovar(c, criado, "200");
        List<RecipienteEstoque> r = recipientes.findByLoteIdOrderBySequencialAsc(l.getId());
        assertEquals(0, r.get(0).getQuantidadeDisponivel().compareTo(new BigDecimal("100")));
        assertEquals(EstadoRecipienteEstoque.FECHADO, r.get(1).getEstado());
        saldo(l, "600");
    }

    @Test
    void retirada500PreservaAbertoEEntregaNaoBaixaNovamente() {
        Cenario c = cenario(false);
        Lote l = lote(c, 1, null, true, new BigDecimal("300"), new BigDecimal("500"));
        PedidoResponseDTO criado = criar(c, "500", UnidadeMedida.ML);
        aprovar(c, criado, "500");
        List<RecipienteEstoque> r = recipientes.findByLoteIdOrderBySequencialAsc(l.getId());
        assertEquals(0, r.get(0).getQuantidadeDisponivel().compareTo(new BigDecimal("300")));
        assertEquals(EstadoRecipienteEstoque.ABERTO, r.get(0).getEstado());
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, r.get(1).getEstado());
        saldo(l, "300");
        assertEquals(StatusPedido.ENTREGUE, pedidos.entregar(criado.getId()).getStatus());
        saldo(l, "300");
    }

    @Test
    void retirada700UsaFechadoEAbertoSemAbrirSegundoFechado() {
        Cenario c = cenario(false);
        Lote l = lote(c, 1, null, true,
                new BigDecimal("300"), new BigDecimal("500"), new BigDecimal("500"));
        PedidoResponseDTO criado = criar(c, "0.700", UnidadeMedida.L);
        assertEquals(0, new BigDecimal("700").compareTo(criado.getItens().get(0).getQuantidadeSolicitada()));
        assertEquals(StatusPedido.APROVADO, aprovar(c, criado, "700").getStatus());
        List<RecipienteEstoque> r = recipientes.findByLoteIdOrderBySequencialAsc(l.getId());
        assertEquals(0, r.get(0).getQuantidadeDisponivel().compareTo(new BigDecimal("100")));
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, r.get(1).getEstado());
        assertEquals(EstadoRecipienteEstoque.FECHADO, r.get(2).getEstado());
        saldo(l, "600");
        assertEquals(0, estoques.findById(c.estoque().getId()).orElseThrow()
                .getQuantidadeAtual().compareTo(new BigDecimal("600")));
    }

    @Test
    void pedidoGrandeConsomeFechadosEComplementaAberto() {
        Cenario c = cenario(false);
        Lote l = lote(c, 1, null, true,
                new BigDecimal("300"), new BigDecimal("500"),
                new BigDecimal("500"), new BigDecimal("500"));
        PedidoResponseDTO criado = criar(c, "1200", UnidadeMedida.ML);
        aprovar(c, criado, "1200");
        List<RecipienteEstoque> r = recipientes.findByLoteIdOrderBySequencialAsc(l.getId());
        assertEquals(0, r.get(0).getQuantidadeDisponivel().compareTo(new BigDecimal("100")));
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, r.get(1).getEstado());
        assertEquals(EstadoRecipienteEstoque.ESGOTADO, r.get(2).getEstado());
        assertEquals(EstadoRecipienteEstoque.FECHADO, r.get(3).getEstado());
        saldo(l, "600");
    }

    @Test
    void fefoIgnoraVencidoEPriorizaLoteValidoMesmoFechado() {
        Cenario c = cenario(true);
        Lote vencido = lote(c, 1, LocalDate.now().minusDays(1), true, new BigDecimal("500"));
        Lote prioritario = lote(c, 2, LocalDate.now().plusDays(2), true, new BigDecimal("500"));
        Lote posterior = lote(c, 3, LocalDate.now().plusDays(20), true, new BigDecimal("300"));
        PedidoResponseDTO criado = criar(c, "200", UnidadeMedida.ML);
        aprovar(c, criado, "200");
        saldo(vencido, "500");
        saldo(prioritario, "300");
        saldo(posterior, "300");
        assertEquals(EstadoRecipienteEstoque.ABERTO,
                recipientes.findByLoteIdOrderBySequencialAsc(prioritario.getId()).get(0).getEstado());
    }

    @Test
    void naoFracionavelRejeitaParcialSemAlterarSaldoOuPedido() {
        Cenario c = cenario(false);
        Lote l = lote(c, 1, null, false, new BigDecimal("500"), new BigDecimal("500"));
        PedidoResponseDTO criado = criar(c, "200", UnidadeMedida.ML);
        assertThrows(BusinessRuleException.class, () -> aprovar(c, criado, "200"));
        saldo(l, "1000");
        assertEquals(StatusPedido.PENDENTE, pedidos.buscarPorId(criado.getId()).getStatus());
        assertEquals(0, estoques.findById(c.estoque().getId()).orElseThrow()
                .getQuantidadeAtual().compareTo(new BigDecimal("1000")));
    }
}
