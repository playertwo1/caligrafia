# AUDIT — Revisão final de alterações do Scribe

Adaptado do checklist da pasta GUARDRAILS do usuário. [Origem e adaptações](docs/GUARDRAILS_INTEGRATION.md). Aplicar após alterações relevantes; escopo proporcional ao pedido.

## Método

Tentar refutar a solução: como ela falha, perde dados ou altera outro fluxo? O autor pode fazer autorrevisão, mas deve identificá-la como tal. Não alegar independência por adotar este checklist.

| Área | Verificação |
|---|---|
| Escopo | Pedido e requisito original atendidos? Funcionalidades removidas, dependências ou refatorações extras têm justificativa? |
| Diff | Cada arquivo/hunk está ligado à tarefa? Há remoções acidentais, duplicação, código morto, TODOs, hardcodes ou logs temporários? |
| Regressão | Impacto em chamadas, UI, arquivos, schema, lifecycle, permissões, integrações, background e compatibilidade? |
| Qualidade | Erros observáveis, nullability, casos vazios, limites, cancelamento, concorrência e alternativa mais simples? |
| Dados | Caminhos reais coincidem? Reabertura mantém strokes/contexto? Falha parcial preserva estado e inventário anteriores? |
| Segurança | Secrets/dados privados em diff/logs? Inputs validados? Permissões mínimas? Operações perigosas têm autorização e recuperação? |
| Git | SHA/branch/status conferidos? Trabalho anterior preservado? Arquivos grandes e artefatos necessários e revisados? |
| Atalhos | Catch vazio, sucesso em falha, mocks/seeds em produção, testes relaxados, recurso desligado ou validação removida? |
| Evidência | Testa o código e chamador de produção? Simulação no teste substituiu ligação inexistente no aplicativo? |
| Documentação | IDs originais preservados? Estado, matriz e resultado correspondem ao código? Pendências físicas continuam pendentes? |

## Validação

Selecionar verificações pelo impacto conforme WATCHDOG. Para mudanças de código, comandos de referência:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest --console=plain
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat assembleDebug --console=plain
.\gradlew.bat :app:lintDebug --console=plain
```

Para investigar os aceites adversariais da V4:

```powershell
.\docs\audit-v4\independent\run-independent-tests.ps1 -AndroidSdk $env:ANDROID_HOME
```

Na base V4, os 16 aceites independentes falham; esse é o baseline conhecido, não motivo para desativá-los. Registrar resultado atual por teste e ID sem substituir evidências históricas. Falha nova é regressão; falha histórica deve permanecer explícita. Corrigir apenas os IDs autorizados na tarefa.

Documentação sem código requer validação de diff/links/coerência, não build automático por ritual. Testes JVM sem Android não comprovam pixels/eventos/lifecycle. Build debug não comprova assinatura. Registrar comando, resultado/exit code, SHA, ambiente e limites; nunca converter execução ausente em PASS.

## Severidade e resultado

- **CRITICAL:** perda de dados, vulnerabilidade ou comportamento destrutivo grave.
- **HIGH:** funcionamento incorreto importante ou regressão relevante.
- **MEDIUM:** problema real com impacto delimitado.
- **LOW:** melhoria sem bloqueio do comportamento avaliado.

A severidade complementa o ID/prioridade histórica; não os substitui. Para cada problema registrar local, reprodução/evidência, impacto e correção recomendada.

Usar o registro abaixo em PROJECT_STATE ou no relatório da tarefa:

```text
AUDIT RESULT: PASS | FAIL
Escopo: alteração/requisito e SHA base
Tipo de revisão: autorrevisão | independente (identificar auditor)
Arquivos e justificativa:
Validações e evidências:
Problemas novos/regressões: severidade, local, impacto, correção
Pendências históricas fora do escopo: IDs e referência
Limites/validações não executadas:
Próxima ação:
```

PASS significa que a alteração delimitada atendeu ao aceite e não apresentou problema relevante nas verificações realizadas. **Não significa ausência de todo bug ou aprovação geral do Scribe.** Requisito da tarefa não atendido, regressão ou verificação indispensável sem evidência impede PASS; explicar o que falta. A revisão documental pode passar enquanto o produto mantém a reprovação V4.

Fechamento de onda/milestone exige critérios próprios e revisão correspondente. Testes verdes, contagens, aparência de telas e este checklist não substituem essas provas.
