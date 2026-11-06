package com.sgl.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AprovarUsuarioRequestDTO;
import com.sgl.dto.request.UsuarioRequestDTO;
import com.sgl.dto.response.UsuarioResponseDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.exception.UsuarioRejectedException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");

    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UnidadeRepository unidadeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EstagiarioRepository estagiarioRepository;
    private final LaboratoriosDaPessoa laboratoriosDaPessoa;

    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        validarTenantUnidade(dto.getUnidadeId());
        exigirPodeConceder(CurrentUser.get(), dto.getPerfil());

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado: " + dto.getEmail());
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new BusinessRuleException("Senha é obrigatória na criação do usuário.");
        }

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), unidade, dto.getPerfil(), Set.of());

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(dto.getPerfil());
        usuario.setUnidade(unidade);
        usuario.substituirLaboratorios(laboratorios);
        usuario.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        return new UsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        // ATENÇÃO — exceção deliberada, ligada ao achado de segurança #1
        // (ainda não existe autenticação real no sistema): o "login de
        // desenvolvimento" do frontend (sgl-web/src/stores/session.ts,
        // entrarDesenvolvimento()) precisa listar usuários de TODAS as
        // unidades, sem header de tenant, para descobrir a qual unidade o
        // usuário pertence antes de existir qualquer sessão. Por isso este
        // método específico continua permitindo listagem sem tenant.
        //
        // Todos os outros métodos deste service (buscarPorId, atualizar,
        // alterarPerfil, Inativar, listarPorLaboratorio) já exigem tenant
        // normalmente. Quando a autenticação real for implementada, este
        // método deve ser substituído por um endpoint de login dedicado que
        // não exponha a lista completa de usuários de todas as unidades.
        List<Usuario> usuarios = TenantContext.unidadeAtual()
                .map(usuarioRepository::findByUnidadePublicId)
                .orElseGet(usuarioRepository::findAll);

        return usuarios.stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

        return usuarioRepository.findByLaboratoriosId(laboratorio.getId()).stream()
                .map(UsuarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(UUID id) {
        return new UsuarioResponseDTO(buscarUsuarioNoTenant(id));
    }

    @Transactional
    public UsuarioResponseDTO atualizar(UUID id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarUsuarioNoTenant(id);
        validarTenantUnidade(dto.getUnidadeId());

        if (usuarioRepository.existsByEmailAndIdNot(dto.getEmail(), usuario.getId())) {
            throw new BusinessRuleException("Já existe um usuário com este email.");
        }

        validarEdicao(usuario, dto);
        validarAlteracaoPerfil(usuario, dto.getPerfil());

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        // A pending or inactive person is not operating, so an empty list is only refused for active ones.
        boolean ficaraAtivo = Boolean.TRUE.equals(dto.getAtivo() != null ? dto.getAtivo() : usuario.getAtivo());
        boolean exigirLista = ficaraAtivo && usuario.getSituacao() != SituacaoUsuario.PENDENTE_VINCULACAO;
        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), unidade, dto.getPerfil(), usuario.getLaboratorios(), exigirLista);

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setPerfil(dto.getPerfil());
        usuario.setUnidade(unidade);
        usuario.substituirLaboratorios(laboratorios);

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        return new UsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO alterarPerfil(UUID id, Perfil novoPerfil) {
        Usuario usuario = buscarUsuarioNoTenant(id);

        if (novoPerfil == null) {
            throw new BusinessRuleException("Perfil é obrigatório.");
        }

        exigirNaoPendente(usuario);
        validarTrocaDePerfil(usuario, novoPerfil);
        validarAlteracaoPerfil(usuario, novoPerfil);
        Perfil anterior = usuario.getPerfil();
        usuario.setPerfil(novoPerfil);
        Usuario salvo = usuarioRepository.save(usuario);
        AUDIT.info("acao=PERFIL_ALTERADO ator={} alvo={} de={} para={}",
                CurrentUser.get().publicId(), salvo.getPublicId(), anterior, novoPerfil);
        return new UsuarioResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioSessaoResponseDTO> listarPendentes() {
        exigirGestao(CurrentUser.get());
        return usuarioRepository
                .findBySituacaoAndUnidadePublicId(SituacaoUsuario.PENDENTE_VINCULACAO, CurrentUser.unidadeEmOperacao())
                .stream()
                .map(UsuarioSessaoResponseDTO::new)
                .toList();
    }

    @Transactional
    public UsuarioSessaoResponseDTO aprovar(UUID id, AprovarUsuarioRequestDTO dto) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirGestao(ator);
        Usuario usuario = buscarPendenteDaUnidadeEmOperacao(id);
        exigirPodeConceder(ator, dto.getPerfil());

        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), usuario.getUnidade(), dto.getPerfil(), usuario.getLaboratorios());

        usuario.setPerfil(dto.getPerfil());
        // The laboratory list applies to every profile (spec 2.1).
        usuario.substituirLaboratorios(laboratorios);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        Usuario salvo = usuarioRepository.save(usuario);
        AUDIT.info("acao=USUARIO_APROVADO ator={} alvo={} perfil={}", ator.publicId(), salvo.getPublicId(), dto.getPerfil());
        return new UsuarioSessaoResponseDTO(salvo);
    }

    @Transactional
    public void recusar(UUID id) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirGestao(ator);
        Usuario usuario = buscarPendenteDaUnidadeEmOperacao(id);
        // Removed rather than flagged: a new login simply recreates the pending record (spec 2.2).
        usuarioRepository.delete(usuario);
        AUDIT.info("acao=PENDENTE_RECUSADO ator={} alvo={}", ator.publicId(), usuario.getPublicId());
    }

    @Transactional
    public void Inativar(UUID id) {
        Usuario usuario = buscarUsuarioNoTenant(id);
        // Deactivating removes access just like a demotion, so it follows the same rules.
        validarTrocaDePerfil(usuario, null);

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException("O usuário já está inativo.");
        }

        usuario.setAtivo(false);
    }

    private Usuario buscarUsuarioNoTenant(UUID id) {
        // Correção de segurança: antes, sem tenant ativo, buscava sem
        // filtro de unidade (findByPublicId), vazando o usuário de outra
        // unidade para quem não enviasse o header. Diferente de
        // listarTodos() acima, aqui NÃO existe motivo de login para manter
        // esse método aberto — a busca por id específico é sempre exigida.
        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        }

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return usuarioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    /**
     * The general update carries profile, active flag, Unidade and laboratory list, so it must not bypass the
     * rules of the dedicated routes. Plain data (name, e-mail) still needs authority over the target.
     */
    private void validarEdicao(Usuario usuario, UsuarioRequestDTO dto) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirGestao(ator);
        boolean mudaAtivo = dto.getAtivo() != null && !dto.getAtivo().equals(usuario.getAtivo());
        Set<UUID> laboratoriosAtuais = usuario.getLaboratorios().stream()
                .map(Laboratorio::getPublicId)
                .collect(Collectors.toSet());
        Set<UUID> laboratoriosPedidos = dto.getLaboratorioIds() == null
                ? Set.of() : new java.util.HashSet<>(dto.getLaboratorioIds());
        boolean mudaLaboratorios = !laboratoriosAtuais.equals(laboratoriosPedidos);
        UUID unidadeAtual = usuario.getUnidade() != null ? usuario.getUnidade().getPublicId() : null;
        boolean mudaAcesso = usuario.getPerfil() != dto.getPerfil() || mudaAtivo
                || mudaLaboratorios
                || !java.util.Objects.equals(unidadeAtual, dto.getUnidadeId());

        // Pendente só muda de perfil/laboratório pela aprovação; inativar continua permitido.
        boolean mudaPerfilOuLaboratorio = usuario.getPerfil() != dto.getPerfil() || mudaLaboratorios;
        if (mudaPerfilOuLaboratorio) {
            exigirNaoPendente(usuario);
        }

        if (mudaAcesso) {
            boolean desativando = mudaAtivo && Boolean.FALSE.equals(dto.getAtivo());
            validarTrocaDePerfil(usuario, desativando ? null : dto.getPerfil());
        } else if (!ator.publicId().equals(usuario.getPublicId())) {
            exigirPodeConceder(ator, usuario.getPerfil());
        }
    }

    /**
     * Rules shared by every change that can strip or grant access. {@code novoPerfil} is null when the
     * target is being deactivated instead of re-profiled.
     */
    private static void exigirNaoPendente(Usuario alvo) {
        if (alvo.getSituacao() == SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.pendenteExigeAprovacao();
        }
    }

    private void validarTrocaDePerfil(Usuario alvo, Perfil novoPerfil) {
        AuthenticatedUser ator = CurrentUser.get();
        if (ator.publicId().equals(alvo.getPublicId())) {
            throw new AccessDeniedException("Ninguém pode alterar o próprio perfil ou acesso.");
        }
        exigirPodeConceder(ator, alvo.getPerfil());
        if (novoPerfil != null) {
            exigirPodeConceder(ator, novoPerfil);
        }
        boolean deixaDeSerAdministradorGeral = alvo.getPerfil() == Perfil.ADMINISTRADOR_GERAL
                && novoPerfil != Perfil.ADMINISTRADOR_GERAL;
        if (deixaDeSerAdministradorGeral && Boolean.TRUE.equals(alvo.getAtivo())
                && usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR_GERAL) <= 1) {
            throw UsuarioRejectedException.ultimoAdministradorGeral();
        }
    }

    // Looked up across Unidades on purpose: a target from another Unidade is a 403, not a 404 (spec 15).
    private Usuario buscarPendenteDaUnidadeEmOperacao(UUID id) {
        Usuario usuario = usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
        UUID unidadeEmOperacao = CurrentUser.unidadeEmOperacao();
        if (usuario.getUnidade() == null || !unidadeEmOperacao.equals(usuario.getUnidade().getPublicId())) {
            throw new AccessDeniedException("O usuário não pertence à Unidade em operação.");
        }
        if (usuario.getSituacao() != SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.naoPendente();
        }
        return usuario;
    }

    private static void exigirGestao(AuthenticatedUser ator) {
        if (!PerfilPolicy.isGestao(ator.perfil())) {
            throw new AccessDeniedException("Apenas gestão pode aprovar, recusar ou alterar perfis.");
        }
    }

    private static void exigirPodeConceder(AuthenticatedUser ator, Perfil perfil) {
        if (!PerfilPolicy.podeConceder(ator.perfil(), perfil)) {
            throw new AccessDeniedException("Seu perfil não pode conceder ou alterar o perfil " + perfil + ".");
        }
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    private void validarAlteracaoPerfil(Usuario usuario, Perfil novoPerfil) {
        if (usuario.getPerfil() == Perfil.ESTAGIARIO
                && novoPerfil != Perfil.ESTAGIARIO
                && estagiarioRepository.existsByIdAndAtivoTrue(usuario.getId())) {
            throw new BusinessRuleException(
                    "Finalize o estágio antes de alterar o perfil do usuário."
            );
        }
    }
}
