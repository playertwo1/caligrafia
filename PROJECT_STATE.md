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
- Aparelho-alvo: Samsung Galaxy S25 Ultra com S Pen original.
- Uso de SDKs Samsung solicitado pelo usuário: identificar e integrar recursos oficialmente compatíveis no M0, atrás de adapters; registrar caso nenhum SDK seja aplicável.
- S Pen Remote SDK não serve como motor de tinta; a S Pen do S25 Ultra não oferece Bluetooth/funções remotas. Captura permanece baseada em `MotionEvent`.
- Direção aprovada: superfície nativa, telas Compose e Android Ink API como primeira opção no M0. Comparar com referência em Compose Canvas ou View customizada no S25 Ultra; adoção final condicionada aos critérios de latência, estabilidade, dados brutos, persistência e replay.

## Última atualização
2026-09-12 — documentação v0.1 publicada no GitHub como pedra fundamental.

2026-09-12 — ROADMAP, AGENTS e ARCHITECTURE alinhados à preferência por SDK Samsung e ao aparelho-alvo S25 Ultra. Compatibilidade de um SDK de escrita ainda não comprovada; validação no aparelho e benchmark pendentes do M0. Alteração apenas documental, sem build/testes de código. Próxima tarefa de implementação permanece SCR-001, seguida de SCR-002.

2026-09-12 — Usuário aprovou a recomendação de avaliar primeiro a Android Ink API em superfície nativa no S25 Ultra. README, ROADMAP, AGENTS, ARCHITECTURE, STYLUS_ENGINE, BACKLOG e REFERENCES alinhados. Decisão de direção, ainda sem protótipo, SDK selecionado ou benchmark. Verificação documental por revisão de consistência e git diff --check; build/testes de código não se aplicam. Próxima tarefa permanece SCR-001, seguida de SCR-002; implementação não iniciada por esta alteração.
