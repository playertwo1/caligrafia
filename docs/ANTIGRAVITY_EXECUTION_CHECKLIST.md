# Ordens de execução — Scribe funcional

## 1. Quem decide e quem executa

**Codex organiza os requisitos e verifica as entregas. Antigravity implementa, testa e apresenta provas.** Este checklist é a ordem de execução; o [roadmap funcional](ANTIGRAVITY_FUNCTIONAL_ROADMAP.md) explica o produto e os PNGs orientam a aparência. Em divergência de sequência ou escopo, prevalece este checklist. Não reorganizar ondas, substituir requisitos ou declarar aprovação do Codex.

Começar na primeira caixa desmarcada. Se o código já atende, verificar e anexar prova antes de marcar; não reimplementar só para produzir diff. Não marcar um bloco inteiro de uma vez. As caixas estão inicialmente vazias porque este documento é uma ordem de trabalho, não uma certificação da implementação existente.

**Marcação:** `[x]` significa executado e comprovado pelo implementador. Codex registra a revisão separadamente. Para cada ID marcado, acrescentar em `docs/product-delivery/EVIDENCE.md`: SHA, arquivo/símbolo, caminho de UI, teste/comando e resultado, evidência visual quando pertinente, limites. Um teste pode cobrir vários IDs, mas a relação deve estar explícita.

**Desvio:** se houver decisão de produto não coberta, registrar `DECISION_REQUEST.md` com ID, impedimento e opções; não inventar comportamento. Continuar somente tarefas independentes já autorizadas. Escolhas locais de nome/organização que não alterem contrato não exigem consulta. Falta de hardware fica pendente e não impede trabalho de software independente.

## 2. Decisões já tomadas — executar como escrito

| Tema | Decisão obrigatória |
|---|---|
| Navegação | Quatro abas: Caderno, Praticar, Evolução, Mais. Praticar abre Hub; nunca inicia exercício automaticamente |
| Voltar | Volta à origem conservando IDs e seleção; na raiz respeita retorno/saída Android sem perder trabalho |
| Entrada padrão | Somente caneta. Toque escreve apenas quando habilitado; borracha nunca gera stroke de tinta |
| Sessão | Uma sessão ativa por vez; pedir encerrar/continuar ao tentar iniciar outra; troca de aba/fundo pausa |
| Tempo | Medir intervalos com relógio monotônico; registrar datas com relógio civil; reabrir sempre pausado |
| Dados | Strokes brutos são fonte de verdade; imagens/curvas/avaliações são derivados |
| Estado | Escrita confirmada atualiza todos os consumidores; save com erro não mostra sucesso |
| Referências | Catálogo didático separado dos dados pessoais; ausência nunca recebe seed ou nota inventada |
| Estilos | Seis estilos do painel; completar referências dos exercícios oferecidos em cada estilo, sem reaproveitar modelo incompatível |
| Importação de fonte | TTF/OTF é referência visual; não inventar ordem de strokes nem avaliação de ductus |
| Professor | Coaching local determinístico; explicar medida e origem; sem backend/LLM nesta entrega |
| Backup | Substituição confirmada do conjunto, com recuperação da versão anterior em falha; sem merge silencioso |
| Arquivos externos | Usar seletor Android para abrir/criar documentos; cancelamento é estado normal |
| Dispositivo | S25 Ultra/S Pen para validação física; emulador prova somente o que efetivamente exercita |
| Arquitetura | Reutilizar packages e modelos atuais; não reescrever ink nem migrar toda persistência como atalho |
| Entrega | Cada botão tem ação, estado, dados e teste; placeholder nunca conta como fluxo entregue |

## F0 — Preparar base e proteger os dados

Arquivos principais: `BackupSerializer`, `ScribeBackupManager`, `LocalPracticeAttemptRepository`, histórico e fábricas de repositórios. Dependência: nenhuma. Saída: base segura para desenvolver os fluxos.

