package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lote", uniqueConstraints = {@UniqueConstraint( name = "uk_lote_estoque_numero", columnNames = {"estoque_centrals_id", "numero_lote"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lote implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	
	/*
	 * O lote pertence a um EstoqueCentral especifico.
	 * Através do EstoqueCentral já conseguimos descobrir Produto e Unidade
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "estoque_central_id", nullable = false)
	private EstoqueCentral estoqueCentral;
	
	@Column(name = "numero_lote", nullable = false, length = 100)
	private String numeroLote;
	
	//Quant. recebida originalmente, não deve diminuir após a entrada
	@Column(nullable = false)
	private Integer quantidadeInicial;
	
	//Quant. que ainda pode ser utilizada
	@Column(nullable = false)
	private Integer quantidadeDisponivel;
	
	@Column(nullable = false)
	private LocalDate dataEntrada;
	
	/*
	 * Pode ser nula para produtos que não tenham validade
	 * Para produto perecivel é obrigatoria
	 */
	private LocalDate dataValidade;
	
	@Column(nullable = false)
    private Boolean ativo = true;
}