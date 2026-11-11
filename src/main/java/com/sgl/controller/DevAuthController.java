package com.sgl.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.DevLoginRequestDTO;
import com.sgl.dto.response.DevLoginResumoDTO;
import com.sgl.dto.response.DevLoginUsuarioDTO;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.model.enums.Perfil;
import com.sgl.service.DevLoginService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Password-less "test login". The bean only exists in dev and demo (elsewhere the routes are a 404) and
 * every handler additionally refuses callers that are not on the machine itself.
 */
@Tag(name = "Autenticação")
@Profile({"dev", "demo"})
@RestController
@RequestMapping("/api/v1/auth/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevLoginService devLoginService;

    @Operation(summary = "Lista usuários de teste (somente dev/demo, somente loopback)")
    @GetMapping("/usuarios")
    public ResponseEntity<List<DevLoginUsuarioDTO>> usuarios(
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) String busca,
            HttpServletRequest request) {
        exigirLoopback(request);
        return ResponseEntity.ok(devLoginService.buscarUsuarios(perfil, unidadeId, busca));
    }

    @Operation(summary = "Resumo por perfil e unidades (somente dev/demo, somente loopback)")
    @GetMapping("/resumo")
    public ResponseEntity<DevLoginResumoDTO> resumo(@RequestParam(required = false) UUID unidadeId,
            HttpServletRequest request) {
        exigirLoopback(request);
        return ResponseEntity.ok(devLoginService.resumo(unidadeId));
    }

    @Operation(summary = "Entrar como usuário de teste (somente dev/demo, somente loopback)",
            description = "Emite o mesmo JWT do login corporativo, sem senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão emitida", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "CREDENCIAIS_INVALIDAS", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Chamada fora da própria máquina", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/entrar")
    public ResponseEntity<LoginResponseDTO> entrar(@Valid @RequestBody DevLoginRequestDTO dto,
            HttpServletRequest request) {
        exigirLoopback(request);
        return ResponseEntity.ok(devLoginService.entrar(dto.getLogin()));
    }

    private static void exigirLoopback(HttpServletRequest request) {
        if (!isLoopback(request.getRemoteAddr())) {
            throw new AccessDeniedException("Login de teste só é aceito a partir da própria máquina.");
        }
    }

    // Relies on the socket peer address on purpose: X-Forwarded-For must stay ignored. Enabling
    // server.forward-headers-strategy (or a cloud platform auto-enabling RemoteIpValve) would let
    // proxies, and therefore clients, forge a loopback address.
    private static boolean isLoopback(String remoteAddr) {
        // getByName(null) and getByName("") resolve to loopback, so fail closed first.
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        try {
            // Literal IPs only in practice (remoteAddr is never a hostname), so no DNS lookup happens.
            return InetAddress.getByName(remoteAddr).isLoopbackAddress();
        } catch (UnknownHostException | RuntimeException e) {
            return false;
        }
    }
}