- [x] F0.01 Registrar branch, HEAD, upstream e arquivos modificados em `docs/product-delivery/BASELINE.md`; preservar alterações existentes.
- [x] F0.02 Ler este checklist, roadmap funcional e parecer `ANTIGRAVITY_AUDIT_REVIEW_V5_RECHECK.md`; registrar a primeira tarefa selecionada.
- [x] F0.03 Executar runner de auditoria vigente e salvar log; contar testes/classes/falhas pelos XML, não pelo texto fixo do script.
- [x] F0.04 Criar `ROUTE_ACTION_MATRIX.md` com colunas ID, origem, botão, destino, argumentos, handler, repositório, teste e estado.
- [x] F0.05 Inventariar os arquivos reais gravados por cada repositório e suas versões; registrar caminhos em `DATA_CONTRACT.md`.
- [x] F0.06 Incorporar as contraprovas de `docs/audit-v5/recheck/AuditV5RecheckTest.kt` à suíte permanente sem enfraquecer asserções.
- [x] F0.07 Corrigir parser: distinguir valor null válido de erro; rejeitar valor ausente antes de alterar arquivos ativos.
- [x] F0.08 Testar JSON com valor ausente, vírgula final, número inválido, controle não escapado, versão/tipo incompatível e conteúdo após objeto.
- [x] F0.09 Testar ZIP inválido sobre dados existentes e comprovar bytes originais preservados; não testar apenas o retorno do parser.
- [x] F0.10 Corrigir invalidação de strokes quando o manifesto ou seu conjunto é substituído; não atualizar apenas metadados.
- [x] F0.11 Testar leitor já aberto após outro escritor substituir strokes do mesmo ID; conferir os pontos novos e o contexto.
- [x] F0.12 Definir proprietário compartilhado dos repositórios por armazenamento; substituir instâncias divergentes nos consumidores afetados.
- [x] F0.13 Garantir erro observável de leitura/save; arquivo corrupto não deve ser sobrescrito com estado vazio como recuperação silenciosa.
- [x] F0.14 Definir vínculo de IDs entre sessão, tentativa, exercício, estilo, variante, prescrição e texto; preservar leitura de registros antigos.
- [x] F0.15 Definir espaço lógico/pauta/versão da referência por tentativa e página; migração não inventa medidas ausentes.
- [x] F0.16 Registrar resultado de cada contraprova e da suíte relevante em EVIDENCE; classificar falhas anteriores e novas separadamente.
- [x] F0.G Confirmar que manifesto inválido não modifica ativos e leitor aberto recebe strokes substituídos; submeter F0 à revisão Codex.

## F1 — Navegação, biblioteca e caderno completos

Alvos: `MainActivity`, telas/VM de caderno, páginas, toolbar e menu Mais. Referências: painel 1 e fluxos 01–03. Entrada: F0.G executado; bloqueio de revisão deve ser respeitado se houver rejeição explícita.

