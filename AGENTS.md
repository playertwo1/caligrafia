# AGENTS.md — Regras para Codex/Astra e outros agentes

## Missão atual
Construir o Scribe incrementalmente. O maior risco é o ink engine; não começar pelas telas finais.

## Regra de execução
1. Leia README, PRODUCT_SPEC, ROADMAP, ARCHITECTURE, STYLUS_ENGINE e PROJECT_STATE.
2. Trabalhe somente no milestone atual.
3. Não avance sem critérios de aceite.
4. Registre decisões relevantes em PROJECT_STATE/ADR.
5. Não introduza backend/cloud no MVP sem requisito explícito.

## Prioridade atual
**M0 — Stylus Lab.**

## Restrições
Kotlin nativo; sem WebView no canvas; raw strokes nunca substituídos por bitmap; não inventar pressure/tilt; domínio não acoplado ao Samsung SDK; integrações Samsung atrás de adapter; nenhuma IA antes do milestone correspondente.

## Qualidade
Build e testes relevantes devem passar; falhas conhecidas são registradas; dependências devem ser justificadas; preferir código simples e observável.

## Ao concluir uma tarefa
Reportar arquivos alterados, decisão, testes, limitações e próxima tarefa recomendada. Atualizar PROJECT_STATE.md.

## Estado de fundação
Este repositório foi criado primeiro como pedra fundamental documental. Não interprete a existência do roadmap como autorização para implementar todos os marcos. Quando o desenvolvimento for explicitamente iniciado, comece por SCR-001 e SCR-002.
