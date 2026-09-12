# Scribe v0.5.0 — Milestone M5: Progress & Evolution (Before/After, Overlay, Dual Replay e Calendário de Consistência)

A versão **v0.5.0** (versionCode: 7) do **Scribe** conclui integralmente o **Milestone M5 (Progress & Evolution)**, trazendo a comparação visual e matemática de evolução caligráfica, sobreposição com transparência ajustável, replay lado a lado com controle de transporte em 60-120 fps, e calendário mensal de consistência 100% não-punitivo.

---

## 1. O que foi entregue no Milestone M5 — Progress & Evolution

### SCR-501: Comparador Before / After e Métricas de Evolução
- Implementação em [`AttemptComparator.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/engine/AttemptComparator.kt) e modelos em [`EvolutionModels.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/model/EvolutionModels.kt):
  - Pareamento de duas tentativas de escrita (primeira tentativa ou baseline vs. tentativa atual ou selecionada).
  - Cálculo de deltas matemáticos determinísticos sem uso de IA ou computação em nuvem:
    - **Delta de Precisão / Score:** diferença percentual de qualidade do traçado (+% de evolução).
    - **Delta de Alinhamento de Inclinação:** redução do desvio angular em relação à pauta formal (graus de erro).
    - **Delta de Cadência / Velocidade:** comparação de velocidade média de traçado (px/ms), indicando fluidez e confiança do movimento.

### SCR-502: Slider de Sobreposição Vetorial Cross-Fade (Overlay)
- Renderização direta no mesmo espaço euclidiano sobre pautas caligráficas clássicas:
  - Slider contínuo de 0% a 100% ajustando opacidade de transição (*cross-fade*).
  - **Tentativa "Antes" (Baseline):** renderizada em tom Coral vibrante (`#E11D48`).
  - **Tentativa "Depois" (Atual):** renderizada em tom Azul Royal clássico (`#2563EB`).
  - Renderização vetorial pura a partir dos pontos brutos imutáveis (`StrokePoint`), preservando fidelidade sem pixelização ou rasterização destrutiva.

### SCR-503: Motor de Dual Replay Lado a Lado Sincronizado
- Módulo [`DualReplayEngine.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/engine/DualReplayEngine.kt):
  - Timeline unificada normalizada pela duração máxima entre os dois traçados.
  - Painel completo de controles de transporte: Play, Pause, Stop, Seek temporal interativo.
  - Três velocidades canônicas de reprodução: **0.5x** (câmera lenta analítica), **1.0x** (velocidade real), **2.0x** (revisão acelerada).
  - Reconstrução pura em tempo real (60 a 120 fps) no hardware gráfico do Galaxy S25 Ultra.

### SCR-504: Calendário de Consistência e Histórico Local Não-Punitivo
- Módulo [`CalendarConsistencyHelper.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/engine/CalendarConsistencyHelper.kt) e repositório [`LocalPracticeAttemptRepository.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt):
  - Gravação atômica segura `.tmp` + `Files.move(..., ATOMIC_MOVE)` com sincronização física de I/O (`fos.fd.sync()`).
  - Serialização `.scribe` individual por tentativa de exercício pedagógico.
  - Grade mensal de calendário com intensidade suave de calor proporcional ao tempo praticado em cada dia.
  - Filosofia 100% não-punitiva: sem perda de "streaks", valorização da dedicação total em minutos e destaque do exercício com maior salto evolutivo.

### Interface Completa de Evolução (Jetpack Compose)
- [`EvolutionScreen.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionScreen.kt) e [`EvolutionViewModel.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionViewModel.kt):
  - 4 abas estruturadas: "Antes / Depois", "Sobreposição", "Replay Duplo" e "Consistência".
  - Pré-visualizações vetoriais em Canvas e seletores interativos.
  - Botão de acesso rápido "Evolução (M5)" na barra do caderno ([`NotebookPracticeScreen.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookPracticeScreen.kt)) e roteador central na [`MainActivity.kt`](file:///c:/Users/fael/Documents/Codex/scribe/app/src/main/java/com/scribe/caligrafia/MainActivity.kt).

---

## 2. Testes Automatizados e Qualidade

- **Testes Unitários:** **137 testes automatizados** passando com 100% de sucesso (29 classes de teste cobrindo M0 a M5).
  - `AttemptComparatorTest.kt`: validação de deltas de precisão (+%), inclinação (graus) e velocidade.
  - `DualReplayEngineTest.kt`: sincronização de timeline, Play/Pause/Stop/Seek e controle de velocidades 0.5x, 1.0x, 2.0x.
  - `CalendarConsistencyHelperTest.kt`: agregação de minutos por dia e geração da grade mensal.
  - `LocalPracticeAttemptRepositoryTest.kt`: salvamento atômico, leitura de tentativas e persistência `.scribe`.
  - `ViewModelInstantiationTest.kt`: verificação de `@JvmOverloads constructor` para `EvolutionViewModel`.
- **Análise Estática de Lint:** `lintDebug` executado com **0 erros**.
- **Watchdog:** 4/4 etapas aprovadas (`scripts/watchdog.ps1`). Zero WebViews e zero dependências não autorizadas de nuvem.
- **Compilação de APKs:**
  - APK Release Assinado: `app-release.apk` (**16.36 MB**)
  - APK Debug: `app-debug.apk` (**22.40 MB**)
- **Versionamento:** `versionCode = 7`, `versionName = "0.5.0"`.

---

## 3. Distribuição dos Artefatos

1. **Google Drive (Aparelho Físico / S25 Ultra):**
   - `E:\Meu Drive\Apks\scribe-v0.5.0-release.apk`
   - `E:\Meu Drive\Apks\scribe-v0.5.0-debug.apk`
   - `E:\Meu Drive\Scribe\scribe-v0.5.0-release.apk`
   - `E:\Meu Drive\Scribe\scribe-v0.5.0-debug.apk`
   - Espelho completo do código e APKs em `E:\Meu Drive\codex\scribe\`
2. **GitHub:**
   - Tag `v0.5.0` criada e sincronizada com `origin/main`.
