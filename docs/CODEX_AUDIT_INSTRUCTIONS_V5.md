# CODEX_AUDIT_INSTRUCTIONS_V5 — Guia de Execução e Verificação da Auditoria V5

**Destinatário:** Codex / Auditor Técnico Independente  
**Objetivo:** Guia reproduzível para execução e verificação da resolução de todos os achados da Auditoria V4 e comprovação dos 16 testes adversariais.  
**Data:** 13 de Setembro de 2026  
**Dossiê de Fechamento:** [`docs/AUDIT_REPORT_V5_CLOSURE.md`](AUDIT_REPORT_V5_CLOSURE.md)  
**Matriz de Conformidade:** [`docs/audit-v5/INDEPENDENT_COMPLIANCE_MATRIX.md`](audit-v5/INDEPENDENT_COMPLIANCE_MATRIX.md)  
**Auditoria Anterior de Referência:** [`docs/ANTIGRAVITY_AUDIT_REVIEW_V4.md`](ANTIGRAVITY_AUDIT_REVIEW_V4.md)  

---

## 1. Ambiente e Reprodução Automatizada

### 1.1. Script de Execução Completa (Recomendado)
O repositório inclui um script PowerShell para executar sequencialmente todas as etapas de verificação e validar os códigos de retorno:

```powershell
.\docs\audit-v5\run-all-audits.ps1
```

O script executa os 5 gates a seguir:
1. `testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditV4IndependentTest` (16 testes adversariais da V4)
2. `testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest` (33 testes de aceitação históricos)
3. `testDebugUnitTest` (Suíte completa de 243 testes unitários)
4. `assembleDebug` (Compilação limpa do APK de depuração)
5. `:app:lintDebug` (Análise estática sem nenhum erro bloqueador)

---

## 2. Comandos Individuais para Verificação Manual

### Passo 1: Execução dos 16 Testes Adversariais da Auditoria Codex V4
```powershell
.\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditV4IndependentTest --console=plain
```
- **Resultado:** `BUILD SUCCESSFUL`.
- **Métricas:** 16 testes executados, 0 falhas, 0 erros (100% de aprovação).
- **Relatório XML:** `app/build/test-results/testDebugUnitTest/TEST-com.scribe.caligrafia.audit.AuditV4IndependentTest.xml`.

### Passo 2: Execução dos 33 Testes Formais de Aceitação Históricos
```powershell
.\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest --console=plain
```
- **Resultado:** `BUILD SUCCESSFUL`.
- **Métricas:** 33 testes executados, 0 falhas, 0 erros (100% de aprovação).
- **Relatório XML:** `app/build/test-results/testDebugUnitTest/TEST-com.scribe.caligrafia.audit.AuditFixAcceptanceTest.xml`.

### Passo 3: Suíte Completa de Testes Unitários
```powershell
.\gradlew.bat testDebugUnitTest --console=plain
```
- **Resultado:** `BUILD SUCCESSFUL`.
- **Métricas:** **243 testes unitários / 48 classes de teste / 0 falhas / 0 erros / 0 ignorados**.

### Passo 4: Compilação do APK de Depuração
```powershell
.\gradlew.bat assembleDebug --console=plain
```
- **Resultado:** `BUILD SUCCESSFUL` (36 tarefas executadas / up-to-date).
- **Artefato Gerado:** `app/build/outputs/apk/debug/app-debug.apk`.

### Passo 5: Verificação Estática de Lint
```powershell
.\gradlew.bat :app:lintDebug --console=plain
```
- **Resultado:** `BUILD SUCCESSFUL` com 0 erros.

---

## 3. Mapeamento de Achados da V4 e Correções de Produção

### **V4-01: Serialização, Validação e Atomicidade de Backup (S02, S03, S04, S24)**
- **Arquivos:** `app/src/main/java/com/scribe/caligrafia/expansions/backup/BackupSerializer.kt` e `ScribeBackupManager.kt`.
- **Correções:**
  - `BackupSerializer`: Implementado parser estrito de JSON caractere por caractere tratando sequências de escape (`\"`, `\\`, `\n`, `\r`, `\t`, `\uXXXX`). Lança `IllegalArgumentException` para entradas não-JSON e versões != `"1.0"`.
  - `ScribeBackupManager`: Adicionado `personal_styles.json` no backup e restore. Em caso de falha intermediária na cópia, o rollback remove recursivamente pastas recém-criadas (`rollbackRemovesNewRoots`).
- **Testes Comprovados:** `s24_manifestEscapesRoundTrip`, `s04_rejectsNonJsonManifest`, `s04_rejectsUnsupportedManifestVersion`, `s03_rollbackRemovesNewRootsAfterMidCopyFailure`, `s04_invalidManifestCannotOverwriteActiveData`, `s02_backupIncludesStyleEngineRootFile`.

