# PROJECT_STATE

**Projeto:** Scribe / Caligrafia
**Versão documental:** v0.1.1
**Versão do aplicativo:** v0.1.1 (versionCode 2)
**Estado:** M1_COMPLETED — Milestone M1 (Caderno) 100% concluído e verificado (75 testes unitários passando, APK 16.17 MB release / 22.09 MB debug, Watchdog aprovado). Transição formal para o Milestone M2 (Treino Guiado).
**Milestone atual:** M2 — Treino Guiado.
**Código de produto:** M0 concluído (SCR-001 a SCR-008); M1 concluído (SCR-009 a SCR-014); Hotfix SCR-BUG-001 (v0.1.1).

## Status de Transição — Gate M0 & Gate M1
- **Gate M0 (Stylus Lab):** Aprovado tecnicamente com pendências de auditoria independente pelo Codex e testes físicos no Samsung Galaxy S25 Ultra registradas no `AUDIT_REPORT.md`.
- **Gate M1 (Caderno):** Aprovado integralmente com 75 testes unitários passando, zero violações arquiteturais e APK v0.1.1 operacional.

## Entregas Concluídas
- **SCR-001 a SCR-008 (Milestone M0 — Stylus Lab):** Bootstrap Android, Device Capability Inspector (Galaxy S25 Ultra + S Pen), Capture Pipeline com raw strokes imutáveis e historical samples, Live Renderer (Android Ink API + Bézier nativo de referência), Palm Rejection com proximidade EMR e modo Stylus Only, Persistence Spike (decisão arquitetural `.scribe` híbrido), Replay vetorial determinístico (0.5x, 1x, 2x) e Lifecycle Edge Cases (flush de traço ativo, contingência e detecção de silo de hardware).
- **SCR-009 Modelo de Caderno, Páginas e Pautas Caligráficas (M1):** Modelos `Notebook` e `NotebookPage`, cálculo trigonométrico rigoroso de pautas caligráficas com 4 linhas e slant lines com $\Delta X = \Delta Y / \tan(\theta)$, proporções clássicas (1:1:1, 2:1:2, 3:2:3), e renderizador de alta performance `GuidelineRenderer` sobre Canvas nativo no plano de fundo.
- **SCR-010 Gestor de Caderno, Páginas e Persistência (.scribe + Room leve) (M1):** Repositório `LocalNotebookRepository` com serializador leve de manifestos `NotebookManifestSerializer`, vinculação com arquivos `.scribe` compactados via `DedicatedFileStrategy`, criação, listagem, ordenação, e remoção recursiva de cadernos e páginas.
- **SCR-011 Ferramentas de Escrita e Pilha Bidirecional de Undo/Redo (M1):** Modelo `ToolConfig` com calibração de espessura (Fina 2.5px, Média 5.0px, Grossa 8.5px), paleta de cores clássicas (Nanquim, Sépia, Azul Real, Vinho, Grafite, Verde), persistência de cor e espessura no traço (`Stroke.kt`) e no Schema v2 de `DedicatedFileStrategy` (com retrocompatibilidade para v1), e pilha bidirecional completa de Desfazer/Refazer em `InMemoryStrokeRepository`.
- **SCR-012 Borracha por Traço Completa e Segment Eraser (M1):** Utilitário `StrokeEraserHelper` com AABB bounding box e distância euclidiana ponto-a-segmento, suporte a sweeping contínuo com múltiplos traços apagados em uma única passada, e apagamento de pontos pontuais/acentos caligráficos.
- **SCR-013 Exportação em Alta Resolução de Página para PNG (M1):** Módulo `PageExporter` que renderiza pautas e traços vetoriais em imagens PNG de alta resolução (1440x2560) preservando a regra inviolável de que traços brutos nunca são substituídos ou destruídos por bitmaps.
- **SCR-014 Interface do Caderno de Prática (M1):** Tela Jetpack Compose `NotebookPracticeScreen` com navegação por páginas (anterior/próxima/adicionar), toolbar de caligrafia com chips de espessura, seletor de cores, presets de pauta e ações de undo/redo/limpar/exportar, tela de canvas nativa `NotebookCanvasView` e roteador central na `MainActivity`.

## Próxima Ação
- **M2 — Treino Guiado (SCR-015):**
  - Definição do modelo de **Glyph de Referência** caligráfico.
  - Implementação do **Ghost Mode** com transparência progressiva (100% -> 70% -> 40% -> 10% -> 0%).
  - Fluxo pedagógico: Trace -> Copiar -> Sozinho.
  - Exercícios de caligrafia por letra com feedback geométrico determinístico.

## Gates
Não iniciar IA, backend, login, marketplace ou biblioteca avançada de estilos antes dos milestones correspondentes. Gate M0 formalizado com pendências externas documentadas.

