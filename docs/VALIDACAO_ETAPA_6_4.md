# Validação — Etapa 6.4 — Ciclo institucional e sincronização

**Data:** 02/10/2026  
**Branch:** `collab/etapa-6-estagiarios-vinculos`  
**Status:** concluída e validada ✅

## Escopo validado

O bloco 6.4 consolidou o ciclo institucional do Estagiário com o ambiente externo como fonte de verdade.

Foi validado que:

- prorrogação da mesma bolsa mantém o mesmo `VinculoEstagio`;
- finalização recebida do ambiente atualiza o vínculo e encerra participações abertas de forma coerente;
- `Usuario` não é desativado pelo encerramento do estágio;
- eventos institucionais com `origem + referenciaEvento` são idempotentes quando a referência existe;
- replay divergente do mesmo evento é bloqueado;
- a mesma referência de evento não pode ser reaproveitada para outro vínculo/Estagiário;
- conflitos temporais com participações e Atividades são bloqueados para reconciliação;
- `referenciaInstitucional` é estável no vínculo, mas não possui unicidade global presumida;
- nova bolsa para a mesma identidade cria novo `VinculoEstagio` após encerramento do anterior;
- o novo vínculo institucional pode nascer sem Atividade inicial;
- vínculo institucional sem participação aberta não torna o Estagiário operacionalmente ativo;
- dois vínculos não finalizados simultâneos para o mesmo Estagiário continuam bloqueados;
- criação de nova ocorrência institucional registra histórico `CRIACAO`;
- reenvio do mesmo evento de criação reaproveita o vínculo existente em vez de duplicá-lo.

## Implementação consolidada

```text
6.4.1 contrato + histórico institucional          ✅
6.4.2 sincronização do vínculo existente          ✅
6.4.3 nova bolsa / novo VinculoEstagio            ✅
6.4.4 testes, validação e documentação            ✅
```

As migrations V30/V31 permanecem imutáveis. A idempotência de eventos é separada da referência institucional do vínculo.

## Validação automatizada

A suíte backend foi executada localmente em 02/10/2026 e concluída sem erros.

A cobertura adicionada no bloco inclui, entre outros cenários:

- prorrogação;
- finalização;
- encerramento de participação aberta;
- idempotência;
- replay divergente;
- tentativa de reativação de vínculo finalizado;
- conflito com fim da Atividade;
- referência institucional incompatível;
- criação de nova bolsa;
- criação sem participação inicial;
- bloqueio de vínculo simultâneo;
- replay do evento de criação sem duplicidade.

## Próximo passo

```text
6.5 — frontend integrado
```

O frontend deve refletir o estado institucional vindo do backend, sem criar workflow cotidiano próprio para prorrogação/finalização, e deve representar o Estagiário por suas participações/contextos de Atividade em vez de um único Laboratório definitivo.
