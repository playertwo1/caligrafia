# Roadmap executável — dos mockups ao Scribe funcional

> **Ponto de Parada Atual (2026-09-13):**
> - **Fases Concluídas:** F0, F1, F2, F3 e F4 (100% dos requisitos e testes de aceitação cumpridos, 297 testes unitários passando, 0 falhas, `assembleDebug` OK).
> - **Commits Publicados no GitHub:** `3d4f0c5` (F0), `9f692d4` (F1), `4dab8cb` (F2), `43a84d6` (F3), `127c604` (F4).
> - **Ponto Onde Parou:** Entrada da **Fase F5 — Assinaturas, exportação e recuperação (F5.01 a F5.21 e F5.G)**.
> - **Checklist de Execução:** [ANTIGRAVITY_EXECUTION_CHECKLIST.md](ANTIGRAVITY_EXECUTION_CHECKLIST.md) atualizado com as marcações de F0 a F4 concluídas.

> **Começar pelo [checklist obrigatório de execução](ANTIGRAVITY_EXECUTION_CHECKLIST.md).** Ele determina a ordem, decisões fixas e caixas individuais com provas. Este documento detalha objetivos e escopo; não substitui o checklist. Codex organiza/verifica; Antigravity executa e comprova.

## Mandato e resultado esperado

Pedido explícito do usuário em 2026-09-13: o MVP abre e permite escrever, mas faltam opções e objetivos dos PNGs de `docs/design`; implementar de verdade todas as áreas planejadas. Este documento passa a ser a sequência de produto vigente, vinculada ao ROADMAP. Substitui a restrição antiga de apenas reproduzir aparência ou manter as opções das imagens como propostas indefinidas. Mantém preservação de dados, execução incremental e comprovação por fluxo.

Não reiniciar o projeto. Reaproveitar telas, motores e repositórios existentes, corrigindo os percursos incompletos. Construir em Kotlin/Compose com canvas nativo e strokes brutos preservados. A autorização abrange as funcionalidades aqui enumeradas; não acrescenta contas, nuvem, marketplace, reconhecimento de assinatura ou backend/LLM. Professor significa coaching determinístico explicável nesta etapa.

**Resultado para o usuário:** escolher o que aprender e em qual estilo → iniciar aula com objetivo e duração → escrever com assistência progressiva → receber feedback verificável → salvar/rever/comparar → alimentar o alfabeto pessoal → aplicar sua referência → exportar e recuperar os próprios dados.

Referências consultadas: [painel geral](design/scribe-visao-12-telas-v1.png), todas as [12 pranchas de fluxos](design/fluxos-v1/README.md), PRODUCT_SPEC, ROADMAP, arquitetura e auditorias locais. As três fotos fornecidas mostram Biblioteca/Caderno/Treino; não provam ausência de todas as outras rotas, mas evidenciam a experiência insuficiente relatada. No treino, a barra Ghost aparece cortada e o exercício `/` ocupa uma pequena região sem expor claramente a jornada pedagógica: corrigir legibilidade, escala e descoberta das opções.

Base remota consultada: `4f8114f`, posterior à revalidação V5 de `32b28c1`. O último commit acrescenta branding/configuração de release; isso não comprova novas integrações de produto. Antes de implementar, registrar HEAD real e diferenças posteriores. Não atribuir aprovação V5 ao novo SHA sem revalidação.

## Navegação obrigatória

Manter quatro abas, com pilha de retorno e restauração de contexto por aba:

| Entrada | Destinos e ações |
|---|---|
| Caderno | Biblioteca → criar/abrir caderno → editor ↔ páginas → exportar; atalhos para estilos e alfabeto |
| Praticar | **Hub de aulas como entrada padrão**, objetivos/categorias/duração → sessão → avaliação/detalhe → nova tentativa/resumo; atalhos Cópia de textos e Professor |
| Evolução | Histórico/calendário → tentativa → replay → escolher duas tentativas → comparação/sobreposição; atalho salvar variante |
| Mais | Lista nomeada: Meu alfabeto, Professor, Estilos, Assinaturas, Cópia de textos, Backup, S Pen e Watch, Laboratório da caneta, Preferências |

