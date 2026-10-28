package com.sgl.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.CargoSubstituicaoDTO;
import com.sgl.service.CargoSubstituicaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cargos a substituir", description = "Cargos da unidade cujo ocupante saiu, foi inativado ou mudou de lotação.")
@RestController
@RequestMapping("/api/v1/cargos-a-substituir")
@RequiredArgsConstructor
public class CargoSubstituicaoController {

    private final CargoSubstituicaoService cargoSubstituicaoService;

    @Operation(summary = "Listar cargos a substituir",
            description = "Lista, para a Unidade em operação, os cargos que precisam de novo ocupante. Apenas gestão (GESTOR, ADMINISTRADOR ou ADMINISTRADOR_GERAL).")
    @GetMapping
    public ResponseEntity<List<CargoSubstituicaoDTO>> listar() {
        return ResponseEntity.ok(cargoSubstituicaoService.listar());
    }
}
