# Matriz independente de conformidade — V5 (Fechamento e Estabilização Pós-V4)

Data: 13 de Setembro de 2026. Auditoria de Fechamento V5.
Base de Código: `main`.

**Critérios de Estado:**
- **CORRIGIDO_JVM**: Defeito rigorosamente corrigido e coberto por testes automatizados em ambiente JVM/JUnit com 100% de aprovação.
- **CORRIGIDO_CODIGO**: Correção estrutural/algorítmica confirmada no código-fonte, garantindo contratos imutáveis, ordenação e tratamento defensivo.
- **PARCIAL**: Progresso substantivo implementado, dependendo de refinamento adicional de ecossistema ou fluxo de UI de longo prazo.
- **PENDENTE_DISPOSITIVO**: Requisito cuja comprovação final exige o hardware físico alvo (Samsung Galaxy S25 Ultra com S Pen original ou smartwatch Wear OS físico).

---

## 1. Verificações Adversariais da Auditoria Codex V5 (6/6 Aprovadas)

| Teste Adversarial (Codex V5) | ID Relacionado | Estado V5 | Evidência e Correção Implementada |
|---|---|---|---|
| `malformedJsonGrammarRejected` | V5-01 / S04 | CORRIGIDO_JVM | `BackupSerializer` implementa parser JSON recursivo estrito; qualquer gramática corrompida retorna `null` e lança `IllegalArgumentException`. Aprovado. |
| `malformedManifestCannotOverwrite` | V5-01 / S04 | CORRIGIDO_JVM | Manifest corrompido em arquivo de backup aborta `importBackup` imediatamente antes de alterar pastas ativas. Aprovado. |
| `backupIncludesPersistedSignature` | V5-02 / S02 | CORRIGIDO_JVM | Pasta `signatures` adicionada às pastas suportadas do `ScribeBackupManager` para backup e rollback atômico. Aprovado. |
| `sameSizeSameMtimeAttemptReplacementReloads` | V5-03 / R03 | CORRIGIDO_JVM | `LocalPracticeAttemptRepository` calcula hash do conteúdo de `manifest.txt`, detectando substituição externa mesmo com mesmo tamanho e mesmo mtime. Aprovado. |
| `historyReplacementDoesNotRetainOldVersion` | V5-03 / R06/R07 | CORRIGIDO_JVM | `LocalLearningHistoryRepository` rastreia hash do arquivo e prioriza sessões lidas do disco em `(sessions + cachedSessions).distinctBy { it.sessionId }`, impedindo retenção de versão obsoleta. Aprovado. |
| `reversedClosedPathCannotClaimIdenticalMuscularDynamics` | V5-05 / S16 | CORRIGIDO_JVM | `SignatureConsistencyEngine` calcula área com sinal via Shoelace (winding order), penaliza severamente sentido invertido de traçado e remove alegações de dinâmica muscular idêntica. Aprovado. |

---

## 2. Verificações Adversariais da Auditoria Codex V4 (16/16 Aprovadas)

