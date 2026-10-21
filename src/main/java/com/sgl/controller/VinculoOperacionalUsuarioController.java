package com.sgl.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.dto.response.VinculoOperacionalUsuarioResponseDTO;
import com.sgl.service.VinculoOperacionalUsuarioService;

/** Temporário: somente perfis DEV/DEMO. Retirar deste controller o acesso
 * de escrita sem SSO antes de qualquer deploy real. */
@RestController
@Profile({"dev", "demo"})
@RequestMapping("/api/v1/vinculos-operacionais")
@RequiredArgsConstructor
public class VinculoOperacionalUsuarioController {
    private final VinculoOperacionalUsuarioService service;
    @GetMapping("/resumo")
    public List<VinculoOperacionalUsuarioService.ResumoUsuario> resumo() {
        return service.resumo();
    }
    @GetMapping("/usuarios/{usuarioId}")
    public List<VinculoOperacionalUsuarioResponseDTO> listar(@PathVariable UUID usuarioId) {
        return service.listar(usuarioId);
    }
    @PostMapping
    public VinculoOperacionalUsuarioResponseDTO criar(@Valid @RequestBody VinculoOperacionalUsuarioRequestDTO dto) {
        return service.criar(dto);
    }
    @PutMapping("/{id}/encerrar")
    public VinculoOperacionalUsuarioResponseDTO encerrar(@PathVariable UUID id) {
        return service.encerrar(id);
    }
    @PutMapping("/usuarios/{usuarioId}/laboratorio-base")
    public void laboratorioBase(@PathVariable UUID usuarioId, @RequestBody Map<String, UUID> dados) {
        service.definirLaboratorioBase(usuarioId, dados.get("laboratorioId"));
    }
}
