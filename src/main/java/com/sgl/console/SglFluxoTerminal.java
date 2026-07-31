package com.sgl.console;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.StatusPedido;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

public class SglFluxoTerminal {

    private final Scanner scanner = new Scanner(System.in);
    private final Map<Long, Unidade> unidades = new LinkedHashMap<>();
    private final Map<Long, Laboratorio> laboratorios = new LinkedHashMap<>();
    private final Map<Long, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<Long, Produto> produtos = new LinkedHashMap<>();
    private final Map<Long, EstoqueCentral> estoques = new LinkedHashMap<>();
    private final Map<Long, Projeto> projetos = new LinkedHashMap<>();
    private final Map<Long, Pedido> pedidos = new LinkedHashMap<>();
    private final List<HistoricoItem> historicos = new ArrayList<>();

    private long seqUnidade = 1;
    private long seqLaboratorio = 1;
    private long seqUsuario = 1;
    private long seqProduto = 1;
    private long seqEstoque = 1;
    private long seqProjeto = 1;
    private long seqPedido = 1;

    public static void main(String[] args) {
        new SglFluxoTerminal().run();
    }

    private void run() {
        while (true) {
            printMenu();
            String op = scanner.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> carregarCenarioExemplo();
                    case "2" -> cadastrarUnidade();
                    case "3" -> cadastrarLaboratorio();
                    case "4" -> cadastrarUsuario();
                    case "5" -> cadastrarProduto();
                    case "6" -> cadastrarEstoqueCentral();
                    case "7" -> cadastrarProjeto();
                    case "8" -> criarPedido();
                    case "9" -> aprovarPedido();
                    case "10" -> entregarPedido();
                    case "11" -> listarResumo();
                    case "0" -> {
                        System.out.println("Encerrando simulador.");
                        return;
                    }
                    default -> System.out.println("Opcao invalida.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Erro: " + ex.getMessage());
            }

            pause();
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== SGL - Simulador de Fluxo Principal ===");
        System.out.println("1. Carregar cenario de exemplo");
        System.out.println("2. Cadastrar unidade");
        System.out.println("3. Cadastrar laboratorio");
        System.out.println("4. Cadastrar usuario");
        System.out.println("5. Cadastrar produto");
        System.out.println("6. Cadastrar estoque central");
        System.out.println("7. Cadastrar projeto");
        System.out.println("8. Criar pedido");
        System.out.println("9. Aprovar pedido");
        System.out.println("10. Entregar pedido");
        System.out.println("11. Listar resumo");
        System.out.println("0. Sair");
        System.out.print("Opcao: ");
    }

    private void carregarCenarioExemplo() {
        unidades.clear();
        laboratorios.clear();
        usuarios.clear();
        produtos.clear();
        estoques.clear();
        projetos.clear();
        pedidos.clear();
        historicos.clear();

        Unidade unidade = new Unidade(seqUnidade++, "Instituto de Biologia", "IB");
        unidades.put(unidade.id, unidade);

        Laboratorio laboratorio = new Laboratorio(seqLaboratorio++, unidade.id, "Microbiologia");
        laboratorios.put(laboratorio.id, laboratorio);

        Usuario usuario = new Usuario(seqUsuario++, "Ana Pesquisadora", "ana@sgl.com", Perfil.PESQUISADOR, laboratorio.id);
        usuarios.put(usuario.id, usuario);

        Produto produto = new Produto(seqProduto++, "Alcool 70%", UnidadeMedida.L, NivelRisco.BAIXO, TipoRisco.INFLAMAVEL, false, null, null);
        produtos.put(produto.id, produto);

        EstoqueCentral estoque = new EstoqueCentral(seqEstoque++, produto.id, 10, 2);
        estoques.put(estoque.id, estoque);

        Projeto projeto = new Projeto(seqProjeto++, laboratorio.id, "Projeto Demo");
        projetos.put(projeto.id, projeto);

        Pedido pedido = new Pedido(seqPedido++, usuario.id, laboratorio.id, projeto.id, "Pedido inicial do fluxo");
        pedido.itens.add(new PedidoItem(produto.id, 3));
        pedidos.put(pedido.id, pedido);

        System.out.println("Cenario carregado.");
    }

