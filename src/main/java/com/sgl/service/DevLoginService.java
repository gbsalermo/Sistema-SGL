package com.sgl.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sgl.dto.response.DevLoginResumoDTO;
import com.sgl.dto.response.DevLoginUsuarioDTO;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.dto.response.UnidadeOpcaoDTO;
import com.sgl.exception.LoginRejectedException;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Developer-only "test login": lists seeded users and opens a session for one of them without a password.
 * Not transactional on purpose, like the corporate login: each repository call is its own short read.
 */
@Service
@Profile({"dev", "demo"})
@RequiredArgsConstructor
public class DevLoginService {

    private static final Logger LOG = LoggerFactory.getLogger(DevLoginService.class);
    private static final int MAX_USUARIOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final AuthService authService;

    public List<DevLoginUsuarioDTO> buscarUsuarios(Perfil perfil, UUID unidadeId, String busca) {
        List<Long> ids = usuarioRepository.buscarIdsParaLoginDeTeste(perfil, unidadeId, padraoDeBusca(busca),
                PageRequest.of(0, MAX_USUARIOS));
        if (ids.isEmpty()) {
            return List.of();
        }
        // The fetch-graph query does not preserve the order of the paged ids, so restore it here.
        Map<Long, Usuario> porId = usuarioRepository.findWithUnidadeAndLaboratoriosByIdIn(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        return ids.stream()
                .map(porId::get)
                .filter(java.util.Objects::nonNull)
                .map(DevLoginUsuarioDTO::de)
                .toList();
    }

    public DevLoginResumoDTO resumo(UUID unidadeId) {
        // Every profile is always present, with 0 when nobody holds it.
        Map<Perfil, Long> contagem = new EnumMap<>(Perfil.class);
        for (Perfil perfil : Perfil.values()) {
            contagem.put(perfil, 0L);
        }
        for (Object[] linha : usuarioRepository.contarAtivosComLoginPorPerfil(unidadeId)) {
            contagem.put((Perfil) linha[0], (Long) linha[1]);
        }
        List<UnidadeOpcaoDTO> unidades = unidadeRepository.findAllSemFiltroDeTenant().stream()
                .map(u -> new UnidadeOpcaoDTO(u.getPublicId(), u.getSigla(), u.getNome()))
                .toList();
        return new DevLoginResumoDTO(contagem, unidades);
    }

    public LoginResponseDTO entrar(String login) {
        Usuario usuario = usuarioRepository.findByLogin(login == null ? null : login.trim())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .orElseThrow(LoginRejectedException::credenciaisInvalidas);
        LOG.warn("DEV-LOGIN: sessão iniciada como {}", usuario.getLogin());
        return authService.issueSession(usuario);
    }

    /** Lowercased and wrapped in % for LIKE, with the LIKE metacharacters escaped (ESCAPE '\'). */
    private static String padraoDeBusca(String busca) {
        if (busca == null || busca.isBlank()) {
            return null;
        }
        String escapado = busca.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