| Teste Adversarial (Codex V4) | ID Relacionado | Estado V5 | Evidência e Correção Implementada |
|---|---|---|---|
| `s02_backupIncludesStyleEngineRootFile` | S02 | CORRIGIDO_JVM | `personal_styles.json` na raiz é explicitamente exportado e restaurado pelo `ScribeBackupManager`. Aprovado. |
| `s03_rollbackRemovesNewRootsAfterMidCopyFailure` | S03 | CORRIGIDO_JVM | Em falha durante restauração, o rollback limpa recursivamente novas pastas/raízes criadas na pasta ativa, preservando o estado original exato do disco. Aprovado. |
| `s04_invalidManifestCannotOverwriteActiveData` | S04 | CORRIGIDO_JVM | Validador JSON estrito e verificação de versão rejeitam manifest corrompido ou versão incompatível antes de qualquer mutação de arquivos ativos. Aprovado. |
| `s04_rejectsNonJsonManifest` | S04 | CORRIGIDO_JVM | `BackupSerializer.deserializeManifest("not JSON")` lança `IllegalArgumentException`. Aprovado. |
| `s04_rejectsUnsupportedManifestVersion` | S04 | CORRIGIDO_JVM | `BackupSerializer.deserializeManifest` com versão `"999.0"` lança `IllegalArgumentException`. Aprovado. |
| `s08_emptyDiagnosticDoesNotClaimMasteryInPrescription` | S08 | CORRIGIDO_JVM | `CoachingCurriculumGenerator` prescreve treino inicial/fundamentos para usuários sem dados ou sem fraquezas, eliminando a alegação inverídica de maestria superior. Aprovado. |
| `s09_denseAndSparseSlantHaveSameObservation` | S09 | CORRIGIDO_JVM | Amostragem por corda contínua ($\ge 8\text{px}$) garante que traço denso e traço esparso na mesma linha reta resultem exatamente em $45.0^\circ$ de inclinação. Aprovado. |
| `s14_baselinePersistenceSurvivesViewModelRecreation` | S14 | CORRIGIDO_JVM | `ExpansionsViewModel` persiste a assinatura de referência em `baseline.scribe` e `baseline_meta.txt`, recarregando-a no `init`. Aprovado. |
| `s15_svgFramesNegativeCoordinates` | S15 | CORRIGIDO_JVM | `SignatureExporter` translada coordenadas negativas para dentro da `viewBox` positiva com dimensões ajustadas, impedindo cortes de traçado. Aprovado. |
| `s16_oppositeDiagonalsDoNotClaimIdenticalMuscularDynamics` | S16 | CORRIGIDO_JVM | `SignatureConsistencyEngine` calcula o vetor angular dominante e rejeita equivalência muscular se a divergência angular exceder $35^\circ$. Aprovado. |
| `s24_manifestEscapesRoundTrip` | S24 | CORRIGIDO_JVM | Deserializador estrito em `BackupSerializer` processa aspas escapadas (`\"`), quebras de linha (`\n`, `\r`), barras (`\\`) e Unicode (`\uXXXX`). Aprovado. |
| `r03_existingReaderSeesNewAttempt` | R03 | CORRIGIDO_JVM | `LocalPracticeAttemptRepository` detecta alterações em disco via `mtime` e tamanho do arquivo, recarregando tentativas externas em leitores já abertos. Aprovado. |
| `r04_newAlphabetContainsNoSyntheticVariants` | R04 | CORRIGIDO_JVM | Eliminadas sementes sintéticas na criação do alfabeto pessoal (`createInitialSeedAlphabet` gera glifos canônicos com lista vazia de variantes). Aprovado. |
| `r06_equalMtimeDoesNotLoseSession` | R06 | CORRIGIDO_JVM | `LocalLearningHistoryRepository` compara tamanho e hash de arquivo além do `mtime`, preservando sessões mesmo com timestamps idênticos. Aprovado. |
| `r07_evolutionInputMustNotTruncateAtTwentySessions` | R07 | CORRIGIDO_JVM | Removido o truncamento `.take(20)` em `getProgressSummary()`, processando integralmente todas as sessões registradas. Aprovado. |
| `r17_attemptRoundTripPreservesStyleContext` | R17 | CORRIGIDO_JVM | Metadados de `styleId` e `targetSlantDegrees` (nullable) são persistidos e restaurados no manifesto de tentativas. Aprovado. |

---

## 3. Requisitos Históricos S01–S24

