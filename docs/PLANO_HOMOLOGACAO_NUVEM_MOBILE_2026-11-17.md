# Plano de homologação do SGL em nuvem e dispositivos móveis

**Criado:** 17/11/2026 · **Estado:** PLANEJADO — sem ambiente implantado ou teste executado por esta revisão documental.  
**Fonte de implementação:** repositórios `gbsalermo/Sistema-SGL` e `gbsalermo/SGL-FRONTEND`, branches `release` do espelho GitHub; **GitLab `release` é canônica**, sujeito à conferência antes do deploy.  
**Responsável por autorização do ensaio:** mantenedor do SGL e, quando cabível, equipe institucional de infraestrutura/segurança.  
**Documento proprietário:** cenários executáveis de homologação cloud/mobile; **problemas e aceites ficam exclusivamente** no [registro único](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md) (`SGL-REV-029`, `030` e demais). Base transversal em [Etapa 12](ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md).

> **Limite explícito:** a análise de 17/11 foi leitura de código e documentação. Não criou infraestrutura, não implantou o SGL, não acessou banco institucional, não enviou requisições a endpoints reais e **não testou uma tela em iPhone/Android**. Este plano evita confundir suporte responsivo escrito em CSS com experiência efetivamente aceita.

## 1. Objetivo e arquitetura do cenário de nuvem

Reproduzir um uso fim a fim com **frontend Vue/Vite compilado**, **API Java/Spring Boot**, **PostgreSQL**, **Flyway ativo**, serviços de autenticação **simulados e autorizados**, acesso **HTTPS**, observabilidade mínima e **dados sintéticos**. O objetivo é verificar contratos HTTP, autorização por pessoa/Unidade, consultas, concorrência, experiência mobile, conteúdo da Ajuda e tolerância a rede intermitente. Não comparar apenas páginas carregadas; testar jornada e conteúdo persistido.

```text
Navegador desktop / Android / iOS
        │ HTTPS
Frontend estático da release  ──→  API /api/v1/*  ──→  PostgreSQL descartável
        │                               │                   │
Ajuda: texto, poster, PDF e MP4        JWT                 Flyway V1–V48+ 
                                        │
                            gateway institucional de teste
                             (stub autorizado, nunca produção)
```

**Ambiente sugerido:** *staging efêmero isolado*. Hospedagem específica deve ser decidida conforme orçamento, rede institucional, compatibilidade do build e regras de dados. **Não criar conta ou deploy sem aprovação**. Evitar perfis `dev/demo` expostos publicamente e credenciais reais; para testes, usar identidade simulada restrita à rede/VPN/IP autorizado, ou autenticação corporativa de homologação **se oficialmente disponível**.

### Condições para abrir o ambiente

1. Confirmar commit exato da **`release` no GitLab** e sincronização espelho GitHub; trabalhar em branch/tag de execução imutável e registrar hashes frontend/backend.
2. Banco PostgreSQL **descartável** sem conexão com produção; executar Flyway desde vazio e, em ensaio separado, migrações em dump sanitizado se aprovado. Não importar tabelas com dados pessoais institucionais.
3. Segredos de infraestrutura fora do Git; configurar `VITE_API_BASE_URL` apontando à API de staging com caminho `/api` coerente com os services `/v1/*`; TLS, CORS restrito, logs higienizados, rate limits e backups/restore de teste.
4. Desabilitar/excluir mecanismos `DevAuthController` em staging público (controller anotado `@Profile(dev,demo)`); validar autenticação JWT e papéis sem dar acesso anônimo à API. Usar um gateway corporativo de teste aprovado, não substituir o fluxo real silenciosamente.
5. Massa sintética determinística: **2 Unidades**, 2–4 laboratórios por Unidade, empregados, dois estagiários distintos, orientadores, vínculos válidos/encerrados, Projeto/SCI/Atividade, lotes perecíveis/não perecíveis, recipientes abertos/fechados, Pedidos, Soluções e Resíduos. Nunca expor identificadores ou credenciais de pessoas reais.
6. Registrar versão de PostgreSQL/Java/Node, recursos CPU/RAM, pool, latência de rede, proxy/CDN, perfil Spring, versão de frontend e configurações **não secretas**. Definir antes as metas de p95/p99 e erro — **sem inventar metas como se já estivessem homologadas**.
7. Janela limitada de smoke → integração → carga gradual; somente com aprovação e proteção de recursos. Testes de concorrência e carga nunca apontam para produção por padrão.

