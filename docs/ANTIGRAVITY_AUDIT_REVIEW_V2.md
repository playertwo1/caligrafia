# Segunda auditoria independente — Scribe v0.6.0

**Data:** 2026-09-12. **Revisor:** Codex. **Commit:** `728ea0d7790b292bd97d15bf60bd719e776f041f` (origin/main). Comparação com a primeira auditoria de `b31538c`.

## Parecer

**Houve correções reais, e build/testes/lint/CI melhoraram. Ainda não considero M0–M6 completos nem a base pronta para M7.** M4–M6 têm modelos, telas e algoritmos, mas faltam conexões fundamentais com a escrita real. Há métricas/dados sintéticos apresentados na experiência de progresso e alfabeto, temporizador inoperante no caminho padrão e PersonalStyle que não chega ao caderno.

Comecei por `O_QUE_FOI_FEITO_E_O_QUE_FALTA.md` e `AUDIT_REPORT.md`, e segui a indicação para `AUDIT_REPORT_M4_M6.md`. Não existe arquivo chamado `audit_reporta.md` nesta revisão; interpretei o nome como referência a esses relatórios. Dossiês de autoria do implementador são declarações a verificar, não aprovação independente.

A revisão ocorreu em worktree isolada `.audit-worktree`, preservando os arquivos locais da auditoria anterior. O código de produção não foi corrigido. Este relatório e suas evidências estão na pasta principal `docs/`.

## Verificações realizadas

