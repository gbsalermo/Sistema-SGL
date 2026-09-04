# Documentação — SGL Backend

**Atualizado em:** 03/09/2026

Este índice separa fontes atuais de snapshots históricos para impedir que uma pessoa ou IA retome o projeto por um estado ultrapassado.

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

## Estado documental atual

| Documento | Papel | Estado |
|---|---|---|
| `../CONTINUIDADE.md` | checkpoint técnico, decisões e próximo passo | **ATUAL — 03/09** |
| `DOSSIE_PROJETO_SGL.md` | visão completa para handoff humano/IA | **ATUAL — 03/09** |
| `MODULO_RESIDUOS.md` | domínio, status e contratos de Resíduos | **ATUAL** |
| `RELATORIOS.md` | cobertura atual de relatórios | **ATUAL — 03/09** |
| `EXPORTACAO_RELATORIOS.md` | PDF/XLSX e regras de exportação | **ATUAL — 03/09** |
| `PENDENCIAS_POS_PROTOTIPO.md` | refactors que não entram no fechamento funcional | **ATUAL** |
| `FLUXO_DO_SISTEMA.md` | fluxo de domínio | referência; validar novidades no dossiê/Swagger |
| `GUIA_ESTRUTURAL.md` | organização arquitetural | referência |
| `diagrama-uml-completo.puml` | visão UML | referência; pode ficar atrás de módulos recentes |

## Mudanças que invalidam o handoff de 31/08

Desde 31/08 chegaram à `main`:

```text
Resíduos reconciliado e completo
Relatório + PDF/XLSX de Resíduos
Código SGL do Resíduo desde o registro inicial
V11 e V12 do Flyway
Estagiários com cadastro/edição/encerramento
Pessoas por laboratório + PDF/XLSX
Suporte backend a Administração/Cadastros
Alteração administrativa isolada de perfil
```

Portanto, qualquer documento que ainda diga que:

```text
Resíduos precisa ser reconciliado
Administração é a próxima etapa
Relatório de Resíduos ainda não existe
Flyway termina em V10
```

deve ser interpretado como histórico até ser revisado.

## Contratos, exemplos e testes

Os arquivos abaixo continuam úteis, mas **não prevalecem sobre Swagger/OpenAPI ou `main`** quando houver diferença de IDs, payloads ou endpoints:

| Documento | Uso correto |
|---|---|
| `ENDPOINTS_INTERNOS.md` | inventário auxiliar de endpoints |
| `JSON_EXEMPLOS.md` | exemplos; conferir contrato atual antes de copiar |
| `REQUISICOES_POSTMAN_LOTES.md` | roteiro de testes de lotes |
| `CODIGOS_REFERENCIA_TESTES.md` | testes de Código SGL/referência |
| `testes.md` | histórico e roteiros de validação |
| `API_AUDITORIA_PRE_SWAGGER.md` | snapshot anterior ao fechamento do Swagger |
| `SGL_Relacao_Completa_Classes.pdf` | snapshot documental de classes |

## Regras importantes para outra IA

- novos contratos públicos usam UUID; `Long` permanece interno;
- migrations aplicadas são imutáveis;
- `Produto != Resíduo` e composição de Resíduo não movimenta estoque automaticamente;
- Unidade não terá CRUD manual normal no frontend;
- usuários serão criados/sincronizados pela futura autenticação institucional; Administração altera perfis existentes;
- autenticação/autorização definitiva ainda não está concluída;
- o próximo bloco do protótipo é **permissões → congelamento → homologação**, não Administração ou Resíduos;
- modelos de Resíduos pré-determinados são opção futura, não requisito atual;
- o refactor para inglês é pós-protótipo.
