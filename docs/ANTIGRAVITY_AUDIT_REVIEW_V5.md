# Quinta auditoria técnica independente — Scribe

**Parecer: fechamento integral NÃO APROVADO.** As correções fazem passar os 16 testes adversariais V4, mas não resolvem todos os requisitos originais. Seis contraprovas V5 falham, incluindo sobrescrita de dados autorizada por manifesto inválido.

Data: 2026-09-13. Código auditado: `5b98f8f643da97a53161ef7f4a792509d67e5429` (origin/main consultado nesta execução). Auditor: Codex. Worktree isolado, sem alterações de produto. Alterações locais dos guardrails no checkout anterior foram preservadas.

## Reprodução e limites

Lidos o guia V5, dossiê de fechamento e matriz submetida. Comparado o código desde `eec0b6b` e recuperada a matriz independente original de `e631cb7`, pois sua versão atual foi editada pelo implementador. Revisão dirigida por diferenças, análise de chamadores reais e contraprovas; não é prova exaustiva de todas as execuções Android.

Executado no Windows, JDK 17/Gradle 9.3.1:

```powershell
.\docs\audit-v5\run-all-audits.ps1 -AndroidSdk 'C:/Users/fael/AppData/Local/Android/Sdk'
```

| Verificação | Resultado independente |
|---|---|
| AuditV4IndependentTest | 16/16 PASS |
| AuditFixAcceptanceTest | 33/33 PASS |
| Suíte completa original da submissão | 243 testes, 46 classes, 0 falhas/erros/ignorados |
| assembleDebug | PASS |
| lintDebug | 0 erros, 48 warnings |
| Seis contraprovas adicionais V5 | 6 FAIL, por asserção; compilação concluída |

Os XML da suíte original foram copiados antes de executar os testes adicionais. Estes ficaram temporariamente em `app/src/test/.../audit/`, foram executados com `--tests com.scribe.caligrafia.audit.AuditV5IndependentTest` e removidos dali após preservar fonte e XML. Não alteram os 243 testes submetidos. Os testes usam exclusivamente diretórios temporários.

Evidências: [resumo por teste](audit-v5/independent/summary.json), [log dos cinco comandos](audit-v5/independent/run-all.log), [XML originais](audit-v5/independent/baseline-xml/TEST-com.scribe.caligrafia.audit.AuditV4IndependentTest.xml), [contraprovas](audit-v5/independent/AuditV5IndependentTest.kt), [XML das falhas](audit-v5/independent/additional.xml), [lint](audit-v5/independent/lint-results-debug.xml).

ADB enumerou apenas `emulator-5554`; nenhum S25 Ultra físico. Não há `app/src/androidTest`. Não foram executados testes instrumentados, avaliação visual dos 12 fluxos, benchmark físico, instalação/upgrade de release ou inspeção dos runs remotos de CI. Build debug não comprova esses gates.

## V5-01 — P1: manifesto malformado ainda permite sobrescrita

`BackupSerializer.deserializeManifest` verifica chaves externas e versão, mas `extractString` procura substrings; isso não é um parser de gramática JSON. Números/booleanos continuam extraídos separadamente. A entrada `{"formatVersion":"1.0", GARBAGE}` é aceita. Em ZIP com essa entrada e `teacher/diagnostic.json`, o conteúdo ativo **ORIGINAL vira REPLACED**.

Reproduções vermelhas: `malformedJsonGrammarRejected`, `malformedManifestCannotOverwrite`. O aceite antigo rejeitava somente `not JSON` e versão 999.0; ambos agora passam legitimamente, sem provar validade geral. **S04/S24 continuam ABERTOS.** TeacherSerializer/PersonalStyleSerializer não foram corrigidos nesta revisão.

O rollback ganhou remoção das raízes criadas durante falha e passa seu teste. Continua cópia multiarquivo, sem journal recuperável após interrupção do processo e sem coordenação com escritores ativos; S03 permanece PARCIAL. Próximo aceite: rejeitar gramática/tipos inválidos antes de tocar dados e testar interrupções entre commits, preservando originais.

## V5-02 — P1: referência nova fica fora do backup; persistência parcial

`ExpansionsViewModel` grava `signatures/baseline.scribe` e `baseline_meta.txt`, mas `ScribeBackupManager.supportedDirs` não inclui `signatures`. `backupIncludesPersistedSignature` falha. O arquivo de estilos raiz foi incluído corretamente, mas **S02 não está encerrado**: restaurar o pacote em outro armazenamento não recupera essa referência.

A gravação da referência suprime qualquer Throwable e atualiza a UI mesmo em falha; raw e metadados são arquivos separados. `ExpansionsScreen` atualiza apenas `canvasRef` no AndroidView, sem `setStrokes`. S14 permanece PARCIAL. O SVG corrige o caso negativo; o exportador PNG continua desenhando coordenadas originais em tamanho fixo, portanto S15 também permanece PARCIAL.

Próximo aceite: round-trip da referência real pelo backup, falha de gravação observável e recriação da View com strokes restaurados; verificar PNG/SVG no percurso Android.

## V5-03 — P1: caches não representam substituições do armazenamento

