# Documentação — SGL Backend

**Atualizado em:** 31/08/2026

Este índice existe para impedir que documentos históricos sejam interpretados como estado atual do projeto.

## Ordem de leitura para retomada

```text
1. ../CONTINUIDADE.md
2. DOSSIE_PROJETO_SGL.md
3. Swagger/OpenAPI em execução
4. documento específico do módulo em que se vai trabalhar
```

## Fonte de verdade

Em caso de conflito:

```text
código da main
→ Swagger/OpenAPI para contrato HTTP
→ CONTINUIDADE.md
→ DOSSIE_PROJETO_SGL.md
→ documentos específicos
→ exemplos/roteiros históricos
```

## Documentos atuais

| Documento | Papel | Estado |
|---|---|---|
| `../CONTINUIDADE.md` | checkpoint técnico, decisões e próximo passo | **ATUAL** |
| `DOSSIE_PROJETO_SGL.md` | visão completa para handoff humano/IA | **ATUAL** |
| `RELATORIOS.md` | decisões e cobertura da central de relatórios | **ATUAL** |
| `EXPORTACAO_RELATORIOS.md` | PDF/XLSX e regras de exportação | **ATUAL** |
| `PENDENCIAS_POS_PROTOTIPO.md` | refactors que não entram no fechamento funcional | **ATUAL** |
| `FLUXO_DO_SISTEMA.md` | regras e fluxo de domínio | referência; validar mudanças recentes no dossiê/Swagger |
| `GUIA_ESTRUTURAL.md` | organização arquitetural do backend | referência |
| `diagrama-uml-completo.puml` | visão UML | referência; pode não refletir módulos ainda não integrados |

## Contratos, exemplos e testes

Os arquivos abaixo continuam úteis, mas **não devem prevalecer sobre Swagger/OpenAPI ou a `main`** quando houver diferença de IDs, payloads ou endpoints:

| Documento | Uso correto |
|---|---|
| `ENDPOINTS_INTERNOS.md` | inventário auxiliar de endpoints |
| `JSON_EXEMPLOS.md` | exemplos de payload; conferir UUID e contrato atual antes de copiar |
| `REQUISICOES_POSTMAN_LOTES.md` | roteiro de testes de lotes |
| `CODIGOS_REFERENCIA_TESTES.md` | testes de Código SGL/referência |
| `testes.md` | histórico e roteiros de validação |
| `API_AUDITORIA_PRE_SWAGGER.md` | snapshot da auditoria feita antes do fechamento do Swagger |
| `SGL_Relacao_Completa_Classes.pdf` | snapshot documental de classes |

## Regras importantes para outra IA

- não tratar um texto “próxima etapa” de documento histórico como planejamento vigente;
- novos contratos públicos devem usar UUID, mesmo que exemplos antigos mostrem IDs numéricos;
- migrations antigas são imutáveis;
- autenticação definitiva ainda não está concluída;
- Resíduos existe em branch divergente e precisa ser reconciliado antes de integrar;
- a próxima grande etapa funcional está no frontend: Administração/Cadastros;
- o refactor para inglês é pós-protótipo, não uma tarefa paralela ao desenvolvimento funcional.
