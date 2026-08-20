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

import com.sgl.dto.request.ProdutoRequestDTO;
import com.sgl.dto.response.ProdutoResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.model.enums.NivelRisco;
import com.sgl.service.ProdutoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Produtos", description = "Operações de cadastro e consulta do catálogo de produtos e materiais.")
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @Operation(summary = "Criar produto", description = "Cadastra um novo produto no catálogo do sistema.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Produto criado com sucesso"), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO dto) {
        ProdutoResponseDTO salvo = produtoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Operation(summary = "Listar produtos", description = "Retorna todos os produtos cadastrados no catálogo.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produtos listados com sucesso"), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarTodos() { return ResponseEntity.ok(produtoService.listarTodos()); }

    @Operation(summary = "Buscar produto por ID", description = "Retorna um produto pelo seu identificador público UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produto encontrado"), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable UUID id) { return ResponseEntity.ok(produtoService.buscarPorId(id)); }

    @Operation(summary = "Atualizar produto", description = "Atualiza os dados do produto identificado pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso"), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody ProdutoRequestDTO dto) { return ResponseEntity.ok(produtoService.atualizar(id, dto)); }

    @Operation(summary = "Excluir produto", description = "Remove o produto identificado pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Produto excluído com sucesso"), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) { produtoService.deletar(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Listar produtos por risco", description = "Retorna os produtos filtrados pelo nível de risco informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produtos listados com sucesso"), @ApiResponse(responseCode = "400", description = "Nível de risco inválido", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/risco/{nivel}")
    public ResponseEntity<List<ProdutoResponseDTO>> listarPorRisco(@PathVariable NivelRisco nivel) { return ResponseEntity.ok(produtoService.listarPorRisco(nivel)); }

    @Operation(summary = "Listar produtos perecíveis", description = "Retorna somente os produtos classificados como perecíveis.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Produtos perecíveis listados com sucesso"), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/pereciveis")
    public ResponseEntity<List<ProdutoResponseDTO>> listarPereciveis() { return ResponseEntity.ok(produtoService.listarPereciveis()); }

    @Operation(summary = "Buscar produtos por nome", description = "Retorna os produtos compatíveis com o nome informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"), @ApiResponse(responseCode = "400", description = "Parâmetro inválido", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/buscar")
    public ResponseEntity<List<ProdutoResponseDTO>> buscarPorNome(@RequestParam String nome) { return ResponseEntity.ok(produtoService.buscarPorNome(nome)); }
}