## 2. Matriz de endpoints a exercitar com sessão real de staging

| ID | Persona / intenção | Caminhos a validar | Evidência de aceite |
|---|---|---|---|
| CL-01 | Login, sessão, 401/403 e expiração | `POST /api/v1/auth/login`, `GET /api/v1/auth/me` e rotas protegidas | JWT ativo, pendente e inativo; sem credenciais reais; navegação e renovação/expiração |
| CL-02 | Perfil e Unidade | GETs de Usuários, Estagiários, Pedidos, Resíduos, Relatórios e Contextos | **Negar acesso não autorizado de outra pessoa da mesma Unidade**, além de isolar Unidades; verificar `SGL-REV-001–004`, `018–020`, `026` |
| CL-03 | Contextos de estágio | `GET /api/v1/contextos-operacionais/usuarios/{id}` e participação em Projeto/Atividade | Estagiário com 0, 1 ou N participações; filtro por vínculo, laboratório, vigência, orientador |
| CL-04 | Pedido | `POST /api/v1/pedidos`, `GET /api/v1/pedidos/por-usuario`, consultar e cancelar | Sem laboratório inelegível; quantidade e unidade corretas; tentativa de agir em nome de terceiros bloqueada |
| CL-05 | Aprovação e estoque | `POST /api/v1/pedidos/{id}/aprovar`, entrega/cancelamento e Movimentações | FEFO/FIFO, abertura mínima de recipientes, baixa única, rollback/estorno conforme regra |
| CL-06 | Solução | `POST /api/v1/solucoes/pedidos` e ações de aprovação/entrega/cancelamento | Prévia/aprovação/preparação, confirmação de solução preparada e rótulo |
| CL-07 | Resíduo | endpoints de `/api/v1/residuos`, histórico e rótulo | Componente livre e catálogo, Projeto/participação, permissão de impressão e transições |
| CL-08 | Estoque e Dashboard | `GET /api/v1/estoque-central/por-unidade`, `/estoque-baixo`, lotes por estoque; listas de gestão | Request count, N estoques, falha parcial de lotes visível, saldos corretos (`SGL-REV-028`) |
| CL-09 | Relatórios | `/api/v1/relatorios/movimentacoes`, `/resumo-operacional`, exportações | Filtros, resultados equivalentes, PDF/XLSX, quantidades por unidade sem somar kg + L (`SGL-REV-025–027`) |
| CL-10 | Registro corporativo de teste | busca/importação de pessoa pela Gestão e evento de estágio sob infraestrutura aprovada | Não confundir primeiro login, importação e evento de vínculo; testar repetição e autorização (`SGL-REV-022`) |
| CL-11 | Central de Ajuda | `/ajuda`, posters, PDF, `/ajuda/videos/<id>.mp4` | HTTP 200/206 quando suportado, MIME correto, conteúdo reproduzível, fallback legível, sem editar mídias (`SGL-REV-012`) |

**Regra:** status HTTP, body/DTO, tempos e trilha de banco devem ser registrados com cliente real, não apenas teste de controller mockado. Não fazer tentativas de varredura de contas/identificadores externos ou de terceiros; usar contas e IDs sintéticos autorizados.

## 3. Matriz mobile — experiência do estagiário

### Dispositivos e condições mínimas

