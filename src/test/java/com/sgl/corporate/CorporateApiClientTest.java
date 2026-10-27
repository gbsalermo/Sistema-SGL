package com.sgl.corporate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateDtos.Empregado;

class CorporateApiClientTest {

    private MockRestServiceServer server;
    private CorporateApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CorporateApiClient(builder, new CorporateApiProperties("http://gw", "h", "sys", "pw", true));
    }

    @Test
    void login_enviaBasicAuthECorpo_eMapeiaUser() {
        String basic = "Basic " + Base64.getEncoder().encodeToString("sys:pw".getBytes());
        server.expect(requestTo("http://gw/autenticacaoapi-h/api/auth/login"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", basic))
            .andExpect(content().json("{\"username\":\"m344136\",\"password\":\"s\"}"))
            .andRespond(withSuccess("""
                {"user":{"id":"1","email":"a@b.br","otherMailbox":[],"login":"m344136","matricula":"344136",
                "nome":"Fulano","displayName":"F","lotacao":"CNPMF","sublotacao":"X","cargo":"Analista",
                "userAccountControl":512},"token":"t","expiresIn":3600,"createAt":"now"}
                """, MediaType.APPLICATION_JSON));

        CorporateLoginUser result = client.login("m344136", "s");

        assertThat(result.matricula()).isEqualTo("344136");
        assertThat(result.userAccountControl()).isEqualTo(512);
        server.verify();
    }

    @Test
    void login_401_lancaCredenciaisInvalidas() {
        server.expect(requestTo("http://gw/autenticacaoapi-h/api/auth/login"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.login("m1", "x")).isInstanceOf(InvalidCorporateCredentialsException.class);
    }

    @Test
    void login_503_lancaIndisponivel() {
        server.expect(requestTo("http://gw/autenticacaoapi-h/api/auth/login"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.login("m1", "x")).isInstanceOf(CorporateApiUnavailableException.class);
    }

    @Test
    void login_respostaNaoJson_lancaIndisponivel() {
        server.expect(requestTo("http://gw/autenticacaoapi-h/api/auth/login"))
            .andRespond(withSuccess("<html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.login("m1", "x")).isInstanceOf(CorporateApiUnavailableException.class);
    }

    @Test
    void empregadoPorMatricula_enviaMatriculaNumericaComZerosAEsquerda() {
        // Empregadoapi takes the numeric matrícula as the login answer returns it ("00344136"), never the AD login.
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00344136"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00344136"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00344136"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.empregadoPorMatricula("m344136");
        client.empregadoPorMatricula("344136");
        client.empregadoPorMatricula("00344136");
        server.verify();
    }

    @Test
    void empregadoPorMatricula_404_retornaVazio() {
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00000999"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.empregadoPorMatricula("999")).isEmpty();
    }

    @Test
    void empregadoPorCpf_400_retornaVazio() {
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/porCpf/abc"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("\"CPF inválido\""));

        assertThat(client.empregadoPorCpf("abc")).isEmpty();
    }

    @Test
    void empregadoPorMatricula_mapeiaCamposEIgnoraDesconhecidos() {
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00344136"))
            .andRespond(withSuccess("""
                {"matricula":"344136","nome":"Fulano","cpf":"123","email":"a@b.br","cargoDescricao":"Analista",
                "unidadeIdLotacao":7,"ativo":true,"cargoComissaoDescricao":"x","subLotacaoNome":"S","isSupervisorCti":false}
                """, MediaType.APPLICATION_JSON));

        Optional<Empregado> result = client.empregadoPorMatricula("344136");

        assertThat(result).isPresent();
        assertThat(result.get().unidadeIdLotacao()).isEqualTo(7);
        assertThat(result.get().subLotacaoNome()).isEqualTo("S");
    }

    @Test
    void empregadosDaUnidade_percorreTodasAsPaginas() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://gw/empregadoapi-h/empregado/listaEmpregadosPorUnidade")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("numeroPagina", "1"))
            .andRespond(withSuccess("""
                {"items":[{"matricula":"1","nome":"A"}],"pageIndex":1,"totalPages":2,"count":2,"hasPreviousPage":false,"hasNextPage":true}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://gw/empregadoapi-h/empregado/listaEmpregadosPorUnidade")))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("numeroPagina", "2"))
            .andRespond(withSuccess("""
                {"items":[{"matricula":"2","nome":"B"}],"pageIndex":2,"totalPages":2,"count":2,"hasPreviousPage":true,"hasNextPage":false}
                """, MediaType.APPLICATION_JSON));

        List<Empregado> result = client.empregadosDaUnidade(7);

        assertThat(result).extracting(Empregado::matricula).containsExactly("1", "2");
        server.verify();
    }

    @Test
    void empregadosPorNome_404_retornaListaVazia() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://gw/empregadoapi-h/empregado/listaEmpregadosPorNome")))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.empregadosPorNome("zzz")).isEmpty();
    }

    @Test
    void colaboradorPorCpf_mapeiaCamposComMaiuscula() {
        server.expect(requestTo("http://gw/colaboradorapi-h/colaborador/porCpf/111"))
            .andRespond(withSuccess("""
                {"ColaboradorId":10,"CPF":"111","Nome":"X","DataNascimento":"2000-01-01","UnidadeLotacaoId":3,
                "NomeUnidadeLotacao":"U","TipoVinculo":"Bolsista","UF":"DF","Ativo":true}
                """, MediaType.APPLICATION_JSON));

        Colaborador result = client.colaboradorPorCpf("111").orElseThrow();

        assertThat(result.colaboradorId()).isEqualTo(10L);
        assertThat(result.cpf()).isEqualTo("111");
        assertThat(result.ativo()).isTrue();
    }

    @Test
    void unidadesEmbrapa_mapeiaArrayJson() {
        server.expect(requestTo("http://gw/unidadeapi-h/unidade/listaUnidadesEmbrapa"))
            .andRespond(withSuccess("""
                [{"unidadeId":7,"unidadecd":206003,"sigla":"CNPMF","nome":"Mandioca"}]
                """, MediaType.APPLICATION_JSON));

        assertThat(client.unidadesEmbrapa()).singleElement()
            .satisfies(u -> assertThat(u.sigla()).isEqualTo("CNPMF"));
    }

    @Test
    void colaboradoresDaUnidade_percorreTodasAsPaginas() {
        String url = "http://gw/colaboradorapi-h/colaborador/lista/porUnidadeLotacao/3/ativo/true";
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(url)))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("numeroPagina", "1"))
            .andRespond(withSuccess("""
                {"items":[{"ColaboradorId":1,"CPF":"1","Nome":"A","Ativo":true}],"pageIndex":1,"totalPages":2,"count":2,"hasPreviousPage":false,"hasNextPage":true}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(url)))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam("numeroPagina", "2"))
            .andRespond(withSuccess("""
                {"items":[{"ColaboradorId":2,"CPF":"2","Nome":"B","Ativo":true}],"pageIndex":2,"totalPages":2,"count":2,"hasPreviousPage":true,"hasNextPage":false}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.colaboradoresDaUnidade(3)).extracting(Colaborador::colaboradorId).containsExactly(1L, 2L);
        server.verify();
    }

    @Test
    void consulta_5xx_lancaIndisponivel() {
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00000001"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.empregadoPorMatricula("1")).isInstanceOf(CorporateApiUnavailableException.class);
    }

    @Test
    void consulta_falhaDeConexao_lancaIndisponivel() {
        server.expect(requestTo("http://gw/empregadoapi-h/empregado/00000001"))
            .andRespond(request -> { throw new java.net.ConnectException("refused"); });

        assertThatThrownBy(() -> client.empregadoPorMatricula("1")).isInstanceOf(CorporateApiUnavailableException.class);
    }

    @Test
    void propriedadesAusentes_falhamComMensagemClara() {
        assertThatThrownBy(() -> CorporateApiConfig.validate(new CorporateApiProperties("http://gw", "h", "", "pw", true)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SGL_CORPORATE_USERNAME");
        assertThatThrownBy(() -> CorporateApiConfig.validate(new CorporateApiProperties(null, "h", "u", "pw", true)))
            .hasMessageContaining("SGL_CORPORATE_BASE_URL");
    }

    @Test
    void semCredenciaisEmModoNaoObrigatorio_validaEChamadasLancamIndisponivel() {
        CorporateApiProperties props = new CorporateApiProperties("http://gw", "h", "", "", false);
        CorporateApiConfig.validate(props);
        CorporateApiClient disabled = new CorporateApiClient(RestClient.builder(), props);

        assertThatThrownBy(() -> disabled.login("m1", "x")).isInstanceOf(CorporateApiUnavailableException.class);
        assertThatThrownBy(() -> disabled.empregadoPorMatricula("1")).isInstanceOf(CorporateApiUnavailableException.class);
    }
}
