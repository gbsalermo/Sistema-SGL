package com.sgl.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import com.sgl.config.AuthProperties;
import com.sgl.corporate.CorporateApiClient;
import com.sgl.corporate.CorporateDtos.CorporateLoginUser;
import com.sgl.corporate.CorporateIdentity;
import com.sgl.corporate.CorporateIdentityResolver;
import com.sgl.corporate.CpfMasker;
import com.sgl.corporate.InvalidCorporateCredentialsException;
import com.sgl.corporate.LoginNormalizer;
import com.sgl.corporate.ProfileSuggestion;
import com.sgl.dto.response.AuthConfigResponseDTO;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.exception.LoginRejectedException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemUsuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.CurrentUser;
import com.sgl.security.JwtService;
import com.sgl.security.LoginRateLimiter;

/**
 * Corporate login, DEV login and "who am I".
 *
 * Login is deliberately not @Transactional: it waits on the corporate gateway (no DB connection
 * should be held meanwhile), and a refusal must still commit side effects such as marking a user
 * that the corporate bases report as inactive.
 */
@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");
    private static final String SYSTEM_ACTOR = "sistema";
    // Serializes the "is there an ADMINISTRADOR_GERAL yet?" check with the save that answers it,
    // so two simultaneous first logins cannot both be promoted (single-instance deployment).
    private static final Object FIRST_ADMIN_LOCK = new Object();

    private final CorporateApiClient corporateApiClient;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final Environment environment;
    private final MudancaLotacaoService mudancaLotacaoService;

    public AuthService(CorporateApiClient corporateApiClient, UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository, JwtService jwtService, LoginRateLimiter rateLimiter,
            AuthProperties authProperties, Clock clock, Environment environment,
            MudancaLotacaoService mudancaLotacaoService) {
        this.corporateApiClient = corporateApiClient;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.authProperties = authProperties;
        this.clock = clock;
        this.environment = environment;
        this.mudancaLotacaoService = mudancaLotacaoService;
        if (authProperties.isPrimeiroLoginAdmin()) {
            LOG.warn("sgl.auth.primeiro-login-admin is ON: the first corporate login becomes ADMINISTRADOR_GERAL "
                    + "if none has ever been registered. Turn SGL_PRIMEIRO_LOGIN_ADMIN off after the first deploy.");
        }
    }

    public LoginResponseDTO login(String login, String senha, String clientIp) {
        if (isBlank(login) || isBlank(senha)) {
            throw LoginRejectedException.credenciaisInvalidas();
        }
        String normalizedLogin = LoginNormalizer.normalize(login);
        String rateLimitKey = normalizedLogin.toLowerCase(Locale.ROOT) + "|" + clientIp;
        if (rateLimiter.isBlocked(rateLimitKey)) {
            throw LoginRejectedException.muitasTentativas();
        }

        CorporateLoginUser corporateUser;
        try {
            corporateUser = corporateApiClient.login(normalizedLogin, senha);
        } catch (InvalidCorporateCredentialsException e) {
            // Only wrong passwords count: gateway outages or refusals after a valid password are not guessing.
            rateLimiter.recordFailure(rateLimitKey);
            throw e;
        }
        rateLimiter.reset(rateLimitKey);

        if (CorporateIdentityResolver.isDisabled(corporateUser.userAccountControl())) {
            findByLoginResponse(corporateUser, normalizedLogin).ifPresent(u -> deactivate(u, normalizedLogin));
            throw LoginRejectedException.contaDesativada();
        }

        CorporateIdentity identity = CorporateIdentityResolver.resolve(corporateUser, corporateApiClient);
        String matricula = realMatricula(identity.matricula());
        Optional<Usuario> existing = findExisting(matricula, identity.cpf());

        if (!identity.ativo()) {
            existing.ifPresent(u -> deactivate(u, normalizedLogin));
            throw LoginRejectedException.contaDesativada();
        }
        // An SGL-side deactivation is an admin decision; a successful corporate login must not undo it.
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getAtivo())) {
            throw LoginRejectedException.contaDesativada();
        }

        Unidade unidade = Optional.ofNullable(identity.unidadeCorporativaId())
                .flatMap(unidadeRepository::findByCodigoCorporativo)
                .orElseThrow(LoginRejectedException::unidadeNaoHabilitada);

        OffsetDateTime now = OffsetDateTime.now(clock);
        Usuario usuario = existing
                .map(u -> usuarioRepository.save(synchronize(u, identity, matricula, unidade, normalizedLogin, now)))
                .orElseGet(() -> createCorporateUser(identity, matricula, unidade, normalizedLogin, now));

        return issueSession(usuario);
    }

    public LoginResponseDTO devLogin(String login) {
        // Only seeded DEV accounts: this password-less path must never open a corporate account.
        Usuario usuario = usuarioRepository.findByLogin(login == null ? null : login.trim())
                .filter(u -> u.getOrigem() == OrigemUsuario.DEV)
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .orElseThrow(LoginRejectedException::credenciaisInvalidas);
        return issueSession(usuario);
    }

    /** Built from the user's own record: the tenant filter does not run on /auth routes. */
    public UsuarioSessaoResponseDTO me() {
        return sessionUser(CurrentUser.get().publicId());
    }

    public AuthConfigResponseDTO config() {
        return new AuthConfigResponseDTO(environment.acceptsProfiles(Profiles.of("dev", "demo")));
    }

    private LoginResponseDTO issueSession(Usuario usuario) {
        String token = jwtService.generate(usuario.getPublicId());
        return new LoginResponseDTO(token, jwtService.expiresAt(token), sessionUser(usuario.getPublicId()));
    }

    private UsuarioSessaoResponseDTO sessionUser(UUID publicId) {
        return usuarioRepository.findWithUnidadeAndLaboratorioByPublicId(publicId)
                .map(UsuarioSessaoResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário da sessão não encontrado."));
    }

    // Profile, laboratório and vínculos belong to the SGL and are never overwritten by synchronization.
    private Usuario synchronize(Usuario usuario, CorporateIdentity identity, String matricula, Unidade unidade,
            String normalizedLogin, OffsetDateTime now) {
        if (!isBlank(identity.nome())) {
            usuario.setNome(identity.nome());
        }
        if (!isBlank(identity.email())) {
            usuario.setEmail(identity.email());
        }
        usuario.setCargo(identity.cargo());
        usuario.setSublotacao(identity.sublotacao());
        mudancaLotacaoService.aplicar(usuario, unidade, provisionalProfile(identity));
        if (!LoginNormalizer.isCpf(normalizedLogin)) {
            usuario.setLogin(normalizedLogin);
        }
        // Fill gaps only: a stored matrícula is the stable key and is never replaced.
        if (isBlank(usuario.getMatricula()) && matricula != null) {
            usuario.setMatricula(matricula);
        }
        if (isBlank(usuario.getCpf()) && identity.cpf() != null) {
            usuario.setCpf(identity.cpf());
        }
        usuario.setUltimoLoginEm(now);
        usuario.setSincronizadoEm(now);
        return usuario;
    }

    private Usuario createCorporateUser(CorporateIdentity identity, String matricula, Unidade unidade,
            String normalizedLogin, OffsetDateTime now) {
        Usuario usuario = new Usuario();
        // Never fall back to the login for the name: it may be a CPF, and the name is shown everywhere.
        usuario.setNome(isBlank(identity.nome()) ? "Usuário corporativo" : identity.nome());
        usuario.setEmail(isBlank(identity.email()) ? null : identity.email());
        usuario.setMatricula(matricula);
        usuario.setCpf(identity.cpf());
        usuario.setCargo(identity.cargo());
        usuario.setSublotacao(identity.sublotacao());
        usuario.setUnidade(unidade);
        usuario.setLogin(storableLogin(normalizedLogin));
        usuario.setOrigem(OrigemUsuario.CORPORATIVO);
        usuario.setAtivo(true);
        usuario.setUltimoLoginEm(now);
        usuario.setSincronizadoEm(now);

        if (!authProperties.isPrimeiroLoginAdmin()) {
            return savePending(usuario, identity);
        }
        synchronized (FIRST_ADMIN_LOCK) {
            // Any ADMINISTRADOR_GERAL, even a deactivated one, means the bootstrap was already used.
            if (usuarioRepository.existsByPerfil(Perfil.ADMINISTRADOR_GERAL)) {
                return savePending(usuario, identity);
            }
            usuario.setPerfil(Perfil.ADMINISTRADOR_GERAL);
            usuario.setSituacao(SituacaoUsuario.ATIVO);
            Usuario saved = usuarioRepository.saveAndFlush(usuario);
            AUDIT.info("acao=PRIMEIRO_LOGIN_ADMINISTRADOR_GERAL ator={} alvo={} login={}",
                    saved.getPublicId(), saved.getPublicId(), maskedLogin(normalizedLogin));
            return saved;
        }
    }

    // The profile is only provisional: the security filter keeps a PENDENTE_VINCULACAO user out of
    // every operational route until a manager approves them and confirms the profile.
    private Usuario savePending(Usuario usuario, CorporateIdentity identity) {
        usuario.setPerfil(provisionalProfile(identity));
        usuario.setSituacao(SituacaoUsuario.PENDENTE_VINCULACAO);
        return usuarioRepository.save(usuario);
    }

    private static Perfil provisionalProfile(CorporateIdentity identity) {
        return ProfileSuggestion.fromCargo(identity.cargo(), identity.tipo()).orElse(Perfil.TECNICO);
    }

    private void deactivate(Usuario usuario, String normalizedLogin) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            return;
        }
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        AUDIT.info("acao=USUARIO_DESATIVADO_PELA_BASE_CORPORATIVA ator={} alvo={} login={}",
                SYSTEM_ACTOR, usuario.getPublicId(), maskedLogin(normalizedLogin));
    }

    /** Matrícula first, then CPF; never by name or e-mail. */
    private Optional<Usuario> findExisting(String matricula, String cpf) {
        Optional<Usuario> byMatricula = matricula == null ? Optional.empty() : usuarioRepository.findByMatricula(matricula);
        if (byMatricula.isPresent() || isBlank(cpf)) {
            return byMatricula;
        }
        return usuarioRepository.findByCpf(cpf);
    }

    // A disabled account is refused before the corporate bases are queried, so the only keys at
    // hand are the ones in the login response itself.
    private Optional<Usuario> findByLoginResponse(CorporateLoginUser corporateUser, String normalizedLogin) {
        String reported = isBlank(corporateUser.matricula()) ? null : corporateUser.matricula().trim();
        String cpf = LoginNormalizer.isCpf(reported) ? reported
                : LoginNormalizer.isCpf(normalizedLogin) ? normalizedLogin : null;
        return findExisting(realMatricula(reported), cpf);
    }

    /** The gateway reports the CPF as "matrícula" for some people; that must never be stored as one. */
    private static String realMatricula(String matricula) {
        return isBlank(matricula) || LoginNormalizer.isCpf(matricula.trim()) ? null : matricula.trim();
    }

    /** A CPF already lives in the cpf column; keeping a second copy in login would only widen its exposure. */
    private static String storableLogin(String normalizedLogin) {
        return LoginNormalizer.isCpf(normalizedLogin) ? null : normalizedLogin;
    }

    private static String maskedLogin(String normalizedLogin) {
        return LoginNormalizer.isCpf(normalizedLogin) ? CpfMasker.mask(normalizedLogin) : normalizedLogin;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
