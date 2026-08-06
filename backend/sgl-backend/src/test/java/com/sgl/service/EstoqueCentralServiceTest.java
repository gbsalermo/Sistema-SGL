package com.sgl.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;

import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;

import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.model.enums.OrigemMovimentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class EstoqueCentralServiceTest {
	
	@Mock
	private EstoqueCentralRepository estoqueCentralRepository;
	
	@Mock
	private ProdutoRepository produtoRepository;
	
	@Mock
	private UnidadeRepository unidadeRepository;
	
	@Mock 
	private UsuarioRepository usuarioRepository;
	
	@Mock
	private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
	
	@InjectMocks
	private EstoqueCentralService estoqueCentralService;
	
	private Unidade unidade;
	private Produto produto;
	private Usuario usuario;
	private EstoqueCentral estoque;
	
	
	@BeforeEach
	void prepararCenario() {
		
		 unidade = Unidade.builder()
		            .id(1L)
		            .nome("Unidade Central")
		            .sigla("UC")
		            .build();

		    produto = Produto.builder()
		            .id(10L)
		            .nome("Álcool 70%")
		            .unidadeArmazenamento("Frasco de 1 L")
		            .ativo(true)
		            .build();

		    usuario = new Usuario();
		    usuario.setId(20L);
		    usuario.setNome("Usuário de Teste");
		    usuario.setAtivo(true);

		    estoque = EstoqueCentral.builder()
		            .id(30L)
		            .unidade(unidade)
		            .produto(produto)
		            .quantidadeAtual(10)
		            .quantidadeMinima(2)
		            .ativo(true)
		            .build();
	}
	
	private MovimentacaoEstoqueDTO criarMovimentacaoDTO(
	        Integer quantidade) {

	    MovimentacaoEstoqueDTO dto =
	            new MovimentacaoEstoqueDTO();

	    dto.setUsuarioId(20L);
	    dto.setQuantidadeMovimentada(quantidade);
	    dto.setOrigem(OrigemMovimentacao.COMPRA);
	    dto.setObservacao(
	            "Movimentação criada durante teste"
	    );

	    return dto;
	}
	
	@Test
	void deveAumentarSaldoAoRealizarEntrada() {

	    MovimentacaoEstoqueDTO dto =
	            criarMovimentacaoDTO(5);

	    when(estoqueCentralRepository.findById(30L))
	            .thenReturn(Optional.of(estoque));

	    when(usuarioRepository.findById(20L))
	            .thenReturn(Optional.of(usuario));

	    estoqueCentralService.entrada(30L, dto);

	    assertEquals(
	            15,
	            estoque.getQuantidadeAtual()
	    );

	    verify(estoqueCentralRepository)
	            .save(estoque);
	}
	
}


