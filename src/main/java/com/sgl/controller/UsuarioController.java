package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.UsuarioPerfilRequestDTO;
import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Usuários", description = "Operações de cadastro e consulta dos usuários do sistema.")
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários cadastrados no sistema.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuários listados com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() { return ResponseEntity.ok(usuarioService.listarTodos()); }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário pelo seu identificador público UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário encontrado", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) { return ResponseEntity.ok(usuarioService.buscarPorId(id)); }

    @Operation(summary = "Listar usuários por laboratório", description = "Retorna os usuários vinculados ao laboratório informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuários listados com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<UsuarioResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) { return ResponseEntity.ok(usuarioService.listarPorLaboratorio(laboratorioId)); }

    @Operation(summary = "Criar usuário", description = "Cadastra um novo usuário no sistema.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Usuário criado com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Unidade ou laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO novoUsuario = usuarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados do usuário identificado pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Usuário ou recurso relacionado não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody UsuarioRequestDTO dto) { return ResponseEntity.ok(usuarioService.atualizar(id, dto)); }

    @Operation(
            summary = "Alterar perfil de acesso",
            description = "Altera somente o perfil de acesso de um usuário existente. Endpoint destinado à administração; não cria nem substitui o cadastro institucional do usuário."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil alterado com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/perfil")
    public ResponseEntity<UsuarioResponseDTO> alterarPerfil(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioPerfilRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.alterarPerfil(id, dto.getPerfil()));
    }

    @Operation(summary = "Inativar usuário", description = "Inativa o usuário identificado pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"), @ApiResponse(responseCode = "400", description = "Regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) { usuarioService.Inativar(id); return ResponseEntity.noContent().build(); }
}