    private void cadastrarUnidade() {
        String nome = prompt("Nome da unidade: ");
        String sigla = prompt("Sigla: ");
        Unidade unidade = new Unidade(seqUnidade++, nome, sigla);
        unidades.put(unidade.id, unidade);
        System.out.println("Unidade criada com id " + unidade.id);
    }

    private void cadastrarLaboratorio() {
        Unidade unidade = getUnidadeById(promptLong("Id da unidade: "));
        String nome = prompt("Nome do laboratorio: ");
        Laboratorio laboratorio = new Laboratorio(seqLaboratorio++, unidade.id, nome);
        laboratorios.put(laboratorio.id, laboratorio);
        System.out.println("Laboratorio criado com id " + laboratorio.id);
    }

    private void cadastrarUsuario() {
        Laboratorio laboratorio = getLaboratorioById(promptLong("Id do laboratorio: "));
        String nome = prompt("Nome do usuario: ");
        String email = prompt("Email: ");
        Perfil perfil = promptEnum("Perfil", Perfil.class);
        Usuario usuario = new Usuario(seqUsuario++, nome, email, perfil, laboratorio.id);
        usuarios.put(usuario.id, usuario);
        System.out.println("Usuario criado com id " + usuario.id);
    }

    private void cadastrarProduto() {
        String nome = prompt("Nome do produto: ");
        UnidadeMedida unidadeMedida = promptEnum("Unidade de medida", UnidadeMedida.class);
        NivelRisco risco = promptEnum("Nivel de risco", NivelRisco.class);
        TipoRisco tipoRisco = promptEnum("Tipo de risco", TipoRisco.class);
        String perecivelTexto = prompt("Perecivel? (s/n): ");
        boolean perecivel = perecivelTexto.equalsIgnoreCase("s");
        TipoPerecivel tipoPerecivel = null;
        LocalDate validade = null;
        if (perecivel) {
            tipoPerecivel = promptEnum("Tipo de perecivel", TipoPerecivel.class);
            validade = LocalDate.parse(prompt("Data de validade (yyyy-MM-dd): "));
        }
        Produto produto = new Produto(seqProduto++, nome, unidadeMedida, risco, tipoRisco, perecivel, tipoPerecivel, validade);
        produtos.put(produto.id, produto);
        System.out.println("Produto criado com id " + produto.id);
    }

    private void cadastrarEstoqueCentral() {
        Produto produto = getProdutoById(promptLong("Id do produto: "));
        int quantidadeAtual = promptInt("Quantidade atual: ");
        int quantidadeMinima = promptInt("Quantidade minima: ");
        if (estoques.values().stream().anyMatch(e -> e.produtoId == produto.id)) {
            throw new IllegalArgumentException("Ja existe estoque para este produto.");
        }
        EstoqueCentral estoque = new EstoqueCentral(seqEstoque++, produto.id, quantidadeAtual, quantidadeMinima);
        estoques.put(estoque.id, estoque);
        System.out.println("Estoque central criado com id " + estoque.id);
    }

    private void cadastrarProjeto() {
        Laboratorio laboratorio = getLaboratorioById(promptLong("Id do laboratorio: "));
        String nome = prompt("Nome do projeto: ");
        Projeto projeto = new Projeto(seqProjeto++, laboratorio.id, nome);
        projetos.put(projeto.id, projeto);
        System.out.println("Projeto criado com id " + projeto.id);
    }

    private void criarPedido() {
        Usuario usuario = getUsuarioById(promptLong("Id do usuario: "));
        Laboratorio laboratorio = getLaboratorioById(promptLong("Id do laboratorio: "));
        Long projetoId = null;
        String vincularProjeto = prompt("Vincular a projeto? (s/n): ");
        if (vincularProjeto.equalsIgnoreCase("s")) {
            Projeto projeto = getProjetoById(promptLong("Id do projeto: "));
            projetoId = projeto.id;
        }

        Pedido pedido = new Pedido(seqPedido++, usuario.id, laboratorio.id, projetoId, prompt("Observacao: "));
        while (true) {
            Produto produto = getProdutoById(promptLong("Id do produto (0 para finalizar): "));
            if (produto.id == 0L) {
                break;
            }
            int quantidade = promptInt("Quantidade solicitada: ");
            pedido.itens.add(new PedidoItem(produto.id, quantidade));
            String mais = prompt("Adicionar outro item? (s/n): ");
            if (!mais.equalsIgnoreCase("s")) {
                break;
            }
        }

        if (pedido.itens.isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos um item.");
        }

        pedidos.put(pedido.id, pedido);
        System.out.println("Pedido criado com id " + pedido.id + " (status PENDENTE)");
    }

