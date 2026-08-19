package com.sgl.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pedido_id", nullable = false)
	@ToString.Exclude
	private Pedido pedido;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produto_id", nullable = false)
	@ToString.Exclude
	private Produto produto;
	
	@Column(nullable = false)
	private Integer quantidadeSolicitada;
	private Integer quantidadeAprovada;

	@PrePersist
	private void gerarPublicId() {
		if(publicId == null) {
			publicId = UUID.randomUUID();
		}
	}
}
