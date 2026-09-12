# ROADMAP — Scribe

## M0 — Stylus Lab [CONCLUÍDO — v0.1.0]
Objetivo: eliminar o maior risco técnico antes de construir o produto.

**Aparelho-alvo:** Samsung Galaxy S25 Ultra com a S Pen fornecida com o aparelho. A validação de escrita e os benchmarks do M0 devem ser realizados nesse dispositivo.

**Integração Samsung:** por preferência do usuário, utilizar SDKs Samsung para recursos de escrita/S Pen quando houver suporte oficial ao S25 Ultra e benefício aplicável. No M0, identificar o SDK e sua versão, verificar disponibilidade/licença e compatibilidade, e integrar os recursos suportados por adapter. Caso não exista SDK aplicável, registrar a limitação e manter a captura por `MotionEvent`; não adicionar dependência sem função utilizável. O S Pen Remote SDK citado nas referências trata de controle remoto; a S Pen do S25 Ultra não possui Bluetooth nem suporta funções remotas, portanto ele não deve ser adotado como motor de tinta.

**Canvas — direção aprovada:** usar superfície nativa e avaliar primeiro a Android Ink API como solução de escrita de baixa latência, com telas em Jetpack Compose. No M0, validar integração, preservação dos pontos brutos/timestamps, persistência e replay no S25 Ultra. Comparar com uma implementação de referência em Compose Canvas ou Android View customizada; recorrer a essas alternativas se a Ink API não atender aos critérios. Registrar medições e decisão antes do Gate M0. Não usar WebView na superfície de escrita.

Entregas: captura `ACTION_DOWN/MOVE/UP/CANCEL`, historical samples, tool type, x/y/timestamps, pressure/orientation/tilt quando disponíveis, rendering em tempo real, palm rejection comportamental, persistência e replay 0.5x/1x/2x.

**Gate M0:** APROVADO. Resolução de probe defects da auditoria do Codex integrada.

## M1 — Caderno [CONCLUÍDO — v0.1.0 / v0.1.2]
Páginas/sessões, guias baseline/x-height/ascender/descender, espessura, undo/redo, borracha por stroke, salvar/restaurar e export PNG sem substituir dados vetoriais. Bloqueio de gestos de borda laterais do S25 Ultra.

## M2 — Treino Guiado [CONCLUÍDO — v0.2.0]
Glyph de referência, Ghost Mode 100/70/40/10/0%, trace → copiar → sozinho, exercícios por letra e feedback determinístico.

## M3 — Style Engine [CONCLUÍDO — v0.3.0]
`ScribeStyle v1`, três estilos pedagógicos iniciais (Cursiva Escolar, Copperplate, Spencerian), importação TTF/OTF como referência visual, preview e fallback de glyph.

## M4 — Learning System [CONCLUÍDO — v0.4.0]
Currículo canônico de 18 lições em 5 estágios (Traços, Famílias, Conexões, Palavras, Frases); sessões de 5/10/15/20 min com temporizador por 5 fases; histórico atômico local e revisão espaçada (SRS) 100% determinística sem nuvem/IA; tela `LearningHubScreen` e métricas não punitivas.

## M5 — Evolução [CONCLUÍDO — v0.5.0]
Before/after com deltas de precisão/inclinação/cadência (`AttemptComparator`), slider de sobreposição com cross-fade (`OverlaySlider`), replay lado a lado sincronizado (`DualReplayEngine`), calendário de consistência com tempo praticado e indicadores não punitivos (`CalendarConsistencyHelper`), repositório com persistência atômica segura `.scribe` (`LocalPracticeAttemptRepository`) e tela unificada `EvolutionScreen`.

## M6 — Meu Alfabeto [CONCLUÍDO — v0.6.0]
Catálogo de 68 glifos canônicos (`PersonalAlphabet`), versionamento de variantes (`GlyphVariant` v1, v2, v3...) com eleição de favoritas, repositório local atômico `.scribe` (`LocalPersonalAlphabetRepository`), motor de compilação de estilo pessoal (`PersonalStyleCompiler`), tela `AlphabetScreen` em Compose com grade adaptativa, preview vetorial e integração imediata com o `StyleEngine`.

## M7 — Professor IA & Coaching Inteligente [CONCLUÍDO — v0.7.0]
Motor biomecânico determinístico de 4 dimensões (`MotorDiagnosticEngine`: estabilidade angular $\theta$, contenção de pauta, ritmo/cadência motora e controle de pressão com a S Pen); gerador dinâmico de treino sob medida (`CoachingCurriculumGenerator`); motor de insights pedagógicos, correções e ergonomia em linguagem clara (`CoachingFeedbackEngine`); repositório atômico local (`LocalTeacherRepository`); tela Compose `TeacherScreen` com radar biomecânico e integração direta no caderno.

## M8 — Expansões e Refinamento do Produto [CONCLUÍDO — v0.8.0]
Sistema de backup e restauração atômica offline `.scribepack` (ZIP com manifesto JSON puro), Laboratório de Assinaturas e Monogramas com cálculo determinístico de repetibilidade e exportação vetorial SVG e PNG transparente, Modo Cópia de Textos Longos com pangramas e poesias clássicas em português (WPM caligráfico deliberado), três novos estilos pedagógicos expandidos (Gótica Textura Quadrata, Itálica Chanceleresca e Uncial Clássica), calibrador de curvas de resposta de pressão da S Pen, adaptador Wear OS / Galaxy Watch com alerta postural e haptics de timer, e tela central `ExpansionsScreen`.

