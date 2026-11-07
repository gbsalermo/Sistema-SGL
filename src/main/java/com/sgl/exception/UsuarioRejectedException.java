package com.sgl.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** An approval, refusal or profile change refused by the SGL; carries the machine-readable {@code codigo}. */
@Getter
public class UsuarioRejectedException extends RuntimeException {

    public static final String USUARIO_NAO_PENDENTE = "USUARIO_NAO_PENDENTE";
    public static final String ULTIMO_ADMINISTRADOR_GERAL = "ULTIMO_ADMINISTRADOR_GERAL";
    public static final String USUARIO_PENDENTE = "USUARIO_PENDENTE";

    private final HttpStatus status;
    private final String codigo;

    private UsuarioRejectedException(HttpStatus status, String codigo, String message) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public static UsuarioRejectedException naoPendente() {
        return new UsuarioRejectedException(HttpStatus.CONFLICT, USUARIO_NAO_PENDENTE,
                "Este usuário não está aguardando aprovação.");
    }

    /**
     * Trocar perfil ou laboratório de quem está PENDENTE_VINCULACAO não libera o acesso: só a aprovação
     * muda a situação para ATIVO. Recusar a troca evita que a gestão ache que já liberou a pessoa.
     */
    public static UsuarioRejectedException pendenteExigeAprovacao() {
        return new UsuarioRejectedException(HttpStatus.CONFLICT, USUARIO_PENDENTE,
                "Este usuário aguarda aprovação. Use Aprovar, na aba Pendentes, para definir perfil e laboratórios.");
    }

    public static UsuarioRejectedException ultimoAdministradorGeral() {
        return new UsuarioRejectedException(HttpStatus.CONFLICT, ULTIMO_ADMINISTRADOR_GERAL,
                "Não é possível remover o último Administrador geral ativo.");
    }
}