“Mais” não deve depender de ícones sem nome nem de menu oculto de desenvolvimento. Praticar não abre sempre o mesmo `/`. O botão continuar só aparece com sessão recuperável real. Voltar retorna à origem; mudar de aba preserva o documento e pausa a sessão ativa conforme política abaixo. Seleções de exercício, estilo, sessão, tentativa, variante, prescrição e texto devem viajar por IDs estáveis; callback não pode descartar o argumento.

Criar um inventário `docs/product-delivery/ROUTE_ACTION_MATRIX.md` antes do código de UI: `ID → origem → controle → destino → parâmetro → handler → repositório → estado/erro → teste`. Cada ação das próximas seções precisa de linha. Nomes de rotas são contrato de produto, não exigência de nova biblioteca de navegação.

## Regras comuns de entrega

1. Cada item abaixo deve entregar UI, comportamento, persistência, navegação e evidência juntos. Cartão, snackbar, preview estático ou método sem chamador não encerra item.
2. Estados obrigatórios quando pertinentes: vazio, carregando, pronto, salvando, erro recuperável, dado insuficiente, capacidade indisponível. “Salvo” apenas após confirmação da operação.
3. Um registro real deve carregar origem, IDs relacionados, estilo e versão da referência, pauta/espaço lógico, strokes e tempos. Avaliação deriva desses dados; ausência de pressão/ângulo permanece ausência. Não usar nota padrão para completar fluxo.
4. Repositórios devem comunicar alterações aos consumidores existentes e invalidar também caches de strokes após restore/substituição. Não criar uma cópia divergente dos dados por ViewModel.
5. Referências pedagógicas do catálogo são permitidas e identificadas como modelo; jamais contam como progresso/variante pessoal. Fontes importadas são referência visual, não evidência de ordem de traçado.
6. Para o treino: congelar tempo praticado ao pausar, trocar aba ou ir ao fundo; retorno não desfaz pausa manual. Relógio monotônico para intervalos, relógio civil apenas para datas. Recuperar sessão interrompida como pausada, sem somar tempo fora do app. Finalizar grava uma única vez; cancelar não cria conclusão fictícia.
7. Telas devem caber no S25 Ultra com fonte ampliada, teclado e insets. Ghost 100/70/40/10/0 deve ser inteiramente acessível; usar seleção compacta ou rolagem explícita. Canvas amplo, alvo corretamente enquadrado e guias com transformação coerente. Não ampliar cabeçalhos decorativos à custa da escrita.
8. Preferir mudanças pequenas nos packages atuais (`guided`, `learning`, `evolution`, `alphabet`, `teacher`, `style`, `expansions`, `ink` e cadernos). Não reescrever o motor nem trocar armazenamento/dependências amplamente para entregar telas.

## Escopo de cada área do painel

### PF-01 — Caderno, biblioteca e páginas

Referências: painel 1; fluxos [01](design/fluxos-v1/01-biblioteca-cadernos.png), [02](design/fluxos-v1/02-organizacao-paginas.png), [03](design/fluxos-v1/03-ferramentas-escrita.png).

- Criar caderno com nome validado, papel pautado/em branco e estilo; cancelar sem criar registro. Listar, buscar por nome, abrir, renomear e excluir com confirmação. Capas são apresentação; não precisam de gerador nem catálogo novo.
- Grade de páginas com miniaturas reais, seleção, adicionar, renomear, duplicar, exportar e excluir. Duplicação copia strokes/pauta para novos IDs; editar a cópia não altera a origem. Exclusão da página ativa seleciona destino válido; última página tem estado explícito para criar outra.
- Editor: caneta fina/média/grossa, cores do mockup, undo/redo, borracha por stroke, configuração de guias e restauração do padrão. Copperplate canônico 52°. Persistir papel, estilo e espaço lógico da página, sem deformar desenho ao reabrir/redimensionar.
- **Aceite:** criar dois cadernos pela UI, escrever em páginas distintas, duplicar/editar/apagar uma cópia, matar/reabrir processo e confirmar independência, conteúdo, nomes, miniaturas e guias. Erro de save preserva a versão anterior e permite tentar novamente.

