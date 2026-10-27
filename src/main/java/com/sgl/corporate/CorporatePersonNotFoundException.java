package com.sgl.corporate;

/** The person authenticated but exists in neither corporate base. Message must never carry the CPF. */
public class CorporatePersonNotFoundException extends RuntimeException {

    public CorporatePersonNotFoundException() {
        super("Pessoa não encontrada nas bases corporativas");
    }
}
