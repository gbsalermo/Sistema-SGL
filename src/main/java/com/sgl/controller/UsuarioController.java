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

import com.sgl.dto.request.AprovarUsuarioRequestDTO;
import com.sgl.dto.request.UsuarioCategoriaCargoRequestDTO;
import com.sgl.dto.request.UsuarioPapeisRequestDTO;
import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
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
            summary = "Alterar papéis do sistema",
            description = "Substitui os papéis acumuláveis (ADMINISTRADOR_GERAL, ADMINISTRADOR_LOCAL, GESTOR) de um usuário da Unidade em operação. Lista vazia remove todos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papéis alterados", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Papel que o ator não pode conceder ou alteração dos próprios papéis", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na Unidade em operação", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Usuário pendente, último Administrador geral (ULTIMO_ADMINISTRADOR_GERAL) ou último administrador local da Unidade (ULTIMO_ADMINISTRADOR_LOCAL)", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/papeis")
    public ResponseEntity<UsuarioResponseDTO> alterarPapeis(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioPapeisRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.alterarPapeis(id, dto.getPapeis()));
    }

    @Operation(
            summary = "Ajustar categoria de cargo",
            description = "Ajusta a categoria de cargo quando o cargo corporativo não a define, como no caso de bolsistas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria alterada", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Estágio ativo impede sair de ESTAGIARIO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Sem autoridade sobre o usuário", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado na Unidade em operação", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Usuário pendente", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}/categoria-cargo")
    public ResponseEntity<UsuarioResponseDTO> alterarCategoriaCargo(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioCategoriaCargoRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.alterarCategoriaCargo(id, dto.getCategoriaCargo()));
    }

    @Operation(summary = "Listar pendentes", description = "Usuários da Unidade em operação aguardando aprovação. Exige perfil de gestão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pendentes listados", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/pendentes")
    public ResponseEntity<List<UsuarioSessaoResponseDTO>> listarPendentes() {
        return ResponseEntity.ok(usuarioService.listarPendentes());
    }

    @Operation(summary = "Aprovar pendente", description = "Ativa um usuário pendente da Unidade em operação, definindo perfil e laboratórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário aprovado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Laboratório inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão ou pessoa de outra Unidade", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Usuário não está pendente (USUARIO_NAO_PENDENTE)", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/aprovar")
    public ResponseEntity<UsuarioSessaoResponseDTO> aprovar(
            @PathVariable UUID id,
            @Valid @RequestBody AprovarUsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.aprovar(id, dto));
    }

    @Operation(summary = "Recusar pendente", description = "Remove o cadastro pendente. Um novo login da pessoa volta a criá-lo como pendente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pendente removido"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão ou pessoa de outra Unidade", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Usuário não está pendente (USUARIO_NAO_PENDENTE)", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}/pendente")
    public ResponseEntity<Void> recusar(@PathVariable UUID id) {
        usuarioService.recusar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Inativar usuário", description = "Inativa o usuário identificado pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"), @ApiResponse(responseCode = "400", description = "Regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable UUID id) { usuarioService.Inativar(id); return ResponseEntity.noContent().build(); }
}
