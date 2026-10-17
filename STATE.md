# STATE — ponto de retomada

Qualquer agente deve conseguir retomar só com este arquivo + a seção ativa do `ROADMAP.md`, sem a conversa anterior. Curto: o que não for estado atual sai daqui. Histórico completo: [`docs/archive/PROJECT_STATE.md`](docs/archive/PROJECT_STATE.md).

**Atualizado:** 2026-10-17 · **Marco ativo:** F5 — Assinaturas, exportação e recuperação · **Estado:** READY

## O que funciona hoje (confirmado no código)
- App Android Kotlin `com.scribe.caligrafia` (versionName `0.8.1` em `app/build.gradle.kts`) com caderno, Stylus Lab, treino guiado/Ghost Mode, Style Engine, Learning System, evolução/replay, Meu Alfabeto, Professor determinístico e expansões (marcos M0–M8, tags `v0.1.0`–`v0.8.0`).
- Fases funcionais F0–F4 implementadas (commits `3d4f0c5`, `9f692d4`, `4dab8cb`, `43a84d6`, `127c604`), marcadas pelo implementador; sem parecer Codex registrado.
- Testes unitários em `app/src/test` (inclui suítes de auditoria V4/V5); CI `.github/workflows/build-release.yml` roda `testDebugUnitTest` e `assembleRelease assembleDebug`.

## Última atividade
- ID / commit: `e0df49d` (2026-09-13) — ponto de parada na fase F5 documentado.
- Resultado real: F0–F4 registradas como concluídas pelo implementador.
- Testes executados: `testDebugUnitTest` (297 testes) e `assembleDebug` → PASS segundo `docs/archive/PROJECT_STATE.md`; NOT_RUN nesta sessão de padronização (2026-10-17, só documentação).
- Evidência: `docs/archive/PROJECT_STATE.md`, `docs/audit-v5/`.

## Próxima ação
- F5.01 — unificar a política de pointer/borracha/eixos da assinatura com a escrita principal; começar por `SignatureCanvasView` e `docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md` §F5.

## Bloqueios e aprovações humanas
- Requisitos de hardware (S25 Ultra físico, Wear OS) seguem `PENDENTE_DISPOSITIVO`.
- Aprovação Codex de F0–F4 não registrada; o parecer V5 de revalidação apontou duas falhas reproduzidas.

## Divergências entre roadmap e implementação
- `docs/archive/PROJECT_STATE.md` declara app `v0.8.5` (versionCode 15), mas `app/build.gradle.kts` tem `versionName = "0.8.1"`, `versionCode = 11`.
- `README.md` dizia que a primeira tarefa seria SCR-001 (bootstrap); o app já passou dos marcos M0–M8.
- Os relatórios em `docs/audit-reports/` e `docs/archive/` têm links `file:///c:/Users/...` de máquina local, quebrados no GitHub (preservados como histórico).