- [x] F1.01 Implementar raiz de Praticar como Hub com categorias, seleção de aula e duração; não disparar timer ao abrir aba.
- [x] F1.02 Criar menu Mais com Alfabeto, Professor, Estilos, Assinaturas, Cópia, Backup, S Pen/Watch, Laboratório e Preferências nomeados.
- [x] F1.03 Mapear todas as entradas à tela real correspondente; registrar destinos ainda incompletos como PARCIAL, sem contá-los como entregues.
- [x] F1.04 Preservar pilha/seleção por aba; voltar de detalhe à origem sem abrir caderno ou exercício diferente.
- [x] F1.05 Remover callbacks que descartem exerciseId, styleId, variantId, prescriptionId ou textId.
- [x] F1.06 Implementar vazio da biblioteca com botão Criar; nenhuma página/caderno de exemplo entra no acervo.
- [x] F1.07 Criar caderno com nome obrigatório de 1–40 caracteres, espaços externos removidos, papel e estilo; erro de validação junto ao campo.
- [x] F1.08 Fazer Cancelar não gravar nada; Criar só fechar formulário após persistência confirmada.
- [x] F1.09 Listar nome, papel/estilo, contagem real, última atualização e estado de save; remover dados ilustrativos.
- [x] F1.10 Implementar busca por nome sem diferenciar maiúsculas e acentos; mostrar vazio de busca e limpar filtro.
- [x] F1.11 Implementar abrir, renomear e excluir caderno com confirmação identificando o alvo; cancelamento preserva tudo.
- [x] F1.12 Mostrar grade de páginas com miniaturas derivadas dos strokes reais, página ativa e contagem consistente.
- [x] F1.13 Implementar adicionar e renomear página; página criada tem ID próprio e papel/pauta do caderno.
- [x] F1.14 Implementar duplicar página com novos IDs de página/strokes e mesma geometria; editar cópia não altera original.
- [x] F1.15 Implementar excluir página com confirmação; se ativa, selecionar vizinha válida; sem páginas, mostrar Criar página.
- [x] F1.16 Conectar exportar página à seleção da página exata; concluir o export externo em F5.
- [x] F1.17 Expor caneta fina/média/grossa e cinco cores do fluxo 03; manter seleção visível e persistida.
- [x] F1.18 Fazer undo/redo operar na página atual; criação de stroke após undo invalida redo adequadamente.
- [x] F1.19 Implementar borracha por stroke sem converter gesto de apagamento em tinta; undo restaura conteúdo apagado.
- [x] F1.20 Implementar painel de guias: estilo, preview, inclinação, aplicar e redefinir; Copperplate padrão 52°.
- [x] F1.21 Persistir configuração e dimensões lógicas por página; resize/reabertura não deforma os pontos.
- [x] F1.22 Ordenar gravações/trocas de página; stroke em curso não pode ser salvo na página recém-selecionada.
- [x] F1.23 Exibir salvando/salvo/erro com tentar novamente; não apagar versão anterior por falha.
- [x] F1.24 Testar pela UI dois cadernos e três páginas, duplicação, exclusão, troca rápida e reinício sem mistura de dados.
- [x] F1.25 Capturar screenshots reais de biblioteca vazia, criação, lista, grade, caneta, borracha e guias; comparar aos PNGs.
- [x] F1.G Registrar F1 como validado somente com percurso biblioteca → página → escrita → reinício completo.

## F2 — Aula e treino com resultado real

Alvos: `LearningHubScreen`, `LearningViewModel`, `SessionTimer`, `GuidedPracticeViewModel`, catálogo e avaliador. Referências: painel 2–3 e fluxos 04–05.

- [x] F2.01 Exibir os cinco estágios: Traços, Famílias de letras, Conexões, Palavras e Frases.
- [x] F2.02 Listar as 18 lições existentes com nome, objetivo e exercícios; corrigir IDs não resolvíveis antes de oferecer Iniciar.
- [x] F2.03 Expor duração 5/10/15/20 min e estilo; resumo da seleção deve aparecer antes do início.
- [x] F2.04 Mostrar Continuar apenas para sessão real recuperável; exibir objetivo e tempo registrado.
- [x] F2.05 Conectar revisão SRS ao exercício real; não criar resultado ao simplesmente abrir revisão.
- [x] F2.06 Fixar uma única sessão ativa; ao iniciar outra, oferecer continuar a existente ou encerrá-la antes de criar nova.
- [x] F2.07 Transportar lessonId/exerciseId/styleId/duração para o treino sem fallback genérico.
- [x] F2.08 Exibir cinco fases pedagógicas reais; registrar nomes e distribuição de tempo do domínio no contrato, sem copiar quatro passos da imagem.
- [x] F2.09 Iniciar relógio somente após ação Iniciar; usar tempo monotônico e exibir tempo efetivamente praticado.
- [x] F2.10 Implementar pausa manual, retorno, encerramento antecipado e pausa por troca de aba/fundo.
- [x] F2.11 Persistir sessão interrompida e reabrir pausada; não somar tempo fora do app nem desfazer pausa manual.
- [x] F2.12 Mostrar nome/símbolo, objetivo, instrução e referência correta do exercício, corretamente enquadrada.
- [x] F2.13 Implementar Cobrir com modelo sobreposto, Copiar com referência adjacente e Sozinho sem modelo sobreposto.
- [x] F2.14 Tornar Ghost 100/70/40/10/0 acessível sem corte; mudança afeta somente derivado visual.
- [x] F2.15 Ajustar alvo e pauta à área disponível sem alterar o espaço lógico; referência não deve virar ponto minúsculo inacessível.
- [x] F2.16 Capturar strokes completos e preservar tool, timestamps e eixos presentes; zero não significa ausência.
- [x] F2.17 Invalidar feedback no começo do novo stroke; não esperar terminar para retirar nota anterior.
- [x] F2.18 Conectar avaliar à tentativa atual e à referência/estilo selecionados; vazio/incompleto não recebe aprovação padrão.
- [x] F2.19 Persistir tentativa concluída com IDs, contexto, tempo e avaliação disponível; repetir cria novo ID.
- [x] F2.20 Fazer evento de tentativa real alimentar sessão e SRS; remover pontes de teste que simulem essa integração manualmente.
- [x] F2.21 Encerrar sessão uma única vez, mesmo após duplo toque/retentativa; cancelamento não cria sessão concluída fictícia.
- [x] F2.22 Mostrar resumo com tempo exato, tentativas reais, última tentativa e botões Rever/Voltar/Próximo treino.
- [x] F2.23 Implementar detalhe de feedback com trecho e linha medidos; texto e destaque devem nomear a mesma linha.
- [x] F2.24 Fazer Tentar novamente conservar alvo/estilo e criar tentativa limpa, preservando a anterior.
- [x] F2.25 Testar percurso Hub → duas tentativas → pausa → retorno → resumo → histórico/SRS pela UI.
- [x] F2.26 Testar 59 segundos, sessão sem avaliação, interrupção e finalização duplicada; registrar esperado/observado.
- [x] F2.G Entregar vídeo do percurso e provas de persistência; sessão útil é requisito, tela isolada não fecha F2.

