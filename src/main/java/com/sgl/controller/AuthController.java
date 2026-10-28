package com.sgl.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.LoginRequestDTO;
import com.sgl.dto.response.AuthConfigResponseDTO;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Autenticação", description = "Login corporativo, sessão atual e opções de autenticação.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Entrar com a conta Embrapa",
            description = "Valida login de rede (ou CPF) e senha nas APIs corporativas e emite o JWT do SGL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão emitida", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Login ou senha ausentes", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "CREDENCIAIS_INVALIDAS", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "CONTA_DESATIVADA, UNIDADE_NAO_HABILITADA ou PESSOA_NAO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "MUITAS_TENTATIVAS", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "AUTENTICACAO_INDISPONIVEL", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // The direct peer address: X-Forwarded-For is not trusted, since any client could forge it to
        // dodge the rate limit. Behind a proxy this needs server.forward-headers-strategy configured.
        return ResponseEntity.ok(authService.login(dto.getLogin(), dto.getSenha(), request.getRemoteAddr()));
    }

    @Operation(summary = "Quem sou eu", description = "Dados do usuário da sessão, inclusive quando está pendente de vinculação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário da sessão", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "NAO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/me")
    public ResponseEntity<UsuarioSessaoResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @Operation(summary = "Opções de autenticação", description = "Informa se o login DEV está disponível neste ambiente.")
    @GetMapping("/config")
    public ResponseEntity<AuthConfigResponseDTO> config() {
        return ResponseEntity.ok(authService.config());
    }
}
