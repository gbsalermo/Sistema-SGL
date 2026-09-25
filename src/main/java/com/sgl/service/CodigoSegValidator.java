package com.sgl.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;

@Component
public class CodigoSegValidator {

	private static final Pattern PADRAO_PROJETO = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}\\.00$");

	private static final Pattern PADRAO_SCI = Pattern
			.compile("^\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}\\.(?:0[1-9]|[1-9]\\d)$");

	private static final Pattern PADRAO_ATIVIDADE = Pattern.compile(
			"^\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}\\.\\d{2}\\.(?:0[1-9]|[1-9]\\d)\\.(?:00[1-9]|0[1-9]\\d|[1-9]\\d{2})$");

	public String validarProjeto(String codigoSeg) {

		if (codigoSeg == null || codigoSeg.isBlank()) {
			return null;
		}

		String codigo = codigoSeg.trim();

		if (!PADRAO_PROJETO.matcher(codigo).matches()) {
			throw new BusinessRuleException("Código SEG de Projeto inválido. Formato esperado: XX.XX.XX.XXX.XX.00.");
		}

		return codigo;
	}

	public String validarSci(String codigoSeg, Projeto projeto) {

		String codigo = exigirCodigo(codigoSeg, "O código SEG do SCI é obrigatório.");

		if (!PADRAO_SCI.matcher(codigo).matches()) {
			throw new BusinessRuleException(
					"Código SEG de SCI inválido. Formato esperado: XX.XX.XX.XXX.XX.SS, com SS entre 01 e 99.");
		}

		if (projeto == null || projeto.getCodigoSeg() == null || projeto.getCodigoSeg().isBlank()) {

			throw new BusinessRuleException("O Projeto deve possuir Código SEG antes de receber um SCI.");
		}

		String codigoProjeto = validarProjeto(projeto.getCodigoSeg());

		if (!raiz(codigo).equals(raiz(codigoProjeto))) {
			throw new BusinessRuleException("O Código SEG do SCI deve possuir a mesma raiz do Projeto.");
		}

		return codigo;
	}

	public String validarAtividade(String codigoSeg, Sci sci) {

		String codigo = exigirCodigo(codigoSeg, "O código SEG da Atividade é obrigatório.");

		if (!PADRAO_ATIVIDADE.matcher(codigo).matches()) {
			throw new BusinessRuleException(
					"Código SEG de Atividade inválido. Formato esperado: XX.XX.XX.XXX.XX.SS.AAA.");
		}

		if (sci == null || sci.getCodigoSeg() == null || sci.getCodigoSeg().isBlank()) {

			throw new BusinessRuleException("O SCI deve possuir Código SEG antes de receber uma Atividade.");
		}

		String codigoSci = sci.getCodigoSeg().trim();

		if (!PADRAO_SCI.matcher(codigoSci).matches()) {
			throw new BusinessRuleException("O SCI pai possui um Código SEG inválido.");
		}

		if (!codigo.startsWith(codigoSci + ".")) {
			throw new BusinessRuleException("O Código SEG da Atividade deve herdar integralmente o Código SEG do SCI.");
		}

		return codigo;
	}

	private String exigirCodigo(String codigoSeg, String mensagem) {

		if (codigoSeg == null || codigoSeg.isBlank()) {
			throw new BusinessRuleException(mensagem);
		}

		return codigoSeg.trim();
	}

	private String raiz(String codigoSeg) {

		int ultimoPonto = codigoSeg.lastIndexOf('.');

		return codigoSeg.substring(0, ultimoPonto);
	}
}