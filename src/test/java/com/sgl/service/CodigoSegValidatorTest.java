package com.sgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;

class CodigoSegValidatorTest {

	private final CodigoSegValidator validator = new CodigoSegValidator();

	@Test
	void deveAceitarETrimarCodigoDeProjetoValido() {

		assertEquals(
				"10.25.00.085.00.00",
				validator.validarProjeto(" 10.25.00.085.00.00 ")
		);
	}

	@Test
	void deveRejeitarProjetoSemSufixoZeroZero() {

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> validator.validarProjeto("10.25.00.085.00.01")
		);

		assertEquals(
				"Código SEG de Projeto inválido. Formato esperado: XX.XX.XX.XXX.XX.00.",
				ex.getMessage()
		);
	}

	@Test
	void deveAceitarSciComMesmaRaizDoProjeto() {

		Projeto projeto = Projeto.builder()
				.codigoSeg("10.25.00.085.00.00")
				.build();

		assertEquals(
				"10.25.00.085.00.01",
				validator.validarSci("10.25.00.085.00.01", projeto)
		);
	}

	@Test
	void deveRejeitarSciComRaizDiferenteDoProjeto() {

		Projeto projeto = Projeto.builder()
				.codigoSeg("10.25.00.085.00.00")
				.build();

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> validator.validarSci("11.25.00.085.00.01", projeto)
		);

		assertEquals(
				"O Código SEG do SCI deve possuir a mesma raiz do Projeto.",
				ex.getMessage()
		);
	}

	@Test
	void deveRejeitarSciComSufixoZeroZero() {

		Projeto projeto = Projeto.builder()
				.codigoSeg("10.25.00.085.00.00")
				.build();

		assertThrows(
				BusinessRuleException.class,
				() -> validator.validarSci("10.25.00.085.00.00", projeto)
		);
	}

	@Test
	void deveAceitarAtividadeHerdandoCodigoCompletoDoSci() {

		Sci sci = Sci.builder()
				.codigoSeg("10.25.00.085.00.01")
				.build();

		assertEquals(
				"10.25.00.085.00.01.001",
				validator.validarAtividade(
						"10.25.00.085.00.01.001",
						sci
				)
		);
	}

	@Test
	void deveRejeitarAtividadeDeOutroSci() {

		Sci sci = Sci.builder()
				.codigoSeg("10.25.00.085.00.01")
				.build();

		BusinessRuleException ex = assertThrows(
				BusinessRuleException.class,
				() -> validator.validarAtividade(
						"10.25.00.085.00.02.001",
						sci
				)
		);

		assertEquals(
				"O Código SEG da Atividade deve herdar integralmente o Código SEG do SCI.",
				ex.getMessage()
		);
	}

	@Test
	void deveRejeitarAtividadeComSequencialZero() {

		Sci sci = Sci.builder()
				.codigoSeg("10.25.00.085.00.01")
				.build();

		assertThrows(
				BusinessRuleException.class,
				() -> validator.validarAtividade(
						"10.25.00.085.00.01.000",
						sci
				)
		);
	}
}
