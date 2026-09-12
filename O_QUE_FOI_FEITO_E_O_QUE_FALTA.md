# Scribe — O Que Foi Feito e O Que Falta (Relatório para Codex e Roadmap)

**Data de Atualização:** 12 de Setembro de 2026  
**Versão:** 0.6.0 (versionCode 8)  
**Aparelho-Alvo Principal:** Samsung Galaxy S25 Ultra (com S Pen original)  
**Repositório GitHub:** https://github.com/playertwo1/caligrafia  

---

## 1. O Que Foi Feito Até Agora (Marcos M0, M1, M2, M3, M4, M5 e M6 100% Concluídos)

### 1.1. Milestone M0 — Stylus Lab (Fundação do Motor de Caneta)
- **SCR-001 — Bootstrap do Projeto:** Android SDK 35, Min SDK 26, Kotlin 2.2.10, Gradle 9.3.1, AGP 9.1.1, Jetpack Compose com Material 3, aceleração gráfica por hardware ativa.
- **SCR-002 — Inspeção Segura de Hardware (`DeviceCapabilityInspector`):** Diagnóstico automático de caneta Stylus, touch, ranges de sensores (`AXIS_PRESSURE`, `AXIS_TILT`, `AXIS_ORIENTATION`, `AXIS_DISTANCE`), identificação das características da S Pen EMR passiva do S25 Ultra (sem Bluetooth/bateria) e delimitação de SDKs Samsung e Android Ink API.
- **SCR-003 — Pipeline de Ingestão de Alta Precisão (`StrokeCapturePipeline`):**
  - Consumo obrigatório de amostras históricas (`event.historySize`) para não perder nenhum ponto entre frames de 120Hz.
  - Preservação estrita de timestamps e sensores em estruturas imutáveis (`StrokePoint` e `Stroke`).
  - Nunca inventar dados (se o sensor não fornecer, permanece `null`).
- **SCR-004 — Renderizador Polimórfico em Tempo Real (`SmoothedReferenceRenderer`):**
  - Interpolação Bézier quadrática suave em pontos médios.
  - Modulação de espessura por pressão física da S Pen.
  - **Regra Inviolável:** A suavização é apenas uma projeção visual no Canvas; os traços brutos continuam 100% intocados e matematicamente puros.
- **SCR-005 — Rejeição de Palma Comportamental (`PalmRejectionPolicy`):**
  - Modo estrito *Stylus Only* (dedo nunca desenha tinta de caligrafia).
  - Preempção imediata por proximidade (Hover EMR da S Pen) e toque ativo da caneta.
  - Cooldown de repouso da mão (500ms).
  - Roteamento do botão lateral da S Pen para borracha vetorial.
- **SCR-006 — Spike de Persistência e ADR:**
  - Benchmark automatizado de 3 abordagens (Tabela Relacional SQLite vs. BLOB Compactado vs. Arquivo Binário Dedicado `.scribe`).
  - `.scribe` comprovou ser **9.2x mais rápido** e **88% mais leve** (7.3 bytes/ponto contra 61.1 bytes/ponto do SQLite normalizado).
- **SCR-007 — Motor de Replay Temporal Vetorial (`StrokeReplayEngine`):**
  - Reconstrução pura e determinística em 60 fps (`computeFrameAt`).
  - Velocidades 0.5x, 1.0x e 2.0x, scrubber temporal deslizante.
- **SCR-008 — Resiliência de Ciclo de Vida e Sessão:**
  - `StylusLabViewModel` com sobrevivência a rotação de tela e split-screen.
  - Flush e commit automático de traço ativo ao perder foco de tela ou bloquear o aparelho.
  - Auto-save assíncrono de contingência em `.scribe`.
  - Detector de reinserção física da S Pen no silo do S25 Ultra (`com.samsung.pen.INSERT`).

---

### 1.2. Milestone M1 — Caderno (Caderno Vetorial Multi-Página)
- **SCR-009 — Pautas Caligráficas Geométricas (`GuidelineConfig`):**
  - 4 linhas mestras: Ascendente, Altura-X (*Waistline*), Linha de Base (*Baseline*) e Descendente.
  - Proporções clássicas: `1:1:1` (Escolar), `2:1:2` (Copperplate), `3:2:3` (Spencerian / Itálica).
  - Linhas de inclinação (*slant lines*) calculadas por trigonometria exata ($\Delta X = \Delta Y / \tan(\theta)$).
  - Renderizador nativo `GuidelineRenderer` em plano de fundo sem impacto no desempenho.