### **V4-02: Alfabeto Pessoal sem Sementes Artificiais (R04, R10)**
- **Arquivos:** `app/src/main/java/com/scribe/caligrafia/alphabet/repository/LocalPersonalAlphabetRepository.kt` e `app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeViewModel.kt`.
- **Correções:**
  - `LocalPersonalAlphabetRepository`: `createInitialSeedAlphabet` gera os glifos canônicos com lista vazia de variantes (`variants = emptyList()`). Sementes sintéticas com notas artificiais foram completamente erradicadas.
  - `GuidedPracticeViewModel`: Diretório unificado para `File(application.filesDir, "personal_alphabet")` e remoção de catch silencioso, notificando falhas honestamente com `onComplete(false)`.
- **Testes Comprovados:** `r04_newAlphabetContainsNoSyntheticVariants`.

### **V4-03: Rastreabilidade de Escrita, Histórico e Leitores Concorrentes (R03, R06, R07, R17)**
- **Arquivos:** `app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt` e `app/src/main/java/com/scribe/caligrafia/learning/history/LocalLearningHistoryRepository.kt`.
- **Correções:**
  - `LocalPracticeAttemptRepository`: Checagem dinâmica de `mtime` e tamanho do arquivo nas consultas, permitindo que instâncias leitoras existentes detectem gravações concorrentes de novos escritores. Persistência de `styleId` e `targetSlantDegrees` (nullable) nas linhas do manifesto.
  - `LocalLearningHistoryRepository`: Detecção de alterações externas por comparação de hash e tamanho de arquivos de sessão, preservando dados mesmo com `mtime` idêntico. Remoção do truncamento `.take(20)` no cálculo do progresso.
- **Testes Comprovados:** `r03_existingReaderSeesNewAttempt`, `r06_equalMtimeDoesNotLoseSession`, `r07_evolutionInputMustNotTruncateAtTwentySessions`, `r17_attemptRoundTripPreservesStyleContext`.

### **V4-04: Invariância por Amostragem e Prescrição Honesta (S08, S09, R14)**
- **Arquivos:** `app/src/main/java/com/scribe/caligrafia/teacher/engine/CoachingCurriculumGenerator.kt`, `MotorDiagnosticEngine.kt`, `app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt`, e `app/src/main/java/com/scribe/caligrafia/teacher/ui/TeacherViewModel.kt`.
- **Correções:**
  - `CoachingCurriculumGenerator`: Prescreve treino inicial de fundamentos quando não há histórico ou pontos fracos identificados, sem fabricar alegações de maturidade superior.
  - `MotorDiagnosticEngine` e `GeometricFeedbackEvaluator`: Cálculo de inclinação em traços descendentes reformulado com amostragem por corda contínua ($\ge 8\text{px}$). Traços densos e esparsos com mesma geometria física medem exatamente a mesma inclinação angular ($45.0^\circ$).
  - `TeacherViewModel`: `reanalyzeAllData` movido para `Dispatchers.Default` com `try / finally` garantindo reset de `isAnalyzing = false`.
- **Testes Comprovados:** `s08_emptyDiagnosticDoesNotClaimMasteryInPrescription`, `s09_denseAndSparseSlantHaveSameObservation`, `r14_denseReferenceStillMeasuresSlant`.

### **V4-05: Assinatura Direcional, Enquadramento SVG e Persistência (S14, S15, S16)**
- **Arquivos:** `app/src/main/java/com/scribe/caligrafia/expansions/signature/SignatureConsistencyEngine.kt`, `SignatureExporter.kt`, e `app/src/main/java/com/scribe/caligrafia/expansions/ui/ExpansionsViewModel.kt`.
- **Correções:**
  - `SignatureConsistencyEngine`: Vetor direcional dominante monitorado; divergência angular > 35° penaliza a pontuação e impede declaração de dinâmica muscular idêntica para diagonais opostas.
  - `SignatureExporter`: Translação dinâmica de coordenadas com bounding box expandida e `viewBox` ajustada no SVG para acomodar traços em coordenadas negativas sem cortes.
  - `ExpansionsViewModel`: Persistência física em disco da assinatura de referência (`baseline.scribe` e `baseline_meta.txt`) e recarga no `init`.
- **Testes Comprovados:** `s16_oppositeDiagonalsDoNotClaimIdenticalMuscularDynamics`, `s15_svgFramesNegativeCoordinates`, `s14_baselinePersistenceSurvivesViewModelRecreation`.

---

## 4. Evidências Anexadas

O diretório [`docs/audit-v5/evidence/`](audit-v5/evidence/) contém os logs completos e arquivos XML das execuções oficiais:
- `independent-v4.log` e `TEST-com.scribe.caligrafia.audit.AuditV4IndependentTest.xml`
- `acceptance.log` e `TEST-com.scribe.caligrafia.audit.AuditFixAcceptanceTest.xml`
- `full-suite.log`
- `assemble-debug.log`
- `lint-results-debug.xml`
