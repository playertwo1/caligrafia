# Terceira auditoria independente — Scribe v0.6.0 → v0.8.0

**Parecer: NÃO APROVADO para fechamento do roadmap M0–M8.**

Data: 2026-09-12. Auditor: Codex. O dossiê `AUDIT_REPORT_M7_M8.md` contém alegações do implementador; não equivale à aprovação independente.

## 1. Base, escopo e limites

- Código auditado: `bb09dc4a8331cab4ba8922e82479293806c294b1`, origin/main observado nesta revisão.
- Tag v0.8.0, resolvida para commit: `0d53029824bb21654ffbac0e28323996ac447732`. O commit posterior acrescenta o dossiê.
- Tag v0.6.0: `cf914fcfd397913e6787211838a35cc7a7c01f67`. A V2 examinou `728ea0d`, com o dossiê M4–M6 e o mesmo núcleo v0.6.0.
- Delta desde 728ea0d: 48 arquivos, 6.032 adições e 95 remoções. [Inventário](audit-v3/delta-numstat.txt). Contagens dependem do intervalo; não misturar tags e commits documentais.
- Trabalho no worktree isolado `../.audit-worktree`; alterações anteriores do checkout principal preservadas. **Caminhos de código neste relatório referem-se ao SHA auditado, não ao código antigo do checkout principal.** Prefixo dos packages: `app/src/main/java/com/scribe/caligrafia/`.
- Inspecionados dossiês M7–M8/M4–M6/M0–M3, documentos de produto/arquitetura/estado, relatórios independentes anteriores, novas telas, motores, serializers, repositórios, navegação, testes, build e workflow.
- Executados Gradle/JVM, lint, testes diagnósticos em arquivos temporários e verificação do APK público. `adb devices` não apresentou aparelho. Não houve teste físico S25 Ultra/Watch, teste instrumentado de UI, benchmark de latência ou inspeção do Google Drive.
- Não se promete ausência de todo bug. Evidências estáticas, provas JVM e validações ainda necessárias estão diferenciadas abaixo. Código de produto não foi corrigido nesta auditoria.

## 2. Resultado executivo

Há implementação real em Kotlin/Compose, modelos vetoriais, motores determinísticos e novas telas. Os 196 testes existem e passam. Isso não comprova entrega de ponta a ponta.

Bloqueadores incluem ZIP incompleto, backup omitindo dados pessoais, restore parcialmente destrutivo, manifesto inválido aceito, referência de assinatura mutável, diagnóstico sobre dados artificiais, prescrições com IDs inexistentes, pressão/Watch sem integração real e release público sem assinatura verificável. Dez provas históricas continuam reproduzindo os mesmos defeitos.

**M7/M8: PARCIAIS. Gates anteriores: não revalidados. Não há aprovação do Codex para declarar 100%, encerrar o roadmap ou anunciar release assinado pronto para instalação.**

## 3. Execuções reproduzidas

| Verificação | Resultado observado | Evidência/limite |
|---|---|---|
| `gradlew.bat :app:testDebugUnitTest :app:lintDebug --console=plain` | BUILD SUCCESSFUL | [Log](audit-v3/build.log) |
| Suíte original, antes dos probes | **196 testes / 45 suítes / 0 falhas / 0 erros** | [Resumo](audit-v3/baseline-summary.json), [XMLs](audit-v3/baseline/) |
| Lint | **0 erros / 48 warnings** | [XML](audit-v3/lint-results-debug.xml); V2 tinha37 warnings |
| `scripts/watchdog.ps1`, sem SkipBuild | **REPROVADO nesta máquina**, duas execuções | [Primeira](audit-v3/watchdog.log), [após parar daemon](audit-v3/watchdog-retry.log) |
| Build debug em diretório novo | **BUILD SUCCESSFUL**, 36 tarefas executadas | [Log](audit-v3/fresh-build.log), [init script](audit-v3/fresh-build.init.gradle) |
| Probes V2 | **10/10 reproduziram defeitos** | [XML](audit-v3/TEST-com.scribe.caligrafia.audit.AuditV2ProbeTest.xml), [fonte](audit-v2/AuditV2ProbeTest.kt) |
| Probes V3 | **10/10 reproduziram condições diagnósticas** | [XML](audit-v3/TEST-com.scribe.caligrafia.audit.AuditV3ProbeTest.xml), [fonte](audit-v3/AuditV3ProbeTest.kt), [log](audit-v3/probes.log) |
| Assinatura do release público | **DOES NOT VERIFY — Missing META-INF/MANIFEST.MF** | [Log](audit-v3/apk-signature.log), [hash](audit-v3/apk-hash.txt) |
| S25 Ultra / Watch / Drive | **NÃO VERIFICADOS nesta revisão** | Sem aparelho via adb; Drive não inspecionado |