- **SCR-010 — Gestor de Caderno e Páginas (`LocalNotebookRepository`):**
  - Entidades imutáveis `Notebook` e `NotebookPage`.
  - Serializador leve de manifestos `NotebookManifestSerializer`.
  - Armazenamento em arquivos binários dedicados `.scribe` por página com salvamento atômico seguro.
  - Criação, paginação, exclusão e carregamento assíncrono via coroutines.
- **SCR-011 — Ferramentas Caligráficas e Pilha Bidirecional de Undo/Redo:**
  - Modelo `ToolConfig` com calibração de espessura (Fina 2.5px, Média 5.0px, Grossa 8.5px).
  - Paleta de tintas nobres: Nanquim, Sépia, Azul Real, Vinho, Grafite e Verde Floresta.
  - Pilha completa de Desfazer (`undo()`) e Refazer (`redo()`) em `InMemoryStrokeRepository`.
  - Schema v2 do `.scribe` com retrocompatibilidade total com a v1.
- **SCR-012 — Borracha por Traço Completa e Segment Eraser:**
  - `StrokeEraserHelper` com AABB Bounding Box e distância ponto-a-segmento euclidiana.
  - Apagamento em varredura contínua (*sweep*) de múltiplos traços sem vazamento de pontos falsos.
  - Apagamento de toques pontuais (pingos no 'i' e acentos).
- **SCR-013 — Exportação de Página para PNG em Alta Resolução (`PageExporter`):**
  - Exportação de página completa em 1440x2560 (ARGB_8888) com matriz de escala real e pautas caligráficas.
  - **Regra Inviolável Garantida:** O bitmap gerado é apenas derivado para exportação; os traços vetoriais brutos permanecem intocados no disco.
- **SCR-014 — Interface Completa do Caderno (`NotebookPracticeScreen`):**
  - Barra de paginação (anterior, próxima, nova página `+`).
  - Toolbar caligráfica completa em Jetpack Compose com seletor e diálogo de estilos caligráficos (M3).
  - Superfície de escrita `NotebookCanvasView` com visual e sensação de papel caligráfico.

---

### 1.3. Hotfix e Refinamentos no Aparelho Real (Galaxy S25 Ultra)
- **SCR-BUG-001 — Correção de Crash ao Iniciar (v0.1.1):** `@JvmOverloads constructor` adicionado em ViewModels para prevenir `NoSuchMethodException` no `AndroidViewModelFactory` do Android e blindagem de ciclo de vida.
- **SCR-FEAT-001 — Bloqueio de Gestos de Borda Laterais estilo Samsung Notes (v0.1.2):** `EdgeGestureExclusionHelper` utilizando `ViewCompat.setSystemGestureExclusionRects` nas laterais da tela para permitir apoiar a mão e escrever junto às bordas sem acionar acidentalmente o gesto de voltar do sistema operacional.

---

### 1.4. Milestone M2 — Treino Guiado (Pedagogia e Feedback Determinístico)
- **SCR-015 — Modelo de Glifo de Referência e Catálogo:** `ReferenceGlyph`, `ReferenceStroke`, `ReferencePoint`, `DirectionalHint` e catálogo `ReferenceGlyphCatalog` com 12 exercícios fundamentais (6 traços básicos calibrados a 52.0° e 6 letras cursivas iniciais).
- **SCR-016 — Ghost Mode Progressivo:** 5 níveis de transparência (100%, 70%, 40%, 10%, 0%) com renderização vetorial e setas direcionais numeradas.
- **SCR-017 — Fluxo Pedagógico em 3 Etapas:** Cobrir (Trace) → Copiar (com modelo ao lado) → Sozinho (Solo).
- **SCR-018 — Motor de Avaliação Geométrica Determinística (`GeometricFeedbackEvaluator`):** Avaliação matemática pura (zero IA/cloud) de limites de pauta, paralelismo angular de inclinação, direção/ordem dos traços e teste de cobertura mínima, fornecendo nota e diagnósticos detalhados em português.
- **SCR-019 — Interface de Treino Guiado (`GuidedPracticeScreen`):** Tela em Compose com tabs de estágio, seletor de exercícios, Ghost Mode e card de feedback.

---

