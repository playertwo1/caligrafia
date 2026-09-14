# AUDIT — Revisão independente do Scribe

Use este contrato quando houver fechamento de tarefa material, fase, gate, release, migração ou revisão independente. Não é leitura obrigatória para toda tarefa trivial.

## Papel
O Auditor tenta **refutar** a solução. Builder pode fazer autorrevisão, mas não pode chamar isso de auditoria independente nem autoaprovar gate material.

## Entrada mínima
1. `PROJECT_STATE.md`;
2. `PHASE_CURRENT.md`;
3. diff/arquivos da mudança;
4. critérios de aceite específicos;
5. evidência executada relevante.

Abra histórico antigo somente se um ID/finding concreto exigir.

## Checklist
| Área | Pergunta |
|---|---|
| Escopo | O pedido/aceite foi atendido sem expansão não autorizada? |
| Diff | Cada hunk é necessário? Há remoção acidental, duplicação, TODO, hardcode ou código morto? |
| Regressão | Chamadores, UI, persistência, lifecycle, permissões e compatibilidade foram considerados? |
| Dados | Falha parcial, reabertura, backup/restore e migração preservam o estado anterior? |
| Segurança | Há secret/dado privado em código, log, artifact ou diff? Inputs/permissões são mínimos? |
| Evidência | O teste exercita o código/chamador real? O requisito físico foi validado fisicamente quando necessário? |
| Atalhos | Há catch vazio, sucesso falso, seed/mock em produção, teste relaxado ou validação removida? |
| Governança | `NOT_RUN != PASS`? Builder e Auditor são distintos no gate material? |
| Git | SHA/branch corretos e baseline preservada? |

## Validação proporcional
Para mudança Kotlin/Android, selecionar comandos de acordo com o impacto. Referência:

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat :app:lintDebug --console=plain
.\gradlew.bat assembleDebug --console=plain
```

Use testes direcionados antes da suíte completa quando apropriado. Release exige validação de assinatura; hardware/lifecycle/pixels não são comprovados por teste JVM.

Mudança puramente documental/config requer coerência, links, schema/parsing quando aplicável e diff; não execute build por ritual se ele não acrescentar evidência.

## Severidade
- `CRITICAL`: perda de dados, secret/chave, vulnerabilidade, corrupção ou ação destrutiva grave.
- `HIGH`: requisito material incorreto, regressão relevante ou gate promovido sem prova.
- `MEDIUM`: problema real delimitado, drift ou inconsistência que pode induzir execução errada.
- `LOW`: melhoria de qualidade sem bloquear o aceite.

## Resultado
Formato mínimo:

```text
AUDIT RESULT: PASS | FAIL
Scope:
Auditor:
Base SHA / target SHA:
Acceptance checked:
Evidence executed:
Findings:
Unexecuted/physical validations:
Historical items outside scope:
Next action:
```

`PASS` vale apenas para o escopo auditado. Não significa ausência de bugs nem aprovação geral do produto.

## Condições de FAIL
- requisito material não atendido;
- regressão relevante;
- evidência indispensável ausente;
- secret/dado sensível exposto;
- gate promovido sem prova;
- auditoria dita independente feita pelo mesmo Builder;
- teste substitui integração inexistente por ligação artificial.

## Regra de gate
Auditor emite PASS/FAIL. A Product Authority decide promoção quando o processo exigir decisão humana. `NOT_RUN != PASS`.