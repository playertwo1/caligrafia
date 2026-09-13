# Dossiê de Evidências de Entrega Funcional — Scribe

## Fase F0 — Preparar base e proteger os dados (Concluída e Verificada)

### 1. Resumo da Execução de F0
- **Data:** 2026-09-13
- **Commit Base:** `e9c42bba7ede1a46b8adf905cff5a2eed019d070`
- **Total de Testes Unitários:** 261 testes passando / 0 falhas / 0 erros em 49 classes
- **Build Status:** `testDebugUnitTest` SUCCESS (9s), `assembleDebug` SUCCESS (20s)

### 2. Evidências Específicas por Item do Checklist

#### [F0.01 - F0.03] Baseline e Auditoria
- **BASELINE.md:** Criado com registro do commit HEAD, upstream, ambiente JVM 17 e catálogo de arquivos no escopo.
- **Auditoria Vigente:** 261 testes unitários verificados via XMLs (`app/build/test-results/testDebugUnitTest/*.xml`), zero falhas, zero ignorados.

#### [F0.04 - F0.05] Contratos de Dados e Rotas
- **ROUTE_ACTION_MATRIX.md:** Mapeadas todas as rotas e ações (NAV-01 a NAV-04, BK-01 a BK-05, GD-01 a GD-04, EV-01 a EV-02, EX-01 a EX-06).
- **DATA_CONTRACT.md:** Inventariados caminhos relativos em disco (`notebooks/`, `attempts/`, `personal_alphabet/`, `signatures/`, `teacher/`, `custom_fonts/`, `learning_history.json`, `personal_styles.json`), formatos de schema (v1 e v2) e invariantes transacionais.

#### [F0.06 - F0.09] Parser JSON Estrito e Contraprova P1 (S04/S24)
- **Causa:** `BackupSerializer.parseValue()` retornava `null` indiscriminadamente para erro de sintaxe e para o literal `null`, permitindo que manifestos malformados como `{"formatVersion":"1.0","x":}` fossem aceitos e sobrescrevessem arquivos ativos.
- **Correção:** Implementada a classe privada `ParsedValue(val value: Any?)` em `BackupSerializer.kt`. Se o caractere não for um token JSON válido, `parseValue()` retorna `null`, fazendo com que `parseObject()` e `parseArray()` falhem imediatamente sem aceitar valores ausentes. Adicionadas validações para proibir vírgulas finais (`{"a":1,}` ou `[1, 2,]`), números truncados (`12.` ou `12e`), caracteres de controle não escapados e lixo após o objeto raiz.
- **Teste Comprovante:** `AuditV5RecheckTest.missingJsonValueCannotOverwriteData` passa, comprovando que o manifesto com `"x":}` é rejeitado e o arquivo ativo `teacher/diagnostic.json` permanece intacto com `"ORIGINAL"`. Testes dedicados `backupSerializer_rejectsMissingValue_f08`, `backupSerializer_rejectsTrailingComma_f08`, `backupSerializer_rejectsInvalidNumbers_f08`, `backupSerializer_rejectsUnescapedControlChars_f08`, `backupSerializer_rejectsContentAfterTopLevelObject_f08` em `ScribeBackupManagerTest` passam 100%.

#### [F0.10 - F0.11] Invalidação de Cache de Strokes e Contraprova P2 (R03/R06/S07)
- **Causa:** `LocalPracticeAttemptRepository.loadManifest()` atualizava metadados quando o hash em disco mudava, mas mantinha o `strokeCache` em memória. Instâncias que já haviam carregado uma tentativa recebiam os traços antigos em vez dos traços substituídos por outro escritor.
- **Correção:** Inclusão de `strokeCache.clear()` explicitamente dentro de `loadManifest()` quando o arquivo de manifesto sofre recarga externa.
- **Teste Comprovante:** `AuditV5RecheckTest.replacedStrokeMustInvalidateReaderCache` passa com sucesso, comprovando que a substituição de traços para o mesmo ID reflete imediatamente no leitor aberto (coordenada final 200f recebida corretamente).

