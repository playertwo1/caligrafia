# AGENTS.md — Scribe / Caligrafia

## Missão
Evoluir o Scribe preservando escrita vetorial, dados do usuário, funcionamento local-first e decisões humanas de produto.

## Bootstrap mínimo
Antes de agir, leia apenas:
1. `AGENTS.md`;
2. `PROJECT_STATE.md`;
3. o pedido atual.

Depois carregue somente o contexto exigido pela tarefa, usando `AI_CONTEXT_INDEX.md` e `context-manifest.json`. Não faça scan completo do repositório por padrão.

## Fontes de verdade
- estado operacional: `PROJECT_STATE.md`;
- autorização/aceite vigente: `PHASE_CURRENT.md`;
- produto: `PRODUCT_SPEC.md` + contratos especializados aplicáveis;
- evolução planejada: `ROADMAP.md` e `docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md`;
- invariantes: `INVARIANTS.yaml`;
- contexto: `CONTEXT_POLICY.md` + `AI_CONTEXT_INDEX.md`;
- revisão/gates: `AUDIT.md`;
- risco elevado: `WATCHDOG.md`.

Status histórico nunca substitui o estado atual. `NOT_RUN != PASS`.

## Governança
- Product Authority: usuário.
- Builder: agente que implementa a mudança autorizada.
- Auditor: agente independente do Builder para gates materiais.
- Builder não pode autoaprovar gate material.
- Descoberta não é autorização.
- Mudança de requisito exige decisão humana registrada.

## Regras inegociáveis
- raw strokes nunca são substituídos por bitmap ou dados sintetizados;
- não inventar pressure/tilt/orientation ausentes;
- núcleo permanece local-first/offline;
- sem backend/cloud/IA funcional nova sem autorização explícita;
- sem `WebView` como superfície de escrita;
- domínio não depende diretamente de SDK proprietário; integrações ficam atrás de adapters;
- preservar dados reais e compatibilidade em migrações/restore;
- não usar seeds, mocks, defaults artificiais ou catch silencioso como prova de recurso;
- nenhum secret, token, senha, keystore ou chave privada entra em código, logs ou commits;
- nenhuma alegação de PASS, teste físico, release assinada ou conclusão total sem evidência correspondente.

## Estratégia de execução
`READ → LOCATE → UNDERSTAND → PLAN → CHANGE → VERIFY → AUDIT → UPDATE STATE`

Prefira a menor mudança coerente e reversível. Validação deve ser proporcional ao impacto. Após falhas repetidas semelhantes, reavalie a hipótese em vez de insistir.

## Gatilhos de contexto
- mudança funcional: carregar `PHASE_CURRENT.md`, contrato/arquivos afetados e testes correspondentes;
- dados/persistência/backup/migração: carregar também `WATCHDOG.md` e contratos de dados;
- segurança/credenciais/release: carregar `SECURITY_INCIDENT.md`, workflow e `WATCHDOG.md`;
- fechamento de fase/gate: carregar `AUDIT.md`, evidências e critérios da fase;
- UI: carregar apenas as referências visuais do fluxo afetado;
- histórico: abrir relatórios antigos somente quando um ID/finding concreto exigir.

## Conclusão de tarefa
Registrar, de forma curta:
- arquivos alterados;
- decisão;
- validação executada;
- limitações/bloqueios;
- próxima ação.

Atualize `PROJECT_STATE.md` somente quando o estado operacional mudar.