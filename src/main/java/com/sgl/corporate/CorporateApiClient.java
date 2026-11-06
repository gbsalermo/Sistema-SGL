package com.sgl.corporate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateDtos.Empregado;
import com.sgl.corporate.CorporateDtos.LoginResponse;
import com.sgl.corporate.CorporateDtos.Page;
import com.sgl.corporate.CorporateDtos.UnidadeCorporativa;

/**
 * Read-only client for the corporate gateway. Never touches the
 * /empregado/dadossensiveis/* endpoints. Deliberately non-final so tests can
 * substitute a subclass for the query methods.
 */
public class CorporateApiClient {

    private static final int PAGE_SIZE = 100;
    // colaboradorapi answers 400 "Parâmetros de paginação inválidos!" above 50 per page.
    private static final int COLABORADOR_PAGE_SIZE = 50;
    private static final int NAME_SEARCH_PAGE_SIZE = 50;
    // Guards against a misbehaving gateway that always reports hasNextPage.
    private static final int MAX_PAGES = 1000;

    private static final ParameterizedTypeReference<Page<Empregado>> EMPREGADO_PAGE =
        new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Page<Colaborador>> COLABORADOR_PAGE =
        new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<UnidadeCorporativa>> UNIDADES =
        new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final String ambiente;
    // Without credentials (dev/demo) every call fails as unavailable instead of hitting the gateway.
    private final boolean disabled;

    public CorporateApiClient(RestClient.Builder builder, CorporateApiProperties properties) {
        this.ambiente = properties.ambiente();
        this.disabled = isBlank(properties.username()) || isBlank(properties.password());
        builder.baseUrl(properties.baseUrl());
        if (!disabled) {
            builder.defaultHeaders(headers -> headers.setBasicAuth(properties.username(), properties.password()));
        }
        this.restClient = builder.build();
    }

    /** False in dev/demo without system credentials, where every call would fail as unavailable. */
    public boolean isEnabled() {
        return !disabled;
    }

    public CorporateLoginUser login(String username, String password) {
        ensureEnabled();
        LoginResponse response;
        try {
            response = restClient.post()
                .uri(path("autenticacaoapi", "/api/auth/login"))
                .body(Map.of("username", username, "password", password))
                .retrieve()
                .body(LoginResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new InvalidCorporateCredentialsException("Credenciais corporativas inválidas");
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
        if (response == null || response.user() == null) {
            throw unavailable(null);
        }
        return response.user();
    }

    public Optional<Empregado> empregadoPorMatricula(String matricula) {
        // Callers hold the AD login ("m344136") or a bare number; normalized here so no call site can get it wrong.
        return Optional.ofNullable(get(path("empregadoapi", "/empregado/{m}"), Empregado.class,
            LoginNormalizer.matriculaCorporativa(matricula)));
    }

    public Optional<Empregado> empregadoPorCpf(String cpf) {
        return Optional.ofNullable(get(path("empregadoapi", "/empregado/porCpf/{c}"), Empregado.class, cpf));
    }

    public List<Empregado> empregadosDaUnidade(int unidadeId) {
        return allPages(page -> get(
            path("empregadoapi", "/empregado/listaEmpregadosPorUnidade?unidadeId={u}&numeroPagina={p}&itensPorPagina={s}"),
            EMPREGADO_PAGE, unidadeId, page, PAGE_SIZE));
    }

    public List<Empregado> empregadosPorNome(String nome) {
        Page<Empregado> page = get(
            path("empregadoapi", "/empregado/listaEmpregadosPorNome?nome={n}&somenteAtivos=true&numeroPagina={p}&itensPorPagina={s}"),
            EMPREGADO_PAGE, nome, 1, NAME_SEARCH_PAGE_SIZE);
        return itemsOf(page);
    }

    public Optional<Colaborador> colaboradorPorCpf(String cpf) {
        return Optional.ofNullable(get(path("colaboradorapi", "/colaborador/porCpf/{c}"), Colaborador.class, cpf));
    }

    public Optional<Colaborador> colaboradorPorId(long id) {
        return Optional.ofNullable(get(path("colaboradorapi", "/colaborador/porId/{i}"), Colaborador.class, id));
    }

    public List<Colaborador> colaboradoresDaUnidade(int unidadeId) {
        return allPages(page -> get(
            path("colaboradorapi", "/colaborador/lista/porUnidadeLotacao/{u}/ativo/true?numeroPagina={p}&itensPorPagina={s}"),
            COLABORADOR_PAGE, unidadeId, page, COLABORADOR_PAGE_SIZE));
    }

    public List<Colaborador> colaboradoresPorNome(String nome) {
        Page<Colaborador> page = get(
            path("colaboradorapi", "/colaborador/lista/porNome?numeroPagina={p}&itensPorPagina={s}&nome={n}"),
            COLABORADOR_PAGE, 1, NAME_SEARCH_PAGE_SIZE, nome);
        return itemsOf(page);
    }

    public List<UnidadeCorporativa> unidadesEmbrapa() {
        List<UnidadeCorporativa> unidades = get(path("unidadeapi", "/unidade/listaUnidadesEmbrapa"), UNIDADES);
        return unidades == null ? List.of() : unidades;
    }

    private String path(String api, String path) {
        return "/" + api + "-" + ambiente + path;
    }

    private <T> List<T> allPages(java.util.function.IntFunction<Page<T>> fetch) {
        List<T> all = new ArrayList<>();
        for (int pageNumber = 1; pageNumber <= MAX_PAGES; pageNumber++) {
            Page<T> page = fetch.apply(pageNumber);
            all.addAll(itemsOf(page));
            if (page == null || !page.hasNextPage()) {
                break;
            }
        }
        return all;
    }

    private static <T> List<T> itemsOf(Page<T> page) {
        return page == null || page.items() == null ? List.of() : page.items();
    }

    private <T> T get(String uriTemplate, Class<T> type, Object... vars) {
        return execute(() -> restClient.get().uri(uriTemplate, vars).retrieve().body(type));
    }

    private <T> T get(String uriTemplate, ParameterizedTypeReference<T> type, Object... vars) {
        return execute(() -> restClient.get().uri(uriTemplate, vars).retrieve().body(type));
    }

    // 400/404 mean "no such record" for this gateway (e.g. malformed CPF, empty search),
    // so they become null; everything else unexpected is an availability problem.
    private <T> T execute(java.util.function.Supplier<T> call) {
        ensureEnabled();
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == HttpStatus.NOT_FOUND.value() || status == HttpStatus.BAD_REQUEST.value()) {
                return null;
            }
            throw unavailable(e);
        } catch (RestClientException e) {
            throw unavailable(e);
        }
    }

    private void ensureEnabled() {
        if (disabled) {
            throw new CorporateApiUnavailableException("Autenticação corporativa desabilitada: credenciais do sistema ausentes", null);
        }
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static CorporateApiUnavailableException unavailable(Throwable cause) {
        return new CorporateApiUnavailableException("APIs corporativas indisponíveis", cause);
    }
}