### PF-02 — Treino guiado

Referências: painel 2; fluxos 03/04/05. Entrada pelo Hub, alfabeto, estilo, Professor e botão repetir.

- Selecionar exercício real de traço/letra/conexão/palavra/frase, com título legível, objetivo, modelo correto e instruções. Manter identidade do exercício; `to/it` não podem cair em um triângulo genérico.
- Modos Cobrir → Copiar → Sozinho e controle Ghost 100/70/40/10/0. Definir Cobrir com modelo na área de escrita, Copiar com referência adjacente e Sozinho sem modelo sobreposto. Mudar opacidade não altera raw nem apaga tentativa em curso.
- Capturar, desfazer, limpar com proteção de tentativa não salva, avaliar e tentar novamente. Feedback anterior é invalidado no início de nova escrita. Guardar a tentativa concluída antes de iniciar outra e oferecer salvar no alfabeto quando o alvo for compatível.
- **Aceite:** selecionar ao menos três tipos diferentes, verificar referências distintas, escrever/avaliar/salvar e navegar ao histórico exato. Repetição não sobrescreve tentativa anterior; caneta, borracha e pausas mantêm comportamento coerente.

### PF-03 — Aulas e sessão com objetivo

Referências: painel 3; [fluxo 04](design/fluxos-v1/04-sessao-ativa.png). Reutilizar LearningHub/SessionTimer e integrar ao Guided.

- Hub mostra próximo treino explicando o objetivo, catálogo das 18 lições existentes em cinco estágios (Traços, Famílias de letras, Conexões, Palavras, Frases), revisões pendentes e continuar de onde parou. Não criar bloqueio artificial de todo o catálogo.
- Escolher 5/10/15/20 minutos, estilo e lição. Expor as **cinco fases pedagógicas do domínio**, com nomes e distribuição de tempo documentados; não copiar as quatro etapas simplificadas da imagem.
- Sessão compartilha captura/avaliação com PF-02. Cada tentativa alimenta sessão, histórico e SRS por evento real, sem registrar manualmente uma nota simulada no teste. Pausar, retomar, encerrar antecipadamente e resumo com tempo efetivo, tentativas e próxima ação.
- **Aceite:** percurso UI Hub → sessão → duas tentativas → pausa/retorno → encerramento → resumo → Evolução/SRS. Finalização repetida não duplica sessão; 59 segundos não viram 1 minuto obrigatório; interrupção restaura objetivo/tempo/estado pausado.

### PF-04 — Evolução, feedback e replay

Referências: painel 4; [05](design/fluxos-v1/05-feedback-geometrico.png) e [06](design/fluxos-v1/06-replay-comparacao.png).

- Histórico completo e calendário com datas locais e duração real, seleção de alvo/estilo e tela da tentativa. Vazio orienta iniciar treino; uma tentativa permite replay, mas explica por que comparação exige outra.
- Replay com play/pause, seek, 0,5x/1x/2x; escolher explicitamente duas tentativas compatíveis, exibir datas e unidades. Preservar tempos relativos em modo real e indicar qualquer normalização.
- Sobreposição com slider, mesma escala/pauta e transformação estável ao longo do replay. Deltas só de métricas disponíveis/comparáveis, sem declaração automática de melhora.
- Detalhe geométrico destaca trecho e linha realmente medidos; texto corresponde ao destaque. Retorno para repetir conserva alvo, estilo e prescrição.
- **Aceite:** sessões reais incluindo mais de 20 registros; selecionar duas tentativas com durações diferentes, confirmar replay e escala; novo resultado aparece sem reiniciar app. Testar amostragem densa/esparsa e alinhamento de coordenadas.

### PF-05 — Meu alfabeto e variantes

Referências: painel 5; [fluxo 07](design/fluxos-v1/07-variantes-alfabeto.png).

