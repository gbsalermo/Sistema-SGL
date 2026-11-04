package com.sgl.corporate;

import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateDtos.Empregado;

/** Looks a freshly authenticated user up in the corporate bases (empregado first, then colaborador). */
public final class CorporateIdentityResolver {

    private static final Logger log = LoggerFactory.getLogger(CorporateIdentityResolver.class);
    private static final int ACCOUNT_DISABLED_BIT = 0x2;

    private CorporateIdentityResolver() {
    }

    public static boolean isDisabled(Integer userAccountControl) {
        return userAccountControl != null && (userAccountControl & ACCOUNT_DISABLED_BIT) != 0;
    }

    public static CorporateIdentity resolve(CorporateLoginUser login, CorporateApiClient client) {
        return resolve(login, null, client);
    }

    /**
     * Identifica a pessoa que acabou de passar no AD.
     *
     * <p>{@code loginDigitado} é o que a pessoa digitou na tela (já validado pelo AD junto com a senha).
     * Ele importa porque colaboradores (estagiários, bolsistas) só são encontráveis por CPF, e a resposta
     * do AD nem sempre traz o CPF em {@code login}/{@code matricula}. Antes desta correção o CPF digitado
     * era descartado e o estagiário caía em "cadastro não encontrado".</p>
     *
     * <p>Ordem: (1) matrícula do AD na base de empregados; (2) qualquer CPF disponível — digitado primeiro —
     * na base de empregados e depois na de colaboradores.</p>
     */
    public static CorporateIdentity resolve(CorporateLoginUser login, String loginDigitado, CorporateApiClient client) {
        // Chave "clássica": a matrícula do AD, ou o login do AD quando a matrícula vem vazia.
        String raw = isBlank(login.matricula()) ? login.login() : login.matricula();
        String key = isBlank(raw) ? null : LoginNormalizer.normalize(raw);

        // 1) Empregado pela matrícula: é o caminho de quem entra com "m344136".
        if (key != null && !LoginNormalizer.isCpf(key)) {
            Optional<Empregado> porMatricula = client.empregadoPorMatricula(key);
            if (porMatricula.isPresent()) {
                return fromEmpregado(porMatricula.get(), login);
            }
        }

        // 2) Por CPF. O digitado vem primeiro porque o AD acabou de confirmá-lo com a senha.
        Optional<String> cpf = Stream.of(loginDigitado, login.login(), login.matricula())
            .filter(v -> !isBlank(v))
            .map(LoginNormalizer::normalize)
            .filter(LoginNormalizer::isCpf)
            .findFirst();
        if (cpf.isPresent()) {
            Optional<Empregado> empregado = client.empregadoPorCpf(cpf.get());
            if (empregado.isPresent()) {
                return fromEmpregado(empregado.get(), login);
            }
            Optional<Colaborador> colaborador = client.colaboradorPorCpf(cpf.get());
            if (colaborador.isPresent()) {
                return fromColaborador(colaborador.get(), cpf.get(), login);
            }
        }

        // Registra só o FORMATO dos campos (nunca os valores: CPF é dado pessoal) para o suporte
        // saber, pelo log, se o problema é formato inesperado do AD ou pessoa ausente das bases.
        log.warn("Pessoa não encontrada nas bases corporativas: digitado={}, ad.login={}, ad.matricula={}, cpfConsultado={}",
            formato(loginDigitado), formato(login.login()), formato(login.matricula()), cpf.isPresent());
        throw notFound();
    }

    /** Classifica um valor sem revelá-lo: VAZIO, CPF, MATRICULA ou OUTRO. */
    static String formato(String valor) {
        if (isBlank(valor)) {
            return "VAZIO";
        }
        String normalizado = LoginNormalizer.normalize(valor);
        if (LoginNormalizer.isCpf(normalizado)) {
            return "CPF";
        }
        return normalizado.matches("m\\d+") ? "MATRICULA" : "OUTRO";
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