#### [F0.13 - F0.15] Integridade Transacional e Não-Corrupção
- **Preservação de Dados:** `LocalPersonalAlphabetRepository` cria backup com sufixo `.corrupted.{timestamp}` caso o manifesto sofra corrupção física, prevenindo que dados sejam silenciosamente limpos ou apagados.
- **Assinaturas:** `AuditV5RecheckTest.signatureFilesSurviveBackupRestore` comprova que arquivos em `signatures/` sobrevivem a exportação, importação e restauração sem alteração de conteúdo.

#### [F0.G] Gate F0
- **Resultado:** Integridade confirmada. Manifestos inválidos não modificam ativos e leitores abertos recebem traços substituídos. Fase F0 concluída com sucesso.

---

## Fase F1 — Navegação, biblioteca e caderno completos (Concluída e Verificada)

### 1. Resumo da Execução de F1
- **Data:** 2026-09-13
- **Total de Testes Unitários:** 264 testes passando / 0 falhas / 0 erros
- **Build Status:** `testDebugUnitTest` SUCCESS (9s), `assembleDebug` SUCCESS (26s)

### 2. Evidências Específicas por Item do Checklist

#### [F1.01 - F1.05] Navegação e Hub Canônico
- **Hub Praticar:** Raiz de Praticar (`GuidedPracticeScreen.kt`) implementada como Hub com categorias pedagógicas e seleção de aula; o timer de prática só inicia no momento em que o usuário inicia a tentativa de escrita.
- **Menu Mais Canônico:** Expandido em `ExpansionsTab` e `ExpansionsScreen.kt` para cobrir os 9 destinos canônicos nomeados:
  1. *Meu Alfabeto* (`AlphabetScreen`)
  2. *Professor* (`TeacherScreen`)
  3. *Estilos* (`StylesCatalogContent` com catálogo completo de estilos históricos)
  4. *Assinaturas* (`SignatureStudioContent`)
  5. *Cópia de Textos* (`PassagesContent`)
  6. *Backup* (`BackupContent`)
  7. *S Pen e Watch* (`SpenAndWatchContent`)
  8. *Laboratório* (`InspectorScreen` conectado via `stylusLabViewModel`)
  9. *Preferências* (`PreferencesContent` implementando Fluxo 12)
- **Mapeamento de Destinos:** Todas as 9 rotas conectadas a telas reais. Destinos dependentes de serviços de sistema ou módulos futuros registrados com badges transparentes de `PARCIAL` (ex: importação de fontes TTF em F7 e AlarmManager em F6).
- **Preservação de Pilha e Seleção:** Back stack e callbacks preservam integralmente os identificadores (`exerciseId`, `styleId`, `targetId`, `passage`).

#### [F1.06 - F1.11] Biblioteca de Cadernos, Busca e Gerenciamento
- **Estado Vazio da Biblioteca:** Apresenta card convidativo com botão "+ Novo caderno", sem seeds fictícias ou páginas fantasmas.
- **Validação de Criação de Caderno:** Bloqueio obrigatório de nome em branco ou com mais de 40 caracteres com feedback visual de erro inline em vermelho e impedimento do submit.
- **Busca Insensível a Caixa e Acentos:** Normalização Unicode NFD aplicada tanto na busca quanto no título; estado vazio de busca dedicado com botão "Limpar busca".
- **Renomeação e Exclusão de Caderno:** Implementado diálogo de confirmação para renomear (1–40 caracteres) e diálogo de exclusão identificando o caderno e sua quantidade de páginas; cancelamento preserva dados intactos.
- **Testes Unitários:** `NotebookRepositoryTest.renameNotebook_valid_title_updates_title_and_updatedAt` e `renameNotebook_invalid_blank_or_too_long_returns_false` passam 100%.

#### [F1.12 - F1.16] Organização e Duplicação de Páginas
- **Duplicação de Páginas:** Implementado `duplicatePage` em `LocalNotebookRepository.kt`. Clona a página atribuindo novo ID único de página, clonando cada stroke com novos IDs de traço mas preservando milimetricamente as coordenadas geométricas, timestamps e pressões.
- **Isolamento de Cópia:** Comprovado por `NotebookRepositoryTest.duplicatePage_clones_page_with_new_ids_and_preserves_strokes` que modificar os traços da página duplicada não altera os traços da página original.
- **Exclusão de Página:** Diálogo com confirmação; caso a página excluída seja a ativa, seleciona a vizinha válida mais próxima.

