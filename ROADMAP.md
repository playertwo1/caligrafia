> **Revisão independente vigente:** fechamento M0–M8 não aprovado na v0.8.0. Os status de conclusão históricos abaixo são alegações do implementador. Ver [auditoria V3](docs/ANTIGRAVITY_AUDIT_REVIEW_V3.md) e [referências visuais com instruções](docs/design/fluxos-v1/README.md). A construção visual deve seguir as ondas de estabilização.
# ROADMAP — Scribe

> **Ordem de trabalho do Antigravity:** abrir o [checklist de execução](docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md), começar pela primeira caixa desmarcada e registrar evidência por ID. As decisões de produto estão fixadas ali; o roadmap funcional é a especificação complementar. Não declarar aprovação independente ao marcar caixas de implementação.

## Prioridade vigente — entrega funcional dos mockups (2026-09-13)

O usuário autorizou explicitamente transformar as 12 áreas do painel e os 12 fluxos complementares em funcionalidades reais. O MVP existente é o ponto de partida. Os marcos históricos abaixo não significam entrega comprovada de todos esses percursos.

**Plano detalhado obrigatório para o Antigravity:** [dos mockups ao Scribe funcional](docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md). Ele contém navegação, escopo por tela PF-01–PF-12, comportamento/persistência, critérios de aceite, dependências e protocolo de evidência. Esta autorização substitui a limitação anterior a ajustes visuais e libera as opções explicitamente enumeradas no plano; auditorias e preservação de dados continuam válidas.

| Ordem | Entrega | Resultado que deve funcionar |
|---|---|---|
| F0 | Integridade e mapa de ações | Resolver contraprovas V5 de manifesto/cache; registrar IDs, dados e rotas |
| F1 | Navegação, biblioteca, editor e páginas | Encontrar as áreas, organizar cadernos e escrever com ferramentas completas |
| F2 | Aulas, objetivos, treino e feedback | Escolher lição/estilo/duração e concluir sessão real ligada ao histórico/SRS |
| F3 | Evolução, replay, variantes e estilos | Rever, comparar, favoritar e usar a própria escrita como referência |
| F4 | Professor explicável e cópia de textos | Seguir orientação até o resultado e copiar o texto realmente selecionado |
| F5 | Assinaturas, exportação e backup | Salvar referência, levar arquivos para fora do app e restaurar dados |
| F6 | Preferências, S Pen/Watch e laboratório | Ajustes persistentes com efeito real, diagnóstico e fallback explícito |
| F7 | Consolidação e entrega | Percursos completos, acessibilidade, preservação de acervo e APK verificável |

**Entrada de Praticar:** Hub de aulas com objetivo, categorias e duração; não abrir invariavelmente um traço `/`. **Mais:** destinos nomeados para Alfabeto, Professor, Estilos, Assinaturas, Cópia de textos, Backup, S Pen/Watch, Laboratório e Preferências.

Não é necessário esperar a aprovação física de todos os marcos para desenvolver um fluxo independente. Corrigir os bloqueadores do fluxo antes de entregá-lo; só fechar cada onda com percurso real e evidência. O Antigravity deve atualizar PROJECT_STATE e `docs/product-delivery/ROUTE_ACTION_MATRIX.md` por tarefa, sem marcar o produto inteiro como concluído por testes unitários verdes.

## M0 — Stylus Lab [CONCLUÍDO — v0.1.0]
Objetivo: eliminar o maior risco técnico antes de construir o produto.

**Aparelho-alvo:** Samsung Galaxy S25 Ultra com a S Pen fornecida com o aparelho. A validação de escrita e os benchmarks do M0 devem ser realizados nesse dispositivo.

**Integração Samsung:** por preferência do usuário, utilizar SDKs Samsung para recursos de escrita/S Pen quando houver suporte oficial ao S25 Ultra e benefício aplicável. No M0, identificar o SDK e sua versão, verificar disponibilidade/licença e compatibilidade, e integrar os recursos suportados por adapter. Caso não exista SDK aplicável, registrar a limitação e manter a captura por `MotionEvent`; não adicionar dependência sem função utilizável. O S Pen Remote SDK citado nas referências trata de controle remoto; a S Pen do S25 Ultra não possui Bluetooth nem suporta funções remotas, portanto ele não deve ser adotado como motor de tinta.

**Canvas — direção aprovada:** usar superfície nativa e avaliar primeiro a Android Ink API como solução de escrita de baixa latência, com telas em Jetpack Compose. No M0, validar integração, preservação dos pontos brutos/timestamps, persistência e replay no S25 Ultra. Comparar com uma implementação de referência em Compose Canvas ou Android View customizada; recorrer a essas alternativas se a Ink API não atender aos critérios. Registrar medições e decisão antes do Gate M0. Não usar WebView na superfície de escrita.

Entregas: captura `ACTION_DOWN/MOVE/UP/CANCEL`, historical samples, tool type, x/y/timestamps, pressure/orientation/tilt quando disponíveis, rendering em tempo real, palm rejection comportamental, persistência e replay 0.5x/1x/2x.

**Gate M0:** APROVADO. Resolução de probe defects da auditoria do Codex integrada.