## Decisões já tomadas
- Kotlin nativo + Jetpack Compose.
- `MotionEvent` como fundação de input.
- raw strokes imutáveis: `StrokePoint` e `Stroke` preservam a história real do movimento.
- pressure/tilt/orientation somente quando disponíveis no hardware; ausentes permanecem `null`.
- local-first.
- TTF/OTF visual ≠ `ScribeStyle` pedagógico.
- Aparelho-alvo: Samsung Galaxy S25 Ultra com S Pen original.
- Uso de SDKs Samsung solicitado pelo usuário: identificar e integrar recursos oficialmente compatíveis no M0, atrás de adapters; registrar caso nenhum SDK seja aplicável.
- S Pen Remote SDK não serve como motor de tinta; a S Pen do S25 Ultra não oferece Bluetooth/funções remotas. Captura permanece baseada em `MotionEvent`.
- Direção aprovada: superfície nativa, telas Compose e Android Ink API como primeira opção no M0. Comparar com referência em Compose Canvas ou View customizada no S25 Ultra; adoção final condicionada aos critérios de latência, estabilidade, dados brutos, persistência e replay.
- Canvas de escrita utilizando interceptação nativa de MotionEvent para fidelidade total a eventos históricos e de hover.
- Suavização visual puramente derivada: o smoothing Bézier ou da Ink API é apenas uma projeção visual no Canvas; os dados brutos gravados na sessão permanecem imutáveis.
- Dedo não gera tinta no modo de caligrafia; S Pen tem preempção absoluta por hover e toque.
- Botão da S Pen ativa a borracha vetorial por traço com suporte total a undo.
- Formato de persistência para cadernos/páginas: Arquivo Binário Dedicado (`.scribe`) com índice relacional no Room. O modelo relacional ponto-a-ponto foi formalmente rejeitado devido à alta sobrecarga de I/O e 9x maior consumo de disco.
- Replay vetorial por reconstrução temporal pura: o motor reconstrói fatias imutáveis dos `Stroke` e `StrokePoint` sem duplicar alocações pesadas ou gerar bitmaps pré-gravados; a renderização do replay reutiliza o renderizador ativo mantendo fidelidade total e 60 fps.
- Resiliência de ciclo de vida e sessão: gerenciamento via ViewModel para sobrevivência a mudanças de configuração (rotação/split-screen), flush automático de traços ativos em background/bloqueio de tela, auto-save contingency em `.scribe` e adapter desacoplado para detecção de silo de S Pen (`com.samsung.pen.INSERT`).
- Watchdog ativo para contenção e garantia contra desvios arquiteturais.

## Histórico de Atualizações
- 2026-09-12 — Documentação v0.1 publicada no GitHub como pedra fundamental.
- 2026-09-12 — ROADMAP, AGENTS e ARCHITECTURE alinhados ao aparelho-alvo S25 Ultra, SDKs Samsung e Android Ink API como primeira opção a avaliar no M0.
- 2026-09-12 — Implementação e validação de SCR-001 (Bootstrap), SCR-002 (Inspector com diagnóstico de S25 Ultra, SDKs Samsung e requisitos Ink API) e Watchdog automatizado.
- 2026-09-12 — Implementação e validação de SCR-003 (Capture Pipeline: raw strokes imutáveis, historical samples, isolamento de ponteiro, repositório em memória e undo/clear).
- 2026-09-12 — Implementação e validação de SCR-004 (Live Renderer: Android Ink API 1.0.0-alpha03, SmoothedReferenceRenderer com Bézier derivado, RawPolylineRenderer, RendererManager e seletor em tempo real no Stylus Lab).
- 2026-09-12 — Implementação e validação de SCR-005 (Stylus-only + Palm Rules: PalmRejectionPolicy com hover proximity S Pen, preempção ativa, botão lateral como borracha, apagador vetorial e 25 testes unitários).
- 2026-09-12 — Implementação e validação de SCR-006 (Persistence Spike: 3 estratégias avaliadas, benchmark automatizado, decisão por arquivo binário dedicado .scribe + Room leve, 29 testes unitários).
- 2026-09-12 — Implementação e validação de SCR-007 (Replay: motor temporal desacoplado computeFrameAt, controle 0.5x/1.0x/2.0x, scrubber temporal, integração no Stylus Lab com isolamento de toque e 38 testes unitários).
- 2026-09-12 — Implementação e validação de SCR-008 (Lifecycle & Session Edge Cases: StylusLabViewModel, flushActiveStroke, auto-save de contingência .scribe, SPenInsertionDetector e 45 testes unitários). Conclusão do Milestone M0.
- 2026-09-12 — Formalização do Gate M0 (auditoria Codex e validação S25 Ultra registradas). Início do Milestone M1 — Caderno. Implementação e validação de SCR-009 (Modelo de Caderno, Páginas e Pautas Caligráficas com GuidelineConfig, proporções 1:1:1 / 2:1:2 / 3:2:3, slant lines com tan(θ), GuidelineRenderer nativo e 54 testes unitários).
- 2026-09-12 — Conclusão do Milestone M1 — Caderno: SCR-010 (LocalNotebookRepository, serializador de manifestos e .scribe dedicado), SCR-011 (ToolConfig, paleta clássica de cores, espessuras e pilha bidirecional de Undo/Redo), SCR-012 (StrokeEraserHelper para varredura e pontos), SCR-013 (PageExporter PNG de alta resolução preservando traços vetoriais), SCR-014 (NotebookPracticeScreen em Compose com folheamento, toolbar e canvas nativo). 73 testes unitários aprovados no Watchdog, APK de 21.12 MB. Transição formal para o Milestone M2 — Treino Guiado.
- 2026-09-12 — Resolução de Bug de Inicialização no Dispositivo Físico (SCR-BUG-001): Identificada ausência de `@JvmOverloads` no construtor primário de `StylusLabViewModel` provocando `NoSuchMethodException` em `AndroidViewModelFactory` durante o `onResume` da `MainActivity`. Adicionado `@JvmOverloads constructor`, blindagem com `try/catch` no ciclo de vida de `MainActivity`, desregistro defensivo do receptor da S Pen no `onPauseLifecycle` e substituição de ícones estendidos por core (`Clear` e `Delete`). Criado teste automatizado `ViewModelInstantiationTest.kt` (totalizando 75 testes 100% aprovados). APKs de release e debug recompilados e sincronizados no Google Drive e GitHub.
