# Quarta auditoria técnica independente — Scribe V4

**Parecer: NÃO APROVADO para declarar concluídas as Ondas 0–3 ou fechar M0–M8.**

Data: 2026-09-13. Auditor: Codex. Código examinado: **`eec0b6bdbec2c0cdcfb0371366e92a3b3aa2102f`**, branch `main`. Esta revisão trata o dossiê do implementador como alegações a verificar, não como evidência de aprovação.

Há correções reais: finalização do ZIP, retirada da chamada de seeds das tentativas, ticker padrão, tick terminal de 300s, ordem flush/sync, mutex de algumas operações, cobertura ponto–segmento, pontos isolados no preview e descoberta de estilos pessoais. Entretanto, permanecem defeitos de integridade, integração, captura, persistência e métricas. Parte substancial dos itens marcados CONCLUÍDO não atende ao aceite original.

## 1. Escopo e método

Lidos os quatro documentos solicitados: [dossiê](../AUDIT_REPORT_STABILIZATION.md), [instruções V4](CODEX_AUDIT_INSTRUCTIONS_V4.md), [matriz submetida](audit-v4/AUDIT_STABILIZATION_MATRIX.md) e [guia visual](design/fluxos-v1/README.md). Também examinados AGENTS, README, PRODUCT_SPEC, ROADMAP, ARCHITECTURE, STYLUS_ENGINE, PROJECT_STATE, V1/V2/V3, código de produção, chamadores de UI, testes e workflow.

Os **66 IDs históricos** têm avaliação individual na [matriz independente](audit-v4/INDEPENDENT_COMPLIANCE_MATRIX.md). Não são 66 defeitos distintos: há sobreposição entre auditorias. Os significados originais foram preservados. Em particular, **R17 originalmente é importação/preview de fontes**, não seleção de inclinação; **A12 é completude e A13 é coerência modelo/slant**; **A16 é benchmark SQLite/Room**, não uma categoria genérica de validação física. O teste submetido chamado `s18_teacherPrescription...` não comprova S18, que é curva de pressão.

Auditoria realizada no checkout recém-clonado, inicialmente limpo. Sem alterações de produto, dependências, testes preexistentes, publicação, commit ou upload. Testes adicionais foram temporariamente copiados para a árvore JUnit, executados e retirados; fonte e resultados ficam em `docs/audit-v4/independent/`. [Delta desde V3](audit-v4/independent/delta-numstat.txt).

Limites: Windows/JVM; SDK local, sem aparelho em `adb devices -l`. Não houve teste instrumentado, captura real de tela, benchmark S25 Ultra, validação Watch, instalação/upgrade ou nova verificação do APK público. A inspeção dos 12 fluxos é de estrutura/chamadores e referências, **não aprovação visual em runtime**. Os APKs públicos e Drive não foram examinados nesta rodada.

## 2. Reprodução

O primeiro comando encontrou SDK não configurado no checkout. Foi usado o SDK já instalado, com `$env:ANDROID_HOME='C:/Users/fael/AppData/Local/Android/Sdk'`, sem alterar Gradle nem instalar dependências novas de produto. JDK Eclipse Adoptium 17.0.20.1; Gradle 9.3.1; Windows 11.

| Verificação | Resultado independente | Evidência |
|---|---|---|
| `testDebugUnitTest --tests com.scribe.caligrafia.audit.AuditFixAcceptanceTest --console=plain` | **33 testes, 0 falhas/erros**, BUILD SUCCESSFUL | [Log](audit-v4/independent/acceptance.log), [XML](audit-v4/independent/acceptance.xml) |
| `testDebugUnitTest --console=plain` | **227 testes / 45 classes / 0 falhas / 0 erros / 0 ignorados** | [Log](audit-v4/independent/full-suite.log), [resumo](audit-v4/independent/baseline-summary.json), [XMLs](audit-v4/independent/baseline/) |
| `assembleDebug --console=plain` | **BUILD SUCCESSFUL**, 36 tarefas | [Log](audit-v4/independent/assemble-debug.log) |
| `:app:lintDebug --console=plain` | **0 erros, 48 warnings**, BUILD SUCCESSFUL | [Log](audit-v4/independent/lint.log), [XML](audit-v4/independent/lint-results-debug.xml) |
| Aceites independentes adversariais | **16 testes / 16 falhas de asserção / 0 erros** | [Fonte](audit-v4/independent/AuditV4IndependentTest.kt), [log](audit-v4/independent/independent-tests.log), [XML](audit-v4/independent/independent-tests.xml), [resumo](audit-v4/independent/independent-summary.json) |
| Hardware | Lista de dispositivos vazia | [Ambiente](audit-v4/independent/environment.txt) |

