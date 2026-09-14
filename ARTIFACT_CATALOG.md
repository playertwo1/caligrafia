# ARTIFACT_CATALOG

## Ownership model
- `MANAGED`: controlado pelo Ideias Standard; mudanças locais exigem atualização consciente do contrato.
- `MERGEABLE`: Standard fornece estrutura, mas conteúdo do projeto evolui localmente.
- `USER_OWNED`: fonte de produto/código criada para o Scribe; nunca sobrescrever automaticamente.

## Catálogo ativo

| Artefato/padrão | Ownership | Papel |
|---|---|---|
| `.idea-standard/project-manifest.json` | MANAGED | contrato de adoção |
| `.idea-standard/standard.lock` | MANAGED | versão/proveniência/ownership |
| `context-manifest.json` | MANAGED | roteamento de contexto |
| `INVARIANTS.yaml` | MERGEABLE | invariantes Standard + Scribe |
| `AGENTS.md` | MERGEABLE | constituição operacional |
| `PROJECT_STATE.md` | MERGEABLE | resume card do estado atual |
| `PHASE_CURRENT.md` | MERGEABLE | escopo/aceite/gates vigentes |
| `AI_CONTEXT_INDEX.md` | MERGEABLE | rotas humanas de contexto |
| `CONTEXT_POLICY.md` | MERGEABLE | orçamento e escalonamento |
| `README.md` | USER_OWNED | entrada humana do projeto |
| `PRODUCT_SPEC.md` | USER_OWNED | contrato de produto |
| `ARCHITECTURE.md` | USER_OWNED | arquitetura do produto |
| `STYLUS_ENGINE.md` | USER_OWNED | contrato do motor de escrita |
| `STYLE_ENGINE.md` | USER_OWNED | contrato de estilos |
| `LEARNING_SYSTEM.md` | USER_OWNED | contrato pedagógico |
| `DATA_MODEL.md` | USER_OWNED | modelo de dados |
| `SECURITY_PRIVACY.md` | USER_OWNED | segurança/privacidade do produto |
| `UI_UX.md` | USER_OWNED | direção de UX |
| `ROADMAP.md` | MERGEABLE | evolução funcional; estado dinâmico fica fora |
| `AUDIT.md` | MERGEABLE | protocolo de revisão/gate |
| `WATCHDOG.md` | MERGEABLE | guardrails de alto risco |
| `CHANGELOG.md` | USER_OWNED | mudanças relevantes |
| `docs/EVIDENCE_POLICY.md` | MERGEABLE | retenção, reprodução e limpeza de evidência |
| `docs/design/**` | USER_OWNED | referências visuais aprovadas |
| `app/src/main/**` | USER_OWNED | código de produção |
| `app/src/test/**` | USER_OWNED | testes de produto/auditoria |
| `docs/product-delivery/**` | USER_OWNED | contratos/evidências funcionais correntes |

## Histórico e evidência
Relatórios históricos em Markdown podem permanecer quando contêm findings/decisões não deriváveis. Logs, XMLs de teste/lint, outputs de build e resultados reproduzíveis não são fontes canônicas: devem preferir CI artifacts e podem ser removidos da árvore ativa. A política detalhada e a referência para recuperação histórica estão em `docs/EVIDENCE_POLICY.md`.

## Regra de upgrade
- `MANAGED`: preview + diff antes de substituir.
- `MERGEABLE`: three-way merge/conciliação sem apagar decisões locais.
- `USER_OWNED`: nunca sobrescrever automaticamente.

## Regra de limpeza
Antes de remover qualquer artefato, classificar: `KEEP`, `CONSOLIDATE/ARCHIVE` ou `REMOVE_FROM_ACTIVE_TREE`. Em dúvida, preservar e registrar finding.