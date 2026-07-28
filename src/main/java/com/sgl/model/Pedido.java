package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sgl.model.enums.StatusPedido;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	@ToString.Exclude
	private Usuario usuario;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "laboratorio_id", nullable = false)
	@ToString.Exclude
	private Laboratorio laboratorio;
	
	
	 @ManyToOne(fetch = FetchType.LAZY)
	 @JoinColumn(name = "projeto_id")
	 @ToString.Exclude 
	 private Projeto projeto;
	 
	
	@Column(nullable = false)
	private LocalDateTime dataSolicitacao;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusPedido status;
	
	private String observacao;
	
	private String arquivoDocumento;
	
	
	 @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	 @Builder.Default 
	 private List<ItemPedido> itens = new ArrayList<>();
	 
	
	
	
	

}