- Grade de glifos com vazio real; letra → variantes → escolher tentativa existente ou praticar para criar uma. Miniatura/replay, nome editável, favorita única por glifo, duplicar e excluir com confirmação.
- “Usar como referência” e “Praticar com esta” passam variantId/glifo/estilo corretos. Duplicação mantém proveniência e não conta como nova prática ou evidência independente nas métricas pessoais.
- **Aceite:** salvar tentativa no treino, vê-la no alfabeto já aberto, favoritar, reabrir processo e praticar exatamente com ela. Alterar/excluir não apaga a tentativa histórica original nem outra variante.

### PF-06 — Professor explicável

Referências: painel 6; [fluxo 09](design/fluxos-v1/09-coaching-explicavel.png).

- Sem dados: convite ao primeiro treino; dados insuficientes por dimensão ficam explícitos. Com dados: observação ligada a tentativa/trecho real, pauta e estilo corretos, limites da medida e próxima ação.
- Prescrição mostra objetivo, exercício resolvível, duração, Ghost e séries/fases; iniciar transporta todos esses campos. Concluir liga resultado à prescrição, atualiza diagnóstico e permite comparar a evidência anterior.
- Reanálise fora da main, ordenada, cancelável quando pertinente e com erro recuperável; não diagnosticar tensão muscular/doença ou chamar heurística local de IA generativa.
- **Aceite:** criar evidência em dois estilos, gerar orientação, abrir exercício correto, concluir e voltar ao Professor com resultado atualizado. Ausência de medida não vira maestria; ID inválido produz erro, nunca referência arbitrária.

### PF-07 — Estilos e meu estilo pessoal

Referências: painel 7; [fluxo 08](design/fluxos-v1/08-estilo-pessoal.png).

- Catálogo: Cursiva Escolar, Copperplate, Spencerian, Gótica, Itálica, Uncial. Cada cartão abre preview, descrição, guia, referências suportadas e aplicar/praticar. Não basta trocar nome/ângulo sobre o mesmo catálogo: registrar cobertura pedagógica por estilo e completar referências válidas para os exercícios oferecidos.
- Importar TTF/OTF por seletor Android, validar arquivo, nomear, pré-visualizar e persistir acesso/cópia local; fallback explícito para glyph ausente. Fonte importada não recebe avaliação de ductus que não possui.
- Meu estilo: vazio → adicionar exemplos → selecionar favoritas → calcular apenas métricas suficientes → criar/atualizar referência → visualizar no caderno/praticar. Favorita trocada invalida derivação antiga; não gerar fonte TTF como requisito oculto.
- **Aceite:** aplicar dois estilos com referências diferentes, importar fonte válida/inválida e reabrir; criar referência pessoal a partir de escrita real, mudar favorita e comprovar atualização no treino. Glifo sem exemplo indica ausência e não usa seed pessoal.

### PF-08 — Assinaturas e monogramas

Referências: painel 8; [fluxo 11](design/fluxos-v1/11-exportacao-arquivos.png).

- Canvas com política de entrada/borracha/sensores igual à escrita principal; definir referência, confirmar substituição, nova tentativa, comparar medidas geométricas e temporais e abrir a referência salva.
- Persistência consistente de strokes/metadados e restauração da View ao sair/voltar ou reabrir. Falha não anuncia referência salva.
- Exportar SVG/PNG transparente com preview, nome e destino externo. Enquadrar negativos e espessura sem corte; PNG e SVG representam o mesmo conteúdo.
- **Aceite:** referência real → reinício → nova tentativa → comparação → exportar/abrir arquivos → backup/restaurar em armazenamento limpo. Não usar “assinatura digital”, autenticidade ou dinâmica muscular como promessa.

### PF-09 — Cópia de textos

Referências: painel 9 e sessão 04.

- Lista de textos do catálogo, preview, autoria quando disponível, escolha de estilo e início; manter texto escolhido visível/recolhível junto ao canvas. Passar textId e conteúdo corretos ao destino.
- Pausar/retomar, páginas adicionais se necessário, concluir, salvar sessão e rever strokes/texto. Indicadores só medidos; sem OCR, não afirmar que palavras foram reconhecidas ou copiadas corretamente. Se calcular cadência com quantidade declarada, identificar essa origem.
- **Aceite:** escolher dois textos distintos, comprovar que o segundo não abre treino genérico; concluir e reabrir cópia exata pelo histórico, com tempo e escrita reais.

