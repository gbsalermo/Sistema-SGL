package com.sgl.model;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locais_armazenamento_residuo", uniqueConstraints = {
		@UniqueConstraint(
				name = "uk_locais_armazenamento_residuo_unidade_nome",
				columnNames = {"unidade_id", "nome"}
				)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalArmazenamentoResiduo implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(
	name = "public_id",
	nullable = false,
	unique = true,
	updatable = false)
	private UUID publicId;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "unidade_id", nullable = false)
	private Unidade unidade;
	
	@Column(nullable = false, length = 150)
	private String nome;
	
	@Column(nullable = false)
	@Builder.Default
	private Boolean ativo = true;
	
	public void validateActive() {
		if(!Boolean.TRUE.equals(ativo)) {
			
			throw new BusinessRuleException(
					"O local de armazenamento informado está inativo."
					);
		}
	}
	
	@PrePersist
	private void generateDefaults() {
		
		if(publicId == null) {
			publicId = UUID.randomUUID();
		}
		
		if(ativo == null) {
			ativo = true;
		}
	}
	

}
