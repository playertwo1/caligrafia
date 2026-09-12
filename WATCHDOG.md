# WATCHDOG — Regras de Conformidade e Auditoria Estrita

Este documento define as regras inegociáveis de operação no repositório **Scribe**. Cada alteração ou tarefa executada deve cumprir integralmente este checklist antes de ser considerada concluída.

---

## 1. Princípios de Contenção (Não Fugir dos Trilhos)

1. **Aderência ao Milestone:** Atuar exclusivamente no marco atual (**M0 — Stylus Lab**). É proibido adiantar funcionalidades de marcos futuros (como IA, nuvem, login ou telas completas de produto).
2. **Nenhuma Dependência Não Autorizada:**
   - Proibido o uso de `WebView` para qualquer finalidade de canvas ou renderização.
   - Proibido introduzir SDKs de nuvem/backend no MVP sem solicitação explícita.
   - Proibido acoplar o domínio a bibliotecas proprietárias (ex.: Samsung Spen SDK); usar sempre abstrações e adapters.
3. **Integridade dos Dados:**
   - Raw strokes nunca são sobrescritos ou descartados por filtros visuais ou bitmaps.
   - Não inventar ou preencher valores de hardware (pressão, inclinação/tilt, orientação) quando o hardware real reportar indisponibilidade (`null`).
4. **Local-First & Offline:** O núcleo do aplicativo deve funcionar 100% de forma local e offline.

---

## 2. Checklist Obrigatório por Tarefa

Antes de concluir qualquer tarefa ou submeter alterações:

- [ ] **Critérios de Aceite:** Todos os critérios de aceite definidos no `BACKLOG.md` para a tarefa foram cumpridos?
- [ ] **Testes Unitários:** Todos os testes relevantes foram executados e passaram (`.\gradlew.bat testDebugUnitTest`)?
- [ ] **Compilação sem Erros:** O APK de depuração compila com sucesso (`.\gradlew.bat assembleDebug`)?
- [ ] **Auditoria de Código:** O script `.\scripts\watchdog.ps1` executou com status de aprovação?
- [ ] **Atualização de Estado:** O arquivo `PROJECT_STATE.md` foi atualizado com o status exato, testes realizados, limitações e a próxima ação imediata?
- [ ] **Relatório Transparente:** O relatório final especifica arquivos alterados, decisões técnicas, testes executados, limitações identificadas e a próxima tarefa recomendada?