SHA-256 do debug produzido: `53cebafc0f01771706256c70ceaf5932556532ced9b5f4eeafcb083575babd2e`. Isso identifica o artefato local; não comprova assinatura de release.

Os 48 warnings são: 14 DefaultLocale, 14 GradleDependency, 10 UseKtx, 3 UnusedResources e um de cada: AndroidGradlePluginVersion, ClickableViewAccessibility, DrawAllocation, MissingApplicationIcon, NewerVersionAvailable, ObsoleteSdkInt e OldTargetApi. Priorizar acessibilidade, alocações e formatação; avisos de versão não autorizam upgrade amplo.

**As 16 falhas independentes são expectativas de comportamento correto que o produto violou.** Não são testes verdes escritos para aceitar o bug, nem 16 funcionalidades novas. As fixtures usam apenas diretórios temporários. O teste de mtime força deliberadamente igualdade do timestamp para testar o contrato do cache; não afirma que toda gravação normal perde dados. O teste de rollback força conflito arquivo/diretório depois da primeira cópia: prova falha de recuperação em estado adverso, não simula queda de energia.

Reprodução dos testes adicionais: [run-independent-tests.ps1](audit-v4/independent/run-independent-tests.ps1). O script preserva a suíte original e deve retornar código diferente de zero enquanto os defeitos persistirem.

## 3. Achados prioritários

### V4-01 — P1 — Restore aceita manifesto inválido e recuperação deixa estado parcial (S03/S04/S24)

**Locais:** `expansions/backup/BackupSerializer.kt:35`, `ScribeBackupManager.kt:140`, `:205`, `:302` (caminhos relativos a `app/src/main/java/com/scribe/caligrafia/`).

`deserializeManifest("not JSON")` devolve um `BackupManifest` preenchido com defaults. A versão `999.0` também é aceita. Em ZIP com esse manifesto inválido e `teacher/diagnostic.json`, o importador retornou **success=true e substituiu ORIGINAL por REPLACED**. Não há parser JSON estrito, validação de versão ou validação dos arquivos internos antes de efetivar o restore.

Existe staging e cópia de rollback, mas o commit continua sendo uma sequência de `Files.copy(REPLACE_EXISTING)` no estado ativo. O rollback restaura somente raízes que existiam no backup anterior: quando a importação criou `notebooks/` e depois falhou em `teacher/`, o novo caderno permaneceu. A mensagem de erro afirmou dados anteriores preservados. Não há journal/recuperação após processo interrompido nem coordenação com escritores ativos; a cópia de recuperação também pode falhar. Não se afirma que uma transação foi comprovada por existir uma pasta temporária.

O teste submetido `s03_importAtomicRollback...` fornece **bytes que nem são um ZIP**: ele falha antes do commit e nunca exercita o rollback anunciado. Os testes independentes exercitaram substituição inválida e falha após cópia.

**Aceite pendente:** validar pacote/esquema/versão/conteúdo; preservar inventário anterior completo, inclusive ausência de arquivos; ordenar leitores/escritores; injetar falha em cada etapa e reabrir após interrupção. Não expor restore como atômico até essas provas passarem.

### V4-02 — P1 — Alfabeto usa caminhos diferentes e ainda nasce com notas artificiais (R04/S02/R10)

**Locais:** `guided/ui/GuidedPracticeViewModel.kt:69`, `:360`; `alphabet/ui/AlphabetViewModel.kt:57`; `alphabet/repository/LocalPersonalAlphabetRepository.kt:314`; `expansions/backup/ScribeBackupManager.kt:21`.