#### [F1.17 - F1.23] Ferramentas de Escrita, Borracha e Guias
- **Pena e Espessuras:** Fina, Média e Grossa expostas na UI com 5 tintas caligráficas canônicas (`CalligraphyColor`) e badges de seleção.
- **Undo/Redo e Borracha:** Desfazer e refazer operam diretamente na página atual; apagamento por stroke remove vetores sem gerar tinta e permite restauração completa via Undo.
- **Pautas-Guia e Persistência:** Copperplate 52° canônica com slider de inclinação e preview; persistência lógica por página protegida por mutex (`pagePersistenceMutex`) para evitar cruzamento de traços durante alternância rápida de páginas.

#### [F1.G] Gate F1
- **Resultado:** Percurso biblioteca → criação de caderno → páginas → escrita vetorial → duplicação → recarga 100% validado. 264 testes unitários passando. Fase F1 concluída com sucesso.

---

## Fase F2 — Aula e treino com resultado real (Concluída e Verificada)

### 1. Resumo da Execução de F2
- **Data:** 2026-09-13
- **Total de Testes Unitários:** 272 testes passando / 0 falhas / 0 erros em 50 classes
- **Build Status:** `testDebugUnitTest` SUCCESS (10s), `assembleDebug` SUCCESS (12s)

### 2. Evidências Específicas por Item do Checklist

#### [F2.01 - F2.02] Currículo Caligráfico Canônico e IDs Resolvíveis
- **5 Estágios Pedagógicos:** `STAGE_1_STROKES`, `STAGE_2_FAMILIES`, `STAGE_3_CONNECTIONS`, `STAGE_4_WORDS`, `STAGE_5_SENTENCES` exibidos via `CurriculumStageSelector` com contagem e nomes em português.
- **Resolução Estrita de Glifos (F2.02):** Corrigidos os IDs canônicos em `CurriculumCatalog.kt` (`basic_underturn`, `basic_overturn`, `basic_compound`, `basic_oval`, `basic_ascending_loop`).
- **Teste Comprovante:** `PhaseF2AcceptanceTest.f2_02_curriculumCatalogGlyphsAreAllResolvableInReferenceGlyphCatalog` valida que cada um dos glyphIds das 18 lições existe e é resolvível em `ReferenceGlyphCatalog.findById()`, eliminando fallbacks genéricos para triângulos ou traços inexistentes.

#### [F2.03 - F2.07] Sessão Única Ativa, Conflito e Pré-Início
- **Resumo Pré-Início (F2.03):** Diálogo `PreStartLessonDialog` exibe metadados completos (título, objetivo, estilo formal, glifos/exercícios, texto-alvo) e chips de duração (5, 10, 15 e 20 min) antes de iniciar qualquer treino.
- **Card Proeminente de Sessão Ativa (F2.04):** `ActiveOngoingSessionCard` renderizado no topo de `LearningHubScreen` exibindo o tempo decorrido formatado (`mm:ss / mm:00`), a fase pedagógica atual e botões para "Continuar Treino", "Encerrar e Salvar" e "Descartar".
- **Sessão Única e Conflito (F2.06):** Ao tentar iniciar uma nova aula com outra em andamento, o sistema apresenta o diálogo de conflito oferecendo continuar o treino atual ou encerrá-lo e salvá-lo antes de iniciar o novo.
- **Transporte Sem Fallback (F2.07):** Callback `onNavigateToPractice: (targetId: String, styleId: String) -> Unit` transporta o ID do exercício e o ID do estilo formal simultaneamente até `GuidedPracticeViewModel.selectTargetAndStyle(exerciseId, styleId)`.

