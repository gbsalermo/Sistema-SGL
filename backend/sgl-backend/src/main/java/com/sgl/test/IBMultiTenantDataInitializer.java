package com.sgl.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.sgl.model.Estagiario;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.ItemPedido;
import com.sgl.model.Laboratorio;
import com.sgl.model.Lote;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Projeto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Massa pequena e idempotente para validar isolamento entre unidades no profile dev.
 * Usa a unidade IB e seus laboratorios ja existentes; nao cria outra unidade.
 */
@Profile("dev")
@Component
@Order(1000)
@RequiredArgsConstructor
public class IBMultiTenantDataInitializer implements CommandLineRunner {

    private static final String SENHA = "123456";

    private final UnidadeRepository unidadeRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstagiarioRepository estagiarioRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;
    private final PedidoRepository pedidoRepository;
    private final ProjetoRepository projetoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Unidade ib = unidadeRepository.findAll().stream()
                .filter(u -> "IB".equalsIgnoreCase(u.getSigla()))
                .findFirst()
                .orElse(null);

        if (ib == null) {
            System.out.println("=== MULTITENANT IB: unidade IB ainda nao existe; carga complementar ignorada. ===");
            return;
        }

        List<Laboratorio> labs = laboratorioRepository.findByUnidadeId(ib.getId()).stream()
                .filter(lab -> Boolean.TRUE.equals(lab.getAtivo()))
                .toList();

        if (labs.isEmpty()) {
            System.out.println("=== MULTITENANT IB: unidade IB sem laboratorio ativo; carga ignorada. ===");
            return;
        }

        Laboratorio labPrincipal = labs.get(0);
        Laboratorio labSecundario = labs.size() > 1 ? labs.get(1) : labPrincipal;

        Usuario admin = usuario("Admin IB", "admin.ib@sgl.local", Perfil.ADMINISTRADOR, ib, labPrincipal);
        Usuario gestor = usuario("Gestor IB", "gestor.ib@sgl.local", Perfil.GESTOR, ib, labPrincipal);
        Usuario tecnico = usuario("Tecnico IB", "tecnico.ib@sgl.local", Perfil.TECNICO, ib, labPrincipal);
        Usuario analista = usuario("Analista IB", "analista.ib@sgl.local", Perfil.ANALISTA, ib, labSecundario);
        Usuario pesquisador = usuario("Pesquisador IB", "pesquisador.ib@sgl.local", Perfil.PESQUISADOR, ib, labSecundario);
        estagiario("Estagiario IB", "estagiario.ib@sgl.local", ib, labSecundario);

        Produto masterMix = produto(
                "Master Mix PCR 2X - IB", "IB-PCR-2X", UnidadeMedida.REACAO,
                "Mistura pronta para PCR usada na massa multitenant do IB.",
                "Freezer -20 C - " + labPrincipal.getNome(),
                true, TipoPerecivel.QUIMICO, NivelRisco.NENHUM, null,
                "frasco para 100 reacoes"
        );
        Produto agarose = produto(
                "Agarose Grau Molecular - IB", "IB-AGR-MOL", UnidadeMedida.G,
                "Agarose para eletroforese usada na massa multitenant do IB.",
                "Armario de reagentes - " + labSecundario.getNome(),
                false, null, NivelRisco.NENHUM, null,
                "frasco de 500 g"
        );

        EstoqueCentral estoqueMaster = estoque(ib, masterMix, 18, 8);
        EstoqueCentral estoqueAgarose = estoque(ib, agarose, 900, 300);

        lote(estoqueMaster, "IB-PCR-TESTE-01", TipoEmbalagem.UNITARIO,
                "frasco para 100 reacoes", 18, LocalDate.now().plusMonths(4));
        lote(estoqueAgarose, "IB-AGR-TESTE-01", TipoEmbalagem.UNITARIO,
                "frasco de 500 g", 900, null);

        Projeto projeto = projetoRepository.findAll().stream()
                .filter(p -> p.getLaboratorio() != null
                        && p.getLaboratorio().getUnidade() != null
                        && p.getLaboratorio().getUnidade().getId().equals(ib.getId())
                        && "Projeto Multitenant IB".equals(p.getNome()))
                .findFirst()
                .orElseGet(() -> projetoRepository.save(Projeto.builder()
                        .laboratorio(labSecundario)
                        .nome("Projeto Multitenant IB")
                        .descricao("Projeto ficticio para validar isolamento de dados entre unidades.")
                        .dataInicio(LocalDate.now().minusDays(20))
                        .responsavel(pesquisador.getNome())
                        .ativo(true)
                        .build()));

        pedido("IB-PENDENTE", pesquisador, labSecundario, projeto, masterMix, 3,
                StatusPedido.PENDENTE, LocalDateTime.now().minusHours(6));
        pedido("IB-APROVADO", tecnico, labPrincipal, projeto, agarose, 100,
                StatusPedido.APROVADO, LocalDateTime.now().minusDays(1));
        pedido("IB-ENTREGUE", analista, labSecundario, projeto, masterMix, 2,
                StatusPedido.ENTREGUE, LocalDateTime.now().minusDays(3));

