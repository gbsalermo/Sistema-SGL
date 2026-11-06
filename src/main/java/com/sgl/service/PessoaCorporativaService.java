package com.sgl.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sgl.corporate.CorporateApiClient;
import com.sgl.corporate.CorporateDtos.Colaborador;
import com.sgl.corporate.CorporateDtos.Empregado;
import com.sgl.corporate.CpfMasker;
import com.sgl.corporate.LoginNormalizer;
import com.sgl.corporate.ProfileSuggestion;
import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.dto.request.ImportarPessoaRequestDTO;
import com.sgl.dto.response.PessoaCorporativaResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ImportacaoRejectedException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;

/**
 * Search and import of people from the corporate bases, always scoped to the Unidade in operation.
 *
 * Deliberately not @Transactional: it waits on the corporate gateway and should not hold a DB
 * connection meanwhile; the single save is atomic on its own.
 */
@Service
public class PessoaCorporativaService {

    private static final Logger LOG = LoggerFactory.getLogger(PessoaCorporativaService.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");
    static final int MIN_TERM_LENGTH = 3;
    static final int MAX_RESULTS = 50;
    static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final CorporateApiClient corporateApiClient;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final Clock clock;

    // The whole Unidade listing is expensive and the API cannot filter by name, so it is cached per
    // base and Unidade. Rows keep the CPF only in memory, to cross-check "já cadastrado".
    private final Map<String, CachedListing> cache = new ConcurrentHashMap<>();

    public PessoaCorporativaService(CorporateApiClient corporateApiClient, UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository, LaboratorioRepository laboratorioRepository, Clock clock) {
        this.corporateApiClient = corporateApiClient;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.laboratorioRepository = laboratorioRepository;
        this.clock = clock;
    }

    private record Person(String referencia, TipoPessoaCorporativa tipo, String nome, String matricula,
            String cpf, String email, String cargo, String sublotacao, String unidadeNome) {
    }

    private record CachedListing(Instant loadedAt, List<Person> people) {
    }

    /** Test hook and manual invalidation. */
    public void clearCache() {
        cache.clear();
    }

    public List<PessoaCorporativaResponseDTO> buscar(TipoPessoaCorporativa tipo, String termo) {
        requireGestao(CurrentUser.get());
        String term = termo == null ? "" : termo.trim();
        if (term.length() < MIN_TERM_LENGTH) {
            throw new BusinessRuleException("Informe ao menos " + MIN_TERM_LENGTH + " caracteres para buscar.");
        }
        Unidade unidade = unidadeEmOperacao();
        int codigo = codigoCorporativo(unidade);

        List<Person> found;
        if (tipo == TipoPessoaCorporativa.EMPREGADO && term.matches("\\d{4,8}")) {
            found = corporateApiClient.empregadoPorMatricula(LoginNormalizer.normalize(term))
                    .filter(e -> isActiveInUnit(e, codigo))
                    .map(e -> toPerson(e, unidade))
                    .map(List::of).orElse(List.of());
        } else {
            String needle = fold(term);
            found = listing(tipo, codigo, unidade).stream()
                    .filter(p -> fold(p.nome()).contains(needle))
                    .limit(MAX_RESULTS)
                    .toList();
        }
        return toResponses(found);
    }

    public UsuarioSessaoResponseDTO importar(ImportarPessoaRequestDTO request) {
        AuthenticatedUser actor = CurrentUser.get();
        requireGestao(actor);
        if (!PerfilPolicy.podeConceder(actor.perfil(), request.getPerfil())) {
            throw new AccessDeniedException("Seu perfil não pode conceder o perfil " + request.getPerfil() + ".");
        }
        Unidade unidade = unidadeEmOperacao();
        int codigo = codigoCorporativo(unidade);
        Laboratorio laboratorio = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(request.getLaboratorioBaseId(), unidade.getPublicId())
                .orElseThrow(() -> new BusinessRuleException("O laboratório-base deve pertencer à Unidade em operação."));
        laboratorio.validateActive();

        // Fetched again by the opaque reference: the browser never held (or sends) the CPF.
        Person person = fetch(request.getTipo(), request.getReferencia(), codigo, unidade);
        if (alreadyRegistered(person)) {
            throw ImportacaoRejectedException.jaCadastrado();
        }

        Usuario usuario = new Usuario();
        usuario.setNome(person.nome());
        usuario.setMatricula(person.matricula());
        usuario.setCpf(person.cpf());
        usuario.setEmail(availableEmail(person));
        usuario.setCargo(person.cargo());
        usuario.setSublotacao(person.sublotacao());
        usuario.setUnidade(unidade);
        // Set directly, bypassing definirLaboratorioBase's ESTAGIARIO guard on purpose: the import sets the
        // laboratory list for every profile (spec 2.1); the estagio flow overrides it for interns later.
        usuario.substituirLaboratorios(List.of(laboratorio));
        usuario.setPerfil(request.getPerfil());
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        usuario.setOrigem(OrigemUsuario.CORPORATIVO);
        usuario.setAtivo(true);

        Usuario saved;
        try {
            saved = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Constraint name only: the exception message may carry the CPF value.
            LOG.warn("Import refused by a uniqueness constraint: constraint={}", constraintName(e));
            // Lost a race against another import or a first login of the same person.
            throw ImportacaoRejectedException.jaCadastrado();
        }
        AUDIT.info("acao=USUARIO_IMPORTADO ator={} alvo={} perfil={} unidade={} cpf={}",
                actor.publicId(), saved.getPublicId(), saved.getPerfil(), unidade.getPublicId(),
                CpfMasker.mask(person.cpf()));

        return usuarioRepository.findWithUnidadeAndLaboratoriosByPublicId(saved.getPublicId())
                .map(UsuarioSessaoResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", saved.getPublicId()));
    }

    private static String constraintName(DataIntegrityViolationException e) {
        return e.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                && cve.getConstraintName() != null ? cve.getConstraintName() : "unknown";
    }

    private Person fetch(TipoPessoaCorporativa tipo, String referencia, int codigo, Unidade unidade) {
        String reference = referencia.trim();
        if (tipo == TipoPessoaCorporativa.EMPREGADO) {
            Empregado empregado = corporateApiClient.empregadoPorMatricula(LoginNormalizer.normalize(reference))
                    .orElseThrow(PessoaCorporativaService::notFound);
            requireImportable(empregado.ativo(), empregado.unidadeIdLotacao(), codigo);
            return toPerson(empregado, unidade);
        }
        long id;
        try {
            id = Long.parseLong(reference);
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Referência de colaborador inválida.");
        }
        Colaborador colaborador = corporateApiClient.colaboradorPorId(id)
                .orElseThrow(PessoaCorporativaService::notFound);
        requireImportable(colaborador.ativo(), colaborador.unidadeLotacaoId(), codigo);
        return toPerson(colaborador);
    }

    // The unit check is what keeps a manager from importing someone else's people by guessing references.
    private static void requireImportable(Boolean ativo, Integer unidadeCorporativa, int codigo) {
        if (unidadeCorporativa == null || unidadeCorporativa != codigo) {
            throw new AccessDeniedException("A pessoa não pertence à Unidade em operação.");
        }
        if (Boolean.FALSE.equals(ativo)) {
            throw new BusinessRuleException("A pessoa está inativa nas bases corporativas.");
        }
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Pessoa não encontrada nas bases corporativas.");
    }

    private boolean alreadyRegistered(Person person) {
        if (person.matricula() != null && usuarioRepository.findByMatricula(person.matricula()).isPresent()) {
            return true;
        }
        return person.cpf() != null && usuarioRepository.findByCpf(person.cpf()).isPresent();
    }

    // Email is unique; a taken one is left empty (it is filled at first login) instead of failing the import.
    private String availableEmail(Person person) {
        if (person.email() == null || person.email().isBlank() || usuarioRepository.existsByEmail(person.email())) {
            return null;
        }
        return person.email();
    }

    private List<Person> listing(TipoPessoaCorporativa tipo, int codigo, Unidade unidade) {
        String key = tipo + ":" + codigo;
        Instant now = clock.instant();
        CachedListing cached = cache.get(key);
        if (cached != null && now.isBefore(cached.loadedAt().plus(CACHE_TTL))) {
            return cached.people();
        }
        List<Person> people = tipo == TipoPessoaCorporativa.EMPREGADO
                ? corporateApiClient.empregadosDaUnidade(codigo).stream()
                        .filter(e -> isActiveInUnit(e, codigo)).map(e -> toPerson(e, unidade)).toList()
                : corporateApiClient.colaboradoresDaUnidade(codigo).stream()
                        .filter(c -> !Boolean.FALSE.equals(c.ativo())).map(PessoaCorporativaService::toPerson).toList();
        cache.put(key, new CachedListing(now, people));
        return people;
    }

    private static boolean isActiveInUnit(Empregado e, int codigo) {
        return !Boolean.FALSE.equals(e.ativo()) && e.unidadeIdLotacao() != null && e.unidadeIdLotacao() == codigo;
    }

    private static Person toPerson(Empregado e, Unidade unidade) {
        return new Person(e.matricula(), TipoPessoaCorporativa.EMPREGADO, e.nome(), e.matricula(), digits(e.cpf()),
                e.email(), e.cargoDescricao(), e.subLotacaoNome(), unidade.getNome());
    }

    private static Person toPerson(Colaborador c) {
        return new Person(String.valueOf(c.colaboradorId()), TipoPessoaCorporativa.COLABORADOR, c.nome(), null,
                digits(c.cpf()), null, c.tipoVinculo(), null, c.nomeUnidadeLotacao());
    }

    private List<PessoaCorporativaResponseDTO> toResponses(List<Person> people) {
        Set<String> matriculas = new HashSet<>();
        Set<String> cpfs = new HashSet<>();
        for (Person p : people) {
            if (p.matricula() != null) {
                matriculas.add(p.matricula());
            }
            if (p.cpf() != null) {
                cpfs.add(p.cpf());
            }
        }
        Map<String, Usuario> byMatricula = new HashMap<>();
        Map<String, Usuario> byCpf = new HashMap<>();
        if (!matriculas.isEmpty()) {
            usuarioRepository.findByMatriculaIn(matriculas).forEach(u -> byMatricula.put(u.getMatricula(), u));
        }
        if (!cpfs.isEmpty()) {
            usuarioRepository.findByCpfIn(cpfs).forEach(u -> byCpf.put(u.getCpf(), u));
        }
        return people.stream().map(p -> {
            Usuario existing = Optional.ofNullable(p.matricula()).map(byMatricula::get)
                    .or(() -> Optional.ofNullable(p.cpf()).map(byCpf::get)).orElse(null);
            return new PessoaCorporativaResponseDTO(p.referencia(), p.tipo(), p.nome(), p.matricula(), p.cargo(),
                    p.sublotacao(), p.unidadeNome(), p.email(), existing != null,
                    existing == null ? null : existing.getSituacao(),
                    ProfileSuggestion.fromCargo(p.cargo(), p.tipo()).orElse(null));
        }).toList();
    }

    private Unidade unidadeEmOperacao() {
        UUID id = CurrentUser.unidadeEmOperacao();
        return unidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
    }

    private static int codigoCorporativo(Unidade unidade) {
        if (unidade.getCodigoCorporativo() == null) {
            throw ImportacaoRejectedException.unidadeSemCodigoCorporativo();
        }
        return unidade.getCodigoCorporativo();
    }

    private static void requireGestao(AuthenticatedUser user) {
        if (!PerfilPolicy.isGestao(user.perfil())) {
            throw new AccessDeniedException("Apenas gestão pode buscar e importar pessoas corporativas.");
        }
    }

    private static String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private static String fold(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
