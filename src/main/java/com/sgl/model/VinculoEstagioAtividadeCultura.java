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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vinculo_estagio_atividade_cultura", uniqueConstraints = {
		@UniqueConstraint(name = "uk_vinculo_atividade_cultura", columnNames = { "vinculo_estagio_atividade_id",
				"cultura_id" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VinculoEstagioAtividadeCultura implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vinculo_estagio_atividade_id", nullable = false)
	@ToString.Exclude
	private VinculoEstagioAtividade participacao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cultura_id", nullable = false)
	@ToString.Exclude
	private Cultura cultura;

	@PrePersist
	private void generatePublicId() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}
	}
}