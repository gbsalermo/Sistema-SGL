package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table( name = "historico_laboratorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoLaboratorio implements Serializable {
	
	public static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "laboratorio_id", nullable = false)
	private Laboratorio laboratorio;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produto_id", nullable = false)
	private Produto produto;
	
	@Column(nullable = false)
	private Integer quantidade;
	
	@Column(nullable = false)
	private LocalDate dataRecebimento;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pedido_id", nullable = false)
	@ToString.Exclude
	private Pedido pedido;
	
	@Column(nullable = false)
	private Boolean ativo = true;

}
