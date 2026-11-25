# Modelagem de modalidades de bolsa por Unidade — validação colaborativa

> **Estado:** implementação na branch `collab/modalidades-bolsa-unidade-2026-11`. **Não homologada.** Sem merge para `release` nem deploy.

## Motivação

Permitir à Gestão cadastrar modalidades específicas de bolsas reutilizáveis dentro de uma Unidade, sem adicionar novos valores ao enum `TipoBolsa` a cada novo programa ou convênio.

A classificação `TipoBolsa` permanece em todas as rotas e vínculos já existentes. Cada modalidade cadastrada informa um **tipo base** dentre os valores legados; a associação da modalidade ao vínculo é **opcional**. Ex.: modalidade "Bolsa FAPESB" classificada na base `BOLSA_INSTITUCIONAL`.

## Persistência

- Flyway **V49**: nova tabela `modalidades_bolsa` com Unidade, nome, tipo base, ativo e UUID público; nome único sem distinção de caixa dentro da mesma Unidade.
- `vinculos_estagio.modalidade_bolsa_id`: FK **nullable** ao catálogo.
- `vinculos_estagio.modalidade_bolsa_nome`: **snapshot histórico** do nome escolhido no momento da associação. Renomear/inativar o catálogo não altera a descrição de bolsas anteriores.
- Vínculos existentes permanecem com esses campos `NULL`, preservando o contrato anterior.
- Exclusão é lógica: a modalidade deixa de ser elegível para novas seleções, sem apagar vínculos já associados.

## API

- `GET /api/v1/modalidades-bolsa`: todas as opções da Unidade (inclusive inativas).
- `GET /api/v1/modalidades-bolsa/ativas`: somente opções elegíveis a novas seleções.
- `POST /api/v1/modalidades-bolsa`: nova opção com `nome`, `tipoBase` e `ativo` opcional.
- `PUT /api/v1/modalidades-bolsa/{id}`: renomear/ativar/inativar; **tipo base imutável**.
- `DELETE /api/v1/modalidades-bolsa/{id}`: inativação lógica.
- Escritas são restringidas a perfis de Gestão pela cadeia de segurança existente; o tenant vem da sessão autenticada, não da requisição.

Campos opcionais adicionais nas rotas existentes de criação inicial do estágio, criação de vínculo e registro de nova bolsa: `modalidadeBolsaId` (UUID). O servidor verifica Unidade, situação ativa e compatibilidade com `tipoBolsa`. No DTO de saída de vínculo, `modalidadeBolsaId` e `modalidadeBolsaNome` são informativos.

Exemplo de solicitação de uma **nova bolsa** (a aprovação/encerramento de períodos segue o serviço existente):

```json
{
  "tipoBolsa": "BOLSA_INSTITUCIONAL",
  "modalidadeBolsaId": "UUID-DA-MODALIDADE",
  "especificacaoBolsa": "Edital fictício 2026/01",
  "dataInicio": "2026-11-25",
  "dataFimPrevista": "2027-11-24"
}
```

## Compatibilidade operacional

- **Não** alteramos `TipoBolsa`, a tabela `estagiarios` legada, nem a semântica de bolsa vigente.
- **Não** modificamos regras de encerramento, prorrogação, nova ocorrência, divisão de participações e sincronização de vínculos com a instituição.
- Ao sincronizar um **tipo base diferente** vindo da fonte institucional, a seleção local específica é descartada para evitar inconsistência; se o tipo base não muda, a identificação local permanece.
- Editar vínculo **não troca modalidade**. Para alterar a bolsa, usa-se o fluxo existente **Registrar nova bolsa**.
- Relatórios legados continuam agrupando pelos tipos base. A exibição do nome específico foi acrescentada à tela de Gestão de Estagiários; ampliar relatórios será uma etapa separada, após homologação.
- Esta branch contém também a correção anterior da massa DEMO: seis estagiários com vínculos/participações ativos e dois encerrados.

## Matriz de homologação antes do merge

- [ ] `./mvnw clean verify` no backend e `npm run build && npm run test` no frontend.
- [ ] PostgreSQL limpo: aplicar Flyway V49 e validar `ddl-auto=validate` (também testar banco com histórico pré-existente).
- [ ] CRUD por Unidade: duplicidades com diferenças de caixa, inativar, reativar, renomear e rejeitar acesso cruzado entre Unidades.
- [ ] Criar estágio/vínculo **sem** modalidade personalizada (regressão).
- [ ] Criar estágio/vínculo **com** modalidade ativa e compatível.
- [ ] Rejeitar modalidade inativa, de outra Unidade ou tipo base incompatível.
- [ ] Registrar nova bolsa com modalidade: situação final da bolsa anterior, data de corte, participação e culturas copiadas/divididas.
- [ ] Prorrogar bolsa customizada sem criar nova ocorrência.
- [ ] Renomear e inativar modalidade e conferir histórico de vínculo (nome original preservado).
- [ ] Sincronizar tipo institucional diferente e conferir precedência institucional.
- [ ] Repetir testes na DEMO e na interface mobile sem afetar a operação real.
