# Documentação — SGL Backend

**Atualizado em:** 04/09/2026

Este diretório reúne documentação vigente, decisões de domínio, material auxiliar de testes e registros históricos. O objetivo deste índice é impedir que um roteiro antigo seja interpretado como estado atual do produto.

---

## Ordem de leitura para retomada

```text
1. ../CONTINUIDADE.md
2. PLANO_PRE_PRODUCAO.md
3. ../README.md
4. DOSSIE_PROJETO_SGL.md
5. Swagger/OpenAPI em execução
6. documento específico da área em trabalho
```

---

## Fonte de verdade

Em caso de conflito:

```text
código da main
→ Swagger/OpenAPI para contrato HTTP
→ ../CONTINUIDADE.md
→ PLANO_PRE_PRODUCAO.md durante o bloco atual
→ DOSSIE_PROJETO_SGL.md
→ decisões/documentos específicos
→ exemplos, roteiros e snapshots históricos
```

A data de um arquivo histórico não prevalece sobre uma implementação mais recente.

---

## Estado do projeto

```text
Primeiro protótipo funcional                 ✅ aprovado
Pré-produção pós-aprovação                   🔧 em andamento
Limpeza/revisão documental                   ✅ concluída
Planejamento de pré-produção                 ✅ consolidado
Etapa 1 — refinamento visual global          ⏭ próxima implementação
Matriz formal de permissões                  ⏳ após a pré-produção atual
Congelamento/homologação final               ⏳ posterior
Autenticação/autorização definitiva          ⏳ posterior
Integração corporativa                       ⏳ posterior
```

O roadmap formal não foi cancelado. Ele começa depois do bloco atual de ajustes de pré-produção.

---

## Documentos vigentes

| Documento | Papel | Estado |
|---|---|---|
| `../CONTINUIDADE.md` | checkpoint técnico e fase atual | **ATUAL — 04/09** |
| `PLANO_PRE_PRODUCAO.md` | sequência canônica das etapas atuais, dependências e regras de execução | **ATUAL — 04/09** |
| `../README.md` | visão rápida do backend e execução | **ATUAL — 04/09** |
| `DOSSIE_PROJETO_SGL.md` | visão consolidada para handoff humano/IA | **ATUAL — 04/09** |
| `MODULO_RESIDUOS.md` | domínio e fluxo de Resíduos | **VIGENTE** |
| `RELATORIOS.md` | cobertura de relatórios | **VIGENTE** |
| `EXPORTACAO_RELATORIOS.md` | regras de PDF/XLSX | **VIGENTE** |
| `PENDENCIAS_POS_PROTOTIPO.md` | refactors e pendências posteriores | **REFERÊNCIA VIGENTE** |
| `FLUXO_DO_SISTEMA.md` | fluxo operacional de domínio | **REFERÊNCIA**; conferir `main` quando houver detalhe de implementação |
| `GUIA_ESTRUTURAL.md` | organização arquitetural | **REFERÊNCIA** |

---

## Plano atual de pré-produção

A sequência aprovada está detalhada em `PLANO_PRE_PRODUCAO.md`:

```text
1. padronização e refinamento visual global
2. Dark Mode definitivo
3. refinamentos do fluxo atual de Resíduos
4. expansão de Resíduos: locais + modelos pré-cadastrados
5. Projetos + vínculos históricos de Estagiários
6. relatórios de Projetos/Laboratórios
7. normalização de unidades + Soluções + integração com Pedidos
8. Manual do Usuário + avaliação opcional de delete lógico
```

Regra deste bloco: alterações funcionais de backend serão implementadas manualmente pelo responsável do projeto; IA pode analisar, orientar a implementação e revisar o resultado.

---

## Decisões atuais que precisam ser preservadas

```text
Long interno + UUID público
Produto != Resíduo
perecível → FEFO
não perecível → FIFO
aprovação baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura os lotes utilizados
Unidade não possui CRUD manual normal no frontend
usuário será sincronizado pela futura identidade corporativa
migrations Flyway aplicadas são imutáveis
```

### Isolamento por Unidade

Desde 04/09 a `main` possui isolamento operacional multitenant por Unidade:

```text
frontend
→ X-SGL-Unidade-Id
→ TenantRequestFilter / TenantContext
→ services e repositories filtrados por Unidade
```

Isso valida separação funcional em desenvolvimento, mas ainda não substitui a futura autenticação/autorização que deverá derivar a Unidade de uma identidade confiável.

---

## Contratos e material auxiliar

Os arquivos abaixo continuam úteis, mas não devem ser usados como fonte superior ao Swagger/OpenAPI ou à `main`:

| Documento | Uso correto |
|---|---|
| `ENDPOINTS_INTERNOS.md` | inventário auxiliar de endpoints |
| `JSON_EXEMPLOS.md` | exemplos de payload; conferir Swagger antes de copiar |
| `REQUISICOES_POSTMAN_LOTES.md` | roteiro de testes de lotes |
| `CODIGOS_REFERENCIA_TESTES.md` | roteiro de testes de códigos/referências |
| `testes.md` | histórico e cenários de validação |
| `SGL_Relacao_Completa_Classes.pdf` | snapshot documental de classes |

---

## Documentos históricos / snapshots

Esses arquivos são mantidos para rastrear decisões ou demonstrações antigas. Eles **não representam planejamento vigente**:

| Documento | Interpretação |
|---|---|
| `API_AUDITORIA_PRE_SWAGGER.md` | auditoria anterior ao Swagger consolidado |
| `DEMO_APRESENTACAO_EMBRAPA.md` | roteiro de demonstração/apresentação |
| exemplos antigos dentro de arquivos de teste | usar apenas como referência histórica |

Se algum documento histórico disser que Administração, Resíduos, Dashboard, isolamento por Unidade ou outro bloco já integrado “ainda será feito”, essa afirmação deve ser ignorada.

---

## Fase atual e roadmap

### Agora

```text
limpeza e atualização documental       ✅
→ planejamento de pré-produção         ✅
→ Etapa 1: refinamento visual          ⏭ atual
→ Etapas 2 a 8                         ⏳ sequenciais
→ estabilização do bloco
```

### Depois

```text
matriz de permissões
→ congelamento funcional
→ homologação integrada final
→ autenticação/autorização/auditoria
→ integração corporativa
→ demais etapas formais de produção
```

Não classificar a matriz de permissões como “próximo passo imediato” enquanto a fase atual de pré-produção estiver aberta.

---

## Regra para outra IA

Antes de alterar o sistema:

```text
1. ler ../CONTINUIDADE.md
2. ler PLANO_PRE_PRODUCAO.md durante a fase atual
3. conferir a main atual
4. conferir Swagger/OpenAPI quando houver contrato HTTP
5. confirmar o documento específico da área
6. distinguir requisito atual de registro histórico
```

Não reconstruir módulos aprovados apenas porque um documento antigo descreve uma fase anterior.
