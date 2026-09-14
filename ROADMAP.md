# ROADMAP — Scribe / Caligrafia

Este documento define **direção, dependências e gates de evolução**. Ele não é fonte de status corrente.

Para saber onde o projeto está agora, use `PROJECT_STATE.md` e `PHASE_CURRENT.md`.

## Regra de autoridade
- produto/decisões: `PRODUCT_SPEC.md` e contratos especializados;
- execução funcional detalhada: `docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md`;
- checklist de entrega: `docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md`;
- estado atual: `PROJECT_STATE.md`;
- gate/revisão: `AUDIT.md`.

Status histórico em documentos antigos nunca substitui o estado atual.

## Linha funcional vigente — F0 a F7

| Fase | Entrega | Gate material |
|---|---|---|
| F0 | Integridade e mapa de ações | dados/manifestos inválidos não corrompem estado; contratos/rotas identificados |
| F1 | Navegação, biblioteca, editor e páginas | percurso de caderno completo e persistente |
| F2 | Aulas, objetivos, treino e feedback | sessão real ponta a ponta ligada a histórico/SRS |
| F3 | Evolução, replay, variantes e estilos | tentativas reais viram comparação/referência pessoal persistente |
| F4 | Professor explicável e cópia de textos | orientação leva ao treino correto; cópia preserva percurso real |
| F5 | Assinaturas, exportação e recuperação | arquivos abrem fora do app; round-trip/restore preserva dados e falha é recuperável |
| F6 | Preferências, S Pen/Watch e laboratório | preferências têm efeito/persistência; hardware/fallback é transparente |
| F7 | Consolidação e entrega | jornadas E2E, acessibilidade, upgrade, preservação de acervo, APK verificável e parecer independente |

Os critérios detalhados permanecem no roadmap funcional/checklist. `PROJECT_STATE.md` informa quais fases/gates estão atualmente abertos ou concluídos.

## Fundação histórica — M0 a M8

Os marcos abaixo descrevem a evolução arquitetural do produto e continuam úteis como mapa de dependências. As marcações históricas de conclusão/versão não devem ser usadas como prova de gate atual.

### M0 — Stylus Lab
Reduzir o maior risco técnico: captura de `ACTION_DOWN/MOVE/UP/CANCEL`, historical samples, tool type, x/y/timestamps, pressure/orientation/tilt quando disponíveis, render em tempo real, palm rejection, persistência e replay. Validar no aparelho-alvo quando o requisito for físico.

### M1 — Caderno
Páginas/sessões, pautas, ferramentas, undo/redo, borracha por stroke, salvar/restaurar e exportar sem substituir os dados vetoriais.

### M2 — Treino Guiado
Glyph de referência, Ghost Mode progressivo, trace → copiar → sozinho, exercícios e feedback determinístico baseado em tentativa real.

### M3 — Style Engine
`ScribeStyle`, estilos pedagógicos, importação TTF/OTF como referência visual, preview e fallback explícito sem inventar ductus.

### M4 — Learning System
Currículo, sessões temporizadas, histórico local e revisão espaçada determinística sem dependência de nuvem/IA.

### M5 — Evolução
Before/after, deltas de métricas, overlay, replay sincronizado, calendário de consistência e persistência atômica das tentativas.

### M6 — Meu Alfabeto
Catálogo de glifos, variantes, favoritos, persistência local e compilação de estilo pessoal a partir de dados reais.

### M7 — Professor / Coaching
Diagnóstico e coaching explicáveis baseados em métricas realmente observadas. Dimensões sem dados suficientes permanecem explícitas como insuficientes, nunca inventadas.

### M8 — Expansões
Backup/restore, assinaturas/monogramas, cópia de textos, estilos adicionais, calibração, Watch/fallback e refinamentos de produto.

## Dependências transversais
- backup/restore depende de contratos de dados e testes de falha/round-trip;
- evolução depende de tentativas reais;
- Professor depende de métricas/contexto corretos;
- estilos pessoais dependem de amostras reais suficientes;
- hardware físico não pode ser certificado por teste JVM;
- release não pode ser certificado por `assembleDebug`;
- ausência de hardware não bloqueia trabalho independente de software, mas mantém o gate físico aberto.

## Referências visuais
`docs/design/scribe-visao-12-telas-v1.png` e `docs/design/fluxos-v1/` orientam UI/fluxos. Use somente a referência do fluxo em trabalho. Mockup não é prova de entrega.

## Evolução após consolidação
Depois de F7 e de uma baseline auditada, melhorias estruturais de código devem ser abertas como change units separadas (por exemplo, decomposição de telas/ViewModels muito grandes), com validação própria. Não misturar refatoração ampla com gates de entrega existentes.

## Regra de avanço
A existência deste roadmap não autoriza executar todas as fases. A autorização corrente está em `PHASE_CURRENT.md`. Nenhum gate muda de `NOT_RUN/OPEN` para `PASS/CLOSED` sem evidência executada e revisão aplicável.