package com.sgl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.model.Usuario;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deactivates in the SGL someone the corporate base reports as inactive or unknown.
 *
 * The caller (e.g. EstagiarioService.criar) refuses the request right afterwards, which rolls its own
 * transaction back. REQUIRES_NEW commits the deactivation independently so it survives that rollback;
 * it lives in its own bean because the annotation only takes effect through the Spring proxy, never
 * on a self-invocation.
 */
@Service
@RequiredArgsConstructor
public class DesativacaoCorporativaService {

    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");
    private static final String SYSTEM_ACTOR = "sistema";

    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void desativar(Usuario usuario) {
        // Reloaded here: the given instance belongs to the caller's persistence context, not this transaction's.
        usuarioRepository.findById(usuario.getId())
                .filter(atual -> Boolean.TRUE.equals(atual.getAtivo()))
                .ifPresent(atual -> {
                    atual.setAtivo(false);
                    usuarioRepository.save(atual);
                    AUDIT.info("acao=USUARIO_DESATIVADO_PELA_BASE_CORPORATIVA ator={} alvo={}",
                            SYSTEM_ACTOR, atual.getPublicId());
                });
    }
}
