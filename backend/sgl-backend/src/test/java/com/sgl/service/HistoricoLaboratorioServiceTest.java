package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.ConsumoProdutoLaboratorioDTO;
import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.HistoricoLaboratorio;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.repository.HistoricoLaboratorioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;

@ExtendWith(MockitoExtension.class)
class HistoricoLaboratorioServiceTest {

    private static final UUID UNIDADE_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID LABORATORIO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROJETO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRODUTO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PEDIDO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID HISTORICO_PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private HistoricoLaboratorioRepository historicoLaboratorioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private HistoricoLaboratorioService historicoLaboratorioService;

    private Laboratorio laboratorio;
    private Projeto projeto;
    private Produto produto;
    private Pedido pedido;
    private HistoricoLaboratorio historico;

    @BeforeEach
    void prepararCenario() {
        Unidade unidade = Unidade.builder()
                .id(1L)
                .publicId(UNIDADE_PUBLIC_ID)
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .publicId(LABORATORIO_PUBLIC_ID)
                .nome("Laboratório A")
                .unidade(unidade)
                .ativo(true)
                .build();

        projeto = Projeto.builder()
                .id(3L)
                .publicId(PROJETO_PUBLIC_ID)
                .nome("Projeto 1")
                .laboratorio(laboratorio)
                .ativo(true)
                .build();

        produto = Produto.builder()
                .id(4L)
                .publicId(PRODUTO_PUBLIC_ID)
                .nome("Produto de teste")
                .unidadeArmazenamento("caixa")
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(5L)
                .publicId(PEDIDO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .build();

        historico = HistoricoLaboratorio.builder()
                .id(6L)
                .publicId(HISTORICO_PUBLIC_ID)
                .laboratorio(laboratorio)
                .produto(produto)
                .quantidade(7)
                .dataRecebimento(LocalDate.of(2026, 6, 15))
                .pedido(pedido)
                .ativo(true)
                .build();
    }

    @Test
    void deveListarMateriaisRecebidosPorProjetoNoLaboratorioEPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 6, 1);
        LocalDate fim = LocalDate.of(2026, 6, 30);

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));
        when(historicoLaboratorioRepository.findByLaboratorioProjetoEPeriodo(
                2L,
                3L,
                inicio,
                fim
        )).thenReturn(List.of(historico));

        List<HistoricoLaboratorioDTO> resultado =
                historicoLaboratorioService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
                        inicio,
                        fim
                );

        assertEquals(1, resultado.size());
        assertEquals(HISTORICO_PUBLIC_ID, resultado.get(0).getId());
        assertEquals(PEDIDO_PUBLIC_ID, resultado.get(0).getPedidoId());
        assertEquals(7, resultado.get(0).getQuantidade());

        verify(historicoLaboratorioRepository).findByLaboratorioProjetoEPeriodo(
                2L,
                3L,
                inicio,
                fim
        );
    }

    @Test
    void deveCalcularConsumoMedioDeProdutoPorLaboratorio() {
        LocalDate inicio = LocalDate.of(2026, 6, 1);
        LocalDate fim = LocalDate.of(2026, 8, 31);

        Pedido segundoPedido = Pedido.builder()
                .id(7L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000007"))
                .laboratorio(laboratorio)
                .build();

        HistoricoLaboratorio primeiroRecebimento = HistoricoLaboratorio.builder()
                .id(8L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000008"))
                .laboratorio(laboratorio)
                .produto(produto)
                .quantidade(6)
                .dataRecebimento(LocalDate.of(2026, 6, 10))
                .pedido(pedido)
                .ativo(true)
                .build();

        HistoricoLaboratorio segundoRecebimento = HistoricoLaboratorio.builder()
                .id(9L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000009"))
                .laboratorio(laboratorio)
                .produto(produto)
                .quantidade(10)
                .dataRecebimento(LocalDate.of(2026, 8, 5))
                .pedido(segundoPedido)
                .ativo(true)
                .build();

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                2L,
                4L,
                inicio,
                fim
        )).thenReturn(List.of(primeiroRecebimento, segundoRecebimento));

        ConsumoProdutoLaboratorioDTO resultado =
                historicoLaboratorioService.calcularConsumoProduto(
                        LABORATORIO_PUBLIC_ID,
                        PRODUTO_PUBLIC_ID,
                        inicio,
                        fim
                );

        assertEquals(LABORATORIO_PUBLIC_ID, resultado.getLaboratorioId());
        assertEquals(PRODUTO_PUBLIC_ID, resultado.getProdutoId());
        assertEquals(2L, resultado.getQuantidadePedidos());
        assertEquals(16, resultado.getQuantidadeTotalRecebida());
        assertEquals(new BigDecimal("8.00"), resultado.getMediaQuantidadePorPedido());
        assertEquals(3, resultado.getMesesConsiderados());
        assertEquals(new BigDecimal("5.33"), resultado.getMediaConsumoMensal());
        assertEquals(6, resultado.getQuantidadeMinimaSugerida());
    }

    @Test
    void deveRetornarMediasZeroQuandoNaoHaConsumoNoPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 6, 1);
        LocalDate fim = LocalDate.of(2026, 6, 30);

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(produtoRepository.findByPublicId(PRODUTO_PUBLIC_ID)).thenReturn(Optional.of(produto));
        when(historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                2L,
                4L,
                inicio,
                fim
        )).thenReturn(List.of());

        ConsumoProdutoLaboratorioDTO resultado =
                historicoLaboratorioService.calcularConsumoProduto(
                        LABORATORIO_PUBLIC_ID,
                        PRODUTO_PUBLIC_ID,
                        inicio,
                        fim
                );

        assertEquals(0L, resultado.getQuantidadePedidos());
        assertEquals(0, resultado.getQuantidadeTotalRecebida());
        assertEquals(new BigDecimal("0.00"), resultado.getMediaQuantidadePorPedido());
        assertEquals(new BigDecimal("0.00"), resultado.getMediaConsumoMensal());
        assertEquals(0, resultado.getQuantidadeMinimaSugerida());
    }

    @Test
    void deveImpedirHistoricoQuandoProjetoNaoPertenceAoLaboratorio() {
        Laboratorio outroLaboratorio = Laboratorio.builder()
                .id(99L)
                .publicId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
                .nome("Outro laboratório")
                .ativo(true)
                .build();

        projeto.setLaboratorio(outroLaboratorio);

        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> historicoLaboratorioService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30)
                )
        );

        assertEquals(
                "O projeto informado não pertence ao laboratório informado.",
                exception.getMessage()
        );
        verify(historicoLaboratorioRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }

    @Test
    void deveImpedirHistoricoComPeriodoInvertido() {
        when(laboratorioRepository.findByPublicId(LABORATORIO_PUBLIC_ID)).thenReturn(Optional.of(laboratorio));
        when(projetoRepository.findByPublicId(PROJETO_PUBLIC_ID)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> historicoLaboratorioService.listarPorProjetoEPeriodo(
                        LABORATORIO_PUBLIC_ID,
                        PROJETO_PUBLIC_ID,
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 6, 1)
                )
        );

        assertEquals(
                "A data inicial não pode ser posterior à data final.",
                exception.getMessage()
        );
        verify(historicoLaboratorioRepository, never())
                .findByLaboratorioProjetoEPeriodo(any(), any(), any(), any());
    }
}
