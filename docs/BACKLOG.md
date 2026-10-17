# BACKLOG

## EPIC M0 — Stylus Lab
- **SCR-001 Bootstrap:** projeto Android; APK debug instala e abre.
- **SCR-002 Device Capability Inspector:** inspecionar tool type e motion ranges no S25 Ultra com S Pen original; capabilities sem crash. Identificar SDKs Samsung aplicáveis e registrar suporte/limitações; verificar requisitos da Android Ink API.
- **SCR-003 Capture Pipeline:** DOWN/MOVE/UP/CANCEL + historical samples; stroke completo em memória.
- **SCR-004 Live Renderer:** prototipar primeiro com Android Ink API em superfície nativa; tinta em tempo real + smoothing derivado sem alterar pontos brutos. Integrar com telas Compose e comparar com referência em Compose Canvas ou View customizada no S25 Ultra. Registrar versão/requisitos da biblioteca e limitações de integração.
- **SCR-005 Stylus-only + Palm Rules:** dedo não gera tinta no modo de escrita.
- **SCR-006 Persistence Spike:** comparar tabela/blob/arquivo; relatório e decisão.
- **SCR-007 Replay:** 0.5x/1x/2x preservando timing relativo.
- **SCR-008 Lifecycle:** sessão sobrevive a background/process recreation conforme política.
- **SCR-009 Benchmark:** medir latência/estabilidade da escrita, memória e tamanho em sessão longa no S25 Ultra. Comparar Ink API com a referência nativa usando os mesmos exercícios e condições; registrar método, resultados e eventuais perdas de pontos.
- **SCR-010 Gate M0:** confirmar Ink API ou justificar alternativa com os resultados do benchmark; fechar canvas/persistência e limitações antes do M1. Validar pontos brutos, reload e replay; registrar integrações Samsung suportadas ou indisponíveis.

## M1 — Notebook
SCR-101 guias; SCR-102 undo/redo; SCR-103 eraser; SCR-104 pages; SCR-105 export.

## M2 — Guided Practice
SCR-201 reference glyph; SCR-202 Ghost Mode; SCR-203 repetition flow; SCR-204 exercise state.

## M3 — Styles
SCR-301 schema; SCR-302 parser; SCR-303 3 bundled packs; SCR-304 TTF/OTF visual import.

## M4 — Learning
SCR-401 curriculum; SCR-402 planner; SCR-403 review rules; SCR-404 history.

## M5 — Progress
SCR-501 before/after; SCR-502 overlay; SCR-503 dual replay; SCR-504 calendar.

## M6 — My Alphabet
SCR-601 preferred glyph; SCR-602 versions; SCR-603 personal pack.

## M7 — AI Teacher
Somente após dados reais e aprovação explícita da arquitetura.
