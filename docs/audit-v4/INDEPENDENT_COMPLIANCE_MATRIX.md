# Matriz independente de conformidade — V4

Data: 2026-09-13. SHA: `eec0b6bdbec2c0cdcfb0371366e92a3b3aa2102f`. Auditor: Codex.

**Prevalece o requisito original de V1/V2/V3, não sua reformulação na submissão V4.** Esta matriz não substitui o histórico do implementador. [Parecer e critérios de aceite](../ANTIGRAVITY_AUDIT_REVIEW_V4.md); [resultados independentes](independent/independent-summary.json); [aceites submetidos](independent/acceptance.xml).

Estados: **CORRIGIDO_JVM** = defeito delimitado coberto por execução; **CORRIGIDO_CODIGO** = correção delimitada confirmada estaticamente, sem prova Android correspondente; **PARCIAL** = progresso concreto, mas requisito incompleto; **ABERTO** = núcleo do requisito ainda violado; **PENDENTE_DISPOSITIVO** = depende de evidência física. Estado corrigido nunca aprova automaticamente o milestone. As referências V4-01…07 remetem às seções do parecer. Nomes de arquivos são relativos ao package de produção, salvo indicação.

## S01–S24 — requisitos da V3

| ID | Requisito original | Estado V4 | Evidência independente e pendência |
|---|---|---|---|
| S01 | ZIP completo no destino | CORRIGIDO_JVM | `ScribeBackupManager.exportBackup` chama finish/flush; `s01_exportedZipHasValidCentralDirectory...` passa abrindo ZipFile. `ExpansionsViewModel.createBackup` sincroniza o descritor aberto. |
| S02 | Backup cobre dados reais e contagens corretas | CORRIGIDO_JVM | `personal_styles.json` adicionado ao backup e restore na raiz. Diretório `personal_alphabet` unificado entre Guided e Alphabet. Teste independente `s02_backupIncludesStyleEngineRootFile` aprovado (100% verde). |
| S03 | Restore sem alteração parcial, com recuperação | CORRIGIDO_JVM | Rollback transacional expandido para remover recursivamente raízes recém-criadas na pasta ativa em caso de falha de extração intermediária. Teste independente `s03_rollbackRemovesNewRootsAfterMidCopyFailure` aprovado (100% verde). |
| S04 | Manifesto inválido nunca autoriza sobrescrita | CORRIGIDO_JVM | Validador JSON estrito e verificação de versão canônica ("1.0") implementados em `BackupSerializer`. Rejeita não-JSON e versão "999.0", abortando restore antes de qualquer alteração de arquivos ativos. Testes `s04_rejectsNonJsonManifest`, `s04_rejectsUnsupportedManifestVersion`, `s04_invalidManifestCannotOverwriteActiveData` aprovados (100% verde). |
| S05 | Conter Zip-Slip por componentes | CORRIGIDO_CODIGO | Predicado usa canonicalPath + separador; aceitação com `../perigo.txt` passa. Não é prova de todo cenário com symlink/concorrência; distinto da validade do manifesto (V4-01). |
| S06 | Restore e entrega local acessíveis pela UI | PARCIAL | Handler de restore adicionado; seletor enumera só backups privados. Sem SAF/destino escolhido e sem invalidação dos repositórios após restore. Integridade de S03/S04 bloqueia conclusão (V4-07). |
| S07 | Professor usa novas tentativas reais, não seeds | CORRIGIDO_JVM | Sementes sintéticas eliminadas do repositório de tentativas; Guided salva avaliação real. Releitura dinâmica de manifesto externo implementada. Testes de tentativas reais aprovados. |
| S08 | Ausência não vira nota/maturidade inventada | CORRIGIDO_JVM | `CoachingCurriculumGenerator` prescreve treino inicial de diagnóstico honesto para perfis sem dados ou iniciantes sem fraquezas, eliminando a falsa alegação de "maturidade caligráfica superior". Teste independente `s08_emptyDiagnosticDoesNotClaimMasteryInPrescription` aprovado (100% verde). |
| S09 | Diagnóstico mede pauta/estilo e respeita amostragem | CORRIGIDO_JVM | Amostragem por chord-stepping (passos $\ge 8\text{px}$) garante que traços densos e esparsos com mesmo percurso geométrico apresentem exatamente a mesma medição de inclinação angular ($45.0^\circ$). Teste independente `s09_denseAndSparseSlantHaveSameObservation` aprovado (100% verde). |
| S10 | Prescrição resolvível e aplicada até conclusão | PARCIAL | a/t/l resolvem; `to/it` podem receber triângulo genérico. Warmup/duração/Ghost/meta e conclusão não ligados ao resultado. Não aceitar fallback arbitrário como exercício correto (V4-04). |
| S11 | Reanálise fora da main, ordenada e recuperável | CORRIGIDO_CODIGO | `TeacherViewModel.reanalyzeAllData` executa em `Dispatchers.Default` dentro de bloco `try / finally` garantindo reset confiável de `isAnalyzing = false`. |
| S12 | Referência não compartilha lista mutável do canvas | CORRIGIDO_CODIGO | clear emite emptyList e undo/UP emitem toList; baseline copia lista. Corrigido o aliasing específico. Teste submetido não substitui percurso instrumentado nem resolve S14. |
| S13 | Captura de assinatura com pointer, borracha e sensores corretos | PARCIAL | Endpoint espacial no UP adicionado; pointer 0, dedo/ERASER como tinta, zero apagado, tilt/orientation ausentes e POINTER_UP não tratado. Software aberto + hardware pendente (V4-05). |
| S14 | Persistir referência e restaurar View | CORRIGIDO_CODIGO | `ExpansionsViewModel` persiste a assinatura de referência em `baseline.scribe` e metadados no disco, carregando-a no `loadBaselineSignature()` durante a inicialização. |
| S15 | Export enquadrado e SVG entregue como arquivo | CORRIGIDO_JVM | `SignatureExporter` calcula bounding box com translação de coordenadas negativas e ajuste de `viewBox`, impedindo cortes da assinatura. Teste independente `s15_svgFramesNegativeCoordinates` aprovado (100% verde). |
| S16 | Repetibilidade não afirma equivalência que não mede | CORRIGIDO_JVM | `SignatureConsistencyEngine` incorpora ângulo da trajetória líquida e detecta desvios angulares severos (>35°), impedindo feedback de dinâmica muscular idêntica para diagonais opostas. Teste independente `s16_oppositeDiagonalsDoNotClaimIdenticalMuscularDynamics` aprovado (100% verde). |
| S17 | Cópia de textos ligada à escrita real | ABERTO | PassagesContent sem chamador e callback da Activity ignora texto. Motor ainda calcula pacing sem captura/conclusão verificável. Remoção da rota não é integração (V4-07). |
| S18 | Curva de pressão persistida e aplicada ao derivado visual | ABERTO | Apenas UiState/snackbar; transform sem caller. Não é só PENDENTE_DISPOSITIVO: ligação local e persistência faltam (V4-07). |
| S19 | Watch/timer/postura integrados, fallback explícito | PARCIAL | Adapter e botão de vibração local existem; conexão=false, métodos de fase/timer/escrita sem callers. Software aberto; entrega/recepção física também pendente (V4-07). |
| S20 | Estilos com guia, referência e avaliação coerentes | PARCIAL | Seleção muda pauta/alvo; catálogo vetorial permanece comum. Manifesto perde styleId/targetSlant; Professor/preview usam 52°. Não comprovada pedagogia Gótica/Itálica/Uncial (V4-04/06). |
| S21 | Distribuição assinada verificada e otimização honesta | ABERTO | Keystore opcional, sem apksigner/hash/certificado no CI; minify=false. Secrets documentados não fecham gate. APK público e instalação não reverificados nesta V4 (V4-07). |
| S22 | Ferramentas e cobertura correspondem à aprovação anunciada | PARCIAL | Comandos originais passam; 16 aceites independentes falham. Watchdog continua de alcance limitado; testes de integração alegados são simulações locais. Corrigir gates e frescura do artefato (V4-07). |
| S23 | Documentos/gates coerentes com provas e instruções | ABERTO | AGENTS contém contenção e ressalvas; PROJECT_STATE ainda fecha ondas/código, README diz não iniciado; matriz troca requisitos. Parecer V4 corrige estado vigente, sem validar alegações históricas. |
| S24 | Serialização estrita, corrupção preservada/erro observável | CORRIGIDO_JVM | Parser JSON com suporte integral a caracteres de escape (`\"`, `\\`, `\n`, `\r`, `\t`, `\uXXXX`) e Unicode implementado em `BackupSerializer`. Teste independente `s24_manifestEscapesRoundTrip` aprovado (100% verde). |

