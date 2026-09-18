# Continuidade SGL — Etapa 4

**Atualizado em:** 18/09/2026  
**Branch:** `feat/etapa-4-residuos`  
**Estado:** implementação da Etapa 4 concluída ✅  
**Pendente:** validação integrada/manual antes do merge  
**Roteiro de validação:** `docs/VALIDACAO_ETAPA_4.md`  
**Próximo bloco após validação:** Etapa 5 — Projetos e Atividades

Este arquivo é o handoff final da Etapa 4. O plano canônico continua em `docs/PLANO_PRE_PRODUCAO.md`.

---

## 1. Escopo entregue

```text
4.1 Locais de armazenamento cadastráveis          ✅
4.2 Modelos de Resíduos pré-cadastrados            ✅
4.3 Uso de modelo ou preenchimento manual          ✅
4.4 Correções administrativas do ciclo             ✅
```

A branch ainda não deve ser tratada como homologada até a execução do roteiro de testes.

---

## 2. Etapa 4.1 — locais de armazenamento

Arquitetura preservada:

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico
```

A Gestão pode selecionar local cadastrado ou informar manualmente. Na confirmação física pode manter ou corrigir o local, preservando histórico.

Migration:

```text
V16__create_residue_storage_locations.sql
```

---

## 3. Etapa 4.2 — ModeloResiduo

Contrato detalhado: `docs/ETAPA_4_2_MODELO_RESIDUO.md`.

Regra principal:

```text
ModeloResiduo = definição reutilizável/editável
Residuo       = ocorrência real independente
```

Implementado:

- `V17__create_residue_models.sql`;
- `ModeloResiduo`;
- `ComponenteModeloResiduo`;
- repository;
- DTOs;
- Service;
- Controller;
- CRUD por Unidade;
- nome único por Unidade com validação case-insensitive;
- Classes ativas da mesma Unidade;
- Produto opcional e disponível para a Unidade;
- validação de tratamento;
- validação de segurança/`OUTRO`;
- inativação lógica;
- tela administrativa de modelos;
- testes backend das regras centrais.

Endpoints:

```text
POST   /api/v1/modelos-residuo
GET    /api/v1/modelos-residuo
GET    /api/v1/modelos-residuo/ativos
GET    /api/v1/modelos-residuo/{id}
PUT    /api/v1/modelos-residuo/{id}
DELETE /api/v1/modelos-residuo/{id}
```

Não existe FK `Residuo -> ModeloResiduo`.

---

## 4. Etapa 4.3 — uso pelo Solicitante

Em `/residuos/novo` existe escolha entre:

```text
Modelo pré-cadastrado
ou
Preenchimento manual
```

Ao selecionar um modelo, o frontend sugere/preenche:

- descrição;
- processo de origem;
- estado físico;
- tratamento;
- recipiente;
- unidade de medida;
- nível/riscos;
- Classes;
- segurança/EPI;
- componentes.

Continuam específicos da ocorrência, entre outros:

- quantidade;
- usuário;
- laboratório;
- projeto;
- observação do gerador.

O usuário revisa os dados antes de enviar. O payload continua usando o contrato normal de criação de `Residuo`, garantindo independência histórica.

---

## 5. Etapa 4.4 — administração do ciclo

Novo status:

```text
CANCELADO
```

Novo endpoint:

```text
PUT /api/v1/residuos/{id}/administrar
```

Ações:

```text
CANCELAR
RETORNAR_ETAPA
```

Requer:

- usuário ativo;
- perfil `ADMINISTRADOR`;
- mesma Unidade pelo mecanismo de tenant atual;
- justificativa obrigatória.

Retorno é sempre de uma etapa:

```text
EM_ANALISE                   → INFORMADO
LIBERADO_PARA_ARMAZENAMENTO → EM_ANALISE
ARMAZENADO_TEMPORARIAMENTE  → LIBERADO_PARA_ARMAZENAMENTO
DESPACHADO                   → ARMAZENADO_TEMPORARIAMENTE
```

Regras de cancelamento:

- `CANCELADO` é terminal para retorno;
- `DESPACHADO` não cancela diretamente;
- para cancelar um despachado, primeiro retornar para `ARMAZENADO_TEMPORARIAMENTE`;
- cancelamento não apaga a ocorrência.

Histórico:

```text
RESIDUO_CANCELADO_ADMINISTRATIVAMENTE
RETORNO_ADMINISTRATIVO_DE_ETAPA
```

Ator, data, status e justificativa permanecem auditáveis.

Relatórios, dashboards e filtros foram ajustados para reconhecer `CANCELADO`.

---

## 6. Fluxo após a Etapa 4

Fluxo normal:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Caminho administrativo adicional:

```text
qualquer etapa elegível
→ CANCELADO

ou

etapa atual
→ etapa imediatamente anterior
```

Código SGL/QR e snapshots históricos continuam pertencendo à ocorrência real.

---

## 7. Pontos que não foram misturados nesta etapa

Continuam fora da Etapa 4:

- achados de segurança que estão sendo tratados pelo supervisor;
- autenticação/autorização definitiva;
- refactor amplo de `Residuo`;
- normalização estrutural de unidades/concentrações;
- Zebra/ZPL e padrão final de rótulos;
- decisão geral de delete lógico;
- testes frontend formais da Etapa 12.

A concentração/quantidade do componente continua como texto no domínio atual para manter compatibilidade com `ComponenteResiduo`. Uma eventual estrutura `BigDecimal + tipo + unidade` deve ser tratada de forma conjunta no refinamento de unidades, não isoladamente no modelo.

---

## 8. Validação antes do merge

Executar:

```text
docs/VALIDACAO_ETAPA_4.md
```

Pré-check esperado:

```bash
# backend
cd backend/sgl-backend
./mvnw test
./mvnw spring-boot:run

# frontend
npm install
npm run build
npm run dev
```

Não há workflow GitHub Actions configurado nestes repositórios, portanto a execução local é parte obrigatória da homologação.

---

## 9. Próximo passo após validação

Se a bateria da Etapa 4 passar:

```text
feat/etapa-4-residuos
→ revisão final
→ merge em main
→ Etapa 5 — Projetos e Atividades
```

Antes da Etapa 5, respeitar o portão de confirmação já descrito em `docs/PLANO_PRE_PRODUCAO.md`: Código SEG, Atividade, SCI e situações de execução.
