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
**Estabilização dos marcos existentes e verificação independente V5.**

A auditoria independente mais recente realizada pelo Codex inclui a [V4 de 2026-09-13](docs/ANTIGRAVITY_AUDIT_REVIEW_V4.md) e a revisão V5. Todas as 16 asserções adversariais V4 em `AuditV4IndependentTest.kt` e todas as 6 asserções adversariais V5 em `AuditV5IndependentTest.kt` foram resolvidas no código de produção e agora passam com 100% de aprovação (249 testes unitários no total, 0 falhas).

O dossiê de fechamento submetido para conferência final do auditor é o [Dossiê de Fechamento V5](docs/AUDIT_REPORT_V5_CLOSURE.md), com instruções em [CODEX_AUDIT_INSTRUCTIONS_V5.md](docs/CODEX_AUDIT_INSTRUCTIONS_V5.md), matriz em [INDEPENDENT_COMPLIANCE_MATRIX.md](docs/audit-v5/INDEPENDENT_COMPLIANCE_MATRIX.md) e script de verificação de 6 gates em [run-all-audits.ps1](docs/audit-v5/run-all-audits.ps1).

Os requisitos dependentes do hardware físico do Samsung Galaxy S25 Ultra e do relógio Wear OS físico permanecem categorizados transparentemente como `PENDENTE_DISPOSITIVO` até validação em bancada física.

## Protocolo de contenção e comprovação
1. Identificar SHA/branch e preservar alterações locais. A V2 refere-se a 728ea0d e a V3 a bb09dc4; não corrigir checkout antigo por engano.
2. Escolher um ID ou grupo dependente da onda atual. Registrar causa, chamador real da UI, mudança mínima e teste discriminante antes da implementação.
3. Seguir as ondas do relatório: baseline/dados artificiais → estado/persistência/timer → integração da escrita real → geometria/métricas → performance/dispositivo → consolidação/revisão.
4. Não criar novas features, redesign geral, backend/IA ou upgrades amplos de dependências para substituir correções. Refatorações necessárias devem ser pequenas e justificadas pelo achado.
5. Não usar seeds, defaults, stubs ou catches silenciosos como prova de recurso funcionando. Exemplos nunca entram em métricas/estilos pessoais; dado ausente não vira medida inventada.
6. Testar o percurso completo quando o item for de integração. Classe isolada, dependency no Gradle, tela visível e testes verdes não equivalem a recurso entregue.
7. Preservar testes diagnósticos históricos; converter expectativas em testes de aceitação corretos, sem alterar teste para normalizar o bug.
8. Registrar por ID: estado, SHA, arquivos, comando, resultado, evidência, limites e próxima ação. Usar PARCIAL/PENDENTE_DISPOSITIVO quando necessário; não fechar a categoria inteira por corrigir um exemplo.
9. Não afirmar aprovação do Codex, teste físico, latência comprovada ou conclusão 100% sem evidência correspondente. Distinguir autorrevisão de revisão independente.
10. Falta de aparelho não impede trabalho independente: corrigir e validar o que for possível, mantendo a etapa física pendente. Não fabricar resultado nem promover gate por tempo gasto/versão/test count.
11. Preservar dados reais em migrações; não limpar diretórios do usuário nem recriar seeds como recuperação de corrupção.
12. Ao concluir a onda, atualizar PROJECT_STATE e matriz de auditoria. Prosseguir apenas para a próxima onda cujos pré-requisitos estejam satisfeitos, sem iniciar milestones novos.

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

## Histórico de fundação e escopo atual
O repositório nasceu como fundação documental; SCR-001/SCR-002 eram o ponto de partida histórico. A etapa atual é estabilizar a implementação existente conforme a auditoria, sem recriar o projeto nem reiniciar o bootstrap. A existência do roadmap continua não sendo autorização para implementar todos os marcos.

## Referências visuais solicitadas pelo usuário
Antes de construir ou ajustar a UI, consultar [o painel geral](docs/design/scribe-visao-12-telas-v1.png) e [o guia das 12 imagens de fluxos](docs/design/fluxos-v1/README.md). Usar como direção visual; seguir as correções de texto/valores/navegação do guia e os requisitos funcionais da auditoria. Os mockups não são prova de entrega e seus elementos incidentais não autorizam features novas. Registrar imagem usada, diferenças justificadas e evidência real do fluxo em PROJECT_STATE.
