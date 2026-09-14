# STANDARD ADOPTION — Evidence

**Projeto:** Scribe / Caligrafia  
**Change unit:** Ideias Standard adoption v1  
**Branch:** `standard-adoption-v1`  
**Baseline funcional:** `phase-f6-f7-finalization` @ `cdb0fd47704edb5c0903626fb53d36cca05616b1`  
**HEAD de implementação antes deste registro:** `da53fd8ed1f8c015258534aaff4718fbf25173ce`  
**Resultado do gate:** `NOT_RUN` — este documento não aprova a adoção.

## Escopo executado

A adoção elevou governança, contexto, segurança, higiene documental e CI sem alterar comportamento funcional Kotlin.

Materializado:

- perfil Ideias Standard `DEEP`;
- packs `android`, `multi-agent` e `sensitive-data`;
- manifest/lock em `.idea-standard/`;
- `AGENTS.md` com bootstrap mínimo;
- `PROJECT_STATE.md` como resume card;
- `PHASE_CURRENT.md` para autorização/gates;
- `AI_CONTEXT_INDEX.md`, `CONTEXT_POLICY.md` e `context-manifest.json`;
- `INVARIANTS.yaml` e `ARTIFACT_CATALOG.md`;
- política de evidência em `docs/EVIDENCE_POLICY.md`;
- `AUDIT.md` e `WATCHDOG.md` como contextos especializados;
- workflow normal `.github/workflows/ci.yml` separado do workflow de release;
- release fail-closed para material de assinatura, com verificação do APK e SHA-256.

## Segurança

Finding HIGH: existia documentação com credenciais reais de assinatura em texto puro.

Ações executadas:

- arquivo removido do tree ativo;
- `.gitignore` reforçado para material de chave/keystore/.env/logs/results;
- incidente registrado em `SECURITY_INCIDENT.md`;
- buscas históricas nos caminhos esperados do keystore e da cópia Base64 não encontraram commits desses arquivos.

Pendente humano: rotacionar as credenciais expostas em GitHub Secrets/ambiente de release preservando a chave/certificado de assinatura quando a compatibilidade de atualização exigir.

A ausência do arquivo no tree não torna a credencial histórica segura novamente.

## Limpeza controlada

Foram removidos do tree ativo **34 arquivos brutos/reproduzíveis** das auditorias V2–V5, incluindo:

- logs de build/test;
- JUnit XMLs duplicados;
- lint XML/TXT;
- stats/diffs regeneráveis.

Foram preservados:

- matrizes e relatórios Markdown;
- testes/probes/scripts;
- summaries JSON;
- hashes e metadados úteis;
- `acceptance.xml` V4, porque a matriz histórica o referencia diretamente;
- diretórios `baseline*` históricos, por decisão conservadora nesta change unit.

Os baselines históricos permanecem fora do contexto padrão e podem ser consolidados em uma change futura. A recuperação forense de outputs removidos continua possível pelo histórico Git, especialmente na baseline `cdb0fd47704edb5c0903626fb53d36cca05616b1`.

## Verificações estruturais

Comparação `phase-f6-f7-finalization...standard-adoption-v1` no HEAD de implementação:

- status: branch à frente da baseline;
- nenhuma alteração em `app/src/main/**` ou outro arquivo Kotlin foi introduzida pela adoção;
- mudanças funcionais do produto não fazem parte desta change unit;
- F5.G, F6.G e F7.G continuam abertos.

## CI

Workflow criado: `.github/workflows/ci.yml`.

Executa:

1. checkout;
2. JDK 17;
3. Gradle setup;
4. guard contra material sensível versionado;
5. `testDebugUnitTest`;
6. `:app:lintDebug`;
7. `assembleDebug`;
8. upload de test results, lint, APK debug e summary como GitHub Actions artifacts.

O workflow dispara em PR, `main`, `standard-adoption-v1` e manualmente. Concurrency cancela runs antigos da mesma branch.

No momento deste registro, o run mais recente para o HEAD de implementação é:

- run: `34840429488`;
- SHA: `da53fd8ed1f8c015258534aaff4718fbf25173ce`;
- estado observado: `pending`;
- conclusão: ainda não disponível.

Portanto, **ADOPTION_VALIDATION continua NOT_RUN/PENDING e não deve ser convertido em PASS por este documento**.

## Limites

- não houve bancada física S25 Ultra/S Pen/Watch nesta adoção;
- não houve E2E físico;
- rotação de secrets depende de ação humana;
- baselines XML históricos não foram removidos em massa nesta change unit;
- auditoria independente ainda não ocorreu.

## Próxima ação

1. obter conclusão real da CI para o SHA final auditável;
2. se FAIL, corrigir a causa e reexecutar;
3. se PASS, executar `STANDARD_ADOPTION_AUDIT_PACKET.md` com auditor independente;
4. somente após auditoria e decisão explícita da Product Authority considerar merge no `main`.

**Regra:** evidência antes de PASS. `NOT_RUN != PASS`.

## Comparação local HEAD × baseline — 2026-09-14

- HEAD `e92d511`: 303 testes, 293 aprovados, 10 falhas.
- Baseline `cdb0fd4`: 303 testes, 293 aprovados, as mesmas 10 falhas.
- Classificação: 10 `PRE_EXISTING`; 0 `ADOPTION_REGRESSION`; 0 `ENVIRONMENT_SPECIFIC`.
- Grupos afetados: `AuditFixAcceptanceTest` (5), `AuditV5RecheckTest` (1) e `PhaseF4AcceptanceTest` (4).
- Causas observadas: oito NPEs de `applicationContext == null` em doubles JVM e duas asserções de inventário/restore de backup.
- Logs ficaram apenas em `%TEMP%`; XML/outputs gerados não foram versionados.
- `ADOPTION_VALIDATION = FAIL_PRE_EXISTING`; `ADOPTION_AUDIT = NOT_RUN`.