#### [F2.08 - F2.11] Relógio Monotônico, Pausa Manual e Persistência de Sessão Interrompida
- **5 Fases Pedagógicas (F2.08):** `WARM_UP` (15%), `DEMO_FOCUS` (15%), `ASSISTED_PRACTICE` (40%), `SOLO_PRACTICE` (20%), `REVIEW_SUMMARY` (10%) com cálculo proporcional dinâmico baseado na duração da sessão.
- **Teste dos 59 Segundos (F2.09 & F2.26):** Comprovado por `PhaseF2AcceptanceTest.f2_09_and_f2_26_monotonicTimerRecords59SecondsAccurately`. Sessão praticada por 59 segundos grava exatamente `actualDurationSeconds = 59` sem arredondamento inventado.
- **Preservação de Pausa Manual (F2.10 & F2.11):** `SessionTimer` rastreia `wasManuallyPaused`. Retorno de ciclo de vida (`onResumeLifecycle`) não reativa a contagem se o usuário pausou manualmente na interface. Comprovado por `PhaseF2AcceptanceTest.f2_10_and_f2_11_manualPausePreservedAcrossAppResume`.
- **Persistência Atômica de Sessão Interrompida (F2.11):** Implementados `saveActiveSession`, `loadActiveSession` e `clearActiveSession` em `LocalLearningHistoryRepository` com serializador JSON determinístico em `LearningHistorySerializer`. Ao reabrir, a sessão é restaurada em estado pausado, sem inflar tempo fora do app. Comprovado por `PhaseF2AcceptanceTest.f2_11_interruptedSessionRestoredPausedWithoutTimeLeak`.

#### [F2.12 - F2.17] Modos de Exercício, Ghost Mode, Pautas e Invalidação de Nota
- **Modos de Prática (F2.13):** `Cobrir` (Trace - glifo centralizado sob a pena), `Copiar` (Copy - modelo fixo em caixa pontilhada à esquerda como gabarito) e `Sozinho` (Solo - pauta limpa de memória).
- **Ghost Mode Progressivo (F2.14):** 100% (FULL), 70% (CLEAR), 40% (FAINT), 10% (WATERMARK) e 0% (OFF) acessíveis via `FilterChip` em barra de rolagem horizontal sem corte.
- **Pauta Adaptativa em Telas Comprimidas (F2.15):** `GuidelineConfig.computeBands` gera faixa proporcional ajustada caso o espaço vertical seja inferior a `topMargin + bandHeight`. Comprovado por `PhaseF2AcceptanceTest.f2_15_guidelineBandsFallbackOnConstrainedPageHeight`.
- **Invalidação no Toque (F2.17):** `StrokeCapturePipeline.onStrokeStarted` limpa e invalida imediatamente a avaliação e a nota anterior no exato momento do `ACTION_DOWN`.

#### [F2.18 - F2.24] Avaliação Geométrica Real, Idempotência e Resumo
- **Sem Aprovação Padrão (F2.18):** Avaliação de tentativa sem traços ou incompleta retorna pontuação zero com feedback de orientação explícito.
- **Idempotência de Finalização (F2.21 & F2.26):** Guard de reentrância em `finishAndSaveSession` impede duplicação mesmo após toques múltiplos rápidos. Comprovado por `PhaseF2AcceptanceTest.f2_21_and_f2_26_duplicateFinalizationIsIdempotent`.
- **Resumo Conclusivo (F2.22):** `ActiveSessionDialog` em fase `REVIEW_SUMMARY` exibe tempo exato praticado em minutos e segundos, contagem de tentativas, média e botões para rever traços, concluir ou voltar.
- **Tentar Novamente Limpo (F2.24):** Preserva a tentativa anterior salva no repositório com ID único e reseta o canvas para novo traço. Comprovado por `PhaseF2AcceptanceTest.f2_24_retryPreservesPreviousAttemptAndStartsFreshCleanAttempt`.

#### [F2.G] Gate F2
- **Resultado:** Trilha de aprendizado, cronômetro deliberado, resolução canônica de exercícios, persistência atômica de sessão interrompida e avaliação geométrica real 100% integrados e validados. 272 testes unitários passando. Fase F2 concluída com sucesso.

