# Matriz de Rotas, Ações e Handlers do Produto — Scribe

Esta matriz documenta o mapeamento entre controles da interface, rotas, argumentos transmitidos, repositórios subjacentes e testes de validação.

| ID | Origem (Tela) | Controle / Ação | Destino | Argumentos | Handler / ViewModel | Repositório | Teste de Aceite | Estado |
|---|---|---|---|---|---|---|---|---|
| **NAV-01** | `NavigationBar` | Tab "Caderno" | `NotebookLibraryScreen` / `NotebookPracticeScreen` | — | `currentTab = NOTEBOOK` | `NotebookRepository` | `AuditFixAcceptanceTest` | PRONTO |
| **NAV-02** | `NavigationBar` | Tab "Praticar" | `GuidedPracticeScreen` (Hub M4 / Canvas) | `lessonId`, `exerciseId` | `currentTab = GUIDED_PRACTICE` | `CurriculumCatalog`, `ReferenceGlyphCatalog` | `AuditFixAcceptanceTest` | PRONTO |
| **NAV-03** | `NavigationBar` | Tab "Evolução" | `EvolutionScreen` | — | `currentTab = EVOLUTION` | `PracticeAttemptRepository` | `AuditFixAcceptanceTest` | PRONTO |
| **NAV-04** | `NavigationBar` | Tab "Mais" | `ExpansionsScreen` | `activeTab` | `currentTab = EXPANSIONS` | `ScribeBackupManager`, `TeacherRepository` | `AuditFixAcceptanceTest` | PRONTO |
| **BK-01** | `NotebookLibraryScreen` | Botão "Novo Caderno" | Modal Criação de Caderno | `title`, `coverStyle`, `guidelineType` | `notebookViewModel.createNewNotebook(...)` | `NotebookRepository` | `NotebookRepositoryTest` | PRONTO |
| **BK-02** | `NotebookLibraryScreen` | Clique em Caderno | `NotebookPracticeScreen` | `notebookId` | `notebookViewModel.openNotebook(id)` | `NotebookRepository` | `NotebookRepositoryTest` | PRONTO |
| **BK-03** | `NotebookPracticeScreen` | Botão "Voltar à Biblioteca" | `NotebookLibraryScreen` | — | `notebookViewModel.closeNotebook()` | `NotebookRepository` | `NotebookRepositoryTest` | PRONTO |
| **BK-04** | `NotebookPracticeScreen` | Botão "Páginas" | `NotebookPagesDialog` | `pageIndex` | `showPagesDialog = true` | `NotebookRepository` | `NotebookRepositoryTest` | PRONTO |
| **BK-05** | `NotebookPagesDialog` | Botão "Nova Página" | `NotebookPracticeScreen` | `newPageId` | `notebookViewModel.addNewPage()` | `NotebookRepository` | `NotebookRepositoryTest` | PRONTO |
| **GD-01** | `GuidedPracticeScreen` | Botão "Aulas (M4)" / Hub | `LearningHubScreen` | — | `showLearningHub = true` | `CurriculumCatalog` | `AuditFixAcceptanceTest` | PRONTO |
| **GD-02** | `LearningHubScreen` | Botão "Iniciar Aula" | `GuidedPracticeScreen` | `lesson`, `duration` | `learningViewModel.startSession(...)` | `SessionTimer`, `PracticeAttemptRepository` | `AuditFixAcceptanceTest` | PRONTO |
| **GD-03** | `GuidedPracticeScreen` | Traço avaliado no Canvas | `LearningViewModel` / `PracticeAttemptRepository` | `scorePercent`, `targetSlant` | `onAttemptEvaluated(glyphId, score)` | `PracticeAttemptRepository`, `LearningHistoryRepository` | `AuditFixAcceptanceTest.r02` | PRONTO |
| **GD-04** | `GeometricFeedbackSheet` | Botão "Salvar no Meu Alfabeto" | `LocalPersonalAlphabetRepository` | `symbol`, `variantStrokes` | `guidedPracticeViewModel.saveAttemptToPersonalAlphabet()` | `LocalPersonalAlphabetRepository` | `AuditFixAcceptanceTest.r04` | PRONTO |
| **EV-01** | `EvolutionScreen` | Tab "Replay Duplo" | Player de Replay Lado a Lado | `attempt1`, `attempt2`, `speed` | `evolutionViewModel.setDualReplaySpeed(...)` | `DualReplayEngine` | `AuditFixAcceptanceTest.r12` | PRONTO |
| **EV-02** | `EvolutionScreen` | Tab "Sobreposição" | Canvas de Overlay | `alphaCrossfade` | `evolutionViewModel.setOverlayAlpha(...)` | `PracticeAttemptRepository` | `AuditFixAcceptanceTest` | PRONTO |
| **EX-01** | `ExpansionsScreen` | Tab "Diagnóstico" | `TeacherScreen` | — | `expansionsViewModel.selectTab(TEACHER)` | `TeacherRepository` | `MotorDiagnosticEngineTest` | PRONTO |
| **EX-02** | `TeacherScreen` | Botão "Praticar Exercício Recomendado" | `GuidedPracticeScreen` | `exerciseId`, `recommendedSlant` | `onNavigateToPractice(exerciseId)` | `ReferenceGlyphCatalog` | `AuditFixAcceptanceTest.s18` | PRONTO |
| **EX-03** | `ExpansionsScreen` | Tab "Textos" | Catálogo de Passagens Longas | `selectedPassage` | `expansionsViewModel.selectPassage(passage)` | `PassageCatalog` | `PassageCatalogTest` | PRONTO |
| **EX-04** | `ExpansionsScreen` | Botão "Praticar Texto no Caderno" | `NotebookPracticeScreen` | `passageText`, `recommendedStyleId` | `onNavigateToPracticeWithText(passage)` | `NotebookRepository`, `StyleEngine` | `AuditFixAcceptanceTest` | PRONTO |
| **EX-05** | `ExpansionsScreen` | Tab "Backup" | Painel de Backup / Restauração | `.scribepack` | `expansionsViewModel.exportBackup(...)` / `importBackup(...)` | `ScribeBackupManager` | `AuditFixAcceptanceTest.s01_s04` | PRONTO |
| **EX-06** | `ExpansionsScreen` | Tab "Caneta S Pen" | Seletor de Curvas de Pressão | `curveType` | `expansionsViewModel.setPressureCurve(curve)` | `SharedPreferences`, `SmoothedReferenceRenderer` | `SmoothedReferenceRendererTest` | PRONTO |
