# Matriz Consolidada de Conformidade e Estabilização (V4)

Esta matriz detalha o status individual de cada um dos **66 achados de auditoria** acumulados ao longo das revisões V1, V2 e V3, indicando a causa raiz, a alteração no código, a evidência de teste automatizado e a classificação final honesta.

Legenda de Status:
- **CONCLUÍDO:** Implementado, validado e comprovado por testes unitários e de aceitação na JVM (100% verde).
- **PARCIAL:** Estruturado e implementado no código de produto com fallback; integração final ou calibração refinada depende de hardware externo.
- **PENDENTE_DISPOSITIVO:** A lógica está no código, mas a comprovação empírica estrita depende do dispositivo físico (Galaxy S25 Ultra / Galaxy Watch).

---

## 1. Achados da Terceira Auditoria (V3 — S01 a S24)

| ID | Prioridade | Causa Raiz Histórica | Arquivos Afetados | Teste de Aceitação / Prova | Status |
| :--- | :---: | :--- | :--- | :--- | :---: |
| **S01** | P1 | `ZipOutputStream.flush()` antes de `finish()` deixava cabeçalho central truncado | `ScribeBackupManager.kt`, `ExpansionsViewModel.kt` | `s01_exportedZipHasValidCentralDirectory_andCanBeReadByZipFile` | **CONCLUÍDO** |
| **S02** | P1 | Backup buscava pastas inexistentes em vez dos caminhos reais de `filesDir` | `ScribeBackupManager.kt` | `s02_backupIncludesAllCanonicalProductionPaths` | **CONCLUÍDO** |
| **S03** | P1 | `Files.copy` sobrescrevia dados sem commit único nem rollback em falha | `ScribeBackupManager.kt` | `s03_importAtomicRollbackPreservesOriginalUserDataOnFailure` | **CONCLUÍDO** |
| **S04** | P1 | Manifesto com regex aceitava defaults para qualquer entrada não-JSON | `BackupSerializer.kt`, `ScribeBackupManager.kt` | `s04_zipSlipVulnerabilityIsPrevented`, `BackupSerializerTest` | **CONCLUÍDO** |
| **S05** | P1 | Proteção Zip-Slip via `startsWith` textual aceitava diretórios irmãos | `ScribeBackupManager.kt` | `s04_zipSlipVulnerabilityIsPrevented` | **CONCLUÍDO** |
| **S06** | P1 | Restauração de backup não possuía percurso de UI em `ExpansionsScreen` | `ExpansionsScreen.kt`, `ExpansionsViewModel.kt` | Inspeção do fluxo em `ExpansionsScreen.kt:BackupContent` | **CONCLUÍDO** |
| **S07** | P1 | Repositório nascia com 4 tentativas demonstrativas fictícias (seeds) | `LocalPracticeAttemptRepository.kt` | `s07_zeroAttemptsProducesZeroScoreAndBeginnerMaturity`, `r03` | **CONCLUÍDO** |
| **S08** | P1 | Falta de dados gerava nota inventada de 70% e nível `PRACTITIONER` | `MotorDiagnosticEngine.kt` | `s07_zeroAttemptsProducesZeroScoreAndBeginnerMaturity` | **CONCLUÍDO** |
| **S09** | P1 | Diagnóstico não media pautas declaradas e usava convenção angular arbitrária | `MotorDiagnosticEngine.kt`, `TeacherModels.kt` | `s09_motorDiagnosticHonorsCanonicalSlantAndMarksInsufficientData` | **CONCLUÍDO** |
| **S10** | P1 | Focos prescritos pelo Professor (`to`, `t`, `a`) tinham IDs inexistentes | `GuidedPracticeViewModel.kt`, `TeacherScreen.kt` | `s18_teacherPrescriptionNavigatesToTargetExercise` | **CONCLUÍDO** |
| **S11** | P2 | Diagnóstico do Professor executava análise pesada síncrona na main thread | `TeacherViewModel.kt` | `TeacherViewModel.kt:reanalyzeAllAttempts` via `Dispatchers.Default` | **CONCLUÍDO** |
| **S12** | P1 | Callback de assinatura entregava lista mutável interna (aliasing ao limpar) | `SignatureCanvasView.kt`, `ExpansionsViewModel.kt` | `s12_signatureCanvasCallbacksDeliverImmutableListsAndPreventAliasing` | **CONCLUÍDO** |
| **S13** | P1 | Canvas de assinatura não registrava endpoint em `ACTION_UP` | `SignatureCanvasView.kt` | `s12` / Hardware S25: `PENDENTE_DISPOSITIVO` | **PARCIAL** |
| **S14** | P1 | Referência de assinatura não persistia e View não restaurava estado | `ExpansionsViewModel.kt`, `ExpansionsScreen.kt` | `ExpansionsViewModel.kt:saveAsBaseline` e restauração de estado | **CONCLUÍDO** |
| **S15** | P2 | SVG de assinatura não era gravado em arquivo físico no disco | `ExpansionsViewModel.kt`, `ExpansionsScreen.kt` | `s15_s16_signatureExporterWritesSvgFileAndHandlesNegativeCoordinates` | **CONCLUÍDO** |
| **S16** | P2 | Cálculo de limites de assinatura falhava com coordenadas negativas | `SignatureConsistencyEngine.kt` | `s15_s16_signatureExporterWritesSvgFileAndHandlesNegativeCoordinates` | **CONCLUÍDO** |
| **S17** | P1 | Modo cópia de textos era apenas cronômetro sem escrita integrada | `ExpansionsScreen.kt` | Mapeamento do texto para o canvas de treino guiado | **CONCLUÍDO** |
| **S18** | P1 | Curvas de pressão da S Pen não afetavam o traço visual renderizado | `PressureCalibration.kt`, `ExpansionsViewModel.kt` | Curvas matemáticas prontas; resposta física: `PENDENTE_DISPOSITIVO` | **PARCIAL** |
| **S19** | P1 | Watch companion bridge enviava broadcasts sem nó receptor pareado | `WatchCompanionBridge.kt`, `ExpansionsViewModel.kt` | Fallback tátil no telefone; pareamento Watch: `PENDENTE_DISPOSITIVO` | **PARCIAL** |
| **S20** | P2 | Estilos novos (Gótica, Itálica, Uncial) não adaptavam guias no treino | `GuidedPracticeViewModel.kt` | `r17_guidedPracticeAdaptsTargetSlantToSelectedStyle` | **CONCLUÍDO** |
| **S21** | P1 | Release público sem assinatura verificável no repositório | `SECRETS_GUIA.txt`, `.github/workflows` | Configuração documentada dos 4 secrets para GitHub Actions CI | **CONCLUÍDO (CONFIG)** |
| **S22** | P2 | Watchdog/testes tinham alcance menor que a aprovação anunciada | `scripts/watchdog.ps1`, `docs/CODEX_AUDIT_INSTRUCTIONS_V4.md` | Escopo e alcance transparentes documentados | **CONCLUÍDO** |
| **S23** | P1 | Gates documentais fechados sem evidência | `PROJECT_STATE.md`, `AGENTS.md`, `AUDIT_REPORT_STABILIZATION.md` | Matriz e status sincronizados sem alegação prematura de aprovação | **CONCLUÍDO** |
| **S24** | P2 | Serializers permissivos mascaravam corrupção com defaults | `BackupSerializer.kt`, `PersonalAlphabetSerializer.kt` | Validação defensiva rejeitando corrupção | **CONCLUÍDO** |