## F3 — Evolução, alfabeto e estilos utilizáveis

Alvos: `EvolutionScreen`, comparação/replay, `AlphabetScreen`, `StyleEngine`, compilador e repositórios. Referências: painel 4/5/7 e fluxos 06–08.

- [x] F3.01 Mostrar histórico completo, calendário e duração em data local; remover limite de 20 do cálculo global.
- [x] F3.02 Implementar filtro por alvo/estilo e seleção de tentativa; vazio orienta praticar e não inventa resultado.
- [x] F3.03 Atualizar histórico já aberto após nova tentativa/restore, incluindo miniaturas e strokes.
- [x] F3.04 Implementar replay individual play/pause/seek e 0,5x/1x/2x com cor/largura originais.
- [x] F3.05 Permitir escolher explicitamente duas tentativas do mesmo alvo e contexto compatível; explicar incompatibilidade.
- [x] F3.06 Exibir datas e métricas com unidades; dado ausente mostra ausência e não delta zero fictício.
- [x] F3.07 Implementar replay duplo em tempo real; normalização opcional precisa de rótulo próprio.
- [x] F3.08 Usar transformação espacial comum e estável na comparação/replay; não recalcular autoescala a cada frame.
- [x] F3.09 Implementar sobreposição e slider com legenda antes/depois; mudança visual não altera raw.
- [x] F3.10 Testar 21+ sessões e dois strokes de 1s/2s; verificar calendário, tempos relativos e escala.
- [x] F3.11 Mostrar alfabeto vazio real, glifos e variantes por glifo; ausência abre convite à prática.
- [x] F3.12 Conectar Adicionar variante à seleção de tentativa compatível ou nova prática daquele glifo.
- [x] F3.13 Mostrar miniatura, data e replay da tentativa selecionada antes de confirmar variante.
- [x] F3.14 Persistir variante com ID/proveniência; atualizar alfabeto já aberto após salvar no treino.
- [x] F3.15 Implementar renomear, favorita única, duplicar e excluir variante com confirmação.
- [x] F3.16 Duplicação não conta como novo treino nem amostra independente para métricas pessoais.
- [x] F3.17 Usar como referência/Praticar com esta deve passar variantId exato e renderizar seus strokes.
- [x] F3.18 Exclusão de variante não apaga a tentativa de origem; favorita removida exige seleção explícita de outra ou estado vazio.
- [x] F3.19 Criar catálogo visual dos seis estilos com nome, descrição e preview; seleção deve persistir.
- [x] F3.20 Criar matriz estilo × exercício e completar referências corretas dos exercícios oferecidos; proibir triângulo genérico para glifo ausente.
- [x] F3.21 Aplicar o estilo a guia/modelo/avaliação e persistir contexto por tentativa; não usar alvo global 52° para todos.
- [x] F3.22 Implementar importação TTF/OTF pelo seletor Android com validação, cancelamento e erro recuperável.
- [x] F3.23 Copiar fonte autorizada para armazenamento do app ou persistir acesso apropriado; testar reabertura offline.
- [x] F3.24 Mostrar preview e glyph ausente explicitamente; rotular fonte como referência visual sem análise de ductus.
- [x] F3.25 Meu estilo: mostrar exemplos/favoritas e métricas suficientes; botão Criar fica indisponível com explicação quando faltam dados.
- [x] F3.26 Compilar derivação dos exemplos reais, manter raw, registrar versão/proveniência e invalidar ao trocar favorita.
- [x] F3.27 Conectar Visualizar no caderno e Praticar com meu estilo à derivação atual; glifo sem exemplo não ganha seed.
- [x] F3.28 Testar comparação → variante → favorita → estilo pessoal → treino → reinício; confirmar os mesmos IDs e desenho.
- [x] F3.G Entregar capturas dos três estados de cada fluxo 06/07/08 e evidências de todos os controles.

