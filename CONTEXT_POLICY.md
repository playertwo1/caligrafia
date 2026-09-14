# CONTEXT_POLICY

## Objetivo
Entregar a cada agente o menor contexto suficiente para executar corretamente sem perder requisito, decisão, risco ou critério de aceite material.

## Princípios
- precisão e integridade vencem economia de tokens;
- contexto é progressivo, não um dump do repositório;
- toda inclusão deve ter motivo concreto;
- resumo nunca substitui a fonte canônica quando a decisão depende do detalhe;
- regra crítica REQUIRED nunca é truncada silenciosamente;
- projeto/fluxo não relacionado não entra no contexto.

## Níveis
- **L0 Bootstrap:** `AGENTS.md`, `PROJECT_STATE.md`, pedido atual.
- **L1 Task:** `PHASE_CURRENT.md` e arquivos diretamente ligados à tarefa.
- **L2 Dependency:** dependências concretas encontradas durante investigação.
- **L3 Deep Reference:** roadmap histórico, auditorias extensas, pesquisa, logs históricos, imagens adicionais.

## Budgets recomendados
São metas, não limites de segurança:
- bootstrap: até ~12 KB;
- contexto inicial da tarefa: até ~25 KB;
- saída inicial de comando: até ~6 KB por comando;
- logs: erro + janela curta antes/depois.

Pode exceder quando necessário para correção, segurança ou integridade. Registre a razão.

## Saídas de comandos
Prefira filtros, testes direcionados e recortes. Em shell, quando apropriado:

```bash
COMMAND 2>&1 | head -c 6000
```

Amplie somente se o recorte não contiver a causa necessária.

## Delta context
Quando uma base conhecida não mudou, reapresente apenas o delta relevante, com fingerprint/revisão da base. Alteração em fonte dependente marca o contexto anterior como STALE.

## Inclusão/exclusão
Cada bloco de contexto deve ser classificável como:
- `REQUIRED` — indispensável para objetivo/aceite/segurança;
- `CONDITIONAL` — abrir se um gatilho concreto ocorrer;
- `DISCOVERY` — usado apenas para localizar dependência;
- `EXCLUDED` — fora de fase, superseded, histórico reproduzível ou não relacionado.

## Overflow
Se todo contexto REQUIRED não couber no orçamento:
1. não truncar silenciosamente;
2. emitir `CONTEXT_OVERFLOW`;
3. dividir a tarefa, ou expandir com justificativa explícita.

## Evidência
Para revisão, prefira diff da tarefa, testes relevantes, resumo de CI e links/IDs para artefatos brutos. Não carregue todos os logs/XMLs por padrão.