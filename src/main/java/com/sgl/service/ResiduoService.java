package com.sgl.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AnalisarResiduoRequestDTO;
import com.sgl.dto.request.ArmazenarResiduoRequestDTO;
import com.sgl.dto.request.ComponenteResiduoRequestDTO;
import com.sgl.dto.request.CriarResiduoRequestDTO;
import com.sgl.dto.request.DespacharResiduoRequestDTO;
import com.sgl.dto.request.ReceberResiduoRequestDTO;
import com.sgl.dto.response.HistoricoResiduoResponseDTO;
import com.sgl.dto.response.ResiduoResponseDTO;
import com.sgl.dto.response.RotuloResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ComponenteResiduo;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.Laboratorio;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Residuo;
import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.repository.HistoricoResiduoRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResiduoService {

    private final ResiduoRepository residuoRepository;
    private final HistoricoResiduoRepository historicoResiduoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final ProjetoRepository projetoRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public ResiduoResponseDTO criar(CriarResiduoRequestDTO dto) {
        Usuario gerador = buscarUsuario(dto.getUsuarioGeradorId());
        gerador.validateActive();

        Laboratorio laboratorio = laboratorioRepository.findByPublicId(dto.getLaboratorioId())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", dto.getLaboratorioId()));
        laboratorio.validateActive();
        validarGeradorNoLaboratorio(gerador, laboratorio);

        Projeto projeto = buscarProjeto(dto.getProjetoId(), laboratorio);

        Residuo residuo = Residuo.builder()
                .laboratorio(laboratorio)
                .gerador(gerador)
                .projeto(projeto)
                .descricao(dto.getDescricao())
                .processoOrigem(dto.getProcessoOrigem())
                .recipiente(dto.getRecipiente())
                .quantidade(dto.getQuantidade())
                .unidadeMedida(dto.getUnidadeMedida())
                .nivelRiscoInformado(dto.getNivelRiscoInformado())
                .riscosInformados(new LinkedHashSet<>(dto.getRiscosInformados()))
                .observacaoGerador(dto.getObservacaoGerador())
                .status(StatusResiduo.INFORMADO)
                .dataInformacao(LocalDateTime.now())
                .build();

        dto.getComponentes().forEach(item -> residuo.addComponente(criarComponente(item)));

        Residuo salvo = residuoRepository.save(residuo);
        registrarHistorico(
                salvo,
                gerador,
                "RESIDUO_INFORMADO",
                dto.getObservacaoGerador()
        );

        return new ResiduoResponseDTO(salvo);
    }

    @Transactional
    public ResiduoResponseDTO receber(UUID id, ReceberResiduoRequestDTO dto) {
        Residuo residuo = buscarEntidade(id);
        Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

        residuo.receber(gestor, dto.getObservacao());
        Residuo salvo = residuoRepository.save(residuo);
        registrarHistorico(salvo, gestor, "RECEBIDO_PELA_GESTAO", dto.getObservacao());

        return new ResiduoResponseDTO(salvo);
    }

    @Transactional
    public ResiduoResponseDTO analisarELiberar(UUID id, AnalisarResiduoRequestDTO dto) {
        Residuo residuo = buscarEntidade(id);
        Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

        residuo.liberarParaArmazenamento(
                gestor,
                dto.getNivelRiscoConfirmado(),
                dto.getRiscosConfirmados(),
                dto.getLocalArmazenamentoTemporario(),
                dto.getDestinoFinalPrevisto(),
                dto.getDataPrevistaDespacho(),
                dto.getObservacaoGestor()
        );

        if (residuo.getCodigoRastreio() == null) {
            residuo.setCodigoRastreio(gerarCodigoRastreio(residuo));
            residuo.setQrCodeConteudo("SGL-RESIDUO:" + residuo.getPublicId());
        }

        Residuo salvo = residuoRepository.save(residuo);
        registrarHistorico(
                salvo,
                gestor,
                "RISCO_CONFERIDO_E_RESIDUO_LIBERADO",
                dto.getObservacaoGestor()
        );

        return new ResiduoResponseDTO(salvo);
    }

    @Transactional
    public ResiduoResponseDTO confirmarArmazenamento(UUID id, ArmazenarResiduoRequestDTO dto) {
        Residuo residuo = buscarEntidade(id);
        Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

        residuo.confirmarArmazenamento(gestor, dto.getLocalArmazenamentoTemporario());
        Residuo salvo = residuoRepository.save(residuo);
        registrarHistorico(
                salvo,
                gestor,
                "ARMAZENAMENTO_TEMPORARIO_CONFIRMADO",
                salvo.getLocalArmazenamentoTemporario()
        );

        return new ResiduoResponseDTO(salvo);
    }

    @Transactional
    public ResiduoResponseDTO despachar(UUID id, DespacharResiduoRequestDTO dto) {
        Residuo residuo = buscarEntidade(id);
        Usuario gestor = buscarUsuarioGestao(dto.getUsuarioGestorId());

        residuo.confirmarDespacho(
                gestor,
                dto.getDestinoFinalConfirmado(),
                dto.getObservacao()
        );

        Residuo salvo = residuoRepository.save(residuo);
        registrarHistorico(
                salvo,
                gestor,
                "DESPACHO_CONFIRMADO",
                dto.getDestinoFinalConfirmado()
        );

        return new ResiduoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public ResiduoResponseDTO buscarPorId(UUID id) {
        return new ResiduoResponseDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<ResiduoResponseDTO> listarTodos() {
        return residuoRepository.findAllByOrderByDataInformacaoDesc().stream()
                .map(ResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResiduoResponseDTO> listarPorStatus(StatusResiduo status) {
        return residuoRepository.findByStatusOrderByDataInformacaoDesc(status).stream()
                .map(ResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResiduoResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        return residuoRepository
                .findByLaboratorioPublicIdOrderByDataInformacaoDesc(laboratorioId)
                .stream()
                .map(ResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResiduoResponseDTO> listarPorGerador(UUID usuarioGeradorId) {
        Usuario gerador = buscarUsuario(usuarioGeradorId);
        gerador.validateActive();

        return residuoRepository
                .findByGeradorPublicIdOrderByDataInformacaoDesc(usuarioGeradorId)
                .stream()
                .map(ResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoResiduoResponseDTO> buscarHistorico(UUID id) {
        Residuo residuo = buscarEntidade(id);
        return historicoResiduoRepository.findByResiduoIdOrderByDataHoraAsc(residuo.getId())
                .stream()
                .map(HistoricoResiduoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public RotuloResiduoResponseDTO gerarDadosRotulo(UUID id) {
        Residuo residuo = buscarEntidade(id);
        residuo.validateLabelAvailable();
        return new RotuloResiduoResponseDTO(residuo);
    }

    private Residuo buscarEntidade(UUID id) {
        return residuoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resíduo", id));
    }

    private Usuario buscarUsuario(UUID id) {
        return usuarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", id));
    }

    private Usuario buscarUsuarioGestao(UUID id) {
        Usuario usuario = buscarUsuario(id);
        usuario.validateActive();

        if (usuario.getPerfil() != Perfil.GESTOR && usuario.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new BusinessRuleException(
                    "A operação de gestão de resíduos exige perfil GESTOR ou ADMINISTRADOR."
            );
        }

        return usuario;
    }

    private Projeto buscarProjeto(UUID projetoId, Laboratorio laboratorio) {
        if (projetoId == null) {
            return null;
        }

        Projeto projeto = projetoRepository.findByPublicId(projetoId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));
        projeto.validateActive();

        if (!projeto.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException(
                    "O projeto informado não pertence ao laboratório gerador do resíduo."
            );
        }

        return projeto;
    }

    private void validarGeradorNoLaboratorio(Usuario gerador, Laboratorio laboratorio) {
        if (gerador.getLaboratorio() != null
                && !gerador.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException(
                    "O usuário gerador não pertence ao laboratório informado."
            );
        }
    }

    private ComponenteResiduo criarComponente(ComponenteResiduoRequestDTO dto) {
        Produto produto = null;
        String nomeComponente = dto.getNomeComponente();

        if (dto.getProdutoId() != null) {
            produto = produtoRepository.findByPublicId(dto.getProdutoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produto", dto.getProdutoId()));

            if (nomeComponente == null || nomeComponente.isBlank()) {
                nomeComponente = produto.getNome();
            }
        }

        if (nomeComponente == null || nomeComponente.isBlank()) {
            throw new BusinessRuleException(
                    "O componente do resíduo deve possuir nome ou referência de produto."
            );
        }

        return ComponenteResiduo.builder()
                .produto(produto)
                .nomeComponente(nomeComponente)
                .principal(Boolean.TRUE.equals(dto.getPrincipal()))
                .concentracaoOuQuantidade(dto.getConcentracaoOuQuantidade())
                .observacao(dto.getObservacao())
                .build();
    }

    private String gerarCodigoRastreio(Residuo residuo) {
        int ano = residuo.getDataInformacao().getYear();
        return String.format("SGL-RES-%d-%06d", ano, residuo.getId());
    }

    private void registrarHistorico(
            Residuo residuo,
            Usuario usuario,
            String acao,
            String observacao) {

        historicoResiduoRepository.save(
                HistoricoResiduo.builder()
                        .residuo(residuo)
                        .usuario(usuario)
                        .status(residuo.getStatus())
                        .acao(acao)
                        .observacao(observacao)
                        .dataHora(LocalDateTime.now())
                        .build()
        );
    }
}