### PF-10 — Backup e restauração

Referências: painel 10; [fluxo 10](design/fluxos-v1/10-restauracao-backup.png).

- Mostrar inventário real, exportar `.scribepack` para destino escolhido e importar pelo seletor nativo. Revisar versão, conteúdo, contagens e política de conflitos antes de confirmar.
- Decisão deste plano: restauração deve recuperar o conjunto do pacote, com confirmação explícita para substituição e recuperação do conjunto anterior em falha; não fazer merge silencioso que ressuscita registros removidos. Implementador registra desenho transacional antes do código.
- Cobrir cadernos/páginas, tentativas/sessões/SRS, variantes, estilos/fontes, diagnóstico/prescrições, assinatura, cópias e preferências aplicáveis. Validar manifesto, caminhos e payloads antes de alterar ativos; coordenar escritores e recarregar consumidores.
- **Aceite:** exportar conjunto real misto → restaurar em ambiente limpo → conferir conteúdo e referências; pacote inválido, cancelamento e falha intermediária não perdem dados. Incluir casos JSON ausente e strokes em cache da V5. Falha de recuperação não pode exibir “dados inalterados”.

### PF-11 — S Pen, Watch e preferências

Referências: painel 11; [fluxo 12](design/fluxos-v1/12-preferencias-acessibilidade.png).

- Curva Linear/Suave/Firme com preview e teste de escrita; persistir e aplicar exclusivamente à aparência derivada. Raw pressure permanece original; capacidade ausente é informada.
- Preferências autorizadas: mão direita/esquerda, posição da barra, somente caneta/caneta e toque, escala de texto, contraste das guias, reduzir animações, lembrete de pausa/intervalo e vibração. Cada opção deve mudar a experiência correspondente e sobreviver ao reinício; nenhuma pode ser só snackbar.
- Lembretes operam durante sessão ativa, com permissões e indisponibilidade tratadas. Testar vibração no telefone. Watch mostra conexão real, envia eventos de fase/pausa e trata desconexão; fallback no telefone explícito. Entrega ao relógio exige validação própria, não apenas adapter.
- **Aceite:** modificar cada preferência, observar efeito e reabrir; raw antes/depois da curva é idêntico. Sem Watch, sessão funciona e UI informa ausência; com aparelho compatível, registrar envio/recepção real. Não condicionar recursos do telefone à disponibilidade de relógio.

### PF-12 — Laboratório da caneta

Referência: painel 12.

- Área de teste, limpar, gravar/parar, salvar/reabrir/replay de amostra isolada e capacidades de pressão/inclinação/orientação com disponível/ausente/a verificar. Não inventar capacidades quando device for desconhecido.
- Informar ferramenta usada e dados observados; exportar registro diagnóstico se necessário ao protocolo de medição. Amostras de laboratório não entram em evolução/alfabeto pessoal automaticamente.
- **Aceite:** gravar amostra, reabrir e comparar raw; unsupported permanece ausente, zero suportado permanece zero. Registrar teste S25 Ultra e limites de medição; laboratório acessível não prova benchmark ou adoção de Ink API.

## Sequência obrigatória para o Antigravity

As ondas abaixo são entregas funcionais, não novos rótulos de aprovação M0–M8. Começar pela primeira incompleta e avançar quando seus critérios estiverem demonstrados; não solicitar autorização novamente para cada etapa já incluída neste mandato.

