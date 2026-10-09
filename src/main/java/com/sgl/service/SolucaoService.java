package com.sgl.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.SolucaoComponenteRequestDTO;
import com.sgl.dto.request.SolucaoRequestDTO;
import com.sgl.dto.response.SolucaoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Produto;
import com.sgl.model.Solucao;
import com.sgl.model.SolucaoComponente;
import com.sgl.model.Unidade;
import com.sgl.model.medida.ConversorUnidadeMedida;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.SolucaoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolucaoService {

    private final SolucaoRepository solucaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public SolucaoResponseDTO criar(SolucaoRequestDTO dto) {
        exigirTenantAtivo();

        Unidade unidade = buscarUnidade(dto.getUnidadeId());
        validarTenantUnidade(unidade.getPublicId());

        String nome = normalizarObrigatorio(dto.getNome());
        validarNomeDuplicado(unidade, nome, null);

        Solucao solucao = Solucao.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
                .build();

        preencherDados(solucao, dto);
        substituirComponentes(solucao, dto.getComponentes());

        return new SolucaoResponseDTO(solucaoRepository.save(solucao));
    }

    @Transactional(readOnly = true)
    public List<SolucaoResponseDTO> listarTodos() {
        exigirTenantAtivo();
        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

        return solucaoRepository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream()
                .map(SolucaoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolucaoResponseDTO> listarAtivas() {
        exigirTenantAtivo();
        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

        return solucaoRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
                .map(SolucaoResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolucaoResponseDTO buscarPorId(UUID id) {
        return new SolucaoResponseDTO(buscarSolucaoNoTenant(id));
    }

    @Transactional
    public SolucaoResponseDTO atualizar(UUID id, SolucaoRequestDTO dto) {
        Solucao solucao = buscarSolucaoNoTenant(id);

        if (!solucao.getUnidade().getPublicId().equals(dto.getUnidadeId())) {
            throw new BusinessRuleException("A solução não pode ser transferida para outra unidade.");
        }

        String nome = normalizarObrigatorio(dto.getNome());
        validarNomeDuplicado(solucao.getUnidade(), nome, solucao.getId());

        solucao.setNome(nome);
        preencherDados(solucao, dto);
        substituirComponentes(solucao, dto.getComponentes());

        if (dto.getAtivo() != null) {
            solucao.setAtivo(dto.getAtivo());
        }

        return new SolucaoResponseDTO(solucaoRepository.save(solucao));
    }

    @Transactional
    public void deletar(UUID id) {
        Solucao solucao = buscarSolucaoNoTenant(id);
        solucao.setAtivo(false);
    }

    private void preencherDados(Solucao solucao, SolucaoRequestDTO dto) {
        validarQuantidadePositiva(dto.getRendimentoQuantidade(), "Rendimento");

        solucao.setDescricao(normalizarOpcional(dto.getDescricao()));
        solucao.setRendimentoQuantidade(dto.getRendimentoQuantidade());
        solucao.setRendimentoUnidade(dto.getRendimentoUnidade());
        solucao.setConcentracao(normalizarOpcional(dto.getConcentracao()));
        solucao.setInstrucoesPreparo(normalizarOpcional(dto.getInstrucoesPreparo()));
    }

    private void substituirComponentes(
            Solucao solucao,
            List<SolucaoComponenteRequestDTO> componentesDTO) {

        if (componentesDTO == null || componentesDTO.isEmpty()) {
            throw new BusinessRuleException("Informe ao menos um componente da solução.");
        }

        Set<UUID> produtosAdicionados = new HashSet<>();
        List<SolucaoComponente> novosComponentes = new java.util.ArrayList<>();

        for (int ordem = 0; ordem < componentesDTO.size(); ordem++) {
            novosComponentes.add(montarComponente(
                    solucao,
                    componentesDTO.get(ordem),
                    produtosAdicionados,
                    ordem
            ));
        }

        solucao.limparComponentes();
        novosComponentes.forEach(solucao::adicionarComponente);
    }

    private SolucaoComponente montarComponente(
            Solucao solucao,
            SolucaoComponenteRequestDTO dto,
            Set<UUID> produtosAdicionados,
            int ordem) {

        if (!produtosAdicionados.add(dto.getProdutoId())) {
            throw new BusinessRuleException(
                    "O mesmo produto não pode aparecer mais de uma vez na composição da solução."
            );
        }

        validarQuantidadePositiva(dto.getQuantidade(), "Quantidade do componente");

        Produto produto = buscarProdutoDaUnidade(
                dto.getProdutoId(),
                solucao.getUnidade().getPublicId()
        );

        if (!dto.getUnidadeMedida().compativelCom(produto.getUnidadeMedida())) {
            throw new BusinessRuleException(
                    "A unidade " + dto.getUnidadeMedida()
                            + " não é compatível com a unidade canônica "
                            + produto.getUnidadeMedida()
                            + " do produto " + produto.getNome() + "."
            );
        }

        ConversorUnidadeMedida.converterParaCanonica(
                dto.getQuantidade(),
                dto.getUnidadeMedida(),
                produto.getUnidadeMedida()
        );

        return SolucaoComponente.builder()
                .solucao(solucao)
                .produto(produto)
                .ordem(ordem)
                .quantidade(dto.getQuantidade())
                .unidadeMedida(dto.getUnidadeMedida())
                .build();
    }

    private Solucao buscarSolucaoNoTenant(UUID id) {
        exigirTenantAtivo();
        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

        return solucaoRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Solução", id));
    }

    private Produto buscarProdutoDaUnidade(UUID produtoId, UUID unidadeId) {
        Produto produto = produtoRepository.findByPublicId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        if (!produtoRepository.pertenceAUnidade(produtoId, unidadeId)) {
            throw new ResourceNotFoundException("Produto", produtoId);
        }

        produto.validateActive();
        return produto;
    }

    private Unidade buscarUnidade(UUID unidadeId) {
        return unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
    }

    private void validarNomeDuplicado(Unidade unidade, String nome, Long idAtual) {
        boolean duplicado = idAtual == null
                ? solucaoRepository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome)
                : solucaoRepository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(
                        unidade.getId(),
                        nome,
                        idAtual
                );

        if (duplicado) {
            throw new BusinessRuleException("Já existe uma solução com este nome na unidade.");
        }
    }

    private void validarQuantidadePositiva(BigDecimal quantidade, String campo) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(campo + " deve ser maior que zero.");
        }
    }

    private String normalizarObrigatorio(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private void validarTenantUnidade(UUID unidadeId) {
        exigirTenantAtivo();

        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    private void exigirTenantAtivo() {
        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação."
            );
        }
    }
}