### 1.5. Resolução da Auditoria do Codex e Milestone M3 — Style Engine (v0.3.0)
- **Resolução de Apontamentos da Auditoria do Codex (`ANTIGRAVITY_AUDIT_REVIEW.md`):**
  - *A02:* Salvamento atômico seguro com arquivos temporários `.tmp` e substituição atômica via `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`.
  - *A04/A05:* Isolamento absoluto da borracha sem gerar traços espúrios de tinta e preservação de continuidade de varredura.
  - *A07:* Preservação de valores zero de sensores físicos (`pressure=0f`, `tilt=0f`, `orientation=0f`).
  - *A08:* Coleta de dados históricos e atuais em `ACTION_POINTER_UP`.
  - *A09/A10:* Remoção de stubs e suporte a escalonamento de projeção real na exportação de PNG.
  - *A11:* Preservação de ID, cor e espessura no replay vetorial temporal.
  - *A12/A13:* Calibração trigonométrica do `BASIC_SLANT` a 52.0° exato e teste de cobertura mínima impedindo aprovação de tentativas truncadas de 2 pontos.
  - *A14:* Reset imediato de avaliação ao registrar novos traços.
  - *A15:* Sincronização de cache de pautas ao navegar pelas páginas do caderno.
  - *A18:* Guarda de versão `Build.VERSION_CODES.Q` para `device.isExternal` e `chmod +x gradlew` no CI (lint aprovado com 0 erros).
  - *A19:* Remoção de senhas hardcoded em `app/build.gradle.kts`.
- **SCR-020 — Formato Canônico ScribeStyle v1:** Entidades `ScribeStyle`, `StyleCategory`, `DuctusRule` e `PressureBehavior`.
- **SCR-021 — Três Famílias de Estilos Canônicos:** Cursiva Escolar Brasileira (1:1:1, 68°), Copperplate / English Roundhand (3:2:3, 52°) e Spencerian Script (2:1:2, 52°).
- **SCR-022 — Importador de Fontes Locais TTF/OTF (`StyleFontImporter`):** Validação segura de magic bytes, geração de estilos visuais e fallback defensivo de Typefaces. Fontes tipográficas funcionam estritamente como gabarito estético e nunca substituem os traços vetoriais brutos.
- **SCR-023 — Motor de Estilos e UI (`StyleEngine`):** Registro centralizado com fallback gracioso para Cursiva Escolar, seletores de estilo e recálculo automático de pautas no Caderno e no Treino Guiado.

---

### 1.6. Milestone M4 — Learning System (Sessões Deliberadas, Currículo e SRS Local)
- **SCR-401 — Currículo Pedagógico Canônico (`CurriculumCatalog`):**
  - 18 lições sequenciais progressivas distribuídas nos 5 estágios canônicos:
    - *Estágio 1 (Traços):* Inclinação/Pressão, Underturn, Overturn, Curva Composta, Oval, Laçada Alta.
    - *Estágio 2 (Famílias):* Família Underturn ('i', 't'), Família Oval ('c', 'o', 'a'), Família Laçada ('l').
    - *Estágio 3 (Conexões):* Ligadura base-base ('it'), Ligadura com ascendente ('al'), Ligadura elevada ('to').
    - *Estágio 4 (Palavras):* 'lua', 'arte', 'calma'.
    - *Estágio 5 (Frases):* 'arte e calma', 'o traço revela a alma', 'viva a caligrafia'.
- **SCR-402 — Temporizador por Fases Pedagógicas (`SessionTimer`):**
  - Opções pré-programadas de 5, 10, 15 e 20 minutos particionadas nas 5 fases:
    - Aquecimento (15%) -> Foco da Lição (15%) -> Prática Assistida Ghost (40%) -> Prática Autônoma (20%) -> Conclusão & Resumo (10%).
  - Suporte a pausa, retomada, avanço manual e método síncrono `tickOneSecond()` para testes automatizados determinísticos.
- **SCR-403 — Motor de Repetição Espaçada 100% Determinístico (`ReviewScheduler`):**
  - Algoritmo offline baseado em consolidação neuromotora:
    - Desempenho < 60%: revisão prioritária em 1 dia.
    - Desempenho 60% a 79%: expansão moderada de intervalo (1.5x).
    - Desempenho >= 80%: consolidação motora (2.2x do intervalo).
  - Recomendação diária inteligente que seleciona automaticamente aquecimento, lição de foco da trilha e lições vencidas com menor pontuação.
