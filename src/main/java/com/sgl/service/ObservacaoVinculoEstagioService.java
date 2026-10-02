package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ObservacaoVinculoEstagioRequestDTO;
import com.sgl.dto.request.TreinamentoSegurancaVinculoRequestDTO;
import com.sgl.dto.response.ObservacaoVinculoEstagioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ObservacaoVinculoEstagio;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.EventoObservacaoVinculoEstagio;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoObservacaoVinculoEstagio;
import com.sgl.repository.ObservacaoVinculoEstagioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObservacaoVinculoEstagioService {

    private final ObservacaoVinculoEstagioRepository observacaoRepository;
    private final VinculoEstagioRepository vinculoEstagioRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ObservacaoVinculoEstagioResponseDTO> listar(UUID vinculoId) {

        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

        buscarVinculoNoTenant(vinculoId, unidadeId);

        return observacaoRepository
                .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataHoraDesc(
                        vinculoId,
                        unidadeId)
                .stream()
                .map(ObservacaoVinculoEstagioResponseDTO::new)
                .toList();
    }

    @Transactional
    public ObservacaoVinculoEstagioResponseDTO adicionarOperacional(
            UUID vinculoId,
            ObservacaoVinculoEstagioRequestDTO dto) {

        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId, unidadeId);
        Usuario operador = buscarOperador(dto.getUsuarioId(), unidadeId);

        ObservacaoVinculoEstagio observacao = ObservacaoVinculoEstagio.builder()
                .vinculoEstagio(vinculo)
                .tipo(TipoObservacaoVinculoEstagio.OPERACIONAL)
                .evento(EventoObservacaoVinculoEstagio.OBSERVACAO)
                .texto(normalizarObrigatorio(dto.getTexto()))
                .usuario(operador)
                .build();

        return new ObservacaoVinculoEstagioResponseDTO(observacaoRepository.save(observacao));
    }

    @Transactional
    public ObservacaoVinculoEstagioResponseDTO alterarTreinamento(
            UUID vinculoId,
            TreinamentoSegurancaVinculoRequestDTO dto) {

        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId, unidadeId);
        Usuario operador = buscarOperador(dto.getUsuarioId(), unidadeId);

        if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {
            throw new BusinessRuleException(
                    "Não é possível alterar o treinamento de um vínculo finalizado.");
        }

        boolean concluidoAtual = Boolean.TRUE.equals(vinculo.getTreinamentoSegurancaConcluido());
        boolean novoEstado = Boolean.TRUE.equals(dto.getConcluido());

        if (concluidoAtual == novoEstado) {
            throw new BusinessRuleException(
                    novoEstado
                            ? "O treinamento de segurança já está concluído."
                            : "O treinamento de segurança já está pendente.");
        }

        vinculo.setTreinamentoSegurancaConcluido(novoEstado);
        vinculoEstagioRepository.save(vinculo);

        ObservacaoVinculoEstagio observacao = ObservacaoVinculoEstagio.builder()
                .vinculoEstagio(vinculo)
                .tipo(TipoObservacaoVinculoEstagio.TREINAMENTO_SEGURANCA)
                .evento(novoEstado
                        ? EventoObservacaoVinculoEstagio.TREINAMENTO_CONCLUIDO
                        : EventoObservacaoVinculoEstagio.TREINAMENTO_REVERTIDO)
                .texto(normalizarOpcional(dto.getObservacao()))
                .usuario(operador)
                .build();

        return new ObservacaoVinculoEstagioResponseDTO(observacaoRepository.save(observacao));
    }

    private VinculoEstagio buscarVinculoNoTenant(UUID vinculoId, UUID unidadeId) {

        return vinculoEstagioRepository
                .findByPublicIdAndEstagiarioUnidadePublicId(vinculoId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de estágio", vinculoId));
    }

    private Usuario buscarOperador(UUID usuarioId, UUID unidadeId) {

        Usuario usuario = usuarioRepository
                .findByPublicIdAndUnidadePublicId(usuarioId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário operador", usuarioId));

        usuario.validateActive();

        if (usuario.getPerfil() != Perfil.GESTOR
                && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new BusinessRuleException(
                    "A operação exige perfil GESTOR ou ADMINISTRADOR.");
        }

        return usuario;
    }

    private String normalizarObrigatorio(String valor) {

        String normalizado = normalizarOpcional(valor);

        if (normalizado == null) {
            throw new BusinessRuleException("Observação é obrigatória.");
        }

        return normalizado;
    }

    private String normalizarOpcional(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private void exigirTenantAtivo() {

        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        }
    }
}
