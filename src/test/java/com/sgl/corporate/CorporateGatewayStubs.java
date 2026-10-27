package com.sgl.corporate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

/** Canned gateway answers shaped like the real corporate APIs, for full-context integration tests. */
public class CorporateGatewayStubs {

    private final MockRestServiceServer server;
    private final String gateway;
    private final String ambiente;

    public CorporateGatewayStubs(MockRestServiceServer server, String baseUrl, String ambiente) {
        this.server = server;
        this.gateway = baseUrl;
        this.ambiente = ambiente;
    }

    public MockRestServiceServer server() {
        return server;
    }

    public void reset() {
        server.reset();
    }

    public void verify() {
        server.verify();
    }

    public void loginOk(String login, String matricula, int userAccountControl) {
        server.expect(requestTo(url("autenticacaoapi", "/api/auth/login")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("""
                {"user":{"login":"%s","matricula":"%s","nome":"Nome do Login","email":"login@embrapa.br",
                "cargo":"Cargo do Login","userAccountControl":%d},"token":"corporate-token"}
                """.formatted(login, matricula, userAccountControl), MediaType.APPLICATION_JSON));
    }

    public void loginStatus(HttpStatus status, int times) {
        server.expect(ExpectedCount.times(times), requestTo(url("autenticacaoapi", "/api/auth/login")))
            .andRespond(withStatus(status));
    }

    public void empregadoPorMatricula(String matricula, String nome, String cpf, String cargo,
                                      int unidadeId, boolean ativo) {
        // The resolver queries by the normalized key ("m344136"); the payload carries the plain number.
        server.expect(requestTo(url("empregadoapi", "/empregado/" + LoginNormalizer.normalize(matricula))))
            .andRespond(withSuccess(empregadoJson(matricula, nome, cpf, cargo, unidadeId, ativo),
                MediaType.APPLICATION_JSON));
    }

    public void empregadoPorCpf(String cpf, String matricula, String nome, String cargo, int unidadeId) {
        server.expect(requestTo(url("empregadoapi", "/empregado/porCpf/" + cpf)))
            .andRespond(withSuccess(empregadoJson(matricula, nome, cpf, cargo, unidadeId, true),
                MediaType.APPLICATION_JSON));
    }

    public void empregadoPorCpfNaoEncontrado(String cpf) {
        server.expect(requestTo(url("empregadoapi", "/empregado/porCpf/" + cpf)))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));
    }

    public void colaboradorPorCpf(String cpf, String nome, int unidadeId, boolean ativo) {
        server.expect(requestTo(url("colaboradorapi", "/colaborador/porCpf/" + cpf)))
            .andRespond(withSuccess("""
                {"ColaboradorId":42,"CPF":"%s","Nome":"%s","UnidadeLotacaoId":%d,
                "NomeUnidadeLotacao":"Setor do Colaborador","TipoVinculo":"Estagiário","Ativo":%b}
                """.formatted(cpf, nome, unidadeId, ativo), MediaType.APPLICATION_JSON));
    }

    public void colaboradorPorCpfNaoEncontrado(String cpf) {
        server.expect(requestTo(url("colaboradorapi", "/colaborador/porCpf/" + cpf)))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));
    }

    private static String empregadoJson(String matricula, String nome, String cpf, String cargo,
                                         int unidadeId, boolean ativo) {
        return """
            {"matricula":"%s","nome":"%s","cpf":"%s","email":"%s@embrapa.br","cargoDescricao":"%s",
            "unidadeIdLotacao":%d,"ativo":%b,"subLotacaoNome":"Sublotação %s"}
            """.formatted(matricula, nome, cpf, matricula, cargo, unidadeId, ativo, matricula);
    }

    private String url(String api, String path) {
        return gateway + "/" + api + "-" + ambiente + path;
    }
}
