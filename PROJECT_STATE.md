# PROJECT_STATE

**Projeto:** Scribe / Caligrafia
**Versão documental:** v0.1
**Estado:** FOUNDATION_READY — aguardando início futuro de implementação.
**Milestone preparado:** M0 — Stylus Lab.
**Código de produto:** ainda não iniciado.

## Pedra fundamental
A visão, arquitetura, motor de stylus, sistema de estilos, pedagogia, dados, UX, privacidade, roadmap e backlog foram documentados antes da implementação.

## Próxima ação quando o projeto for retomado
1. Ler toda a documentação.
2. Executar SCR-001 — Bootstrap Android.
3. Executar SCR-002 — Device Capability Inspector no aparelho-alvo.
4. Só então avançar para Capture Pipeline e Live Renderer.

## Gates
Não iniciar IA, backend, login, marketplace ou biblioteca avançada de estilos antes do Gate M0.

## Decisões já tomadas
- Kotlin nativo + Jetpack Compose.
- `MotionEvent` como fundação de input.
- raw strokes imutáveis.
- pressure/tilt/orientation somente quando disponíveis.
- local-first.
- TTF/OTF visual ≠ `ScribeStyle` pedagógico.
- Samsung-specific é opcional/adaptado.

## Última atualização
2026-09-12 — documentação v0.1 publicada no GitHub como pedra fundamental.
