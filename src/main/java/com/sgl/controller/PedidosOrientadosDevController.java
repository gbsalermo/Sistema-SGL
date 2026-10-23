package com.sgl.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.sgl.dto.response.PedidoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.service.VinculoOperacionalUsuarioService;
import com.sgl.tenant.TenantContext;

/** Consulta exclusivamente DEV/DEMO enquanto não houver SSO institucional.
 * Sem aprovação de pedidos e sem autorização real de identidade. */
@RestController
@Profile({"dev", "demo"})
@RequestMapping("/api/v1/pedidos-orientados")
@RequiredArgsConstructor
public class PedidosOrientadosDevController {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final VinculoOperacionalUsuarioService vinculos;

    @GetMapping("/orientadores/{orientadorId}")
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listar(@PathVariable UUID orientadorId) {
        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow(
            () -> new BusinessRuleException("Unidade obrigatória."));
        Usuario orientador = usuarios.findByPublicIdAndUnidadePublicId(orientadorId, unidadeId)
            .orElseThrow(() -> new ResourceNotFoundException("Orientador", orientadorId));
        if ((orientador.getPerfil() != Perfil.ANALISTA && orientador.getPerfil() != Perfil.PESQUISADOR)
            || !Boolean.TRUE.equals(orientador.getAtivo())) {
            throw new BusinessRuleException("Consulta reservada aos orientadores analistas/pesquisadores ativos.");
        }
        return pedidos.listarPedidosDeOrientandos(orientadorId, unidadeId).stream()
            .filter(p -> vinculos.vinculadoLaboratorio(orientador, p.getLaboratorio(), LocalDate.now()))
            .map(PedidoResponseDTO::new).toList();
    }
}