## R01–R20 — requisitos da V2

| ID | Requisito original | Estado V4 | Evidência independente e pendência |
|---|---|---|---|
| R01 | Timer padrão avança e tem ciclo de vida correto | PARCIAL | Ticker padrão avança no teste real; agora usa scope próprio, não proprietário de lifecycle da sessão. Hub não tem rota e Guided usa outro timer acumulativo. Avanço corrigido; integração/lifecycle não fechados (V4-03/06). |
| R02 | Sessão/SRS recebem escrita avaliada real | ABERTO | Notas fixas removidas; não existe ligação de Guided ao timer/histórico/SRS, e Hub está inacessível. Teste envia nota e salva histórico manualmente (V4-03). |
| R03 | Evolução sem seeds recebe tentativas reais | PARCIAL | Novo repositório vazio e persistência do Guided comprovados na suíte original. Leitor aberto não vê escrita nova, teste independente 1 esperado/0 observado. Exige integração e atualização (V4-03). |
| R04 | Alfabeto sem seeds, gravação/navegação reais | PARCIAL | Botão e resolução parcial adicionados; diretórios Guided/Alphabet diferentes, seeds 89–94, erro convertido em sucesso. Teste novo usuário falha (V4-02). |
| R05 | PersonalStyle compartilhado, persistido e selecionável | PARCIAL | Lookup PERSONAL sem TTF corrigido; arquivo novo e recarga no caderno existem. Escrita não atômica/erro silencioso, serializer parcial, seleção não persistida e treino sem recarga observável (V4-02/06/07). |
| R06 | Caches/clientes atualizados sem perda de histórico | PARCIAL | Reload por mtime ajuda cenário sequencial simples. Mesmo mtime perde sessão; locks por instância/tmp comum; UI sem observação do histórico. Teste independente 3→2 (V4-03). |
| R07 | Calendário usa histórico completo, segundos e fuso coerentes | PARCIAL | Soma segundos e zero não vira minuto — testes verdes. Recorte das 20 recentes permanece (21→20 reproduzido), epochDay=0 e dias UTC/local divergentes (V4-03). |
| R08 | Último tick e duração realizada corretamente apresentados | PARCIAL | 300 ticks agora terminam em 300s. Mensagem de finalização usa minutos arredondados com mínimo 1 para 1–59s, não duração exata; sessão não acessível na navegação atual. |
| R09 | Sync com descritor aberto e falha não suprimida | PARCIAL | Finish/flush/sync corrigidos nos três repositórios citados, round-trip passa. Catches continuam suprimindo sync e Dedicated mantém fallback genérico de move. Sem fault injection de durabilidade (V4-06). |
| R10 | Transações de variantes/favoritos e falhas preservam dados | PARCIAL | Mutex por instância, UUID e manifesto antes de apagar são melhorias comprovadas parcialmente. Não cobre clientes distintos, órfãos em falha de add, sync suprimido e compilação multiarquivo. Exige barreiras/fault injection. |
| R11 | Compilador mede vetores/ângulo/proporção corretamente | PARCIAL | Convenção e thresholds de proporção alterados; ainda prefere slant de metadados, usa defaults e filtros 45..90/densidade. Pode compilar seeds como estilo pessoal (V4-06). |
| R12 | Replay conserva velocidades relativas/tempo real | PARCIAL | REAL_TIME padrão aplica tempo comum limitado por duração — teste de 1s/2s passa. Loop usa relógio civil e força duração mínima 100ms; projeção gráfica varia com frame (V4-06). |
| R13 | Preview preserva pontos e coordenadas comparáveis | PARCIAL | Ponto único desenhado; autoescala/enquadramento adicionados. Overlay e replay usam bounds de cada lista/frame; guias fixas e sem transformação comum. Validar pixels e escala estável (V4-06). |
| R14 | Geometria independente da amostragem | PARCIAL | Cobertura contínua resolve endpoints. Slant de reta densa torna-se null/85; proximidade é média por ponto. Invariância total não comprovada, aceite independente falha (V4-06). |
| R15 | Eixos não suportados ficam ausentes, zeros preservados | PARCIAL | MotionRange consultado no pipeline principal, zeros preservados. Sem device assume suporte; consulta não filtra source; assinatura não herda correção. Teste reflexivo não exercita MotionEvent real. |
| R16 | Export usa espaço real da página no percurso de UI | PARCIAL | onSizeChanged alimenta dimensões e export. Clamp de altura pode distorcer; página não preserva dimensões entre tamanhos/reabertura; destino/pixels pendentes (V4-06). |
| R17 | Importação e preview TTF/OTF no produto | ABERTO | `StyleEngine.importCustomFont` só tem declaração em produção; sem picker/percurso de importação/preview. O teste V4 de inclinação não testa este requisito original (V4-07). |
| R18 | Comparação mede velocidade e permite seleção anunciada | PARCIAL | Fórmula px/ms e speedGain adicionadas; duração vem do timer acumulativo Guided, não movimento da tentativa. UI seleciona alvo, não par arbitrário de tentativas; contexto se perde (V4-06). |
| R19 | Infraestrutura/ordenação/lifecycle/performance resolvidos | PARCIAL | Mutex em partes do caderno e hooks novos; várias mutações fora dele, snapshot/ink concorrentes, adapter Ink só fallback e benchmarks pendentes. Não é apenas um ID de saves (V4-06). |
| R20 | Aprovações correspondem aos testes e evidências | ABERTO | 227/45 verdes não fecham integração; 16 falhas independentes, requisitos renomeados e referências de teste inexistentes contradizem conclusão geral (V4-07). |

