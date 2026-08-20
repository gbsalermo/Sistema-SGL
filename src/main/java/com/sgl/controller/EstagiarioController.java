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

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.EstagiarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Estagiários", description = "Operações de cadastro, consulta e encerramento de estágios.")
@RestController
@RequestMapping("/api/v1/estagiarios")
@RequiredArgsConstructor
public class EstagiarioController {

    private final EstagiarioService estagiarioService;

    @Operation(summary = "Listar todos os estagiários", description = "Retorna todos os estagiários cadastrados no sistema, ativos ou inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estagiários listados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<EstagiarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estagiarioService.listarTodos());
    }

    @Operation(summary = "Buscar estagiário por ID", description = "Retorna um estagiário pelo seu identificador público UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estagiário encontrado"),
            @ApiResponse(responseCode = "404", description = "Estagiário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EstagiarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estagiarioService.buscarPorId(id));
    }

    @Operation(summary = "Listar estagiários por laboratório", description = "Retorna os estagiários vinculados ao laboratório informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estagiários listados com sucesso"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<EstagiarioResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(estagiarioService.listarPorLaboratorio(laboratorioId));
    }

    @Operation(summary = "Listar estagiários ativos", description = "Retorna somente os estagiários com vínculo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estagiários ativos listados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/ativos")
    public ResponseEntity<List<EstagiarioResponseDTO>> listarAtivos() {
        return ResponseEntity.ok(estagiarioService.listarAtivos());
    }

    @Operation(summary = "Criar estagiário", description = "Cadastra um novo estagiário no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estagiário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário ou laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<EstagiarioResponseDTO> criar(@Valid @RequestBody EstagiarioRequestDTO dto) {
        EstagiarioResponseDTO novoEstagiario = estagiarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoEstagiario);
    }

    @Operation(summary = "Atualizar estagiário", description = "Atualiza os dados do estagiário identificado pelo UUID informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estagiário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Estagiário ou recurso relacionado não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EstagiarioResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EstagiarioRequestDTO dto) {
        return ResponseEntity.ok(estagiarioService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir estagiário", description = "Remove o cadastro do estagiário identificado pelo UUID informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estagiário excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estagiário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        estagiarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Encerrar estágio", description = "Finaliza o vínculo de estágio do estagiário informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estágio encerrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Estagiário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/encerrar")
    public ResponseEntity<EstagiarioResponseDTO> encerrarEstagio(@PathVariable UUID id) {
        return ResponseEntity.ok(estagiarioService.encerrarEstagio(id));
    }
}
