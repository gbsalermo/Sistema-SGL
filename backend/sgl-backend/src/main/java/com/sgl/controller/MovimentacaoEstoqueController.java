package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.DescarteProdutoRequestDTO;
import com.sgl.dto.request.EntradaLoteRequestDTO;
import com.sgl.dto.response.LoteResponseDTO;
import com.sgl.dto.response.MovimentacaoEstoqueResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Usuario;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.UsuarioRepository;
import com.sgl.service.MovimentacaoEstoqueService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Movimentações de Estoque", description = "Consultas e operações físicas de entrada, saída e descarte de materiais.")
@RestController
@RequestMapping("/api/v1/movimentacoes")
@RequiredArgsConstructor
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Listar movimentações", description = "Retorna todas as movimentações de estoque registradas no sistema.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarTodos() { return ResponseEntity.ok(movimentacaoService.listarTodos()); }

    @Operation(summary = "Buscar movimentação por ID", description = "Retorna uma movimentação pelo seu identificador público UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentação encontrada", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Movimentação não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoEstoqueResponseDTO> buscarPorId(@PathVariable UUID id) { return ResponseEntity.ok(movimentacaoService.buscarPorId(id)); }

    @Operation(summary = "Listar movimentações por produto", description = "Retorna as movimentações vinculadas ao produto informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/produto")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorProduto(@RequestParam UUID produtoId) { return ResponseEntity.ok(movimentacaoService.listarPorProduto(produtoId)); }

    @Operation(summary = "Listar movimentações por laboratório", description = "Retorna as movimentações vinculadas ao laboratório informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/laboratorio")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) { return ResponseEntity.ok(movimentacaoService.listarPorLaboratorio(laboratorioId)); }

    @Operation(summary = "Listar movimentações por usuário", description = "Retorna as movimentações registradas pelo usuário informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/usuario")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorUsuario(@RequestParam UUID usuarioId) { return ResponseEntity.ok(movimentacaoService.listarPorUsuario(usuarioId)); }

    @Operation(summary = "Listar movimentações por pedido", description = "Retorna as movimentações geradas pelo pedido informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/pedido")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorPedido(@RequestParam UUID pedidoId) { return ResponseEntity.ok(movimentacaoService.listarPorPedido(pedidoId)); }

    @Operation(summary = "Listar movimentações por tipo", description = "Retorna as movimentações filtradas pelo tipo informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Movimentações listadas com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "400", description = "Tipo de movimentação inválido", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/tipo")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listarPorTipo(@RequestParam TipoMovimentacao tipo) { return ResponseEntity.ok(movimentacaoService.listarPorTipo(tipo)); }

    // usuarioId temporário até a autenticação fornecer o usuário atual.
    @Operation(summary = "Registrar entrada de lote", description = "Registra a entrada física de um novo lote no estoque e gera a movimentação correspondente.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Entrada de lote registrada com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Estoque ou usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/estoques/{estoqueId}/lotes")
    public ResponseEntity<LoteResponseDTO> registrarEntradaLote(@PathVariable UUID estoqueId, @RequestParam UUID usuarioId, @Valid @RequestBody EntradaLoteRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
        LoteResponseDTO lote = movimentacaoService.registrarEntradaLote(estoqueId, dto, usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(lote);
    }

    // usuarioId temporário até a autenticação fornecer o usuário atual.
    @Operation(summary = "Descartar produto vencido", description = "Registra o descarte de quantidade vencida do estoque e gera as movimentações correspondentes.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Descarte registrado com sucesso", useReturnTypeSchema = true), @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Estoque ou usuário não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/estoques/{estoqueId}/descarte-vencimento")
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> descartarVencidos(@PathVariable UUID estoqueId, @RequestParam UUID usuarioId, @Valid @RequestBody DescarteProdutoRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByPublicId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
        return ResponseEntity.ok(movimentacaoService.registrarDescarteVencimento(estoqueId, dto.getQuantidade(), dto.getJustificativa(), usuario));
    }
}
