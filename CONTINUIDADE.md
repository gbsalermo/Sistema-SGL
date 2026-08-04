# 📦 Continuidade do Projeto SGL

## 📋 Estado do projeto

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Início:** 13/07/2026  
**Última atualização:** 04/08/2026  
**Fase atual:** consolidação estrutural e regras de negócio do backend

Este arquivo é a referência principal para continuar o desenvolvimento. O `README.md` apresenta a visão pública e resumida; este documento registra decisões, pendências técnicas e a ordem de execução.

---

## 🎯 Objetivo

O SGL automatiza e centraliza o controle de materiais em laboratórios de pesquisa e ensino.

O sistema deve permitir:

- cadastrar unidades, laboratórios, usuários e estagiários;
- manter um catálogo central de produtos;
- controlar separadamente o estoque de cada Unidade;
- registrar entradas e saídas de materiais;
- monitorar estoque mínimo para reposição;
- criar, aprovar, rejeitar, entregar e cancelar pedidos;
- vincular pedidos opcionalmente a projetos;
- manter histórico do que foi entregue a cada laboratório;
- controlar risco, perecibilidade e validade dos produtos;
- futuramente, autenticar usuários, armazenar documentos e oferecer uma interface Vue.js.

---

## 🧭 Ordem oficial de desenvolvimento

1. **Corrigir decisões estruturais contraditórias.**
2. Consolidar regras de negócio.
3. Padronizar exceções e respostas HTTP.
4. Testar fluxos completos e falhas.
5. Implementar autenticação e autorização.
6. Migrar definitivamente para PostgreSQL.
7. Iniciar o frontend.

### Situação do item 1

- [x] Definir oficialmente a arquitetura do EstoqueCentral por Unidade.
- [ ] Migrar o código atual para a nova definição.
- [ ] Revisar diagramas UML e ER após a alteração do código.
- [ ] Conferir enum `Perfil` diretamente no código e padronizar toda a documentação.
- [ ] Revisar os demais trechos antigos que ainda possam descrever o Projeto como agrupador de pedidos.

---

# 1. Decisão estrutural: EstoqueCentral por Unidade

## Problema encontrado

A documentação apresentava duas ideias incompatíveis:

1. cada Unidade possuir seu próprio estoque central;
2. cada Produto possuir um único registro global em `EstoqueCentral`.

O código atual segue a segunda interpretação, usando relação `OneToOne` entre `EstoqueCentral` e `Produto`, com `produto_id` único e sem vínculo com `Unidade`.

Essa implementação não atende corretamente ao modelo multiunidade do SGL, pois faria todas as unidades compartilharem o mesmo saldo de produto.

## Decisão aprovada em 04/08/2026

Cada **Unidade possui seu próprio estoque central**.

Conceitualmente:

```text
Unidade
└── Estoque Central
    ├── Produto A — quantidade atual e mínima
    ├── Produto B — quantidade atual e mínima
    └── Produto C — quantidade atual e mínima
```

No modelo relacional, `EstoqueCentral` representa o saldo de **um Produto dentro de uma Unidade específica**.

Campos conceituais:

```text
EstoqueCentral
- id
- unidade
- produto
- quantidadeAtual
- quantidadeMinima
- ativo
```

A combinação abaixo deve ser única:

```text
Unidade + Produto
```

Restrição esperada no banco:

```sql
UNIQUE (unidade_id, produto_id)
```

### Consequências

- O mesmo Produto pode existir no estoque de várias Unidades.
- Uma Unidade não pode possuir dois registros de estoque para o mesmo Produto.
- Produto continua sendo um catálogo central, sem saldo próprio.
- Pedido consome apenas o estoque da Unidade à qual pertence o laboratório solicitante.
- Cancelamento devolve a quantidade ao mesmo registro de estoque utilizado na aprovação.
- Consultas de estoque baixo devem poder ser filtradas por Unidade.

Exemplo:

```text
Produto: Álcool 70%
├── Unidade A — 50 frascos
├── Unidade B — 25 frascos
└── Unidade C — 80 frascos
```

## Situação da implementação

> ⚠️ **Decisão documentada, mas código ainda não migrado.**

A implementação atual de `EstoqueCentral` ainda usa `@OneToOne` com `Produto` e não possui `Unidade`. A alteração do código ficará sob responsabilidade do desenvolvedor.

---

# 2. Regras de movimentação do estoque

## Entrada

Entrada soma ao saldo atual:

```text
quantidadeAtual = quantidadeAtual + quantidadeEntrada
```

Exemplo:

```text
Saldo atual: 20
Entrada: 10
Novo saldo: 30
```

## Saída

Saída subtrai do saldo atual:

