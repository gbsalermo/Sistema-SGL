package com.sgl.service;

import java.util.LinkedHashSet;
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
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PapelPolicy;
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
        Set<PapelSistema> papeis = papeisPedidos(dto.getPapeis(), Set.of());
        papeis.forEach(papel -> exigirPodeConceder(CurrentUser.get(), papel));
        if (dto.getLaboratorioIds() != null && !dto.getLaboratorioIds().isEmpty()
                && !PapelPolicy.isAdministrador(CurrentUser.get().papeis())) {
            throw new AccessDeniedException("A lista de laboratórios é organizada pela equipe de cada laboratório.");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("Email já cadastrado: " + dto.getEmail());
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new BusinessRuleException("Senha é obrigatória na criação do usuário.");
        }

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), unidade, papeis, Set.of());

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.substituirPapeis(papeis);
        usuario.setCategoriaCargo(dto.getCategoriaCargo());
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
        // alterarPapeis, Inativar, listarPorLaboratorio) já exigem tenant
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

        Set<PapelSistema> papeis = papeisPedidos(dto.getPapeis(), usuario.getPapeis());
        CategoriaCargo categoria = dto.getCategoriaCargo() != null ? dto.getCategoriaCargo() : usuario.getCategoriaCargo();
        validarEdicao(usuario, dto, papeis, categoria);
        validarSaidaDeEstagiario(usuario, categoria);

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        // A pending or inactive person is not operating, so an empty list is only refused for active ones.
        boolean ficaraAtivo = Boolean.TRUE.equals(dto.getAtivo() != null ? dto.getAtivo() : usuario.getAtivo());
        // Only a change to the list must leave at least one laboratory: someone the responsável removed
        // from their last team can still have their name corrected.
        boolean exigirLista = ficaraAtivo && usuario.getSituacao() != SituacaoUsuario.PENDENTE_VINCULACAO
                && mudaLaboratorios(usuario, dto.getLaboratorioIds());
        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), unidade, papeis, usuario.getLaboratorios(), exigirLista);

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (!usuario.getPapeis().equals(papeis)) {
            AUDIT.info("acao=PAPEIS_ALTERADOS ator={} alvo={} antes={} depois={}",
                    CurrentUser.get().publicId(), usuario.getPublicId(), Set.copyOf(usuario.getPapeis()), papeis);
        }
        usuario.substituirPapeis(papeis);
        usuario.setCategoriaCargo(categoria);
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
    public UsuarioResponseDTO alterarPapeis(UUID id, List<PapelSistema> novosPapeis) {
        Usuario usuario = buscarUsuarioNoTenant(id);
        if (novosPapeis == null) {
            throw new BusinessRuleException("Papéis são obrigatórios.");
        }
        exigirGestao(CurrentUser.get());
        exigirNaoPendente(usuario);
        Set<PapelSistema> papeis = papeisPedidos(novosPapeis, Set.of());
        validarMudancaDeAcesso(usuario, papeis);
        Set<PapelSistema> antes = Set.copyOf(usuario.getPapeis());
        usuario.substituirPapeis(papeis);
        Usuario salvo = usuarioRepository.save(usuario);
        AUDIT.info("acao=PAPEIS_ALTERADOS ator={} alvo={} antes={} depois={}",
                CurrentUser.get().publicId(), salvo.getPublicId(), antes, papeis);
        return new UsuarioResponseDTO(salvo);
    }

    @Transactional
    public UsuarioResponseDTO alterarCategoriaCargo(UUID id, CategoriaCargo novaCategoria) {
        Usuario usuario = buscarUsuarioNoTenant(id);
        if (novaCategoria == null) {
            throw new BusinessRuleException("Categoria de cargo é obrigatória.");
        }
        exigirGestao(CurrentUser.get());
        exigirNaoPendente(usuario);
        // Same authority as for roles: you only adjust people whose access you could have granted.
        validarMudancaDeAcesso(usuario, usuario.getPapeis());
        validarSaidaDeEstagiario(usuario, novaCategoria);
        CategoriaCargo anterior = usuario.getCategoriaCargo();
        usuario.setCategoriaCargo(novaCategoria);
        Usuario salvo = usuarioRepository.save(usuario);
        AUDIT.info("acao=CATEGORIA_ALTERADA ator={} alvo={} de={} para={}",
                CurrentUser.get().publicId(), salvo.getPublicId(), anterior, novaCategoria);
        return new UsuarioResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioSessaoResponseDTO> listarPendentes() {
        exigirAdministrador(CurrentUser.get());
        return usuarioRepository
                .findBySituacaoAndUnidadePublicId(SituacaoUsuario.PENDENTE_VINCULACAO, CurrentUser.unidadeEmOperacao())
                .stream()
                .map(UsuarioSessaoResponseDTO::new)
                .toList();
    }

    @Transactional
    public UsuarioSessaoResponseDTO aprovar(UUID id, AprovarUsuarioRequestDTO dto) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirAdministrador(ator);
        Usuario usuario = buscarPendenteDaUnidadeEmOperacao(id);
        Set<PapelSistema> papeis = papeisPedidos(dto.getPapeis(), Set.of());
        papeis.forEach(papel -> exigirPodeConceder(ator, papel));

        Set<Laboratorio> laboratorios = laboratoriosDaPessoa.resolver(
                dto.getLaboratorioIds(), usuario.getUnidade(), papeis, usuario.getLaboratorios());

        usuario.substituirPapeis(papeis);
        if (dto.getCategoriaCargo() != null) {
            usuario.setCategoriaCargo(dto.getCategoriaCargo());
        }
        // The laboratory list applies to every profile (spec 2.1).
        usuario.substituirLaboratorios(laboratorios);
        usuario.setSituacao(SituacaoUsuario.ATIVO);
        Usuario salvo = usuarioRepository.save(usuario);
        AUDIT.info("acao=USUARIO_APROVADO ator={} alvo={} papeis={} categoria={}", ator.publicId(), salvo.getPublicId(),
                papeis, salvo.getCategoriaCargo());
        return new UsuarioSessaoResponseDTO(salvo);
    }

    @Transactional
    public void recusar(UUID id) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirAdministrador(ator);
        Usuario usuario = buscarPendenteDaUnidadeEmOperacao(id);
        // Removed rather than flagged: a new login simply recreates the pending record (spec 2.2).
        usuarioRepository.delete(usuario);
        AUDIT.info("acao=PENDENTE_RECUSADO ator={} alvo={}", ator.publicId(), usuario.getPublicId());
    }

    @Transactional
    public void Inativar(UUID id) {
        Usuario usuario = buscarUsuarioNoTenant(id);
        // Deactivating removes access just like a demotion, so it follows the same rules.
        validarMudancaDeAcesso(usuario, null);

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
    private static boolean mudaLaboratorios(Usuario usuario, List<UUID> pedidos) {
        Set<UUID> atuais = usuario.getLaboratorios().stream().map(Laboratorio::getPublicId).collect(Collectors.toSet());
        return !atuais.equals(pedidos == null ? Set.of() : new java.util.HashSet<>(pedidos));
    }

    private void validarEdicao(Usuario usuario, UsuarioRequestDTO dto, Set<PapelSistema> papeis,
            CategoriaCargo categoria) {
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
        boolean mudaPapeisOuCategoria = !usuario.getPapeis().equals(papeis)
                || usuario.getCategoriaCargo() != categoria;
        boolean mudaAcesso = mudaPapeisOuCategoria || mudaAtivo
                || mudaLaboratorios
                || !java.util.Objects.equals(unidadeAtual, dto.getUnidadeId());

        // Laboratories are organized by each laboratory's team; centrally, only administrators change the list.
        if (mudaLaboratorios && !PapelPolicy.isAdministrador(ator.papeis())) {
            throw new AccessDeniedException("A lista de laboratórios é organizada pela equipe de cada laboratório.");
        }

        // Pendente só muda de papel, categoria ou laboratório pela aprovação; inativar continua permitido.
        if (mudaPapeisOuCategoria || mudaLaboratorios) {
            exigirNaoPendente(usuario);
        }

        if (mudaAcesso) {
            boolean desativando = mudaAtivo && Boolean.FALSE.equals(dto.getAtivo());
            // Roles added in the same request are checked even when deactivating: "null keeps roles" on a
            // later reactivation would otherwise hand them over without anyone having granted them.
            papeis.stream().filter(papel -> !usuario.temPapel(papel)).forEach(papel -> exigirPodeConceder(ator, papel));
            validarMudancaDeAcesso(usuario, desativando ? null : papeis);
        } else if (!ator.publicId().equals(usuario.getPublicId())
                && !PapelPolicy.podeAlterar(ator.papeis(), usuario.getPapeis())) {
            throw new AccessDeniedException("Seus papéis não permitem alterar este usuário.");
        }
    }

    // Only "Aprovar" takes someone out of PENDENTE_VINCULACAO; any other access change is refused.
    private static void exigirNaoPendente(Usuario alvo) {
        if (alvo.getSituacao() == SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.pendenteExigeAprovacao();
        }
    }

    /**
     * Rules shared by every change that can strip or grant access. {@code novosPapeis} is null when the
     * target is being deactivated instead of having its roles changed.
     */
    private void validarMudancaDeAcesso(Usuario alvo, Set<PapelSistema> novosPapeis) {
        AuthenticatedUser ator = CurrentUser.get();
        if (ator.publicId().equals(alvo.getPublicId())) {
            throw new AccessDeniedException("Ninguém pode alterar os próprios papéis ou acesso.");
        }
        if (!PapelPolicy.podeAlterar(ator.papeis(), alvo.getPapeis())) {
            throw new AccessDeniedException("Seus papéis não permitem alterar um usuário com papéis " + alvo.getPapeis() + ".");
        }
        if (novosPapeis != null) {
            novosPapeis.stream().filter(papel -> !alvo.temPapel(papel)).forEach(papel -> exigirPodeConceder(ator, papel));
        }
        if (!Boolean.TRUE.equals(alvo.getAtivo())) {
            return;
        }
        if (perde(alvo, novosPapeis, PapelSistema.ADMINISTRADOR_GERAL)
                && usuarioRepository.contarAtivosComPapel(PapelSistema.ADMINISTRADOR_GERAL) <= 1) {
            throw UsuarioRejectedException.ultimoAdministradorGeral();
        }
        if (perde(alvo, novosPapeis, PapelSistema.ADMINISTRADOR_LOCAL) && alvo.getUnidade() != null
                && usuarioRepository.contarAtivosComPapelNaUnidade(
                        PapelSistema.ADMINISTRADOR_LOCAL, alvo.getUnidade().getPublicId()) <= 1) {
            throw UsuarioRejectedException.ultimoAdministradorLocal();
        }
    }

    private static boolean perde(Usuario alvo, Set<PapelSistema> novosPapeis, PapelSistema papel) {
        return alvo.temPapel(papel) && (novosPapeis == null || !novosPapeis.contains(papel));
    }

    /** Null keeps {@code atuais}; duplicates collapse because roles are a set. */
    private static Set<PapelSistema> papeisPedidos(List<PapelSistema> pedidos, Set<PapelSistema> atuais) {
        return pedidos == null ? new LinkedHashSet<>(atuais) : new LinkedHashSet<>(pedidos);
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

    // Approving and refusing newcomers moved to each laboratory's team; centrally, administrators only.
    private static void exigirAdministrador(AuthenticatedUser ator) {
        if (!PapelPolicy.isAdministrador(ator.papeis())) {
            throw new AccessDeniedException("Apenas administradores fazem isso fora da equipe do laboratório.");
        }
    }

    private static void exigirGestao(AuthenticatedUser ator) {
        if (!PapelPolicy.isGestao(ator.papeis())) {
            throw new AccessDeniedException("Apenas gestão pode aprovar, recusar ou alterar perfis.");
        }
    }

    private static void exigirPodeConceder(AuthenticatedUser ator, PapelSistema papel) {
        if (!PapelPolicy.podeConceder(ator.papeis(), papel)) {
            throw new AccessDeniedException("Seus papéis não permitem conceder ou remover o papel " + papel + ".");
        }
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    private void validarSaidaDeEstagiario(Usuario usuario, CategoriaCargo novaCategoria) {
        if (usuario.isEstagiario()
                && novaCategoria != CategoriaCargo.ESTAGIARIO
                && estagiarioRepository.existsByIdAndAtivoTrue(usuario.getId())) {
            throw new BusinessRuleException(
                    "Finalize o estágio antes de alterar a categoria do usuário."
            );
        }
    }
}
