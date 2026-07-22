package com.sgl.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sgl.model.Laboratorio;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;

    @Override
    public void run(String... args) throws Exception {
        // Unidades
        Unidade u1 = unidadeRepository.save(new Unidade(null, "Instituto de Biologia", "IB", null));
        Unidade u2 = unidadeRepository.save(new Unidade(null, "Instituto de Fisica", "IF", null));
        Unidade u3 = unidadeRepository.save(new Unidade(null, "Instituto de Quimica", "IQ", null));

        // Laboratórios (sem responsável ainda)
        Laboratorio lab1 = laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Microbiologia", "Lab de estudo de microrganismos", null, true));
        Laboratorio lab2 = laboratorioRepository.save(new Laboratorio(null, u1, "Laboratorio de Genetica", "Lab de analise genetica", null, true));
        Laboratorio lab3 = laboratorioRepository.save(new Laboratorio(null, u2, "Laboratorio de Optica", "Lab de estudo da luz", null, true));
        Laboratorio lab4 = laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Quimica Organica", "Lab de sintese organica", null, true));
        Laboratorio lab5 = laboratorioRepository.save(new Laboratorio(null, u3, "Laboratorio de Analise Instrumental", "Lab de instrumentacao analitica", null, false));

        // Usuários
        Usuario admin = usuarioRepository.save(new Usuario(null, "Admin Sistema", "admin@sgl.com", "123456", Perfil.ADMINISTRADOR, u1, lab5, true));
        Usuario carlos = usuarioRepository.save(new Usuario(null, "Dr. Carlos Silva", "carlos@ib.com", "123456", Perfil.GESTOR, u3, lab1, true));
        Usuario ana = usuarioRepository.save(new Usuario(null, "Dra. Ana Santos", "ana@ib.com", "123456", Perfil.TECNICO, u2, lab2, true));
        Usuario joao = usuarioRepository.save(new Usuario(null, "Joao Pereira", "joao@if.com", "123456", Perfil.PESQUISADOR, u1, lab3, true));
        Usuario maria = usuarioRepository.save(new Usuario(null, "Maria Oliveira", "maria@iq.com", "123456", Perfil.ESTAGIARIO, u2, lab4, true));

        // Atualizar laboratórios com os responsáveis
        lab1.setResponsavel(carlos);
        lab2.setResponsavel(ana);
        lab3.setResponsavel(joao);
        lab4.setResponsavel(maria);
        lab5.setResponsavel(admin);

        laboratorioRepository.save(lab1);
        laboratorioRepository.save(lab2);
        laboratorioRepository.save(lab3);
        laboratorioRepository.save(lab4);
        laboratorioRepository.save(lab5);

        // Produtos (catálogo central)
        Produto p1 = produtoRepository.save(Produto.builder()
                .nome("Alcool Etílico 70%")
                .descricao("Alcool etílico para desinfecção de superfícies")
                .codigoReferencia("ALC-ETI-70")
                .unidadeMedida(UnidadeMedida.L)
                .localizacaoFisica("Prateleira A1 - Sala de Reagentes")
                .risco(NivelRisco.BAIXO)
                .tipoRisco(TipoRisco.INFLAMAVEL)
                .descricaoRisco("Inflamável - armazenar longe de chamas")
                .perecivel(false)
                .condicoesArmazenamento("Local ventilado, longe de fontes de calor")
                .ativo(true)
                .build());

        Produto p2 = produtoRepository.save(Produto.builder()
                .nome("Microplacas 96 poços")
                .descricao("Microplacas de poliestireno para ELISA")
                .codigoReferencia("MIC-96-PO")
                .unidadeMedida(UnidadeMedida.UNIDADE)
                .localizacaoFisica("Armário B3 - Materiais descartáveis")
                .risco(NivelRisco.NENHUM)
                .perecivel(false)
                .ativo(true)
                .build());

        Produto p3 = produtoRepository.save(Produto.builder()
                .nome("Midio de Cultivo BHI")
                .descricao("Brain Heart Infusion - para cultivo de bactérias")
                .codigoReferencia("MID-BHI-500")
                .unidadeMedida(UnidadeMedida.FRASCO)
                .localizacaoFisica("Prateleira C2 - Meios de cultivo")
                .risco(NivelRisco.MEDIO)
                .tipoRisco(TipoRisco.BIOLOGICO)
                .descricaoRisco("Material biológico - manusear com EPI")
                .perecivel(true)
                .dataValidade(java.time.LocalDate.now().plusMonths(6))
                .tipoPerecivel(TipoPerecivel.MICROBIANO)
                .condicoesArmazenamento("Armazenar em geladeira 2-8°C")
                .ativo(true)
                .build());

        Produto p4 = produtoRepository.save(Produto.builder()
                .nome("Formaldeído 37%")
                .descricao("Solução de formaldeído para fixação de tecidos")
                .codigoReferencia("FOR-37-500")
                .unidadeMedida(UnidadeMedida.ML)
                .localizacaoFisica("Armário D1 - Produtos químicos perigosos")
                .risco(NivelRisco.ALTO)
                .tipoRisco(TipoRisco.TOXICO)
                .descricaoRisco("Tóxico e cancerígeno - usar capela")
                .perecivel(false)
                .condicoesArmazenamento("Armazenar em capela, temperatura ambiente")
                .ativo(true)
                .build());

        Produto p5 = produtoRepository.save(Produto.builder()
                .nome("Pipetas Sterile 1000uL")
                .descricao("Pipetas descartáveis estéreis")
                .codigoReferencia("PIP-1000-E")
                .unidadeMedida(UnidadeMedida.CAIXA)
                .localizacaoFisica("Armário B1 - Material descartável")
                .risco(NivelRisco.NENHUM)
                .perecivel(false)
                .ativo(true)
                .build());

        Produto p6 = produtoRepository.save(Produto.builder()
                .nome("Extrato de DNA Plant Wizard")
                .descricao("Kit de extração de DNA genômico de plantas")
                .codigoReferencia("EXT-DNA-PL")
                .unidadeMedida(UnidadeMedida.FRASCO)
                .localizacaoFisica("Freezer -80°C - Sala de Biologia Molecular")
                .risco(NivelRisco.NENHUM)
                .perecivel(true)
                .dataValidade(java.time.LocalDate.now().plusMonths(3))
                .tipoPerecivel(TipoPerecivel.QUIMICO)
                .condicoesArmazenamento("Armazenar em freezer -80°C")
                .ativo(true)
                .build());

        System.out.println("=== Dados de teste injetados com sucesso! ===");
        System.out.println("=== 3 Unidades, 5 Laboratórios, 5 Usuários, 6 Produtos ===");
    }
}