## F4 — Professor e cópia de textos

Alvos: Teacher, gerador de prescrição, Guided, catálogo de textos e rota Passages. Referências: painel 6/9 e fluxo 09.

- [x] F4.01 Mostrar Professor vazio com ação Primeiro treino; dimensão sem dados não recebe valor fictício.
- [x] F4.02 Calcular diagnóstico com contexto individual de estilo/pauta/referência, sem alvo global indevido.
- [x] F4.03 Vincular observação à tentativa/trecho que a sustenta e expor Ver detalhe.
- [x] F4.04 Remover alegação de tensão muscular, autenticidade ou diagnóstico médico não medidos.
- [x] F4.05 Executar leitura/cálculo/save fora da main quando bloqueantes; serializar reanálises e mostrar erro/tentar novamente.
- [x] F4.06 Mostrar prescrição com exercício, objetivo, duração, Ghost e fases/séries reais.
- [x] F4.07 Validar todos os IDs prescritos contra catálogo antes de habilitar Iniciar; erro não vira exercício genérico.
- [x] F4.08 Transportar prescriptionId e parâmetros completos; aplicar 70% ao Ghost, não ao tamanho da tela.
- [x] F4.09 Concluir treino relaciona resultado à prescrição e atualiza Professor; mostrar progresso apenas quando comparável.
- [x] F4.10 Testar orientação em dois estilos e destino correto para conexões/letras; guardar vídeo até resultado atualizado.
- [x] F4.11 Implementar lista de textos com preview e autoria disponível, escolha de estilo e botão Copiar.
- [x] F4.12 Passar textId/conteúdo ao canvas de cópia; não redirecionar ao exercício `/`.
- [x] F4.13 Exibir texto recolhível, timer, pausa/retorno e espaço para continuar em página adicional.
- [x] F4.14 Concluir cópia salva texto vinculado, strokes e tempo; não afirmar reconhecimento/correção textual sem medição.
- [x] F4.15 Abrir cópia pelo histórico com texto e escrita exatos; retomada interrompida conserva o trabalho.
- [x] F4.16 Testar dois textos diferentes, pausa, reinício e conclusão; nenhum deles vira modelo genérico.
- [x] F4.G Demonstrar Professor → prescrição → resultado e Texto → escrita → histórico, ambos com dados reais.

## F5 — Assinaturas, exportação e recuperação

Alvos: `SignatureCanvasView`, `ExpansionsViewModel`, exportadores, gerenciador de backup. Referências: painel 8/10 e fluxos 10–11.

