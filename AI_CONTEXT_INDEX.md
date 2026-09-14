# AI_CONTEXT_INDEX

Use este índice para carregar o menor contexto suficiente. Comece sempre por `AGENTS.md`, `PROJECT_STATE.md` e o pedido atual.

| Tipo de tarefa | REQUIRED | CONDITIONAL |
|---|---|---|
| Estado/retomada | `PROJECT_STATE.md`, `PHASE_CURRENT.md` | `ROADMAP.md`, `CHANGELOG.md` |
| Mudança funcional | `PHASE_CURRENT.md`, arquivos afetados, testes correspondentes | `PRODUCT_SPEC.md`, `ARCHITECTURE.md`, contrato especializado |
| Ink/S Pen | arquivos afetados, `STYLUS_ENGINE.md` | `WATCHDOG.md`, referência visual do fluxo |
| Persistência/backup/migração | arquivos afetados, `docs/product-delivery/DATA_CONTRACT.md`, `WATCHDOG.md` | evidências/relatórios históricos do ID concreto |
| UI | tela/VM afetados + referência visual específica | `UI_UX.md`, `docs/design/fluxos-v1/README.md` |
| Segurança/release | workflow/config afetado, `SECURITY_INCIDENT.md`, `WATCHDOG.md` | `SECURITY_PRIVACY.md`, build config |
| Gate/auditoria | `AUDIT.md`, `PHASE_CURRENT.md`, evidência da fase | histórico de auditoria necessário ao finding |
| Standard/governança | `.idea-standard/project-manifest.json`, `.idea-standard/standard.lock`, `INVARIANTS.yaml`, `CONTEXT_POLICY.md` | `ARTIFACT_CATALOG.md` |

## Deep reference
Não carregar por padrão:
- relatórios históricos extensos de auditoria;
- logs/XMLs de execuções passadas;
- todas as 13 imagens de design simultaneamente;
- roadmap completo quando a fase atual já responde à pergunta;
- arquivos de outra fase sem dependência concreta.

## Escalonamento
`REQUIRED → identificar lacuna concreta → CONDITIONAL → dependência específica → DEEP REFERENCE`.

Se o contexto obrigatório ultrapassar o orçamento, não truncar regra crítica: declarar `CONTEXT_OVERFLOW` e dividir a tarefa ou justificar expansão.