    private void aprovarPedido() {
        Pedido pedido = getPedidoById(promptLong("Id do pedido: "));
        if (pedido.status != StatusPedido.PENDENTE) {
            throw new IllegalArgumentException("Apenas pedidos pendentes podem ser aprovados.");
        }

        for (PedidoItem item : pedido.itens) {
            EstoqueCentral estoque = getEstoqueByProdutoId(item.produtoId);
            if (estoque.quantidadeAtual < item.quantidadeSolicitada) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto " + item.produtoId);
            }
        }

        for (PedidoItem item : pedido.itens) {
            EstoqueCentral estoque = getEstoqueByProdutoId(item.produtoId);
            estoque.quantidadeAtual -= item.quantidadeSolicitada;
            item.quantidadeAprovada = item.quantidadeSolicitada;
        }

        pedido.status = StatusPedido.APROVADO;
        System.out.println("Pedido aprovado e estoque baixado.");
    }

    private void entregarPedido() {
        Pedido pedido = getPedidoById(promptLong("Id do pedido: "));
        if (pedido.status != StatusPedido.APROVADO) {
            throw new IllegalArgumentException("Apenas pedidos aprovados podem ser entregues.");
        }

        for (PedidoItem item : pedido.itens) {
            historicos.add(new HistoricoItem(getPedidoById(pedido.id).laboratorioId, item.produtoId, item.quantidadeAprovada, LocalDateTime.now(), pedido.id));
        }

        pedido.status = StatusPedido.ENTREGUE;
        System.out.println("Pedido entregue e historico registrado.");
    }

    private void listarResumo() {
        System.out.println();
        System.out.println("=== RESUMO ===");
        System.out.println("Unidades: " + unidades.size());
        System.out.println("Laboratorios: " + laboratorios.size());
        System.out.println("Usuarios: " + usuarios.size());
        System.out.println("Produtos: " + produtos.size());
        System.out.println("Estoques: " + estoques.size());
        System.out.println("Projetos: " + projetos.size());
        System.out.println("Pedidos: " + pedidos.size());
        System.out.println("Historicos: " + historicos.size());
        for (Pedido pedido : pedidos.values()) {
            System.out.println("- Pedido " + pedido.id + " | status=" + pedido.status + " | itens=" + pedido.itens.size());
        }
    }

    private Unidade getUnidadeById(Long id) {
        Unidade unidade = unidades.get(id);
        if (unidade == null) {
            throw new IllegalArgumentException("Unidade nao encontrada.");
        }
        return unidade;
    }

    private Laboratorio getLaboratorioById(Long id) {
        Laboratorio laboratorio = laboratorios.get(id);
        if (laboratorio == null) {
            throw new IllegalArgumentException("Laboratorio nao encontrado.");
        }
        return laboratorio;
    }

    private Usuario getUsuarioById(Long id) {
        Usuario usuario = usuarios.get(id);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario nao encontrado.");
        }
        return usuario;
    }

    private Produto getProdutoById(Long id) {
        if (id == 0L) {
            return new Produto(0L, "", UnidadeMedida.UNIDADE, NivelRisco.NENHUM, TipoRisco.NENHUM, false, null, null);
        }
        Produto produto = produtos.get(id);
        if (produto == null) {
            throw new IllegalArgumentException("Produto nao encontrado.");
        }
        return produto;
    }

    private Projeto getProjetoById(Long id) {
        Projeto projeto = projetos.get(id);
        if (projeto == null) {
            throw new IllegalArgumentException("Projeto nao encontrado.");
        }
        return projeto;
    }

    private Pedido getPedidoById(Long id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido nao encontrado.");
        }
        return pedido;
    }

    private EstoqueCentral getEstoqueByProdutoId(Long produtoId) {
        return estoques.values().stream()
                .filter(e -> e.produtoId == produtoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estoque central nao encontrado para o produto."));
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private Long promptLong(String label) {
        String value = prompt(label);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor numerico invalido.");
        }
    }

    private int promptInt(String label) {
        String value = prompt(label);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor numerico invalido.");
        }
    }

    private <E extends Enum<E>> E promptEnum(String label, Class<E> enumType) {
        System.out.println(label + ":");
        E[] values = enumType.getEnumConstants();
        for (int i = 0; i < values.length; i++) {
            System.out.println((i + 1) + ". " + values[i].name());
        }
        int escolha = promptInt("Escolha: ");
        if (escolha < 1 || escolha > values.length) {
            throw new IllegalArgumentException("Escolha invalida.");
        }
        return values[escolha - 1];
    }

    private void pause() {
        System.out.print("Pressione ENTER para continuar...");
        scanner.nextLine();
    }

    private static final class Unidade {
        final long id;
        final String nome;
        final String sigla;

        Unidade(long id, String nome, String sigla) {
            this.id = id;
            this.nome = nome;
            this.sigla = sigla;
        }
    }

    private static final class Laboratorio {
        final long id;
        final long unidadeId;
        final String nome;

        Laboratorio(long id, long unidadeId, String nome) {
            this.id = id;
            this.unidadeId = unidadeId;
            this.nome = nome;
        }
    }

    private static final class Usuario {
        final long id;
        final String nome;
        final String email;
        final Perfil perfil;
        final long laboratorioId;

        Usuario(long id, String nome, String email, Perfil perfil, long laboratorioId) {
            this.id = id;
            this.nome = nome;
            this.email = email;
            this.perfil = perfil;
            this.laboratorioId = laboratorioId;
        }
    }

    private static final class Produto {
        final long id;
        final String nome;
        final UnidadeMedida unidadeMedida;
        final NivelRisco nivelRisco;
        final TipoRisco tipoRisco;
        final boolean perecivel;
        final TipoPerecivel tipoPerecivel;
        final LocalDate dataValidade;

        Produto(long id, String nome, UnidadeMedida unidadeMedida, NivelRisco nivelRisco,
                TipoRisco tipoRisco, boolean perecivel, TipoPerecivel tipoPerecivel, LocalDate dataValidade) {
            this.id = id;
            this.nome = nome;
            this.unidadeMedida = unidadeMedida;
            this.nivelRisco = nivelRisco;
            this.tipoRisco = tipoRisco;
            this.perecivel = perecivel;
            this.tipoPerecivel = tipoPerecivel;
            this.dataValidade = dataValidade;
        }
    }

    private static final class EstoqueCentral {
        final long id;
        final long produtoId;
        int quantidadeAtual;
        final int quantidadeMinima;

        EstoqueCentral(long id, long produtoId, int quantidadeAtual, int quantidadeMinima) {
            this.id = id;
            this.produtoId = produtoId;
            this.quantidadeAtual = quantidadeAtual;
            this.quantidadeMinima = quantidadeMinima;
        }
    }

    private static final class Projeto {
        final long id;
        final long laboratorioId;
        final String nome;

        Projeto(long id, long laboratorioId, String nome) {
            this.id = id;
            this.laboratorioId = laboratorioId;
            this.nome = nome;
        }
    }

    private static final class Pedido {
        final long id;
        final long usuarioId;
        final long laboratorioId;
        final Long projetoId;
        final String observacao;
        final List<PedidoItem> itens = new ArrayList<>();
        StatusPedido status = StatusPedido.PENDENTE;

        Pedido(long id, long usuarioId, long laboratorioId, Long projetoId, String observacao) {
            this.id = id;
            this.usuarioId = usuarioId;
            this.laboratorioId = laboratorioId;
            this.projetoId = projetoId;
            this.observacao = observacao;
        }
    }

    private static final class PedidoItem {
        final long produtoId;
        final int quantidadeSolicitada;
        int quantidadeAprovada;

        PedidoItem(long produtoId, int quantidadeSolicitada) {
            this.produtoId = produtoId;
            this.quantidadeSolicitada = quantidadeSolicitada;
        }
    }

    private static final class HistoricoItem {
        final long laboratorioId;
        final long produtoId;
        final int quantidade;
        final LocalDateTime data;
        final long pedidoId;

        HistoricoItem(long laboratorioId, long produtoId, int quantidade, LocalDateTime data, long pedidoId) {
            this.laboratorioId = laboratorioId;
            this.produtoId = produtoId;
            this.quantidade = quantidade;
            this.data = data;
            this.pedidoId = pedidoId;
        }
    }
}
