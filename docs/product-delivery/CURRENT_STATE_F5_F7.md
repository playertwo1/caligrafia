# Estado corrente — F5, F6 e F7

**Data:** 2026-09-13/14  
**Branch de trabalho:** `phase-f6-f7-finalization`  
**Base:** `3e321e1d25adff75552e3042fa4242e97385538e` (`phase-f5-signature-export-recovery`)  
**Política:** não promover para `main` nem declarar gates fechados sem validação executada.

## Estado por fase

### F5 — Assinaturas, exportação e recuperação

A implementação desenvolvida na branch anterior foi preservada como base desta branch: captura/persistência da referência de assinatura, draft restaurável, exportações SVG/PNG, exportação de página, seletores Android, backup/restore com staging/substituição/rollback e testes de regressão adicionados. O validador de pacote também aceita os metadados canônicos de caderno.

**Estado correto:** `IMPLEMENTADA_AGUARDANDO_VALIDACAO_LOCAL`.  
**Gate F5.G:** aberto até executar testes/build e comprovar round-trip/recuperação.

### F6 — Preferências e laboratório

Implementado nesta branch, sem alegar execução de testes:

- contrato central de preferências persistidas com compatibilidade das chaves históricas;
- mão dominante + lado recomendado/override da barra;
- modo global só caneta / caneta + toque integrado à política de palm rejection;
- contraste de pautas aplicado no renderer sem alterar raw;
- curva Linear/Suave/Firme aplicada no render do caderno sem alterar raw;
- relógio de pausa que ignora pausa/background;
- vibração condicionada à preferência/capacidade;
- probe de conexão Watch baseado em dispositivo Bluetooth/GATT realmente conectado, retornando falso sem permissão/sinal;
- correção de pressão/tilt/orientação para preservar zero suportado e separar ausência de capability;
- laboratório continua isolado de aprendizado/SRS/alfabeto e já possui save/load/replay/limpar/autosave;
- testes determinísticos F6 adicionados, mas ainda não executados.

Pendências de F6 registradas em `docs/product-delivery/F6/OPTION_EFFECT_MATRIX.md`: UI completa de Preferências, efeito visual do lado da barra, escala de texto, reduzir animações, preview de contraste, limitar seletor visual às três curvas, integração do lembrete à sessão, permissão runtime Bluetooth, recepção Wear/Data Layer, botão explícito Gravar/Parar no Laboratório, linguagem "a verificar", reinício real e bancada S25 Ultra/Watch.

**Estado correto:** `PARCIAL_SOFTWARE_AVANCADO`.  
**Gate F6.G:** aberto.

### F7 — Consolidação e entrega

Revisão estática iniciada em `docs/product-delivery/F7/STATIC_REVIEW.md`.

Achado principal: `ROUTE_ACTION_MATRIX.md` ainda não enumera individualmente várias ações novas de F5/F6, logo F7.01 não pode ser fechado. Screenshots reais, acessibilidade, jornadas E2E, upgrade, suíte/auditoria/build/lint, APK de entrega e hash permanecem dependentes de execução local/aparelho.

**Estado correto:** `EM_ANDAMENTO_ESTATICO`.  
**Gate F7.G:** aberto.

## Bloqueio de validação nesta sessão

GitHub Actions não pode ser usado por limite mensal informado pelo usuário. Foi tentado clone/execução fora do Actions, mas o container desta sessão não consegue resolver o proxy/rede para acessar o GitHub via `git`, embora o conector GitHub consiga ler e gravar arquivos. Portanto, nenhum novo teste Gradle, build, lint ou APK foi falsamente declarado como executado.

## Próxima ação objetiva

1. Concluir os itens de UI/integradores F6 listados na matriz.
2. Executar `docs/product-delivery/run-local-f5-f7-validation.ps1` em checkout limpo da branch.
3. Corrigir qualquer falha antes de avançar.
4. Executar bancada S25 Ultra/S Pen/Watch, screenshots, acessibilidade, duas jornadas completas e upgrade com acervo anterior.
5. Atualizar `PROJECT_STATE.md`, `EVIDENCE.md`, checklist e matriz com resultados reais do mesmo SHA.
6. Somente após parecer final do Codex considerar merge/promoção para `main`.

## Instrução de retomada

Leia, nesta ordem: `AGENTS.md`, `docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md`, este arquivo, `docs/product-delivery/F6/OPTION_EFFECT_MATRIX.md`, `docs/product-delivery/F7/STATIC_REVIEW.md` e `docs/product-delivery/LOCAL_VALIDATION_PROMPT_F5_F6_F7.md`. Trabalhe na primeira pendência verificável e não replaneje requisitos já LOCKED.
