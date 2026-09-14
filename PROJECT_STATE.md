# PROJECT_STATE

**Projeto:** Scribe / Caligrafia  
**Branch operacional desta adoção:** `standard-adoption-v1`  
**Baseline funcional preservada:** `phase-f6-f7-finalization` @ `cdb0fd47704edb5c0903626fb53d36cca05616b1`  
**Perfil Ideias Standard:** `DEEP`  
**Packs:** `android`, `multi-agent`, `sensitive-data`  
**Status da adoção:** `IN_PROGRESS`  
**Gate da adoção:** `NOT_RUN`

## Estado do produto
- F0–F4: histórico de implementação/aceites existente; não reabrir sem finding concreto.
- F5 — assinaturas/exportação/recuperação: `IMPLEMENTADA_AGUARDANDO_VALIDACAO_LOCAL`.
- F6 — preferências/S Pen/Watch/laboratório: `PARCIAL_SOFTWARE_AVANCADO`.
- F7 — consolidação/entrega: `EM_ANDAMENTO_ESTATICO`.
- Gates `F5.G`, `F6.G` e `F7.G`: **ABERTOS**.

Fonte detalhada do estado funcional pré-adoção: `docs/product-delivery/CURRENT_STATE_F5_F7.md`.

## Tarefa atual
Adotar o Ideias Standard, reduzir drift/contexto, corrigir segurança documental, limpar artefatos gerados sem autoridade própria e separar CI de release — **sem alterar comportamento Kotlin do produto**.

## Última tarefa concluída
- branch de adoção criada a partir da baseline funcional;
- design e plano da adoção registrados;
- arquivo com credenciais em texto puro removido da árvore ativa;
- `.gitignore` endurecido;
- workflow de release alterado para exigir material de assinatura e verificar APK.

## Última validação conhecida
Na baseline funcional, testes dirigidos e compilação Kotlin foram reportados como aprovados; a suíte consolidada, E2E, upgrade e bancada física permanecem necessárias para os gates F5/F6/F7. Esta adoção ainda não executou Gradle em checkout limpo.

## Findings abertos
1. **SECURITY/HIGH:** valores de credenciais de assinatura foram versionados historicamente; rotação humana é obrigatória. Ver `SECURITY_INCIDENT.md`.
2. **STATE/MEDIUM:** documentação histórica contém status contraditórios/obsoletos sobre M0/F0/F5; adoção está consolidando fontes de verdade.
3. **VERSION/MEDIUM:** metadados documentais e `app/build.gradle.kts` não estão alinhados sobre versão/versionCode; não corrigir silenciosamente nesta adoção.
4. **DEVICE:** validações S25 Ultra/S Pen/Watch continuam pendentes onde o requisito depende de hardware real.

## Bloqueios
- rotação dos secrets de assinatura depende da Product Authority/GitHub Secrets;
- gates físicos dependem dos aparelhos reais;
- ausência de execução Gradle nesta sessão impede declarar validação funcional nova.

## Próxima ação
1. concluir contratos de governança/contexto e catálogo de ownership;
2. eliminar contradições de documentação ativa;
3. limpar evidência bruta reproduzível da árvore ativa sem apagar fontes canônicas;
4. adicionar CI de validação contínua;
5. produzir `STANDARD_ADOPTION_EVIDENCE.md` e pacote de auditoria;
6. somente após revisão independente considerar a adoção pronta para merge.

## Evidências/pointers
- estado funcional: `docs/product-delivery/CURRENT_STATE_F5_F7.md`;
- evidência funcional: `docs/product-delivery/EVIDENCE.md`;
- incidente: `SECURITY_INCIDENT.md`;
- fase/autorização: `PHASE_CURRENT.md`;
- plano: `docs/superpowers/plans/2026-09-14-ideias-standard-adoption.md`.

**Regra:** `NOT_RUN != PASS`. Builder não fecha gate material sozinho.