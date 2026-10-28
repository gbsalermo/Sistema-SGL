package com.sgl.test;

import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;

/** Shared tweak for seeded example users so they can sign in through the DEV login. */
final class DevUsers {

    private DevUsers() {
    }

    /** The e-mail's local part ("gestor.ib@sgl.local" → "gestor.ib") is readable and already unique per seed. */
    static <T extends Usuario> T withDevLogin(T usuario) {
        String email = usuario.getEmail();
        usuario.setLogin(email.substring(0, email.indexOf('@')));
        usuario.setOrigem(OrigemUsuario.DEV);
        return usuario;
    }
}
