package com.sgl.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.DevLoginRequestDTO;
import com.sgl.dto.response.LoginResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Password-less login for seeded DEV users. The bean only exists in dev and demo, so elsewhere the route is a 404. */
@Tag(name = "Autenticação")
@Profile({"dev", "demo"})
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class DevAuthController {

    private final AuthService authService;

    @Operation(summary = "Login DEV (somente dev/demo)", description = "Emite o JWT para um usuário de exemplo, sem senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão emitida", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "CREDENCIAIS_INVALIDAS", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/dev-login")
    public ResponseEntity<LoginResponseDTO> devLogin(@Valid @RequestBody DevLoginRequestDTO dto) {
        return ResponseEntity.ok(authService.devLogin(dto.getLogin()));
    }
}
