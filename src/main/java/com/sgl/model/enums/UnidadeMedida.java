package com.sgl.model.enums;

import java.math.BigDecimal;

public enum UnidadeMedida {

    ML(DimensaoMedida.VOLUME, GrupoConversaoMedida.VOLUME, "1"),
    L(DimensaoMedida.VOLUME, GrupoConversaoMedida.VOLUME, "1000"),

    MG(DimensaoMedida.MASSA, GrupoConversaoMedida.MASSA, "1"),
    G(DimensaoMedida.MASSA, GrupoConversaoMedida.MASSA, "1000"),
    KG(DimensaoMedida.MASSA, GrupoConversaoMedida.MASSA, "1000000"),

    METRO(DimensaoMedida.COMPRIMENTO, GrupoConversaoMedida.COMPRIMENTO, "1"),

    UNIDADE(DimensaoMedida.CONTAGEM, GrupoConversaoMedida.UNIDADE, "1"),
    REACAO(DimensaoMedida.CONTAGEM, GrupoConversaoMedida.REACAO, "1");

    private final DimensaoMedida dimensao;
    private final GrupoConversaoMedida grupoConversao;
    private final BigDecimal fatorParaUnidadeBase;

    UnidadeMedida(
            DimensaoMedida dimensao,
            GrupoConversaoMedida grupoConversao,
            String fatorParaUnidadeBase) {
        this.dimensao = dimensao;
        this.grupoConversao = grupoConversao;
        this.fatorParaUnidadeBase = new BigDecimal(fatorParaUnidadeBase);
    }

    public DimensaoMedida getDimensao() {
        return dimensao;
    }

    public GrupoConversaoMedida getGrupoConversao() {
        return grupoConversao;
    }

    public BigDecimal getFatorParaUnidadeBase() {
        return fatorParaUnidadeBase;
    }

    public boolean compativelCom(UnidadeMedida outra) {
        return outra != null && grupoConversao == outra.grupoConversao;
    }

    public boolean ehUnidadeBaseDoGrupo() {
        return fatorParaUnidadeBase.compareTo(BigDecimal.ONE) == 0;
    }
}