O Watchdog passou nas buscas estáticas/testes, mas packageDebug falhou ao remover `build/intermediates/incremental/packageDebug/tmp/.../zip-cache`, bloqueado no Windows. O APK antigo ainda existente recebeu OK no passo de integridade. Parar daemon e repetir não resolveu. Para separar ambiente de compilação, `:app:assembleDebug -I ../docs/audit-v3/fresh-build.init.gradle` usou `build/audit-fresh` e passou. Isso não vira “Watchdog4/4”. A limpeza do cache foi rejeitada pela revisão automática de segurança; a alternativa não apagou o diretório.

**Probes verdes significam reprodução, não aceitação.** Foram retirados da árvore de testes e preservados como evidência. O probe de aliasing demonstra o modelo compartilhado; o de Zip-Slip demonstra o predicado incorreto, sem alegar exploração completa do importador. Nenhum dado pessoal foi usado.

## 4. Conformidade

| Invariante | Parecer | Justificativa |
|---|---|---|
| Kotlin nativo / sem WebView | Conforme no escopo inspecionado | Compose/Views nativas |
| Sem backend/cloud de aplicação | Conforme no código inspecionado | Sem cliente cloud/LLM remoto; sem INTERNET no manifest. `allowBackup=true` exige esclarecer se “zero cloud” também pretende excluir backup gerenciado pelo sistema; não prova upload |
| Raw strokes preservados | **Não integralmente conforme** | S12–S14: lista mutável, ponto UP perdido, ausência de persistência da referência |
| Dados ausentes não inventados | **Não conforme** | S08/S09 e R15 |
| Adapters Samsung/Wear | Estrutura parcial | Interface Watch existe; transporte/integração não comprovados |
| Atomicidade e fd.sync | **Não conforme globalmente** | Teacher sincroniza com descriptor aberto; repositórios antigos não. Restore não é transação |
| Reflexão ViewModels | Evidência estática/unitária favorável | Novos ViewModels têm JvmOverloads e verificação de construtores; não prova init/IO/runtime sem falhas |
| Gates e contenção | **Não conforme documentalmente** | Pendências anteriores persistem apesar de conclusão declarada |
| APK assinado/otimizado | **Assinatura pública reprovada** | S21; minify desativado |

## 5. Achados S01–S24

**P1:** bloqueador de integridade, entrega central ou distribuição. **P2:** funcionalidade parcial, medição, robustez ou qualidade a corrigir antes de fechar o marco. Todos estão **ABERTOS**. As correções propostas não foram implementadas.

### S01 — P1 — ZIP exportado sem diretório central no destino

**Local:** `expansions/backup/ScribeBackupManager.kt:119–120`; `ExpansionsViewModel.kt:224`.

`ZipOutputStream(outputStream.buffered())` recebe flush ANTES de finish. O diretório central gerado por finish fica no buffer intermediário; não há flush posterior nem close desse wrapper. Caller sincroniza/fecha somente FileOutputStream externo.

**Prova:** `exportedZipHasNoCentralDirectory` abre arquivo exportado com ZipFile e obtém `zip END header not found`. Teste original usa ZipInputStream, que lê entradas locais sem exigir diretório central.

**Correção/aceite:** definir propriedade dos streams; finalizar ZIP e descarregar todos os buffers antes de sync com descriptor aberto. Publicar arquivo temporário só após conclusão. Abrir saída real com ZipFile e ferramenta ZIP independente; conferir entradas/CRC; falha de escrita não pode deixar arquivo final anunciado como válido.

### S02 — P1 — Backup omite caminhos reais e conta entidades incorretamente

**Local:** `ScribeBackupManager.kt:24–74`; repositórios e construtores dos ViewModels.

