package com.sgl.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.model.ModalidadeBolsa;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.TipoBolsa;

class VinculoEstagioModalidadeBolsaResponseTest {

    @Test
    void exposicaoMantemNomeHistoricoMesmoAposRenomearCatalogo() {
        UUID id = UUID.fromString("bb000000-0000-0000-0000-000000000001");
        ModalidadeBolsa catalogo = ModalidadeBolsa.builder()
                .publicId(id)
                .nome("Bolsa FAPESB — nome novo")
                .tipoBase(TipoBolsa.BOLSA_INSTITUCIONAL)
                .ativo(false)
                .build();

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_INSTITUCIONAL);
        vinculo.setModalidadeBolsa(catalogo);
        vinculo.setModalidadeBolsaNome("Bolsa FAPESB");

        VinculoEstagioResponseDTO response = new VinculoEstagioResponseDTO(vinculo);

        assertEquals(TipoBolsa.BOLSA_INSTITUCIONAL, response.getTipoBolsa());
        assertEquals(id, response.getModalidadeBolsaId());
        assertEquals("Bolsa FAPESB", response.getModalidadeBolsaNome());
    }

    @Test
    void vinculoLegadoSegueValidoSemModalidadeCustomizada() {
        VinculoEstagio legado = new VinculoEstagio();
        legado.setTipoBolsa(TipoBolsa.BOLSA_CAPES);

        VinculoEstagioResponseDTO response = new VinculoEstagioResponseDTO(legado);
        assertEquals(TipoBolsa.BOLSA_CAPES, response.getTipoBolsa());
        assertNull(response.getModalidadeBolsaId());
        assertNull(response.getModalidadeBolsaNome());
    }
}
