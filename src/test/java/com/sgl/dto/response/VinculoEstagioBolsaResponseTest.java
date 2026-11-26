package com.sgl.dto.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.model.Bolsa;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.TipoBolsa;

class VinculoEstagioBolsaResponseTest {

    @Test
    void bolsaIndependenteUsaSnapshotMesmoAposRenomear() {
        UUID id = UUID.fromString("bb000000-0000-0000-0000-000000000001");
        Bolsa catalogo = Bolsa.builder()
                .publicId(id).nome("Bolsa FAPESB — nome novo").ativo(false).build();

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setBolsa(catalogo);
        vinculo.setBolsaNome("Bolsa FAPESB");

        VinculoEstagioResponseDTO response = new VinculoEstagioResponseDTO(vinculo);
        assertNull(response.getTipoBolsa());
        assertEquals(id, response.getBolsaId());
        assertEquals("Bolsa FAPESB", response.getBolsaNome());
    }

    @Test
    void vinculoLegadoSegueValidoSemBolsaDeCatalogo() {
        VinculoEstagio legado = new VinculoEstagio();
        legado.setTipoBolsa(TipoBolsa.BOLSA_CAPES);

        VinculoEstagioResponseDTO response = new VinculoEstagioResponseDTO(legado);
        assertEquals(TipoBolsa.BOLSA_CAPES, response.getTipoBolsa());
        assertNull(response.getBolsaId());
        assertNull(response.getBolsaNome());
    }
}
