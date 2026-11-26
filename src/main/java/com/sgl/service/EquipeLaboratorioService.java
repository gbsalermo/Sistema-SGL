package com.sgl.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.dto.request.EditarVinculoOperacionalRequestDTO;
import com.sgl.dto.request.ImportarLoteRequestDTO;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.dto.response.UsuarioSessaoResponseDTO;
import com.sgl.dto.response.VinculoOperacionalUsuarioResponseDTO;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.dto.response.MembroEquipeResponseDTO;
import com.sgl.dto.response.PessoaCorporativaResponseDTO;
import com.sgl.dto.response.ResultadoImportacaoDTO;
import com.sgl.dto.response.ResultadoImportacaoDTO.Resultado;
import com.sgl.exception.ImportacaoRejectedException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.exception.UsuarioRejectedException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PapelPolicy;

import lombok.RequiredArgsConstructor;

/**
 * The team of one laboratory: the people who have it in their list, plus those with a running
 * temporary link to it. Managed by the laboratory's responsável or by an administrator of the
 * Unidade; a GESTOR alone does not manage teams.
 */
@Service
@RequiredArgsConstructor
public class EquipeLaboratorioService {

    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");
    private static final Logger LOG = LoggerFactory.getLogger(EquipeLaboratorioService.class);

    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoOperacionalUsuarioRepository vinculoRepository;
    private final PessoaCorporativaService pessoaCorporativaService;
    private final VinculoOperacionalUsuarioService vinculoOperacionalService;
    private final UsuarioService usuarioService;
    private final Clock clock;

    /**
     * Loads the laboratory of the Unidade in operation (404 otherwise) and refuses (403) anyone who is
     * neither an administrator nor the responsável of this active laboratory.
     */
    @Transactional(readOnly = true)
    public Laboratorio exigirGestaoDaEquipe(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(laboratorioId, CurrentUser.unidadeEmOperacao())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
        AuthenticatedUser ator = CurrentUser.get();
        if (PapelPolicy.isAdministrador(ator.papeis())) {
            return laboratorio;
        }
        boolean responsavel = laboratorio.getResponsavel() != null
                && ator.publicId().equals(laboratorio.getResponsavel().getPublicId());
        if (!responsavel || !Boolean.TRUE.equals(laboratorio.getAtivo())) {
            throw new AccessDeniedException("Só o responsável pelo laboratório ou um administrador organiza esta equipe.");
        }
        return laboratorio;
    }