```text
quantidadeAtual = quantidadeAtual - quantidadeSaida
```

A operação deve falhar quando a quantidade solicitada for maior que a quantidade disponível.

```text
quantidadeAtual nunca pode ser negativa
```

## Quantidade mínima

Cada registro de estoque possui uma quantidade mínima para reposição.

Um item está com estoque baixo quando:

```text
quantidadeAtual <= quantidadeMinima
```

A consulta deve considerar a Unidade, evitando misturar alertas de instituições diferentes.

---

# 3. Produto, estoque e histórico

## Produto

`Produto` é um catálogo central. Ele descreve o material, mas não define quanto existe disponível.

O Produto não pertence diretamente a uma Unidade ou Laboratório.

Principais informações:

- nome;
- descrição;
- código de referência;
- unidade de medida;
- unidade de armazenamento;
- localização física;
- risco e tipo de risco;
- perecibilidade e validade;
- condições de armazenamento;
- estado ativo.

## EstoqueCentral

É o saldo real de um Produto dentro de uma Unidade.

É o único módulo responsável por:

- entrada;
- saída;
- quantidade atual;
- quantidade mínima;
- baixa por aprovação de pedido;
- devolução por cancelamento;
- alerta de reposição.

## HistoricoLaboratorio

`HistoricoLaboratorio` não é estoque disponível.

Ele registra somente que um laboratório recebeu determinado produto por meio de um pedido.

```text
Laboratório X recebeu 4 unidades do Produto Y
na data Z, por meio do Pedido N.
```

Não deve possuir operações próprias de entrada, saída ou atualização de saldo.

---

# 4. Fluxo de Pedido

```text
Pedido
→ Laboratório
→ Unidade do laboratório
→ EstoqueCentral localizado por Unidade + Produto
```

Fluxo principal:

1. usuário cria o Pedido;
2. o Pedido possui pelo menos um ItemPedido;
3. o Pedido pertence ao laboratório do usuário;
4. Projeto é opcional e, quando informado, deve pertencer ao mesmo laboratório;
5. status inicial é `PENDENTE`;
6. gestor ou administrador aprova ou rejeita;
7. na aprovação, a quantidade aprovada pode ser menor que a solicitada;
8. o estoque é localizado pela combinação `Unidade + Produto`;
9. aprovação reduz o estoque;
10. entrega cria registros em `HistoricoLaboratorio`;
11. cancelamento de pedido aprovado devolve o estoque.

Fluxo de status:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Regras importantes:

- pedido de uma Unidade não pode consumir estoque de outra;
- quantidade aprovada não pode superar a solicitada;
- estoque nunca pode ficar negativo;
- aprovação deve ser transacional;
- se qualquer item falhar, nenhuma baixa parcial deve permanecer;
- apenas pedido aprovado pode ser entregue;
- pedido entregue ou rejeitado não volta para estado anterior.

---

# 5. Papel do Projeto

`Projeto` é um contexto opcional do Pedido.

Ele não cria um estoque separado, não controla saldo e não é um agrupador obrigatório de pedidos.

```text
Pedido pode possuir projetoId ou projetoId = null
```

Quando informado, o Projeto deve pertencer ao mesmo Laboratório do Pedido.

---

# 6. Padrões arquiteturais

## Camadas

```text
Controller → DTO → Service → Repository → Entity → Banco
```

- Controller recebe e devolve DTOs.
- Service contém conversões e regras de negócio.
- Repository trabalha somente com Entity.
- Entity e Repository não conhecem DTO.
- Relacionamentos são expostos nos DTOs principalmente por IDs.

## Injeção de dependência

Usar construtor com `@RequiredArgsConstructor`.

Evitar injeção em campo com `@Autowired`.

## Transações

- escrita: `@Transactional`;
- leitura: `@Transactional(readOnly = true)`.

Operações compostas, principalmente aprovação, entrega e cancelamento de pedido, devem permanecer atômicas.

## Validações

DTO:

- obrigatoriedade;
- formato;
- tamanho;
- valores mínimos;
- e-mail.

Service:

- existência de relacionamentos;
- unicidade dependente do banco;
- compatibilidade entre Unidade e Laboratório;
- compatibilidade entre Laboratório e Projeto;
- transições de status;
- disponibilidade de estoque;
- entidades ativas.

## Exceções

Existe tratamento global com `@RestControllerAdvice`. A padronização completa será executada no item 3 do roadmap.

---

# 7. Módulos implementados

