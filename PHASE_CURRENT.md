# PHASE_CURRENT — Standard Adoption v1

## Objetivo da fase
Elevar governança, contexto, segurança, CI e higiene documental do Scribe sem alterar comportamento funcional do aplicativo.

## Escopo autorizado
- adoção do Ideias Standard;
- compactação e consolidação documental;
- criação de manifestos/invariantes/política de contexto;
- remoção de secrets e artefatos gerados sem autoridade própria;
- reorganização de evidência histórica;
- CI de validação e endurecimento de release;
- documentação de findings existentes.

## Fora do escopo desta fase
- novas features;
- redesign funcional;
- refatoração ampla de Kotlin;
- alteração de formatos de dados do usuário;
- migrações funcionais;
- mudança de versão/versionCode do app sem decisão separada;
- fechamento de F5.G/F6.G/F7.G;
- introdução de IA/backend/cloud no produto.

## Baseline
`phase-f6-f7-finalization` @ `cdb0fd47704edb5c0903626fb53d36cca05616b1`.

## Critérios de aceite
1. nenhum segredo conhecido permanece na árvore ativa;
2. `AGENTS.md + PROJECT_STATE.md + pedido atual` bastam como bootstrap;
3. uma fonte canônica existe para estado e autorização correntes;
4. contexto adicional é carregado progressivamente;
5. artefatos têm ownership e razão de existência;
6. logs/XMLs/outputs reproduzíveis não ficam como contexto ativo por padrão;
7. CI separa validação contínua de release;
8. release é fail-closed quando assinatura está incompleta;
9. nenhum arquivo Kotlin é alterado pela adoção;
10. diff final recebe auditoria independente.

## Gates
- `ADOPTION_IMPLEMENTATION`: `IMPLEMENTED_AWAITING_VALIDATION`.
- `ADOPTION_VALIDATION`: `NOT_RUN` até conclusão real da CI para o SHA auditável.
- `ADOPTION_AUDIT`: `NOT_RUN` até auditor independente.
- `MERGE_TO_MAIN`: bloqueado até auditoria e decisão da Product Authority.

## Evidência necessária
- comparação contra baseline;
- inventário KEEP/CONSOLIDATE/REMOVE;
- resultado dos checks de segurança/governança;
- resultado da CI disponível para o SHA auditado;
- declaração explícita de validações não executadas.

## Estado da evidência
`STANDARD_ADOPTION_EVIDENCE.md` materializa o inventário atual. `STANDARD_ADOPTION_AUDIT_PACKET.md` define a revisão independente. A política de retenção/limpeza está em `docs/EVIDENCE_POLICY.md`.

## Regra de avanço
Implementação do Builder não equivale a PASS. `NOT_RUN != PASS`. A adoção não fecha nenhum gate funcional F5/F6/F7.