## M1 — Caderno [CONCLUÍDO — v0.1.0 / v0.1.2]
Páginas/sessões, guias baseline/x-height/ascender/descender, espessura, undo/redo, borracha por stroke, salvar/restaurar e export PNG sem substituir dados vetoriais. Bloqueio de gestos de borda laterais do S25 Ultra.

## M2 — Treino Guiado [CONCLUÍDO — v0.2.0]
Glyph de referência, Ghost Mode 100/70/40/10/0%, trace → copiar → sozinho, exercícios por letra e feedback determinístico.

## M3 — Style Engine [CONCLUÍDO — v0.3.0]
`ScribeStyle v1`, três estilos pedagógicos iniciais (Cursiva Escolar, Copperplate, Spencerian), importação TTF/OTF como referência visual, preview e fallback de glyph.

## M4 — Learning System [CONCLUÍDO — v0.4.0]
Currículo canônico de 18 lições em 5 estágios (Traços, Famílias, Conexões, Palavras, Frases); sessões de 5/10/15/20 min com temporizador por 5 fases; histórico atômico local e revisão espaçada (SRS) 100% determinística sem nuvem/IA; tela `LearningHubScreen` e métricas não punitivas.

## M5 — Evolução [CONCLUÍDO — v0.5.0]
Before/after com deltas de precisão/inclinação/cadência (`AttemptComparator`), slider de sobreposição com cross-fade (`OverlaySlider`), replay lado a lado sincronizado (`DualReplayEngine`), calendário de consistência com tempo praticado e indicadores não punitivos (`CalendarConsistencyHelper`), repositório com persistência atômica segura `.scribe` (`LocalPracticeAttemptRepository`) e tela unificada `EvolutionScreen`.

## M6 — Meu Alfabeto [CONCLUÍDO — v0.6.0]
Catálogo de 68 glifos canônicos (`PersonalAlphabet`), versionamento de variantes (`GlyphVariant` v1, v2, v3...) com eleição de favoritas, repositório local atômico `.scribe` (`LocalPersonalAlphabetRepository`), motor de compilação de estilo pessoal (`PersonalStyleCompiler`), tela `AlphabetScreen` em Compose com grade adaptativa, preview vetorial e integração imediata com o `StyleEngine`.

## M7 — Professor IA & Coaching Inteligente [CONCLUÍDO — v0.7.0]
Motor biomecânico determinístico de 4 dimensões (`MotorDiagnosticEngine`: estabilidade angular $\theta$, contenção de pauta, ritmo/cadência motora e controle de pressão com a S Pen); gerador dinâmico de treino sob medida (`CoachingCurriculumGenerator`); motor de insights pedagógicos, correções e ergonomia em linguagem clara (`CoachingFeedbackEngine`); repositório atômico local (`LocalTeacherRepository`); tela Compose `TeacherScreen` com radar biomecânico e integração direta no caderno.

## M8 — Expansões e Refinamento do Produto [CONCLUÍDO — v0.8.0]
Sistema de backup e restauração atômica offline `.scribepack` (ZIP com manifesto JSON puro), Laboratório de Assinaturas e Monogramas com cálculo determinístico de repetibilidade e exportação vetorial SVG e PNG transparente, Modo Cópia de Textos Longos com pangramas e poesias clássicas em português (WPM caligráfico deliberado), três novos estilos pedagógicos expandidos (Gótica Textura Quadrata, Itálica Chanceleresca e Uncial Clássica), calibrador de curvas de resposta de pressão da S Pen, adaptador Wear OS / Galaxy Watch com alerta postural e haptics de timer, e tela central `ExpansionsScreen`.


## Referências visuais para construção da interface

Por solicitação do usuário, o projeto contém **13 imagens de referência**: um painel geral com 12 telas e 12 imagens complementares, cada uma com três estados de um fluxo.

- [Painel geral das telas](docs/design/scribe-visao-12-telas-v1.png).
- [Índice das 12 referências de fluxos e instruções para o Antigravity](docs/design/fluxos-v1/README.md).
- [Auditoria independente V3: achados, dependências e ondas de estabilização](docs/ANTIGRAVITY_AUDIT_REVIEW_V3.md).

**Direção visual:** papel claro, tinta azul-marinho, ações em azul, guias discretas, controles nativos e espaço amplo para S Pen no Galaxy S25 Ultra. Usar as imagens como base de composição e interação; consultar o guia antes de copiar textos, valores, navegação ou controles.

**Ordem de trabalho vigente:** seguir F0–F7 do [plano funcional](docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md). Cada ação enumerada precisa funcionar de ponta a ponta. Busca, duplicação e preferências descritas no plano agora estão autorizadas. Contas, cloud e funcionalidades incidentais não enumeradas continuam fora do escopo. As imagens não aprovam milestones nem substituem os critérios de aceite.

**Aceite visual por fluxo:** registrar qual imagem foi usada, implementar estados vazio/carregando/sucesso/erro pertinentes, verificar handlers e persistência, comparar screenshots reais com a referência e validar legibilidade, alvos de toque e área de escrita no aparelho. Registrar diferenças justificadas em PROJECT_STATE. Não usar as imagens geradas como evidência de aplicativo funcionando.

