# Proposta — histórico de formações acadêmicas do SGL

> **Estado:** modelagem aprovada como proposta para discussão, **não implementada**. Documento deliberadamente isolado do ciclo de correção de produtos e de qualquer release de produção.

## Objetivo

Permitir que o gestor registre o histórico acadêmico da **pessoa**: graduação, especialização/pós-graduação lato sensu, mestrado, doutorado, pós-doutorado quando aplicável, técnico e ensino médio. Cada ocorrência informa o curso e se a formação foi concluída, permanece em andamento ou foi interrompida. Uma mesma pessoa pode ter várias formações, inclusive simultâneas.

## Reaproveitar o modelo atual

- `Curso`: catálogo reutilizável por Unidade, com nome, UUID público e ativo/inativo; **não transformá-lo em currículo individual**.
- `VinculoEstagio`: manter `formacao`, `formacaoOutro` e `curso_id` como **referência/snapshot contextual do vínculo** durante a transição.
- `FormacaoEstagiario`: não reutilizar indiscriminadamente como novo enum de grau acadêmico; contém `APOIO_ADMINISTRATIVO` e `APOIO_TECNICO`, que são categorias de atuação, não níveis educacionais.
- Não alterar substituição de bolsas, encerramento/prorrogação de vínculos, orientação, atividades e suas regras.

## Nova entidade sugerida

`FormacaoAcademica` com vínculo obrigatório à entidade **Usuario/Pessoa** (não ao vínculo de estágio), Unidade de contexto e um `Curso` do catálogo da mesma Unidade.

Campos propostos:
- Identificador público, pessoa/usuário, Unidade e curso.
- `nivelAcademico`: ENSINO_MEDIO, TECNICO, GRADUACAO, ESPECIALIZACAO, MESTRADO, DOUTORADO, POS_DOUTORADO, OUTRO (com cuidado para não tratar pós-doutorado como título de grau).
- `situacao`: EM_ANDAMENTO, CONCLUIDO, INTERROMPIDO (eventualmente TRANCADO, mediante decisão funcional).
- Instituição de ensino (texto ou catálogo futuro), data/ano de início, conclusão efetiva opcional, previsão opcional e observações.
- Histórico mínimo de criação, atualização, mudança de situação e autoria, com trilha de auditoria; evitar exclusão física quando existir referência histórica.

## Regras propostas

1. Várias formações por pessoa, inclusive no mesmo nível; impedir apenas duplicidades inequívocas da **mesma ocorrência**, e não bloquear cursos homônimos de instituições diferentes.
2. Curso do catálogo é reutilizável. Cursos inativos continuam visíveis em históricos, mas não elegíveis para novos registros.
3. Só marcar conclusão mediante ação explícita e, se a data for informada, validar `dataConclusao >= dataInicio`. Não converter automaticamente previsão em conclusão.
4. Não presumir que formação antiga está concluída quando houver apenas `curso_id` no vínculo.
5. Respeitar autorização administrativa, Unidade em operação e política de privacidade: informações acadêmicas pertencem à pessoa, e acesso por outras Unidades deve seguir regras expressas.
6. Alteração da bolsa ou encerramento do estágio não apaga nem encerra formações acadêmicas.
7. Não mudar de imediato o contrato de `VinculoEstagio`; manter compatibilidade de DTOs e relatórios antigos.
8. Para registros históricos, migração assistida/conciliada e idempotente; não criar históricos duplicados por vários vínculos da mesma pessoa.

## Interface proposta

Na Gestão de Estagiários, adicionar seção **Formação acadêmica** no perfil: linha do tempo/lista de cursos, nível, situação e instituição; botão **Adicionar formação**, com seletor de curso existente ou **Cadastrar novo curso** reaproveitando o catálogo da Unidade. Edição de andamento, conclusão ou interrupção. Exibir formações concluídas e em andamento conjuntamente, sem misturar com vínculos/bolsas.

O formulário de estágio existente pode continuar usando a formação associada ao vínculo. Uma integração futura pode sugerir uma formação em andamento para preencher o vínculo, mas sem modificar o histórico automaticamente.

## Plano de implementação futuro (não iniciado)

1. Validar permissões e escopo pessoa × Unidade com o cliente.
2. Definir contratos de nível, situação e datas; migration aditiva e entidade `FormacaoAcademica`.
3. Repositório, DTOs e service com isolamento por Unidade e validações.
4. API de listagem/CRUD com inativação/correção auditável.
5. Frontend com histórico no perfil e formulário reutilizando `Curso`.
6. Migração assistida dos vínculos legados, sem inferência de conclusão.
7. Testes: pessoa com múltiplos cursos simultâneos; histórico após troca de bolsa; pessoa que deixa de ser estagiário; isolamento institucional; curso inativo; duplicação e datas; mobile.

**Não levar esta proposta junto com o hotfix do cadastro de produtos.**
