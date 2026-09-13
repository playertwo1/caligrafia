# Auditoria independente para o Antigravity — Scribe v0.2.0

Data: 2026-09-12. Revisor: Codex. Base auditada: `b31538c443b21318762e14a34cc065a8027dfc53` (`main`, após fast-forward dos sete commits novos).

## Parecer

**Há implementação substancial, mas o projeto não está validado como “M0/M1/M2 100% concluídos”.** Encontrei defeitos de preservação de dados, borracha, avaliação e exportação, além de funcionalidades descritas como prontas que são placeholders ou simulações. Recomendo corrigir e revalidar a base antes de avançar o Style Engine. Este parecer não afirma má-fé do autor; compara afirmações com evidências verificáveis.

Comecei pelo `AUDIT_REPORT.md` fornecido pelo Antigravity e preservei seu conteúdo como declaração histórica do autor. Este arquivo é a revisão independente. Não corrigi código de produção nem avancei milestone.

## Verificação executada e limites

- Leitura de AUDIT_REPORT, regras, especificação, roadmap, arquitetura, motor de stylus, estado, backlog, relatório de persistência, código dos fluxos principais, testes, Gradle, CI e Watchdog.
- `gradlew.bat testDebugUnitTest assembleDebug lintDebug --console=plain`, com JBR do Android Studio e SDK Android local: **91 testes / 17 suítes passaram; assembleDebug passou; lintDebug falhou (1 erro, 35 avisos)**. O comando agregado termina em falha por lint.
- Seis probes JVM executados separadamente: todos confirmaram as condições defeituosas esperadas. Não são seis testes de aceitação aprovando o produto. Os fontes ficaram em `docs/audit`, fora da suíte normal, para não transformar bugs em contrato permanente.
- `adb devices`: nenhum dispositivo conectado. Sem teste físico de S25 Ultra, screenshots de UI real, medição de latência ou instrumentação nesta auditoria. Achados estáticos/riscos são identificados explicitamente.
- Release v0.2.0 existe no GitHub, com debug de 22.174.917 bytes e release de 16.225.681 bytes, confirmados pela API pública. Isso confirma anexos, **não** instalação, identidade com o fonte ou assinatura. Não verifiquei cópias do Drive/keystore privado.
- Os cinco últimos workflows consultados falharam; release publicada pode ter sido gerada manualmente. Não confundir anexos existentes com CI funcionando.
- Segunda execução de testes encontrou bloqueio de diretório de resultados no Windows; usei diretório binário separado via init script e a execução dos probes concluiu. Não classifico esse bloqueio local como bug do aplicativo.

## Achados e ações

P1 = corrigir antes de considerar a base aprovada (dados, resultado incorreto ou verificação essencial). P2 = correção funcional/qualidade importante. Cada item traz sua forma de confirmação e teste de aceite.

### A01 — Android Ink API declarada, mas não integrada

