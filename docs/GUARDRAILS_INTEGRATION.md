# Integração dos guardrails do Google Drive

Data: 2026-09-13. Base local: `e631cb7`. Escopo: adaptar regras de execução/revisão ao Scribe, sem modificar produto ou fechar achados V4.

Encontrados e lidos integralmente os três arquivos Markdown da [pasta GUARDRAILS](https://drive.google.com/drive/folders/12JieR_wGOPqdBmWmJdHRqquLFgjF0fza), todos com última modificação informada em 2026-09-13. Os originais do Drive foram preservados; não há sincronização automática.

| Fonte | Aproveitamento no projeto |
|---|---|
| [AGENTS_GUARDRAILS.md](https://drive.google.com/file/d/1nPdaQRnQH9ROBe_WzvYDJFH9BYmpln_x/view) | Leitura prévia, controle de escopo, preservação do trabalho e ligação de [AGENTS](../AGENTS.md) a WATCHDOG/AUDIT |
| [WATCHDOG.md](https://drive.google.com/file/d/1zH3fYkqrIIQ_qgjubwzdwUguQw9xoy7J/view) | Autonomia com mudanças pequenas, riscos, recuperação, segurança, revisão após falhas repetidas e dependências justificadas em [WATCHDOG](../WATCHDOG.md) |
| [AUDIT.md](https://drive.google.com/file/d/1-n9yn6S66o06oaRg9aqZyesZVvvOvxhm/view) | Revisão adversarial de escopo/diff/regressão/segurança, caça a atalhos, severidades e PASS/FAIL em [AUDIT](../AUDIT.md) |

## Adaptações necessárias

- Corrigida a prioridade antiga de WATCHDOG, que ainda mandava trabalhar somente em M0; agora referencia estabilização e V4.
- AGENTS aponta a auditoria mais recente e preserva contenção, Samsung, raw strokes e demais regras específicas.
- Acrescentados contratos de diretórios/estado compartilhado, ausência versus zero, contexto de métricas e transação/recuperação, derivados dos problemas V4.
- Validação proporcional: documentação usa diff/links/coerência; código e integrações exigem seus testes. Não há dispensa de verificação relevante.
- PASS de tarefa é delimitado. Autorrevisão não ganha independência; defeitos históricos não impedem concluir tarefa documental, mas continuam bloqueando os gates correspondentes.
- Regra de parada aplicada à decisão que exige informação/autorização ainda ausente; não cria reconfirmação para trabalho já autorizado e reversível.
- A hierarquia genérica da fonte não substitui as instruções superiores do ambiente. Documentos externos continuam sendo referências.
- O script existente não ganhou capacidades apenas por atualizar documentos. Limites de lint, assinatura, frescura do artefato e integração permanecem explicitados em WATCHDOG.

## Incrementos futuros dentro da estabilização

S22/R20: aperfeiçoar `scripts/watchdog.ps1` para relatório por verificação, artefato da execução atual e ausência de aprovação global enganosa; incluir validações pertinentes. S21/A19: bloquear distribuição sem assinatura verificada. Esses trabalhos demandam alteração/teste de scripts e CI próprios e **não foram implementados por esta integração documental**.