- **SCR-404 — Histórico Local Seguro e Métricas Não-Punitivas (`LocalLearningHistoryRepository`):**
  - Serializador puro em Kotlin `LearningHistorySerializer` sem dependências do framework Android (zero erros de mock em JVM).
  - Gravação atômica defensiva `.tmp` + `Files.move(..., ATOMIC_MOVE)` com sincronização física de I/O (`fos.fd.sync()`).
  - Métricas sem penalização ou "streak fires": tempo total praticado, total de sessões, lições únicas e dias ativos no último mês.
- **Interface Completa do Hub de Aprendizado (`LearningHubScreen` e `LearningViewModel`):**
  - Tela rica em Jetpack Compose com cards de progresso, seletor de duração, recomendação diária, acordeão dos 5 estágios e diálogo modal da sessão ativa.
  - Botão de atalho "Aulas (M4)" integrado na toolbar do caderno e navegação bidirecional na `MainActivity`.

---

### 1.7. Milestone M5 — Evolução & Progresso (Before/After, Overlay, Dual Replay e Consistência)
- **SCR-501 — Comparador Before / After e Métricas de Evolução (`AttemptComparator`):**
  - Pareamento de duas tentativas (baseline / primeira tentativa vs. tentativa atual ou selecionada).
  - Cálculo de deltas matemáticos determinísticos:
    - Delta de precisão/score (+% de evolução).
    - Delta de inclinação angular (redução de desvio em relação ao ângulo canônico da pauta).
    - Delta de cadência de escrita (velocidade média em pixels/ms).
- **SCR-502 — Slider de Sobreposição Vetorial Cross-Fade (`OverlaySlider`):**
  - Sobreposição direta das duas tentativas no mesmo sistema de coordenadas com pautas caligráficas clássicas no fundo.
  - Slider contínuo de 0% a 100% alternando opacidade:
    - Tentativa "Antes" renderizada em tom Coral vibrante (`#E11D48`).
    - Tentativa "Depois" renderizada em tom Azul Royal clássico (`#2563EB`).
  - Renderização vetorial pura a partir dos pontos brutos (`StrokePoint`), sem rasterização intermediária.
- **SCR-503 — Motor de Dual Replay Lado a Lado Sincronizado (`DualReplayEngine`):**
  - Replay temporal de vetores em duas janelas paralelas sincronizadas por timeline unificada baseada na duração máxima.
  - Painel de controles de transporte de áudio/vídeo: Play, Pause, Stop, Seek interativo.
  - Três velocidades canônicas de reprodução: 0.5x (câmera lenta analítica), 1.0x (tempo real), 2.0x (visão acelerada).
  - Reconstrução pura em 60 a 120 fps sobre o hardware gráfico do S25 Ultra.
- **SCR-504 — Calendário de Consistência e Métricas Agregadas Não-Punitivas (`CalendarConsistencyHelper`):**
  - Repositório local com persistência atômica segura `.tmp` + `ATOMIC_MOVE` (`LocalPracticeAttemptRepository`).
  - Serialização `.scribe` individual por tentativa vinculada ao exercício pedagógico.
  - Grade mensal de calendário com intensidade suave de cor por tempo praticado.
  - Filosofia 100% não-punitiva: zero penalidades de quebra de sequência ("streaks"), valorização da soma total de minutos praticados e destaque do exercício com maior salto evolutivo.
- **Interface Completa de Evolução (`EvolutionScreen` e `EvolutionViewModel`):**
  - 4 abas estruturadas: "Antes / Depois", "Sobreposição", "Replay Duplo" e "Consistência".
  - Seletores interativos de exercícios e tentativas com pré-visualização vetorial no Canvas nativo.
  - Botão de acesso rápido "Evolução (M5)" na barra superior do caderno e rota direta na `MainActivity`.

---

### 1.8. Milestone M6 — Meu Alfabeto & PersonalStyle (Construção da Escrita Própria)
- **SCR-601 — Modelo de Domínio do Alfabeto Pessoal & Variantes:**
  - Entidades `PersonalGlyph`, `GlyphVariant` (v1, v2, v3...), `PersonalAlphabet` e `AlphabetCategoryStats`.
  - Catálogo de 68 caracteres e ligaduras canônicas: minúsculas (`'a'` a `'z'`), maiúsculas (`'A'` a `'Z'`), algarismos (`'0'` a `'9'`) e conexões/símbolos (`"it"`, `"al"`, `"to"`, `"&"`, `"?"`, `"!"`).
  - Suporte a versionamento progressivo por caractere, eleição de variantes favoritas e histórico vetorial preservado.
