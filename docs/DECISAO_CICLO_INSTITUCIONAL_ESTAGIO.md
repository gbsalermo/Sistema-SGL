# Decisão estrutural — Ciclo institucional do Estagiário

**Data:** 02/10/2026  
**Status:** decisão arquitetural implementada e validada no bloco 6.4 em 02/10/2026  
**Branch:** `collab/etapa-6-estagiarios-vinculos`

## 1. Fonte de verdade

O ciclo institucional do Estagiário não é decidido pelo SGL.

A fonte autoritativa é o ambiente institucional/corporativo para, no mínimo:

- identidade do Usuario;
- matrícula/CPF ou identificador institucional equivalente;
- estado institucional ativo/inativo do Usuario;
- existência e período do vínculo de estágio;
- situação institucional do vínculo quando disponibilizada pela integração.

O SGL consome e espelha essas informações e aplica consequências operacionais internas.

Senha é credencial de acesso e não identifica uma nova pessoa nem um novo vínculo.

## 2. Identidade, papel e ocorrência

```text
Usuario
→ identidade institucional estável
→ matrícula/CPF permanece

Estagiario
→ papel persistente no domínio

VinculoEstagio
→ cada ocorrência institucional de bolsa/estágio
```

Trocar senha não cria novo `Usuario`, novo `Estagiario` ou novo `VinculoEstagio`.

## 3. Prorrogação x nova bolsa

### Prorrogação da mesma bolsa/vínculo

```text
mesmo VinculoEstagio
dataFimPrevista anterior
→ nova dataFimPrevista maior
→ situação institucional PRORROGADO quando aplicável
→ histórico de sincronização
```

A prorrogação não cria novo vínculo e não expande automaticamente participações em Atividades.

### Nova bolsa depois do término

```text
mesmo Usuario
→ mesmo Estagiario
→ VinculoEstagio anterior FINALIZADO
→ novo VinculoEstagio
```

O novo vínculo pode ter período, tipo de bolsa, orientador e demais dados institucionais diferentes.

## 4. Atividade não identifica o vínculo institucional

A decisão anterior de que todo novo `VinculoEstagio` precisava nascer com a primeira Atividade foi válida para o fluxo local/manual construído no 6.2, mas não pode ser regra absoluta depois da integração institucional.

Para vínculos recebidos do ambiente:

```text
novo VinculoEstagio institucional
→ pode existir sem participação em Atividade
→ ainda NÃO é operacionalmente ativo no SGL
→ Gestão associa ao menos uma Atividade
→ passa a poder operar como Estagiário
```

A regra de atividade obrigatória continua valendo para o estado operacional:

```text
Estagiário operacionalmente ativo
=
Usuario.ativo
AND vínculo não FINALIZADO
AND >= 1 participação aberta
```

O endpoint local de criação com primeira Atividade permanece como fluxo de compatibilidade/DEV até a integração institucional substituí-lo.

## 5. Encerramento

Não haverá fluxo cotidiano:

```text
Gestor
→ "Finalizar estágio"
```

como fonte de verdade do vínculo.

Quando o ambiente informar finalização:

```text
VinculoEstagio
→ situacao = FINALIZADO
→ dataFimEfetiva = valor institucional
→ SGL encerra participações ainda abertas
→ Usuario NÃO é desativado pelo SGL
```

Se a data institucional for incompatível com algum histórico já persistido, a sincronização deve falhar/ser sinalizada para reconciliação. O SGL não deve reescrever silenciosamente o passado.

## 6. Usuario inativo

Se o ambiente informar `Usuario.ativo = false`:

- o Usuario perde acesso operacional;
- o Estagiário deixa de ser operacionalmente ativo;
- o SGL não deve concluir sozinho que o vínculo foi FINALIZADO, a menos que o ambiente também informe o encerramento do vínculo.

Estado do Usuario e situação do vínculo continuam conceitos distintos.

## 7. Histórico e auditoria

O 6.4 deve registrar alterações institucionais recebidas pelo SGL, especialmente:

- prorrogação do período;
- finalização;
- criação de nova ocorrência de vínculo;
- mudanças de situação relevantes ao ciclo institucional.

O histórico deve registrar valores anteriores/novos, origem e data/hora.

Não é necessário exigir justificativa de Gestor para uma alteração que veio do ambiente institucional.

## 8. Identificador da ocorrência institucional

CPF/matrícula identifica a pessoa, não uma bolsa específica.

Quando o ambiente disponibilizar um identificador próprio do vínculo/bolsa/contrato, ele deve ser usado como referência institucional estável para sincronização.

Até que o contrato externo confirme o escopo desse identificador, o SGL **não assume unicidade global** de `referenciaInstitucional`. O campo permanece indexado para consulta, mas sem restrição `UNIQUE`.

Implementação Flyway: V30 introduziu o campo/histórico e V31 remove a restrição de unicidade global inicialmente assumida, preservando a imutabilidade de migrations já publicadas.

A idempotência de eventos é tratada separadamente por `origem + referenciaEvento` quando a fonte fornecer uma referência de evento. Eventos sem referência não devem ser artificialmente identificados.

Até que esse contrato externo esteja disponível, qualquer correspondência por vínculo aberto/período deve ser tratada como compatibilidade temporária de DEV, não como identificação definitiva de produção.

## 9. Fronteira de responsabilidade

```text
Ambiente institucional
├── identidade
├── Usuario ativo/inativo
├── ocorrência do vínculo
├── período institucional
└── situação institucional

SGL
├── participações em Atividades
├── Culturas
├── treinamento de segurança
├── Pedidos
├── Resíduos
└── demais operações laboratoriais
```

## 10. Bloco 6.4 revisado

```text
6.4.1 contrato e histórico da sincronização institucional ✅
6.4.2 sincronização do vínculo existente ✅ concluído e validado em 02/10/2026
      → prorrogação ✅
      → finalização ✅
      → idempotência por origem + referência do evento ✅
      → encerramento coerente das participações abertas ✅
6.4.3 nova bolsa / novo VinculoEstagio ✅ validado
      → mesma identidade
      → criação sem Atividade inicial
      → evento de CRIACAO auditado e idempotente por referência quando disponível
      → ativação operacional somente após Atividade
6.4.4 testes, validação e documentação ✅ concluído
```

Correção administrativa manual do ciclo institucional, caso necessária no futuro, será fluxo excepcional e auditável; não faz parte do workflow normal do 6.4.
