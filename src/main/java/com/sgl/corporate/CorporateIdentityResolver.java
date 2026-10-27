package com.sgl.corporate;

import java.util.Optional;

import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateDtos.Empregado;

/** Looks a freshly authenticated user up in the corporate bases (empregado first, then colaborador). */
public final class CorporateIdentityResolver {

    private static final int ACCOUNT_DISABLED_BIT = 0x2;

    private CorporateIdentityResolver() {
    }

    public static boolean isDisabled(Integer userAccountControl) {
        return userAccountControl != null && (userAccountControl & ACCOUNT_DISABLED_BIT) != 0;
    }

    public static CorporateIdentity resolve(CorporateLoginUser login, CorporateApiClient client) {
        String raw = isBlank(login.matricula()) ? login.login() : login.matricula();
        if (isBlank(raw)) {
            throw new CorporatePersonNotFoundException();
        }
        String key = LoginNormalizer.normalize(raw);
        if (!LoginNormalizer.isCpf(key)) {
            return client.empregadoPorMatricula(key)
                .map(e -> fromEmpregado(e, login))
                .orElseThrow(CorporateIdentityResolver::notFound);
        }
        Optional<Empregado> empregado = client.empregadoPorCpf(key);
        if (empregado.isPresent()) {
            return fromEmpregado(empregado.get(), login);
        }
        return client.colaboradorPorCpf(key)
            .map(c -> fromColaborador(c, key, login))
            .orElseThrow(CorporateIdentityResolver::notFound);
    }

    private static CorporateIdentity fromEmpregado(Empregado e, CorporateLoginUser login) {
        return new CorporateIdentity(TipoPessoaCorporativa.EMPREGADO, e.matricula(), digits(e.cpf()),
            e.nome(), firstNonBlank(e.email(), login.email()), e.cargoDescricao(), e.subLotacaoNome(),
            e.unidadeIdLotacao(), Boolean.TRUE.equals(e.ativo()));
    }

    private static CorporateIdentity fromColaborador(Colaborador c, String loginCpf, CorporateLoginUser login) {
        String cpf = c.cpf() != null ? digits(c.cpf()) : loginCpf;
        return new CorporateIdentity(TipoPessoaCorporativa.COLABORADOR, null, cpf, c.nome(), login.email(),
            c.tipoVinculo(), c.nomeUnidadeLotacao(), c.unidadeLotacaoId(), Boolean.TRUE.equals(c.ativo()));
    }

    private static String digits(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static CorporatePersonNotFoundException notFound() {
        return new CorporatePersonNotFoundException();
    }
}