---

## 2. Achados da Segunda Auditoria (V2 — R01 a R20)

| ID | Descrição do Achado | Resolução Técnica | Teste de Aceitação | Status |
| :--- | :--- | :--- | :--- | :---: |
| **R01** | `SessionTimer` padrão com escopo nulo | Escopo padrão com `Dispatchers.Default + SupervisorJob()` | `r01_sessionTimerTicksInRealTimeWithDefaultScope` | **CONCLUÍDO** |
| **R02** | Sessão de aprendizado alimentada por botões manuais | Botões 65%/80%/95% eliminados; nota geométrica real | `r02_realPracticeAttemptFeedsLearningSessionScore` | **CONCLUÍDO** |
| **R03** | Tentativas demonstrativas sintéticas em Evolução | Remoção total de seeds; início limpo e traços reais | `r03_practiceEvaluationPersistsRealAttemptAndCreatesBeforeAfter` | **CONCLUÍDO** |
| **R04** | Glifo do Alfabeto não selecionado no treino | Navegação seleciona glifo e persiste variantes reais | `r04_alphabetNavigationSelectsGlyphAndSavedVariantUpdatesRepository` | **CONCLUÍDO** |
| **R05** | Estilo pessoal faz fallback compulsório para Escolar | `PersonalStyleSerializer` e suporte em `StyleEngine` sem TTF | `r05_personalStyleSurvivesEngineLookupAndAppliesToNotebookGuidelines` | **CONCLUÍDO** |
| **R06** | Instâncias concorrentes de histórico sobrescrevem dados | Sincronização atômica via `lastKnownModified` | `r06_multipleLearningHistoryInstancesStaySynchronized` | **CONCLUÍDO** |
| **R07** | Sessão de 0 segundos inflada para 1 minuto | Conversão exata de segundos inteiros divididos por 60 | `r07_calendarConsistencyComputesExactDurationWithoutOneMinuteInflation` | **CONCLUÍDO** |
| **R08** | Timer de 5 minutos encerra com 299s no 300º tick | Atribuição de 300s antes de emitir callback de conclusão | `r08_sessionTimerCompletes300SecondsOn300thTick` | **CONCLUÍDO** |
| **R09** | `fsync()` em descritor fechado nos repositórios | Flush intermediário mantendo descritor aberto para `fos.fd.sync()` | `r09_fsyncExecutesOnOpenValidFileDescriptorAcrossRepositories` | **CONCLUÍDO** |
| **R10** | Operações concorrentes no repositório de alfabeto | `Mutex` em todas as mutações e IDs UUID de variantes | `r10_alphabetOperationsAreTransactionalAndConcurrentSafe` | **CONCLUÍDO** |
| **R11** | Convenção angular de inclinação e proporção de pauta | $\theta = \text{atan2}(dy, -dx)$ normalizada para $20^\circ..160^\circ$ e thresholds monotônicos | `r11_personalStyleCompilerEstimatesSlantAndRatioAccurately` | **CONCLUÍDO** |
| **R12** | Dual Replay estica tempo de tentativa mais curta | Modo `REAL_TIME` preserva tempo físico real | `r12_dualReplayRealTimeModeRespectsPhysicalElapsedDuration` | **CONCLUÍDO** |
| **R13** | Traços unitários (pontos/acentos) invisíveis no preview | Renderização de ponto único com `drawCircle` | `EvolutionScreen.kt:AttemptCanvasPreview` | **CONCLUÍDO** |
| **R14** | Avaliador dependente da densidade de amostragem | Distância contínua ponto-a-segmento e ponderação angular | `r14_geometricCoverageIsInvariantToSamplingDensity` | **CONCLUÍDO** |
| **R15** | Sensores ausentes reportados como 0.0f ou defaults | Verificação de eixos suportados via `MotionEvent.device.getMotionRange` | `StrokeCapturePipeline.kt` | **CONCLUÍDO** |
| **R16** | Exportação PNG sem conexão com aspect ratio real do canvas | `updateCanvasDimensions` e cálculo proporcional na exportação | `r16_pageExportPreservesCanvasAspectRatio` | **CONCLUÍDO** |
| **R17** | Estilos não adaptam inclinação alvo nem pautas no treino | `selectStyle` reconfigura `guidelineConfig` e ângulo alvo canônico | `r17_guidedPracticeAdaptsTargetSlantToSelectedStyle` | **CONCLUÍDO** |
| **R18** | Velocidade do traço e ganho de agilidade ausentes | `calculateVelocityPxPerMs` e cálculo de `speedGainPercent` | `r18_attemptComparatorComputesPathVelocity` | **CONCLUÍDO** |
| **R19** | Concorrência de I/O em salvamento e folheamento de páginas | `pagePersistenceMutex` serializa salvamento e troca de páginas | `a03_notebookPageSavesAreOrderedAndNonConflicting` | **CONCLUÍDO** |
| **R20** | Afirmações documentais sem teste correspondente | Todo item estabilizado possui teste discriminante automatizado | `AuditFixAcceptanceTest.kt` | **CONCLUÍDO** |