        System.out.println("=== MULTITENANT IB: massa complementar pronta. ===");
        System.out.println("=== Usuarios IB: admin/gestor/tecnico/analista/pesquisador/estagiario @sgl.local | senha 123456 ===");
    }

    private Usuario usuario(String nome, String email, Perfil perfil, Unidade unidade, Laboratorio lab) {
        return usuarioRepository.findByEmail(email).orElseGet(() -> usuarioRepository.save(
                new Usuario(null, null, nome, email, passwordEncoder.encode(SENHA), perfil, unidade, lab, true)
        ));
    }

    private Estagiario estagiario(String nome, String email, Unidade unidade, Laboratorio lab) {
        return estagiarioRepository.findAll().stream()
                .filter(e -> email.equalsIgnoreCase(e.getEmail()))
                .findFirst()
                .orElseGet(() -> {
                    Estagiario e = new Estagiario();
                    e.setNome(nome);
                    e.setEmail(email);
                    e.setSenha(passwordEncoder.encode(SENHA));
                    e.setPerfil(Perfil.ESTAGIARIO);
                    e.setUnidade(unidade);
                    e.setLaboratorio(lab);
                    e.setAtivo(true);
                    e.setDataInicioEstagio(LocalDate.now().minusMonths(2));
                    e.setTipoBolsa(TipoBolsa.BOLSA_INSTITUCIONAL);
                    e.setObservacao("Usuario ficticio para teste multitenant do IB.");
                    return estagiarioRepository.save(e);
                });
    }

    private Produto produto(String nome, String codigo, UnidadeMedida unidadeMedida,
                            String descricao, String localizacao, boolean perecivel,
                            TipoPerecivel tipoPerecivel, NivelRisco risco, TipoRisco tipoRisco,
                            String unidadeArmazenamento) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome).stream()
                .filter(p -> codigo.equalsIgnoreCase(p.getCodigoReferencia()))
                .findFirst()
                .orElseGet(() -> produtoRepository.save(Produto.builder()
                        .nome(nome)
                        .codigoReferencia(codigo)
                        .unidadeMedida(unidadeMedida)
                        .descricao(descricao)
                        .localizacaoFisica(localizacao)
                        .risco(risco)
                        .tipoRisco(tipoRisco)
                        .perecivel(perecivel)
                        .tipoPerecivel(tipoPerecivel)
                        .condicoesArmazenamento(perecivel ? "Conservar conforme indicacao do fabricante." : "Ambiente seco e protegido.")
                        .unidadeArmazenamento(unidadeArmazenamento)
                        .ativo(true)
                        .build()));
    }

    private EstoqueCentral estoque(Unidade unidade, Produto produto, int quantidade, int minimo) {
        return estoqueCentralRepository.findAll().stream()
                .filter(e -> e.getUnidade().getId().equals(unidade.getId())
                        && e.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .orElseGet(() -> estoqueCentralRepository.save(EstoqueCentral.builder()
                        .unidade(unidade)
                        .produto(produto)
                        .quantidadeAtual(quantidade)
                        .quantidadeMinima(minimo)
                        .ativo(true)
                        .build()));
    }

    private Lote lote(EstoqueCentral estoque, String numeroLote, TipoEmbalagem tipoEmbalagem,
                      String apresentacao, int quantidade, LocalDate validade) {
        return loteRepository.findByEstoqueCentralId(estoque.getId()).stream()
                .filter(l -> numeroLote.equals(l.getNumeroLote()))
                .findFirst()
                .orElseGet(() -> {
                    Produto produto = estoque.getProduto();
                    int sequencial = loteRepository.buscarMaiorSequencialInternoPorProduto(produto.getId()) + 1;
                    String sigla = produto.getCodigoReferencia().trim().toUpperCase(Locale.ROOT)
                            .replaceAll("[^A-Z0-9]+", "-").replaceAll("^-+|-+$", "");
                    Lote l = new Lote();
                    l.setEstoqueCentral(estoque);
                    l.definirCodigoInterno("LOT-" + sigla + "-" + String.format(Locale.ROOT, "%03d", sequencial), sequencial);
                    l.setNumeroLote(numeroLote);
                    l.setTipoEmbalagem(tipoEmbalagem);
                    l.setApresentacao(apresentacao);
                    l.setQuantidadeApresentacoes(quantidade);
                    l.setConteudoPorApresentacao(1);
                    l.setFracionavel(true);
                    l.setQuantidadeInicial(quantidade);
                    l.setQuantidadeDisponivel(quantidade);
                    l.setDataEntrada(LocalDate.now().minusDays(12));
                    l.setDataValidade(validade);
                    l.setAtivo(true);
                    return loteRepository.save(l);
                });
    }

    private void pedido(String marcador, Usuario usuario, Laboratorio lab, Projeto projeto,
                        Produto produto, int quantidade, StatusPedido status, LocalDateTime data) {
        boolean existe = pedidoRepository.findAll().stream()
                .anyMatch(p -> p.getObservacao() != null && p.getObservacao().contains(marcador));
        if (existe) return;

        Pedido pedido = Pedido.builder()
                .usuario(usuario)
                .laboratorio(lab)
                .projeto(projeto)
                .dataSolicitacao(data)
                .status(status)
                .observacao("[" + marcador + "] Pedido ficticio para teste multitenant IB.")
                .itens(new ArrayList<>())
                .build();
        ItemPedido item = ItemPedido.builder()
                .pedido(pedido)
                .produto(produto)
                .quantidadeSolicitada(quantidade)
                .build();
        pedido.getItens().add(item);
        pedidoRepository.save(pedido);
    }
}
