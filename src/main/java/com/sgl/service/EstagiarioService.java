package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EstagiarioRequestDTO;
import com.sgl.dto.response.EstagiarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstagiarioService {

    private final EstagiarioRepository estagiarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public EstagiarioResponseDTO criar(EstagiarioRequestDTO dto) {
        Usuario usuario = buscarUsuario(dto.getUsuarioId());
        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
        validarTenantUnidade(usuario.getUnidade() != null ? usuario.getUnidade().getPublicId() : null);
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

        if (estagiarioRepository.existsById(usuario.getId())) {
            throw new BusinessRuleException("Usuário já possui cadastro de estagiário.");
        }

        usuario.validateInternProfile();
        validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());
        validarUnidadeCompativel(usuario, laboratorio);

        usuario.setLaboratorio(laboratorio);
        usuarioRepository.save(usuario);

        entityManager.createNativeQuery(
                "INSERT INTO estagiarios (id, data_inicio_estagio, data_fim_estagio, tipo_bolsa, observacao) "
                        + "VALUES (:id, :dataInicio, :dataFim, :tipoBolsa, :observacao)")
                .setParameter("id", usuario.getId())
                .setParameter("dataInicio", dto.getDataInicioEstagio())
                .setParameter("dataFim", dto.getDataFimEstagio())
                .setParameter("tipoBolsa", dto.getTipoBolsa().name())
                .setParameter("observacao", dto.getObservacao())
                .executeUpdate();

        entityManager.clear();

        Estagiario salvo = estagiarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estagiário recém-criado",
                        usuario.getId()
                ));

        return new EstagiarioResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<EstagiarioResponseDTO> listarTodos() {
        // Correção de segurança: sem tenant definido, este método caía num
        // "findAll" que devolvia estagiários de todas as unidades. Agora
        // exigimos o header X-SGL-Unidade-Id também para listar.
        exigirTenantAtivo();

        List<Estagiario> estagiarios = estagiarioRepository
                .findByUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());

        return estagiarios.stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstagiarioResponseDTO buscarPorId(UUID id) {
        return new EstagiarioResponseDTO(buscarEstagiarioNoTenant(id));
    }

    @Transactional(readOnly = true)
    public List<EstagiarioResponseDTO> listarPorLaboratorio(UUID id) {
        Laboratorio laboratorio = buscarLaboratorio(id);
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

        return estagiarioRepository.findByLaboratorioId(laboratorio.getId())
                .stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EstagiarioResponseDTO> listarAtivos() {
        // Mesma correção: exigir tenant em vez de cair para "todas as
        // unidades" quando o header não é enviado.
        exigirTenantAtivo();

        List<Estagiario> estagiarios = estagiarioRepository
                .findByUnidadePublicIdAndAtivoTrue(TenantContext.unidadeAtual().orElseThrow());

        return estagiarios.stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional
    public EstagiarioResponseDTO atualizar(UUID id, EstagiarioRequestDTO dto) {
        Estagiario estagiario = buscarEstagiarioNoTenant(id);

        if (!id.equals(dto.getUsuarioId())) {
            throw new BusinessRuleException(
                    "Não é permitido trocar o usuário vinculado do estagiário."
            );
        }

        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);
        estagiario.validateInternProfile();
        validarUnidadeCompativel(estagiario, laboratorio);

        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setLaboratorio(laboratorio);
        preencherEstagiario(estagiario, dto);

        return new EstagiarioResponseDTO(estagiarioRepository.save(estagiario));
    }

    @Transactional
    public void deletar(UUID id) {
        Estagiario estagiario = buscarEstagiarioNoTenant(id);

        // Correção de bug de lógica: "Estagiario" é uma extensão de "Usuario"
        // (mesma tabela de login, herança JOINED) e as duas classes
        // compartilham a coluna "ativo". Antes, este método fazia
        // "estagiario.setAtivo(false)", que é EXATAMENTE o mesmo efeito de
        // desativar a conta de usuário (bloqueando login) — igual ao que
        // acontece em encerrarEstagio(). Ou seja, "excluir o estagiário"
        // (uma ação que devia só encerrar o vínculo de estágio) também
        // desligava a pessoa do sistema inteiro, sem avisar.
        //
        // Aqui só marcamos o fim do estágio (dataFimEstagio), sem tocar em
        // "ativo". Se o objetivo for realmente desativar o login da pessoa,
        // isso deve ser feito de forma explícita chamando encerrarEstagio()
        // (que já documenta esse efeito) ou o endpoint de inativar usuário.
        estagiario.setDataFimEstagio(LocalDate.now());
    }

    private Estagiario buscarEstagiarioNoTenant(UUID id) {
        // Correção de segurança: antes, sem tenant ativo, caía numa busca
        // sem filtro de unidade (findByPublicId), permitindo ler o
        // estagiário de outra unidade só por não enviar o header.
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return estagiarioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));
    }

    private Usuario buscarUsuario(UUID uuid) {
        // Mesma correção de segurança aplicada à busca de usuário.
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return usuarioRepository.findByPublicIdAndUnidadePublicId(uuid, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", uuid));
    }

    private Laboratorio buscarLaboratorio(UUID uuid) {
        return laboratorioRepository.findByPublicId(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", uuid));
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    /**
     * Garante que existe uma unidade (tenant) definida para a requisição
     * atual. Ver o mesmo método em EstoqueCentralService para a explicação
     * completa do porquê essa checagem existe (correção do "modo sem
     * tenant" que vazava dados entre unidades).
     */
    private void exigirTenantAtivo() {
        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        }
    }

    private void validarUnidadeCompativel(Usuario usuario, Laboratorio laboratorio) {
        if (usuario.getUnidade() == null
                || laboratorio.getUnidade() == null
                || !usuario.getUnidade().getId().equals(laboratorio.getUnidade().getId())) {
            throw new BusinessRuleException(
                    "O estagiário e o laboratório devem pertencer à mesma unidade."
            );
        }
    }

    private void preencherEstagiario(Estagiario estagiario, EstagiarioRequestDTO dto) {
        validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());

        estagiario.setDataInicioEstagio(dto.getDataInicioEstagio());
        estagiario.setDataFimEstagio(dto.getDataFimEstagio());
        estagiario.setTipoBolsa(dto.getTipoBolsa());
        estagiario.setObservacao(dto.getObservacao());

        if (dto.getAtivo() != null) {
            estagiario.setAtivo(dto.getAtivo());
        }
    }

    private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
        if (dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new BusinessRuleException(
                    "Data de fim do estágio não pode ser menor que data de início."
            );
        }
    }

    @Transactional
    public EstagiarioResponseDTO encerrarEstagio(UUID id) {
        Estagiario estagiario = buscarEstagiarioNoTenant(id);

        if (!Boolean.TRUE.equals(estagiario.getAtivo())) {
            throw new BusinessRuleException("O estágio já está encerrado.");
        }

        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(estagiario.getDataInicioEstagio())) {
            throw new BusinessRuleException("Não é possível encerrar um estágio antes da data de início.");
        }

        estagiario.setAtivo(false);
        estagiario.setDataFimEstagio(hoje);

        return new EstagiarioResponseDTO(estagiarioRepository.save(estagiario));
    }
}
