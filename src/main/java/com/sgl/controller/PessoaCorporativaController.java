package com.sgl.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.dto.request.ImportarPessoaRequestDTO;
import com.sgl.dto.response.PessoaCorporativaResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.service.PessoaCorporativaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pessoas corporativas", description = "Busca e importação de empregados e colaboradores da Unidade em operação.")
@RestController
@RequestMapping("/api/v1/pessoas-corporativas")
@RequiredArgsConstructor
public class PessoaCorporativaController {

    private final PessoaCorporativaService service;

    @Operation(summary = "Buscar pessoas nas bases corporativas",
            description = "Matrícula (4 a 7 dígitos, só empregados) ou nome (mínimo 3 caracteres). Máximo de 50 resultados.")
    @GetMapping
    public List<PessoaCorporativaResponseDTO> buscar(@RequestParam TipoPessoaCorporativa tipo,
            @RequestParam String termo) {
        return service.buscar(tipo, termo);
    }

    @Operation(summary = "Importar pessoa corporativa",
            description = "Cria o usuário já ATIVO, com perfil e laboratório-base.")
    @PostMapping("/importar")
    public ResponseEntity<UsuarioSessaoResponseDTO> importar(@Valid @RequestBody ImportarPessoaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.importar(request));
    }
}