    @Transactional(readOnly = true)
    public List<MembroEquipeResponseDTO> listar(UUID laboratorioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        LocalDate hoje = LocalDate.now(clock);
        Map<Long, Usuario> pessoas = new LinkedHashMap<>();
        usuarioRepository.findByLaboratoriosId(laboratorio.getId()).forEach(u -> pessoas.put(u.getId(), u));
        Map<Long, List<VinculoOperacionalUsuario>> vinculosPorPessoa = new LinkedHashMap<>();
        for (VinculoOperacionalUsuario vinculo : vinculoRepository.findByLaboratorioAndEncerradoFalse(laboratorio)) {
            if (vinculo.vigente(hoje)) {
                pessoas.putIfAbsent(vinculo.getUsuario().getId(), vinculo.getUsuario());
                vinculosPorPessoa.computeIfAbsent(vinculo.getUsuario().getId(), id -> new ArrayList<>()).add(vinculo);
            }
        }
        Long responsavelId = laboratorio.getResponsavel() != null ? laboratorio.getResponsavel().getId() : null;
        return pessoas.values().stream()
                .map(u -> membro(u, laboratorio, vinculosPorPessoa.getOrDefault(u.getId(), List.of()), responsavelId))
                .sorted(Comparator.comparing(MembroEquipeResponseDTO::responsavel).reversed()
                        .thenComparing(MembroEquipeResponseDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public void incluir(UUID laboratorioId, UUID usuarioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        Usuario alvo = alvoDaUnidade(usuarioId);
        if (alvo.getSituacao() == SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.pendenteExigeAprovacao();
        }
        alvo.validateActive();
        if (alvo.atuaNoLaboratorio(laboratorio)) {
            return;
        }
        List<Laboratorio> novos = new ArrayList<>(alvo.getLaboratorios());
        novos.add(laboratorio);
        alvo.substituirLaboratorios(novos);
        usuarioRepository.save(alvo);
        auditar("EQUIPE_INCLUSAO", alvo, laboratorio);
    }

    /**
     * Removing someone means they lose access to this laboratory: it leaves their list and their
     * running temporary links to it are closed today. Their other laboratories are untouched, and
     * removing the last one is allowed (the person stays active, waiting for another team).
     */
    @Transactional
    public void retirar(UUID laboratorioId, UUID usuarioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        Usuario alvo = alvoDaUnidade(usuarioId);
        if (laboratorio.getResponsavel() != null && alvo.getId().equals(laboratorio.getResponsavel().getId())) {
            throw UsuarioRejectedException.responsavelNaEquipe();
        }
        LocalDate hoje = LocalDate.now(clock);
        List<VinculoOperacionalUsuario> vinculos = vinculoRepository
                .findByLaboratorioAndUsuarioAndEncerradoFalse(laboratorio, alvo);
        boolean naLista = alvo.atuaNoLaboratorio(laboratorio);
        if (!naLista && vinculos.isEmpty()) {
            throw new ResourceNotFoundException("Pessoa na equipe do laboratório", usuarioId);
        }
        if (naLista) {
            alvo.substituirLaboratorios(alvo.getLaboratorios().stream()
                    .filter(item -> !Objects.equals(item.getId(), laboratorio.getId()))
                    .toList());
            usuarioRepository.save(alvo);
        }
        vinculos.forEach(vinculo -> vinculo.encerrarEm(hoje));
        vinculoRepository.saveAll(vinculos);
        auditar("EQUIPE_RETIRADA", alvo, laboratorio);
    }

    public List<PessoaCorporativaResponseDTO> candidatos(UUID laboratorioId, TipoPessoaCorporativa tipo, String termo) {
        exigirGestaoDaEquipe(laboratorioId);
        return pessoaCorporativaService.candidatos(tipo, termo);
    }

    /**
     * Registers each person on their own, so one failure never undoes the others, and reports the
     * outcome person by person. Not transactional on purpose.
     */
    public List<ResultadoImportacaoDTO> importar(UUID laboratorioId, ImportarLoteRequestDTO lote) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        List<ResultadoImportacaoDTO> resultados = new ArrayList<>();
        for (ImportarLoteRequestDTO.ItemImportacao item : lote.itens()) {
            resultados.add(importarUm(item, laboratorio));
        }
        return resultados;
    }

    private ResultadoImportacaoDTO importarUm(ImportarLoteRequestDTO.ItemImportacao item, Laboratorio laboratorio) {
        try {
            Usuario usuario = pessoaCorporativaService.importarParaLaboratorio(
                    item.tipo(), item.referencia(), item.categoriaCargo(), laboratorio);
            return new ResultadoImportacaoDTO(item.referencia(), usuario.getNome(), Resultado.CADASTRADA, null);
        } catch (ImportacaoRejectedException e) {
            Resultado resultado = ImportacaoRejectedException.JA_CADASTRADO.equals(e.getCodigo())
                    ? Resultado.JA_CADASTRADA : Resultado.ERRO;
            return new ResultadoImportacaoDTO(item.referencia(), null, resultado, e.getMessage());
        } catch (ResourceNotFoundException e) {
            return new ResultadoImportacaoDTO(item.referencia(), null, Resultado.NAO_ENCONTRADA, e.getMessage());
        } catch (RuntimeException e) {
            // Gateway outages and business refusals are reported for this person only; the batch goes on.
            LOG.warn("Importação em lote: falha em uma pessoa: {}", e.getClass().getSimpleName());
            return new ResultadoImportacaoDTO(item.referencia(), null, Resultado.ERRO, e.getMessage());
        }
    }

    public List<UsuarioSessaoResponseDTO> listarPendentes(UUID laboratorioId) {
        exigirGestaoDaEquipe(laboratorioId);
        return usuarioService.listarPendentesParaEquipe();
    }

    public UsuarioSessaoResponseDTO aprovarPendente(UUID laboratorioId, UUID usuarioId, CategoriaCargo categoria) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        return usuarioService.aprovarNaEquipe(usuarioId, laboratorio, categoria);
    }

    public List<VinculoOperacionalUsuarioResponseDTO> listarVinculos(UUID laboratorioId) {
        return vinculoOperacionalService.listarDaEquipe(exigirGestaoDaEquipe(laboratorioId));
    }

    public VinculoOperacionalUsuarioResponseDTO criarVinculo(UUID laboratorioId, VinculoOperacionalUsuarioRequestDTO dto) {
        return vinculoOperacionalService.criarNaEquipe(exigirGestaoDaEquipe(laboratorioId), dto);
    }

    public VinculoOperacionalUsuarioResponseDTO editarVinculo(UUID laboratorioId, UUID vinculoId,
            EditarVinculoOperacionalRequestDTO dto) {
        return vinculoOperacionalService.editarNaEquipe(exigirGestaoDaEquipe(laboratorioId), vinculoId, dto);
    }

    public VinculoOperacionalUsuarioResponseDTO encerrarVinculo(UUID laboratorioId, UUID vinculoId) {
        return vinculoOperacionalService.encerrarNaEquipe(exigirGestaoDaEquipe(laboratorioId), vinculoId);
    }

    private Usuario alvoDaUnidade(UUID usuarioId) {
        if (CurrentUser.get().publicId().equals(usuarioId)) {
            throw new AccessDeniedException("Ninguém altera o próprio acesso à equipe.");
        }
        return usuarioRepository.findByPublicIdAndUnidadePublicId(usuarioId, CurrentUser.unidadeEmOperacao())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
    }

    private void auditar(String acao, Usuario alvo, Laboratorio laboratorio) {
        AUDIT.info("acao={} ator={} alvo={} laboratorio={}", acao, CurrentUser.get().publicId(),
                alvo.getPublicId(), laboratorio.getPublicId());
    }

    private static MembroEquipeResponseDTO membro(Usuario usuario, Laboratorio laboratorio,
            List<VinculoOperacionalUsuario> vinculos, Long responsavelId) {
        boolean semFim = vinculos.stream().anyMatch(v -> v.getDataFim() == null);
        LocalDate ate = semFim ? null : vinculos.stream()
                .map(VinculoOperacionalUsuario::getDataFim)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new MembroEquipeResponseDTO(usuario.getPublicId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPapeis(), usuario.getCategoriaCargo(), usuario.getCargo(),
                usuario.atuaNoLaboratorio(laboratorio), ate, usuario.getId().equals(responsavelId));
    }
}