**Emulação inicial de viewport:** larguras CSS **320, 360, 390, 430, 768 e 1024 px**; testar também desktop para regressão. Incluir portrait e landscape, zoom do navegador em 200%, fonte aumentada, teclado virtual aberto e gesto de voltar.  
**Dispositivos reais no aceite:** ao menos **Android/Chrome** e **iOS/Safari**, em versões suportadas pela instituição. Simulação desktop não reproduz completamente viewport dinâmico, teclado, seleção nativa, Safe Area, rolagem por toque ou mídia.  
**Rede:** Wi-Fi normal, throttling tipo conexão móvel lenta e perda/interrupção temporária de rede; registrar cenários e perfil sem afirmar disponibilidade offline. Testar retorno à conexão sem envio duplicado.  
**Acessibilidade:** foco, rótulos, contraste, mensagens anunciáveis, alvos tocáveis, modais com botão de fechar visível, ausência de scroll horizontal da página e rolagem localizada em tabelas grandes.

| ID | Jornada do estagiário | O que observar no celular |
|---|---|---|
| MB-01 | Autenticar, atualizar tela, expirar sessão e voltar | Formulário não oculto pelo teclado, sessão segura, 401/403 amigáveis, sem dados cruzados |
| MB-02 | Primeiro acesso pendente, liberação e escolha de laboratório | Estado claro, nenhum botão operacional habilitado indevidamente; acesso após autorização |
| MB-03 | Estagiário multilaboratório com 0/1/N participações | Seleção de Atividade/Projeto/SCI e laboratório **permitido**, erro útil sem participação; nada cortado |
| MB-04 | Novo Pedido de material | Adicionar/remover itens, selecionar apresentação ou medida, informar quantidade decimal, verificar saldo, urgência, observações, enviar **uma vez** |
| MB-05 | Meus Pedidos / detalhe | Encontrar situação, abrir pedido, ver observações; listas/detalhes funcionam com toque e rolagem |
| MB-06 | Solicitar Solução | Padrão/fora do catálogo, vários componentes, unidade/quantidade, concentração/finalidade, envio e acompanhamento |
| MB-07 | Informar Resíduo | Modelo/manual, laboratório, composição livre/catálogo, quantidade, riscos e medidas de segurança; teclado/scroll até **Enviar** |
| MB-08 | Meus Resíduos e Minhas Soluções | Acompanhar situação, histórico e mensagens sem layout quebrado |
| MB-09 | Central de Ajuda no celular | Menu abre/fecha, busca, FAQ, texto legível, vídeo não estoura largura e fallback permite prosseguir |
| MB-10 | Falha de conexão durante formulário/envio | Feedback de erro, dados não perdidos indevidamente, ausência de pedido duplicado após retry/refresh |
| MB-11 | Trocar laboratório/atividade repetidas vezes | Dados dependentes reinicializados corretamente; não permitir submit com laboratório da participação anterior |
| MB-12 | Acessibilidade e compatibilidade | Controle de foco, rolagem dentro de modal, zoom 200%, acessibilidade com leitor de tela básico, navegadores reais |

**Critério bloqueante de mobile:** nenhum fluxo essencial de estagiário pode exigir desktop; envio deve ser possível sem rolagem horizontal global, campos/ações não podem ficar inacessíveis atrás de teclado, overlays ou drawers, e erro de rede/validação deve ser compreensível. Mensagens de permissão devem refletir o **backend**, não apenas filtro visual.

### Evidência atual de código (não homologação)

- `NovoPedidoView.vue` contém grades de itens e `@media` para 900/720 px; `NovaSolucaoView.vue` tem regras móveis para composição e grade com uma coluna a 720 px; `InformarResiduoView.vue` reduz grades em 900/620 px.
- `EstagiariosGestaoView.vue` possui tabela com `min-width: 1160px` dentro de `.table-wrap {overflow-x:auto}` e media queries em 1150/680/480 px. Essa é **tela de Gestão** e não prova bloqueio do solicitante no celular; testar navegação/horizontal scroll local no papel apropriado.
- `HelpCenterView.vue` transforma menu lateral em drawer a 959 px; `HelpTaskContent.vue` limita vídeo a largura de 100%; `HelpCenterView.spec.ts` cobre toggle/Escape com Vue Test Utils, **não toque e Safari reais**.
- Não há comprovação de E2E mobile real só por esses estilos e unitários. Não afirmar “100% responsivo” sem MB-01 a MB-12.

