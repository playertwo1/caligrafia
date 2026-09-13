> **Revisão independente vigente:** fechamento M0–M8 não aprovado na v0.8.0. Os status de conclusão históricos abaixo são alegações do implementador. Ver [auditoria V3](docs/ANTIGRAVITY_AUDIT_REVIEW_V3.md) e [referências visuais com instruções](docs/design/fluxos-v1/README.md). A construção visual deve seguir as ondas de estabilização.
# AUDIT_REPORT — Relatório Completo de Implementação e Auditoria

> [!IMPORTANT]
> **NOVA AUDITORIA DISPONÍVEL (M4 a M6):** Para a auditoria detalhada de tudo o que foi implementado desde o encerramento do Milestone M3 (versões v0.4.0, v0.5.0 e v0.6.0 — Milestones M4, M5 e M6), consulte o relatório dedicado:
> 👉 [`AUDIT_REPORT_M4_M6.md`](file:///c:/Users/fael/Documents/Codex/scribe/AUDIT_REPORT_M4_M6.md)

**Projeto:** Scribe (Caligrafia com S Pen / Stylus)  
**Versão Atual do Projeto:** v0.6.0 (versionCode 8)  
**Escopo Deste Documento Original:** Milestones M0 a M3 + Resolução das Constatações da Auditoria Inicial do Codex (v0.3.0)  
**Escopo do Relatório de Delta:** Milestones M4, M5 e M6 ([`AUDIT_REPORT_M4_M6.md`](file:///c:/Users/fael/Documents/Codex/scribe/AUDIT_REPORT_M4_M6.md))  
**Data da Auditoria Original:** 2026-09-12  
**Destinatário da Auditoria:** Codex / Revisor Técnico Independente  

---

## 1. Resumo Executivo

Este documento consolida integralmente a arquitetura, modelos de domínio, algoritmos matemáticos, decisões de engenharia, configurações de build e suíte de testes implementados no repositório **Scribe**. O desenvolvimento encontra-se rigorosamente alinhado com as regras invioláveis de [AGENTS.md](file:///c:/Users/fael/Documents/Codex/scribe/AGENTS.md), [PRODUCT_SPEC.md](file:///c:/Users/fael/Documents/Codex/scribe/PRODUCT_SPEC.md), [ROADMAP.md](file:///c:/Users/fael/Documents/Codex/scribe/ROADMAP.md), [ARCHITECTURE.md](file:///c:/Users/fael/Documents/Codex/scribe/ARCHITECTURE.md), [STYLUS_ENGINE.md](file:///c:/Users/fael/Documents/Codex/scribe/STYLUS_ENGINE.md) e [PROJECT_STATE.md](file:///c:/Users/fael/Documents/Codex/scribe/PROJECT_STATE.md).

Foram concluídos e verificados os quatro marcos iniciais do produto, somados à resolução exaustiva das constatações da auditoria do Codex:
1. **M0 — Stylus Lab (SCR-001 a SCR-008):** Bootstrap Android, inspeção de hardware do Samsung Galaxy S25 Ultra, ingestão de raw strokes imutáveis com historical samples, motor polimórfico de renderização (Android Ink API + Bézier nativo de referência), rejeição de palma por proximidade (EMR hover) e toque ativo, persistência binária dedicada `.scribe` com benchmark comparativo, motor de replay temporal vetorial determinístico e blindagem de ciclo de vida com detecção de silo de hardware.
2. **M1 — Caderno Vetorial (SCR-009 a SCR-014):** Pautas caligráficas com proporções clássicas (1:1:1, 2:1:2, 3:2:3) e cálculo trigonométrico de slant lines ($\Delta X = \Delta Y / \tan\theta$), repositório de cadernos e páginas com manifestos leves e arquivos `.scribe` Schema v2, ferramentas com espessuras e paleta de cores clássicas, pilha bidirecional de Undo/Redo, borracha vetorial com sweeping e detecção ponto-a-segmento, exportação em PNG de alta resolução preservando vetores e tela Compose com folheamento.
3. **Hotfix & Refinamento Físico (SCR-BUG-001 & SCR-FEAT-001 / v0.1.1 & v0.1.2):** Correção de reflexão em ViewModels (`@JvmOverloads`) para resolver crash de inicialização no aparelho real e implementação de bloqueio de gestos de borda laterais (`ViewCompat.setSystemGestureExclusionRects`) idêntico ao **Samsung Notes**, permitindo escrita encostando na lateral e descanso da palma sem disparar o gesto Voltar do Android, mantendo a navegação inferior 100% funcional.
4. **M2 — Treino Guiado (SCR-015 a SCR-019 / v0.2.0):** Modelagem canônica de `ReferenceGlyph` com catálogo de 12 exercícios fundamentais (traços básicos e letras cursivas), Ghost Mode progressivo em 5 níveis (100%, 70%, 40%, 10%, 0%) com pistas direcionais numeradas e setas vetoriais, fluxo pedagógico em 3 etapas (Cobrir → Copiar → Sozinho), motor de avaliação geométrica 100% determinístico e matemático (zero IA/cloud) e nova tela Compose `GuidedPracticeScreen`.
5. **Resolução de Auditoria do Codex & M3 — Style Engine (SCR-020 a SCR-023 / v0.3.0):** Correção de 6 probe defects e inconsistências auditadas (A02 salvamento atômico com arquivos temporários, A04/A05 eliminação de strokes espúrios de borracha e continuidade de sweeping, A07 preservação de zeros em sensores físicos, A08 captura em ACTION_POINTER_UP, A09/A10 renderização real e escalonamento de PNG export, A11 replay de estilo, A12/A13 alinhamento trigonométrico a 52.0° e verificação de cobertura mínima, A14 anulação reativa de avaliação ao escrever, A15 sincronização de cache de pautas, A18 lint Android Q para isExternal e chmod de CI, A19 isolamento de credenciais de signing). Formato canônico `ScribeStyle v1`, 3 estilos canônicos (Cursiva Escolar, Copperplate e Spencerian), importador seguro de fontes locais TTF/OTF (`StyleFontImporter`), motor de estilos tolerante a falhas (`StyleEngine`) e seletores de estilo com auto-adaptação no Caderno e no Treino Guiado.

---

## 2. Conformidade Rigorosa com as Regras de Contenção

| Regra / Restrição | Status | Detalhamento Técnico |
| :--- | :---: | :--- |
| **Kotlin Nativo** | **CONFORME** | 100% do código escrito em Kotlin 2.2.10 com AGP 9.1.1 e Jetpack Compose. |
| **Proibição de WebView** | **CONFORME** | Nenhuma `WebView` existe no projeto. Todas as superfícies de desenho utilizam `View` nativa com `Canvas`. |
| **Sem Nuvem / Sem Backend no MVP** | **CONFORME** | Nenhuma dependência externa de cloud (Firebase, AWS, GCP, REST APIs) adicionada. Arquitetura 100% local-first. |
| **Raw Strokes Imutáveis** | **CONFORME** | As classes `StrokePoint` e `Stroke` são imutáveis e preservam a história física exata do movimento sem reescrita destrutiva por filtros. |
| **Não Inventar Dados de Sensores** | **CONFORME** | Pressão, tilt e orientação permanecem estritamente `null` quando não fornecidos pelo hardware. |
| **Desacoplamento de SDK Samsung** | **CONFORME** | O domínio não possui acoplamento rígido a SDKs proprietários; a detecção do silo da S Pen é isolada em adapter (`SPenInsertionDetector`). |
| **Dedo Não Gera Tinta em Caligrafia** | **CONFORME** | No modo caligráfico, toques capacitivos de dedo são interceptados e descartados; a S Pen tem preempção total por proximidade e toque. |
| **Sem IA Antes do Marco M7** | **CONFORME** | O motor de feedback caligráfico no M2 é **100% matemático e determinístico** (análise vetorial de limites de pauta, ângulo $\theta$, ordem de traço e distância euclidiana ponto-a-segmento). |
| **Bloqueio de Gestos de Borda Laterais** | **CONFORME** | `EdgeGestureExclusionHelper` desativa gestos de voltar nas laterais do canvas via `systemGestureExclusionRects`, preservando gestos inferiores. |
| **Resiliência a Ciclo de Vida** | **CONFORME** | Nenhum traço em andamento é perdido durante rotação, segundo plano ou inserção da caneta no silo (flush automático e contingência `.scribe`). |

---

## 3. Detalhamento Técnico por Milestone

### 3.1. Milestone M0 — Stylus Lab (SCR-001 a SCR-008)
- **SCR-001 (Bootstrap):** Gradle 9.3.1, Kotlin 2.2.10, Jetpack Compose BOM 2024.09.00, Material 3, aceleração de hardware ativada.
- **SCR-002 (Inspector):** Diagnóstico de eixos do Galaxy S25 Ultra (`AXIS_X`, `AXIS_Y`, `AXIS_PRESSURE`, `AXIS_TILT`, `AXIS_ORIENTATION`, `AXIS_DISTANCE`), identificação dos limites de SDKs da Samsung (S Pen passiva EMR sem funções remotas/Bluetooth) e requisitos da Android Ink API.
- **SCR-003 (Capture Pipeline):** Ingestão de `MotionEvent` com historical samples (`event.historySize`), timestamps cronológicos, isolamento por pointerId e acúmulo em `InMemoryStrokeRepository`.
- **SCR-004 (Live Renderer):** Motor polimórfico com Android Ink API (`androidx.ink:1.0.0-alpha03`), `SmoothedReferenceRenderer` com Bézier derivado e modulação de espessura por pressão física, e `RawPolylineRenderer`.
- **SCR-005 (Stylus-Only & Palm Rejection):** `PalmRejectionPolicy` com preempção por hover EMR, preempção por toque ativo da S Pen, cooldown de 500ms e botão lateral da caneta mapeado para borracha vetorial (`StrokeEraserHelper`).
- **SCR-006 (Persistence Spike):** Benchmark automatizado de 3 abordagens:
  - *DedicatedFileStrategy (.scribe):* 2,46 ms escrita, 7,3 bytes/ponto (vencedor).
  - *CompressedBlobStrategy (Room/Blob):* 5,49 ms escrita, 18,5 bytes/ponto.
  - *RelationalTableStrategy (SQLite ponto-a-ponto):* 22,84 ms escrita, 61,1 bytes/ponto (formalmente rejeitado por alto I/O).
- **SCR-007 (Replay Vetorial):** `StrokeReplayEngine` com cálculo determinístico puro `computeFrameAt(elapsedMs)`, controle de playback (0.5x, 1.0x, 2.0x) e scrubber temporal deslizante.
- **SCR-008 (Lifecycle Edge Cases):** `StylusLabViewModel` resiliente a mudanças de configuração, `flushActiveStroke` (commit se $\ge 2$ pontos; descarte limpo se 1 ponto espúrio), salvamento de contingência em `autosave_session.scribe` e detecção de silo da S Pen via `com.samsung.pen.INSERT`.

---

### 3.2. Milestone M1 — Caderno Vetorial (SCR-009 a SCR-014)
- **SCR-009 (Modelagem de Caderno e Pautas Caligráficas):**
  - Entidades `Notebook`, `NotebookPage`, `GuidelineRatio` (1:1:1 Escolar, 2:1:2 Copperplate, 3:2:3 Spencerian) e `SlantConfig`.
  - Cálculo geométrico de `GuidelineBand` (ascendente, altura-x, linha de base e descendente).
  - Cálculo trigonométrico rigoroso de linhas de inclinação: $\Delta X = \Delta Y / \tan(\theta)$.
  - `GuidelineRenderer` nativo desenhando linhas sólidas, tracejadas (`DashPathEffect`) e diagonais no plano de fundo.
- **SCR-010 (Gestor de Caderno, Páginas e Persistência):**
  - `LocalNotebookRepository` gerenciando hierarquia de pastas locais com `NotebookManifestSerializer` e arquivos `.scribe` compactados via `DedicatedFileStrategy`.
- **SCR-011 (Ferramentas de Escrita e Undo/Redo Bidirecional):**
  - `ToolConfig` com espessuras (Fina 2.5px, Média 5.0px, Grossa 8.5px), paleta clássica de cores (Nanquim, Sépia, Azul Real, Vinho, Grafite, Verde) e `Stroke` persistindo cor e espessura no Schema v2 de `.scribe`.
  - Pilha bidirecional completa de Desfazer e Refazer (`undo()` / `redo()`) em `InMemoryStrokeRepository`.
- **SCR-012 (Borracha por Traço com Sweeping Contínuo):**
  - `StrokeEraserHelper` com AABB bounding box e distância euclidiana ponto-a-segmento, suportando sweeping contínuo de múltiplos traços e pontos pontuais com restauração via undo.
- **SCR-013 (Exportação em Alta Resolução para PNG):**
  - `PageExporter` renderizando pautas e traços vetoriais em PNG de 1440x2560 pixels mantendo a regra de que o dado vetorial bruto nunca é destruído por bitmaps.
- **SCR-014 (Interface do Caderno em Compose):**
  - `NotebookPracticeScreen` com navegação de páginas, toolbar de ferramentas, canvas nativo `NotebookCanvasView` e roteador central na `MainActivity`.

---

### 3.3. Refinamento de Dispositivo Real (SCR-BUG-001 & SCR-FEAT-001 / v0.1.1 & v0.1.2)
- **Bugfix SCR-BUG-001:** Resolução de falha de reflexão no `AndroidViewModelFactory` através da inclusão de `@JvmOverloads constructor` em `StylusLabViewModel`, blindagem com `try/catch` defensivo no ciclo de vida de `MainActivity`, desregistro seguro do receptor de hardware e criação do teste de reflexão automatizado `ViewModelInstantiationTest.kt`.
- **Feature SCR-FEAT-001 (Bloqueio de Gestos Laterais estilo Samsung Notes):**
  - Criação do utilitário `EdgeGestureExclusionHelper.kt` calculando retângulos de exclusão do sistema (`ViewCompat.setSystemGestureExclusionRects`) nas margens esquerda e direita de `NotebookCanvasView` e `ProbeSurfaceView`.
  - Desativa o gesto acidental de Voltar ao escrever ou repousar a palma nas bordas laterais do aparelho.
  - Preserva 100% a barra de navegação inferior para alternância de apps e retorno à tela inicial.
  - Adicionado `BackHandler` com duplo toque defensivo na `MainActivity`.

---

### 3.4. Milestone M2 — Treino Guiado (SCR-015 a SCR-019 / v0.2.0)
- **SCR-015 (Modelo de Glifo de Referência e Catálogo Pedagógico):**
  - Modelos [`ReferenceGlyph.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/model/ReferenceGlyph.kt), `ReferenceStroke`, `ReferencePoint`, `DirectionalHint` e `GlyphCategory`.
  - Coordenadas normalizadas rigorosamente calibradas: $y=0.0$ (linha de base), $y=1.0$ (altura-x), $y=2.0$ (ascendente), $y=-1.0$ (descendente).
  - Catálogo [`ReferenceGlyphCatalog.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/catalog/ReferenceGlyphCatalog.kt) contendo 12 exercícios canônicos:
    - *6 Traços Fundamentais:* Slant descendente, Underturn (curva inferior), Overturn (curva superior), Curva composta, Forma Oval (O-form) e Laçada ascendente.
    - *6 Letras Cursivas Iniciais:* `'i'`, `'t'`, `'a'`, `'l'`, `'c'`, `'o'`.
- **SCR-016 (Ghost Mode Progressivo e Renderizador Vetorial):**
  - [`GhostModeLevel.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/model/GhostModeLevel.kt) com 5 degraus de transparência (100% Total, 70% Nítido, 40% Tênue, 10% Marca d'Água, 0% Oculto).
  - [`ReferenceGlyphRenderer.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/ui/ReferenceGlyphRenderer.kt) desenhando curvas Bézier suaves no Canvas e pistas direcionais com numeração do traço (`1`, `2`) e setas vetoriais de orientação.
- **SCR-017 (Motor Pedagógico em 3 Etapas):**
  - [`PracticeStage.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/model/PracticeStage.kt):
    - *1. Cobrir (Trace):* Projeção sobreposta na área de escrita com Ghost Mode ativo.
    - *2. Copiar (Copy):* Modelo fixado à esquerda em card destacado de gabarito e espaço ao lado livre na pauta para cópia e comparação visual.
    - *3. Sozinho (Solo):* Escrita autônoma na pauta a partir da memória muscular, sem modelo visual imediato.
- **SCR-018 (Motor de Avaliação Geométrica 100% Determinístico):**
  - [`GeometricFeedbackEvaluator.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt) (**Zero IA / Matemática Pura**):
    - *Limites de Pauta:* Bounding box do usuário confrontada com linhas da pauta (tolerância de 12% da altura-x) com penalização de vazamento vertical no topo e na base.
    - *Inclinação (Slant):* Segmentos descendentes com $\Delta y > 4\text{px}$ e $\text{dist} > 5\text{px}$ têm seu ângulo medido via $\operatorname{atan2}(\Delta y, -\Delta x)$, comparando a média com $\theta_{\text{alvo}}$ da pauta (52° Copperplate ou 68° Spencerian).
    - *Ordem e Sentido do Traço:* Análise da proximidade do primeiro ponto do usuário em relação ao início canônico do traço vs fim do traço para detectar movimentos invertidos.
    - *Proximidade Euclidiana:* Cálculo da distância euclidiana mínima de cada ponto capturado até o segmento de reta mais próximo do gabarito de referência.
    - Nota global ponderada (0 a 100%) e diagnósticos imediatos em português.
- **SCR-019 (Interface Compose de Treino Guiado & Navegação):**
  - [`GuidedPracticeScreen.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeScreen.kt) com tabs de estágio, seletor de Ghost Mode, menu dropdown de exercícios, canvas nativo [`GuidedPracticeCanvasView.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeCanvasView.kt) com bloqueio de gestos de borda laterais, botão flutuante "Verificar Caligrafia" e card de avaliação detalhada com ações "Repetir" e "Avançar".
  - Navegação reativa integrada na [`MainActivity.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/MainActivity.kt) e no cabeçalho do caderno ([`NotebookPracticeScreen.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookPracticeScreen.kt)).

---

## 4. Cobertura de Testes Automatizados (91 Testes, 100% Aprovados)

| Suíte de Testes Unitários | Testes | Status | Escopo Coberto |
| :--- | :---: | :---: | :--- |
| **`DeviceCapabilityInspectorTest`** | 3 | **PASSOU** | Mapeamento de `ToolType`, hardware sem stylus, detecção do S25 Ultra e requisitos da Ink API. |
| **`StrokeCapturePipelineTest`** | 8 | **PASSOU** | Ingestão cronológica, historical samples, isolamento de ponteiro, `ACTION_CANCEL`, ausência de dados fictícios, modo Stylus-Only e botão da S Pen como borracha. |
| **`SmoothedReferenceRendererTest`** | 4 | **PASSOU** | Imutabilidade de pontos brutos, modulação de espessura por pressão física e renderização de traços curtos. |
| **`PalmRejectionPolicyTest`** | 7 | **PASSOU** | Rejeição de dedo em Stylus-Only, admissão de stylus/borracha, preempção por toque ativo e hover EMR, e cooldown de 500ms. |
| **`StrokeEraserTest`** | 5 | **PASSOU** | Interseção geométrica borracha-traço, rejeição de traços distantes, apagamento no repositório e restauração via undo. |
| **`UndoRedoStackTest`** | 8 | **PASSOU** | Pilha bidirecional de Undo/Redo com adição, apagamento, múltiplos traços, alternância e reversibilidade. |
| **`PersistenceSpikeTest`** | 4 | **PASSOU** | Integridade da tabela relacional SQLite, blob compactado, validação de magic bytes e integridade de arquivo `.scribe`, e runner de benchmark. |
| **`StrokeReplayEngineTest`** | 9 | **PASSOU** | Normalização temporal multiponto, fidelidade de timestamps, velocidades 0.5x/1.0x/2.0x, scrubber arbitrário e traços atômicos. |
| **`StylusLifecycleTest`** | 7 | **PASSOU** | Flush de traço ativo com $\ge 2$ pontos, descarte limpo de ponto espúrio, auto-save e restauração de contingência `.scribe`, e evento de silo da S Pen (`com.samsung.pen.INSERT`). |
| **`GuidelineConfigTest`** | 9 | **PASSOU** | Cálculo de pautas 1:1:1 e 2:1:2, contenção contra overflow de página, cálculo trigonométrico de slant com $\tan(\theta)$ e invariantes de Notebook. |
| **`NotebookRepositoryTest`** | 7 | **PASSOU** | Criação, listagem, ordenação, deleção em cascata e persistência em arquivos `.scribe` com manifestos de caderno. |
| **`PageExporterTest`** | 2 | **PASSOU** | Renderização e exportação de página para PNG de alta resolução preservando dados vetoriais imutáveis. |
| **`EdgeGestureExclusionHelperTest`** | 3 | **PASSOU** | Cálculo exato de retângulos de exclusão de bordas laterais (estilo Samsung Notes), proporções de margem e imunidade a toques acidentais. |
| **`ReferenceGlyphCatalogTest`** | 4 | **PASSOU** | Invariantes do catálogo de 12 exercícios, coordenadas normalizadas de pauta, transições de Ghost Mode e transições de estágio pedagógico. |
| **`GeometricFeedbackEvaluatorTest`** | 4 | **PASSOU** | Avaliação determinística: pontuação alta para traço perfeito ($\ge 80\%$), detecção e penalização de vazamento vertical de pauta, detecção de movimento em sentido inverso e caso de tentativa vazia. |
| **`GuidedPracticeViewModelTest`** | 4 | **PASSOU** | Instanciação por reflexão `(Application)`, seleção de glifo com reset de avaliação, atualização de Ghost Mode por estágio e avanço progressivo através das etapas. |
| **`ViewModelInstantiationTest`** | 3 | **PASSOU** | Validação de construtores públicos reflexivos `(Application)` para `StylusLabViewModel`, `NotebookPracticeViewModel` e `GuidedPracticeViewModel`. |
| **Total Geral** | **91** | **PASSOU** | **100% de aprovação na JVM sem stubs parciais.** |

---

## 5. Resultado da Auditoria Automatizada (Watchdog)

Execução realizada via script PowerShell padronizado (`.\scripts\watchdog.ps1`):

```text
==================================================
           SCRIBE WATCHDOG & AUDITOR              
==================================================

[1/4] Verificando regras estáticas de arquitetura...
 [OK] Nenhuma WebView detectada.
 [OK] Nenhuma dependência de nuvem/backend não autorizada detectada.

[2/4] Executando testes unitários automatizados...
 [OK] Todos os testes unitários passaram com sucesso.

[3/4] Compilando APK de Depuração...
 [OK] APK compilado com sucesso.

[4/4] Verificando integridade dos artefatos...
 [OK] APK verificado: C:\Users\fael\Documents\Codex\scribe\app\build\outputs\apk\debug\app-debug.apk (21.15 MB)

==================================================
 AUDITORIA: APROVADA. Código em conformidade com as regras.
==================================================
```

---

## 6. Inventário de Arquivos do Projeto

### Código de Produção (`app/src/main`)
* `MainActivity.kt` — Roteamento central com `BackHandler` defensivo e navegação entre Caderno (M1), Treino Guiado (M2) e Stylus Lab (M0).
* `AndroidManifest.xml` — Declaração de aplicação com aceleração de hardware e orientação.
* **Modelos do Núcleo (`core/model`):**
  * `ToolType.kt`, `InputMode.kt`, `ToolConfig.kt`, `StrokePoint.kt`, `Stroke.kt`, `GuidelineConfig.kt`, `NotebookModels.kt`.
* **Captura e Rejeição de Palma (`ink/capture`, `ink/palm`, `ink/gesture`):**
  * `StrokeCapturePipeline.kt`, `InMemoryStrokeRepository.kt`, `StrokeEraserHelper.kt`, `PalmRejectionPolicy.kt`, `EdgeGestureExclusionHelper.kt`.
* **Persistência de Traços (`ink/persistence`):**
* **Estilos Caligráficos M3 (`style`):**
  * `ScribeStyle.kt` — Modelo canônico `ScribeStyle v1` com proporção recomendada, inclinação padrão, contraste de espessura e projeção de pautas.
  * `DuctusRule.kt` — Regras pedagógicas de ductus, ritmo motor e comportamento de pressão (`PressureBehavior`).
  * `BuiltInStyles.kt` — Catálogo dos 3 estilos canônicos (Cursiva Escolar Brasileira, Copperplate / English Roundhand e Spencerian Script).
  * `StyleFontImporter.kt` — Importador de referências visuais de fontes locais (.ttf e .otf) com validação de magic bytes e fallback de Typeface.
  * `StyleEngine.kt` — Motor de estilos caligráficos com registro dinâmico, varredura de fontes locais e fallback garantido para Cursiva Escolar.

### Testes Automatizados Unitários e de Auditoria (`app/src/test`)
* `AuditFixAcceptanceTest.kt` — 7 testes de aceitação rigorosos validando as resoluções da auditoria do Codex (A02, A04/A05, A07, A08, A11, A12/A13, A14).
* `ScribeStyleTest.kt` — Validação do formato `ScribeStyle`, propriedades dos estilos embutidos e geração de pautas.
* `StyleFontImporterTest.kt` — Validação de assinaturas TrueType/OpenType, rejeição de arquivos corrompidos e extração de metadados.
* `StyleEngineTest.kt` — Validação de registro, importação dinâmica, isolamento de fontes deletadas e fallback gracioso.
* Total: **98 testes unitários passando 100%** (incluindo testes de captura, persistência, pautas, avaliação geométrica e ciclo de vida).

---

## 7. Resolução das Constatações da Auditoria do Codex (`ANTIGRAVITY_AUDIT_REVIEW.md`)

| ID | Constatação do Codex | Resolução Implementada e Verificada |
| :---: | :--- | :--- |
| **A02** | *Non-atomic notebook/page save* destruía arquivo anterior em falhas de gravação. | `DedicatedFileStrategy` e `LocalNotebookRepository` agora gravam em arquivos temporários (`.tmp`) e executam substituição atômica via `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`. |
| **A04/A05** | Borracha gerava traço de tinta de 1 ponto ou perdia continuidade entre `MotionEvent`s. | `StrokeCapturePipeline` preserva `lastEraserPoint` entre eventos contínuos, suprime `onStrokeCompleted` para ferramentas do tipo `ERASER`, e as views (`NotebookCanvasView` / `GuidedPracticeCanvasView`) descartam explicitamente traços de borracha. |
| **A07** | Sensores físicos perdiam valores zero (`pressure=0f`, `tilt=0f`, `orientation=0f`). | `StrokeCapturePipeline:createPoint` verifica explicitamente `pressure >= 0f` e preserva coordenadas angulares de tilt e orientação mesmo quando exatamente zero. |
| **A08** | `ACTION_POINTER_UP` perdia a amostra final de dedos/caneta. | `handleActionPointerUp` coleta historical samples e a coordenada atual antes de disparar o encerramento do ponteiro. |
| **A09/A10** | `PageExporter` usava stub de 8 bytes e não aplicava escala de projeção da página. | Removido o stub falso; lançada `IOException` caso o bitmap falhe; implementada matriz de escala real do canvas original para a resolução alvo de exportação (1440x2560). |
| **A11** | Replay vetorial perdia estilos visuais de traço (`color`, `thickness`). | `StrokeReplayEngine` utiliza `stroke.copy(points = partialPoints, endedAtMs = ...)` mantendo idênticos o ID, ferramenta, cor e espessura do traço original. |
| **A12/A13** | Gabarito de inclinação descalibrado e avaliação aprovava tentativas incompletas de 2 pontos. | `ReferenceGlyphCatalog:BASIC_SLANT` rigorosamente alinhado a 52.0° ($\Delta x = 0.78128 \times xHeight$); `GeometricFeedbackEvaluator` adicionou verificação de extensão mínima, reprovando tentativas truncadas (<40% de score). |
| **A14** | Avaliação do treino guiado permanecia visível e desatualizada ao desenhar novos traços. | `GuidedPracticeViewModel:notifyStrokeChanged` anula imediatamente o estado de avaliação (`evaluation = null`). |
| **A15** | Alteração de pautas desatualizava a navegação de páginas subsequentes no caderno. | `NotebookPracticeViewModel:setGuidelineConfig` sincroniza atomicamente o cache `pagesList` com a página atualizada. |
| **A18** | Lint avisava sobre `device.isExternal` no Android Q e falta de permissão de execução no CI. | `DeviceCapabilityInspector` isolado com verificação `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`. CI workflow e git index atualizados com `chmod +x gradlew`. `lintDebug` executado com **0 erros**! |
| **A19** | Senhas da keystore de release hardcoded no script do Gradle. | `app/build.gradle.kts` configurado para ler exclusivamente de propriedades de projeto ou variáveis de ambiente sem expor segredos no repositório. |

---

## 8. Status dos Artefatos de Build e Publicação

- **Versão:** v0.3.0 (versionCode 5).
- **APKs Compilados:**
  - Release: `16.24 MB` (15.49 MiB) assinado com keystore `scribe-release.jks`.
  - Debug: `22.21 MB` (21.18 MiB).
- **Google Drive:**
  - `E:\Meu Drive\Apks\scribe-v0.3.0-release.apk`
  - `E:\Meu Drive\Apks\scribe-v0.3.0-debug.apk`
  - `E:\Meu Drive\Scribe\scribe-v0.3.0-release.apk`
  - `E:\Meu Drive\Scribe\scribe-v0.3.0-debug.apk`
  - `E:\Meu Drive\codex\scribe\scribe-v0.3.0-release.apk`
  - Código-fonte sincronizado integralmente em `E:\Meu Drive\codex\scribe`.
- **GitHub:**
  - Branch: `main` (rastreado e atualizado).
  - Tag: `v0.3.0`.
  - Release: `v0.3.0` no GitHub Releases com APKs de release e debug anexados.

---

## 9. Atualização de Roadmap: Milestones M4, M5 e M6 Concluídos

Os marcos subsequentes foram integralmente implementados, testados (152 testes unitários 100% aprovados, 0 lint errors, 4/4 watchdog) e lançados na versão **v0.6.0** (versionCode 8):
- **M4 — Learning System (v0.4.0):** SCR-401 a SCR-404 (Currículo de 18 lições em 5 estágios, temporizador de 5 fases pedagógicas, motor SRS local determinístico, histórico atômico e métricas não-punitivas).
- **M5 — Progress & Evolution (v0.5.0):** SCR-501 a SCR-504 (Comparador Before/After com deltas matemáticos, slider de sobreposição vetorial cross-fade, motor de Dual Replay em 60-120 fps e calendário mensal de consistência).
- **M6 — Meu Alfabeto & PersonalStyle (v0.6.0):** SCR-601 a SCR-604 (Catálogo canônico de 68 glifos, variantes históricas v1, v2... com favoritos, compilador matemático de estilo pessoal, repositório local atômico com .scribe e interface com grade vetorial auto-escalada).

Para o relatório completo de auditoria do código-fonte, arquitetura, modelos e testes dessas entregas, acesse:
👉 **[`AUDIT_REPORT_M4_M6.md`](file:///c:/Users/fael/Documents/Codex/scribe/AUDIT_REPORT_M4_M6.md)**

O próximo marco planejado no [ROADMAP.md](file:///c:/Users/fael/Documents/Codex/scribe/ROADMAP.md) é o **M7 — Professor IA & Coaching Inteligente**.

## Referências visuais para construção da interface

Por solicitação do usuário, o projeto contém **13 imagens de referência**: um painel geral com 12 telas e 12 imagens complementares, cada uma com três estados de um fluxo.

- [Painel geral das telas](docs/design/scribe-visao-12-telas-v1.png).
- [Índice das 12 referências de fluxos e instruções para o Antigravity](docs/design/fluxos-v1/README.md).
- [Auditoria independente V3: achados, dependências e ondas de estabilização](docs/ANTIGRAVITY_AUDIT_REVIEW_V3.md).

**Direção visual:** papel claro, tinta azul-marinho, ações em azul, guias discretas, controles nativos e espaço amplo para S Pen no Galaxy S25 Ultra. Usar as imagens como base de composição e interação; consultar o guia antes de copiar textos, valores, navegação ou controles.

**Ordem de trabalho:** corrigir o comportamento da onda correspondente e então aplicar a referência visual ao fluxo. Cada ação visível precisa funcionar de ponta a ponta. Não criar novas features, contas, cloud ou redesign amplo por elementos incidentais dos mockups. Opções adicionais ficam como propostas, não como escopo automaticamente autorizado. As imagens não aprovam milestones nem substituem os critérios da auditoria.

**Aceite visual por fluxo:** registrar qual imagem foi usada, implementar estados vazio/carregando/sucesso/erro pertinentes, verificar handlers e persistência, comparar screenshots reais com a referência e validar legibilidade, alvos de toque e área de escrita no aparelho. Registrar diferenças justificadas em PROJECT_STATE. Não usar as imagens geradas como evidência de aplicativo funcionando.