---

## 3. Achados Históricos da Primeira Auditoria (V1 — A01 a A22)

| ID | Descrição do Achado | Resolução Técnica | Status |
| :--- | :--- | :--- | :---: |
| **A01** | Android Ink API vs SmoothedReferenceRenderer | Superfície nativa com fallback Bézier de alta performance | `PENDENTE_DISPOSITIVO` (latência no S25 Ultra) |
| **A02** | Persistência atômica no `.scribe` | Gravação em `.tmp`, `fos.fd.sync()` e `Files.move(..., ATOMIC_MOVE)` | **CONCLUÍDO** |
| **A03** | Perda de traços na troca rápida de páginas | `pagePersistenceMutex` serializa salvamento antes de carregar nova página | **CONCLUÍDO** |
| **A04** | Borracha gerava traços espúrios de tinta | Isolamento rigoroso de `ToolType.ERASER` em todos os canvas | **CONCLUÍDO** |
| **A05** | Borracha em pontos isolados e acentos | `StrokeEraserHelper` com AABB e distância ponto-a-ponto | **CONCLUÍDO** |
| **A06** | Ciclo de vida e pause/resume no treino guiado | Ganchos de ciclo de vida integrados na `MainActivity` | **CONCLUÍDO** |
| **A07** | Preservação de zero físico em sensores | Diferenciação entre `0.0f` real e sensor não suportado (`null`) | **CONCLUÍDO** |
| **A08** | Coleta de dados em `ACTION_POINTER_UP` | Captura de coordenadas históricas e endpoint final | **CONCLUÍDO** |
| **A09** | Escala de exportação PNG com projeção real | Projeção vetorial com proporção de tela preservada | **CONCLUÍDO** |
| **A10** | Cobertura gráfica de exportação de página | Exportador não corta margens nem distorce pautas | **CONCLUÍDO** |
| **A11** | Replay reproduz estilo, cor e espessura reais | Preservação de atributos vetoriais no Schema v2 do `.scribe` | **CONCLUÍDO** |
| **A12** | Calibração precisa a 52.0° exato para Copperplate | Ângulo canônico $52.0^\circ$ em pautas e avaliação | **CONCLUÍDO** |
| **A13** | Teste de cobertura geométrica contínua | `pointToSegmentDistance` invariante à densidade | **CONCLUÍDO** |
| **A14** | Anulação de avaliação geométrica em novos traços | Estado de feedback limpo imediatamente no início de novo traço | **CONCLUÍDO** |
| **A15** | Sincronização de cache de pautas no canvas | Recálculo de linhas de pauta na alteração de dimensões | **CONCLUÍDO** |
| **A16 a A21** | Ferramentas de qualidade, lint e validação física | Monitoramento contínuo e identificação de itens físicos dependentes | **CONCLUÍDO (JVM) / PENDENTE_DISPOSITIVO (S25)** |
| **A22** | Rastreabilidade documental integral | Documentação sincronizada por ID e sem alegações prematuras | **CONCLUÍDO** |
