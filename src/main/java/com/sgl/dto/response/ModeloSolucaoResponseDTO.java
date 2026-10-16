package com.sgl.dto.response;

import java.util.*;
import com.sgl.model.ModeloSolucao;
import lombok.*;

@Getter @NoArgsConstructor
public class ModeloSolucaoResponseDTO {
    private UUID id;
    private UUID unidadeId;
    private String nome;
    private String descricao;
    private String instrucoesPreparo;
    private Boolean ativo;
    private List<ComponenteModeloSolucaoResponseDTO> componentes;

    public ModeloSolucaoResponseDTO(ModeloSolucao m) {
        id = m.getPublicId();
        unidadeId = m.getUnidade().getPublicId();
        nome = m.getNome();
        descricao = m.getDescricao();
        instrucoesPreparo = m.getInstrucoesPreparo();
        ativo = m.getAtivo();
        componentes = m.getComponentes().stream()
            .sorted(Comparator.comparing(com.sgl.model.ComponenteModeloSolucao::getOrdem))
            .map(ComponenteModeloSolucaoResponseDTO::new).toList();
    }
}

