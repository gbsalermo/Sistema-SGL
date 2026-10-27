# Etapa 9 — Relatórios consolidados | contrato e checklist de início

**Preparado em:** 26/10/2026  
**Branches:** `collab/etapa-9-relatorios-consolidados` nos repositórios `gbsalermo/Sistema-SGL` e `gbsalermo/SGL-FRONTEND`.  
**Base:** commits de merge da Etapa 8 na `main`: backend `818d5804f7dddf8eb21fb62c5e5e5a9a841b7adc`; frontend `12a195af7e90ab63cadc949432d5adca0fb54b98`.  
**Estado original (26/10):** preparação documental, anterior à implementação. **Checkpoint de encerramento (27/10):** Etapa 9 implementada e validada funcionalmente na branch colaborativa, sem merge para `main`. As seções “Entregas planejadas” e “Checklist” abaixo documentam o plano de partida e devem ser lidas como histórico; as entregas efetivas e limitações estão em `CONTINUIDADE.md` e nos documentos de Relatórios/Exportação e Integração Frontend.

## Fonte de verdade e conflitos documentais

1. O roadmap vigente é `Sistema-SGL/docs/PLANO_PRE_PRODUCAO.md`, seção "Sequência macro vigente após a 7": **9 — Relatórios consolidados**, 10 — Rótulos, 11 — Manual/delete lógico, 12 — testes globais, 13 — auditoria estrutural final.
2. Trechos mais antigos que chamam "Etapa 9" de Pedidos/Soluções/participação são históricos e **foram superados** pela Etapa 8 concluída. Não reabrir as regras de vínculo, estoque ou participação por causa desses títulos antigos.
3. Projeto/SCI/Atividade têm hub operacional próprio. A Central de Relatórios será um caminho **somente de consulta, resumo e exportação**, não CRUD alternativo.
4. `Sistema-SGL/docs/RELATORIOS.md` (decisões originalmente registradas como evolução da Etapa 7) e `SGL-FRONTEND/CONTINUIDADE.md`, seção "Decisão futura — Central de Relatórios na Etapa 9", definem a intenção atual. Este documento consolida o início da etapa, sem declarar implementação.

## Entregas planejadas

### 9.1 — Auditoria dos contratos atuais

- Mapear consultas/DTOs/serviços de Estagiários, Produtos, Movimentações, Resumo operacional, Estoque/Lotes, Resíduos, Fiscalização e Pessoas por Laboratório.
- Conferir filtros, tenant/Unidade, status, datas, paginação e reutilização de exportações em PDF/XLSX. Verificar onde há fonte de dados suficiente para Projetos/SCI/Atividades e vínculos.
- Evitar introduzir consultas N+1 e consultas sem delimitação por Unidade. Medir/corrigir problemas concretos antes de refatoração abrangente.
- Registrar testes existentes e lacunas reais; não presumir que build anterior valida alterações futuras.

### 9.2 — Relatório de Projetos

- Backend: contrato de relatório de Projeto por Unidade e filtros aprovados; definir campos e filtros precisos antes de codificar.
- Campos de referência da decisão vigente: Código SEG, Projeto, Laboratório, responsável/líder, datas, status, situação de execução, recurso externo, total de SCI e Atividades. Dimensões de participações/Estagiários apenas se puderem ser obtidas sem ambiguidade e sem expor outros tenants.
- Prévia JSON e exportações PDF/XLSX com **mesma consulta e mesmos filtros**, quando implementadas. Não duplicar cálculo oficial na UI.
- Frontend: adicionar **Projetos** à Central de Relatórios, preservando identidade visual, responsividade, Dark/Light, feedback de erros/vazios, carregamento e acessibilidade.

### 9.3 — Movimentações consolidadas (interface)

- Substituir as **duas opções de topo** "Movimentações" e "Resumo operacional" por **uma opção "Movimentações"**.
- Exibir modos internos **Resumo** e **Detalhamento**, reaproveitando respectivamente os endpoints já existentes de resumo operacional e movimentações.
- **Não eliminar endpoints/exports antigos sem necessidade**; manter compatibilidade de filtros, prévias e downloads.

### 9.4 — Testes, regressão e encerramento

- Backend: JUnit (cálculo, filtros, Unidade, exportação e compatibilidade), ArchUnit já integrado, inicialização PostgreSQL/Flyway. Respeitar as decisões FEFO/FIFO, baixa única e snapshots sem reimplementar estoque.
- Frontend: `npm run build`, navegação, modos internos, filtros, erros, estados vazios, responsividade e visual padronizado; manter suites expandidas da Etapa 12 como trilho separado.
- Comparar prévia com PDF/XLSX pelos mesmos filtros; garantir que nenhum módulo existente desapareça da Central.
- Documentar exatamente o implementado/validado em cada bloco; enviar para `collab/*`; revisar workflow GitHub → GitLab; MR no GitLab somente com aceite expresso.

## Regras de implementação e colaboração

- **Backend alterado manualmente pelo responsável, por entidade/serviço e em pequenas entregas**; a IA deve propor mudanças incrementais e orientar testes. Se uma tarefa exigir modificação direta, respeitar o fluxo combinado para o módulo.
- GitLab institucional é a fonte canônica de `main`. GitHub `collab/*` é espaço de colaboração; workflow sincroniza a branch com GitLab. **Sem push/merge na `main` pelo GitHub, sem force push.**
- Antes de cada implementação: ler `CONTINUIDADE.md`, `docs/PLANO_PRE_PRODUCAO.md`, `docs/RELATORIOS.md`, `docs/EXPORTACAO_RELATORIOS.md`, `docs/GUIA_ESTRUTURAL.md`, `docs/ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md` no backend; `CONTINUIDADE.md`, `docs/PADRAO_VISUAL_PRE_PRODUCAO.md`, `docs/IDENTIDADE_VISUAL.md`, `docs/REGRA_PADRONIZACAO_NOVOS_MODULOS.md`, `docs/INVENTARIO_TELAS.md` e services/views de relatórios no frontend.
- Manter a identidade visual existente (componentes, tipografia, cards, estados, modais, dark/light, mobile). Não inventar uma segunda experiência de Relatórios ou uma segunda fonte de verdade.
- Autenticação institucional é trilho separado e não deve bloquear a evolução do MVP atual. Não usar dados de seed/DEMO como prova de contrato de produção.
- Se houver conflito entre documento histórico e contrato atual, explicitar o adendo, preservando rastreabilidade.

## Critérios de pronto para iniciar 9.1

- [x] Merge da Etapa 8 confirmado nos dois repositórios e na `main` do GitHub.
- [x] Branch da Etapa 9 criada a partir do respectivo HEAD de `main`.
- [x] Roadmap e decisões de relatório anteriores identificados e reconciliados documentalmente.
- [ ] Auditoria detalhada dos endpoints/DTOs/filtros atuais (primeira atividade funcional da 9.1).
- [ ] Contrato final do relatório de Projetos homologado.
- [ ] Código, build e testes da Etapa 9 realizados.
