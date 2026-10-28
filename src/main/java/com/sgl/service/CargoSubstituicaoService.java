package com.sgl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.CargoSubstituicaoDTO;
import com.sgl.dto.response.CargoSubstituicaoDTO.TipoCargo;
import com.sgl.model.Usuario;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Lista os cargos da unidade que ficaram "sem dono" (spec 5.7 e 5.8.5).
 *
 * <p>Não existe tabela de pendências: a lista é calculada a cada consulta. Um cargo entra
 * quando o ocupante está inativo, não está ATIVO no SGL (por exemplo, voltou a aguardar
 * aprovação) ou pertence a outra unidade. Assim,
 * quando o gestor troca o ocupante pela tela do cadastro, o item some sozinho.</p>
 */
@Service
@RequiredArgsConstructor
public class CargoSubstituicaoService {

    /** Resíduo nesses status saiu do fluxo; o gestor recebedor deixou de importar. */
    static final List<StatusResiduo> STATUS_RESIDUO_FINAIS =
            List.of(StatusResiduo.DESPACHADO, StatusResiduo.CANCELADO);

    private final LaboratorioRepository laboratorioRepository;
    private final VinculoEstagioRepository vinculoEstagioRepository;
    private final ResiduoRepository residuoRepository;

    @Transactional(readOnly = true)
    public List<CargoSubstituicaoDTO> listar() {
        if (!PerfilPolicy.isGestao(CurrentUser.get().perfil())) {
            throw new AccessDeniedException("Apenas gestão pode ver os cargos a substituir.");
        }
        // Sem Unidade em operação não há "unidade do gestor": falha fechado em vez de listar tudo.
        UUID unidadeId = CurrentUser.unidadeEmOperacao();
        SituacaoUsuario liberado = SituacaoUsuario.ATIVO;

        List<CargoSubstituicaoDTO> cargos = new ArrayList<>();

        // Orientador primeiro: os orientandos ficam sem operar até a troca (spec 5.8.5).
        vinculoEstagioRepository.findComOrientadorSemCondicoes(unidadeId, SituacaoEstagio.FINALIZADO, liberado)
                .forEach(v -> cargos.add(new CargoSubstituicaoDTO(
                        TipoCargo.ORIENTADOR_ESTAGIO, v.getPublicId(),
                        "Orientador de " + v.getEstagiario().getNome(),
                        v.getOrientador().getPublicId(), v.getOrientador().getNome())));

        laboratorioRepository.findComResponsavelSemCondicoes(unidadeId, liberado)
                .forEach(l -> cargos.add(new CargoSubstituicaoDTO(
                        TipoCargo.RESPONSAVEL_LABORATORIO, l.getPublicId(),
                        "Responsável pelo laboratório " + l.getNome(),
                        l.getResponsavel().getPublicId(), l.getResponsavel().getNome())));

        residuoRepository.findEmFluxoComGestorSemCondicoes(unidadeId, STATUS_RESIDUO_FINAIS, liberado)
                .forEach(r -> {
                    Usuario gestor = r.getGestorRecebedorInicial();
                    cargos.add(new CargoSubstituicaoDTO(
                            TipoCargo.GESTOR_RECEBEDOR_RESIDUO, r.getPublicId(),
                            "Gestor recebedor do resíduo " + r.getDescricao(),
                            gestor.getPublicId(), gestor.getNome()));
                });

        return cargos;
    }
}
