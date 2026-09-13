# Baseline de Execução — Scribe Funcional

**Data:** 2026-09-13  
**Branch:** `main`  
**HEAD Commit:** `e9c42bba7ede1a46b8adf905cff5a2eed019d070`  
**Upstream:** `origin/main` (sincronizado)  
**Status do Working Tree:** Limpo (sem alterações pendentes)

## Configuração do Ambiente
- **OS:** Windows 11
- **Gradle:** 9.3.1 (wrapper local)
- **Kotlin:** 2.0.21
- **Android Target SDK:** 35
- **Min SDK:** 26
- **JVM Target:** 17

## Estado dos Testes Unitários de Partida
- **Testes Executados:** 252 testes unitários
- **Falhas / Erros:** 0 falhas, 0 erros
- **Compilação Debug:** `assembleDebug` SUCCESS (20s)
- **Compilação Release:** `assembleRelease` SUCCESS (1m 4s) com assinatura JKS e 4 secrets ativos
- **Versão:** v0.8.1 (versionCode 11)

## Arquivos e Módulos no Escopo de F0
1. `app/src/main/java/com/scribe/caligrafia/expansions/backup/BackupSerializer.kt` (parser JSON estrito)
2. `app/src/main/java/com/scribe/caligrafia/expansions/backup/ScribeBackupManager.kt` (rollback e integridade atômica)
3. `app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt` (invalidação de cache de strokes)
4. `app/src/main/java/com/scribe/caligrafia/learning/history/LocalLearningHistoryRepository.kt` (sincronização multicliente)
5. `app/src/test/java/com/scribe/caligrafia/audit/AuditV5RecheckTest.kt` (incorporação de testes permanentes)
