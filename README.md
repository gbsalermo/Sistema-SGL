# 📦 SGL — Sistema de Gestão de Laboratórios

[![Java](https://img.shields.io/badge/Java-17+-orange)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.x-green)](https://vuejs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)](https://www.postgresql.org)
[![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)]()

## 📋 Sobre o projeto

O **SGL — Sistema de Gestão de Laboratórios** automatiza e centraliza o controle de materiais em laboratórios de pesquisa e ensino.

O sistema organiza a operação na hierarquia **Unidade → Laboratório → Usuário**, permite pedidos vinculados opcionalmente a projetos, controla riscos e perecibilidade dos produtos e mantém histórico das entregas realizadas aos laboratórios.

> ⚠️ O projeto está na fase de consolidação do backend. O frontend ainda não foi iniciado.
>
> Para decisões, progresso e próximas tarefas, consulte [`CONTINUIDADE.md`](CONTINUIDADE.md).

---

## 🧭 Estado atual

| Módulo | Status | Observação |
|---|:---:|---|
| Unidade | ✅ Concluído | CRUD implementado e revisado |
| Laboratório | ✅ Concluído | CRUD implementado e revisado |
| Usuário e Perfil | ✅ Concluído | Senha com BCrypt; autenticação ainda pendente |
| Estagiário | ✅ Concluído | Herança de `Usuario` com estratégia `JOINED` |
| Produto | ✅ Concluído | Catálogo central com risco e perecibilidade |
| EstoqueCentral | ⚠️ Em ajuste estrutural | CRUD atual existe, mas precisa ser vinculado à Unidade |
| Projeto | ✅ Concluído | Vínculo opcional no Pedido |
| ItemPedido e Pedido | ✅ Concluído | Aprovação, rejeição, entrega e cancelamento |
| HistoricoLaboratorio | ✅ Implementado | Histórico de materiais entregues ao laboratório |
| Regras e validações globais | 🔎 Em andamento | Etapa atual |
| Spring Security + JWT | ⏳ Pendente | Após consolidação das regras |
| PostgreSQL definitivo | ⏳ Pendente | H2 permanece no desenvolvimento |
| Frontend Vue.js | ⬜ Não iniciado | Será iniciado após validação final do backend |

### Ordem oficial das próximas etapas

1. Corrigir decisões estruturais contraditórias.
2. Consolidar regras de negócio.
3. Padronizar exceções e respostas HTTP.
4. Testar fluxos completos e falhas.
5. Implementar autenticação e autorização.
6. Migrar definitivamente para PostgreSQL.
7. Iniciar o frontend.

---

## 🏛️ Hierarquia do sistema

```text
UNIDADE (Instituição/Tenant)
│
├── ESTOQUE CENTRAL DA UNIDADE
│   ├── Produto A — saldo e quantidade mínima
│   ├── Produto B — saldo e quantidade mínima
│   └── Produto C — saldo e quantidade mínima
│
└── LABORATÓRIOS
    ├── USUÁRIOS
    ├── PROJETOS
    └── PEDIDOS
        └── ITENS DO PEDIDO
```

### Decisão oficial sobre o estoque

Cada **Unidade possui seu próprio estoque central**.

A entidade `EstoqueCentral` representa o saldo de **um Produto dentro de uma Unidade específica**. Portanto, o mesmo produto pode existir no estoque de várias unidades, mas não pode aparecer duas vezes no estoque da mesma unidade.

```text
Produto: Álcool 70%
├── Unidade A — 50 frascos
├── Unidade B — 25 frascos
└── Unidade C — 80 frascos
```

A identificação lógica de um registro de estoque é:

```text
Unidade + Produto
```

No banco, essa combinação deverá possuir restrição única:

```text
UNIQUE (unidade_id, produto_id)
```

> O código atual ainda representa `EstoqueCentral` como uma relação global `OneToOne` com `Produto`. Essa implementação será migrada para o modelo por Unidade.

---

## ⚙️ Fluxo de estoque

### Entrada

A entrada soma uma quantidade ao saldo atual:

```text
Saldo atual: 20
Entrada: 10
Novo saldo: 30
```

```text
quantidadeAtual = quantidadeAtual + quantidadeEntrada
```

### Saída

A saída subtrai uma quantidade do saldo atual:

```text
Saldo atual: 30
Saída: 8
Novo saldo: 22
```

A operação deve ser recusada quando a quantidade solicitada for maior que a disponível. O estoque nunca pode ficar negativo.

### Estoque mínimo

Cada registro possui uma `quantidadeMinima` para indicar necessidade de reposição.

```text
quantidadeAtual <= quantidadeMinima
```

Quando essa condição for verdadeira, o item deve aparecer nas consultas de estoque baixo da respectiva Unidade.

### Pedido

O laboratório do pedido define qual estoque deve ser utilizado:

```text
Pedido
→ Laboratório
→ Unidade do laboratório
→ Registro de EstoqueCentral da Unidade + Produto
```

Um pedido de uma Unidade nunca pode consumir o estoque de outra Unidade.

---

## 📦 Produto, estoque e histórico

### Produto

`Produto` é um **catálogo central**. Ele descreve o material e não possui saldo próprio nem pertence diretamente a uma Unidade ou Laboratório.

Exemplos de informações do produto:

- nome e descrição;
- código de referência;
- unidade de medida e armazenamento;
- localização física;
- nível e tipo de risco;
- perecibilidade e validade;
- condições de armazenamento.

### EstoqueCentral

`EstoqueCentral` representa o saldo real de um produto em uma Unidade. É o único ponto do sistema que controla entrada e saída.

Campos conceituais:

```text
id
unidade
produto
quantidadeAtual
quantidadeMinima
ativo
```

### HistoricoLaboratorio

`HistoricoLaboratorio` não é um segundo estoque. Ele apenas registra que determinado laboratório recebeu uma quantidade de um produto por meio de um pedido.

```text
Laboratório X recebeu 4 unidades do Produto Y
na data Z, por meio do Pedido N.
```

Ele não possui entrada, saída ou saldo disponível.

---

## 🧾 Fluxo de pedido

1. Um usuário cria um Pedido com pelo menos um ItemPedido.
2. O Pedido pertence ao laboratório do usuário.
3. Um Projeto pode ser informado opcionalmente, desde que pertença ao mesmo laboratório.
4. O pedido nasce como `PENDENTE`.
5. Um gestor ou administrador aprova ou rejeita.
6. Na aprovação, a quantidade aprovada pode ser menor que a solicitada.
7. O sistema localiza o estoque pela combinação **Unidade do laboratório + Produto**.
8. A aprovação reduz o saldo do EstoqueCentral da Unidade.
9. A entrega cria registros no HistoricoLaboratorio.
10. O cancelamento de um pedido aprovado devolve as quantidades ao mesmo estoque utilizado na aprovação.

Fluxo de status:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

---

## 🧩 Padrões de arquitetura

```text
Controller → DTO → Service → Repository → Entity → Banco
```

- DTOs são usados entre Controller e Service.
- Repository trabalha apenas com Entity.
- Conversões entre DTO e Entity ficam no Service ou em construtores do DTO.
- DTOs expõem IDs de relacionamentos, evitando objetos bidirecionais completos.
- Injeção de dependência via construtor com `@RequiredArgsConstructor`.
- Métodos de escrita usam `@Transactional`.
- Métodos de leitura usam `@Transactional(readOnly = true)`.
- Bean Validation trata formato e obrigatoriedade.
- Regras que dependem do banco ou de outras entidades ficam no Service.
- Exceções são tratadas globalmente com `@RestControllerAdvice`.

---

## 📊 Entidades principais

| Entidade | Papel |
|---|---|
| `Unidade` | Instituição/tenant; possui laboratórios e seu estoque central |
| `Laboratorio` | Pertence a uma Unidade e possui usuários e projetos |
| `Usuario` | Usuário do sistema com perfil de acesso |
| `Estagiario` | Especialização de Usuario para informações de estágio |
| `Produto` | Catálogo central de materiais |
| `EstoqueCentral` | Saldo de um Produto dentro de uma Unidade |
| `Projeto` | Contexto opcional associado a um Pedido |
| `Pedido` | Solicitação de materiais de um laboratório |
| `ItemPedido` | Produto e quantidades solicitada/aprovada |
| `HistoricoLaboratorio` | Registro de materiais entregues ao laboratório |

> `Projeto` não agrupa nem controla obrigatoriamente os pedidos. Ele é apenas um vínculo opcional que identifica o contexto do pedido.

---

## 📏 Regras principais

- Cada Unidade possui seu próprio estoque central.
- Cada combinação `Unidade + Produto` possui no máximo um registro de estoque.
- O mesmo Produto pode existir no estoque de várias Unidades.
- Estoque nunca fica negativo.
- Entrada soma ao saldo existente.
- Saída subtrai do saldo existente.
- Estoque baixo ocorre quando `quantidadeAtual <= quantidadeMinima`.
- Pedido utiliza somente o estoque da Unidade do laboratório.
- Todo pedido nasce como `PENDENTE`.
- Pedido deve possuir pelo menos um item.
- Quantidade aprovada não pode superar a solicitada.
- Aprovação reduz o estoque da Unidade.
- Cancelamento de pedido aprovado devolve o estoque.
- Entrega cria histórico para o laboratório.
- Produto perecível exige informações de validade conforme as regras do domínio.
- Produto de risco alto exige descrição adequada do risco.
- Exclusões que quebrariam integridade referencial devem ser bloqueadas.

---

## 🚀 Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 17+, Spring Boot, Spring Data JPA |
| Banco atual de desenvolvimento | H2 |
| Banco definitivo planejado | PostgreSQL 14+ |
| API | REST |
| Segurança planejada | Spring Security + JWT |
| Frontend planejado | Vue.js 3 |
| Utilitários | Lombok e Bean Validation |

---

## 📁 Estrutura principal

```text
Sistema-SGL/
├── backend/sgl-backend/
│   ├── src/main/java/com/sgl/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── config/
│   │   └── exception/
│   └── pom.xml
├── docs/
│   ├── codigos-referencia-pedidos.md
│   └── diagramas/
├── CONTINUIDADE.md
└── README.md
```

---

## 🔧 Execução do backend

```bash
cd backend/sgl-backend
mvn clean install
mvn spring-boot:run
```

A aplicação é executada, por padrão, em:

```text
http://localhost:8080
```

---

## 🗺️ Roadmap

- [x] CRUDs básicos.
- [x] Fluxo de pedidos.
- [x] Histórico de entregas por laboratório.
- [x] Definir oficialmente o estoque central por Unidade.
- [ ] Migrar o código de EstoqueCentral para `Unidade + Produto`.
- [ ] Consolidar regras de negócio.
- [ ] Padronizar exceções e respostas HTTP.
- [ ] Executar testes completos de sucesso e falha.
- [ ] Implementar autenticação e autorização.
- [ ] Migrar definitivamente para PostgreSQL.
- [ ] Validar o backend ponta a ponta.
- [ ] Iniciar o frontend Vue.js.
- [ ] Adicionar Swagger/OpenAPI.
- [ ] Implementar documentos, relatórios, exportações e notificações.

---

## 📚 Documentação complementar

- [`CONTINUIDADE.md`](CONTINUIDADE.md) — decisões, estado atual e próximos passos.
- [`docs/codigos-referencia-pedidos.md`](docs/codigos-referencia-pedidos.md) — referência do fluxo de pedidos.
- `docs/diagramas/` — diagramas do sistema.

## 📝 Licença

A definir.

## 👤 Responsável

Gabriel Salermo
