package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Residuo;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ResiduoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioResiduosService {

    private final ResiduoRepository residuoRepository;
    private final LaboratorioRepository laboratorioRepository;

    @Transactional(readOnly = true)
    public RelatorioResiduosResponseDTO gerar(
            StatusResiduo status,
            UUID laboratorioId,
            NivelRisco nivelRisco,
            LocalDate dataInicio,
            LocalDate dataFim) {

        if (laboratorioId != null && !laboratorioRepository.existsByPublicId(laboratorioId)) {
            throw new ResourceNotFoundException("Laboratório", laboratorioId);
        }

        List<Residuo> filtrados = residuoRepository.findAllByOrderByDataInformacaoDesc().stream()
                .filter(residuo -> status == null || residuo.getStatus() == status)
                .filter(residuo -> laboratorioId == null
                        || residuo.getLaboratorio().getPublicId().equals(laboratorioId))
                .filter(residuo -> nivelRisco == null || riscoEfetivo(residuo) == nivelRisco)
                .filter(residuo -> dataInicio == null
                        || !residuo.getDataInformacao().toLocalDate().isBefore(dataInicio))
                .filter(residuo -> dataFim == null
                        || !residuo.getDataInformacao().toLocalDate().isAfter(dataFim))
                .toList();

        List<ResiduoResponseDTO> itens = filtrados.stream()
                .map(ResiduoResponseDTO::new)
                .toList();

        return new RelatorioResiduosResponseDTO(
                LocalDateTime.now(),
                itens.size(),
                contar(filtrados, StatusResiduo.INFORMADO),
                contar(filtrados, StatusResiduo.EM_ANALISE),
                contar(filtrados, StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO),
                contar(filtrados, StatusResiduo.ARMAZENADO_TEMPORARIAMENTE),
                contar(filtrados, StatusResiduo.DESPACHADO),
                (int) filtrados.stream().filter(residuo -> riscoEfetivo(residuo) == NivelRisco.ALTO).count(),
                itens
        );
    }

    private Integer contar(List<Residuo> residuos, StatusResiduo status) {
        return (int) residuos.stream().filter(residuo -> residuo.getStatus() == status).count();
    }

    private NivelRisco riscoEfetivo(Residuo residuo) {
        return residuo.getNivelRiscoConfirmado() != null
                ? residuo.getNivelRiscoConfirmado()
                : residuo.getNivelRiscoInformado();
    }
}