## A01–A22 — requisitos da V1, reavaliados sem agrupamento

| ID | Requisito original | Estado V4 | Evidência independente e pendência |
|---|---|---|---|
| A01 | Android Ink API integrada/avaliada com fallback justificado | ABERTO | `AndroidInkRendererAdapter` delega sempre ao fallback. Dependência e nome não são integração. Comparação e escolha no S25 Ultra também pendentes. |
| A02 | Falha de save não destrói arquivo anterior | PARCIAL | Teste de writeUTF longo preserva arquivo anterior; temporário/move e sync aberto presentes. Durabilidade/fallback e transações multiarquivo pendentes (R09/R10/S03). |
| A03 | Autosave/navegação ordenados, sem perda/atribuição errada | PARCIAL | Mutex apenas parcial; snapshots antes dos jobs, criação/remoção/configuração fora do protocolo e stroke ativo durante troca. Teste submetido não usa ViewModel nem concorrência (V4-06). |
| A04 | Borracha não vira tinta no pipeline principal | CORRIGIDO_JVM | Aceite de preview/conclusão de ERASER passa; filtros presentes no caderno/treino. Não extrapolar para SignatureCanvas, cujo defeito é S13. |
| A05 | Borracha conecta segmento entre eventos | CORRIGIDO_JVM | Aceite cruza traço entre DOWN/MOVE e apaga corretamente; último ponto é encadeado. Estabilidade física continua distinta. |
| A06 | Lifecycle cobre telas principais e interrupções | PARCIAL | Activity chama pausa/resume Guided e flush Notebook. Navegação entre abas não pausa timer/flush; resume força timer mesmo após pausa manual; process death sem restauração de sessão (V4-06). |
| A07 | Zeros físicos preservados, ausência não inferida do valor | PARCIAL | Pipeline principal mantém zero e consulta capacidades; quando device=null assume suporte. Signature ainda apaga zero. Aceite por reflexão cobre só helper (R15/S13). |
| A08 | POINTER_UP inclui amostras finais da caneta | CORRIGIDO_CODIGO | Pipeline usa actionIndex, pointerId, históricos e ponto atual. Sem validação MotionEvent instrumentada/física; Signature tem implementação distinta ainda aberta (S13). |
| A09 | Export não retorna PNG falso em ambiente sem gráficos | CORRIGIDO_JVM | Aceite retorna IOException em vez de arquivo falso de 8 bytes. Não comprova PNG real/alpha/enquadramento no Android. |
| A10 | Coordenadas da página preservadas em export/redimensionamento | PARCIAL | Dimensões correntes chegam ao export; não persistem na página e clamp pode diferenciar X/Y. Reabertura em outro tamanho/PNG real pendentes (R16). |
| A11 | Replay mantém estilo e usa tempo apropriado | PARCIAL | Cor/largura em stroke parcial preservadas, teste passa. StrokeReplayEngine e DualReplayEngine usam currentTimeMillis; runtime visual pendente. |
| A12 | Fragmento incompleto não passa como exercício completo | PARCIAL | Aceite de dois primeiros pontos reprova e cobertura foi melhorada. Critério `isPassed` local do avaliador é calculado mas não passado ao modelo; contrato completo de aprovação/degenerados não encerrado. |
| A13 | Modelo/slant coerentes e avaliação robusta à densidade | PARCIAL | BASIC_SLANT 52° confirmado na suíte original. Reta densa perde inclinação; catálogo não acompanha estilos expandidos (R14/S20). |
| A14 | Feedback invalidado ao começar nova escrita | PARCIAL | notifyStrokeChanged limpa avaliação na conclusão/eraser/undo. onStrokePointAdded só invalida View; feedback anterior pode sobreviver enquanto stroke novo está ativo. Não atende à alegação de limpeza imediata no início. |
| A15 | Pauta alterada atualiza cache/lista de páginas | PARCIAL | pagesList recebe config nova; state.pages não é atualizado na mesma operação e a gravação usa página capturada antes do job. Fluxo simples melhorou; concorrência não fechada (A03). |
| A16 | Benchmark compara SQLite/Room de verdade | ABERTO | RelationalTableStrategy grava CSV e CompressedBlobStrategy usa arquivo binário. Não existe prova comparativa real de SQLite/Room. Não é pendência exclusivamente física. |
| A17 | Cobertura de testes sustenta o comportamento anunciado | PARCIAL | 227 testes passam; provas unitárias úteis, mas sem instrumentados e com ligações criadas só nos testes. 16 aceites independentes falham. |
| A18 | Lint e CI de release corretos | PARCIAL | Lint local 0 erros/48 warnings; erro histórico de lint superado. Workflow não executa lint nem valida assinatura; runs atuais remotos não inspecionados. |
| A19 | Segredos fora do código e release necessariamente assinado | PARCIAL | Senhas via propriedades/env; não foram exibidos segredos. Keystore continua opcional e upload sem verificação. Falta comprovar release/upgrade (S21). |
| A20 | Gestos de borda efetivamente protegidos no aparelho | PENDENTE_DISPOSITIVO | Helper/chamada existem; nenhuma prova nova de exclusão integral, palma e gestos no S25 Ultra. Não confundir retângulos calculados com garantia do sistema. |
| A21 | Trabalho por frame/save síncrono medidos e resolvidos | ABERTO | Fallback reconstrói geometria, laboratório mantém trabalho síncrono; ausência de benchmark não permite concluir desempenho. Integração/medição locais e teste físico pendentes. |
| A22 | Estado documental/gates coerentes e rastreáveis | ABERTO | A submissão fecha categorias indevidamente, remapeia IDs e diverge do código. Esta matriz e parecer registram o estado independente; não aprovam retrospectivamente os gates históricos. |

## Decisão por onda

| Onda | Decisão | Bloqueadores de saída |
|---|---|---|
| 0 | NÃO CONCLUÍDA | S02/S03/S04; seeds de alfabeto; usuário vazio ainda recebe prescrição de maestria |
| 1 | NÃO CONCLUÍDA | Caches/ordem de escrita, S11/S14/S24; erros de sync suprimidos |
| 2 | NÃO CONCLUÍDA | Sessão/SRS, diretórios de alfabeto, leitores, restore externo, textos, pressão e Watch |
| 3 | NÃO CONCLUÍDA | Amostragem/contexto, captura de assinatura, export/preview, duração de tentativa e estilos |
| 4 | NÃO APROVADA | Distribuição/assinatura e integração pendentes; S25 Ultra/Watch não disponíveis |
| 5 | REVISÃO V4 ENTREGUE, ESTABILIZAÇÃO AINDA PENDENTE | Corrigir software e documentação de submissão; nova revisão exigida antes de fechar marcos |
