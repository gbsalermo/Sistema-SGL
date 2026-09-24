package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.AdministrarResiduoRequestDTO;
import com.sgl.dto.request.AnalisarResiduoRequestDTO;
import com.sgl.dto.request.ArmazenarResiduoRequestDTO;
import com.sgl.dto.request.CriarResiduoRequestDTO;
import com.sgl.dto.request.DespacharResiduoRequestDTO;
import com.sgl.dto.request.ReceberResiduoRequestDTO;
import com.sgl.dto.response.HistoricoResiduoResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.dto.response.RotuloResiduoResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.service.ResiduoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Resíduos", description = "Comunicação, conferência, rotulagem, armazenamento temporário e despacho de resíduos laboratoriais.")
@RestController
@RequestMapping("/api/v1/residuos")
@RequiredArgsConstructor
public class ResiduoController {

    private final ResiduoService residuoService;

    @Operation(summary = "Informar resíduo", description = "Registra a entrega de um resíduo gerado pelo laboratório. Componentes podem ou não referenciar produtos do catálogo e nunca movimentam estoque.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resíduo informado com sucesso",
                    content = @Content(schema = @Schema(implementation = ResiduoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário, laboratório, projeto ou produto de referência não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ResiduoResponseDTO> criar(
            @Valid @RequestBody CriarResiduoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(residuoService.criar(dto));
    }

    @Operation(summary = "Listar resíduos")
    @GetMapping
    public ResponseEntity<List<ResiduoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(residuoService.listarTodos());
    }

    @Operation(summary = "Buscar resíduo por ID público")
    @GetMapping("/{id}")
    public ResponseEntity<ResiduoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(residuoService.buscarPorId(id));
    }

    @Operation(summary = "Listar resíduos por status")
    @GetMapping("/por-status")
    public ResponseEntity<List<ResiduoResponseDTO>> listarPorStatus(
            @RequestParam StatusResiduo status) {
        return ResponseEntity.ok(residuoService.listarPorStatus(status));
    }

    @Operation(summary = "Listar resíduos por laboratório")
    @GetMapping("/por-laboratorio")
    public ResponseEntity<List<ResiduoResponseDTO>> listarPorLaboratorio(
            @RequestParam UUID laboratorioId) {
        return ResponseEntity.ok(residuoService.listarPorLaboratorio(laboratorioId));
    }

    @Operation(summary = "Listar resíduos por gerador", description = "Suporta a experiência Meus resíduos usando o usuário da sessão atual como gerador.")
    @GetMapping("/por-gerador")
    public ResponseEntity<List<ResiduoResponseDTO>> listarPorGerador(
            @RequestParam UUID usuarioGeradorId) {
        return ResponseEntity.ok(residuoService.listarPorGerador(usuarioGeradorId));
    }

    @Operation(summary = "Receber resíduo para análise", description = "Marca o recebimento pela gestão e inicia a etapa de conferência dos dados informados pelo laboratório.")
    @PutMapping("/{id}/receber")
    public ResponseEntity<ResiduoResponseDTO> receber(
            @PathVariable UUID id,
            @Valid @RequestBody ReceberResiduoRequestDTO dto) {
        return ResponseEntity.ok(residuoService.receber(id, dto));
    }

    @Operation(summary = "Analisar e liberar resíduo", description = "Confirma os riscos, define armazenamento/destino e libera os dados consolidados do rótulo. O código SGL já é gerado no registro inicial do resíduo.")
    @PutMapping("/{id}/analisar-liberar")
    public ResponseEntity<ResiduoResponseDTO> analisarELiberar(
            @PathVariable UUID id,
            @Valid @RequestBody AnalisarResiduoRequestDTO dto) {
        return ResponseEntity.ok(residuoService.analisarELiberar(id, dto));
    }

    @Operation(summary = "Confirmar armazenamento temporário")
    @PutMapping("/{id}/armazenar")
    public ResponseEntity<ResiduoResponseDTO> confirmarArmazenamento(
            @PathVariable UUID id,
            @Valid @RequestBody ArmazenarResiduoRequestDTO dto) {
        return ResponseEntity.ok(residuoService.confirmarArmazenamento(id, dto));
    }

    @Operation(summary = "Confirmar despacho do resíduo")
    @PutMapping("/{id}/despachar")
    public ResponseEntity<ResiduoResponseDTO> despachar(
            @PathVariable UUID id,
            @Valid @RequestBody DespacharResiduoRequestDTO dto) {
        return ResponseEntity.ok(residuoService.despachar(id, dto));
    }

    @Operation(
            summary = "Administrar ciclo do resíduo",
            description = "Permite ao Administrador cancelar um resíduo ou retornar uma etapa operacional, sempre com justificativa registrada no histórico."
    )
    @PutMapping("/{id}/administrar")
    public ResponseEntity<ResiduoResponseDTO> administrarCiclo(
            @PathVariable UUID id,
            @Valid @RequestBody AdministrarResiduoRequestDTO dto) {
        return ResponseEntity.ok(residuoService.administrarCiclo(id, dto));
    }

    @Operation(summary = "Obter prévia do rótulo", description = "Retorna os dados do rótulo desde o registro inicial. A impressão física só é permitida após análise e liberação.")
    @GetMapping("/{id}/rotulo")
    public ResponseEntity<RotuloResiduoResponseDTO> gerarRotulo(@PathVariable UUID id) {
        return ResponseEntity.ok(residuoService.gerarDadosRotulo(id));
    }

    @Operation(summary = "Consultar histórico operacional dos resíduos da unidade", description = "Retorna os eventos de rastreabilidade dos resíduos da unidade atual, do mais recente para o mais antigo.")
    @GetMapping("/historico/unidade")
    public ResponseEntity<List<HistoricoResiduoResponseDTO>> buscarHistoricoDaUnidade() {
        return ResponseEntity.ok(residuoService.buscarHistoricoDaUnidade());
    }

    @Operation(summary = "Consultar histórico dos resíduos do gerador", description = "Retorna os eventos de rastreabilidade dos resíduos pertencentes ao gerador da unidade atual, do mais recente para o mais antigo.")
    @GetMapping("/historico/por-gerador")
    public ResponseEntity<List<HistoricoResiduoResponseDTO>> buscarHistoricoPorGerador(
            @RequestParam UUID usuarioGeradorId) {
        return ResponseEntity.ok(residuoService.buscarHistoricoPorGerador(usuarioGeradorId));
    }

    @Operation(summary = "Consultar histórico do resíduo")
    @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistoricoResiduoResponseDTO.class))))
    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoResiduoResponseDTO>> buscarHistorico(@PathVariable UUID id) {
        return ResponseEntity.ok(residuoService.buscarHistorico(id));
    }
}
