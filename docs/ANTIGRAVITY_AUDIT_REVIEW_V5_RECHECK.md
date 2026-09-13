# Auditoria independente V5 — revalidação

**Fechamento integral NÃO APROVADO.** Commit auditado: `32b28c11753d5a8b2db86cf5ec75163818eb18cf`, origin/main em 2026-09-13. As seis contraprovas anteriores foram corrigidas no seu escopo, mas persistem duas falhas de integridade reproduzidas e as pendências de integração já registradas.

## Execução independente

Reexaminados instruções V5, dossiê e matriz; revisadas as diferenças desde `5b98f8f`. Worktree isolado, preservando alterações locais. Executado `docs/audit-v5/run-all-audits.ps1 -AndroidSdk C:/Users/fael/AppData/Local/Android/Sdk`; retorno 0.

| Verificação | Resultado |
|---|---|
| Adversariais V4 | 16/16 PASS |
| Contraprovas V5 anteriores | 6/6 PASS; fonte integrada idêntica à preservada pelo auditor |
| Aceites históricos | 33/33 PASS |
| Suíte completa | 249 testes, 47 classes, nenhuma falha/erro/ignorado |
| assembleDebug / lintDebug | PASS / 0 erros, 48 warnings |
| Três verificações adicionais | 1 PASS, 2 FAIL por asserção |

Contagens obtidas dos XML, não dos números fixos impressos pelo runner. [Resumo](audit-v5/recheck/summary.json), [runner](audit-v5/recheck/run-all.log), [fonte adicional](audit-v5/recheck/AuditV5RecheckTest.kt), [XML adicional](audit-v5/recheck/additional.xml). XML completos preservados em `audit-v5/recheck/baseline-xml`. Teste adicional copiado temporariamente para `app/src/test/.../audit`, executado com `--tests com.scribe.caligrafia.audit.AuditV5RecheckTest` e removido após captura da evidência. Nenhum código de produto alterado.

## Achados atuais

**P1 — S04/S24: valor JSON ausente ainda autoriza sobrescrita.** `BackupSerializer.parseValue()` retorna null tanto para erro quanto para null válido, e `parseObject()` aceita o retorno sem distinguir os casos. O manifesto `{"formatVersion":"1.0","x":}` é aceito. Importá-lo junto de `teacher/diagnostic.json` muda ORIGINAL para REPLACED. `missingJsonValueCannotOverwriteData` falha. O parser novo rejeita GARBAGE, mas ainda não valida toda a gramática. Corrigir propagação de erro e verificar manifesto/tipos antes de tocar dados ativos.

**P2 — R03/R06/S07: recarga de metadados conserva strokes antigos.** `LocalPracticeAttemptRepository.loadManifest()` não invalida `strokeCache`; `loadFullAttempt()` usa getOrPut. Após outro repositório salvar strokes novos para o mesmo ID e modificar o manifesto, o leitor retorna os strokes anteriores (x final esperado 200, observado 0). `replacedStrokeMustInvalidateReaderCache` falha. O hash corrige a contraprova anterior de metadados, mas não a coerência do registro completo. Invalidar cache de strokes na substituição e testar restore com consumidores abertos.

**Correções reconhecidas:** signatures agora participa de export, restore e rollback. `signatureFilesSurviveBackupRestore` passa, comprovando os bytes de raw/metadados em outro diretório, sem afirmar validade gráfica. Histórico prioriza registro do disco para mesmo ID; detecção por hash resolve os casos anteriores de mtime/tamanho. A comparação distingue o contorno invertido testado e remove a alegação de dinâmica muscular/autenticidade. Os 16 testes V4 continuam verdes.

**Pendências V4/V5 sem mudança relevante:** restore ainda sem journal/coordenação global; baseline mantém erros silenciosos e AndroidView sem setStrokes; alfabeto mantém Flow/cache por instância; Professor ignora contexto individual e não ordena reanálises; LearningHub/SRS, passagens, pressão e importação de fontes permanecem sem percurso integral. Notebook/lifecycle, replay/export de página, Ink API e distribuição assinada não receberam correções neste commit. A ausência de alterações foi conferida por diff; não se reivindica novo teste dinâmico para cada item.

**Documentação:** permanece a declaração de fechamento integral e a referência ao teste inexistente `s14_baselinePersistenceSurvivesViewModelRecreation`. O guia ainda anuncia 243 testes/48 classes; a execução atual tem 249/47. A existência de hash agora é verdadeira e substitui essa crítica específica anterior. Preservar o parecer histórico; matriz do implementador não equivale à aprovação independente.

## Matriz e decisão

[Matriz reavaliada dos 66 requisitos](audit-v5/INDEPENDENT_REVIEW_MATRIX_RECHECK.md). Ela atualiza o delta e conserva os requisitos originais, sem trocar R17 (importação de fontes) por persistência de inclinação. Os seis testes anteriores verdes não fecham categorias mais amplas.

Não aprovar integralmente Ondas 0–3/M0–M8. Próxima tarefa: corrigir os dois casos reproduzidos, depois concluir as integrações pendentes. ADB mostrou somente emulator-5554; não houve testes instrumentados, revisão visual dos 12 fluxos, benchmark S25 Ultra ou validação de release/upgrade. Pendências de software não devem ser classificadas apenas como falta de aparelho.
