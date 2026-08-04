package com.sgl.test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.sgl.model.Estagiario;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
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
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final PedidoRepository pedidoRepository;
    private final ProjetoRepository projetoRepository;
    private final EstagiarioRepository estagiarioRepository;

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

        Estagiario maria = new Estagiario();
        maria.setNome("Maria Oliveira");
        maria.setEmail("maria@iq.com");
        maria.setSenha("123456");
        maria.setPerfil(Perfil.ESTAGIARIO);
        maria.setUnidade(u2);
        maria.setLaboratorio(lab4);
        maria.setAtivo(true);
        maria.setDataInicioEstagio(java.time.LocalDate.now().minusMonths(2));
        maria.setTipoBolsa(TipoBolsa.BOLSA_INSTITUCIONAL);
        maria.setObservacao("Cadastro inicial de estágio para testes");
        maria = estagiarioRepository.save(maria);

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
                .unidadeArmazenamento("frasco de 1L")
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
                .unidadeArmazenamento("caixa com 50 unidades")
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
                .unidadeArmazenamento("frasco de 500mL")
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
                .unidadeArmazenamento("frasco de 500mL")
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
                .unidadeArmazenamento("caixa com 1000 unidades")
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
                .unidadeArmazenamento("kit com 50 reações")
                .ativo(true)
                .build());

        // Estoques centrais por Unidade.
        // A combinação Unidade + Produto deve ser única.
        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u1)
                .produto(p1)
                .quantidadeAtual(80)
                .quantidadeMinima(20)
                .ativo(true)
                .build());

        // O mesmo produto pode existir em outra Unidade com saldo próprio.
        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u2)
                .produto(p1)
                .quantidadeAtual(100)
                .quantidadeMinima(20)
                .ativo(true)
                .build());

        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u1)
                .produto(p2)
                .quantidadeAtual(50)
                .quantidadeMinima(10)
                .ativo(true)
                .build());

        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u1)
                .produto(p3)
                .quantidadeAtual(30)
                .quantidadeMinima(5)
                .ativo(true)
                .build());

        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u3)
                .produto(p4)
                .quantidadeAtual(15)
                .quantidadeMinima(5)
                .ativo(true)
                .build());

        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u2)
                .produto(p5)
                .quantidadeAtual(200)
                .quantidadeMinima(30)
                .ativo(true)
                .build());

        estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(u3)
                .produto(p6)
                .quantidadeAtual(10)
                .quantidadeMinima(3)
                .ativo(true)
                .build());

        // Projetos
        Projeto proj1 = projetoRepository.save(Projeto.builder()
                .laboratorio(lab3)
                .nome("Projeto de Óptica Avançada")
                .descricao("Estudo de fenômenos ópticos em materiais nanoestruturados")
                .dataInicio(java.time.LocalDate.now().minusMonths(3))
                .responsavel("Dr. Joao Pereira")
                .ativo(true)
                .build());

        Projeto proj2 = projetoRepository.save(Projeto.builder()
                .laboratorio(lab4)
                .nome("Síntese de Novos Compostos")
                .descricao("Desenvolvimento de novos compostos orgânicos para catálise")
                .dataInicio(java.time.LocalDate.now().minusMonths(1))
                .responsavel("Maria Oliveira")
                .ativo(true)
                .build());

        // Pedidos de teste
        Pedido pedido1 = Pedido.builder()
                .usuario(joao)
                .laboratorio(lab3)
                .projeto(proj1)
                .dataSolicitacao(LocalDateTime.now().minusDays(2))
                .status(StatusPedido.PENDENTE)
                .observacao("Materiais para experimento de óptica")
                .itens(new ArrayList<>())
                .build();

        ItemPedido item1 = ItemPedido.builder()
                .pedido(pedido1)
                .produto(p1)
                .quantidadeSolicitada(5)
                .build();
        pedido1.getItens().add(item1);

        ItemPedido item2 = ItemPedido.builder()
                .pedido(pedido1)
                .produto(p5)
                .quantidadeSolicitada(2)
                .build();
        pedido1.getItens().add(item2);

        pedidoRepository.save(pedido1);

        Pedido pedido2 = Pedido.builder()
                .usuario(maria)
                .laboratorio(lab4)
                .projeto(proj2)
                .dataSolicitacao(LocalDateTime.now().minusDays(1))
                .status(StatusPedido.PENDENTE)
                .observacao("Formaldeído para síntese")
                .itens(new ArrayList<>())
                .build();

        ItemPedido item3 = ItemPedido.builder()
                .pedido(pedido2)
                .produto(p4)
                .quantidadeSolicitada(3)
                .build();
        pedido2.getItens().add(item3);

        pedidoRepository.save(pedido2);

        System.out.println("=== Dados de teste injetados com sucesso! ===");
        System.out.println("=== 3 Unidades, 5 Labs, 5 Usuarios, 1 Estagiario, 6 Produtos, 7 registros de EstoqueCentral, 2 Projetos, 2 Pedidos ===");
    }
}
