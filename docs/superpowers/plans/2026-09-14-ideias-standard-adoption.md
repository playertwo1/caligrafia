# Ideias Standard Adoption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adotar o Ideias Standard no Scribe/Caligrafia, reduzir drift/contexto, remover material inseguro ou gerado sem valor ativo e separar validação contínua de release.

**Architecture:** A adoção é feita numa branch isolada, preservando a branch funcional atual. Contratos estruturados e documentos curtos passam a ser fontes operacionais; histórico e evidência extensa deixam de ser contexto padrão. O código Kotlin não é refatorado nesta mudança.

**Tech Stack:** Android/Kotlin/Gradle, GitHub Actions, Markdown, JSON/YAML declarativo.

**Spec:** `docs/superpowers/specs/2026-09-14-ideias-standard-adoption-design.md`

## Global Constraints
- Baseline funcional: `phase-f6-f7-finalization`.
- Não fechar F5.G/F6.G/F7.G sem evidência correspondente.
- Não alterar comportamento do app nesta adoção.
- Preservar código, testes, design assets e decisões de produto.
- Nenhum secret pode permanecer versionado.
- Evidência bruta reproduzível deve preferir artifacts de CI a arquivos versionados.
- `NOT_RUN != PASS`.

---

### Task 1: Segurança e baseline

**Files:**
- Delete: `SECRETS_GUIA.txt`
- Modify: `.gitignore`
- Create: `SECURITY_INCIDENT.md`
- Modify: `.github/workflows/build-release.yml`

**Produces:** árvore ativa sem credenciais conhecidas e release fail-closed.

- [ ] Remover o arquivo com credenciais versionadas.
- [ ] Bloquear padrões de keystore/segredos locais no `.gitignore`.
- [ ] Registrar orientação de rotação sem reproduzir valores.
- [ ] Fazer release exigir material de assinatura e nunca publicar release não assinada como válida.
- [ ] Revisar diff por tokens/password/base64/keystore.

### Task 2: Núcleo de governança e contexto

**Files:**
- Modify: `AGENTS.md`
- Replace: `PROJECT_STATE.md`
- Create: `PHASE_CURRENT.md`
- Create: `AI_CONTEXT_INDEX.md`
- Create: `CONTEXT_POLICY.md`
- Create: `context-manifest.json`
- Create: `INVARIANTS.yaml`
- Create: `ARTIFACT_CATALOG.md`
- Create: `.idea-standard/project-manifest.json`
- Create: `.idea-standard/standard.lock`

**Produces:** bootstrap curto, fontes de verdade explícitas e contratos legíveis por máquina.

- [ ] Compactar AGENTS para constituição operacional.
- [ ] Reescrever PROJECT_STATE como resume card do estado real F5/F6/F7.
- [ ] Separar autorização/aceite vigente em PHASE_CURRENT.
- [ ] Declarar rotas de contexto por tipo de tarefa.
- [ ] Declarar invariantes críticos com IDs estáveis.
- [ ] Registrar ownership dos artefatos.

### Task 3: Documentação canônica e contradições

**Files:**
- Modify: `README.md`
- Modify: `ROADMAP.md`
- Modify: `CHANGELOG.md`
- Modify: `AUDIT.md`
- Modify: `WATCHDOG.md`
- Review: `PRODUCT_SPEC.md`, `ARCHITECTURE.md`, `DATA_MODEL.md`, `LEARNING_SYSTEM.md`, `STYLE_ENGINE.md`, `STYLUS_ENGINE.md`, `UI_UX.md`, `SECURITY_PRIVACY.md`, `REFERENCES.md`, `BACKLOG.md`

**Produces:** uma fonte canônica por tipo de fato e ausência de instruções de início obsoletas.

- [ ] Remover status dinâmico duplicado do README/ROADMAP.
- [ ] Fazer documentos extensos apontarem para PROJECT_STATE/PHASE_CURRENT para estado atual.
- [ ] Tornar WATCHDOG condicional a risco e AUDIT condicional a revisão/gate.
- [ ] Preservar contratos de produto que ainda tenham autoridade própria.

### Task 4: Limpeza de evidência e documentos superseded

**Files:**
- Remove from active tree: logs/XMLs/outputs reproduzíveis em `docs/audit*`.
- Consolidate/archive: relatórios históricos de auditoria e narrativas superseded.
- Preserve: scripts, matrizes finais, resumos, imagens de design e testes reais em `app/src/test`.

**Produces:** árvore menor sem perda de rastreabilidade útil.

- [ ] Remover apenas artefatos reproduzíveis e sem autoridade própria.
- [ ] Manter scripts necessários para reproduzir auditorias.
- [ ] Manter matrizes/resumos que carreguem decisão ou finding não derivável.
- [ ] Criar índice de histórico se necessário para apontar ao Git para detalhes antigos.

### Task 5: CI e evidência moderna

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `.github/workflows/build-release.yml`
- Modify: `.gitignore`

**Produces:** validação contínua em PR/push e release separado.

- [ ] CI executar unit tests, lintDebug e assembleDebug.
- [ ] CI executar checks simples de secrets/governança.
- [ ] Upload de relatórios/build outputs como artifacts com retenção limitada.
- [ ] Release depender de tag/manual e assinatura válida.

### Task 6: Validação e pacote de auditoria

**Files:**
- Create: `STANDARD_ADOPTION_EVIDENCE.md`
- Create: `STANDARD_ADOPTION_AUDIT_PACKET.md`
- Modify: `PROJECT_STATE.md`
- Modify: `CHANGELOG.md`

**Produces:** adoção audit-ready, sem autoaprovação.

- [ ] Comparar branch de adoção contra baseline funcional.
- [ ] Confirmar que arquivos Kotlin não foram alterados.
- [ ] Confirmar que design assets permanecem.
- [ ] Confirmar ausência de segredo conhecido na árvore ativa.
- [ ] Registrar validações executadas e não executadas.
- [ ] Deixar gate da adoção `AUDIT_READY`, nunca PASS por autorrevisão.