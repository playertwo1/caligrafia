# Changelog

## Em desenvolvimento — Ideias Standard adoption v1

### Segurança
- removido arquivo versionado que continha credenciais de assinatura em texto puro;
- adicionada política de resposta/rotação em `SECURITY_INCIDENT.md`;
- `.gitignore` endurecido para keystores, chaves, `.env`, logs e outputs gerados;
- release alterado para exigir secrets, verificar assinatura do APK e gerar SHA-256.

### Governança e contexto
- adoção formal do Ideias Standard em perfil `DEEP` com packs `android`, `multi-agent` e `sensitive-data`;
- `AGENTS.md` reduzido para bootstrap mínimo e leitura progressiva;
- `PROJECT_STATE.md` convertido em resume card operacional;
- criado `PHASE_CURRENT.md` para escopo/autorização/gates correntes;
- criados `AI_CONTEXT_INDEX.md`, `CONTEXT_POLICY.md`, `context-manifest.json` e `INVARIANTS.yaml`;
- criado `ARTIFACT_CATALOG.md` com ownership `MANAGED / MERGEABLE / USER_OWNED`;
- `README.md` e `ROADMAP.md` deixaram de duplicar status dinâmico;
- `AUDIT.md` e `WATCHDOG.md` tornados contextos especializados por revisão/risco.

### Estado
- nenhuma alteração funcional Kotlin faz parte desta adoção;
- F5.G/F6.G/F7.G permanecem abertos;
- gate da adoção permanece `NOT_RUN` até validação e auditoria independente.

## Em desenvolvimento — F5/F6/F7

- Runner local corrigido para resultados reais e descoberta do Android SDK.
- Backup/restore centralizado para todas as preferências F6, compatível com pacotes antigos.
- Controles F6, lateralidade, pausa ativa, animações, Watch conservador e laboratório ampliados.
- Gates dependentes de E2E, upgrade e hardware permanecem abertos.
