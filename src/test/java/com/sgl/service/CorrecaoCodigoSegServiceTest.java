package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sgl.dto.request.CorrecaoCodigoSegRequestDTO;
import com.sgl.dto.response.HistoricoCorrecaoCodigoSegResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.codigoseg.HistoricoCorrecaoCodigoSeg;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.codigoseg.HistoricoCorrecaoCodigoSegRepository;
import com.sgl.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class CorrecaoCodigoSegServiceTest {

	private static final UUID UNIDADE_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000601");
	private static final UUID PROJETO_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000602");
	private static final UUID SCI_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000603");
	private static final UUID ATIVIDADE_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000604");
	private static final UUID USUARIO_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000605");

	@Mock
	private ProjetoRepository projetoRepository;

	@Mock
	private SciRepository sciRepository;

	@Mock
	private AtividadeRepository atividadeRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private HistoricoCorrecaoCodigoSegRepository historicoRepository;

	@Spy
	private CodigoSegValidator codigoSegValidator = new CodigoSegValidator();

	@InjectMocks
	private CorrecaoCodigoSegService service;

	private Projeto projeto;
	private Sci sci;
	private Atividade atividade;
	private Usuario usuario;

	@BeforeEach
	void setUp() {

		Unidade unidade = Unidade.builder()
				.id(1L)
				.publicId(UNIDADE_ID)
				.nome("Unidade Teste")
				.sigla("UT")
				.build();

		Laboratorio laboratorio = Laboratorio.builder()
				.id(10L)
				.publicId(UUID.randomUUID())
				.unidade(unidade)
				.nome("Laboratório Teste")
				.ativo(true)
				.build();

		projeto = Projeto.builder()
				.id(20L)
				.publicId(PROJETO_ID)
				.laboratorio(laboratorio)
				.nome("Projeto")
				.codigoSeg("95.95.95.001.01.00")
				.status(StatusProjeto.ATIVO)
				.situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
				.ativo(true)
				.build();

		sci = Sci.builder()
				.id(30L)
				.publicId(SCI_ID)
				.projeto(projeto)
				.codigoSeg("95.95.95.001.01.01")
				.nome("SCI")
				.status(StatusProjeto.ATIVO)
				.situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
				.ativo(true)
				.build();

		atividade = Atividade.builder()
				.id(40L)
				.publicId(ATIVIDADE_ID)
				.sci(sci)
				.codigoSeg("95.95.95.001.01.01.001")
				.nome("Atividade")
				.status(StatusProjeto.ATIVO)
				.situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
				.ativo(true)
				.build();

		usuario = new Usuario(
				50L,
				USUARIO_ID,
				"Gestor Teste",
				"gestor.seg@teste.local",
				"senha",
				Perfil.GESTOR,
				unidade,
				laboratorio,
				true
		);
	}

	@AfterEach
	void tearDown() {
		TenantContext.limpar();
	}

	private CorrecaoCodigoSegRequestDTO request(String novoCodigo) {

		return new CorrecaoCodigoSegRequestDTO(
				USUARIO_ID,
				novoCodigo,
				" Correção de erro de digitação "
		);
	}

	private void mockUsuario() {

		when(usuarioRepository.findByPublicIdAndUnidadePublicId(
				USUARIO_ID,
				UNIDADE_ID
		)).thenReturn(Optional.of(usuario));
	}

	@SuppressWarnings("unchecked")
	private void mockHistoricoSaveAll() {

		when(historicoRepository.saveAll(any()))
				.thenAnswer(invocation -> {

					List<HistoricoCorrecaoCodigoSeg> historicos =
							invocation.getArgument(0);

					for (HistoricoCorrecaoCodigoSeg historico : historicos) {
						historico.setPublicId(UUID.randomUUID());
						historico.setDataHora(LocalDateTime.now());
					}

					return historicos;
				});
	}

	@Test
	void deveCorrigirProjetoECascatearParaSciEAtividade() {

		TenantContext.definir(UNIDADE_ID);

		when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
				PROJETO_ID, UNIDADE_ID))
				.thenReturn(Optional.of(projeto));

		mockUsuario();

		when(sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
				PROJETO_ID, UNIDADE_ID))
				.thenReturn(List.of(sci));

		when(atividadeRepository
				.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(
						PROJETO_ID, UNIDADE_ID))
				.thenReturn(List.of(atividade));

		mockHistoricoSaveAll();

		List<HistoricoCorrecaoCodigoSegResponseDTO> resultado =
				service.corrigirProjeto(
						PROJETO_ID,
						request("96.96.96.001.01.00")
				);

		assertEquals("96.96.96.001.01.00", projeto.getCodigoSeg());
		assertEquals("96.96.96.001.01.01", sci.getCodigoSeg());
		assertEquals("96.96.96.001.01.01.001", atividade.getCodigoSeg());
		assertEquals(3, resultado.size());
		assertEquals("Correção de erro de digitação", resultado.get(0).getJustificativa());

		verify(projetoRepository).save(projeto);
		verify(sciRepository).saveAll(List.of(sci));
		verify(atividadeRepository).saveAll(List.of(atividade));
		verify(historicoRepository).saveAll(any());
	}

	@Test
	void deveCorrigirSciECascatearParaAtividade() {

		TenantContext.definir(UNIDADE_ID);

		when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
				SCI_ID, UNIDADE_ID))
				.thenReturn(Optional.of(sci));

		mockUsuario();

		when(atividadeRepository.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
				SCI_ID, UNIDADE_ID))
				.thenReturn(List.of(atividade));

		mockHistoricoSaveAll();

		List<HistoricoCorrecaoCodigoSegResponseDTO> resultado =
				service.corrigirSci(
						SCI_ID,
						request("95.95.95.001.01.02")
				);

		assertEquals("95.95.95.001.01.02", sci.getCodigoSeg());
		assertEquals("95.95.95.001.01.02.001", atividade.getCodigoSeg());
		assertEquals(2, resultado.size());

		verify(sciRepository).save(sci);
		verify(atividadeRepository).saveAll(List.of(atividade));
	}

	@Test
	void deveCorrigirSomenteAtividade() {

		TenantContext.definir(UNIDADE_ID);

		when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
				ATIVIDADE_ID, UNIDADE_ID))
				.thenReturn(Optional.of(atividade));

		mockUsuario();

		when(historicoRepository.save(any(HistoricoCorrecaoCodigoSeg.class)))
				.thenAnswer(invocation -> {
					HistoricoCorrecaoCodigoSeg historico = invocation.getArgument(0);
					historico.setPublicId(UUID.randomUUID());
					historico.setDataHora(LocalDateTime.now());
					return historico;
				});

		List<HistoricoCorrecaoCodigoSegResponseDTO> resultado =
				service.corrigirAtividade(
						ATIVIDADE_ID,
						request("95.95.95.001.01.01.002")
				);

		assertEquals("95.95.95.001.01.01.002", atividade.getCodigoSeg());
		assertEquals(1, resultado.size());

		verify(atividadeRepository).save(atividade);
		verify(historicoRepository).save(any(HistoricoCorrecaoCodigoSeg.class));
	}

	@Test
	void deveBloquearColisaoGlobalAoCorrigirSci() {

		TenantContext.definir(UNIDADE_ID);

		when(sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
				SCI_ID, UNIDADE_ID))
				.thenReturn(Optional.of(sci));

		mockUsuario();

		when(sciRepository.existsByCodigoSegAndPublicIdNot(
				"95.95.95.001.01.02",
				SCI_ID
		)).thenReturn(true);

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> service.corrigirSci(
						SCI_ID,
						request("95.95.95.001.01.02")
				)
		);

		assertEquals(
				"Já existe um SCI com este Código SEG.",
				ex.getMessage()
		);

		verify(sciRepository, never()).save(any());
		verify(historicoRepository, never()).saveAll(any());
	}

	@Test
	void deveRejeitarCorrecaoDeProjetoSemCodigoInicial() {

		projeto.setCodigoSeg(null);

		TenantContext.definir(UNIDADE_ID);

		when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
				PROJETO_ID, UNIDADE_ID))
				.thenReturn(Optional.of(projeto));

		mockUsuario();

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> service.corrigirProjeto(
						PROJETO_ID,
						request("96.96.96.001.01.00")
				)
		);

		assertEquals(
				"O Projeto ainda não possui Código SEG. A primeira definição deve ser realizada pelo fluxo comum de atualização.",
				ex.getMessage()
		);
	}

	@Test
	void deveRejeitarUsuarioSemPerfilDeGestao() {

		usuario.setPerfil(Perfil.PESQUISADOR);

		TenantContext.definir(UNIDADE_ID);

		when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
				ATIVIDADE_ID, UNIDADE_ID))
				.thenReturn(Optional.of(atividade));

		mockUsuario();

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> service.corrigirAtividade(
						ATIVIDADE_ID,
						request("95.95.95.001.01.01.002")
				)
		);

		assertEquals(
				"A correção de Código SEG exige perfil GESTOR ou ADMINISTRADOR.",
				ex.getMessage()
		);
	}

	@Test
	void devePermitirCorrecaoDeAtividadeInativa() {

		atividade.setAtivo(false);
		atividade.setStatus(StatusProjeto.CONCLUIDO);

		TenantContext.definir(UNIDADE_ID);

		when(atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
				ATIVIDADE_ID, UNIDADE_ID))
				.thenReturn(Optional.of(atividade));

		mockUsuario();

		when(historicoRepository.save(any(HistoricoCorrecaoCodigoSeg.class)))
				.thenAnswer(invocation -> {
					HistoricoCorrecaoCodigoSeg historico = invocation.getArgument(0);
					historico.setPublicId(UUID.randomUUID());
					historico.setDataHora(LocalDateTime.now());
					return historico;
				});

		service.corrigirAtividade(
				ATIVIDADE_ID,
				request("95.95.95.001.01.01.002")
		);

		assertEquals(
				"95.95.95.001.01.01.002",
				atividade.getCodigoSeg()
		);
	}

	@Test
	void deveRejeitarCorrecaoSemTenant() {

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> service.corrigirProjeto(
						PROJETO_ID,
						request("96.96.96.001.01.00")
				)
		);

		assertEquals(
				"Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.",
				ex.getMessage()
		);
	}

	@Test
	void deveBloquearTodaCorrecaoDoProjetoSeDescendenteColidir() {

		TenantContext.definir(UNIDADE_ID);

		when(projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(
				PROJETO_ID, UNIDADE_ID))
				.thenReturn(Optional.of(projeto));

		mockUsuario();

		when(sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
				PROJETO_ID, UNIDADE_ID))
				.thenReturn(List.of(sci));

		when(atividadeRepository
				.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(
						PROJETO_ID, UNIDADE_ID))
				.thenReturn(List.of(atividade));

		when(sciRepository.existsByCodigoSegAndPublicIdNot(
				"96.96.96.001.01.01",
				SCI_ID
		)).thenReturn(true);

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> service.corrigirProjeto(
						PROJETO_ID,
						request("96.96.96.001.01.00")
				)
		);

		assertEquals(
				"Já existe um SCI com este Código SEG.",
				ex.getMessage()
		);

		assertEquals("95.95.95.001.01.00", projeto.getCodigoSeg());
		assertEquals("95.95.95.001.01.01", sci.getCodigoSeg());
		assertEquals("95.95.95.001.01.01.001", atividade.getCodigoSeg());

		verify(projetoRepository, never()).save(any());
		verify(sciRepository, never()).saveAll(any());
		verify(atividadeRepository, never()).saveAll(any());
		verify(historicoRepository, never()).saveAll(any());
	}

}