- [x] Unidade.
- [x] Laboratório.
- [x] Usuário.
- [x] Perfil como enum.
- [x] Senha criptografada com BCrypt.
- [x] Estagiário com herança de Usuario usando `JOINED`.
- [x] Produto.
- [x] EstoqueCentral na arquitetura antiga global por Produto.
- [x] Entrada e saída de estoque.
- [x] Projeto.
- [x] ItemPedido.
- [x] Pedido.
- [x] Aprovação, rejeição, entrega e cancelamento.
- [x] HistoricoLaboratorio.
- [x] DataInitializer com dados de teste.
- [x] Diagramas e referências de pedidos.

---

# 8. Pendências imediatas de código

## Migração do EstoqueCentral

- [ ] adicionar relacionamento de `EstoqueCentral` com `Unidade`;
- [ ] trocar `Produto` de `OneToOne` para `ManyToOne`;
- [ ] remover unicidade isolada de `produto_id`;
- [ ] criar unicidade composta entre `unidade_id` e `produto_id`;
- [ ] adicionar `unidadeId` ao `EstoqueCentralDTO`;
- [ ] carregar e validar a Unidade no Service;
- [ ] impedir cadastro duplicado da combinação Unidade + Produto;
- [ ] alterar consultas de Repository para considerar Unidade;
- [ ] alterar aprovação e cancelamento de Pedido para buscar estoque por Unidade + Produto;
- [ ] adicionar consultas de estoque por Unidade;
- [ ] atualizar DataInitializer;
- [ ] atualizar testes do Postman;
- [ ] atualizar diagramas após o código estar concluído.

---

# 9. Próximas etapas

## Etapa 2 — Consolidar regras de negócio

- validar vínculos entre Unidade, Laboratório, Usuário, Projeto e Pedido;
- impedir uso de entidades inativas;
- revisar regras de risco e perecibilidade;
- revisar todas as transições de status;
- eliminar validações duplicadas ou conflitantes.

## Etapa 3 — Padronizar exceções e respostas

Planejar exceções de domínio, por exemplo:

- recurso não encontrado;
- recurso duplicado;
- entidade em uso;
- estoque insuficiente;
- transição de status inválida;
- regra de negócio violada.

Padronizar respostas `400`, `401`, `403`, `404`, `409` e demais códigos necessários.

## Etapa 4 — Testar fluxos completos e falhas

Testar:

- CRUDs válidos e inválidos;
- entradas e saídas;
- estoque insuficiente;
- duplicidade Unidade + Produto;
- pedido com Unidade incompatível;
- aprovação parcial;
- rollback de aprovação;
- entrega;
- cancelamento e devolução;
- exclusões bloqueadas por integridade.

## Etapa 5 — Autenticação e autorização

- Spring Security;
- JWT;
- login por e-mail e senha;
- autorização por Perfil;
- restrição por Unidade e Laboratório.

## Etapa 6 — PostgreSQL

- configurar perfis de ambiente;
- criar banco definitivo;
- preparar migrations;
- revisar constraints e tipos;
- remover dependência do H2 como ambiente principal.

## Etapa 7 — Frontend

O frontend Vue.js só deve ser iniciado após:

- regras estabilizadas;
- respostas HTTP padronizadas;
- autenticação funcionando;
- PostgreSQL configurado;
- backend validado ponta a ponta.

---

# 10. Funcionalidades futuras

- Swagger/OpenAPI;
- upload e download de documentos;
- relatórios gerenciais;
- exportação PDF e Excel;
- notificações por e-mail;
- dashboard;
- código de barras e QR Code;
- relatórios de risco;
- controle avançado de validade;
- auditoria detalhada de movimentações.

---

## 📂 Referências

- [`README.md`](README.md) — visão geral do projeto.
- [`docs/codigos-referencia-pedidos.md`](docs/codigos-referencia-pedidos.md) — referência do fluxo de pedidos.
- `docs/diagramas/` — diagramas UML, ER e arquitetura.

---

## 📝 Histórico resumido de decisões

| Data | Decisão |
|---|---|
| 13/07/2026 | Início do SGL e definição de Java, Spring Boot, Vue.js e PostgreSQL |
| 17/07/2026 | Adoção de DTOs, validação e exception handler global |
| 17/07/2026 | Substituição de Estudante/Pesquisador por Usuario + Perfil |
| 20/07/2026 | Responsável do Laboratório passou a ser Usuario |
| 21/07/2026 | Separação entre Produto, EstoqueCentral e HistoricoLaboratorio |
| 24/07/2026 | Projeto definido como vínculo opcional no Pedido |
| 31/07/2026 | Revisões dos CRUDs de Unidade, Usuario e Projeto |
| 03/08/2026 | Estagiario migrou para herança JOINED de Usuario |
| 04/08/2026 | EstoqueCentral definido oficialmente por combinação Unidade + Produto |