- Checkout limpo de 728ea0d; leitura das regras, especificação, roadmap, arquitetura, estado, dossiês, diferenças das correções e fluxos M3–M6.
- `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`: **sucesso; 152 testes, 33 suítes, zero falhas; APK debug compilado; lint com 0 erros e 37 avisos**.
- Dez testes diagnósticos independentes reproduziram comportamentos defeituosos/limitados. São asserts da situação observada, **não aceitação de produto**. Fontes ficaram em `docs/audit-v2`, fora da suíte normal.
- GitHub API: cinco últimos runs consultados passaram. [Run de 728ea0d](https://github.com/playertwo1/caligrafia/actions/runs/34715157612). O problema de execução do wrapper no Linux foi corrigido no workflow.
- `adb devices`: nenhum dispositivo conectado. Sem inspeção visual no S25 Ultra, benchmark de latência, teste de energia/process death ou verificação criptográfica dos APKs publicados. Não verifiquei cópias do Drive.
- P1 = bloqueia conclusão confiável ou causa dados/resultados incorretos; P2 = correção funcional importante. Riscos sem reprodução estão explicitamente marcados.

## Situação dos 22 achados anteriores

“Corrigido” nesta tabela descreve o defeito delimitado, não aprovação geral do marco.

| Item anterior | Situação nesta revisão | Evidência/reserva |
|---|---|---|
| A01 Ink API | Pendente | adapter continua delegando sempre ao fallback |
| A02 atomicidade | Parcial | falha de serialização preserva arquivo anterior; durabilidade e transações ainda pendentes (R09/R10) |
| A03 concorrência notebook | Pendente | saves independentes e estado entre threads (R19) |
| A04 borracha vira tinta | Corrigido no pipeline | conclusão e preview de ERASER filtrados; acceptance test passou; agrupamento de undo ainda não implementado |
| A05 ponte entre eventos | Corrigido | lote inclui último ponto anterior; acceptance test passou |
| A06 ciclo de vida | Parcial | notebook recebe pausa; treino não, foco/CANCEL/process death não resolvidos integralmente |
| A07 sensores | Parcial | preserva zero, ainda não distingue eixo ausente (R15) |
| A08 POINTER_UP | Corrigido no código | agora extrai históricos e ponto atual; teste físico/multi-touch pendente |
| A09 PNG falso | Corrigido | falha explícita, sem stub de 8 bytes; imagem real não validada na JVM |
| A10 escala export | Não resolvido na UI | parâmetros adicionados, chamada não os preenche (R16) |
| A11 replay estilo | Parcial | copy preserva cor/largura; relógio de parede permanece |
| A12 completude | Parcial | fragmento de dois primeiros pontos reprova; reta completa de dois endpoints também reprova incorretamente (R14) |
| A13 modelo/slant | Parcial | BASIC_SLANT calibrado a 52°; amostragem e estilos novos ainda incoerentes |
| A14 feedback obsoleto | Corrigido | notifyStrokeChanged limpa evaluation |
| A15 cache pautas | Corrigido no fluxo simples | pagesList atualizado; concorrência continua |
| A16 benchmark SQLite | Pendente | implementações CSV/binário e alegações SQLite mantidas |
| A17 cobertura | Parcial | sete acceptance tests melhores, mas sem integração real/Android gráfico |
| A18 lint/CI | Corrigido o erro anterior | 0 erros lint; cinco runs verdes; lint não incluído no workflow |
| A19 assinatura | Parcial | senhas removidas; release sem chave ainda permitido e assinatura não verificada no CI |
| A20 gestos laterais | Pendente de validação | algoritmo não alterado; sem prova física de exclusão integral |
| A21 desempenho | Pendente | save síncrono no laboratório e reconstrução por frame mantidos |
| A22 documentação/gates | Pendente | conclusões totais apesar de lacunas; README/AGENTS e contagem divergentes |

## Achados novos e correções incompletas

### R01 — Timer das aulas não funciona com a construção usada na aplicação

**P1. Evidência:** [LearningViewModel.kt:46](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/ui/LearningViewModel.kt#L46); [SessionTimer.kt:175](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/session/SessionTimer.kt#L175).

**Reproduzido:** `SessionTimer()` não recebe scope e `startTicker` retorna imediatamente. A construção padrão do ViewModel é exatamente essa; não existe outro tick ligado à tela. Após 1,2 s, elapsed continua 0. Os testes existentes avançam artificialmente com `tickOneSecond` e não detectam a integração ausente.
**Correção:** iniciar o ticker em escopo ligado ao ciclo de vida (ex.: viewModelScope), com relógio monotônico, cancelamento e política de background explícitos. **Aceite:** sessão iniciada pela UI avança sem intervenção, pausa/retoma e termina no tempo correto; teste do caminho padrão de construção.

### R02 — Sessão de aprendizado está desconectada da escrita e usa notas escolhidas em botões

**P1. Evidência:** [LearningHubScreen.kt:614](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/ui/LearningHubScreen.kt#L614); [LearningViewModel.kt:111](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/ui/LearningViewModel.kt#L111); [MainActivity.kt:103](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/MainActivity.kt#L103).

**Confirmado por rastreamento:** o diálogo oferece 65%, 80% e 95% para registrar tentativa. Não captura strokes, não navega para o canvas da lição e não recebe resultado do avaliador. Lições de palavras/frases existem como catálogo, mas não como exercícios executáveis conectados a essa sessão. O SRS usa a nota desses botões, sem evidência da escrita correspondente.
**Correção:** conectar sessão→lição→canvas→tentativa avaliada→histórico/SRS. Caso se deseje autoavaliação, identificá-la explicitamente e separá-la de métricas medidas. **Aceite:** completar uma lição real produz stroke IDs, lessonId, duração real e feedback de origem rastreável, sem botões de nota artificial na experiência normal.

### R03 — Evolução exibe exemplos como progresso e não recebe tentativas reais

**P1. Evidência:** [LocalPracticeAttemptRepository.kt:49](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt#L49); [LocalPracticeAttemptRepository.kt:199](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt#L199); [EvolutionViewModel.kt:81](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionViewModel.kt#L81).

**Reproduzido:** diretório vazio gera 4 tentativas, com notas 63, 91, 66 e 89 e datas artificialmente separadas por duas semanas. A busca de `saveAttempt(` na produção encontrou só a declaração e chamadas internas dessas sementes, não no treino/caderno. A tela não rotula claramente os dados como demonstração; comparações e ganhos entram no resumo normal.
**Correção:** remover seeds do repositório real ou usar modo de demonstração explícito e isolado; gravar tentativas reais do treino com procedência. **Aceite:** novo usuário sem prática vê estado vazio, treino real aparece no before/after após duas tentativas, e exemplos nunca alimentam métricas nem M7.

### R04 — Meu Alfabeto nasce com escrita sintética e não tem fluxo de salvar a escrita do usuário

**P1. Evidência:** [LocalPersonalAlphabetRepository.kt:282](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/repository/LocalPersonalAlphabetRepository.kt#L282); [AlphabetViewModel.kt:125](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/ui/AlphabetViewModel.kt#L125); [AlphabetScreen.kt:200](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/ui/AlphabetScreen.kt#L200); [MainActivity.kt:118](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/MainActivity.kt#L118).

**Reproduzido:** novo repositório já contém 4 glifos completos com variantes e notas 89–94. Essas variantes são consideradas pessoais e alimentam compilação. Existe `addVariant` no ViewModel/repositório, mas nenhuma chamada da UI de produção ao método do ViewModel. O atalho “Praticar este Glifo” envia o símbolo e MainActivity o ignora; abre o treino no glifo anterior/padrão, não necessariamente o escolhido. O catálogo de treino também não cobre os 68 caracteres.
**Correção:** separar amostras de variantes do usuário, criar ação “Salvar no meu alfabeto”, passar glyphId/símbolo à navegação e tratar glifos sem exercício. **Aceite:** escolher uma letra, escrever, salvar v1/v2, favoritar e reabrir preserva exclusivamente a escrita feita; compilação não usa seeds como dados pessoais.

### R05 — PersonalStyle é registrado, mas rejeitado, isolado e não persistido

**P1. Evidência:** [StyleEngine.kt:48](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/style/engine/StyleEngine.kt#L48); [AlphabetViewModel.kt:155](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/ui/AlphabetViewModel.kt#L155); [LocalPersonalAlphabetRepository.kt:202](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/repository/LocalPersonalAlphabetRepository.kt#L202); [NotebookPracticeViewModel.kt:59](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L59).

**Reproduzido:** registrar um PersonalStyle e imediatamente chamar `getStyle(id)` devolve `cursiva_escolar`, porque estilo pessoal tem `customFontPath=null` e o método o remove como arquivo ausente. Além disso, Alphabet, Notebook e Guided usam três instâncias separadas do StyleEngine; registrar em uma não atualiza outras. “Usar no Caderno” só navega, sem selecionar/transmitir estilo. O repositório salva ID e timestamp da compilação, mas não a definição ScribeStyle; não há carga posterior dessa definição.
**Correção:** distinguir PERSONAL de CUSTOM_FONT, repositório compartilhado/observável de estilos e persistência da definição e seleção. **Aceite:** compilar, usar no caderno/treino e reiniciar mantém o mesmo estilo resolvido por ID e visível em todos os seletores.

### R06 — Histórico possui caches independentes e telas exibem dados antigos

**P1. Evidência:** [LocalLearningHistoryRepository.kt:21](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/history/LocalLearningHistoryRepository.kt#L21); [EvolutionViewModel.kt:55](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionViewModel.kt#L55); [LearningViewModel.kt:45](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/ui/LearningViewModel.kt#L45).

**Reproduzido:** duas instâncias para o mesmo diretório não compartilham dados; após A salvar sessão, B ainda mostra zero. Se ambas escreverem, B pode sobrescrever o arquivo sem a sessão A; probe terminou apenas com `[b]`. Na aplicação, Learning grava e Evolution lê de sua própria instância, portanto o defeito de leitura obsoleta é diretamente relevante; a sobrescrita por dois escritores demonstra risco da API, sem afirmar que Evolution escreve hoje. `loadData` de Evolution ocorre só na inicialização e não força reload do cache.
**Correção:** instância única observável por aplicação ou leitura/transação consistente com versionamento; atualização ao entrar na tela. **Aceite:** concluir sessão depois de já abrir Evolução atualiza calendário sem reiniciar; múltiplos clientes não perdem sessões.

### R07 — Calendário e totais não representam o histórico completo nem o tempo real

**P2. Evidência:** [LocalLearningHistoryRepository.kt:78](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/history/LocalLearningHistoryRepository.kt#L78); [EvolutionViewModel.kt:74](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionViewModel.kt#L74); [CalendarConsistencyHelper.kt:34](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/engine/CalendarConsistencyHelper.kt#L34).

**Reproduzido:** com 21 sessões de 1 minuto, resumo Learning mostra 21 minutos e Evolution mostra 20 por usar só `recentSessions`. Uma sessão de zero segundos vira 1 minuto no calendário, enquanto total acumulado permanece zero. Também `daysActiveLast30Days` agrupa por divisão UTC de epoch, mas calendário usa fuso local; podem divergir perto da meia-noite. `epochDay` dos dias é sempre zero.
**Correção:** consulta do mês completo e agregação total separada da lista recente; somar segundos antes de arredondar; não fabricar minuto; usar LocalDate/fuso consistente. **Aceite:** mais de 20 sessões, sessões curtas/zero e datas perto de meia-noite produzem o mesmo total/dias em todas as telas.

### R08 — Último segundo da sessão é perdido e mensagem confunde duração planejada com realizada

**P2. Evidência:** [SessionTimer.kt:89](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/session/SessionTimer.kt#L89); [LearningViewModel.kt:152](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/ui/LearningViewModel.kt#L152).

**Reproduzido:** após 300 ticks de uma sessão de 5 minutos, `isFinished=true`, mas `totalElapsedSeconds=299`. O ramo final chama finishSession sem gravar newTotalElapsed. Sem o último segundo, uma sessão completa aparece como 4 minutos no total inteiro. Finalização antecipada informa minutos planejados (5/10/15/20), embora persista `actualDurationSeconds` diferente; com R01 esse valor pode ser zero.
**Correção:** atualizar contadores no tick terminal e informar duração realizada. **Aceite:** 300 ticks registram 300s; encerrar após 30s informa 30s, não 5min; skip não fabrica tempo.

### R09 — fsync é chamado depois de fechar o stream e a falha é ignorada

**P1. Evidência:** [LocalLearningHistoryRepository.kt:111](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/learning/history/LocalLearningHistoryRepository.kt#L111); [LocalPracticeAttemptRepository.kt:175](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt#L175); [DedicatedFileStrategy.kt:98](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/ink/persistence/strategies/DedicatedFileStrategy.kt#L98).

**Código + reprodução do padrão de fechamento:** o `use` do Writer/stream interno fecha o FileOutputStream antes de `fos.fd.sync()`. O probe confirmou descritor inválido e falha de sync. O catch descarta a exceção, embora os relatórios anunciem sincronização física garantida. O binário dedicado também fecha wrappers antes de tentar sync. O manifesto do alfabeto, que escreve diretamente no fos e sincroniza antes de sair do use, não tem esse mesmo problema.
**Correção:** terminar compressão e esvaziar buffers, executar sync com descritor aberto e então fechar. Tratar garantia de atomicidade separada da durabilidade a falha de energia; evitar fallback genérico para move não atômico sem explicitar limitação. **Aceite:** fault injection e observação da sequência finish/flush/sync/close/move, sem exceções de sync suprimidas.

### R10 — Atualizações do alfabeto não são transacionais

**P1. Evidência:** [LocalPersonalAlphabetRepository.kt:81](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/repository/LocalPersonalAlphabetRepository.kt#L81); [LocalPersonalAlphabetRepository.kt:162](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/repository/LocalPersonalAlphabetRepository.kt#L162).

**Risco por análise de concorrência, não reproduzido por interleaving nesta rodada.** add/favorite/delete fazem read-modify-write em Dispatchers.IO sem Mutex. ConcurrentHashMap protege o cache, não a transação do manifesto/Flow. Duas operações podem partir da mesma versão e perder uma mudança. IDs usam versão+milissegundo, sujeitos a colisão concorrente. Delete remove .scribe antes de garantir manifesto novo; falha posterior deixa referência quebrada.
**Correção:** serializar transações, IDs únicos, revisão/versionamento, ordem de commit com recuperação e rollback. **Aceite:** teste com barreiras forçando operações simultâneas e falha de manifesto após exclusão, sem perder variantes ou favoritos.

### R11 — Compilador usa metadados como medição e calcula ângulo com convenção incompatível

**P1. Evidência:** [PersonalStyleCompiler.kt:107](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/engine/PersonalStyleCompiler.kt#L107); [PersonalStyleCompiler.kt:128](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/engine/PersonalStyleCompiler.kt#L128); [PersonalStyleCompiler.kt:170](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/alphabet/engine/PersonalStyleCompiler.kt#L170).

**Reproduzido:** uma linha descendente de 52° conforme convenção do próprio avaliador (dx negativo) resulta em fallback 60° no compilador, pois ele usa atan2(dy,dx), descarta ângulo >90 e não usa atan2(dy,-dx). Quando há qualquer ângulo gravado válido, ele retorna média desses metadados sem analisar strokes, sem ponderação por comprimento e ignorando os demais. Fallback 60 é descrito como “inclinação média observada”.
Proporção também exige revisão: rótulo “alongado” seleciona Ratio323 (ascenderRatio=1,5), menor que Ratio212 (2) selecionada no intervalo inferior; isso inverte a monotonicidade entre extensão medida e pauta recomendada. Contraste usa percentis globais de pressão, sem distinguir ascendente/descendente; não comprova a modulação direcional anunciada.
**Correção:** padronizar geometria, registrar origem/confiança da medida, distinguir fallback de observação e calibrar proporção com fixtures de altura conhecida. **Aceite:** 52°→52° com dados brutos válidos, dados ausentes→“não medido”, altura maior não reduz proporção; descrições refletem algoritmo efetivo.

### R12 — Dual replay altera a velocidade relativa das tentativas

**P2. Evidência:** [DualReplayEngine.kt:165](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/engine/DualReplayEngine.kt#L165); [DualReplayEngine.kt:121](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/engine/DualReplayEngine.kt#L121).

**Reproduzido:** trilha A de 1s e B de 2s, na metade da timeline de 2s, mostram A em 0,5s e B em 1s. A trilha rápida é esticada para terminar junto, contrariando a afirmação de tempo real preservado a 1x. Esse comportamento pode ser útil como modo normalizado, mas precisa ser nomeado e separado do modo de tempo real. O ticker soma 16ms fixos por iteração, sem medir atraso efetivo; frames lentos alongam duração. Não há interpolação entre pontos, apenas filtro de timestamps.
**Correção:** timeline comum em milissegundos com tempo real monotônico e clamp por trilha; oferecer normalização como opção distinta. **Aceite:** após 1s em 1x, A completa e B pela metade; seek/play coerentes sob atraso de frame.

### R13 — Previews de evolução perdem pontos isolados e não transformam coordenadas

**P2. Evidência:** [EvolutionScreen.kt:738](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionScreen.kt#L738); [EvolutionScreen.kt:748](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionScreen.kt#L748).

**Confirmado no renderer:** strokes de um ponto são descartados, apagando pingos/acentos da visualização. Strokes em pixels do canvas de escrita são desenhados diretamente em cards com outras dimensões, sem matriz de escala/origem, podendo sair da área visível. Pautas são redesenhadas como frações do card e sempre com 52°, sem metadados da sessão. O helper de inclinação desenha topo à esquerda da base, contrário à convenção do BASIC_SLANT.
**Correção:** pontos isolados como círculos, transform comum e metadados de pauta/estilo; manter alinhamento Before/After intencional. **Aceite:** preview com ponto, stroke perto da borda e duas resoluções preserva tudo; screenshot/instrumentação necessários para confirmar layout físico.

### R14 — Correção de completude penaliza a mesma linha com amostragem diferente

**P1. Evidência:** [GeometricFeedbackEvaluator.kt:79](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt#L79); [GeometricFeedbackEvaluator.kt:189](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt#L189).

**Reproduzido:** BASIC_SLANT completo com 16 pontos recebe 100%; a mesma reta completa representada pelos dois endpoints recebe 38% e reprovação. A cobertura verifica distância a pontos do aluno, não aos segmentos; portanto confunde baixa densidade com trecho faltante. A medida de slant continua com limiares por amostra, também dependentes da densidade.
**Correção:** cobertura modelo→polilinha com reamostragem uniforme e medida de inclinação por distância acumulada. **Aceite:** mesma geometria em 2/16/160 pontos gera avaliação equivalente; fragmento real continua reprovando. A correção anterior solucionou o exemplo ínfimo, mas não a categoria inteira.

### R15 — Sensores sem suporte continuam sendo gravados como se existissem

**P1. Evidência:** [StrokeCapturePipeline.kt:461](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/ink/capture/StrokeCapturePipeline.kt#L461).

**Correção parcial de A07:** zero válido passou a ser preservado, mas captura ainda não consulta suporte do eixo por dispositivo/source. Valores padrão finitos de eixos indisponíveis são aceitos como medida. O teste reflexivo usa valores fornecidos diretamente e não valida MotionEvent em dispositivo sem o sensor.
**Correção/aceite:** capacidades reais cacheadas; eixo suportado com zero preservado, eixo ausente null, inclusive históricos; teste de integração Android e aparelho. Não substituir o erro “zero→null” por “ausente→zero”.

### R16 — Exportação ganhou parâmetros de escala, mas a UI não os fornece

**P2. Evidência:** [PageExporter.kt:23](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/notebook/export/PageExporter.kt#L23); [NotebookPracticeViewModel.kt:311](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L311).

**Confirmado:** a biblioteca aceita sourceWidth/sourceHeight, mas exportCurrentPage passa apenas width=1440, height=2560 e includeGuidelines=true. Logo no fluxo de usuário scaleX/scaleY permanecem 1. O modelo ainda não preserva sistema de coordenadas de página para resize/rotação. O relatório afirma “matriz de escala real” como recurso concluído.
**Correção:** conectar dimensões lógicas da página e transformar entrada/render/export com preservação de aspecto. **Aceite:** export disparado pela UI mantém proporções de canvas real diferente de 1440×2560; teste com decodificação/pixels, não só existência ou falha esperada.

### R17 — Motor de estilos não entrega importação/preview de fonte no produto

**P1. Evidência:** [StyleFontImporter.kt:105](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/style/importer/StyleFontImporter.kt#L105); [StyleEngine.kt:67](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/style/engine/StyleEngine.kt#L67); [GuidedPracticeViewModel.kt:49](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeViewModel.kt#L49); [GuidedPracticeViewModel.kt:59](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeViewModel.kt#L59).

**Confirmado por busca de chamadas:** importCustomFont e loadTypefaceSafe não são chamados por fluxo de UI da produção. Não há seletor de arquivo conectado; Guided usa engine sem diretório de fontes. Seleção de estilo altera pauta, mas o glifo continua vindo do mesmo catálogo de 12 referências; mudar para estilo de 68° mantém modelo BASIC_SLANT de 52°, reintroduzindo desencontro de pauta/modelo. recommendedStrokeWidth/contrast/ductus são majoritariamente metadados, não pincel ou trajeto por estilo.
**Correção:** definir o escopo real de cada estilo, conectar importação SAF, persistência e Typeface/glyph fallback visual; adaptar referências pedagógicas por estilo ou declarar referência genérica. **Aceite:** importar TTF real e observar o gabarito dessa fonte, reiniciar e reabrir; mudar estilo não penaliza referência exata por alvo incompatível.

### R18 — Comparação anuncia velocidade e seleção de tentativas que não estão implementadas

**P2. Evidência:** [AttemptComparator.kt:32](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/engine/AttemptComparator.kt#L32); [LocalPracticeAttemptRepository.kt:91](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/repository/LocalPracticeAttemptRepository.kt#L91); [EvolutionScreen.kt:650](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/evolution/ui/EvolutionScreen.kt#L650).

**Confirmado:** comparator calcula delta de duração, não distância percorrida/tempo em px/ms. BeforeAfterComparison não tem campo de velocidade. UI seleciona exercício/target, não um par arbitrário de tentativas; repositório escolhe baseline e última automaticamente. Relatórios falam em cadência px/ms e seletores de tentativas. Também comparação sempre usa alvo padrão 52°, sem guardar estilo/pauta no registro de tentativa.
**Correção:** implementar as capacidades anunciadas ou descrever corretamente duração e baseline automático; guardar alvo/estilo da tentativa antes de comparar inclinação. **Aceite:** testes com distâncias e tempos diferentes e seletores de tentativas reais, ou documentação explicitamente reduzida ao comportamento existente.

### R19 — Correções antigas de infraestrutura ainda deixam riscos de dados e validação

**P1. Evidência:** [NotebookPracticeViewModel.kt:126](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L126); [MainActivity.kt:150](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/MainActivity.kt#L150); [AndroidInkRendererAdapter.kt:37](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/app/src/main/java/com/scribe/caligrafia/ink/renderer/AndroidInkRendererAdapter.kt#L37).

**Confirmado por diff e leitura:** notebook ainda dispara saves independentes sem ordenação/revisão e carrega lista mutável na thread IO; substituição atômica de arquivo não impede versão antiga vencer. Pausa agora dá flush no caderno, mas não no treino guiado; não resolve foco/CANCEL/process death integralmente. Laboratório mantém save síncrono no callback e renderer continua reconstruindo toda geometria por frame. Ink API continua fallback incondicional, sem integração real. Benchmark de persistência continua CSV/binário, embora relatório repita comparação SQLite e ganho 9,2x.
**Correção:** retomar os itens A01/A03/A06/A16/A20/A21 da revisão anterior; não considerar a lista inteira fechada pela aprovação de sete acceptance tests. **Aceite:** matriz abaixo e evidência no S25 Ultra, ainda não conectado nesta revisão.

### R20 — Declarações de conclusão e qualidade ultrapassam os testes executados

**P1. Evidência:** [O_QUE_FOI_FEITO_E_O_QUE_FALTA.md:10](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/O_QUE_FOI_FEITO_E_O_QUE_FALTA.md#L10); [AUDIT_REPORT_M4_M6.md:238](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/AUDIT_REPORT_M4_M6.md#L238); [README.md:3](https://github.com/playertwo1/caligrafia/blob/728ea0d7790b292bd97d15bf60bd719e776f041f/README.md#L3).

**Confirmado:** 152 testes em 33 classes passaram aqui; O_QUE_FOI_FEITO e PROJECT_STATE dizem 149, AUDIT_REPORT_M4_M6 diz 152. README/AGENTS ainda apresentam estado inicial/M0 enquanto outros documentos aprovam M6/M7. Lint agora tem 0 erros, mas 37 avisos. Testes continuam com `isReturnDefaultValues=true`, sem suíte androidTest; o PNG é validado por falha controlada na JVM, não imagem real. CI agora funciona, mas workflow ainda não executa lint nem verifica explicitamente assinatura antes de publicar; sem keystore o Gradle permite release não assinado. Senhas hardcoded foram removidas — melhoria confirmada.
**Correção:** relatar funcionalidades integradas versus modelos/helpers/demos, atualizar matriz e testes de ponta a ponta, identificar exemplos e remover promessas de “100%”. **Aceite:** M7 só depois de base real conectada e volume de sessões reais; 152 testes verdes não comprovam M4–M6 completos.

## Resultados dos probes

| Diagnóstico | Observado em 728ea0d |
|---|---|
| Timer padrão depois de 1,2s | 0 segundos decorridos |
| Sessão de 300 ticks | terminou com 299 segundos |
| Registro de PersonalStyle | resolver devolve cursiva_escolar |
| Dados de usuário novo | 4 tentativas pontuadas e 4 glifos completos sintéticos |
| Duas instâncias de histórico | B não vê A; após escrita de ambas resta só sessão B |
| Totais de 21 sessões | aprendizado=21 min; evolução=20 min |
| Sessão de zero segundos no calendário | contabiliza 1 minuto |
| Compilador com linha real de 52° | fallback de 60° |
| Dual replay 1s/2s na metade | A em 0,5s, B em 1s; tempos relativos normalizados |
| Mesma reta com 16 ou 2 pontos | 100% versus 38% |
| fsync depois de Writer.use | descritor fechado, sync falha |

## Evidências e reprodução

- [Resultados das 33 suítes existentes](audit-v2/baseline-tests.json).
- [Log de build/test/lint](audit-v2-build.log) e [relatório completo de lint](audit-v2/lint-results-debug.txt).
- [Fonte dos dez probes](audit-v2/AuditV2ProbeTest.kt), [resultado XML](audit-v2/probe-results.xml) e [log](audit-v2/probes-build.log).

Para repetir em checkout de 728ea0d, copiar temporariamente `AuditV2ProbeTest.kt` para `app/src/test/java/com/scribe/caligrafia/audit/` e executar:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME = 'C:/Users/fael/AppData/Local/Android/Sdk'
./gradlew.bat testDebugUnitTest --tests 'com.scribe.caligrafia.audit.AuditV2ProbeTest' --console=plain
```

Remover a cópia temporária ao terminar. Os probes documentam o defeito; após correção, inverter as expectativas e transformar em testes de aceitação. O timer padrão usa espera curta para detectar ausência total de ticker; para validar precisão usar relógio/dispatcher injetável. O probe de sync reproduz o padrão de fechamento, não mede resistência física a queda de energia.

## Otimizações e recomendações adicionais

- Substituir histórico/estilos por repositórios únicos e observáveis, mantendo I/O e compilação fora da thread principal. Learning/Evolution leem/escrevem sincronamente dentro de `viewModelScope.launch` padrão e alguns construtores fazem seed/I/O na principal; medir StrictMode/frame times.
- Usar cache de geometria estática e cálculo incremental em canvas, evitando alocar/copiar listas inteiras por frame. Não declarar ganhos antes de benchmark real.
- Parsing JSON manual por substrings/regex e manifesto delimitado por `|` precisam casos com aspas, barras, quebras de linha, delimitadores e corrupção. Preferir serializador consolidado; não classifiquei como falha reproduzida nesta rodada. Repositórios não devem substituir silenciosamente dados corrompidos por seeds.
- Seleção de glifo/variante usa coroutines sem token de seleção: retorno lento de preview anterior pode preencher a seleção nova. Adicionar cancelamento/checagem de ID e teste com atraso controlado.
- Comparações e currículo precisam registrar versão do estilo/alvo e procedência da nota. Isso evita comparar métricas incompatíveis ou treinar M7 com exemplos artificiais/autoavaliações.
- Conferir layout da toolbar do caderno no aparelho: recebeu vários atalhos adicionais; sem screenshot real não afirmo overflow confirmado. Manter termos M4/M5/SCR fora da UI final e separar laboratório.
- SRS é uma heurística determinística; não há evidência apresentada de validação científica dos multiplicadores 1,5/2,2 para consolidação neuromotora. Descrever como heurística do produto e calibrar com dados reais.

## Plano obrigatório de estabilização para o Antigravity

**Diretriz do usuário após esta auditoria:** detalhar as correções e evitar desvios de escopo. Este plano governa a execução da estabilização; a ordem abaixo substitui a sequência resumida anterior. Não é autorização para implementar novos milestones. Os achados permanecem vinculados a 728ea0d; o plano pode orientar commits posteriores, que precisam ser identificados.

### 1. Missão atual e limites de trabalho

A missão é tornar os fluxos existentes corretos, conectados à escrita real, persistentes e verificáveis. “Ter uma classe”, “ter uma tela”, “compilar”, “ter testes verdes” e “funcionar de ponta a ponta” são estados diferentes e devem ser reportados separadamente.

Regras de contenção:

1. Trabalhar nas correções R01–R20 e nos itens A01–A22 ainda abertos, seguindo as ondas deste plano. Referenciar o ID em cada alteração relevante.
2. Não iniciar M7/Professor IA, backend, nuvem, autenticação, assinatura, marketplace, novos cursos ou expansão de catálogo enquanto a estabilização estiver pendente. O roadmap descreve futuro, não autorização automática.
3. Não criar telas novas para aparentar completude quando a conexão entre as existentes estiver faltando. Componentes adicionais são aceitáveis apenas quando necessários ao fluxo de correção identificado.
4. Não fazer redesign geral, modularização massiva, troca de arquitetura ou atualização indiscriminada de dependências. Uma extração pequena que elimine a causa comprovada é aceitável e deve ser justificada pelo ID.
5. Não avançar de onda por quantidade de commits, versão do APK ou número de testes. A saída depende dos critérios observáveis definidos abaixo.
6. Não renumerar IDs para esconder pendências. Uma correção parcial continua parcial, com subitens explícitos.
7. Não alterar expectativa de teste para aceitar o bug; corrigir implementação ou demonstrar por que o requisito/teste estava errado. Preservar a evidência original e registrar a decisão.
8. Não introduzir defaults, seeds, sleeps arbitrários, catches genéricos ou fallbacks silenciosos para simular resultado. Fallback legítimo deve identificar a limitação e preservar dados.
9. Não atribuir “aprovado pelo Codex”, “validado no S25 Ultra”, “100% concluído”, “sem perda de dados” ou “baixa latência comprovada” sem evidência correspondente. O implementador entrega correção candidata; não fabrica aprovação independente.
10. Não publicar release nova apenas para encerrar uma onda. Publicação deve seguir autorização existente e testes aplicáveis; nome/assinatura do artefato precisam corresponder ao que foi produzido. Não marcar release não assinada como assinada.
11. Uma limitação externa (ex.: telefone desconectado) não impede correções independentes. Executar o que for possível e marcar somente o teste dependente como pendente; nunca inventar resultado físico.
12. Se aparecer defeito novo que bloqueia a correção atual, abrir ID adicional estável, explicar dependência e resolver o mínimo necessário. Melhorias adjacentes sem dependência ficam em backlog.

### 2. Inicialização obrigatória de cada retomada

Antes de editar código:

- Ler `AGENTS.md`, este relatório, o plano de execução corrente e `PROJECT_STATE.md`.
- Conferir branch, SHA e alterações locais. A auditoria foi feita em `728ea0d`; a pasta principal desta sessão ainda contém a base antiga e documentos locais. A versão auditada está em `.audit-worktree`. **Não aplicar correções no código antigo por engano.** Em outro checkout, identificar o SHA equivalente e conferir diferenças.
- Preservar alterações existentes. Não executar reset/clean, substituir arquivos de auditoria nem descartar trabalho alheio para “sincronizar”.
- Ler as funções citadas pelo achado e seus chamadores reais. Links neste relatório apontam para linhas do commit auditado, que podem mudar após correções.
- Registrar o item ativo, o comportamento incorreto, a menor correção pretendida e os testes que distinguem sucesso de falso sucesso.
- Se o código mudou desde a auditoria, classificar cada item como ainda reproduzível, corrigido com evidência, parcial ou não verificável. Não assumir que texto de changelog prova implementação.

Ao retomar após interrupção, continuar pelo item ativo registrado; não reiniciar todo o projeto nem saltar para a próxima feature visível.

### 3. Estados e definição de encerramento

| Estado | Quando usar | Evidência mínima |
|---|---|---|
| ABERTO | defeito/lacuna identificado | ID, cenário, referência ao código |
| EM_CORRECAO | trabalho ativo delimitado | hipótese causal, arquivos e teste planejado |
| PARCIAL | parte do item corrigida | subitens feitos e faltantes, sem fechar o ID inteiro |
| IMPLEMENTADO_NAO_VALIDADO | código alterado, sem prova suficiente | diff/commit e lista de validações pendentes |
| VALIDADO_AUTOMATICAMENTE | teste aplicável executado | comando, resultado, SHA, assertions relevantes |
| PENDENTE_DISPOSITIVO | falta confirmação física específica | roteiro e motivo; indicar testes locais já feitos |
| PRONTO_PARA_REVISAO | implementação e evidências disponíveis | pacote de entrega completo, limites explícitos |
| FECHADO | critérios aplicáveis satisfeitos e revisão registrada | referência da revisão; identificar revisor e alcance real |

Esses estados não significam que todo item exige aparelho. Algoritmo puro pode ser verificado automaticamente; latência, gestos, pixels reais e lifecycle completo exigem evidência adequada. Se o revisor for o próprio implementador, registrar como autorrevisão, nunca como revisão independente.

### 4. Ondas de execução e dependências

#### Onda 0 — Baseline e isolamento de dados artificiais

**IDs:** R03/R04/R20; preparação para os demais.

- Registrar SHA de partida, build, resultados por suíte, lint e testes disponíveis.
- Identificar seeds por IDs/origem. Para instalações existentes, produzir migração específica para remover ou isolar somente amostras conhecidas. Não apagar variantes reais, arquivos desconhecidos ou o histórico inteiro.
- Manter demos em modo explicitamente separado e identificado, caso sejam úteis. Nenhuma demo entra em progresso, estilo pessoal, SRS ou dados futuros de IA.
- Abrir a matriz de acompanhamento de todos os IDs. Registrar lacunas sem promover gates.

**Saída:** usuário novo possui zero prática pessoal e zero evolução inventada; instalação com dados reais preserva esses dados; baseline e matriz disponíveis. Ainda não declarar M4/M5/M6 concluídos.

#### Onda 1 — Fundamentos de estado, persistência e relógio

**IDs:** R01/R06/R08/R09/R10/R19; A02/A03/A06/A11 remanescentes.

- Corrigir ticker padrão e contagem terminal; tornar relógio/dispatcher testáveis.
- Estabelecer proprietário único do estado compartilhado e uma ordem de escrita por documento. Não é obrigatório instalar framework de DI: solução simples e explícita pode atender.
- Tratar atomicidade de arquivo, ordenação de versões, transação entre arquivos e durabilidade como quatro problemas separados.
- Conectar lifecycle dos pipelines ativos e liberar callbacks das Views descartadas.
- Garantir mensagens de erro e recuperação coerentes, sem “salvo” após falha.

**Saída:** probes de timer/cache/ordenação passam com expectativas corrigidas; fault injection preserva último estado válido; cancelar/navegar não mistura páginas; desenho não é sobrescrito por snapshot antigo.

#### Onda 2 — Escrita real conectada a aprendizado, evolução e alfabeto

**IDs:** R02/R03/R04/R05/R06.

- Criar o percurso verificável: iniciar lição → abrir exercício correto → capturar escrita → avaliar → salvar tentativa → atualizar histórico/SRS → visualizar evolução → selecionar tentativa → salvar variante → compilar estilo → usar no caderno.
- Transmitir IDs e contexto de navegação; não depender do último glifo que estava aberto.
- Compartilhar registro de estilos e persistir PersonalStyle completo. Tratar estilo pessoal sem fonte como categoria válida.
- Atualizar telas já abertas a partir do estado persistido/observável, sem exigir reinício para ver mudanças.

**Saída:** roteiro de ponta a ponta abaixo completo com dados reais ou eventos de input controlados; prova de que os handlers da UI chamam a lógica correta. Testar apenas métodos de repositório não satisfaz esta saída.

#### Onda 3 — Geometria, métricas e fidelidade visual

**IDs:** R07/R11/R12/R13/R14/R15/R16/R17/R18; A08/A10/A12/A13 remanescentes.

- Corrigir medidas independentes da densidade de amostragem, origem de sensor e convenção angular.
- Separar replay em tempo real de comparação normalizada.
- Conectar espaço lógico de página a preview/export; preservar pontos isolados e aspecto geométrico.
- Conectar fontes e estilos ao gabarito visual correto; registrar limites de cobertura por estilo.
- Reconciliar calendário e totais a partir de segundos reais e datas locais consistentes.

**Saída:** testes adversariais da matriz abaixo aprovados; pixels/PNG avaliados em runtime gráfico adequado; nenhuma nota se apresenta como medição quando é default ou escolha manual.

#### Onda 4 — Performance, validação física e ferramentas de qualidade

**IDs:** R19/R20; A01/A16/A17/A18/A19/A20/A21 remanescentes.

- Integrar ou relatar corretamente a Ink API. Comparar alternativas reais, não dois nomes que chamam o mesmo renderer.
- Refazer benchmark com estratégias reais se continuar havendo comparação SQLite/Room; caso contrário, nomear CSV/binário e limitar conclusões.
- Medir e otimizar os gargalos comprovados. Instrumentar antes de prometer melhorias.
- Incluir lint e verificações relevantes no CI/Watchdog; verificar assinatura quando a saída for de publicação assinada.
- Validar S25 Ultra com versão Android/One UI e condições registradas.

**Saída:** relatório antes/depois com dados brutos e método, build/CI consistente e limitações físicas explícitas. Se o aparelho não estiver disponível, manter a validação física pendente e entregar o restante.

#### Onda 5 — Consolidação documental e reauditoria

**IDs:** R20/A22 e fechamento de matriz.

- Atualizar README, PROJECT_STATE, ROADMAP, BACKLOG e relatórios com a mesma realidade de implementação e validação.
- Preservar os relatórios históricos; acrescentar resposta por ID e evidência, sem apagar achados antigos ou reescrever a história como se nunca tivessem existido.
- Entregar o pacote de revisão definido ao final. Não iniciar M7 por conta própria após a entrega.

**Saída:** nenhuma pendência escondida sob “100%”; revisor consegue reproduzir resultados do SHA entregue.

### 5. Contrato mínimo dos dados de ponta a ponta

Esta é uma proposta de contrato a adaptar ao modelo existente, não obrigação de criar todas essas classes ou de fazer reestruturação ampla. A informação precisa existir em local apropriado e ser rastreável.

| Informação | Necessidade | Invariante |
|---|---|---|
| attemptId e revision | identidade e atualização | única; save repetido não duplica tentativa nem reverte versão |
| origem do dado | separar prática, demo e autoavaliação | demo nunca conta como prática pessoal; origem desconhecida não é inventada |
| lessonId/glyphId/targetId | ligar lição ao que foi escrito | navegar por letra seleciona essa letra ou informa ausência de exercício |
| sessionId | relacionar timer e tentativas | finalizar sessão não associa escrita de outra sessão |
| styleId + versão/configuração do alvo | comparar critérios equivalentes | nota guarda o alvo usado; mudança posterior de estilo não reinterpreta nota antiga |
| origem/unidade dos tempos | replay e histórico | uptime não é data civil; não ordenar boots diferentes como uma sessão única |
| duração real e planejada | contagem honesta | minutos exibidos derivam de duração real; skip não soma tempo não praticado |
| stroke IDs + pontos originais | fidelidade | não substituir original por smoothing, previsão ou bitmap |
| coordenadas/dimensões lógicas | transformar View/PNG/preview | input e desenho usam transformações coerentes e reversíveis |
| presença dos eixos | distinguir zero de ausência | null = indisponível; zero válido continua zero |
| origem da avaliação | distinguir heurística, medida e autoavaliação | nota manual nunca é rotulada como resultado geométrico automático |
| sourceAttemptId da variante | alfabeto rastreável | variante salva mantém ligação opcional correta; não fabrica vínculo |
| definição persistida do estilo | sobreviver a reinício | ID resolve para o mesmo estilo sem exigir TTF para PERSONAL |

Migrações precisam tratar registros antigos com campos ausentes. Não preencher ângulo, origem de sensor, duração ou aprovação com valores plausíveis e apresentá-los como observados. Quando não houver dado, usar estado desconhecido/ausente e fallback visual identificado.

### 6. Roteiros obrigatórios de integração

#### Fluxo A — Usuário novo, sem dados artificiais

1. Criar armazenamento de teste vazio, sem apagar dados de uso real.
2. Abrir aprendizado, evolução e alfabeto.
3. Confirmar zero sessões/tentativas pessoais/glifos do usuário.
4. Se houver demo, confirmar indicação visível e exclusão de totais/compilação/SRS.
5. Reiniciar e confirmar que as telas não repovoam prática artificial.

#### Fluxo B — Uma lição real até o histórico

1. Selecionar lição e duração de 5 minutos.
2. Confirmar exercício e estilo corretos no canvas.
3. Esperar/ticar relógio controlado; pausar e retomar.
4. Escrever uma tentativa válida; avaliar e guardar ID e dados capturados.
5. Finalizar após duração conhecida; confirmar mensagem com tempo realizado.
6. Abrir evolução que já havia sido aberta antes; confirmar atualização sem reiniciar.
7. Reiniciar processo e conferir sessão/tentativa, campos e pontos salvos.
8. Repetir save/retorno de tela e confirmar que não duplicou a tentativa.

#### Fluxo C — Antes/depois e meu alfabeto

1. Fazer duas tentativas reais do mesmo alvo com durações e geometrias conhecidas.
2. Confirmar origem, estilo e baseline do comparativo; não misturar exemplos.
3. Reproduzir em tempo real; trilha de 1s termina antes da de 2s.
4. Salvar tentativa como variante v1; salvar outra como v2; trocar favorita.
5. Voltar ao glifo e confirmar preview correspondente, inclusive com carregamento atrasado.
6. Compilar estilo e aplicar no caderno/treino.
7. Reiniciar processo: estilo resolve por ID, seleção e geometria permanecem.

#### Fluxo D — Interrupções e falhas

1. Iniciar traço; bloquear tela, trocar app, abrir diálogo, navegar e testar rotação separadamente.
2. Definir expectativa de cada cancelamento: preservar traço válido ou descartar input rejeitado, sem tratar tudo como o mesmo caso.
3. Simular falha antes de escrever, no payload, no sync, no move e no commit de manifesto.
4. Confirmar recuperação do último estado válido e mensagem de falha.
5. Forçar saves fora de ordem com barreiras de teste; último estado lógico deve vencer.
6. Executar add/favorite/delete de variantes com concorrência controlada.
7. Verificar que apagar variante não deixa manifesto apontando para arquivo removido após falha.

Não declarar que esses roteiros foram executados só porque foram descritos. Cada execução precisa de resultado, ambiente, SHA e evidência.

### 7. Matriz de testes adversariais

| Área | Casos mínimos | O que o teste deve detectar |
|---|---|---|
| Timer | construtor padrão; 300/600/900/1200s; pausa; cancel; skip; atraso no scheduler | ticker inexistente, último segundo perdido, contagem fictícia ou drift |
| Histórico | duas instâncias/clientes; 0, 20, 21 e muitas sessões; reinício | cache obsoleto, sobrescrita, truncamento de métricas |
| Calendário | 0s, 30s+30s, 59s+1s; meia-noite local; mudança de mês | arredondamento por sessão, dia UTC incorreto, totais incompatíveis |
| Identidade | mesmo attemptId salvo duas vezes; revisões fora de ordem | duplicação de sessão, snapshot antigo vencendo |
| Atomicidade | falha em cada fase; arquivo anterior válido; arquivo corrompido | perda da versão anterior e falso sucesso |
| Alfabeto | novo usuário; v1/v2; favorita; exclusão; concorrência | seeds como dados pessoais, variante órfã, lost update |
| Estilo pessoal | null customFontPath; resolver por ID; três telas; reinício | fallback indevido, instância isolada, estilo não persistido |
| Sensores | eixo ausente; eixo válido=0; histórico; pressão não finita | informação inventada e perda de zeros válidos |
| Captura | palma+stylus; POINTER_UP; CANCEL; movimento com histórico | perda de amostra final, mistura de ponteiros e estado preso |
| Borracha | DOWN→MOVE→UP rápido; vários lotes; um ponto; undo da passada | lacunas entre eventos, tinta de borracha e undo fragmentado |
| Avaliador | mesma reta em 2/16/160 pontos; densidade irregular; fragmento real | confundir amostragem com qualidade/completude |
| Direção | oval reverso; começo=fim; ordem de strokes trocada | aprovar sentido pela mera proximidade dos endpoints |
| Ângulo | 52°, 68°, 90°; downstroke com dx negativo; metadado ausente | convenção errada, fallback apresentado como observação |
| Replay | 1s e 2s; 0.5x/1x/2x; seek; frame atrasado; ponto isolado | normalização oculta, drift, desaparecimento e perda de estilo |
| PNG/preview | tamanhos distintos; borda; acento; transparência; alocação falha | corte, deformação, ponto ausente e PNG inválido |
| Fonte | TTF/OTF real; corrompida; header apenas; glyph ausente; reinício | validação superficial e fallback não identificado |
| UI assíncrona | selecionar A, depois B, resposta de A chega por último | preview/dado de seleção errada |
| Migração | dados reais+seeds antigos; arquivo desconhecido; versão anterior | exclusão indevida e preenchimento inventado |

Testes devem afirmar invariantes do produto. Não adicionar centenas de testes de getters para aumentar o total. Usar testes puros para matemática, integração para repositório/chamadores, runtime Android para desenho/entrada e aparelho para comportamento físico. `isReturnDefaultValues=true` não comprova que APIs Android funcionaram.

### 8. Plano de otimização com evidência

**Otimização não pode alterar a fonte primária dos strokes nem adiar correção de perda de dados.** As propostas abaixo são candidatos a medir, não bugs novos comprovados nem promessa de ganho.

| Prioridade | Candidato | Medida antes/depois | Restrição |
|---|---|---|---|
| Alta | remover I/O/compressão da thread de input/UI | StrictMode, tempo por callback e frames longos | fila deve preservar ordem e flush confiável |
| Alta | coalescer saves do mesmo documento | writes por minuto, bytes gravados, latência de persistência | nunca descartar a revisão final; saída segura de lifecycle |
| Alta | cache de geometria dos strokes concluídos | duração de onDraw, alocações/frame, GC | invalidar em undo/erase/style/transform corretos |
| Alta | atualização incremental do stroke ativo | cópias de pontos e custo ao aumentar comprimento | capturar todos os pontos reais, sem amostrar input destrutivamente |
| Média | cache de bounds e índice espacial de borracha | candidatos examinados e tempo por lote | incluir segmento entre eventos e pontos isolados |
| Média | reamostragem derivada/índice para avaliação | tempo e invariância de nota | preservar vetor original e cobertura legítima |
| Média | leitura preguiçosa de vetores em histórico/alfabeto | tempo para abrir, memória residente | não manter todos os arquivos grandes em cache sem limite |
| Média | atualizações de UI por estado necessário/frame | recomposições e jank | não recompor tela inteira a cada amostra física |
| Média | parse e serialização robustos | round-trip, tempo, memória, casos escapados | migrar sem perda; não substituir parser sem teste de compatibilidade |
| Baixa | minificação/tamanho de APK | tamanho e startup release | validar reflexão/ViewModels e bibliotecas nativas antes de habilitar |
| Baixa | limpeza de recursos/deprecações | lint e manutenção | não transformar em upgrade amplo de stack |

Protocolo de medição sugerido:

- Cargas pequenas e longas, incluindo 10 mil e 50 mil pontos, múltiplos strokes, borracha, undo e replay. Esses tamanhos são cargas propostas, não promessa de capacidade já validada.
- Registrar modelo, Android/One UI, resolução, densidade, refresh rate, build debug/release, versão do renderer, estado térmico e método.
- Repetir o mesmo cenário e reportar mediana e percentis úteis, mantendo resultados brutos. Separar cold start de warm start.
- Para frame budget, usar a taxa realmente medida/configurada: 60Hz corresponde a cerca de 16,7ms; 120Hz a cerca de 8,3ms. Isso não é orçamento inteiro disponível ao aplicativo nem medida automática de latência ponta→tinta.
- Separar latência de captura, CPU de desenho, frame apresentado e latência visual ponta→tinta. Um cronômetro ao redor de onDraw não mede todas elas.
- Definir limiar de aceite e método antes da comparação e registrar quem o definiu. Não escolher um limiar depois de ver o resultado só para aprovar.
- Se não houver equipamento/método para medir latência visual, reportar somente métricas medidas; não deduzir “igual ao Samsung Notes” por aparência subjetiva.
- Ganho percentual só pode comparar a mesma carga, ambiente e método; não comparar CSV desktop com SQLite imaginário no telefone.

### 9. Regras de erro e recuperação

- Erro de disco precisa chegar ao estado da UI e à evidência de teste; `finally { isSaving=false }` não é tratamento do erro.
- Não capturar `Throwable` indiscriminadamente para converter qualquer falha em sucesso. Preservar cancelamento de coroutines e diferenciar indisponibilidade recuperável de falha de programação.
- Publicar em memória uma revisão como salva somente quando o contrato de persistência tiver sido satisfeito. Se houver UI otimista, indicar estado pendente/falha e recuperar coerentemente.
- Arquivo temporário único resolve colisão de nome; não resolve lost update. Mutex por instância resolve um cliente; não resolve caches independentes para o mesmo arquivo.
- `ATOMIC_MOVE` resolve substituição de um arquivo em condições suportadas; não torna atualização de manifesto+vetor uma transação completa nem garante sync após fechamento.
- Corrupção deve produzir recuperação/quarentena diagnosticável. Não recriar seeds sobre arquivo potencialmente recuperável.
- Em testes destrutivos, usar diretórios temporários exclusivos e conferir seus caminhos. Nunca usar dados reais do usuário como fixture.
- Logs não devem despejar texto escrito, strokes completos ou arquivos privados sem necessidade; preferir IDs de teste, contagens, versão e erro sanitizado.

### 10. Critérios de produto que não podem ser substituídos por aparência

- Progresso precisa representar ações do usuário. “Não punitivo” não significa esconder regressão medida nem fabricar evolução positiva.
- “Meu Alfabeto” precisa permitir inserir escrita própria, além de visualizar e favoritar exemplos.
- “Usar estilo” precisa alterar o estado realmente usado pela tela, sobreviver ao ciclo de vida esperado e preservar referências corretas.
- “Importar fonte” precisa partir de arquivo selecionável pelo usuário e chegar ao gabarito visível; função sem chamador não é recurso entregue.
- “Treinar frase” precisa chegar a um exercício aplicável, não apenas a um título no currículo.
- “Exportar PNG” precisa produzir imagem decodificável e acessível pelo fluxo de produto; mensagem com nome de arquivo não prova que o usuário consegue obtê-lo.
- “Replay 1x” precisa respeitar a definição declarada de tempo. Se for normalizado, deve estar explícito.
- Defaults de estilo e pincel podem existir, mas não devem ser exibidos como medidas observadas da escrita pessoal.
- Aprovação automática de nota não deve ser apresentada como avaliação absoluta da qualidade da caligrafia. Rever linguagem e escopo de feedback conforme PRODUCT_SPEC.

### 11. Checklist por correção e modelo de entrega

Copiar este registro para cada ID no acompanhamento. Não preencher “passou” antes de executar.

```markdown
### Rxx — Título
- Estado atual:
- SHA de partida / SHA da correção:
- Causa raiz e cenário de reprodução:
- Chamador real da UI afetado:
- Arquivos alterados e motivo:
- Menor mudança necessária:
- Dados/migração afetados:
- Teste que falhava antes:
- Comando executado após a mudança:
- Resultado e caminho da evidência:
- Integração/UI validada em:
- Dispositivo/Android/One UI, se aplicável:
- Limitações/subitens ainda abertos:
- Regressões adjacentes verificadas:
- Revisão: autorrevisão ou revisor identificado:
- Próxima ação permitida e dependência:
```

A cada entrega de onda, incluir:

1. Resumo funcional antes/depois, sem linguagem promocional.
2. IDs corrigidos, parciais e pendentes em listas separadas.
3. Diff/commits e efeito em dados existentes.
4. Comandos/resultados de testes relevantes; build/lint após integração. Evitar repetir testes sem mudança ou motivo.
5. Resultado do roteiro de ponta a ponta aplicável e evidência real de UI/dispositivo quando exigida.
6. Otimizações com medição ou marcadas como propostas não medidas.
7. Riscos remanescentes e próxima onda; nenhum gate promovido sem critérios.

### 12. Checklist final de reauditoria

- [ ] R01–R20 e A01–A22 possuem resposta individual e subitens remanescentes explícitos.
- [ ] Não há prática sintética misturada ao histórico pessoal, SRS ou compilação de estilo.
- [ ] Timer padrão e suas fases funcionam no fluxo de aplicação.
- [ ] Tentativa real chega a histórico/evolução/alfabeto com origem e IDs corretos.
- [ ] PersonalStyle é resolvido, compartilhado e restaurado após reinício.
- [ ] Saves concorrentes e falhas não destroem revisão válida nem misturam páginas.
- [ ] Sensor ausente é distinguido de valor zero real.
- [ ] Avaliação é estável sob reamostragem da mesma geometria.
- [ ] Export/preview preservam coordenadas, estilo e pontos isolados.
- [ ] Replay em tempo real e normalizado têm semânticas explícitas.
- [ ] Build/test/lint/CI refletem o SHA entregue; assinatura validada quando houver publicação assinada.
- [ ] Benchmark/validação no S25 Ultra têm evidências, ou estão claramente pendentes.
- [ ] Documentos concordam sobre implementação, validação e próximos passos.
- [ ] Relatórios históricos foram preservados e nenhuma aprovação foi atribuída sem revisão.
- [ ] Não foram introduzidas features fora da estabilização nem avanço autônomo a M7.

**Instrução final ao Antigravity:** se terminar uma tarefa, atualize a evidência e continue apenas no próximo item permitido deste plano. Se algo estiver apenas desenhado, stubado, demonstrado com dados sintéticos ou validado em teste puro, use esse nome. Não substitua ligação de ponta a ponta por telas bonitas, quantidade de testes ou texto de release.