## 4. Carga, resiliência e observabilidade na nuvem

Executar em camadas, **sem tráfego agressivo até aprovar limite/volume**:
1. **Smoke controlado:** iniciar serviços, verificar health, auth, CORS, primeira navegação, assets e cada contrato prioritário.
2. **Fluxos integrados:** executar CL-01 a CL-11 com persistência PostgreSQL e dois perfis/Unidades sintéticos, coletar resultados, status, redirects e erros de JS.
3. **Medições de frontend:** registrar tamanho dos bundles e mídia, número de requests por tela, waterfall, LCP/INP/CLS quando disponível, tempo de interação e mudanças de navegação. Registrar limites antes do aceite, sem inventar medições.
4. **Perfil de carga aprovado:** cenários com N usuários simultâneos e N registros por domínio definidos junto ao mantenedor; medir throughput, erros, p50/p95/p99, heap, CPU, pool e SQL. Investigar `findAll().stream()` de Relatórios e fan-out de lotes no Dashboard, comparando pequeno vs. grande volume.
5. **Falhas e recuperação:** API 401/403/422/5xx, timeout, queda de banco de teste, reinício de serviço, cliente offline temporário, request abortada, validação repetida, recebimento de eventos duplicados e consistência de estoque. Nunca apagar dados ou induzir carga em banco institucional.
6. **Repetir no mobile:** testar rede móvel lenta, posters/vídeos da Ajuda e formulários extensos, evitando baixar mídia pesada sem necessidade; comparar experiência com desktop e registrar reflows.

## 5. Instrumentação, evidências e portões

Para cada teste, preencher:

| Campo | Registro obrigatório |
|---|---|
| ID e cenário | CL-xx / MB-xx, versão da matriz |
| Artefato | GitLab `release` commit backend/frontend + hash dos builds |
| Ambiente | URL de staging privada/restrita, plataforma, versões sem segredos |
| Persona | perfil, Unidade, laboratórios, participação, fixtures sintéticos |
| Cliente | Android/Chrome ou iOS/Safari e versão; viewport; rede |
| Resultado | esperado, observado, **PASSOU / FALHOU / BLOQUEADO / NÃO EXECUTADO** |
| Desempenho | total requests, waterfall, p50/p95/p99, erros, consumo de recursos, quando medido |
| Evidência | screenshot/video de tela sintética, traces sem tokens, logs higienizados |
| Pendência | ID do [registro único](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md), decisão e reteste |

**Portão de aceite:** nenhum risco de autorização horizontal P1 sem decisão/teste; rotas e DTOs confiáveis, migrations Flyway executadas em PostgreSQL isolado, fluxos essenciais de estagiários usáveis em **Android e iOS**, navegação e formulários sem bloqueios, tratamento de falhas de rede e carga dentro de metas **previamente aprovadas**. Ausência de dispositivo ou ambiente gera `BLOQUEADO/NÃO EXECUTADO`, nunca `PASSOU`.

**Dependências pré-existentes:** `SGL-REV-001–004`, `009–013`, `015–017`, `018–020`, `022–028`. O foco mobile e o ambiente de cloud/staging ficam rastreados em `SGL-REV-029/030`.

## 6. Próxima ação autorizável (não executada)

Quando o mantenedor aprovar infraestrutura: (1) escolher hospedagem/rede e teto de custo, (2) criar staging efêmero, (3) injetar fixtures sintéticos e gateway institucional de teste, (4) executar smoke de CL-01–11, (5) realizar navegação em Android/iOS com MB-01–12, (6) medir somente depois carga gradual, (7) emitir relatório com falhas e critérios go/no-go. **Não usar este documento como permissão de criar serviços, custos, contas, envios de e-mail ou testes em produção.**