- **SCR-602 — Motor de Compilação de Estilo Pessoal (`PersonalStyleCompiler`):**
  - Análise matemática puramente determinística e offline sobre os traços das variantes favoritas do usuário:
    - *Inclinação Média ($\theta_{médio}$):* ponderação dos eixos descendentes da escrita do usuário.
    - *Proporção de Pauta:* detecção da relação ascender/corpo para seleção de `Ratio111`, `Ratio212` ou `Ratio323`.
    - *Espessura e Contraste:* cálculo de modulação de pressão e espessura base recomendada.
  - Geração de `ScribeStyle` com categoria `StyleCategory.PERSONAL` e auto-registro no `StyleEngine` para uso imediato no Caderno e Treino Guiado.
- **SCR-603 — Repositório Local com Persistência Atômica Segura (`LocalPersonalAlphabetRepository`):**
  - Gravação defensiva `.tmp` + `ATOMIC_MOVE` + `fos.fd.sync()` para o manifesto `personal_alphabet_manifest.json`.
  - Armazenamento de traços vetoriais brutos imutáveis em arquivos dedicados compactados `.scribe` via `DedicatedFileStrategy`.
  - Serializador puro em Kotlin `PersonalAlphabetSerializer` sem dependência de stubs do `android.jar`.
  - Pré-carregamento com sementes caligráficas prévias para experiência rica imediata.
- **SCR-604 — Interface "Meu Alfabeto" em Jetpack Compose (`AlphabetScreen` e `AlphabetViewModel`):**
  - Grade responsiva de glifos com pré-visualização vetorial em tempo real auto-escalada com pautas clássicas.
  - Barra de progresso de curadoria do alfabeto e botão de destaque para compilação do estilo próprio.
  - Bottom sheet de inspeção com histórico completo de versões gravadas, botão de favorito (estrela) e atalho para praticar no Treino Guiado.
  - Diálogo comemorativo com resumo dos parâmetros calculados do estilo e atalho "Usar no Caderno".
  - Botão de acesso rápido "Alfabeto (M6)" na toolbar do caderno e rota central na `MainActivity`.

---

## 2. Cobertura de Testes e Qualidade

- **Testes Unitários Automatizados:** 149 testes passando 100% (33 classes de testes unitários cobrindo M0 a M6).
- **Verificação do Watchdog (`watchdog.ps1`):** Aprovado (4/4 verificações).
  - Zero WebViews.
  - Zero dependências não autorizadas de nuvem/backend.
- **Análise de Lint (`lintDebug`):** 0 erros.
- **Compilação:**
  - APK Debug: `app-debug.apk` (22.52 MB)
  - APK Release Assinado: `app-release.apk` (16.42 MB)

---

## 3. O Que Falta Implementar nos Próximos Marcos (Roadmap M7 e M8)

Conforme a especificação [ROADMAP.md](file:///c:/Users/fael/Documents/Codex/scribe/ROADMAP.md) e [PRODUCT_SPEC.md](file:///c:/Users/fael/Documents/Codex/scribe/PRODUCT_SPEC.md):

### 3.1. M7 — Professor IA (PRÓXIMO MARCO)
- Somente após volume suficiente de sessões e variantes reais: modelagem local e segura para interpretação de padrões de escrita e sugestões de treino sem inventar métricas.
- Respeito absoluto à privacidade do usuário e aos dados vetoriais locais (zero nuvem obrigatória).

### 3.2. M8 — Expansões
- Integração de relógio para haptics/ritmo (Galaxy Watch), backup opcional e exportação avançada.

---

## 4. Instruções para Auditoria pelo Codex

1. O arquivo completo de auditoria técnica detalhada está disponível em [`AUDIT_REPORT.md`](file:///c:/Users/fael/Documents/Codex/scribe/AUDIT_REPORT.md).
2. O script de verificação estática e compilação do Watchdog pode ser executado via terminal com:
   ```powershell
   .\scripts\watchdog.ps1
   ```
3. O APK assinado pronto para instalação direta no Samsung Galaxy S25 Ultra foi copiado para o Google Drive na pasta `Apks/` e `Scribe/`.