**Prioridade:** P1. **Evidência:** [AndroidInkRendererAdapter.kt:31](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/renderer/AndroidInkRendererAdapter.kt#L31); [NotebookPracticeViewModel.kt:60](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L60).

**Confirmado por código.** O adapter chama `fallbackRenderer` incondicionalmente, inclusive dentro do `try`; não chama nenhuma API `androidx.ink`. Caderno e treino usam diretamente `SmoothedReferenceRenderer`. A dependência está no Gradle, mas isso não constitui implementação nem benchmark da Ink API. `isHardwareAcceleratedOnDevice` começa em `true` sem consultar hardware; o catch repete a mesma operação que falhou.
**Resolver:** implementar um protótipo real da Ink API ou identificar honestamente o adapter como placeholder e a escolha como pendente. Registrar versão, suporte e comparação medida no S25 Ultra. **Aceite:** execução real identificável, benchmark contra referência e preservação dos dados; nunca apresentar fallback como Ink API.

### A02 — Gravação não atômica destrói a versão anterior em caso de falha

**Prioridade:** P1. **Evidência:** [DedicatedFileStrategy.kt:42](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/persistence/strategies/DedicatedFileStrategy.kt#L42); [SessionLifecycleManager.kt:22](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/lifecycle/SessionLifecycleManager.kt#L22).

**Reproduzido.** `FileOutputStream(file)` trunca o arquivo de destino antes de terminar a nova escrita. O probe salva uma página válida e força uma falha de serialização com ID grande; depois a página anterior não pode mais ser lida. O ID artificial é injeção de falha, não uma entrada normal da UI; demonstra a ausência de atomicidade também relevante a interrupção/I/O. Manifestos e metadados também são sobrescritos diretamente.
**Resolver:** arquivo temporário no mesmo diretório, flush/sync apropriado, substituição atômica e recuperação da última versão válida; transação coerente para metadados. **Aceite:** falha em cada fase mantém íntegra a versão anterior e informa o erro, sem perda silenciosa.

### A03 — Autosaves e navegação concorrem sobre os mesmos arquivos e estado

**Prioridade:** P1. **Evidência:** [NotebookPracticeViewModel.kt:115](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L115); [NotebookPracticeViewModel.kt:140](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L140); [LocalNotebookRepository.kt:147](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/repository/LocalNotebookRepository.kt#L147).

**Risco confirmado pela estrutura; interleaving ainda não reproduzido no aparelho.** Cada alteração dispara uma coroutine IO independente. Não há Mutex/fila/revisão para impedir uma gravação antiga de terminar depois da nova, ou dois streams escreverem no mesmo arquivo. `loadStrokes` modifica lista mutável na thread IO enquanto a View lê/escreve na principal. A troca de página mantém captura habilitada durante I/O: um novo traço pode ser descartado pelo load seguinte ou associado à página errada. Dois cliques de adicionar podem ler o mesmo manifesto e perder uma das referências.
**Resolver:** único proprietário do estado em memória, snapshots imutáveis, fila de escrita ordenada por documento, controle de versão e transição de página que finalize a captura e impeça interação até concluir. Propagar erros e usar `try/finally` no indicador de salvamento. **Aceite:** teste com barreiras/delays que force saves fora de ordem, escrita durante navegação, múltiplos cliques e falha de disco; último estado persistido correto e nenhuma exceção concorrente.

### A04 — Borracha vira um novo traço de tinta no caderno e treino

**Prioridade:** P1. **Evidência:** [NotebookCanvasView.kt:70](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookCanvasView.kt#L70); [GuidedPracticeCanvasView.kt:88](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeCanvasView.kt#L88); [StrokeCapturePipeline.kt:399](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/capture/StrokeCapturePipeline.kt#L399).

**Confirmado por fluxo de código.** A captura também conclui strokes `ERASER`, e ambas as Views adicionam todo stroke ao repositório. Seus renderizadores não filtram a ferramenta. Assim, a borracha apaga mas depois deixa seu próprio caminho desenhado, salvo e contado na avaliação. O laboratório já filtra `ERASER`, mas M1/M2 não. A prévia ativa também desenha o caminho da borracha.
**Resolver:** tratar borracha como comando, não tinta; filtrar conclusão e prévia, mantendo apenas cursor visual. Agrupar undo por gesto de borracha. **Aceite:** apagar e levantar a caneta nunca adiciona stroke; salvar/reabrir e avaliar não reintroduzem a borracha; um undo restaura a passada inteira.

### A05 — Borracha perde o segmento entre dois MotionEvents

**Prioridade:** P1. **Evidência:** [StrokeCapturePipeline.kt:172](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/capture/StrokeCapturePipeline.kt#L172).

**Reproduzido.** Cada callback recebe só o lote atual, sem o ponto final do lote anterior. Uma borracha de (0,0) para (100,0), em eventos separados, não apaga um stroke vertical em x=50 com raio 5. O helper sabe detectar a interseção, mas não recebe o segmento contínuo.
**Resolver:** incluir o último ponto anterior no próximo lote da borracha, sem duplicar dados brutos. **Aceite:** o probe deve passar a remover o stroke; cobrir DOWN→MOVE e MOVE→UP, com e sem históricos.

### A06 — Proteção de ciclo de vida não cobre as telas principais

**Prioridade:** P1. **Evidência:** [MainActivity.kt:104](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/MainActivity.kt#L104); [NotebookCanvasView.kt:32](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookCanvasView.kt#L32); [GuidedPracticeViewModel.kt:38](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeViewModel.kt#L38).

**Confirmado por integração ausente.** `onPause`/`onResume` da Activity operam só o ViewModel do laboratório. Os pipelines de caderno e treino não recebem flush por pausa, perda de foco ou silo. Não há persistência/restauração da tentativa guiada; ViewModel sozinho não sobrevive à morte do processo. Callbacks do pipeline ainda capturam as Views sem liberação explícita. `ACTION_CANCEL` chama callback de cancelamento que essas Views não registram, descartando o traço ativo.
**Resolver:** coordenar ciclo de vida do pipeline da tela ativa, finalizar/guardar escrita válida, desassociar callbacks ao descartar a View e definir política explícita para CANCEL de sistema versus cancelamento por palma. Salvar tentativa/estágio conforme política documentada. **Aceite:** testar escrita seguida de bloqueio, rotação, navegação, silo e morte de processo em M0/M1/M2. Não garantir “nenhum traço perdido” só com testes da classe auxiliar.

### A07 — Ausência de sensor é inferida pelo valor e apaga zeros válidos

**Prioridade:** P1. **Evidência:** [StrokeCapturePipeline.kt:403](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/capture/StrokeCapturePipeline.kt#L403).

**Reproduzido.** Pressão zero, tilt zero e orientação zero viram `null`. Tilt zero é uma posição válida; não significa sensor ausente. A função também aceita pressão positiva sem verificar que o eixo é fornecido pelo dispositivo/source. Os testes com `onPointerDown` e pontos já construídos não verificam essa conversão real.
**Resolver:** consultar/cachear capacidades por dispositivo/source e preservar todo valor válido, inclusive zero. **Aceite:** testes da entrada MotionEvent com eixo disponível em zero e eixo ausente; aplicar mesma regra ao histórico. Fonte: [MotionEvent](https://developer.android.com/reference/android/view/MotionEvent.html) e `InputDevice.getMotionRange`.

### A08 — ACTION_POINTER_UP perde a última amostra da caneta

**Prioridade:** P2. **Evidência:** [StrokeCapturePipeline.kt:364](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/capture/StrokeCapturePipeline.kt#L364).

**Confirmado por código.** Quando a caneta sobe e outro ponteiro permanece, usa `emptyList()`; o caminho ACTION_UP coleta o ponto final e históricos. Isso faz a fidelidade depender da presença da palma.
**Resolver:** extrator comum de amostras do ponteiro correto para UP/POINTER_UP. **Aceite:** MotionEvent multi-touch com ponto final distinto deve preservá-lo e manter timestamps.

### A09 — Exportação pode retornar falso sucesso com PNG inválido

**Prioridade:** P1. **Evidência:** [PageExporter.kt:50](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/export/PageExporter.kt#L50); [PageExporter.kt:90](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/export/PageExporter.kt#L90); [PageExporterTest.kt:63](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/test/java/com/scribe/caligrafia/notebook/export/PageExporterTest.kt#L63).

**Reproduzido na JVM configurada pelo projeto.** Quando `Bitmap.createBitmap` retorna null ou lança qualquer Throwable, a produção escreve só os oito bytes da assinatura PNG. O arquivo não tem IHDR/IDAT e não é uma imagem. O probe confirmou 8 bytes e falha de decodificação. Em falha real de alocação, o mesmo ramo mascara o erro; se já existir um arquivo, pode devolver o antigo. O retorno booleano de `compress` também é ignorado.
**Resolver:** retirar fallback de teste do código de produção, propagar falha e liberar bitmap em finally; usar teste gráfico apropriado. **Aceite:** decodificar PNG, conferir dimensões e pixels conhecidos; simular falha e garantir mensagem de erro sem artefato apresentado como sucesso.

### A10 — Export e redimensionamento não têm sistema de coordenadas da página

**Prioridade:** P2. **Evidência:** [PageExporter.kt:57](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/export/PageExporter.kt#L57); [NotebookCanvasView.kt:154](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookCanvasView.kt#L154); [NotebookModels.kt:29](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/core/model/NotebookModels.kt#L29).

**Limitação confirmada por código.** Captura usa pixels locais da View, mas export desenha os mesmos pixels em 1440×2560 sem transformação e sem dimensão original no modelo. Uma escrita até a direita de um canvas 1080px não ocupa a mesma proporção no PNG de 1440px. Rotação/mudança de tamanho também não reprojeta página e traços conjuntamente.
**Resolver:** espaço lógico de documento e transformação reversível de input/render/export, preservando dados de origem. **Aceite:** mesmos alinhamentos relativos em múltiplos tamanhos e PNG; não basta testar existência do arquivo. Oferecer salvar/compartilhar via SAF ou MediaStore/FileProvider: hoje a UI informa só nome em diretório específico do app.

### A11 — Replay perde estilo durante o traço e usa relógio de parede

**Prioridade:** P2. **Evidência:** [StrokeReplayEngine.kt:169](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/replay/StrokeReplayEngine.kt#L169); [StrokeReplayEngine.kt:85](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/replay/StrokeReplayEngine.kt#L85).

**Perda de estilo reproduzida:** frame parcial retorna `color=null` e `baseWidthPx=null` embora o original tenha cor e largura. Ao concluir, reaparece o estilo original. O loop usa `System.currentTimeMillis`, sujeito a ajuste do relógio, e trunca o delta fracionário em cada tick; o comentário de 60–120fps não corresponde a um relógio de frames, pois há delay fixo de 16ms.
**Resolver:** usar `stroke.copy(points=...)` e preservar metadados; relógio monotônico ou frame clock com acumulador preciso. **Aceite:** mesmas propriedades em frame parcial/final e duração consistente em 0.5x/1x/2x com relógio injetável.

### A12 — Exercício incompleto recebe nota de aprovação

**Prioridade:** P1. **Evidência:** [GeometricFeedbackEvaluator.kt:279](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt#L279); [GeometricFeedbackEvaluator.kt:115](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt#L115).

**Reproduzido:** em BASIC_SLANT, só os primeiros 2 dos 16 pontos recebem 87%, exatamente como o traço completo, com `isPassed=true`. Proximidade mede apenas aluno→modelo; não exige cobertura modelo→aluno. Limites só penalizam excesso, não alcance insuficiente. Quantidade de strokes coincide mesmo que o único stroke seja minúsculo.
**Resolver:** medir cobertura/completude e extensão por traço com tolerâncias, sem premiar fragmentos. **Aceite:** referência completa, fragmento, ponto isolado e repetição do mesmo trecho devem produzir diagnósticos diferentes; não anunciar domínio/aprovação a partir de fragmento.

### A13 — Modelo inclinado contradiz a pauta e a avaliação depende da amostragem

**Prioridade:** P1. **Evidência:** [ReferenceGlyphCatalog.kt:57](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/catalog/ReferenceGlyphCatalog.kt#L57); [GeometricFeedbackEvaluator.kt:175](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/evaluator/GeometricFeedbackEvaluator.kt#L175).

**Reproduzido:** o BASIC_SLANT projetado tem ~76,50°, mas a pauta pede 52°. A reprodução exata recebe apenas 87% e desvio de inclinação. Além disso, segmentos só entram na medida se dy>4px e comprimento>5px: a mesma linha mais densamente amostrada pode ser ignorada, retornando 85 como inclinação neutra. Curvas fechadas são avaliadas por proximidade do começo/fim, insuficiente para distinguir sentido quando ambos coincidem.
**Resolver:** calibrar a geometria do catálogo contra a pauta; medir segmentos com reamostragem por comprimento/distância acumulada e usar progressão ao longo da referência para direção. **Aceite:** referência projetada coerente; invariância à densidade/velocidade; reversão de oval e troca de ordem detectadas. Validação pedagógica humana ainda pendente, não chamar catálogo de calibrado sem evidência.

### A14 — Feedback fica desatualizado após nova escrita

**Prioridade:** P2. **Evidência:** [GuidedPracticeViewModel.kt:95](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeViewModel.kt#L95); [GuidedPracticeScreen.kt:265](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/guided/ui/GuidedPracticeScreen.kt#L265).

**Confirmado por código.** Notificação de alteração atualiza só contagem. Depois de avaliar, ainda é possível escrever/apagar, mas o card mantém avaliação antiga e o botão de verificar permanece escondido.
**Resolver:** invalidar avaliação em toda alteração ou bloquear edição da tentativa avaliada com ação explícita de nova tentativa. **Aceite:** escrever/apagar após avaliar nunca conserva nota atribuída a outro conjunto de strokes.

### A15 — Pauta atualizada fica obsoleta na lista de páginas

**Prioridade:** P2. **Evidência:** [NotebookPracticeViewModel.kt:224](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L224); [NotebookPracticeViewModel.kt:151](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/viewmodel/NotebookPracticeViewModel.kt#L151).

**Confirmado por código.** Alteração atualiza arquivo e `currentPage`, mas não `pagesList`. Ao ir a outra página e voltar, a navegação usa o objeto antigo em cache e restaura visualmente a pauta anterior; só um reload da lista corrige.
**Resolver:** manter lista e página atual consistentes ou recarregar metadados ao navegar. **Aceite:** alterar pauta, ir/voltar, exportar e reiniciar mantém a mesma configuração.

### A16 — Benchmark não compara SQLite/Room de verdade

**Prioridade:** P1. **Evidência:** [RelationalTableStrategy.kt:35](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/persistence/strategies/RelationalTableStrategy.kt#L35); [CompressedBlobStrategy.kt:39](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/persistence/strategies/CompressedBlobStrategy.kt#L39); [PERSISTENCE_SPIKE_REPORT.md:40](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/docs/PERSISTENCE_SPIKE_REPORT.md#L40).

**Confirmado.** “Tabela relacional” escreve CSV; “Room/BLOB” escreve arquivo binário. Nenhuma dessas estratégias abre banco, usa transação SQL ou Room. Portanto medições podem comparar esses formatos, mas não demonstram custo de SQLite, foreign keys ou ganho frente ao Room. A projeção de 50.000 pontos não é medição no S25 Ultra. O relatório atribui “Revisor: Codex” antes desta auditoria independente.
**Resolver:** renomear resultados para os formatos reais e retirar conclusões indevidas; se a decisão requer comparação com banco, executar implementações reais e cargas maiores/repetições no aparelho. **Aceite:** resultados brutos, ambiente, versão, unidade, mediana/percentis e round-trip completo (inclusive cor/espessura). Só atribuir revisão após revisão efetiva.

### A17 — Testes passam, mas não sustentam a cobertura anunciada

**Prioridade:** P1. **Evidência:** [PageExporterTest.kt:62](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/test/java/com/scribe/caligrafia/notebook/export/PageExporterTest.kt#L62); [watchdog.ps1:49](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/scripts/watchdog.ps1#L49).

**Confirmado:** executei 17 suítes e 91 testes, zero falhas. Porém `app/build.gradle.kts` ativa `unitTests.isReturnDefaultValues=true`, sem Robolectric configurado; chamadas do android.jar recebem defaults. Não há testes em `app/src/androidTest`. Renderizadores são exercitados sem validação real de pixels, export passa com PNG inválido e os retângulos calculados não provam exclusão de gestos no sistema. “100% sem stubs parciais” e “auditoria aprovada” excedem o que foi testado.
**Resolver:** manter testes puros, usar Robolectric/instrumentação para Framework e testes no S25 Ultra para input/latência/gestos. Watchdog deve reportar checks específicos e executar lint; sua busca de cloud só olha alguns padrões em build.gradle, não resolve dependências do catálogo. **Aceite:** falhas de integração introduzidas deliberadamente devem ser detectadas; publicar resultados por ambiente, sem confundir taxa de aprovação com cobertura.

### A18 — Lint reprovado e CI de release falhando

**Prioridade:** P1. **Evidência:** [DeviceCapabilityInspector.kt:81](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/inspector/DeviceCapabilityInspector.kt#L81); [build-release.yml:36](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/.github/workflows/build-release.yml#L36).

**Executado:** `lintDebug` termina com 1 erro/35 warnings. Erro NewApi: `InputDevice.isExternal` requer API 29 e minSdk=26; catch de Throwable não substitui guard explícito para lint. Os avisos incluem dependências, alocação em desenho, locale e ícone ausente; atualizar dependências exige validação, não atualização cega.
**GitHub confirmado pela API pública:** os cinco últimos runs consultados falharam. No run de b31538c, “Run Unit Tests” falhou e build/upload foram pulados. `git ls-files -s gradlew` retorna 100644; Linux recebe wrapper sem bit executável, mas YAML chama `./gradlew` e não aplica chmod. Isso é um defeito reproduzível da configuração; não baixei log privado para afirmar ser a única causa do run.
**Resolver:** guard de API ou decisão documentada sobre minSdk; `git update-index --chmod=+x gradlew` ou execução por bash; executar tests/lint/build no CI e verificar assinatura antes do upload. **Aceite:** pipeline verde a partir de checkout limpo. [Run consultado](https://github.com/playertwo1/caligrafia/actions/runs/34701886129).

### A19 — Senha de assinatura embutida e release pode sair sem assinatura

**Prioridade:** P1. **Evidência:** [build-release.yml:10](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/.github/workflows/build-release.yml#L10).

**Confirmado em `app/build.gradle.kts:25–45`.** Há senhas padrão literais para keystore/key password no código versionado (não reproduzidas aqui). A existência de senha no código não prova que a chave privada vazou; se esse fallback foi usado para a chave real, o segredo precisa ser tratado como exposto. Sem arquivo keystore, `assembleRelease` ainda pode gerar release não assinado, enquanto o job/artifact se apresenta como assinado.
**Resolver:** remover defaults, exigir configuração segura para publicação, verificar APK com apksigner; revisar/alterar senhas caso usadas e preservar continuidade da chave de atualização, sem trocá-la arbitrariamente. **Aceite:** ausência de segredo bloqueia publicação assinada com erro claro; nenhuma credencial em código/log; assinatura e certificado verificados.

### A20 — Exclusão de borda inteira não é garantida pelo Android

**Prioridade:** P2. **Evidência:** [EdgeGestureExclusionHelper.kt:46](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/gesture/EdgeGestureExclusionHelper.kt#L46); [MainActivity.kt:44](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/MainActivity.kt#L44).

**Limitação da plataforma, sem teste físico nesta auditoria.** O helper pede retângulos de toda a altura. A documentação Android impõe limite vertical de 200dp nas condições normais, com exceções específicas; definir comportamento de barras transitórias não equivale a escondê-las persistentemente. Logo não se pode garantir bloqueio integral nem equivalência ao Samsung Notes só pelo cálculo dos rects.
**Resolver:** exclusão estratégica/dinâmica onde a escrita ocorre e validação em navegação por gestos real; documentar limites. **Aceite:** matriz de regiões nas laterais, toque/caneta, One UI e navegação inferior. Fonte: [WindowInsets](https://developer.android.com/reference/android/view/WindowInsets.html).

### A21 — Trabalho por frame e salvamento síncrono no laboratório

**Prioridade:** P2. **Evidência:** [SmoothedReferenceRenderer.kt:101](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/ink/renderer/SmoothedReferenceRenderer.kt#L101); [NotebookCanvasView.kt:162](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/notebook/ui/NotebookCanvasView.kt#L162); [StylusLabViewModel.kt:198](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/app/src/main/java/com/scribe/caligrafia/inspector/viewmodel/StylusLabViewModel.kt#L198).

**Custo confirmado por código; impacto em ms ainda não medido.** Cada redraw percorre todos os pontos dos strokes concluídos, recria paths e listas de pressão; a prévia copia todos os pontos ativos. O laboratório comprime e grava a sessão inteira sincronamente em `triggerAutoSave`, chamado no callback de entrada; o comentário diz background, mas não há dispatch. Caderno faz save integral por alteração, incluindo lotes de borracha.
**Resolver:** cache de geometria concluída, atualização incremental e invalidação por frame, fila IO ordenada com coalescência e flush seguro. O vetor continua fonte primária. A espessura atual usa média de pressão do stroke inteiro, não variação ao longo dele; avaliar pincel por segmento se necessário à caligrafia. **Aceite:** medir frame time/GC/latência e I/O em sessões de 10k/50k pontos no aparelho, preservando pontos e feedback visual.

### A22 — Estados, gates e escopo anunciado estão inconsistentes

**Prioridade:** P1. **Evidência:** [AUDIT_REPORT.md:5](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/AUDIT_REPORT.md#L5); [PROJECT_STATE.md:6](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/PROJECT_STATE.md#L6); [README.md:3](https://github.com/playertwo1/caligrafia/blob/b31538c443b21318762e14a34cc065a8027dfc53/README.md#L3).

**Confirmado.** README diz implementação não iniciada; AGENTS mantém M0; PROJECT_STATE anuncia M3 e M2 100% aprovado, enquanto admite teste físico pendente no Gate M0. BACKLOG conserva SCR-009 como benchmark M0 e SCR-010 como gate; o dossiê reutiliza esses IDs para M1. O relatório de auditoria do autor não substitui validação independente. Não há evidência versionada suficiente para fechar latência/estabilidade no S25 Ultra, nem integração real de Ink API.
Além disso, a UI de feedback exibe nota percentual, “Apto para avançar”/“Requer mais treino”, enquanto PRODUCT_SPEC exclui score absoluto de qualidade no MVP. Esclarecer com o usuário se se trata de métrica geométrica limitada; preferir orientação por dimensão e não representar a porcentagem como qualidade global da caligrafia.
**Resolver:** reconciliar IDs e status, manter histórico do que foi implementado e do que foi efetivamente validado, reabrir gates pendentes e remover “100%” sem suporte. **Aceite:** matriz única requisito→código→teste→evidência física→decisão de gate; tratar A01–A19 antes de declarar marcos encerrados.

## Evidências diagnósticas reproduzidas

| Probe | Resultado observado em b31538c | Resultado exigido após correção |
|---|---|---|
| Replay parcial | cor e espessura `null` | preservar estilo original |
| Sensor com zero | tilt/orientação viram `null` | preservar zero se eixo suportado |
| Borracha em eventos separados | stroke cruzado permanece | apagar stroke cruzado |
| Falha na regravação | arquivo anterior ilegível | versão anterior recuperável |
| Export com gráficos stubados | 8 bytes, não decodifica | erro explícito ou PNG real em runtime gráfico |
| Avaliação parcial | 2/16 pontos = 87%, completo = 87%; referência ~76,5° para alvo 52° | exigir completude e referência coerente |

Fontes e logs: [AuditProbeTest.kt](audit/AuditProbeTest.kt), [resultado XML](audit/probe-results.xml), [baseline build](audit/baseline-build.log), [lint](audit/lint-results-debug.txt), [execução dos probes](audit/probes-build.log), [init script](audit/probe-output.gradle).

Para reproduzir, copiar `AuditProbeTest.kt` temporariamente para `app/src/test/java/com/scribe/caligrafia/audit/` e executar:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
$env:ANDROID_HOME = 'C:/Users/fael/AppData/Local/Android/Sdk'
./gradlew.bat -I docs/audit/probe-output.gradle testDebugUnitTest --tests 'com.scribe.caligrafia.audit.AuditProbeTest' --console=plain
```

Os probes fazem asserts do comportamento defeituoso para demonstrá-lo; após corrigir, substituí-los por testes com expectativas corretas. Remover a cópia temporária ao terminar. O probe do sensor usa reflexão da conversão privada e não substitui teste MotionEvent/dispositivo. O probe PNG demonstra justamente a insuficiência do runtime stubado.

## Recomendações adicionais

- Definir política de corrupção/recuperação: `loadPageStrokes` pode lançar sem tratamento no ViewModel; versões negativas, contagens excessivas e payload truncado precisam rejeição controlada e backup. Arquivos são locais hoje; não classifico isso como vulnerabilidade remota.
- `Stroke.points: List` é somente leitura na interface, não profundamente imutável; copiar listas na fronteira do repositório evita mutação pelo chamador. Captura já faz `toList`, um ponto positivo.
- Manter metadados de sessão independentes de uptime entre reinicializações; planejar replay de página editada em múltiplos boots e preservar ordem de captura sem depender de ordenar timestamps de boots diferentes.
- Caderno não expõe replay da página: o motor é usado no laboratório com outro repositório. Planejar ligação da página salva ao replay para cumprir a experiência do produto, sem afirmar que o motor não existe.
- Avaliar resampling/índice espacial no avaliador e borracha somente com medição. Tirar avaliação extensa da thread principal caso sessões maiores causem jank; não inventar ganhos percentuais.
- Remover termos de implementação da UI de produto (`M1`, `.scribe`, etc.), distinguir laboratório de telas de usuário e verificar layout em resolução/densidade reais do S25 Ultra e fontes ampliadas. Sem screenshots reais, não afirmo overflow visual confirmado.
- Links `file:///c:/Users/.../scribe` do dossiê não funcionam para outros revisores; substituir por links relativos. Deprecações de ícones e recursos não usados podem ser resolvidos junto à limpeza de lint.

## O que está efetivamente implementado

Kotlin/Compose com superfícies View nativas; modelos de strokes e ferramentas; captura de históricos em MOVE/UP; regras puras de palma; cálculo de pautas; repositório de caderno/página; serialização binária com schema v2; undo/redo básico; motor temporal; catálogo com 12 exercícios e fluxo de três estágios; feedback geométrico sem cloud/IA. Há 91 testes existentes que realmente passaram nesta máquina, APK debug compilável e anexos da release publicados. Esses méritos não eliminam os defeitos de integração descritos acima.

## Ordem de resolução sugerida

1. **Dados e escrita:** A02–A08 e A03 com testes de concorrência/recuperação; revisar leituras de arquivo corrompido.
2. **Validação confiável:** A09, A16–A19; pipeline verde com lint e testes gráficos reais.
3. **Motor e fidelidade:** A01, A10–A11, A20–A21; benchmark real no S25 Ultra.
4. **Pedagogia e estado:** A12–A15; revisar catálogo e métricas com casos completos, incompletos e reamostrados.
5. **Documentação/gates:** A22; atualizar matriz e evidências após correções, sem substituir teste físico por percentual de testes unitários.

Para devolver ao Codex: informar commit de cada correção, IDs resolvidos, comandos/resultados, reproduções antes/depois, riscos restantes e logs do dispositivo (modelo, Android/One UI, resolução, taxa da tela e condições). Se algo não for corrigido, marcar explicitamente como pendente com justificativa.