| Onda | Itens e trabalho | Gate para avançar | Status de Execução |
|---|---|---|---|
| F0 — Integridade e mapa | Registrar SHA, inventário de ações, schema/IDs, corrigir duas contraprovas V5, baseline de dados reais e migração necessária | Manifesto inválido não altera dados; consumidor aberto recebe strokes novos; testes anteriores preservados. Sem exigir fechamento de todos os 66 IDs antes de iniciar qualquer UI | **CONCLUÍDO** (`3d4f0c5`) |
| F1 — Descoberta e caderno | PF-01; Hub PF-03 como entrada; menu Mais e pilhas de navegação para as 12 áreas | Percurso biblioteca/editor/páginas completo; rotas funcionais existentes acessíveis. Itens ainda em construção marcados no inventário, sem contá-los como entregues | **CONCLUÍDO** (`9f692d4`) |
| F2 — Aprender de ponta a ponta | PF-02/03; detalhe de feedback PF-04; catálogo de estilos inicial PF-07 | Escolher objetivo → sessão real → pausa → avaliar → resumo → histórico/SRS, sem dados artificiais | **CONCLUÍDO** (`4dab8cb`) |
| F3 — Rever e personalizar | PF-04/05/07 completos | Comparar duas tentativas → salvar/favoritar variante → usar referência pessoal, persistente e observável | **CONCLUÍDO** (`43a84d6`) |
| F4 — Orientar e copiar | PF-06/09 | Prescrição → treino correto → resultado; texto escolhido → cópia real → histórico | **CONCLUÍDO** (`127c604`) |
| **F5 — Levar e recuperar dados** | **PF-08/10 e exportações de PF-01** | **Arquivos abrem fora do app; referência e conjunto misto sobrevivem ao round-trip; falhas recuperáveis** | **PARADA ATUAL (A Iniciar: F5.01–F5.21 e F5.G)** |
| F6 — Ajustar e medir | PF-11/12 | Preferências efetivas/persistidas, laboratório isolado, fallback do telefone. Watch e medições físicas registrados separadamente | PENDENTE |
| F7 — Consolidação do produto | Revisão das 12 áreas e 12 fluxos, acessibilidade, atualização de dados antigos, APK de entrega | Checklist completo de ações, screenshots reais, vídeos dos percursos, testes e release identificado; parecer independente sem autoaprovação | PENDENTE |

Dependências transversais: PF-10 só entra em entrega ao usuário após integridade comprovada; referências PF-07 precisam existir antes de oferecer seus exercícios em PF-02; evolução depende de tentativas reais; Professor depende das métricas/contexto corretos. Ausência de hardware não bloqueia trabalho de software independente, mas não fecha a parte física.

## Protocolo de tarefa, prova e passagem de bastão

Para cada subitem criar ticket `PF-XX.n` no inventário: problema observado, PNG/painel correspondente, origem/ação/destino, dados lidos/escritos, causa/chamador existente, mudança mínima, migração, dependências de auditoria, teste discriminante e critério de aceite. Estados: NÃO_INICIADO, EM_ANDAMENTO, PARCIAL, VALIDADO_SOFTWARE, PENDENTE_DISPOSITIVO, ENTREGUE. Separar estado de software do gate físico quando ambos existirem.

Ao terminar cada onda, registrar `docs/product-delivery/Fn/`: SHA, comandos/saídas, nomes reais dos testes, screenshots comparativas e vídeo curto de percurso com dados criados no aplicativo, limites e pendências. Atualizar PROJECT_STATE e a matriz de ações. Não copiar screenshot do mockup como prova; não fechar linha com clique que termina em placeholder.

Testar unidade onde houver regra/durabilidade e instrumentação para navegação/estado/captura. Rodar testes relevantes, suíte completa, assembleDebug e lint; executar o runner de auditoria vigente, incluindo novas contraprovas. Não fixar contagens esperadas como definição de sucesso. Na entrega, documentar versão/SHA/hash do APK, assinatura e instalação/upgrade preservando dados, sem expor segredos.

**Cenário final obrigatório:** instalação limpa → criar caderno/página → escolher estilo/aula/duração → praticar e receber feedback → concluir sessão → comparar com segunda tentativa → favoritar variante → praticar com estilo pessoal → seguir orientação → copiar texto → salvar assinatura → exportar → criar/restaurar backup → verificar preferências e dados após reinício. Realizar também com acervo anterior, sem reset do usuário.

O desenho visual segue papel claro, azul-marinho e ações azuis dos PNGs, com texto e números corrigidos pelo guia. Busca, duplicação e preferências descritas neste plano agora são trabalho autorizado; ícones incidentais de Perfil/Explorar, decorações e números de exemplo não criam novas funcionalidades. Entregue primeiro um percurso útil completo por onda e, ao final, todas as áreas listadas.