- [ ] F5.01 Unificar política de pointer/borracha/eixos da assinatura com escrita principal; preservar endpoint e zeros suportados.
- [ ] F5.02 Salvar referência como conjunto consistente de raw/metadados; erro de gravação não confirma sucesso.
- [ ] F5.03 Confirmar antes de substituir referência; Nova tentativa não apaga referência existente.
- [ ] F5.04 Aplicar strokes ao recriar a View, não apenas ao ViewModel; testar sair/voltar e reiniciar.
- [ ] F5.05 Mostrar comparação apenas de medidas disponíveis; nenhuma alegação de assinatura autêntica ou dinâmica muscular.
- [ ] F5.06 Implementar preview e seleção SVG/PNG transparente da assinatura atual/referência explicitamente escolhida.
- [ ] F5.07 Enquadrar bounds negativos e espessura em ambos os formatos sem corte ou deformação.
- [ ] F5.08 Conectar exportar página com fundo marfim/pautado ou branco/sem linhas e PNG; preservar proporção lógica.
- [ ] F5.09 Solicitar nome/destino via seletor Android; tratar cancelamento, acesso negado e falha de escrita sem falso sucesso.
- [ ] F5.10 Abrir PNG/SVG produzidos fora do app e conferir conteúdo, transparência e dimensões.
- [ ] F5.11 Inventariar backup com contagens reais de entidades, não quantidade de arquivos como se fossem sessões.
- [ ] F5.12 Incluir cadernos, tentativas/sessões/SRS, variantes, estilos/fontes, Professor, assinaturas, cópias e preferências persistidas.
- [ ] F5.13 Exportar pacote para URI escolhida; confirmar fechamento do ZIP e reabertura válida.
- [ ] F5.14 Selecionar pacote externo, validar versão/gramática/tipos/caminhos/payloads e mostrar revisão antes de restaurar.
- [ ] F5.15 Informar que restauração substitui conjunto e solicitar confirmação identificando arquivo/contagens.
- [ ] F5.16 Implementar staging e recuperação persistente do conjunto anterior; coordenar escritores durante commit e recuperação.
- [ ] F5.17 Em sucesso invalidar todos os consumidores, inclusive caches vetoriais; removidos no pacote não podem ressuscitar por merge.
- [ ] F5.18 Em erro mostrar condição real e recuperação disponível; só afirmar dados inalterados quando verificado.
- [ ] F5.19 Testar pacote inválido, cancelamento, falha entre arquivos e interrupção de processo durante restore.
- [ ] F5.20 Testar export/restore de conjunto misto em armazenamento limpo e em acervo existente; conferir IDs, raw e vínculos.
- [ ] F5.21 Testar referência de assinatura pelo round-trip e abertura da View restaurada.
- [ ] F5.G Entregar arquivos reproduzíveis e prova de recuperação; sem isso não publicar restore como concluído.

## F6 — Preferências e laboratório

Alvos: preferências persistidas, adaptadores de captura/render, Watch e laboratório. Referências: painel 11/12 e fluxo 12.