| Dado | Caminho real em filesDir | Procurado no backup |
|---|---|---|
| Alfabeto | personal_alphabet/ | alphabet/ |
| Tentativas | attempts/ | practice_attempts/ |
| Histórico | learning_history.json | learning/learning_history.json |
| Fontes | custom_fonts/ | fonts/ |
| Diagnóstico | teacher/diagnostic.json | flag procura diagnostic_latest.json |
| Páginas | notebooks/ID/pages/*.scribe | contagem só vê filhos imediatos de ID |

Diagnóstico/páginas são incluídos pela recursão das pastas corretas, mas flag/contagens podem ser falsos/zero. Testes originais criam pastas fictícias iguais à lista errada.

**Prova:** fixture com caminhos reais exportou somente manifest.json e teacher/diagnostic.json, com teacherFlag=false.

**Aceite:** inventário de dados real; round-trip criando pelos repositórios efetivos e reabrindo em instalação vazia. Comparar IDs, pontos, sensores, favoritos, estilos e histórico. Definir política para signatures e futura referência persistida; não adicionar mais fixtures em pastas inventadas.

### S03 — P1 — Restore parcialmente destrutivo, sem atomicidade de conjunto

**Local:** `ScribeBackupManager.kt:181–223`.

Após staging, faz Files.copy(REPLACE_EXISTING) arquivo por arquivo sobre dados ativos. Sem commit único, rollback, coordenação com escritores/caches nem sync dos arquivos finais. Arquivos extras antigos permanecem: merge implícito, não restauração exata.

**Prova:** página nova seguida de obstáculo em alphabet retorna sucesso=false, mas página antiga já foi substituída por changed.

**Aceite:** definir restore versus merge; validar antes de tocar no ativo; publicar geração coerente com recuperação/rollback e coordenação de escritores. Injetar falha em cada etapa e reiniciar: sempre geração anterior íntegra ou nova íntegra. Testar falta de espaço, concorrência, extras e referências ausentes. Não apagar todas as pastas como atalho.

### S04 — P1 — Manifesto inválido permite sobrescrever dados

**Local:** `BackupSerializer.kt:30`; importBackup.

Regex retorna BackupManifest com defaults até para NOT JSON. Import só exige objeto não nulo; não valida versão, esquema, contagens, referências ou payload scribe.

**Prova:** ZIP com manifest.json=NOT JSON e página de texto inválido retorna sucesso e sobrescreve página existente.

**Aceite:** rejeitar JSON inválido, obrigatórios ausentes, versão futura, números fora de intervalo e payload corrompido ANTES de commit. Testar ZIP válido com manifesto inválido, não somente bytes que sequer são ZIP.

### S05 — P1 — Proteção Zip-Slip usa prefixo textual insuficiente

**Local:** `ScribeBackupManager.kt:147–150`.

path.startsWith(canonicalPath) aceita irmão temp_restore_123_other como se estivesse dentro de temp_restore_123. Probe demonstra predicado aceitando irmão e Path.startsWith por componentes rejeitando. Não foi executado ataque completo adivinhando timestamp interno.

**Aceite:** contenção canônica por componentes, allowlist de caminhos, staging exclusivo, tratamento de symlinks/colisões; limites de entradas, tamanho descompactado e manifesto (copyTo atual não tem orçamento). Testar traversal, prefixo, absolutos, separadores, duplicatas, conflitos arquivo/pasta, ZIP oversized e imports concorrentes, sem escrita fora do staging.

### S06 — P1 — Restauração não tem percurso de UI; backup fica privado

**Local:** `ExpansionsScreen.kt:667–770`; `ExpansionsViewModel.kt:249`.

Tela só cria arquivo em filesDir/backups. Sem botão/seletor de restauração, exportação/compartilhamento para destino escolhido ou caller de restoreBackup em produção. Cópia privada dentro dos dados do próprio app não entrega recuperação acessível por si só.

**Aceite:** usuário escolhe destino local, exporta, escolhe arquivo e restaura; reabrir os dados pelo produto. Concluir S01–S05 antes de expor restore. Usar fluxo de documentos/URIs local, sem backend/cloud.

### S07 — P1 — Professor analisa seeds, não novas tentativas reais

**Local:** `TeacherViewModel.kt:44,82`; `LocalPracticeAttemptRepository.kt:220–250`; R02–R04.

Repositório nasce com quatro tentativas demonstrativas. Callers de saveAttempt em produção continuam sendo os seeds. Treino guiado não grava tentativa no repositório do Professor; reanalisar não inclui automaticamente a escrita nova.

**Prova histórica:** usuário novo tem4 tentativas pontuadas63/66/89/91 e4 glifos pessoais preenchidos.

**Aceite:** instalação vazia sem evolução inventada; treino real salva tentativa identificada; reanálise inclui exatamente essa tentativa. Migrar/isolar apenas seeds conhecidos e preservar dados reais, sem trocar notas dos exemplos como correção.

### S08 — P1 — Ausência de dados vira maturidade e medidas inventadas

**Local:** `MotorDiagnosticEngine.kt:117,235,290,342`; `CoachingCurriculumGenerator.kt:21`.

Sem tentativas: overall70/PRACTITIONER. Sem descendentes: observed=alvo e score70 GOOD. Sem velocidades: observed0,40/score75 GOOD. Sem pressão: observed0,5/score75 GOOD e “Pressão uniforme mantida”. Nada disso foi medido. primaryWeakness=null da instalação vazia aciona mestria, com “maturidade caligráfica superior”, foco to e Ghost10%.

**Provas:** noWritingProducesMaturityAndMasteryPrescription; absentSensorIsReportedAsMeasuredGoodPressure.

**Aceite:** SEM_DADOS/INSUFICIENTE explícito, observed ausente e dimensão não medida excluída da nota/maturidade. Treino inicial pode existir com descrição honesta. Corrigir testes que exigem nota70 como aceitação, preservando evidência diagnóstica histórica.

### S09 — P1 — Diagnóstico não mede as pautas declaradas

**Local:** `MotorDiagnosticEngine.kt:96–205`.

Contenção usa média de scorePercent, sem baseline/x-height/ascender/descender. Sem attempts usa altura20..400px para escolher80 ou60; texto “sem estouros” extrapola entrada. Ângulo atan2(dy,dx), filtro30..90, difere da convenção do avaliador/guia já discutida em R11. Thresholds dy>3/dist>4 dependem da densidade; target52 mistura estilos. Contraste não condicionado a estilo; pressão elevada não prova tensão muscular.

**Aceite:** contexto de estilo/pauta/escala/capacidades; mesma curva reamostrada e deslocada, estilos68/85/90, ausência de descidas e cruzamento de cada limite. Linguagem deve descrever observação, sem diagnóstico biomecânico/postural não comprovado.

### S10 — P1 — Todos os focos prescritos têm IDs inexistentes

**Local:** `CoachingCurriculumGenerator.kt:30,44,57,70,83`; MainActivity rota TEACHER_AI; `GuidedPracticeViewModel.kt:81`.

Focos to/t/a/it/l não existem: catálogo usa letter_t/letter_a/letter_l e não tem to/it. selectGlyphById retorna silenciosamente; tela abre exercício anterior. Warmups underturn/compound_curve/ascending_loop também divergem de basic_*. Navegação transmite só focusExerciseId, sem aplicar duração/aquecimento/Ghost/meta. markPrescriptionCompleted não tem caller de UI/resultado.

**Prova:** everyPrescribedFocusIsMissingFromRealCatalog verifica todos os ramos.

**Aceite:** prescrição resolvível, erro explícito de ID, contexto aplicado, conclusão ligada à tentativa. Não inventar IDs/fallback para exercício qualquer para tornar o teste verde.

### S11 — P2 — Professor faz análise na main thread e deixa falhas escaparem

**Local:** `TeacherViewModel.kt:58–102`; `LocalTeacherRepository.kt:122`.

viewModelScope.launch executa getAllAttempts síncrono e análise completa sem dispatcher de trabalho. Múltiplas listas/passagens crescem com histórico. Sem try/finally, flags loading/analyzing podem ficar presas e erro escapa. Diagnóstico/prescrição/insights são três commits independentes com tmp fixo sem exclusão mútua.

**Acerto:** Teacher faz write/flush/sync com descriptor aberto. Preservar isso.

**Aceite:** trabalho pesado fora da main, operações ordenadas, snapshot coerente, cancelamento respeitado e erro recuperável. Testar dupla reanálise, falha em cada arquivo e histórico grande. Medir antes de criar cache/agregação incremental.

### S12 — P1 — Referência de assinatura compartilha lista mutável do canvas

**Local:** `SignatureCanvasView.kt:85,92,170`; `ExpansionsViewModel.kt:90,137`.

Callback entrega completedStrokes diretamente. ViewModel guarda mesma lista em currentStrokes e baselineAttempt.strokes. Desenhar/salvar referência/limpar para nova tentativa esvazia também a referência. Undo e novos strokes a modificam.

**Prova:** baselineListAliasingAllowsClearToEraseReference demonstra modelo; ligação real confirmada nos callbacks, sem teste instrumentado.

**Aceite:** snapshots imutáveis nas fronteiras, baseline inalterada após clear/undo/novo stroke e emissões observáveis corretas. List em data class não congela MutableList compartilhada.

### S13 — P1 — Canvas de assinatura reintroduz falhas de captura

**Local:** `SignatureCanvasView.kt:96–184`.

Sempre pointer0; aceita dedo como tinta; ERASER vira stroke desenhado; sem activePointerId/POINTER_UP. UP não acrescenta endpoint. Pressão0 vira null; tilt/orientation descartados sempre. CANCEL apaga ativo sem registro. Não reutiliza política/correções do pipeline principal.

**Aceite:** DOWN/MOVE/UP com endpoint exclusivo, pointer não zero, palma/dedo simultâneo, borracha, zeros e eixos ausentes, cancelamento/lifecycle. Reutilização/extração mínima do pipeline, sem reescrever engine inteiro. S25 ainda necessário para aprovar palma/estabilidade.

### S14 — P1 — Referência não persiste e View não restaura estado

**Local:** `ExpansionsViewModel.kt:107–145,186`; `ExpansionsScreen.kt:271–286`.

saveAsBaseline só atualiza StateFlow; loadBaselineSignature é vazio. Não grava referência vetorial. AndroidView factory/update não chama setStrokes do estado do VM. Recriar aba pode mostrar canvas vazio com métricas/export do estado anterior; processo encerrado perde referência.

**Aceite:** persistência vetorial robusta, reload e View recriada a partir de snapshot. Testar troca de aba, navegação, recriação e processo encerrado. PNG não substitui raw strokes.

### S15 — P2 — Export de assinatura pode cortar geometria e SVG não é entregue como arquivo

**Local:** `SignatureExporter.kt:20–115`; `ExpansionsScreen.kt:201–218,415–427`; `ExpansionsViewModel.kt:148–183`.

SVG1080x500/PNG1200x600 usam coordenadas cruas do canvas240dp, sem transformação de origem. Cores/larguras também são substituídas por defaults/mínimos. SVG aparece em diálogo de até15 linhas sem salvar/compartilhar; PNG vai a filesDir/signatures.

**Aceite:** espaço lógico e transformação preservando aspecto/margens, pontos isolados e diferentes densidades; PNG transparente verificado em runtime gráfico; arquivo local acessível. Imagem de assinatura não é assinatura digital criptográfica.

### S16 — P2 — Nota de assinatura não mede equivalência de forma

**Local:** `SignatureConsistencyEngine.kt:76–160`.

Contagem/duração/aspecto/velocidade podem coincidir em formas diferentes. Diagonais opostas recebem100 e texto de dinâmica muscular praticamente idêntica. maxX/maxY começam em Float.MIN_VALUE, positivo, inadequado a coordenadas negativas.

**Prova:** oppositeSignatureDiagonalsReceivePerfectConsistency.

**Aceite:** limitar texto às quatro métricas ou comparar geometria caso requisito exija forma. Sem afirmar autenticação. Testar formas distintas com estatísticas iguais e coordenadas negativas.

### S17 — P1 — Cópia de textos é cronômetro sem captura de escrita

**Local:** `ExpansionsScreen.kt:578–615`; `PassagePacingEngine.kt:15`.

Botões iniciam/finalizam cronômetro. WPM usa todas as palavras do texto mesmo sem desenhar. Sem canvas na aba ou tentativa/histórico integrado. Pausas entre letras são inferidas só do tempo total. Relógio civil e estado remember não dão lifecycle de sessão robusto.

**Aceite:** escrita real e conclusão verificável; distinguir contagem declarada de medição, monotonicidade, pausa/cancelamento/lifecycle. Sem escrita não diagnosticar movimento. Não trocar nome do botão mantendo dossiê de modo completo.

### S18 — P1 — Curva de pressão selecionada não afeta tinta

**Local:** `ExpansionsViewModel.kt:282`; `PressureCalibration.kt:38`; buscas globais de callers.

setPressureCurve só altera estado/snackbar. transform não tem caller de produção, preferência não persiste e não chega a renderer/pipeline. Função matemática testada não é integração S Pen.

**Aceite:** preferência persistida e propagada ao derivado visual; mesma sequência com curvas diferentes deve renderizar diferente e manter rawPressure idêntica. Não modificar sensores brutos para simular integração.

### S19 — P1 — Watch e alerta contínuo são infraestrutura não integrada

**Local:** `WatchCompanionBridge.kt:37–82`; `ExpansionsViewModel.kt:301`.

isWatchConnected sempre false. sendTimerSync emite broadcast sem receptor/ponte Wear observados; true significa ausência de exceção. sendTimerSync/sendPhaseChangeHaptic/onWritingActivityDetected não têm callers em produção. Só botão de teste vibra telefone; não há monitor alimentado por escrita.

**Aceite:** concluir percurso declarado com adapter e evidência ou declarar stub/fallback manual e marco parcial. Testar fase real, tempo ativo e reconexão. Broadcast enviado não prova recebimento. Sem Watch, concluir partes independentes e manter PENDENTE_DISPOSITIVO.

### S20 — P2 — Estilos novos no registro não entregam pedagogia específica

**Local:** `StyleEngine.kt:29,46`; ExpandedStyles; ReferenceGlyphCatalog.

Três estilos entram de fato em getAvailableStyles/getStyle. Não há catálogo novo por estilo: permanecem12 exercícios com referências anteriores enquanto guias mudam85/90. R05, rejeição de estilo pessoal sem customFontPath, continua.

**Aceite:** matriz de suporte por estilo e coerência de guia/referência/avaliação ao selecionar Gótica/Itálica/Uncial. Metadado de estilo não prova desenho/ductus específico entregue.

### S21 — P1 — Release público sem assinatura verificável; minificação desativada

**Local:** `app/build.gradle.kts:37–44`; workflow; [release v0.8.0](https://github.com/playertwo1/caligrafia/releases/tag/v0.8.0).

Assets: app-debug.apk22.732.123 bytes e **app-release-unsigned.apk16.528.809 bytes**. Segundo arquivo baixado/verificado com apksigner36.0.0: DOES NOT VERIFY/Missing META-INF/MANIFEST.MF. SHA256 `888d3b53397eeb816562c177d6185e2740c6446766cde453f647a5765e798ddd`, igual ao digest GitHub. Não é conclusão só pelo nome.

Build permite keystore ausente e workflow publica release/*.apk assim mesmo. isMinifyEnabled=false; arquivo proguard-android-optimize não ativa R8. Release sem minify pode ser decisão válida, mas não comprova otimização por R8. Drive não foi inspecionado e não se presume arquivo idêntico.

**Aceite:** publicação assinada falha sem credenciais; verificar assinatura/certificado esperado, versão/applicationId e hash antes de upload; instalação/upgrade preservam dados. Não expor segredos. Se ativar minify, testar release/reflexão; toggle não prova qualidade.

### S22 — P2 — Watchdog/testes têm alcance menor que a aprovação anunciada

**Local:** scripts/watchdog.ps1; workflow; app/build.gradle.kts testOptions.

Watchdog busca string WebView e3 famílias de dependências, roda unit tests/build debug e checa existência/tamanho. Não roda lint nem valida integração, raw, atomicidade, assinatura ou frescura/versão do APK. Artefato antigo recebe OK após build falhar, embora resultado global continue corretamente reprovado.

isReturnDefaultValues=true/sem runtime gráfico limitam testes Android. Fixtures artificiais e defaults explicam parte dos verdes. binaryResultsDirectory com timestamp cria diretórios sucessivos e não resolve origem dos locks.

**Aceite:** explicitar alcance, adicionar gates pertinentes e artefato fresco; testes Android para eventos/pixels/lifecycle. Classificar48 warnings:14 DefaultLocale,14 GradleDependency,10 UseKtx e outros. Sem upgrade amplo só para zerar aviso automático. Corrigir locking com evidência, sem ocultar falhas.

### S23 — P1 — Gates documentais fechados sem evidência e instruções divergentes

**Local:** ROADMAP/PROJECT_STATE/O_QUE_FOI_FEITO/AUDIT_REPORT_M7_M8/README/AGENTS.

v0.8.0 declara conclusão total. README ainda descreve fundação não implementada e AGENTS remoto prioriza M0. AGENTS local fornecido pelo usuário contém protocolo V2, ausente no SHA remoto. Não presumir que Antigravity viu arquivos locais não presentes no seu checkout; divergência tampouco aprova os defeitos.

**Aceite:** sincronizar instruções/matriz no checkout real antes de corrigir; manter dossiês como alegações históricas com ligação à revisão independente; mudar gates somente com evidência por ID. M7/M8 parciais, sem aprovação retroativa dos anteriores.

### S24 — P2 — Serializers permissivos mascaram corrupção

**Local:** `TeacherSerializer.kt:52–103,206–256`; `LocalTeacherRepository.kt:33–109`; BackupSerializer.

Regex não implementa JSON completo: aspas escapadas podem truncar, escapes não são decodificados, delimitadores em strings fragilizam arrays/objetos, números exponenciais não são lidos integralmente. Faltantes viram defaults plausíveis. Repositório pode substituir diagnóstico inválido por default, apagando arquivo corrompido; insights vazios legítimos são regenerados.

**Evidência:** estática; cada variante de escape não foi reproduzida dinamicamente. S04 tem prova de manifesto inválido.

**Aceite:** parser/esquema/versionamento adequados, round-trip de aspas/barras/newline/Unicode/números, corrupção preservada com erro observável. Cancelamento separado de IO; não capturar Throwable para converter toda falha em fallback.

## 6. Pendências anteriores

[V2 e plano obrigatório](ANTIGRAVITY_AUDIT_REVIEW_V2.md#plano-obrigatório-de-estabilização-para-o-antigravity) continuam normativos. Não duplicar nomes de correção nem fechar categoria por um exemplo.

| IDs | Estado/evidência nesta revisão |
|---|---|
| R01/R08 | REPRODUZIDOS: timer padrão0 após espera;300 ticks encerram299s |
| R02 | ABERTO: aulas sem escrita real; notas manuais |
| R03/R04 | REPRODUZIDOS:4 tentativas/4 glifos seed; símbolo do alfabeto ainda ignorado na navegação |
| R05 | REPRODUZIDO: personal_style_v1 resolve cursiva_escolar |
| R06 | REPRODUZIDO: instâncias A/B escrevem; reabertura só B |
| R07 | REPRODUZIDO: Learning21min/Evolução20min;0s vira1min |
| R09 | Padrão de IO REPRODUZIDO e código antigo presente: descriptor fechado, sync falha |
| R10 | ABERTO; sem nova prova concorrente; implementação não corrigida no delta |
| R11 | REPRODUZIDO: linha52° compila fallback60° |
| R12 | REPRODUZIDO: metade do replay500ms/1000ms para durações1s/2s |
| R13 | ABERTO: preview/coordenadas/pontos isolados sem correção nem nova validação gráfica |
| R14 | REPRODUZIDO: mesma reta100 densa/38 endpoints |
| R15 | ABERTO; S08/S13 ampliam lacuna de sensor |
| R16/R17/R18 | ABERTOS por fluxo/delta: escala export não conectada; import sem percurso; cadência limitada |
| R19 | ABERTO: Ink fallback, concorrência/lifecycle, benchmark/performance sem fechamento |
| R20 | ABERTO: testes/versões promovidos indevidamente a gate |

A01–A22 mantêm estado por subitem da V1/V2. Não se reabrem correções reais do eraser/pointer-up principal: S13 é regressão em superfície nova. Também não se fecham lacunas antigas sem prova nesta revisão.

## 7. Plano obrigatório de estabilização para o Antigravity

**Missão: estabilizar o produto existente. Sem M9, redesign geral, backend/cloud/LLM, framework novo, reescrita ampla ou expansão de catálogo como substituto de correção.** Auditar M7/M8 existentes não aprova retrospectivamente o roadmap.

### Onda0 — Estado confiável e dados protegidos

Confirmar SHA/branch/instruções no checkout real; preservar alterações. Abrir matriz A/R/S e corrigir anúncios de conclusão/assinatura. Resolver S07/S08 e R03/R04 sem apagar dados reais. Resolver S01–S05 ANTES de expor restore na UI.

**Saída:** ZIP íntegro, cobertura real, rejeição segura de pacote inválido, recuperação/transação demonstrada; usuário novo sem evolução fictícia. Ainda não fechar marco.

### Onda1 — Estado, persistência e relógio

R01/R06/R08/R09/R10/R19 + S11/S12/S14/S24. Uma ordem de escrita por entidade, snapshots imutáveis, lifecycle e erro observáveis. Separar atomicidade de arquivo, transação de conjunto, durabilidade e ordenação.

**Saída:** fault injection/reabertura preservam estado válido; timer funciona no construtor real; referência não muda com nova tentativa e sobrevive ao processo.

### Onda2 — Integração real

R02–R06 + S06/S07/S10/S17/S18/S19. UI → captura → avaliação → tentativa → histórico/evolução/alfabeto → diagnóstico → prescrição → exercício correto → nova tentativa. Backup/assinatura precisam de entrega local acessível.

**Saída:** roteiro completo com handlers reais. Falta de Watch não impede integração local; etapa física permanece pendente.

### Onda3 — Medidas e captura

R07/R11–R18 + S08/S09/S13/S15/S16/S20. Sem defaults como medidas; contexto de estilo/escala; gabarito/export/replay consistentes; densidade, coordenadas, pontos e ferramentas testados.

**Saída:** testes discriminantes corretos e validação gráfica adequada; sem precisão/maturidade inventada para preencher UI.

### Onda4 — Distribuição, qualidade e aparelho

S21/S22 + R19/R20. Assinatura/hash/certificado, instalação/upgrade, lint classificado, benchmark antes/depois. S25 com Android/One UI registrados: escrita longa, palma, bordas, pause/resume, sensores, export/replay. Watch com evidência própria.

**Saída:** distinguir APROVADO_AUTOMATIZADO, PARCIAL e PENDENTE_DISPOSITIVO. Não promover etapa física por tempo gasto ou número de testes.

### Onda5 — Consolidação e reauditoria

Atualizar documentos/matriz, preservar histórico e apresentar SHA/diff/evidências/limites à revisão independente. **Não escrever “Codex aprovou” antes de aprovação explícita.** Não fechar M8 por corrigir ZIP, nem M7 por corrigir um ID.

### Cartão obrigatório por ID

```text
ID / prioridade / estado:
Estado: ABERTO | EM_CORRECAO | PARCIAL | PENDENTE_DISPOSITIVO | CORRIGIDO_A_REVISAR
SHA/branch inicial e final:
Causa raiz e requisito violado:
Percurso real de UI/chamadores:
Arquivos/funções, mudança mínima e motivo:
Teste que falha antes e passa depois:
Comando, resultado, caminho da evidência:
Dados preservados/migração/recuperação:
Limites/dependências/próxima ação da onda:
Revisão independente: NÃO REALIZADA até que aconteça
```

Não alterar teste para aceitar bug. Converter probes em expectativas de aceitação corretas preservando história. Não chamar20 probes de20 funcionalidades aprovadas. Não usar seed/default/stub/catch silencioso para comprovar recurso.

## 8. Otimizações condicionadas à medição

| Área | Ação após correção funcional | Verificação |
|---|---|---|
| SignatureCanvas | Cache de paths concluídos, reutilizar objetos; hoje há Path e redesenho a cada frame | frame time/allocations com1k/10k/100k pontos |
| Professor | Dispatcher e snapshot; incremental só se necessário | tempo UI/memória/cancelamento com histórico grande |
| Backup | Inventário consistente, streaming limitado, progresso/erro | round-trip, RAM/disco, falta de espaço |
| Persistência | Proprietário explícito, ordem, geração/versionamento | concorrência/fault injection |
| Render/export | Transformação de coordenadas comum | imagens em densidades/tamanhos distintos |
| Telas | Separar componentes de ExpansionsScreen937 linhas/TeacherScreen564 após estabilizar contratos | handlers/testabilidade, sem redesign |
| Build | Triar48 warnings e diretórios com timestamp; resolver locks | repetibilidade, crescimento do cache e CI |

## 9. Evidências e decisão

[Código auditado](https://github.com/playertwo1/caligrafia/tree/bb09dc4a8331cab4ba8922e82479293806c294b1), [release observada](audit-v3/github-release.json), [V1](ANTIGRAVITY_AUDIT_REVIEW.md), [V2](ANTIGRAVITY_AUDIT_REVIEW_V2.md). Evidências executáveis e logs estão em audit-v3.

**Não aprovo o fechamento M0–M8.** Está comprovado que a suíte JVM original passa e que o código compila em diretório novo. Correções, integração real, release assinado e gates físicos continuam necessários. Esta tarefa produziu documentação/evidências; não publicou APK nem implementou correções de produto.
