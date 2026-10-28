package com.sgl.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Mudança de lotação: quem a Embrapa passou para outra Unidade perde o que tinha na antiga e volta a
 * aguardar aprovação na nova, como num primeiro login. Perfil, laboratório e vínculos operacionais
 * eram decisões da Unidade antiga e não valem na nova.
 *
 * <p>Exceção: o ADMINISTRADOR_GERAL é transversal e só acompanha a Unidade nova.</p>
 *
 * <p>Autoria (pedidos, resíduos, históricos) e os cargos que a pessoa ocupava (responsável de
 * laboratório, orientador, gestor recebedor) continuam apontando para ela; a Unidade antiga os vê em
 * "Cargos a substituir".</p>
 */
@Service
@RequiredArgsConstructor
public class MudancaLotacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(MudancaLotacaoService.class);

    private final VinculoOperacionalUsuarioRepository vinculoRepository;
    private final Clock clock;

    /**
     * Define a Unidade do usuário e, se ela mudou, aplica a regra. {@code perfilProvisorio} é a
     * sugestão pelo cargo, a mesma de um primeiro login: o perfil é obrigatório, mas um pendente não
     * opera nada até a aprovação confirmar o perfil.
     */
    public void aplicar(Usuario usuario, Unidade nova, Perfil perfilProvisorio) {
        Unidade atual = usuario.getUnidade();
        usuario.setUnidade(nova);
        if (atual == null || mesma(atual, nova) || usuario.getPerfil() == Perfil.ADMINISTRADOR_GERAL) {
            return;
        }

        usuario.setLaboratorio(null);
        usuario.setPerfil(perfilProvisorio);
        usuario.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);

        LocalDate hoje = LocalDate.now(clock);
        List<VinculoOperacionalUsuario> abertos = vinculoRepository.findByUsuarioAndEncerradoFalse(usuario);
        abertos.forEach(v -> {
            v.setEncerrado(true);
            if (v.getDataFim() == null || v.getDataFim().isAfter(hoje)) {
                v.setDataFim(hoje);
            }
        });
        vinculoRepository.saveAll(abertos);

        // Ids, not publicIds: the old Unidade is a lazy proxy and login runs outside a transaction.
        LOG.info("Mudança de lotação: usuário {} saiu da Unidade id={} para a id={}; volta a aguardar aprovação, {} vínculo(s) encerrado(s)",
                usuario.getPublicId(), atual.getId(), nova.getId(), abertos.size());
    }

    private static boolean mesma(Unidade a, Unidade b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }
}
