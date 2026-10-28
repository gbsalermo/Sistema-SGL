package com.sgl.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.model.Residuo;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;

class RotuloResiduoResponseDTOTest {

    @Test
    void apresentaDataEstadoEOrientacoesInformadasNaPrevia() {
        Residuo residuo = residuoBasico();
        when(residuo.getNivelRiscoInformado()).thenReturn(NivelRisco.BAIXO);
        when(residuo.getRiscosInformados()).thenReturn(Set.of(TipoRisco.PERIGO_AMBIENTAL));
        when(residuo.getMedidasSegurancaInformadas()).thenReturn(Set.of(MedidaSeguranca.LUVAS));
        when(residuo.getObservacaoSegurancaInformada()).thenReturn("Usar proteção adequada ao procedimento.");
        when(residuo.getObservacaoGerador()).thenReturn("Observação registrada pelo gerador.");

        RotuloResiduoResponseDTO rotulo = new RotuloResiduoResponseDTO(residuo);

        assertFalse(rotulo.isClassificacaoConfirmada());
        assertEquals(LocalDateTime.of(2026, 10, 28, 10, 30), rotulo.getDataInformacao());
        assertEquals(EstadoFisicoResiduo.LIQUIDO, rotulo.getEstadoFisico());
        assertEquals(Set.of(MedidaSeguranca.LUVAS), rotulo.getMedidasSeguranca());
        assertTrue(rotulo.getObservacoesRotulo().contains("Gerador:"));
        assertEquals(Set.of(TipoRisco.PERIGO_AMBIENTAL), rotulo.getRiscos());
    }

    @Test
    void nuncaReutilizaRiscoOuMedidaInformadosQuandoGestaoConfirmaVazio() {
        Residuo residuo = residuoBasico();
        when(residuo.getNivelRiscoInformado()).thenReturn(NivelRisco.ALTO);
        when(residuo.getNivelRiscoConfirmado()).thenReturn(NivelRisco.NENHUM);
        when(residuo.getRiscosInformados()).thenReturn(Set.of(TipoRisco.PERIGO_SAUDE));
        when(residuo.getRiscosConfirmados()).thenReturn(Set.of());
        when(residuo.getMedidasSegurancaInformadas()).thenReturn(Set.of(MedidaSeguranca.LUVAS));
        when(residuo.getMedidasSegurancaConfirmadas()).thenReturn(Set.of());

        RotuloResiduoResponseDTO rotulo = new RotuloResiduoResponseDTO(residuo);

        assertTrue(rotulo.isClassificacaoConfirmada());
        assertTrue(rotulo.getRiscos().isEmpty());
        assertTrue(rotulo.getMedidasSeguranca().isEmpty());
        assertEquals(NivelRisco.NENHUM, rotulo.getNivelRisco());
    }

    private Residuo residuoBasico() {
        Residuo residuo = mock(Residuo.class);
        Laboratorio laboratorio = mock(Laboratorio.class);
        when(laboratorio.getNome()).thenReturn("Laboratório de teste");
        when(residuo.getLaboratorio()).thenReturn(laboratorio);
        Usuario gerador = mock(Usuario.class);
        when(gerador.getNome()).thenReturn("Gerador de teste");
        when(residuo.getGerador()).thenReturn(gerador);
        when(residuo.getPublicId()).thenReturn(UUID.randomUUID());
        when(residuo.getUnidadeIdSnapshot()).thenReturn(UUID.randomUUID());
        when(residuo.getUnidadeNomeSnapshot()).thenReturn("Unidade de teste");
        when(residuo.getUnidadeSiglaSnapshot()).thenReturn("UT");
        when(residuo.getComponentes()).thenReturn(List.of());
        when(residuo.getDataInformacao()).thenReturn(LocalDateTime.of(2026, 10, 28, 10, 30));
        when(residuo.getEstadoFisico()).thenReturn(EstadoFisicoResiduo.LIQUIDO);
        return residuo;
    }
}
