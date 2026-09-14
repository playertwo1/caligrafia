# STANDARD ADOPTION — Independent Audit Packet

## Papel

Este pacote é para auditor independente. O Builder não pode usar este documento para autoaprovar a adoção.

## Escopo

Auditar exclusivamente a adoção do Ideias Standard em `standard-adoption-v1` contra a baseline funcional:

`phase-f6-f7-finalization` @ `cdb0fd47704edb5c0903626fb53d36cca05616b1`

A auditoria não reabre requisitos funcionais F0–F7 sem finding concreto e não fecha F5.G/F6.G/F7.G.

## Bootstrap mínimo

Ler primeiro:

1. `AGENTS.md`;
2. `PROJECT_STATE.md`;
3. `PHASE_CURRENT.md`;
4. `STANDARD_ADOPTION_EVIDENCE.md`;
5. este pacote.

Depois usar `AI_CONTEXT_INDEX.md`/`context-manifest.json` para expandir somente o contexto necessário.

## Checks obrigatórios

### 1. Escopo e integridade

- comparar baseline vs branch candidata;
- confirmar que a adoção não alterou Kotlin/comportamento funcional;
- confirmar que Product Authority, Builder e Auditor continuam separados;
- confirmar `NOT_RUN != PASS`;
- confirmar que gates F5.G/F6.G/F7.G permanecem abertos.

### 2. Segurança

- confirmar remoção do arquivo de credenciais do tree ativo;
- revisar `.gitignore` e `SECURITY_INCIDENT.md`;
- confirmar que nenhum valor secreto foi copiado para novos documentos;
- tratar rotação humana pendente como bloqueio operacional do release confiável, não como motivo para apagar/changer a chave de assinatura sem decisão.

### 3. Contexto/governança

- `AGENTS.md + PROJECT_STATE.md + pedido atual` devem ser bootstrap suficiente;
- estado corrente deve ter fonte primária única;
- `PHASE_CURRENT.md` deve conter autorização/gates atuais;
- contexto adicional deve ser progressivo;
- `AUDIT.md` e `WATCHDOG.md` não devem ser obrigatórios em toda tarefa, mas seus gatilhos não podem ser perdidos;
- nenhum orçamento pode truncar regra REQUIRED silenciosamente.

### 4. Ownership/lifecycle

- validar `.idea-standard/project-manifest.json` e `.idea-standard/standard.lock` por coerência estrutural;
- revisar `ARTIFACT_CATALOG.md` e classes MANAGED/MERGEABLE/USER_OWNED;
- confirmar que o Standard não ganhou autoridade sobre código/produto USER_OWNED.

### 5. Evidência e limpeza

- revisar `docs/EVIDENCE_POLICY.md`;
- confirmar que remoções se limitaram a outputs brutos/reproduzíveis ou claramente redundantes;
- confirmar preservação das matrizes, summaries, scripts/probes, hashes e evidência não reproduzível;
- confirmar que outputs históricos removidos seguem recuperáveis pelo Git;
- diretórios `baseline*` históricos ainda presentes não são finding bloqueante desta change unit, desde que fora do contexto padrão e sem autoridade canônica.

### 6. CI/release

- revisar `.github/workflows/ci.yml`;
- revisar `.github/workflows/build-release.yml`;
- confirmar separação entre validação normal e assinatura/release;
- confirmar fail-closed do release quando secrets estão ausentes;
- verificar resultado real da CI para o SHA auditado;
- `pending`, `cancelled`, `skipped` ou ausência de run não equivalem a PASS.

## Severidade

- CRITICAL: perda de dados, segredo ativo exposto, bypass destrutivo ou autoridade/gate comprometido;
- HIGH: regressão material, secret handling inseguro, mudança funcional fora do escopo;
- MEDIUM: inconsistência real de governança/evidência que pode induzir agente ao erro;
- LOW: melhoria não bloqueante.

CRITICAL/HIGH bloqueiam a adoção. MEDIUM bloqueia quando afeta autoridade, segurança, evidência, integridade ou gate.

## Saída obrigatória

```text
AUDIT RESULT: PASS | FAIL
SHA auditado:
Baseline:
CI observada:
Findings:
Limites:
Correções exigidas:
Próxima ação:
```

PASS desta auditoria significa apenas que a change unit de adoção atende ao seu escopo. **Não aprova F5.G, F6.G, F7.G nem o produto inteiro.** Merge no `main` continua dependente da Product Authority.