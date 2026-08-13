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
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;

@ExtendWith(MockitoExtension.class)
class HistoricoLaboratorioServiceTest {

    @Mock
    private HistoricoLaboratorioRepository historicoLaboratorioRepository;

    @Mock
    private LaboratorioRepository laboratorioRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

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
                .nome("Unidade Central")
                .sigla("UC")
                .build();

        laboratorio = Laboratorio.builder()
                .id(2L)
                .nome("Laboratório A")
                .unidade(unidade)
                .ativo(true)
                .build();

        projeto = Projeto.builder()
                .id(3L)
                .nome("Projeto 1")
                .laboratorio(laboratorio)
                .ativo(true)
                .build();

        produto = Produto.builder()
                .id(4L)
                .nome("Produto de teste")
                .unidadeArmazenamento("caixa")
                .ativo(true)
                .build();

        pedido = Pedido.builder()
                .id(5L)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .build();

        historico = HistoricoLaboratorio.builder()
                .id(6L)
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

        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(3L)).thenReturn(Optional.of(projeto));
        when(historicoLaboratorioRepository.findByLaboratorioProjetoEPeriodo(
                2L,
                3L,
                inicio,
                fim
        )).thenReturn(List.of(historico));

        List<HistoricoLaboratorioDTO> resultado =
                historicoLaboratorioService.listarPorProjetoEPeriodo(
                        2L,
                        3L,
                        inicio,
                        fim
                );

        assertEquals(1, resultado.size());
        assertEquals(6L, resultado.get(0).getId());
        assertEquals(5L, resultado.get(0).getPedidoId());
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
                .laboratorio(laboratorio)
                .build();

        HistoricoLaboratorio primeiroRecebimento = HistoricoLaboratorio.builder()
                .id(8L)
                .laboratorio(laboratorio)
                .produto(produto)
                .quantidade(6)
                .dataRecebimento(LocalDate.of(2026, 6, 10))
                .pedido(pedido)
                .ativo(true)
                .build();

        HistoricoLaboratorio segundoRecebimento = HistoricoLaboratorio.builder()
                .id(9L)
                .laboratorio(laboratorio)
                .produto(produto)
                .quantidade(10)
                .dataRecebimento(LocalDate.of(2026, 8, 5))
                .pedido(segundoPedido)
                .ativo(true)
                .build();

        when(laboratorioRepository.findById(2L)).thenReturn(Optional.of(laboratorio));
        when(produtoRepository.findById(4L)).thenReturn(Optional.of(produto));
        when(historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                2L,
                4L,
                inicio,
                fim
        )).thenReturn(List.of(primeiroRecebimento, segundoRecebimento));

        ConsumoProdutoLaboratorioDTO resultado =
                historicoLaboratorioService.calcularConsumoProduto(
                        2L,
                        4L,
                        inicio,
                        fim
                );

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

        when(laboratorioRepository.findById(2L)).thenReturn(Optional.of(laboratorio));
        when(produtoRepository.findById(4L)).thenReturn(Optional.of(produto));
        when(historicoLaboratorioRepository.findByLaboratorioProdutoEPeriodo(
                2L,
                4L,
                inicio,
                fim
        )).thenReturn(List.of());

        ConsumoProdutoLaboratorioDTO resultado =
                historicoLaboratorioService.calcularConsumoProduto(
                        2L,
                        4L,
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
                .nome("Outro laboratório")
                .ativo(true)
                .build();

        projeto.setLaboratorio(outroLaboratorio);

        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(3L)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> historicoLaboratorioService.listarPorProjetoEPeriodo(
                        2L,
                        3L,
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
        when(laboratorioRepository.existsById(2L)).thenReturn(true);
        when(projetoRepository.findById(3L)).thenReturn(Optional.of(projeto));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> historicoLaboratorioService.listarPorProjetoEPeriodo(
                        2L,
                        3L,
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
