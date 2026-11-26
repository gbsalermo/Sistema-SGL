package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.dto.request.ImportarLoteRequestDTO;
import com.sgl.dto.response.MembroEquipeResponseDTO;
import com.sgl.dto.response.PessoaCorporativaResponseDTO;
import com.sgl.dto.response.ResultadoImportacaoDTO;
import com.sgl.service.EquipeLaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Equipe do laboratório", description = "Equipe organizada pelo responsável do laboratório ou por um administrador da Unidade.")
@RestController
@RequestMapping("/api/v1/laboratorios/{laboratorioId}/equipe")
@RequiredArgsConstructor
public class EquipeLaboratorioController {

    private final EquipeLaboratorioService service;

    @Operation(summary = "Listar a equipe", description = "Pessoas com o laboratório na lista e pessoas com acesso temporário vigente.")
    @GetMapping
    public List<MembroEquipeResponseDTO> listar(@PathVariable UUID laboratorioId) {
        return service.listar(laboratorioId);
    }

    @Operation(summary = "Buscar pessoas da Embrapa ainda fora do SGL", description = "Ao menos 3 letras do nome.")
    @GetMapping("/candidatos-embrapa")
    public List<PessoaCorporativaResponseDTO> candidatos(@PathVariable UUID laboratorioId,
            @RequestParam TipoPessoaCorporativa tipo, @RequestParam String termo) {
        return service.candidatos(laboratorioId, tipo, termo);
    }

    @Operation(summary = "Cadastrar pessoas da Embrapa na equipe, em lote",
            description = "Até 50 por envio. Cada pessoa é cadastrada à parte e o resultado vem pessoa a pessoa.")
    @PostMapping("/importar")
    public List<ResultadoImportacaoDTO> importar(@PathVariable UUID laboratorioId,
            @Valid @RequestBody ImportarLoteRequestDTO lote) {
        return service.importar(laboratorioId, lote);
    }

    @Operation(summary = "Incluir pessoa já cadastrada na equipe", description = "Idempotente: incluir quem já é da equipe não muda nada.")
    @PostMapping("/{usuarioId}")
    public ResponseEntity<Void> incluir(@PathVariable UUID laboratorioId, @PathVariable UUID usuarioId) {
        service.incluir(laboratorioId, usuarioId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Retirar pessoa da equipe", description = "Tira o laboratório da lista da pessoa e encerra os acessos temporários dela a ele.")
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> retirar(@PathVariable UUID laboratorioId, @PathVariable UUID usuarioId) {
        service.retirar(laboratorioId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
