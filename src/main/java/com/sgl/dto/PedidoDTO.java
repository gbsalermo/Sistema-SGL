package com.sgl.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Pedido;
import com.sgl.model.enums.StatusPedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

	private UUID id;
	
	@NotNull(message = "Id do usuario é obrigatorio")
	private UUID usuarioId;
	
	private String usuarioNome;
	
	@NotNull(message = "Id do laboratório é obrigatório")
	private UUID laboratorioId;
	
	private String laboratorioNome;
	
	private UUID projetoId;
	
	private String projetoNome;
	
	private LocalDateTime dataSolicitacao;
	
	//@NotNull(message = "Status é obrigatório") - Verificar a necessidade de separar um dto para criação e outro de pedido pronto
	private StatusPedido status;
	
	private String observacao;
	
	private String arquivoDocumento;
	
	@Valid
	@NotEmpty(message = "Pedido deve ter pelo menos 1 item")
	private List<ItemPedidoDTO> itens;
	
	public PedidoDTO(Pedido entity) {
		
		this.id = entity.getPublicId();
		this.usuarioId = entity.getUsuario().getPublicId();
		this.usuarioNome = entity.getUsuario().getNome();
		this.laboratorioId = entity.getLaboratorio().getPublicId();
		this.laboratorioNome = entity.getLaboratorio().getNome();
		this.projetoId = entity.getProjeto() != null ? entity.getProjeto().getPublicId() : null;
		this.projetoNome = entity.getProjeto() != null ? entity.getProjeto().getNome() : null;
		this.dataSolicitacao = entity.getDataSolicitacao();
		this.status = entity.getStatus();
		this.observacao = entity.getObservacao();
		this.arquivoDocumento = entity.getArquivoDocumento();
		this.itens = entity.getItens().stream()
				.map(ItemPedidoDTO::new)
				.toList();
		
		
	}
}