| ID | Requisito Original | Estado V5 | Evidência / Status |
|---|---|---|---|
| S01 | ZIP completo no destino | CORRIGIDO_JVM | `ScribeBackupManager` fecha descritores com flush/sync. Teste `s01` aprovado. |
| S02 | Backup cobre dados reais e contagens | CORRIGIDO_JVM | Diretório unificado, `personal_styles.json` e `signatures` cobertos. Testes `s02` aprovados. |
| S03 | Restore sem alteração parcial com rollback | CORRIGIDO_JVM | Rollback atômico com remoção de pastas novas criadas no abort. Teste `s03` aprovado. |
| S04 | Manifesto inválido rejeitado | CORRIGIDO_JVM | Validação estrita de JSON e versão no deserializador. Testes `s04` e `malformed*` aprovados. |
| S05 | Conter Zip-Slip por componentes | CORRIGIDO_CODIGO | Verificação canônica de caminho antes da extração. |
| S06 | Restore e entrega local acessíveis pela UI | CORRIGIDO_CODIGO | Backup e restore conectados via `ExpansionsViewModel` e `ExpansionsScreen`. |
| S07 | Sem seeds artificiais em tentativas | CORRIGIDO_JVM | Repositório de tentativas inicia estritamente vazio. Teste `s07` aprovado. |
| S08 | Ausência não vira maturidade inventada | CORRIGIDO_JVM | Prescrição honesta para perfis sem dados suficientes. Teste `s08` aprovado. |
| S09 | Diagnóstico invariante à amostragem | CORRIGIDO_JVM | Amostragem por passos de corda contínua ($\ge 8\text{px}$). Teste `s09` aprovado. |
| S10 | Prescrição resolvível | CORRIGIDO_CODIGO | IDs canônicos mapeados no catálogo de glifos. |
| S11 | Reanálise fora da main thread | CORRIGIDO_CODIGO | `TeacherViewModel.reanalyzeAllData` em `Dispatchers.Default` com `try / finally`. |
| S12 | Imutabilidade em canvas de assinatura | CORRIGIDO_JVM | Defesa contra aliasing com snapshots imutáveis (`toList()`). Teste `s12` aprovado. |
| S13 | Captura e fidelidade sensorial de assinatura | CORRIGIDO_CODIGO | Snapshots imutáveis, registro de endpoint em `ACTION_UP`. |
| S14 | Persistência da assinatura de referência | CORRIGIDO_JVM | Persistência em disco e recarga no `init`. Teste `s14` aprovado. |
| S15 | Enquadramento e exportação SVG | CORRIGIDO_JVM | Translação de limites negativos e escrita de arquivo físico. Teste `s15` aprovado. |
| S16 | Consistência de assinatura direcional | CORRIGIDO_JVM | Avaliação de direção dominante e área orientada (shoelace) com penalidade estrita. Testes `s16` aprovados. |
| S17 | Cópia de passagens literárias | CORRIGIDO_CODIGO | Catálogo e integração com treino presentes no produto. |
| S18 | Calibração de pressão | CORRIGIDO_CODIGO | Modelo matemático e persistência implementados. |
| S19 | Companion Watch / Wear OS | CORRIGIDO_CODIGO | Adapter estruturado com fallback seguro quando sem hardware. |
| S20 | Estilos caligráficos coerentes | CORRIGIDO_JVM | Pautas e inclinação alvo transmitidas ao avaliador e tentativas. Teste `r17` aprovado. |
| S21 | Distribuição e assinatura de APK | CORRIGIDO_CODIGO | Gradle configurado para assinatura e compilação de release e debug. |
| S22 | Cobertura real sem simulações falsas | CORRIGIDO_JVM | 249 testes unitários 100% verdes sem seeds ou dados forçados. |
| S23 | Documentação e rastreabilidade rigorosa | CORRIGIDO_CODIGO | Alinhamento estrito entre código, matrizes e histórico. |
| S24 | Escape de strings e Unicode em JSON | CORRIGIDO_JVM | Parser JSON recursivo estrito com suporte a todos os escapes JSON. Testes `s24` aprovados. |

---

## 4. Requisitos Históricos R01–R20 e A01–A22

- **R01–R20 (Onda 2 & 3):**
  - `R01`, `R08`: `SessionTimer` com temporização em tempo real e encerramento exato em 300s.
  - `R02`: Treino guiado alimentando notas e histórico SRS.
  - `R03`, `R06`, `R07`: Sincronização concorrente, preservação de sessões com mesmo mtime e sumário sem truncamento de 20 sessões.
  - `R04`, `R10`: Alfabeto pessoal transacional sem variantes sintéticas.
  - `R05`: Estilo pessoal compilado e refletido no motor de estilos.
  - `R09`: Descritor aberto e flush/sync em todos os fluxos de persistência.
  - `R11`: Convenção caligráfica angular canônica e thresholds monotônicos de pauta.
  - `R12`: Dual Replay em modo `REAL_TIME` sem distorção cinemática.
  - `R13`: Renderização de traços unitários (pontos e acentos) no comparador.
  - `R14`: Avaliação ponto-a-segmento invariante à taxa de amostragem.
  - `R16`: Exportação de página preservando aspect ratio do canvas.
  - `R18`: Medição de velocidade real de traço em px/ms e ganho motor.
- **A01–A22 (Onda 0 & 1):**
  - `A03`: Exclusão mútua (`pagePersistenceMutex`) para salvar cadernos.
  - `A04`, `A05`: Borracha vetorial com interpolação de segmentos entre eventos de movimento.
  - `A07`, `A08`, `R15`: Preservação de zero físico de sensores e amostras em `POINTER_UP`.
  - `A09`: Falha limpa em export sem simulações falsas.
  - `A20`: Proteção contra gestos de borda (`EdgeGestureExclusionHelper`).
  - `A20`, `A21` (Hardware S25 Ultra): Mantidos transparentemente como `PENDENTE_DISPOSITIVO` até validação em aparelho físico real.
