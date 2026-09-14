# HANDOFF — Ideias Standard Adoption

**Data:** 2026-09-14  
**Status:** PAUSADO A PEDIDO DA PRODUCT AUTHORITY  
**Repositório:** `playertwo1/caligrafia`  
**Branch de trabalho:** `standard-adoption-v1`  
**Branch baseline preservada:** `phase-f6-f7-finalization`  
**Baseline SHA:** `cdb0fd47704edb5c0903626fb53d36cca05616b1`  
**HEAD antes deste handoff:** `d6906ab83153516fddf5e86104df3b96ca50fc1d`  
**Promoção para `main`:** NÃO AUTORIZADA / NÃO EXECUTADA

## Objetivo da mudança

Elevar o Caligrafia/Scribe ao padrão de qualidade do `ideias_standard`, usando este repositório como primeiro piloto brownfield. A adoção deve reduzir contexto desperdiçado, eliminar duplicação documental, separar estado dinâmico de histórico, reforçar segurança e CI e preservar toda regra funcional/material do produto.

## Decisão aprovada

Foi aprovada a opção de **adoção completa do Ideias Standard + limpeza controlada**, sem misturar nesta mesma change unit uma grande refatoração Kotlin.

Perfil alvo:

- `DEEP`
- pack `android`
- pack `multi-agent`
- pack `sensitive-data`
- pack `ai` NÃO ativado nesta adoção; IA no produto continua futura

## O que já foi materializado na branch

A branch está 21 commits à frente da baseline e já contém, entre outras mudanças:

- `.idea-standard/project-manifest.json`
- `.idea-standard/standard.lock`
- `AI_CONTEXT_INDEX.md`
- `ARTIFACT_CATALOG.md`
- `CONTEXT_POLICY.md`
- `INVARIANTS.yaml`
- `PHASE_CURRENT.md`
- `context-manifest.json`
- `SECURITY_INCIDENT.md`
- revisão de `AGENTS.md`
- revisão de `AUDIT.md`
- revisão de `WATCHDOG.md`
- compactação de `PROJECT_STATE.md`
- revisão de `README.md`
- revisão de `ROADMAP.md`
- atualização de `CHANGELOG.md`
- atualização de `.gitignore`
- atualização de `.github/workflows/build-release.yml`
- spec de adoção em `docs/superpowers/specs/2026-09-14-ideias-standard-adoption-design.md`
- plano de execução em `docs/superpowers/plans/2026-09-14-ideias-standard-adoption.md`

## Finding crítico de segurança

Foi encontrado `SECRETS_GUIA.txt` com credenciais de assinatura em texto puro.

Ações já executadas:

- `SECRETS_GUIA.txt` removido da branch de adoção;
- incidente registrado em `SECURITY_INCIDENT.md`;
- `.gitignore` reforçado para materiais de keystore/segredos.

Importante:

- não repetir valores secretos em documentação, logs ou prompts;
- considerar as credenciais expostas como comprometidas;
- trocar as **senhas** associadas ao material de assinatura antes de considerar o fluxo de release confiável;
- preservar o certificado/chave de assinatura necessário à compatibilidade de atualização do app, salvo decisão explícita diferente;
- buscas feitas nos caminhos `keystore/scribe-release.jks` e `keystore/keystore-base64.txt` não mostraram commits desses arquivos, mas isso não elimina a necessidade de rotação das credenciais expostas.

## Estado funcional do produto que deve ser preservado

A baseline correta é `phase-f6-f7-finalization`.

- F5: implementação preservada, gate F5.G ainda aberto.
- F6: software avançado/parcialmente implementado, gate F6.G ainda aberto.
- F7: consolidação estática iniciada, gate F7.G ainda aberto.
- nenhuma dessas fases deve ser marcada PASS por documentação ou por esta adoção.
- validações físicas S25 Ultra / S Pen / Watch e jornadas E2E continuam necessárias conforme os contratos existentes.

## Limpeza ainda pendente

Continuar a classificação antes de remover arquivos:

### KEEP

- código e testes de produto;
- specs canônicas;
- decisões e invariantes;
- scripts reproduzíveis;
- matrizes finais;
- assets de design legítimos, incluindo as referências visuais.

### ARCHIVE / CONSOLIDATE

- auditorias históricas em Markdown;
- relatórios antigos ainda úteis como evidência histórica;
- documentos superseded como narrativas de progresso antigas.

### REMOVE FROM ACTIVE TREE quando comprovadamente reproduzível

- `*.log` gerados;
- `TEST-*.xml` gerados;
- `lint-results*.xml` / `lint-results*.txt` gerados;
- outputs e baselines duplicados/reproduzíveis;
- artefatos temporários;
- qualquer material de segredo.

Regra: não remover algo duvidoso só para reduzir volume. Preservar história via Git e manter apenas evidência resumida/canônica no tree ativo.

## Próximas ações ao retomar

1. Ler somente `AGENTS.md`, `PROJECT_STATE.md`, este handoff e o pedido atual.
2. Conferir `standard-adoption-v1` e comparar contra `phase-f6-f7-finalization`.
3. Revisar o plano em `docs/superpowers/plans/2026-09-14-ideias-standard-adoption.md`.
4. Terminar o lote de segurança e confirmar que nenhum valor sensível permanece no tree ativo.
5. Concluir governança/contexto sem alterar comportamento Kotlin.
6. Executar a limpeza controlada de logs/XMLs/outputs históricos reproduzíveis.
7. Separar CI normal de release e manter evidência bruta como artifact de CI, não como documentação ativa.
8. Validar diff completo contra a baseline.
9. Executar testes/build/lint aplicáveis quando houver ambiente disponível.
10. Solicitar auditoria independente antes de qualquer promoção ao `main`.

## Regras de retomada

- não trabalhar diretamente no `main`;
- não fazer force push;
- não declarar gates F5/F6/F7 fechados sem evidência específica;
- não reintroduzir secrets em arquivos do repositório;
- não iniciar grande refatoração Kotlin nesta change unit;
- `NOT_RUN != PASS`;
- integridade, segurança e evidência têm prioridade sobre economia de contexto.

## Observação operacional

Foram criadas branches auxiliares de snapshot durante a preparação do handoff (`handoff-temp-check`, `standard-adoption-v1-backup` e `resume-snapshot-do-not-use`). Elas apontam para o mesmo estado de snapshot e não fazem parte do desenho do produto; podem ser removidas posteriormente para reduzir ruído de branches.

## Próxima ação exata

Retomar pela **conclusão da adoção documental/segurança e limpeza controlada na branch `standard-adoption-v1`**, usando este arquivo como cartão de retomada. Não avançar para refatoração Kotlin nem merge em `main` nesta etapa.