Guided constrói `LocalPersonalAlphabetRepository(application.filesDir)`, enquanto Alphabet usa `File(application.filesDir, "personal_alphabet")`. O repositório acrescenta apenas `strokes/` e `personal_alphabet_manifest.json` ao diretório recebido. Assim, **o botão Salvar no Meu Alfabeto grava em outra árvore que a tela Meu Alfabeto lê**. Mesmo corrigindo o diretório, os caches e mutexes por instância não sincronizam clientes distintos automaticamente.

Novo repositório ainda chama `createInitialSeedAlphabet()`: `a/l/i/it` recebem variantes favoritas com 92/94/89/91. O teste independente confirmou uma variante em `a` sem escrita do usuário. Esses exemplos alimentam compilação pessoal. Em `saveAttemptToPersonalAlphabet`, exceções são convertidas em mensagem de sucesso e `onComplete(true)`; ausência de avaliação ainda usa 70 e 52 como valores padrão.

O backup cobre mais diretórios que antes, mas omite os dados Guided na raiz (`strokes/`, manifesto) e `personal_styles.json`, que o StyleEngine lê da raiz. A omissão do último foi reproduzida. As contagens continuam contando arquivos de variante como glifos e a existência do histórico como uma lição.

**Aceite pendente:** um contrato de diretório e proprietário de estado para todos os chamadores; exemplos isolados; erros observáveis; escrever→ver na tela→favoritar→reabrir→backup/restore com dados reais. Não migrar limpando dados existentes.

### V4-03 — P1 — A escrita não alimenta a sessão/SRS e leitores não veem novas tentativas (R02/R03/R06/S07)

**Locais:** `MainActivity.kt:222`; `guided/ui/GuidedPracticeViewModel.kt:220`; `evolution/repository/LocalPracticeAttemptRepository.kt:43`; `evolution/ui/EvolutionViewModel.kt:73`; `learning/history/LocalLearningHistoryRepository.kt:89`.

As quatro abas existem, mas `LearningHubScreen` ficou sem chamador na navegação de produção. Guided usa um cronômetro próprio e salva tentativas; não chama `LearningViewModel.recordAttempt`, não finaliza `CompletedSessionRecord` e não atualiza SRS. Remover os botões de notas fixas foi correto, mas não criou a ligação. O teste R02 a cria artificialmente com `sessionTimer.recordAttempt(eval...)` e `historyRepo.recordSession(...)` dentro do próprio teste.

Guided, Teacher e Evolution instanciam repositórios distintos de tentativas. O manifesto só é carregado na construção; `getAllAttempts()` lê cache. Reproduzido: escritor salva uma tentativa, leitor já aberto continua com zero. O estado da Evolução é carregado na inicialização, sem assinatura de alterações do repositório. A correção funciona parcialmente quando um leitor é criado depois da gravação; não é atualização contínua de ponta a ponta.

No histórico, `lastModified() > lastKnownModified` não detecta timestamp igual/mais antigo, e `lock`/arquivo `.tmp` são por instância/fixo. Reproduzido com mesmo mtime: três sessões esperadas, duas preservadas. A consulta da Evolução continua usando `recentSessions.take(20)`: 21 minutos reais viram 20, embora a soma de segundos no helper tenha sido corrigida.

**Aceite pendente:** fluxo de sessão acessível e resultado real rastreável; estado observável compartilhado ou invalidação/versionamento correto; teste abrir Evolução/Professor antes de escrever; mais de 20 sessões; concorrência/falha de gravação.

### V4-04 — P1 — Professor continua com falsos diagnósticos e reanálise na main (S08–S11/S20)

**Locais:** `teacher/engine/MotorDiagnosticEngine.kt:93`, `:173`; `CoachingCurriculumGenerator.kt:22`; `teacher/ui/TeacherViewModel.kt:79`; `guided/ui/GuidedPracticeViewModel.kt:272`.

O diagnóstico vazio agora retorna zero/BEGINNER/INSUFFICIENT_DATA — correção real. Porém `weakness == null` ainda gera **“Seu traço apresenta maturidade caligráfica superior”** no gerador de prescrição. O aceite independente falhou nesse percurso de usuário novo.

