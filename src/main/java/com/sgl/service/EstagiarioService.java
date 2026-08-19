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

        if (estagiarioRepository.existsById(usuario.getId())) {
            throw new BusinessRuleException("Usuário já possui cadastro de estagiário.");
        }

        usuario.validateInternProfile();
        validarDatas(dto.getDataInicioEstagio(), dto.getDataFimEstagio());
        validarUnidadeCompativel(usuario, laboratorio);

        usuario.setLaboratorio(laboratorio);
        usuarioRepository.save(usuario);

        // O insert nativo mantém a linha da herança JOINED vinculada ao mesmo id de usuário.
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
        return estagiarioRepository.findAll().stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstagiarioResponseDTO buscarPorId(UUID id) {
        return estagiarioRepository.findByPublicId(id)
                .map(EstagiarioResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));
    }

    @Transactional(readOnly = true)
    public List<EstagiarioResponseDTO> listarPorLaboratorio(UUID id) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));

        return estagiarioRepository.findByLaboratorioId(laboratorio.getId())
                .stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EstagiarioResponseDTO> listarAtivos() {
        return estagiarioRepository.findByAtivoTrue().stream()
                .map(EstagiarioResponseDTO::new)
                .toList();
    }

    @Transactional
    public EstagiarioResponseDTO atualizar(UUID id, EstagiarioRequestDTO dto) {
        Estagiario estagiario = estagiarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));

        if (!id.equals(dto.getUsuarioId())) {
            throw new BusinessRuleException(
                    "Não é permitido trocar o usuário vinculado do estagiário."
            );
        }

        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
        estagiario.validateInternProfile();
        validarUnidadeCompativel(estagiario, laboratorio);

        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setLaboratorio(laboratorio);
        preencherEstagiario(estagiario, dto);

        return new EstagiarioResponseDTO(estagiarioRepository.save(estagiario));
    }

    @Transactional
    public void deletar(UUID id) {
        Estagiario estagiario = estagiarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));

        estagiario.setAtivo(false);
        if (estagiario.getDataFimEstagio() == null) {
            estagiario.setDataFimEstagio(LocalDate.now());
        }
    }

    private Usuario buscarUsuario(UUID uuid) {
        return usuarioRepository.findByPublicId(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", uuid));
    }

    private Laboratorio buscarLaboratorio(UUID uuid) {
        return laboratorioRepository.findByPublicId(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", uuid));
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
        Estagiario estagiario = estagiarioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estagiário", id));

        if (!Boolean.TRUE.equals(estagiario.getAtivo())) {
            throw new BusinessRuleException("O estágio já está encerrado.");
        }

        estagiario.setAtivo(false);
        if (estagiario.getDataFimEstagio() == null) {
            estagiario.setDataFimEstagio(LocalDate.now());
        }

        return new EstagiarioResponseDTO(estagiarioRepository.save(estagiario));
    }
}
