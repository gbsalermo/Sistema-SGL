package com.sgl.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.ProdutoResponseDTO;
import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioProdutosService {

    private final ProdutoRepository produtoRepository;

    @Transactional(readOnly = true)
    public RelatorioProdutosResponseDTO gerar(
            Boolean ativo,
            Boolean fiscalizado,
            Boolean perecivel,
            NivelRisco risco,
            OrgaoFiscalizador orgaoFiscalizador) {

        List<Produto> filtrados = produtoRepository.findAll().stream()
                .filter(produto -> ativo == null || Boolean.TRUE.equals(produto.getAtivo()) == ativo)
                .filter(produto -> fiscalizado == null || Boolean.TRUE.equals(produto.getFiscalizado()) == fiscalizado)
                .filter(produto -> perecivel == null || Boolean.TRUE.equals(produto.getPerecivel()) == perecivel)
                .filter(produto -> risco == null || produto.getRisco() == risco)
                .filter(produto -> orgaoFiscalizador == null
                        || produto.getOrgaosFiscalizadores().contains(orgaoFiscalizador))
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int ativos = (int) filtrados.stream().filter(produto -> Boolean.TRUE.equals(produto.getAtivo())).count();
        int inativos = filtrados.size() - ativos;
        int fiscalizados = (int) filtrados.stream().filter(produto -> Boolean.TRUE.equals(produto.getFiscalizado())).count();
        int pereciveis = (int) filtrados.stream().filter(produto -> Boolean.TRUE.equals(produto.getPerecivel())).count();
        int comRisco = (int) filtrados.stream()
                .filter(produto -> produto.getRisco() != null && produto.getRisco() != NivelRisco.NENHUM)
                .count();

        List<ProdutoResponseDTO> itens = filtrados.stream()
                .map(ProdutoResponseDTO::new)
                .toList();

        return new RelatorioProdutosResponseDTO(
                LocalDateTime.now(),
                filtrados.size(),
                ativos,
                inativos,
                fiscalizados,
                pereciveis,
                comRisco,
                itens
        );
    }
}