Contenção continua sendo a média de `scorePercent`, sem medir baseline/x-height/ascender/descender, e pode afirmar “sem estouros”. O diagnóstico ignora o contexto por tentativa e aplica o alvo padrão 52°. Os limiares `dy > 4`, `dist > 5` tornam a observação dependente de densidade: mesma reta de 45°, mesma duração, resulta 45° com endpoints e `null` com 101 pontos. Pressão ainda sustenta texto de tensão muscular sem comprovação correspondente.

O gerador continua emitindo `to/t/a/it/l`. O resolvedor novo corrige símbolos presentes como a/t/l, mas **IDs não resolvidos geram o mesmo desenho de três pontos** em `createDynamicGlyph`, apenas com outro nome. `to/it` não ganham ductus real por esse fallback. Navegação passa só o alvo; aquecimento, duração, Ghost/meta e conclusão de prescrição não são conectados ao resultado.

A função real é `reanalyzeAllData()`: usa `viewModelScope.launch` sem dispatcher de trabalho para `getAllAttempts()` e análise, sem try/finally de `isAnalyzing`. Não existe a implementação `reanalyzeAllAttempts` em `Dispatchers.Default` descrita no dossiê. O repositório grava diagnóstico/prescrição/insights separadamente, com `.tmp` fixo e sem transação de snapshot.

**Aceite pendente:** contexto medido persistido, ausência diferenciada de desempenho, invariância por amostragem/escala, IDs resolvidos ou erro explícito, prescrição executável e reanálise ordenada fora da main com erro/cancelamento observáveis.

### V4-05 — P1/P2 — Assinatura ainda perde estado/capacidades e export pode recortar tudo (S13–S16)

**Locais:** `expansions/signature/SignatureCanvasView.kt:96`; `expansions/ui/ExpansionsViewModel.kt:111`, `:216`; `ExpansionsScreen.kt:306`; `SignatureExporter.kt:18`; `SignatureConsistencyEngine.kt:136`.

Snapshots `toList()` corrigem o aliasing das listas emitidas pelo canvas. Contudo a referência fica só em StateFlow; `loadBaselineSignature()` é vazio e AndroidView não chama `setStrokes` ao recriar. S14 não está concluído.

O canvas próprio continua usando pointer 0, aceita dedo, transforma ERASER em stroke renderizável, não trata POINTER_UP e descarta pressão zero/tilt/orientação. UP acrescenta posição somente quando coordenadas mudam, não todo endpoint sensorial/temporal. São pendências de software, além da validação física.

SVG agora é gravado fisicamente pelo botão — melhoria real. Entretanto SVG/PNG usam coordenadas cruas com viewport fixo e cores/larguras substituídas. O aceite independente demonstrou uma assinatura inteiramente negativa mantida fora do `viewBox 0 0 1080 500`. O cálculo dos máximos negativos foi corrigido, mas diagonais opostas ainda recebem **100** e texto de dinâmica muscular idêntica/autenticidade. O arquivo vai a `filesDir/signatures`, sem percurso de seleção/compartilhamento de destino.

**Aceite pendente:** referência vetorial persistida e View restaurada; política de captura comum; transformação de export validada por pixels; linguagem restrita às métricas realmente medidas; entrega em destino acessível escolhido pelo usuário.

### V4-06 — P1/P2 — Mutex não fecha o ciclo de páginas; tempo/contexto de tentativa não são fiéis (A03/A06/R11–R19)

**Locais:** `notebook/viewmodel/NotebookPracticeViewModel.kt:177`, `:263`, `:287`, `:414`; `guided/ui/GuidedPracticeViewModel.kt:89`, `:258`; `evolution/repository/LocalPracticeAttemptRepository.kt:35`; `alphabet/engine/PersonalStyleCompiler.kt:112`.

O mutex serializa partes de IO, mas snapshots são capturados antes de jobs em Dispatchers.IO adquirirem o lock; não há versão que impeça um snapshot antigo de sobrescrever um novo. `createNotebook`/`deleteNotebook`/`setGuidelineConfig` e carga inicial estão fora do mesmo protocolo. Criação de caderno troca o repositório em memória sem salvar/flush explícito da página anterior; folheamento não encerra o stroke ativo. Isso é risco estático de ordem/atribuição, não uma corrida de UI reproduzida nesta máquina. O teste A03 submetido exercita dez saves sequenciais com um mutex criado no teste, não esse código.

