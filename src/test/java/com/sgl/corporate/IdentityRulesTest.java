package com.sgl.corporate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClient;

import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateDtos.Empregado;
import com.sgl.model.enums.Perfil;

class IdentityRulesTest {

    /** In-memory stand-in: overrides the query methods so no HTTP is involved. */
    private static class FakeClient extends CorporateApiClient {
        private final Empregado empregado;
        private final Colaborador colaborador;

        FakeClient(Empregado empregado, Colaborador colaborador) {
            super(RestClient.builder(), new CorporateApiProperties("http://localhost", "h", "", "", false));
            this.empregado = empregado;
            this.colaborador = colaborador;
        }

        @Override
        public Optional<Empregado> empregadoPorMatricula(String matricula) {
            return Optional.ofNullable(empregado).filter(e -> e.matricula().equals(matricula));
        }

        @Override
        public Optional<Empregado> empregadoPorCpf(String cpf) {
            return Optional.ofNullable(empregado).filter(e -> cpf.equals(e.cpf().replaceAll("\\D", "")));
        }

        @Override
        public Optional<Colaborador> colaboradorPorCpf(String cpf) {
            return Optional.ofNullable(colaborador);
        }
    }

    @ParameterizedTest
    @CsvSource({"m344136,m344136", "M344136,m344136", "344136,m344136", "0344136,m344136",
        "' m344136 ',m344136", "123.456.789-09,12345678909", "admin,admin"})
    void normalize(String in, String out) {
        assertEquals(out, LoginNormalizer.normalize(in));
    }

    @Test
    void isCpf_requiresElevenDigits() {
        assertTrue(LoginNormalizer.isCpf("12345678909"));
        assertFalse(LoginNormalizer.isCpf("m344136"));
        assertFalse(LoginNormalizer.isCpf("344136"));
    }

    @Test
    void mask_mostraSoOsDoisUltimosDigitos() {
        assertEquals("***.***.***-09", CpfMasker.mask("12345678909"));
    }

    @Test
    void isDisabled_bit2() {
        assertTrue(CorporateIdentityResolver.isDisabled(514));
        assertFalse(CorporateIdentityResolver.isDisabled(512));
        assertFalse(CorporateIdentityResolver.isDisabled(null));
    }

    @Test
    void resolve_matricula_usaEmpregadoapi() {
        Empregado emp = new Empregado("m344136", "Fulano", "123.456.789-09", "f@embrapa.br",
            "Analista A", 7, true, "Setor X");
        CorporateLoginUser login = new CorporateLoginUser("fulano", "m344136", "Fulano", "login@embrapa.br",
            "L", "S", "Analista", 512);

        CorporateIdentity id = CorporateIdentityResolver.resolve(login, new FakeClient(emp, null));

        assertEquals(TipoPessoaCorporativa.EMPREGADO, id.tipo());
        assertEquals("m344136", id.matricula());
        assertEquals("12345678909", id.cpf());
        assertEquals(7, id.unidadeCorporativaId());
        assertEquals("Analista A", id.cargo());
        assertEquals("f@embrapa.br", id.email());
        assertTrue(id.ativo());
    }

    @Test
    void resolve_cpf_semEmpregado_usaColaborador() {
        Colaborador col = new Colaborador(5L, "123.456.789-09", "Beltrano", 9, "Unidade", "Bolsista", true);
        CorporateLoginUser login = new CorporateLoginUser("12345678909", "12345678909", "Beltrano",
            "b@x.br", null, null, "Bolsista", 512);

        CorporateIdentity id = CorporateIdentityResolver.resolve(login, new FakeClient(null, col));

        assertEquals(TipoPessoaCorporativa.COLABORADOR, id.tipo());
        assertNull(id.matricula());
        assertEquals("12345678909", id.cpf());
        assertEquals("b@x.br", id.email());
        assertEquals(9, id.unidadeCorporativaId());
    }

    @Test
    void resolve_naoEncontrado_lancaIndisponivel() {
        CorporateLoginUser login = new CorporateLoginUser("x", "m1", "X", null, null, null, null, 512);
        assertThrows(CorporatePersonNotFoundException.class,
            () -> CorporateIdentityResolver.resolve(login, new FakeClient(null, null)));
    }

    @Test
    void resolve_matriculaNula_usaLogin() {
        Empregado emp = new Empregado("m344136", "Fulano", "12345678909", null, "Analista", 7, true, null);
        CorporateLoginUser login = new CorporateLoginUser("m344136", null, "Fulano", "l@x.br", null, null, null, 512);
        CorporateIdentity id = CorporateIdentityResolver.resolve(login, new FakeClient(emp, null));
        assertEquals("m344136", id.matricula());
        assertEquals("l@x.br", id.email());
    }

    @Test
    void resolve_matriculaELoginEmBranco_lancaSemConsultar() {
        CorporateLoginUser login = new CorporateLoginUser(" ", "", "X", null, null, null, null, 512);
        assertThrows(CorporatePersonNotFoundException.class,
            () -> CorporateIdentityResolver.resolve(login, new FakeClient(null, null) {
                @Override
                public Optional<Empregado> empregadoPorMatricula(String m) {
                    throw new AssertionError("gateway must not be called");
                }
            }));
    }

    @Test
    void toString_naoExpoeCpf() {
        CorporateIdentity id = new CorporateIdentity(TipoPessoaCorporativa.EMPREGADO, "m1", "12345678909",
            "N", null, null, null, 1, true);
        assertFalse(id.toString().contains("12345678909"));
        assertTrue(id.toString().contains("***.***.***-09"));
    }

    @Test
    void mask_casosBorda() {
        assertNull(CpfMasker.mask(null));
        assertEquals("***.***.***-09", CpfMasker.mask("123.456.789-09"));
        assertEquals("***", CpfMasker.mask("123"));
    }

    @Test
    void normalize_zeroENulo() {
        assertEquals("m0", LoginNormalizer.normalize("0"));
        assertNull(LoginNormalizer.normalize(null));
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL", "Motorista", "bolsista", "Analista"}, nullValues = "NULL")
    void fromCargo_colaboradorSempreEstagiario(String cargo) {
        assertEquals(Optional.of(Perfil.ESTAGIARIO),
            ProfileSuggestion.fromCargo(cargo, TipoPessoaCorporativa.COLABORADOR));
    }

    @ParameterizedTest
    @CsvSource({"Pesquisador,PESQUISADOR", "analista,ANALISTA", "Técnico,TECNICO", "assistente,TECNICO",
        "estagiário,ESTAGIARIO", "bolsista,ESTAGIARIO"})
    void fromCargo(String cargo, Perfil esperado) {
        assertEquals(Optional.of(esperado), ProfileSuggestion.fromCargo(cargo, TipoPessoaCorporativa.EMPREGADO));
    }

    @Test
    void fromCargo_desconhecido_vazio() {
        assertEquals(Optional.empty(), ProfileSuggestion.fromCargo("Motorista", TipoPessoaCorporativa.EMPREGADO));
        assertEquals(Optional.empty(), ProfileSuggestion.fromCargo(null, TipoPessoaCorporativa.EMPREGADO));
    }
}