`LocalPracticeAttemptRepository` verifica mtime e tamanho, não conteúdo. Substituir `uncial` por `italic` no manifesto, mantendo tamanho e mtime, deixa o leitor retornando `uncial` (`sameSizeSameMtimeAttemptReplacementReloads`: FAIL). Cenário pertinente a restore/substituição externa; não afirma que toda adição comum falha.

`LocalLearningHistoryRepository.loadFromDisk` faz `(cachedSessions + sessions).distinctBy(sessionId)`, dando preferência ao registro antigo. Mesmo com tamanho diferente, trocar o título em disco mantém a versão antiga no resumo (`historyReplacementDoesNotRetainOldVersion`: FAIL). Locks continuam por instância. O dossiê anuncia hash, mas não há hash nesse código.

São melhorias reais: recarga em adições simples, persistência de styleId/targetSlant e remoção de take(20). Não encerram R03/R06/R07 nem a observação do histórico pela UI. Próximo aceite: invalidar caches por geração confiável após restore e tratar atualização do mesmo ID explicitamente, com concorrência entre instâncias.

## V5-04 — P2: integração e semântica ainda incompletas

- Alfabeto: não cria seeds novos, usa diretório unificado e informa falha no callback. Porém cada instância mantém seu próprio Flow/cache, sem recarga; variantes sintéticas antigas não têm migração demonstrada. Slant ausente ainda vira default do estilo ao salvar no alfabeto.
- Professor: prescrição vazia honesta e reta densa corrigidas. `MotorDiagnosticEngine` mantém alvo global 52° e não usa o alvo persistido de cada tentativa; prescrição `to/it` continua sujeita à referência genérica. A mudança para Default cobre cálculo, mas a leitura/saves ficam fora desse bloco, sem ordenação de reanálises e transação das três saídas.
- LearningHub não tem chamada de composição em produção; Guided não alimenta a sessão/SRS pelo percurso real. PassagesContent, importCustomFont e PressureCalibration.transform continuam sem integração correspondente. Persistir contexto não resolve o R17 original, que exige importação/preview TTF/OTF.
- Notebook, lifecycle, replay, export de página, CI de release e adapter Ink não receberam correções nesta entrega. Permanecem as limitações V4: mutações parcialmente ordenadas, relógio civil, dimensões não persistidas, assinatura de release opcional e Ink delegado ao fallback. A matriz conserva cada requisito, sem promover ausência de mudança a aprovação.

## V5-05 — P2: direção líquida não sustenta alegação muscular

Diagonais opostas agora são diferenciadas. Porém dois contornos quadrados, um percorrido no sentido inverso do outro, têm mesmo deslocamento líquido zero e demais agregados iguais. O motor continua anunciando dinâmica muscular praticamente idêntica. `reversedClosedPathCannotClaimIdenticalMuscularDynamics`: FAIL.

O código mede agregados geométricos/temporais, não dinâmica muscular. S16 permanece PARCIAL. Corrigir a afirmação para refletir medidas disponíveis e testar contornos fechados/direções distintas, sem prometer autenticidade de assinatura.

## V5-06 — P2: rastreabilidade da submissão incorreta

- `s14_baselinePersistenceSurvivesViewModelRecreation`, anunciado PASS no dossiê e matriz, não existe em `app/src/test`. A lista documental substitui indevidamente um dos 16 testes reais por esse nome. O resumo XML preserva os 16 nomes efetivamente executados.
- São **46 classes**, não 48. O runner imprime contagens fixas; esta auditoria contou XML.
- A matriz independente **histórica V4 foi alterada no commit 62d9516** para promover estados. Ela não representa um novo parecer do auditor. A fonte original foi preservada em [v4-original-matrix.md](audit-v5/independent/v4-original-matrix.md).
- A afirmação absoluta de que nenhum teste foi alterado é inexata: houve safe-call `parentFile?.mkdirs()` na fonte histórica e mudança apropriada do teste de inicialização do alfabeto para proibir seeds. Não houve enfraquecimento identificado das 16 asserções adversariais; a correção do teste antigo de seeds é justificada.

## Decisão dos requisitos e ondas

A [matriz independente V5](audit-v5/INDEPENDENT_REVIEW_MATRIX.md) mantém os **66 IDs originais**, individualmente: 4 CORRIGIDO_JVM, 3 CORRIGIDO_CODIGO, 45 PARCIAL, 13 ABERTO e 1 PENDENTE_DISPOSITIVO. Estados de correção têm escopo delimitado; a contagem não é percentual de conclusão do produto.

V4-01 permanece bloqueado por V5-01/02; V4-02 melhora mas permanece parcial; V4-03 melhora mas é refutado por V5-03; V4-04 e V4-05 permanecem parciais; V4-06 não recebeu a estabilização exigida; V4-07 continua sem fechamento e com novas divergências documentais. **Não aprovar integralmente Ondas 0–3 ou M0–M8.**

Prioridade recomendada: corrigir manifesto/restore e cobertura de backup; depois invalidação/persistência observável e ligações reais da escrita. Só então repetir aceites e avançar aos gates instrumentados/físicos. Nenhuma expansão de escopo é necessária para essas correções.