O timer Guided inicia na construção e é retomado no `onResume` da Activity, mesmo em outra aba; não reinicia por tentativa e não pausa ao sair da aba. `durationMs = elapsedSeconds * 1000`, mínimo 1s, não é a duração do stroke. `AttemptComparator` divide comprimento por esse tempo acumulado, portanto sua fórmula nova não garante velocidade motora real no percurso de produção.

`PracticeAttemptRecord` ganhou `styleId`/`targetSlantDegrees`, mas `AttemptMeta`, o manifesto e `loadFullAttempt` não os carregam: gravar/reabrir devolve `styleId=null` (reproduzido), também perdendo o alvo. Sem inclinação medida, Guided grava o próprio alvo como média. O compilador pessoal continua preferindo metadados a vetores e usa defaults 60/5/1, além de limiares por segmento e faixa 45..90; não a generalização 20..160 declarada.

Cobertura ponto–segmento corrigiu a reta com poucos endpoints, mas a nota total não é invariante: o avaliador perde slant em amostragem densa e lhe atribui 85. Replay em modo REAL_TIME corrige o mapeamento relativo, porém o loop continua usando `System.currentTimeMillis`. Preview agora desenha pontos e escala, mas recalcula enquadramento a partir do subconjunto visível e por chamada: replay/overlay não têm transformação comum estável. Export do caderno recebeu dimensões, mas a altura limitada a 720..4000 pode produzir escalas X/Y diferentes em aspectos extremos; dimensões de origem não são persistidas na página.

**Aceite pendente:** proprietário e sequência de eventos coerentes, snapshots versionados, testes com barreiras/falhas no ViewModel, relógio monotônico e duração por tentativa, persistência do contexto, geometria/transformação estável e validação gráfica.

### V4-07 — P1/P2 — Recursos inacessíveis, preferências sem efeito e gates documentais excessivos (S06/S17–S24/R17/R20)

O backup lista somente `filesDir/backups`; não abre arquivo externo nem permite selecionar destino pelo sistema. `PassagesContent` não tem chamador, e o callback `onNavigateToPracticeWithText` da MainActivity ignora o texto. O modo Cópia de Textos não foi integrado. R17 original continua aberto: `importCustomFont` não tem chamador de UI, e seleção de pauta não entrega importação/preview TTF/OTF.

Pressão só altera UiState/snackbar: nenhuma chamada de produção aplica `PressureCalibration.transform`, nem persiste a preferência. Watch continua retornando false; não há callers de timer/fase/atividade de escrita. `PENDENTE_DISPOSITIVO` não cobre essa falta de ligação de software.

Gradle continua permitindo release sem keystore, `isMinifyEnabled=false`, workflow sobe `release/*.apk` sem verificar assinatura/hash/certificado. Documentar secrets não prova publicação assinada; esta revisão não rebaixou nem aprovou um APK público que não verificou. Watchdog não executa lint/assinatura/integração e checa existência/tamanho de artefato, inclusive após build malsucedido. Não foi reexecutado nesta rodada; os comandos Gradle solicitados e lint foram executados diretamente.

Serializers continuam regex/defaults; o round-trip de aspas/barras/newline falhou. `PersonalStyleSerializer` usa `split("}")` e omite ductus na serialização; Teacher mantém defaults/recuperação silenciosa. Não existem classes de teste denominadas `BackupSerializerTest` e `PersonalStyleSerializerTest` na árvore examinada, embora sejam citadas como evidência no dossiê. Existem testes em outras classes, o que não sustenta a referência documental específica.

README ainda anuncia implementação não iniciada; PROJECT_STATE anuncia Ondas 0–3 concluídas e código M0–M8 concluído, apesar da ressalva V3 ao final. A matriz agrupa A16–A21 indevidamente e troca o requisito de R17. **É necessário corrigir a rastreabilidade, não só acrescentar contagem de testes.**

## 4. Os 12 fluxos