- [ ] F6.01 Criar tela Preferências com grupos Escrita, Leitura e Pausas, com ações Voltar/Concluir coerentes.
- [ ] F6.02 Persistir mão direita/esquerda; aplicar posição recomendada da barra, permitindo ajuste independente explícito.
- [ ] F6.03 Persistir posição esquerda/direita da barra e demonstrar efeito no editor sem cobrir área da palma.
- [ ] F6.04 Implementar somente caneta/caneta e toque; não tratar palma como desenho no padrão.
- [ ] F6.05 Implementar escala de texto com preview respeitando escala do sistema; nenhum controle essencial pode desaparecer.
- [ ] F6.06 Implementar contraste das guias com preview e efeito no canvas, sem modificar raw.
- [ ] F6.07 Implementar reduzir animações e aplicar às transições; replay voluntário permanece disponível.
- [ ] F6.08 Implementar curva Linear/Suave/Firme, preview e teste; aplicar só ao render derivado e persistir escolha.
- [x] F6.09 Testar igualdade dos dados de pressão bruta antes/depois das três curvas. Evidência local: `PhaseF6AcceptanceTest`.
- [ ] F6.10 Implementar lembrete de pausa habilitado/intervalo durante sessão ativa; pausa/fundo não acumulam alertas indevidos.
- [ ] F6.11 Implementar vibração habilitada e Testar vibração, tratando capacidade/permissão indisponível.
- [ ] F6.12 Exibir Watch conectado/desconectado pela conexão real; sem relógio, manter fallback no telefone.
- [ ] F6.13 Conectar eventos de fase/pausa ao adapter e testar desconexão sem travar sessão; registrar recepção física separadamente.
- [ ] F6.14 Expor Laboratório no menu Mais com gravar/parar, limpar, salvar/reabrir e replay de amostra.
- [ ] F6.15 Mostrar pressão/tilt/orientação disponíveis/ausentes/a verificar conforme dispositivo, sem inferir suporte pelo valor zero.
- [x] F6.16 Isolar amostras do laboratório de histórico de aprendizagem, SRS e alfabeto pessoal. Repositórios/diretórios exclusivos confirmados.
- [ ] F6.17 Testar reinício para cada preferência e raw do laboratório; registrar o efeito visual, não apenas valor em memória.
- [ ] F6.18 Registrar teste físico S25 Ultra e Watch quando disponíveis; sem aparelho, marcar pendência física específica.
- [ ] F6.G Entregar matriz opção → efeito → persistência → teste; nenhuma opção deve existir só como texto/snackbar.

## F7 — Verificação e entrega final

- [ ] F7.01 Percorrer cada linha da ROUTE_ACTION_MATRIX; remover lacunas de handler, argumento, estado e persistência.
- [ ] F7.02 Confirmar correspondência das 12 áreas do painel e dos 12 fluxos complementares; registrar diferenças justificadas.
- [ ] F7.03 Comparar screenshots reais com as 12 pranchas; não usar imagens geradas como prova de implementação.
- [ ] F7.04 Verificar S25 Ultra em fonte padrão/ampliada, insets, teclado e alcance da S Pen; todos os níveis Ghost acessíveis.
- [ ] F7.05 Verificar labels de acessibilidade, ordem de foco, contraste e alvos de toque dos controles essenciais.
- [ ] F7.06 Executar a jornada: caderno → aula → treino → feedback → resumo → comparação → variante → estilo pessoal.
- [ ] F7.07 Continuar jornada: Professor → treino prescrito → cópia → assinatura → export → backup → restore → reinício.
- [ ] F7.08 Repetir casos relevantes em instalação atualizada com acervo antigo; nunca limpar dados para fazer teste passar.
- [ ] F7.09 Executar suíte completa, runner de auditoria, assembleDebug e lint; preservar XML/logs associados ao mesmo SHA.
- [ ] F7.10 Verificar APK de entrega assinado, versão/SHA/hash, instalação e atualização preservando acervo; não publicar segredos.
- [ ] F7.11 Atualizar PROJECT_STATE com itens concluídos, parciais e pendências físicas; corrigir afirmações antigas que conflitem com evidência vigente.
- [ ] F7.12 Entregar índice de evidências e lista de falhas conhecidas ao Codex; não afirmar aprovação independente antecipadamente.
- [ ] F7.G Codex registra parecer final; somente então converter o conjunto revisado em entrega aprovada.

## 3. Mensagem obrigatória ao terminar cada etapa

Usar este formato, sem declarar a onda inteira concluída por avanço parcial:

```text
Etapa: F?.??–F?.??
SHA/branch:
Caixas concluídas e evidências:
Arquivos/símbolos alterados:
Percurso real demonstrado:
Testes: comando, quantidade real, falhas, XML/log
Dados preservados/migração:
Pendências e bloqueios:
Próxima caixa:
Revisão Codex: AINDA NÃO REALIZADA / referência ao parecer
```

Para continuar após interrupção: ler PROJECT_STATE, checar SHA, abrir primeira caixa desmarcada, conferir evidência da anterior e retomar. Não replanejar o produto. Não pedir ao usuário que escolha entre requisitos já decididos aqui.
