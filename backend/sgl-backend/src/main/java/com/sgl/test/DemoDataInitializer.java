package com.sgl.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.sgl.model.ComponenteResiduo;
import com.sgl.model.Estagiario;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.HistoricoResiduo;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Residuo;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.HistoricoResiduoRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.ResiduoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Massa de dados exclusivamente para apresentação.
 *
 * <p>Ative com o profile "demo". O cenário representa aproximadamente 30 dias
 * de operação fictícia da unidade Embrapa Mandioca e Fruticultura. Pessoas,
 * e-mails, lotes, pedidos e ocorrências são fictícios.</p>
 */
@Profile("demo")
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private static final String SENHA_DEMO = "123456";

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstagiarioRepository estagiarioRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProjetoRepository projetoRepository;
    private final ResiduoRepository residuoRepository;
    private final HistoricoResiduoRepository historicoResiduoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (unidadeRepository.count() > 0) {
            System.out.println("=== DEMO SGL: banco já possui dados; carga fictícia ignorada. ===");
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();

        Unidade unidade = unidadeRepository.save(
                new Unidade(null, null, "Embrapa Mandioca e Fruticultura", "CNPMF", null)
        );

        Laboratorio labBiomol = criarLaboratorio(
                unidade,
                "Laboratório de Biologia Molecular",
                "Genotipagem, extração de DNA, PCR e apoio ao melhoramento genético."
        );
        Laboratorio labFitopatologia = criarLaboratorio(
                unidade,
                "Laboratório de Fitopatologia",
                "Diagnose de doenças, isolamento e cultivo de fungos e bactérias."
        );
        Laboratorio labEntomologia = criarLaboratorio(
                unidade,
                "Laboratório de Entomologia",
                "Manejo integrado de pragas e estudos de controle biológico."
        );
        Laboratorio labSolos = criarLaboratorio(
                unidade,
                "Laboratório de Solos e Nutrição de Plantas",
                "Análises químicas de solo e tecido vegetal."
        );
        Laboratorio labVirologia = criarLaboratorio(
                unidade,
                "Laboratório de Virologia",
                "Diagnóstico molecular e sorológico de fitovírus."
        );
        Laboratorio labEcofisiologia = criarLaboratorio(
                unidade,
                "Laboratório de Ecofisiologia Vegetal e Meteorologia",
                "Fisiologia vegetal, status hídrico, meteorologia e modelagem."
        );
        Laboratorio labCentral = criarLaboratorio(
                unidade,
                "Central de Soluções",
                "Preparo de soluções, pesagem de alíquotas e apoio aos laboratórios."
        );

        Usuario admin = criarUsuario(
                unidade, labCentral, "Mariana Souza", "mariana.souza@demo.sgl.local",
                Perfil.ADMINISTRADOR, true
        );
        Usuario gestor = criarUsuario(
                unidade, labCentral, "Ricardo Almeida", "ricardo.almeida@demo.sgl.local",
                Perfil.GESTOR, true
        );
        Usuario camila = criarUsuario(
                unidade, labBiomol, "Camila Menezes", "camila.menezes@demo.sgl.local",
                Perfil.TECNICO, true
        );
        Usuario paulo = criarUsuario(
                unidade, labFitopatologia, "Paulo Nascimento", "paulo.nascimento@demo.sgl.local",
                Perfil.PESQUISADOR, true
        );
        Usuario juliana = criarUsuario(
                unidade, labEntomologia, "Juliana Rocha", "juliana.rocha@demo.sgl.local",
                Perfil.ANALISTA, true
        );
        Usuario andre = criarUsuario(
                unidade, labSolos, "André Lima", "andre.lima@demo.sgl.local",
                Perfil.PESQUISADOR, true
        );
        Usuario fernanda = criarUsuario(
                unidade, labVirologia, "Fernanda Costa", "fernanda.costa@demo.sgl.local",
                Perfil.TECNICO, true
        );
        Usuario lucas = criarUsuario(
                unidade, labEcofisiologia, "Lucas Ribeiro", "lucas.ribeiro@demo.sgl.local",
                Perfil.PESQUISADOR, true
        );
        Usuario beatriz = criarUsuario(
                unidade, labCentral, "Beatriz Santos", "beatriz.santos@demo.sgl.local",
                Perfil.TECNICO, true
        );
        criarUsuario(
                unidade, labEntomologia, "Sérgio Oliveira", "sergio.oliveira@demo.sgl.local",
                Perfil.ANALISTA, false
        );

        Estagiario ana = criarEstagiario(
                unidade, labBiomol, "Ana Clara Barbosa", "ana.barbosa@demo.sgl.local",
                hoje.minusMonths(5), null, TipoBolsa.BOLSA_CNPQ, true,
                "Iniciação científica em genotipagem de mandioca."
        );
        Estagiario gabriel = criarEstagiario(
                unidade, labFitopatologia, "Gabriel Teixeira", "gabriel.teixeira@demo.sgl.local",
                hoje.minusMonths(3), null, TipoBolsa.BOLSA_INSTITUCIONAL, true,
                "Apoio em isolamento e identificação de fitopatógenos."
        );
        Estagiario larissa = criarEstagiario(
                unidade, labEntomologia, "Larissa Gomes", "larissa.gomes@demo.sgl.local",
                hoje.minusMonths(4), null, TipoBolsa.BOLSA_CNPQ, true,
                "Apoio em criação e monitoramento de insetos."
        );
        Estagiario joao = criarEstagiario(
                unidade, labSolos, "João Victor Silva", "joao.silva@demo.sgl.local",
                hoje.minusMonths(2), null, TipoBolsa.VOLUNTARIO, true,
                "Apoio no preparo de amostras de solo e tecido vegetal."
        );
        criarEstagiario(
                unidade, labVirologia, "Marina Alves", "marina.alves@demo.sgl.local",
                hoje.minusMonths(8), hoje.minusDays(10), TipoBolsa.BOLSA_CAPES, false,
                "Estágio encerrado após conclusão do plano de atividades."
        );
        criarEstagiario(
                unidade, labEcofisiologia, "Pedro Henrique", "pedro.henrique@demo.sgl.local",
                hoje.minusMonths(6), hoje.minusDays(20), TipoBolsa.CONTRATUAL, false,
                "Vínculo finalizado no mês da simulação."
        );

        labBiomol.setResponsavel(camila);
        labFitopatologia.setResponsavel(paulo);
        labEntomologia.setResponsavel(juliana);
        labSolos.setResponsavel(andre);
        labVirologia.setResponsavel(fernanda);
        labEcofisiologia.setResponsavel(lucas);
        labCentral.setResponsavel(gestor);
        laboratorioRepository.saveAll(List.of(
                labBiomol, labFitopatologia, labEntomologia, labSolos,
                labVirologia, labEcofisiologia, labCentral
        ));

        Projeto projBiomol = criarProjeto(
                labBiomol,
                "Genotipagem de variedades de mandioca",
                "Caracterização molecular e seleção assistida por marcadores.",
                hoje.minusMonths(8),
                "Camila Menezes"
        );
        Projeto projFito = criarProjeto(
                labFitopatologia,
                "Diagnóstico de Fusarium em banana",
                "Isolamento e caracterização de isolados associados a sintomas de murcha.",
                hoje.minusMonths(5),
                "Paulo Nascimento"
        );
        Projeto projEnto = criarProjeto(
                labEntomologia,
                "Controle biológico de mosca-branca",
                "Avaliação de agentes de controle biológico em condições controladas.",
                hoje.minusMonths(4),
                "Juliana Rocha"
        );
        Projeto projSolos = criarProjeto(
                labSolos,
                "Nutrição mineral da mandioca",
                "Avaliação de macronutrientes e micronutrientes em solo e tecido vegetal.",
                hoje.minusMonths(7),
                "André Lima"
        );
        Projeto projViro = criarProjeto(
                labVirologia,
                "Monitoramento molecular de fitovírus",
                "Detecção por PCR e ELISA em amostras de fruteiras tropicais.",
                hoje.minusMonths(6),
                "Fernanda Costa"
        );
        Projeto projEco = criarProjeto(
                labEcofisiologia,
                "Resposta hídrica de citros",
                "Monitoramento de trocas gasosas e disponibilidade hídrica.",
                hoje.minusMonths(3),
                "Lucas Ribeiro"
        );

        Produto etanol = criarProduto(
                "Etanol 70%", "CNPMF-ETA-70", UnidadeMedida.L,
                "Solução para assepsia e rotinas laboratoriais.",
                "Prateleira Q1 - Central de Soluções",
                NivelRisco.MEDIO, TipoRisco.INFLAMAVEL, "Inflamável; manter longe de fontes de ignição.",
                false, null, "Ambiente ventilado e protegido de calor.", "garrafa de 1 L",
                false, Set.of(), null
        );
        Produto hipoclorito = criarProduto(
                "Hipoclorito de Sódio 2,5%", "CNPMF-HIP-25", UnidadeMedida.L,
                "Solução para descontaminação de bancadas e materiais.",
                "Prateleira Q2 - Central de Soluções",
                NivelRisco.MEDIO, TipoRisco.CORROSIVO, "Irritante/corrosivo em contato concentrado.",
                false, null, "Local fresco, ao abrigo de luz.", "garrafa de 1 L",
                false, Set.of(), null
        );
        Produto hcl = criarProduto(
                "Ácido Clorídrico 37%", "CNPMF-HCL-37", UnidadeMedida.ML,
                "Reagente concentrado para preparo de soluções e análises.",
                "Armário de corrosivos - Central de Soluções",
                NivelRisco.ALTO, TipoRisco.CORROSIVO, "Corrosivo; utilizar EPI e capela.",
                false, null, "Armário exclusivo para corrosivos.", "frasco de 1 L",
                true, Set.of(OrgaoFiscalizador.POLICIA_FEDERAL),
                "Produto controlado para fins demonstrativos do relatório de fiscalização."
        );
        Produto dnaKit = criarProduto(
                "Kit de Extração de DNA Vegetal", "CNPMF-DNA-EXT", UnidadeMedida.REACAO,
                "Kit para extração de DNA genômico de tecido vegetal.",
                "Geladeira 2-8 °C - Biologia Molecular",
                NivelRisco.BAIXO, TipoRisco.IRRITANTE, "Alguns componentes podem causar irritação.",
                true, TipoPerecivel.QUIMICO, "Manter refrigerado entre 2 e 8 °C.", "kit com 50 reações",
                false, Set.of(), null
        );
        Produto masterMix = criarProduto(
                "Master Mix PCR 2X", "CNPMF-PCR-2X", UnidadeMedida.REACAO,
                "Mistura pronta para reações de PCR.",
                "Freezer -20 °C - Biologia Molecular",
                NivelRisco.NENHUM, null, null,
                true, TipoPerecivel.QUIMICO, "Manter congelado a -20 °C.", "frasco para 100 reações",
                false, Set.of(), null
        );
        Produto agarose = criarProduto(
                "Agarose Grau Molecular", "CNPMF-AGR-MOL", UnidadeMedida.G,
                "Agarose para eletroforese de DNA.",
                "Armário B2 - Biologia Molecular",
                NivelRisco.NENHUM, null, null,
                false, null, "Ambiente seco e protegido de umidade.", "frasco de 500 g",
                false, Set.of(), null
        );
        Produto ponteiras = criarProduto(
                "Ponteiras 1000 µL com filtro", "CNPMF-PON-1000", UnidadeMedida.CAIXA,
                "Ponteiras estéreis com filtro para micropipetas.",
                "Armário M3 - Materiais descartáveis",
                NivelRisco.NENHUM, null, null,
                false, null, "Ambiente limpo e seco.", "caixa com racks",
                false, Set.of(), null
        );
        Produto luvas = criarProduto(
                "Luvas Nitrílicas sem pó", "CNPMF-LUV-NIT", UnidadeMedida.CAIXA,
                "Luvas de procedimento para uso laboratorial.",
                "Armário EPI - Almoxarifado",
                NivelRisco.NENHUM, null, null,
                false, null, "Ambiente seco.", "caixa com 100 unidades",
                false, Set.of(), null
        );
        Produto bda = criarProduto(
                "Meio BDA preparado", "CNPMF-BDA", UnidadeMedida.FRASCO,
                "Meio Batata Dextrose Ágar preparado para cultivo de fungos.",
                "Geladeira 2-8 °C - Fitopatologia",
                NivelRisco.BAIXO, TipoRisco.BIOLOGICO, "Material destinado a cultivo microbiológico.",
                true, TipoPerecivel.MICROBIANO, "Refrigerado entre 2 e 8 °C.", "frasco preparado",
                false, Set.of(), null
        );
        Produto metanol = criarProduto(
                "Metanol Grau HPLC", "CNPMF-MET-HPLC", UnidadeMedida.L,
                "Solvente para preparo de amostras e análises.",
                "Armário de inflamáveis - Central de Soluções",
                NivelRisco.ALTO, TipoRisco.TOXICO, "Tóxico e inflamável; manipular em capela.",
                false, null, "Armário para inflamáveis, ventilado.", "garrafa de 1 L",
                true, Set.of(OrgaoFiscalizador.POLICIA_FEDERAL),
                "Produto fiscalizado no cenário de demonstração."
        );
        Produto pbs = criarProduto(
                "Tampão PBS 10X", "CNPMF-PBS-10X", UnidadeMedida.FRASCO,
                "Tampão concentrado para rotinas de biologia molecular e imunodiagnóstico.",
                "Geladeira 2-8 °C - Central de Soluções",
                NivelRisco.NENHUM, null, null,
                true, TipoPerecivel.QUIMICO, "Refrigerado entre 2 e 8 °C.", "frasco de 500 mL",
                false, Set.of(), null
        );
        Produto kcl = criarProduto(
                "Cloreto de Potássio P.A.", "CNPMF-KCL-PA", UnidadeMedida.G,
                "Sal analítico utilizado em preparo de soluções.",
                "Prateleira S2 - Solos e Nutrição",
                NivelRisco.NENHUM, null, null,
                false, null, "Ambiente seco.", "frasco de 1 kg",
                false, Set.of(), null
        );
        Produto tmb = criarProduto(
                "Substrato ELISA TMB", "CNPMF-TMB-ELISA", UnidadeMedida.FRASCO,
                "Substrato cromogênico para ensaios ELISA.",
                "Geladeira 2-8 °C - Virologia",
                NivelRisco.BAIXO, TipoRisco.IRRITANTE, "Evitar contato com pele e olhos.",
                true, TipoPerecivel.QUIMICO, "Refrigerado e protegido da luz.", "frasco de 100 mL",
                false, Set.of(), null
        );
        Produto formaldeido = criarProduto(
                "Formaldeído 37%", "CNPMF-FOR-37", UnidadeMedida.ML,
                "Solução para preservação/fixação em procedimentos específicos.",
                "Armário químico ventilado - Fitopatologia",
                NivelRisco.ALTO, TipoRisco.PERIGO_SAUDE, "Tóxico; manipular somente com EPI e em capela.",
                false, null, "Armário ventilado e segregado.", "frasco de 500 mL",
                true, Set.of(OrgaoFiscalizador.POLICIA_FEDERAL, OrgaoFiscalizador.ANVISA),
                "Controle demonstrativo por múltiplos órgãos."
        );

        EstoqueCentral estEtanol = criarEstoque(unidade, etanol, 46, 20);
        EstoqueCentral estHipoclorito = criarEstoque(unidade, hipoclorito, 17, 8);
        EstoqueCentral estHcl = criarEstoque(unidade, hcl, 6500, 2000);
        EstoqueCentral estDna = criarEstoque(unidade, dnaKit, 70, 30);
        EstoqueCentral estMaster = criarEstoque(unidade, masterMix, 8, 20);
        EstoqueCentral estAgarose = criarEstoque(unidade, agarose, 1200, 500);
        EstoqueCentral estPonteiras = criarEstoque(unidade, ponteiras, 11, 6);
        EstoqueCentral estLuvas = criarEstoque(unidade, luvas, 4, 10);
        EstoqueCentral estBda = criarEstoque(unidade, bda, 12, 10);
        EstoqueCentral estMetanol = criarEstoque(unidade, metanol, 12, 5);
        EstoqueCentral estPbs = criarEstoque(unidade, pbs, 24, 10);
        EstoqueCentral estKcl = criarEstoque(unidade, kcl, 750, 300);
        EstoqueCentral estTmb = criarEstoque(unidade, tmb, 7, 5);
        EstoqueCentral estFormaldeido = criarEstoque(unidade, formaldeido, 2500, 1000);

        Lote loteEtanol = criarLote(estEtanol, 1, "FORN-ETA-2408-A", TipoEmbalagem.GARRAFA,
                "garrafa de 1 L", 60, 46, hoje.minusDays(30), null, true, true,
                "Entrada de reposição mensal.");
        Lote loteHipoclorito = criarLote(estHipoclorito, 1, "FORN-HIP-2408-B", TipoEmbalagem.GARRAFA,
                "garrafa de 1 L", 20, 17, hoje.minusDays(18), null, true, true,
                "Reposição para rotinas de descontaminação.");
        Lote loteHcl = criarLote(estHcl, 1, "FORN-HCL-0826", TipoEmbalagem.GARRAFA,
                "garrafa de 1 L", 8000, 6500, hoje.minusDays(26), null, true, true,
                "Produto fiscalizado - documentação conferida.");
        Lote loteDna = criarLote(estDna, 1, "DNA-0826-17", TipoEmbalagem.KIT,
                "kit com 50 reações", 100, 70, hoje.minusDays(29), hoje.plusDays(29), true, true,
                "Validade dentro de 30 dias para demonstrar alerta preventivo.");
        Lote loteMasterVencido = criarLote(estMaster, 1, "PCR-0726-09", TipoEmbalagem.UNITARIO,
                "frasco para 100 reações", 20, 0, hoje.minusDays(45), hoje.minusDays(3), false, true,
                "Lote vencido e integralmente descartado.");
        Lote loteMaster = criarLote(estMaster, 2, "PCR-0826-11", TipoEmbalagem.UNITARIO,
                "frasco para 100 reações", 30, 8, hoje.minusDays(20), hoje.plusDays(4), true, true,
                "Saldo crítico e vencimento próximo.");
        Lote loteAgarose = criarLote(estAgarose, 1, "AGR-0826-05", TipoEmbalagem.UNITARIO,
                "frasco de 500 g", 1500, 1200, hoje.minusDays(22), null, true, true,
                null);
        Lote lotePonteiras = criarLote(estPonteiras, 1, "PON-0826-03", TipoEmbalagem.CAIXA,
                "caixa com racks", 15, 11, hoje.minusDays(10), null, true, false,
                "Material não fracionável por caixa.");
        Lote loteLuvas = criarLote(estLuvas, 1, "LUV-0826-02", TipoEmbalagem.CAIXA,
                "caixa com 100 unidades", 20, 4, hoje.minusDays(26), null, true, false,
                "Saldo abaixo do mínimo.");
        Lote loteBdaDescartado = criarLote(estBda, 1, "BDA-0726-04", TipoEmbalagem.UNITARIO,
                "frasco preparado", 8, 0, hoje.minusDays(35), hoje.minusDays(5), false, true,
                "Lote vencido e descartado no período.");
        Lote loteBda = criarLote(estBda, 2, "BDA-0826-08", TipoEmbalagem.UNITARIO,
                "frasco preparado", 20, 12, hoje.minusDays(21), hoje.plusDays(14), true, true,
                "Novo lote após descarte do anterior.");
        Lote loteMetanol = criarLote(estMetanol, 1, "MET-0826-01", TipoEmbalagem.GARRAFA,
                "garrafa de 1 L", 20, 12, hoje.minusDays(20), null, true, true,
                "Produto fiscalizado.");
        Lote lotePbs = criarLote(estPbs, 1, "PBS-0826-06", TipoEmbalagem.UNITARIO,
                "frasco de 500 mL", 40, 24, hoje.minusDays(18), hoje.plusDays(18), true, true,
                "Vencimento em menos de 30 dias.");
        Lote loteKcl = criarLote(estKcl, 1, "KCL-0826-12", TipoEmbalagem.UNITARIO,
                "frasco de 1 kg", 1000, 750, hoje.minusDays(5), null, true, true,
                null);
        Lote loteTmbVencido = criarLote(estTmb, 1, "TMB-0726-02", TipoEmbalagem.UNITARIO,
                "frasco de 100 mL", 3, 3, hoje.minusDays(40), hoje.minusDays(2), true, true,
                "Lote vencido ainda aguardando descarte - alerta crítico da demonstração.");
        Lote loteTmb = criarLote(estTmb, 2, "TMB-0826-04", TipoEmbalagem.UNITARIO,
                "frasco de 100 mL", 4, 4, hoje.minusDays(3), hoje.plusMonths(5), true, true,
                "Reposição recente.");
        Lote loteFormaldeido = criarLote(estFormaldeido, 1, "FOR-0826-07", TipoEmbalagem.GARRAFA,
                "frasco de 500 mL", 3000, 2500, hoje.minusDays(16), null, true, true,
                "Documentação de produto controlado anexada ao processo de compra.");

        Pedido ped01 = criarPedido(
                paulo, labFitopatologia, projFito, agora.minusDays(28).withHour(9).withMinute(12),
                StatusPedido.ENTREGUE, agora.minusDays(27).withHour(14).withMinute(20),
                false, null, "Rotina de descontaminação e cultivo.",
                item(etanol, 5, 5), item(ponteiras, 2, 2)
        );
        Pedido ped02 = criarPedido(
                camila, labBiomol, projBiomol, agora.minusDays(25).withHour(8).withMinute(35),
                StatusPedido.ENTREGUE, agora.minusDays(24).withHour(10).withMinute(5),
                false, null, "Extração de DNA de folhas de mandioca.",
                item(dnaKit, 20, 20)
        );
        Pedido ped03 = criarPedido(
                joao, labSolos, projSolos, agora.minusDays(22).withHour(11).withMinute(10),
                StatusPedido.REJEITADO, null,
                false, null, "Solicitação sem justificativa suficiente para produto controlado.",
                item(hcl, 2500, null)
        );
        Pedido ped04 = criarPedido(
                gabriel, labFitopatologia, projFito, agora.minusDays(20).withHour(9).withMinute(45),
                StatusPedido.ENTREGUE, agora.minusDays(19).withHour(15).withMinute(10),
                false, null, "Meio de cultivo para isolamento de fungos.",
                item(bda, 8, 8)
        );
        Pedido ped05 = criarPedido(
                larissa, labEntomologia, projEnto, agora.minusDays(18).withHour(14).withMinute(5),
                StatusPedido.CANCELADO, null,
                false, null, "Cancelado após revisão do planejamento experimental; material devolvido.",
                item(luvas, 2, 2)
        );
        Pedido ped06 = criarPedido(
                fernanda, labVirologia, projViro, agora.minusDays(15).withHour(8).withMinute(20),
                StatusPedido.ENTREGUE, agora.minusDays(14).withHour(11).withMinute(40),
                false, null, "Preparo de soluções para ensaio molecular.",
                item(metanol, 2, 2), item(pbs, 4, 4)
        );
        Pedido ped07 = criarPedido(
                ana, labBiomol, projBiomol, agora.minusDays(12).withHour(16).withMinute(10),
                StatusPedido.REJEITADO, null,
                false, null, "Quantidade solicitada incompatível com saldo disponível.",
                item(masterMix, 40, null)
        );
        Pedido ped08 = criarPedido(
                camila, labBiomol, projBiomol, agora.minusDays(9).withHour(10).withMinute(25),
                StatusPedido.APROVADO, null,
                false, null, "Aprovado; aguardando retirada pelo laboratório.",
                item(agarose, 250, 250)
        );
        Pedido ped09 = criarPedido(
                fernanda, labVirologia, projViro, agora.minusDays(7).withHour(9).withMinute(0),
                StatusPedido.ENTREGUE, agora.minusDays(6).withHour(13).withMinute(25),
                false, null, "Extração de material genético para confirmação molecular.",
                item(dnaKit, 10, 10)
        );
        Pedido ped10 = criarPedido(
                andre, labSolos, projSolos, agora.minusDays(4).withHour(8).withMinute(15),
                StatusPedido.ENTREGUE, agora.minusDays(3).withHour(16).withMinute(0),
                false, null, "Preparo de solução padrão.",
                item(kcl, 250, 250)
        );
        Pedido ped11 = criarPedido(
                juliana, labEntomologia, projEnto, agora.minusDays(3).withHour(14).withMinute(35),
                StatusPedido.PENDENTE, null,
                false, null, "Reposição de EPI para rotina com insetos.",
                item(luvas, 3, null)
        );
        Pedido ped12 = criarPedido(
                ana, labBiomol, projBiomol, agora.minusDays(1).withHour(8).withMinute(5),
                StatusPedido.PENDENTE, null,
                true, "PCR de amostras com janela experimental nesta semana.",
                "Pedido urgente para continuidade da bateria de PCR.",
                item(masterMix, 12, null)
        );
        Pedido ped13 = criarPedido(
                paulo, labFitopatologia, projFito, agora.minusDays(1).withHour(13).withMinute(40),
                StatusPedido.APROVADO, null,
                false, null, "Aprovado para retirada na Central de Soluções.",
                item(etanol, 3, 3), item(hipoclorito, 2, 2)
        );
        Pedido ped14 = criarPedido(
                fernanda, labVirologia, projViro, agora.minusHours(5),
                StatusPedido.PENDENTE, null,
                true, "Execução de ELISA programada para amanhã pela manhã.",
                "Reposição urgente de substrato para ELISA.",
                item(tmb, 2, null)
        );
        Pedido ped15 = criarPedido(
                beatriz, labCentral, null, agora.minusHours(8),
                StatusPedido.ENTREGUE, agora.minusHours(2),
                false, null, "Saída para preparo de solução desinfetante.",
                item(hipoclorito, 3, 3)
        );
        Pedido ped16 = criarPedido(
                lucas, labEcofisiologia, projEco, agora.minusHours(4),
                StatusPedido.PENDENTE, null,
                false, null, "Material para campanha de medições desta semana.",
                item(etanol, 2, null), item(luvas, 1, null)
        );

        mov(loteEtanol, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                60, 0, 60, agora.minusDays(30).withHour(8).withMinute(15), "Compra mensal - NF fictícia 4821.");
        mov(loteDna, gestor, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                100, 0, 100, agora.minusDays(29).withHour(10).withMinute(20), "Reposição de kit para biologia molecular.");
        mov(loteEtanol, gestor, ped01, labFitopatologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                5, 60, 55, agora.minusDays(27).withHour(14).withMinute(20), "Entrega do pedido ped01.");
        mov(lotePonteiras, gestor, ped01, labFitopatologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                2, 15, 13, agora.minusDays(27).withHour(14).withMinute(21), "Entrega do pedido ped01.");
        mov(loteDna, gestor, ped02, labBiomol, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                20, 100, 80, agora.minusDays(24).withHour(10).withMinute(5), "Entrega para extração de DNA.");
        mov(loteHcl, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                8000, 0, 8000, agora.minusDays(26).withHour(9).withMinute(30), "Entrada de produto fiscalizado.");
        mov(loteLuvas, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                20, 0, 20, agora.minusDays(26).withHour(10).withMinute(5), "Reposição de EPI.");
        mov(loteBdaDescartado, beatriz, null, labFitopatologia, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                8, 0, 8, agora.minusDays(35).withHour(11).withMinute(0), "Lote preparado no início do ciclo.");
        mov(loteBdaDescartado, gestor, null, labFitopatologia, TipoMovimentacao.DESCARTE_VENCIMENTO, OrigemMovimentacao.DESCARTE,
                8, 20, 12, agora.minusDays(5).withHour(16).withMinute(20), "Descarte integral por vencimento.");
        mov(loteBda, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                20, 0, 20, agora.minusDays(21).withHour(8).withMinute(40), "Novo lote de meio preparado.");
        mov(loteBda, gestor, ped04, labFitopatologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                8, 20, 12, agora.minusDays(19).withHour(15).withMinute(10), "Atendimento de rotina fitopatológica.");
        mov(loteLuvas, gestor, ped05, labEntomologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                2, 20, 18, agora.minusDays(18).withHour(15).withMinute(0), "Separação antes do cancelamento.");
        mov(loteLuvas, gestor, ped05, labEntomologia, TipoMovimentacao.DEVOLUCAO, OrigemMovimentacao.DEVOLUCAO,
                2, 18, 20, agora.minusDays(18).withHour(16).withMinute(25), "Devolução após cancelamento.");
        mov(loteMetanol, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                20, 0, 20, agora.minusDays(20).withHour(9).withMinute(0), "Entrada de solvente fiscalizado.");
        mov(lotePbs, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                40, 0, 40, agora.minusDays(18).withHour(8).withMinute(50), "Reposição de tampão.");
        mov(loteMetanol, gestor, ped06, labVirologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                2, 20, 18, agora.minusDays(14).withHour(11).withMinute(40), "Entrega para Virologia.");
        mov(lotePbs, gestor, ped06, labVirologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                4, 40, 36, agora.minusDays(14).withHour(11).withMinute(41), "Entrega para Virologia.");
        mov(loteEtanol, admin, null, null, TipoMovimentacao.AJUSTE, OrigemMovimentacao.INVENTARIO,
                1, 45, 46, agora.minusDays(13).withHour(17).withMinute(5), "Ajuste positivo após conferência física.");
        mov(loteDna, gestor, ped09, labVirologia, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                10, 80, 70, agora.minusDays(6).withHour(13).withMinute(25), "Entrega para análise molecular.");
        mov(loteMasterVencido, gestor, null, labBiomol, TipoMovimentacao.DESCARTE_VENCIMENTO, OrigemMovimentacao.DESCARTE,
                20, 28, 8, agora.minusDays(3).withHour(15).withMinute(50), "Descarte de lote vencido do Master Mix.");
        mov(loteKcl, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                1000, 0, 1000, agora.minusDays(5).withHour(9).withMinute(35), "Reposição de reagente para Solos.");
        mov(loteKcl, gestor, ped10, labSolos, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                250, 1000, 750, agora.minusDays(3).withHour(16).withMinute(0), "Entrega para preparo de solução padrão.");
        mov(loteHipoclorito, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                20, 0, 20, agora.minusDays(18).withHour(10).withMinute(10), "Reposição de solução de descontaminação.");
        mov(loteHipoclorito, gestor, ped15, labCentral, TipoMovimentacao.SAIDA, OrigemMovimentacao.PEDIDO,
                3, 20, 17, agora.minusHours(2), "Entrega concluída hoje.");
        mov(loteTmb, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                4, 3, 7, agora.minusHours(6), "Reposição recebida hoje; lote vencido antigo ainda pendente de descarte.");
        mov(lotePonteiras, admin, null, null, TipoMovimentacao.ENTRADA, OrigemMovimentacao.COMPRA,
                2, 9, 11, agora.minusHours(3), "Entrada complementar de material descartável.");

        criarResiduo(
                labBiomol, ana, projBiomol, gestor,
                "Mistura de tampões e solventes da extração de DNA vegetal",
                "Extração de DNA de folhas de mandioca",
                "Bombona PEAD 5 L", new BigDecimal("2.500"), UnidadeMedida.L,
                NivelRisco.ALTO, Set.of(TipoRisco.TOXICO, TipoRisco.INFLAMAVEL),
                StatusResiduo.INFORMADO, agora.minusHours(7),
                "Resíduo recém-informado; aguarda recebimento pela gestão.",
                "Abrigo químico - setor A", "Tratamento por empresa licenciada",
                null,
                componente(dnaKit, "Reagentes do kit de extração", true, "mistura residual"),
                componente(etanol, "Etanol", false, "aprox. 30%")
        );
        criarResiduo(
                labFitopatologia, gabriel, projFito, gestor,
                "Solução com formaldeído utilizada na fixação de material vegetal",
                "Fixação de amostras para análise fitopatológica",
                "Bombona homologada 10 L", new BigDecimal("4.200"), UnidadeMedida.L,
                NivelRisco.ALTO, Set.of(TipoRisco.TOXICO, TipoRisco.PERIGO_SAUDE),
                StatusResiduo.EM_ANALISE, agora.minusDays(1).withHour(10).withMinute(10),
                "Recebido pela gestão; conferência de risco em andamento.",
                "Abrigo químico - setor B", "Tratamento externo especializado",
                null,
                componente(formaldeido, "Formaldeído", true, "solução diluída")
        );
        criarResiduo(
                labEntomologia, larissa, projEnto, gestor,
                "Solução aquosa contaminada após ensaio com agente de controle",
                "Bioensaio de controle biológico",
                "Frasco âmbar 2 L", new BigDecimal("1.300"), UnidadeMedida.L,
                NivelRisco.MEDIO, Set.of(TipoRisco.PERIGO_AMBIENTAL, TipoRisco.IRRITANTE),
                StatusResiduo.EM_ANALISE, agora.minusDays(2).withHour(15).withMinute(20),
                "Amostra recebida; composição em validação.",
                "Abrigo químico - setor C", "Tratamento externo",
                null,
                componente(null, "Solução residual de bioensaio", true, "1,3 L")
        );
        criarResiduo(
                labFitopatologia, paulo, projFito, gestor,
                "Meio de cultura utilizado com crescimento fúngico inativado",
                "Isolamento de fungos fitopatogênicos",
                "Recipiente autoclavável 5 L", new BigDecimal("3.000"), UnidadeMedida.L,
                NivelRisco.MEDIO, Set.of(TipoRisco.BIOLOGICO),
                StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO, agora.minusDays(4).withHour(9).withMinute(0),
                "Material autoclavado; liberado para armazenamento temporário.",
                "Abrigo de resíduos biológicos - posição B04", "Coleta de resíduo biológico",
                hoje.plusDays(6),
                componente(bda, "Meio BDA utilizado", true, "3 L")
        );
        criarResiduo(
                labVirologia, fernanda, projViro, gestor,
                "Mistura de soluções de lavagem e substrato após ensaio ELISA",
                "Ensaio ELISA para diagnóstico de fitovírus",
                "Bombona 5 L", new BigDecimal("3.800"), UnidadeMedida.L,
                NivelRisco.MEDIO, Set.of(TipoRisco.IRRITANTE),
                StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, agora.minusDays(8).withHour(11).withMinute(35),
                "Rotulado e armazenado em área temporária.",
                "Abrigo químico - estante Q2", "Tratamento por empresa licenciada",
                hoje.plusDays(4),
                componente(tmb, "Substrato TMB residual", true, "traços"),
                componente(pbs, "Soluções de lavagem", false, "predominante")
        );
        criarResiduo(
                labCentral, beatriz, null, gestor,
                "Mistura de metanol e etanol de preparo e lavagem de vidrarias",
                "Preparo de soluções e lavagem técnica",
                "Bombona metálica homologada 20 L", new BigDecimal("11.500"), UnidadeMedida.L,
                NivelRisco.ALTO, Set.of(TipoRisco.INFLAMAVEL, TipoRisco.TOXICO),
                StatusResiduo.ARMAZENADO_TEMPORARIAMENTE, agora.minusDays(5).withHour(16).withMinute(10),
                "Resíduo segregado e aguardando coleta programada.",
                "Abrigo de inflamáveis - posição I03", "Incineração/coprocrocessamento externo",
                hoje.plusDays(2),
                componente(metanol, "Metanol", true, "aprox. 45%"),
                componente(etanol, "Etanol", false, "aprox. 35%")
        );
        criarResiduo(
                labBiomol, camila, projBiomol, gestor,
                "Soluções vencidas e consumíveis líquidos de PCR",
                "Rotina de PCR e descarte de reagentes vencidos",
                "Bombona 5 L", new BigDecimal("2.000"), UnidadeMedida.L,
                NivelRisco.BAIXO, Set.of(TipoRisco.IRRITANTE),
                StatusResiduo.DESPACHADO, agora.minusDays(15).withHour(10).withMinute(0),
                "Coleta concluída e comprovante registrado.",
                "Abrigo químico - posição Q1", "Tratamento externo especializado",
                hoje.minusDays(8),
                componente(masterMix, "Master Mix vencido", true, "resíduo líquido")
        );
        criarResiduo(
                labSolos, andre, projSolos, gestor,
                "Solução ácida residual de digestão de amostras de solo",
                "Digestão ácida para análise mineral",
                "Bombona PEAD 10 L", new BigDecimal("6.400"), UnidadeMedida.L,
                NivelRisco.ALTO, Set.of(TipoRisco.CORROSIVO),
                StatusResiduo.DESPACHADO, agora.minusDays(27).withHour(13).withMinute(30),
                "Resíduo despachado no primeiro ciclo do mês.",
                "Abrigo químico - setor A", "Neutralização e tratamento externo",
                hoje.minusDays(20),
                componente(hcl, "Ácido clorídrico diluído", true, "mistura ácida")
        );
        criarResiduo(
                labVirologia, fernanda, projViro, gestor,
                "Substrato cromogênico vencido separado para descarte",
                "Controle de validade dos reagentes do ELISA",
                "Frasco âmbar 500 mL", new BigDecimal("0.300"), UnidadeMedida.L,
                NivelRisco.BAIXO, Set.of(TipoRisco.IRRITANTE),
                StatusResiduo.INFORMADO, agora.minusDays(3).withHour(17).withMinute(10),
                "Aguardando recebimento formal pela gestão.",
                "Abrigo químico - estante Q2", "Tratamento químico externo",
                null,
                componente(tmb, "Substrato TMB vencido", true, "300 mL")
        );

        System.out.println("============================================================");
        System.out.println(" SGL DEMO - EMBRAPA MANDIOCA E FRUTICULTURA");
        System.out.println(" Cenário fictício de 30 dias carregado com sucesso.");
        System.out.println(" Gestão: ricardo.almeida@demo.sgl.local / " + SENHA_DEMO);
        System.out.println(" Admin:  mariana.souza@demo.sgl.local / " + SENHA_DEMO);
        System.out.println(" Solicitante: paulo.nascimento@demo.sgl.local / " + SENHA_DEMO);
        System.out.println(" Estagiária: ana.barbosa@demo.sgl.local / " + SENHA_DEMO);
        System.out.println("============================================================");
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome, String descricao) {
        return laboratorioRepository.save(
                new Laboratorio(null, null, unidade, nome, descricao, null, true)
        );
    }

    private Usuario criarUsuario(
            Unidade unidade,
            Laboratorio laboratorio,
            String nome,
            String email,
            Perfil perfil,
            boolean ativo) {

        return usuarioRepository.save(
                new Usuario(
                        null,
                        null,
                        nome,
                        email,
                        passwordEncoder.encode(SENHA_DEMO),
                        perfil,
                        unidade,
                        laboratorio,
                        ativo
                )
        );
    }

    private Estagiario criarEstagiario(
            Unidade unidade,
            Laboratorio laboratorio,
            String nome,
            String email,
            LocalDate inicio,
            LocalDate fim,
            TipoBolsa bolsa,
            boolean ativo,
            String observacao) {

        Estagiario estagiario = new Estagiario();
        estagiario.setNome(nome);
        estagiario.setEmail(email);
        estagiario.setSenha(passwordEncoder.encode(SENHA_DEMO));
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorio);
        estagiario.setAtivo(ativo);
        estagiario.setDataInicioEstagio(inicio);
        estagiario.setDataFimEstagio(fim);
        estagiario.setTipoBolsa(bolsa);
        estagiario.setObservacao(observacao);
        return estagiarioRepository.save(estagiario);
    }

    private Projeto criarProjeto(
            Laboratorio laboratorio,
            String nome,
            String descricao,
            LocalDate inicio,
            String responsavel) {

        return projetoRepository.save(
                Projeto.builder()
                        .laboratorio(laboratorio)
                        .nome(nome)
                        .descricao(descricao)
                        .dataInicio(inicio)
                        .responsavel(responsavel)
                        .ativo(true)
                        .build()
        );
    }

    private Produto criarProduto(
            String nome,
            String codigo,
            UnidadeMedida unidadeMedida,
            String descricao,
            String localizacao,
            NivelRisco nivelRisco,
            TipoRisco tipoRisco,
            String descricaoRisco,
            boolean perecivel,
            TipoPerecivel tipoPerecivel,
            String condicoesArmazenamento,
            String unidadeArmazenamento,
            boolean fiscalizado,
            Set<OrgaoFiscalizador> orgaos,
            String observacaoFiscalizacao) {

        return produtoRepository.save(
                Produto.builder()
                        .nome(nome)
                        .descricao(descricao)
                        .codigoReferencia(codigo)
                        .unidadeMedida(unidadeMedida)
                        .localizacaoFisica(localizacao)
                        .risco(nivelRisco)
                        .tipoRisco(tipoRisco)
                        .descricaoRisco(descricaoRisco)
                        .perecivel(perecivel)
                        .tipoPerecivel(tipoPerecivel)
                        .condicoesArmazenamento(condicoesArmazenamento)
                        .unidadeArmazenamento(unidadeArmazenamento)
                        .fiscalizado(fiscalizado)
                        .orgaosFiscalizadores(new LinkedHashSet<>(orgaos))
                        .observacaoFiscalizacao(observacaoFiscalizacao)
                        .ativo(true)
                        .build()
        );
    }

    private EstoqueCentral criarEstoque(Unidade unidade, Produto produto, int atual, int minimo) {
        return estoqueCentralRepository.save(
                EstoqueCentral.builder()
                        .unidade(unidade)
                        .produto(produto)
                        .quantidadeAtual(atual)
                        .quantidadeMinima(minimo)
                        .ativo(true)
                        .build()
        );
    }

    private Lote criarLote(
            EstoqueCentral estoque,
            int sequencial,
            String numeroFornecedor,
            TipoEmbalagem tipoEmbalagem,
            String apresentacao,
            int quantidadeInicial,
            int quantidadeDisponivel,
            LocalDate dataEntrada,
            LocalDate dataValidade,
            boolean ativo,
            boolean fracionavel,
            String observacao) {

        String sigla = estoque.getProduto().getCodigoReferencia()
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
        lote.setNumeroLote(numeroFornecedor);
        lote.setTipoEmbalagem(tipoEmbalagem);
        lote.setApresentacao(apresentacao);
        lote.setQuantidadeApresentacoes(quantidadeInicial);
        lote.setConteudoPorApresentacao(1);
        lote.setFracionavel(fracionavel);
        lote.setObservacao(observacao);
        lote.setQuantidadeInicial(quantidadeInicial);
        lote.setQuantidadeDisponivel(quantidadeDisponivel);
        lote.setDataEntrada(dataEntrada);
        lote.setDataValidade(dataValidade);
        lote.setAtivo(ativo);
        return loteRepository.save(lote);
    }

    private Pedido criarPedido(
            Usuario usuario,
            Laboratorio laboratorio,
            Projeto projeto,
            LocalDateTime dataSolicitacao,
            StatusPedido status,
            LocalDateTime dataEntrega,
            boolean urgente,
            String motivoUrgencia,
            String observacao,
            ItemDemo... itens) {

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(laboratorio)
                .projeto(projeto)
                .dataSolicitacao(dataSolicitacao)
                .dataEntrega(dataEntrega)
                .status(status)
                .urgente(urgente)
                .motivoUrgencia(motivoUrgencia)
                .observacao(observacao)
                .itens(new ArrayList<>())
                .build();

        for (ItemDemo item : itens) {
            ItemPedido entidade = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(item.produto())
                    .quantidadeSolicitada(item.solicitada())
                    .quantidadeAprovada(item.aprovada())
                    .build();
            pedido.getItens().add(entidade);
        }
        return pedidoRepository.save(pedido);
    }

    private static ItemDemo item(Produto produto, int solicitada, Integer aprovada) {
        return new ItemDemo(produto, solicitada, aprovada);
    }

    private void mov(
            Lote lote,
            Usuario usuario,
            Pedido pedido,
            Laboratorio laboratorio,
            TipoMovimentacao tipo,
            OrigemMovimentacao origem,
            int quantidade,
            int anterior,
            int atual,
            LocalDateTime quando,
            String observacao) {

        movimentacaoEstoqueRepository.save(
                MovimentacaoEstoque.builder()
                        .produto(lote.getEstoqueCentral().getProduto())
                        .laboratorio(laboratorio)
                        .usuario(usuario)
                        .pedido(pedido)
                        .lote(lote)
                        .tipoMovimentacao(tipo)
                        .origem(origem)
                        .quantidadeMovimentada(quantidade)
                        .quantidadeAnterior(anterior)
                        .quantidadeAtual(atual)
                        .dataMovimentacao(quando)
                        .observacao(observacao)
                        .estoqueCentral(lote.getEstoqueCentral())
                        .build()
        );
    }

    private Residuo criarResiduo(
            Laboratorio laboratorio,
            Usuario gerador,
            Projeto projeto,
            Usuario gestor,
            String descricao,
            String processoOrigem,
            String recipiente,
            BigDecimal quantidade,
            UnidadeMedida unidade,
            NivelRisco nivelInformado,
            Set<TipoRisco> riscos,
            StatusResiduo statusFinal,
            LocalDateTime dataInformacao,
            String observacao,
            String localArmazenamento,
            String destinoPrevisto,
            LocalDate dataPrevistaDespacho,
            ComponenteDemo... componentes) {

        Residuo residuo = Residuo.builder()
                .laboratorio(laboratorio)
                .gerador(gerador)
                .projeto(projeto)
                .descricao(descricao)
                .processoOrigem(processoOrigem)
                .recipiente(recipiente)
                .quantidade(quantidade)
                .unidadeMedida(unidade)
                .nivelRiscoInformado(nivelInformado)
                .riscosInformados(new LinkedHashSet<>(riscos))
                .observacaoGerador(observacao)
                .status(StatusResiduo.INFORMADO)
                .dataInformacao(dataInformacao)
                .build();

        for (ComponenteDemo componente : componentes) {
            residuo.addComponente(
                    ComponenteResiduo.builder()
                            .produto(componente.produto())
                            .nomeComponente(componente.nome())
                            .principal(componente.principal())
                            .concentracaoOuQuantidade(componente.quantidade())
                            .build()
            );
        }

        residuo = residuoRepository.save(residuo);
        residuo.setCodigoRastreio(
                String.format("SGL-RES-%d-%06d", dataInformacao.getYear(), residuo.getId())
        );

        salvarHistorico(
                residuo, gerador, StatusResiduo.INFORMADO,
                "RESIDUO_INFORMADO", observacao, dataInformacao
        );

        if (atingiu(statusFinal, StatusResiduo.EM_ANALISE)) {
            LocalDateTime recebimento = dataInformacao.plusHours(4);
            residuo.setGestorResponsavel(gestor);
            residuo.setDataRecebimento(recebimento);
            residuo.setStatus(StatusResiduo.EM_ANALISE);
            salvarHistorico(
                    residuo, gestor, StatusResiduo.EM_ANALISE,
                    "RECEBIDO_PELA_GESTAO", "Recebido para conferência e análise.", recebimento
            );
        }

        if (atingiu(statusFinal, StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO)) {
            LocalDateTime liberacao = dataInformacao.plusDays(1).plusHours(2);
            residuo.setNivelRiscoConfirmado(nivelInformado);
            residuo.setRiscosConfirmados(new LinkedHashSet<>(riscos));
            residuo.setObservacaoGestor(observacao);
            residuo.setLocalArmazenamentoTemporario(localArmazenamento);
            residuo.setDestinoFinalPrevisto(destinoPrevisto);
            residuo.setDataPrevistaDespacho(dataPrevistaDespacho);
            residuo.setDataLiberacao(liberacao);
            residuo.setQrCodeConteudo("SGL-RESIDUO:" + residuo.getPublicId());
            residuo.setStatus(StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO);
            salvarHistorico(
                    residuo, gestor, StatusResiduo.LIBERADO_PARA_ARMAZENAMENTO,
                    "RISCO_CONFERIDO_E_RESIDUO_LIBERADO", "Risco conferido e rótulo liberado.", liberacao
            );
        }

        if (atingiu(statusFinal, StatusResiduo.ARMAZENADO_TEMPORARIAMENTE)) {
            LocalDateTime armazenamento = dataInformacao.plusDays(2).plusHours(1);
            residuo.setDataArmazenamentoTemporario(armazenamento);
            residuo.setStatus(StatusResiduo.ARMAZENADO_TEMPORARIAMENTE);
            salvarHistorico(
                    residuo, gestor, StatusResiduo.ARMAZENADO_TEMPORARIAMENTE,
                    "ARMAZENAMENTO_TEMPORARIO_CONFIRMADO", localArmazenamento, armazenamento
            );
        }

        if (atingiu(statusFinal, StatusResiduo.DESPACHADO)) {
            LocalDateTime despacho = dataInformacao.plusDays(5).plusHours(3);
            residuo.setDataDespacho(despacho);
            residuo.setDestinoFinalConfirmado(destinoPrevisto);
            residuo.setStatus(StatusResiduo.DESPACHADO);
            salvarHistorico(
                    residuo, gestor, StatusResiduo.DESPACHADO,
                    "DESPACHO_CONFIRMADO", destinoPrevisto, despacho
            );
        }

        residuo.setStatus(statusFinal);
        return residuoRepository.save(residuo);
    }

    private void salvarHistorico(
            Residuo residuo,
            Usuario usuario,
            StatusResiduo status,
            String acao,
            String observacao,
            LocalDateTime data) {

        historicoResiduoRepository.save(
                HistoricoResiduo.builder()
                        .residuo(residuo)
                        .usuario(usuario)
                        .status(status)
                        .acao(acao)
                        .observacao(observacao)
                        .dataHora(data)
                        .build()
        );
    }

    private static boolean atingiu(StatusResiduo finalStatus, StatusResiduo etapa) {
        return finalStatus.ordinal() >= etapa.ordinal();
    }

    private static ComponenteDemo componente(
            Produto produto,
            String nome,
            boolean principal,
            String quantidade) {
        return new ComponenteDemo(produto, nome, principal, quantidade);
    }

    private record ItemDemo(Produto produto, int solicitada, Integer aprovada) {
    }

    private record ComponenteDemo(Produto produto, String nome, boolean principal, String quantidade) {
    }
}