Consultados guia e [contato das 12 referências](audit-v4/independent/reference-contact-sheet.jpg), derivado das pranchas originais apenas para inspeção. **Não é screenshot do aplicativo.** Não se exige funcionalidade incidental dos mockups (duplicação, perfil, opções futuras). A navegação canônica Caderno/Praticar/Evolução/Mais foi constatada no código.

| Fluxo | O que existe | Limite/pendência independente |
|---|---|---|
| 01 Biblioteca | Capas, criação, seleção e exclusão com handlers | Inicialização cria Prática Diária e abre escrita, sem percurso inicial vazio; criação/troca precisam do protocolo de persistência V4-06 |
| 02 Páginas | Grade 2 colunas, atual, adicionar/excluir com confirmação | “Miniaturas” são folhas decorativas com cinco linhas, sem traços da página; ordem de saves pendente |
| 03 Ferramentas | Barra, caneta, borracha, guias e exclusão de gestos no código | Sem prova física de borda/palma/toques; fluxo de assinatura não herda a política corrigida |
| 04 Sessão | Cronômetro Guided e pausa manual | Sessão pedagógica de cinco fases/resumo/SRS não conectada à aba atual; timer segue fora da aba |
| 05 Feedback | Sheet, métricas, tentar novamente, avançar, salvar alfabeto | Slant/duração/contexto e destino de alfabeto incorretos; feedback só é invalidado ao concluir stroke, não ao primeiro ponto |
| 06 Replay/comparação | Antes/depois, overlay, replay e calendário | Cache obsoleto, limite 20 sessões, enquadramento variável e guias fixas 52°; não aprovado graficamente |
| 07 Variantes | Tela/seleção/favorita/exclusão | Seeds pessoais, diretório divergente e caches; percurso real não fechado |
| 08 Estilo pessoal | Compilar, persistir parte da definição, usar no caderno | Compila seeds, defaults e JSON parcial; treino não recarrega catálogo dinamicamente |
| 09 Coaching | Diagnóstico/prescrição/navegar | Falsa maestria vazia, medidas incompletas e fallback de glifos; prescrição não concluída pelo resultado |
| 10 Restaurar | Diálogo escolhe backup privado, confirma e importa | Sem seletor nativo/revisão real do manifesto; integridade reprovada em V4-01 |
| 11 Exportar | PNG de página e SVG/PNG de assinatura graváveis | Sem seleção de destino; assinatura pode recortar; PNG não validado em runtime gráfico |
| 12 Preferências | Curvas, lembrete e botão háptico | Preferências sem persistência/efeito no renderer; monitor/Watch não ligado. Demais sugestões visuais não viram requisitos novos |

**Redesenho integral: PARCIAL no código; fidelidade visual, acessibilidade, estados e interação no S25 Ultra NÃO VERIFICADOS.** A ausência de aparelho não impede identificar handlers desconectados, nem autoriza alegar aprovação dos 12 fluxos.

## 5. Decisão e próxima entrega exigida

Não aprovo o fechamento das Ondas 0–3: seus critérios de saída ainda falham. Não aprovo fechamento M0–M8 ou distribuição de release como assinada/verificada. Reconheço os avanços isolados e os resultados positivos dos comandos originais, com os limites da matriz individual.

Prioridade: **integridade do restore e caminhos de dados → seeds/erros falsamente bem-sucedidos → estado compartilhado e sessão real → persistência de assinatura/contexto → métricas/captura → fluxos locais/qualidade → aparelho**. Não iniciar novas expansões para substituir essas correções.

A próxima submissão deve apresentar SHA, delta mínimo por ID original, chamador real, prova que falha nesta versão e passa na correção, round-trip/recuperação e limites físicos. Os 16 testes adicionais são um ponto de partida, não uma lista exaustiva de aceite.

Fontes primárias externas consultadas para delimitar contratos: [Android Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files) — seleção de documento/destino por URI; [Kotlin: estado compartilhado e exclusão mútua](https://kotlinlang.org/docs/shared-mutable-state-and-concurrency.html) — proteger a operação compartilhada completa. As conclusões sobre Scribe derivam do código e das reproduções locais acima.
