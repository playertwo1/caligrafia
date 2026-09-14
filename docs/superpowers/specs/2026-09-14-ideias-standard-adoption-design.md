# Ideias Standard Adoption Design

## Objetivo
Elevar o Scribe/Caligrafia ao padrão de governança, contexto, segurança e evidência do `ideias_standard` sem misturar essa adoção com refatorações funcionais amplas do produto.

## Baseline
- Branch base: `phase-f6-f7-finalization`
- Branch de adoção: `standard-adoption-v1`
- Estado funcional preservado: F5/F6/F7 em consolidação/validação, com gates ainda abertos.
- `main` não é a fonte de trabalho atual desta adoção.

## Estratégia
1. Segurança primeiro: remover credenciais versionadas, documentar rotação e endurecer CI/secrets.
2. Reduzir contexto inicial: `AGENTS.md + PROJECT_STATE.md + tarefa atual`; demais fontes por gatilho.
3. Separar estado dinâmico de histórico: `PROJECT_STATE.md` curto, `PHASE_CURRENT.md` para autorização/aceite vigente, histórico em changelog/evidências.
4. Introduzir contratos estruturados do Standard: manifesto, lock, invariantes, índice/política de contexto e catálogo de artefatos.
5. Limpeza controlada: manter código, testes, specs, scripts e design; arquivar/consolidar relatórios históricos; retirar outputs gerados e secrets da árvore ativa.
6. Separar CI de validação e release. Evidência bruta vira artifact de Actions; o Git preserva resumo estruturado e scripts reproduzíveis.
7. Não alterar comportamento do app nem promover gates F5/F6/F7 nesta adoção.

## Ownership
- `MANAGED`: contratos do Standard e CI de conformance.
- `MERGEABLE`: README, AGENTS, PROJECT_STATE, PHASE_CURRENT, ROADMAP, CHANGELOG, AUDIT, WATCHDOG.
- `USER_OWNED`: produto, código Kotlin, testes funcionais, design assets e decisões de produto.

## Segurança
- Nenhum segredo, senha, keystore, token ou Base64 de chave pode existir no repositório.
- O arquivo histórico com credenciais deve ser removido e seus valores considerados comprometidos.
- Rotação de senhas/segredos deve preservar a chave/certificado de assinatura existente quando necessário para compatibilidade de atualização.
- Release deve falhar de forma explícita quando material de assinatura estiver ausente.

## Contexto
Níveis:
- L0: `AGENTS.md`, `PROJECT_STATE.md`, solicitação atual.
- L1: `PHASE_CURRENT.md` e arquivos diretamente relacionados à tarefa.
- L2: dependências concretamente descobertas.
- L3: roadmap histórico, auditorias antigas, relatórios extensos e referências profundas.

Nunca truncar silenciosamente regra crítica. Contexto extra exige motivo de inclusão.

## Limpeza
### KEEP
Código, testes, contratos de produto, scripts reproduzíveis, matrizes finais, documentação canônica e design assets.

### CONSOLIDATE/ARCHIVE
Relatórios históricos de auditoria e narrativas superseded que ainda possuem valor de rastreabilidade.

### REMOVE FROM ACTIVE TREE
Secrets, logs gerados, XMLs de testes/lint, outputs reproduzíveis e duplicações sem autoridade própria.

Histórico Git preserva material removido; a branch baseline permanece intacta.

## CI
`ci.yml`: testes, lint, assembleDebug, checks de segurança/governança e artifacts de evidência.

`build-release.yml`: apenas tag/manual, validação prévia, material de assinatura obrigatório, assembleRelease, verificação/hashes e publicação.

## Gates
A adoção só pode ser considerada pronta para merge quando:
- nenhum secret conhecido permanecer na árvore ativa;
- contexto/governança estiverem coerentes e sem status contraditório;
- limpeza não remover fonte canônica ou asset necessário;
- CI de validação existir e for executável;
- diff receber auditoria independente.

A adoção não fecha automaticamente F5.G, F6.G ou F7.G.