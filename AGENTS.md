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

## Aparelho-alvo e integração Samsung
- Aparelho principal: Samsung Galaxy S25 Ultra com sua S Pen original. Validar capacidades e desempenho no aparelho real.
- O usuário solicitou utilizar SDK Samsung. Identificar e utilizar SDKs Samsung oficialmente compatíveis quando oferecerem recursos aplicáveis à escrita/S Pen; documentar SDK, versão, licença, capacidades e limitações no M0.
- Manter integrações atrás de adapter para preservar o modelo de strokes. Essa separação não proíbe o uso de SDK Samsung.
- Não confundir S Pen Remote SDK com motor de escrita. A S Pen do S25 Ultra não possui Bluetooth nem suporta funções remotas; não adicionar esse SDK como requisito de captura de tinta. Se não houver SDK Samsung aplicável, documentar a limitação e usar as APIs Android de stylus.
- Direção aprovada pelo usuário: superfície nativa com Android Ink API como primeira opção a avaliar no M0; Jetpack Compose para telas. Comparar no S25 Ultra com uma referência em Compose Canvas ou View customizada e usar alternativa se necessário. Validar latência, estabilidade, preservação de dados e replay antes de confirmar a escolha. Não presumir que a aprovação desta direção equivale a benchmark concluído.

## Restrições
Kotlin nativo; sem WebView no canvas; raw strokes nunca substituídos por bitmap; não inventar pressure/tilt; domínio não acoplado ao Samsung SDK; integrações Samsung atrás de adapter; nenhuma IA antes do milestone correspondente.

## Qualidade
Build e testes relevantes devem passar; falhas conhecidas são registradas; dependências devem ser justificadas; preferir código simples e observável.

## Ao concluir uma tarefa
Reportar arquivos alterados, decisão, testes, limitações e próxima tarefa recomendada. Atualizar PROJECT_STATE.md.

## Estado de fundação
Este repositório foi criado primeiro como pedra fundamental documental. Não interprete a existência do roadmap como autorização para implementar todos os marcos. Quando o desenvolvimento for explicitamente iniciado, comece por SCR-001 e SCR-002.
