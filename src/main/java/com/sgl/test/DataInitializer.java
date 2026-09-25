package com.sgl.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.sgl.model.Atividade;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.Estagiario;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.LocalArmazenamentoResiduo;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.LocalArmazenamentoResiduoRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProjetoRepository projetoRepository;
    private final SciRepository sciRepository;
    private final AtividadeRepository atividadeRepository;
    private final EstagiarioRepository estagiarioRepository;
    private final ClasseResiduoRepository classeResiduoRepository;
    private final LocalArmazenamentoResiduoRepository localArmazenamentoResiduoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (unidadeRepository.count() > 0) {
            garantirCadastrosResiduos();
            garantirDadosEtapa5();
            System.out.println("=== Dados de desenvolvimento já existem. Catálogos e massa da Etapa 5 conferidos. ===");
            return;
        }

        Unidade u1 = unidadeRepository.save(new Unidade(null, null, "Instituto de Biologia", "IB", null));
        Unidade u2 = unidadeRepository.save(new Unidade(null, null, "Instituto de Fisica", "IF", null));
        Unidade u3 = unidadeRepository.save(new Unidade(null, null, "Instituto de Quimica", "IQ", null));

        Laboratorio lab1 = laboratorioRepository.save(new Laboratorio(null, null, u1, "Laboratorio de Microbiologia", "Lab de estudo de microrganismos", null, true));
        Laboratorio lab2 = laboratorioRepository.save(new Laboratorio(null, null, u1, "Laboratorio de Genetica", "Lab de analise genetica", null, true));
        Laboratorio lab3 = laboratorioRepository.save(new Laboratorio(null, null, u2, "Laboratorio de Optica", "Lab de estudo da luz", null, true));
        Laboratorio lab4 = laboratorioRepository.save(new Laboratorio(null, null, u3, "Laboratorio de Quimica Organica", "Lab de sintese organica", null, true));
        Laboratorio lab5 = laboratorioRepository.save(new Laboratorio(null, null, u3, "Laboratorio de Analise Instrumental", "Lab de instrumentacao analitica", null, false));

        Usuario admin = usuarioRepository.save(new Usuario(null, null, "Admin Sistema", "admin@sgl.com", passwordEncoder.encode("123456"), Perfil.ADMINISTRADOR, u3, lab5, true));
        Usuario carlos = usuarioRepository.save(new Usuario(null, null, "Dr. Carlos Silva", "carlos@ib.com", passwordEncoder.encode("654321"), Perfil.GESTOR, u1, lab1, true));
        Usuario ana = usuarioRepository.save(new Usuario(null, null, "Dra. Ana Santos", "ana@ib.com", passwordEncoder.encode("987654"), Perfil.TECNICO, u1, lab2, true));
        Usuario joao = usuarioRepository.save(new Usuario(null, null, "Joao Pereira", "joao@if.com", passwordEncoder.encode("456789"), Perfil.PESQUISADOR, u2, lab3, true));

        Estagiario maria = new Estagiario();
        maria.setNome("Maria Oliveira");
        maria.setEmail("maria@iq.com");
        maria.setSenha(passwordEncoder.encode("246813"));
        maria.setPerfil(Perfil.ESTAGIARIO);
        maria.setUnidade(u3);
        maria.setLaboratorio(lab4);
        maria.setAtivo(true);
        maria.setDataInicioEstagio(LocalDate.now().minusMonths(2));
        maria.setTipoBolsa(TipoBolsa.BOLSA_INSTITUCIONAL);
        maria.setObservacao("Cadastro inicial de estágio para testes");
        maria = estagiarioRepository.save(maria);

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
                .tipoPerecivel(TipoPerecivel.QUIMICO)
                .condicoesArmazenamento("Armazenar em freezer -80°C")
                .unidadeArmazenamento("kit com 50 reações")
                .ativo(true)
                .build());

        EstoqueCentral e1 = criarEstoque(u1, p1, 80, 20);
        EstoqueCentral e2 = criarEstoque(u2, p1, 100, 20);
        EstoqueCentral e3 = criarEstoque(u1, p2, 50, 10);
        EstoqueCentral e4 = criarEstoque(u1, p3, 30, 5);
        EstoqueCentral e5 = criarEstoque(u3, p4, 15, 5);
        EstoqueCentral e6 = criarEstoque(u2, p5, 200, 30);
        EstoqueCentral e7 = criarEstoque(u3, p6, 10, 3);

        criarLoteInicial(e1, "INI-ALC-IB", 80, null);
        criarLoteInicial(e2, "INI-ALC-IF", 100, null);
        criarLoteInicial(e3, "INI-MIC-IB", 50, null);
        criarLoteInicial(e4, "INI-BHI-IB", 30, LocalDate.now().plusMonths(6));
        criarLoteInicial(e5, "INI-FOR-IQ", 15, null);
        criarLoteInicial(e6, "INI-PIP-IF", 200, null);
        criarLoteInicial(e7, "INI-DNA-IQ", 10, LocalDate.now().plusMonths(3));

        Projeto proj1 = projetoRepository.save(Projeto.builder()
                .laboratorio(lab3)
                .nome("Projeto de Óptica Avançada")
                .descricao("Estudo de fenômenos ópticos em materiais nanoestruturados")
                .dataInicio(LocalDate.now().minusMonths(3))
                .responsavel("Dr. Joao Pereira")
                .codigoSeg("98.98.98.001.01.00")
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .possuiRecursoExterno(false)
                .ativo(true)
                .build());

        Projeto proj2 = projetoRepository.save(Projeto.builder()
                .laboratorio(lab4)
                .nome("Síntese de Novos Compostos")
                .descricao("Desenvolvimento de novos compostos orgânicos para catálise")
                .dataInicio(LocalDate.now().minusMonths(1))
                .responsavel("Maria Oliveira")
                .codigoSeg("98.98.98.002.01.00")
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .possuiRecursoExterno(true)
                .empresaRecursoExterno("Empresa Fictícia DEV")
                .ativo(true)
                .build());

        Sci sci1 = sciRepository.save(Sci.builder()
                .projeto(proj1)
                .codigoSeg("98.98.98.001.01.01")
                .nome("Caracterização óptica de nanoestruturas")
                .responsavel("Dr. Joao Pereira")
                .dataInicio(LocalDate.now().minusMonths(2))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build());

        Sci sci2 = sciRepository.save(Sci.builder()
                .projeto(proj2)
                .codigoSeg("98.98.98.002.01.01")
                .nome("Avaliação catalítica de novos compostos")
                .responsavel("Maria Oliveira")
                .dataInicio(LocalDate.now().minusDays(20))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build());

        atividadeRepository.save(Atividade.builder()
                .sci(sci1)
                .codigoSeg("98.98.98.001.01.01.001")
                .nome("Preparação e caracterização das amostras ópticas")
                .responsavel("Dr. Joao Pereira")
                .dataInicio(LocalDate.now().minusDays(45))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build());

        atividadeRepository.save(Atividade.builder()
                .sci(sci2)
                .codigoSeg("98.98.98.002.01.01.001")
                .nome("Ensaios catalíticos preliminares")
                .responsavel("Maria Oliveira")
                .dataInicio(LocalDate.now().minusDays(15))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true)
                .build());

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

        garantirCadastrosResiduos();
        garantirDadosEtapa5();

        System.out.println("=== Dados de teste injetados com sucesso! ===");
        System.out.println("=== Estoques iniciais criados com lotes correspondentes ===");
    }

    private void garantirDadosEtapa5() {

        Unidade unidade = unidadeRepository.findAll().stream()
                .filter(item -> "IQ".equalsIgnoreCase(item.getSigla()))
                .findFirst()
                .orElse(null);

        if (unidade == null) {
            System.out.println("=== ETAPA 5 DEV: unidade IQ não encontrada; massa complementar ignorada. ===");
            return;
        }

        Laboratorio laboratorio = laboratorioRepository.findByUnidadeId(unidade.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .findFirst()
                .orElse(null);

        if (laboratorio == null) {
            System.out.println("=== ETAPA 5 DEV: unidade IQ sem laboratório ativo; massa complementar ignorada. ===");
            return;
        }

        LocalDate hoje = LocalDate.now();

        Projeto projetoBiossensores = garantirProjetoEtapa5(
                laboratorio,
                "96.96.96.001.01.00",
                "Biossensores para Monitoramento Ambiental",
                "Desenvolvimento e validação de biossensores para monitoramento de contaminantes em matrizes ambientais.",
                "Dra. Helena Costa",
                hoje.minusMonths(5),
                hoje.plusMonths(7),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO,
                true,
                "Instituto Parceiro DEV"
        );

        Sci sciBiossensores = garantirSciEtapa5(
                projetoBiossensores,
                "96.96.96.001.01.01",
                "Plataforma eletroquímica de detecção",
                "Dra. Helena Costa",
                hoje.minusMonths(4),
                hoje.plusMonths(5),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO
        );

        Sci sciValidacao = garantirSciEtapa5(
                projetoBiossensores,
                "96.96.96.001.01.02",
                "Validação em amostras ambientais",
                "Carlos Menezes",
                hoje.minusMonths(2),
                hoje.plusMonths(6),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO
        );

        garantirAtividadeEtapa5(
                sciBiossensores,
                "96.96.96.001.01.01.001",
                "Preparação dos eletrodos sensores",
                "Ana Martins",
                hoje.minusMonths(3),
                hoje.plusMonths(1),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO
        );

        garantirAtividadeEtapa5(
                sciBiossensores,
                "96.96.96.001.01.01.002",
                "Curvas analíticas e seletividade",
                "Dra. Helena Costa",
                hoje.minusMonths(2),
                hoje.plusMonths(3),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO
        );

        garantirAtividadeEtapa5(
                sciValidacao,
                "96.96.96.001.01.02.001",
                "Coleta e preparo de amostras",
                "Carlos Menezes",
                hoje.minusMonths(1),
                hoje.plusMonths(2),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO
        );

        Projeto projetoCatalise = garantirProjetoEtapa5(
                laboratorio,
                "96.96.96.002.01.00",
                "Catálise Sustentável com Materiais Híbridos",
                "Avaliação de materiais híbridos de baixo impacto para rotas catalíticas aplicadas a processos laboratoriais.",
                "Dra. Paula Ribeiro",
                hoje.minusMonths(8),
                hoje.plusMonths(2),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO,
                false,
                null
        );

        Sci sciCatalise = garantirSciEtapa5(
                projetoCatalise,
                "96.96.96.002.01.01",
                "Síntese e caracterização de catalisadores",
                "Dra. Paula Ribeiro",
                hoje.minusMonths(7),
                hoje.plusMonths(1),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO
        );

        garantirAtividadeEtapa5(
                sciCatalise,
                "96.96.96.002.01.01.001",
                "Síntese dos materiais híbridos",
                "Marcos Lima",
                hoje.minusMonths(6),
                hoje.minusDays(10),
                StatusProjeto.CONCLUIDO,
                SituacaoExecucaoProjeto.NAO_INFORMADO
        );

        garantirAtividadeEtapa5(
                sciCatalise,
                "96.96.96.002.01.01.002",
                "Ensaios de desempenho catalítico",
                "Dra. Paula Ribeiro",
                hoje.minusMonths(3),
                hoje.plusMonths(1),
                StatusProjeto.ATIVO,
                SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO
        );

        Projeto projetoConcluido = garantirProjetoEtapa5(
                laboratorio,
                "96.96.96.003.01.00",
                "Rastreabilidade de Reagentes Críticos",
                "Projeto piloto concluído para padronização de rastreabilidade de reagentes críticos.",
                "Equipe de Gestão IQ",
                hoje.minusYears(1),
                hoje.minusMonths(1),
                StatusProjeto.CONCLUIDO,
                SituacaoExecucaoProjeto.NAO_INFORMADO,
                false,
                null
        );

        Sci sciRastreabilidade = garantirSciEtapa5(
                projetoConcluido,
                "96.96.96.003.01.01",
                "Modelo piloto de rastreabilidade",
                "Equipe de Gestão IQ",
                hoje.minusMonths(11),
                hoje.minusMonths(2),
                StatusProjeto.CONCLUIDO,
                SituacaoExecucaoProjeto.NAO_INFORMADO
        );

        garantirAtividadeEtapa5(
                sciRastreabilidade,
                "96.96.96.003.01.01.001",
                "Validação do fluxo piloto",
                "Equipe de Gestão IQ",
                hoje.minusMonths(8),
                hoje.minusMonths(2),
                StatusProjeto.CONCLUIDO,
                SituacaoExecucaoProjeto.NAO_INFORMADO
        );
    }

    private Projeto garantirProjetoEtapa5(
            Laboratorio laboratorio,
            String codigoSeg,
            String nome,
            String descricao,
            String responsavel,
            LocalDate dataInicio,
            LocalDate dataFim,
            StatusProjeto status,
            SituacaoExecucaoProjeto situacaoExecucao,
            boolean possuiRecursoExterno,
            String empresaRecursoExterno) {

        return projetoRepository.findAll().stream()
                .filter(item -> codigoSeg.equals(item.getCodigoSeg()))
                .findFirst()
                .orElseGet(() -> projetoRepository.save(
                        Projeto.builder()
                                .laboratorio(laboratorio)
                                .nome(nome)
                                .descricao(descricao)
                                .responsavel(responsavel)
                                .codigoSeg(codigoSeg)
                                .dataInicio(dataInicio)
                                .dataFim(dataFim)
                                .status(status)
                                .situacaoExecucao(situacaoExecucao)
                                .possuiRecursoExterno(possuiRecursoExterno)
                                .empresaRecursoExterno(empresaRecursoExterno)
                                .ativo(true)
                                .build()
                ));
    }

    private Sci garantirSciEtapa5(
            Projeto projeto,
            String codigoSeg,
            String nome,
            String responsavel,
            LocalDate dataInicio,
            LocalDate dataFim,
            StatusProjeto status,
            SituacaoExecucaoProjeto situacaoExecucao) {

        UUID unidadeId = projeto.getLaboratorio().getUnidade().getPublicId();

        return sciRepository
                .findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
                        projeto.getPublicId(),
                        unidadeId
                )
                .stream()
                .filter(item -> codigoSeg.equals(item.getCodigoSeg()))
                .findFirst()
                .orElseGet(() -> sciRepository.save(
                        Sci.builder()
                                .projeto(projeto)
                                .codigoSeg(codigoSeg)
                                .nome(nome)
                                .responsavel(responsavel)
                                .dataInicio(dataInicio)
                                .dataFim(dataFim)
                                .status(status)
                                .situacaoExecucao(situacaoExecucao)
                                .ativo(true)
                                .build()
                ));
    }

    private Atividade garantirAtividadeEtapa5(
            Sci sci,
            String codigoSeg,
            String nome,
            String responsavel,
            LocalDate dataInicio,
            LocalDate dataFim,
            StatusProjeto status,
            SituacaoExecucaoProjeto situacaoExecucao) {

        UUID unidadeId = sci.getProjeto().getLaboratorio().getUnidade().getPublicId();

        return atividadeRepository
                .findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        sci.getPublicId(),
                        unidadeId
                )
                .stream()
                .filter(item -> codigoSeg.equals(item.getCodigoSeg()))
                .findFirst()
                .orElseGet(() -> atividadeRepository.save(
                        Atividade.builder()
                                .sci(sci)
                                .codigoSeg(codigoSeg)
                                .nome(nome)
                                .responsavel(responsavel)
                                .dataInicio(dataInicio)
                                .dataFim(dataFim)
                                .status(status)
                                .situacaoExecucao(situacaoExecucao)
                                .ativo(true)
                                .build()
                ));
    }

    private void garantirCadastrosResiduos() {
        for (Unidade unidade : unidadeRepository.findAll()) {
            garantirClasseResiduo(
                    unidade,
                    "A",
                    "Solventes ou soluções de substâncias orgânicas que não contenham halogênios"
            );
            garantirClasseResiduo(
                    unidade,
                    "B",
                    "Solventes ou soluções orgânicas que contenham halogênios"
            );
            garantirClasseResiduo(
                    unidade,
                    "F",
                    "Resíduos sólidos de produtos químicos orgânicos"
            );
            garantirClasseResiduo(
                    unidade,
                    "H",
                    "Outros"
            );

            garantirLocalArmazenamento(unidade, "Abrigo de resíduos");
            garantirLocalArmazenamento(unidade, "Almoxarifado químico");
        }
    }

    private void garantirClasseResiduo(
            Unidade unidade,
            String codigo,
            String descricao) {

        if (classeResiduoRepository.existsByUnidadeIdAndCodigoIgnoreCase(
                unidade.getId(),
                codigo)) {
            return;
        }

        classeResiduoRepository.save(
                ClasseResiduo.builder()
                        .unidade(unidade)
                        .codigo(codigo)
                        .descricao(descricao)
                        .ativo(true)
                        .build()
        );
    }

    private void garantirLocalArmazenamento(
            Unidade unidade,
            String nome) {

        if (localArmazenamentoResiduoRepository.existsByUnidadeIdAndNomeIgnoreCase(
                unidade.getId(),
                nome)) {
            return;
        }

        localArmazenamentoResiduoRepository.save(
                LocalArmazenamentoResiduo.builder()
                        .unidade(unidade)
                        .nome(nome)
                        .ativo(true)
                        .build()
        );
    }

    private EstoqueCentral criarEstoque(
            Unidade unidade,
            Produto produto,
            int quantidade,
            int quantidadeMinima) {

        return estoqueCentralRepository.save(EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(quantidade)
                .quantidadeMinima(quantidadeMinima)
                .ativo(true)
                .build());
    }

    private void criarLoteInicial(
            EstoqueCentral estoque,
            String numeroLote,
            int quantidade,
            LocalDate dataValidade) {

        Produto produto = estoque.getProduto();
        int maiorSequencial = loteRepository.buscarMaiorSequencialInternoPorProduto(produto.getId());
        int sequencial = maiorSequencial + 1;
        String sigla = produto.getCodigoReferencia()
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.definirCodigoInterno(
                "LOT-" + sigla + "-" + String.format(Locale.ROOT, "%03d", sequencial),
                sequencial
        );
        lote.setNumeroLote(numeroLote);
        lote.setApresentacao("Legado");
        lote.setQuantidadeApresentacoes(quantidade);
        lote.setConteudoPorApresentacao(1);
        lote.setFracionavel(true);
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeDisponivel(quantidade);
        lote.setDataEntrada(LocalDate.now().minusDays(30));
        lote.setDataValidade(dataValidade);
        lote.setAtivo(true);
        loteRepository.save(lote);
    }
}
