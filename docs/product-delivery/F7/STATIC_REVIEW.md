# F7 — Revisão estática de consolidação

**Branch:** `phase-f6-f7-finalization`  
**Natureza desta revisão:** inspeção de código/documentação; não substitui build, testes instrumentados, screenshots reais ou bancada física.

## Resultado executivo

F7 foi avançada até o limite verificável sem executar Gradle e sem S25 Ultra/Watch disponíveis neste runtime. O produto possui as quatro abas canônicas e os destinos do menu Mais; F0–F4 têm evidências históricas executadas, F5 possui implementação em branch ainda aguardando validação local, e F6 recebeu endurecimentos de preferências/entrada/render/capabilities nesta branch. **F7.G permanece aberto.**

## F7.01 — ROUTE_ACTION_MATRIX

A matriz existente contém as rotas NAV-01..NAV-04, BK-01..BK-05, GD-01..GD-04, EV-01..EV-02 e EX-01..EX-06. A inspeção encontrou uma lacuna documental: as ações introduzidas/amadurecidas em F5 e F6 ainda não estão enumeradas individualmente, incluindo pelo menos:

- escolha explícita da fonte da assinatura e exportação SVG/PNG;
- exportação de página com variantes de fundo;
- exportação, seleção, revisão, confirmação e restauração de `.scribepack`;
- preferências de mão/lado da barra/modo de entrada/escala/contraste/animação/curva/pausa/vibração;
- status Watch, teste háptico e fallback no telefone;
- Laboratório: salvar/reabrir/replay/capabilities e futuro controle explícito gravar/parar.

**Estado:** `PARCIAL`. Antes de fechar F7.01, ampliar `ROUTE_ACTION_MATRIX.md` e verificar para cada linha handler, argumentos, estado, persistência e teste.

## F7.02 — 12 áreas + 12 fluxos

O menu Mais atual expõe Alfabeto, Professor, Estilos, Assinaturas, Cópia de textos, Backup, S Pen & Watch, Laboratório e Preferências; as quatro abas canônicas são Caderno, Praticar, Evolução e Mais. O guia `docs/design/fluxos-v1/README.md` continua sendo a referência das 12 pranchas complementares.

**Estado:** `PARCIAL_ESTATICO`. A presença estrutural dos destinos foi inspecionada, porém a correspondência visual/funcional de cada estado das 12 pranchas exige execução real e screenshots.

## F7.03–F7.05 — visual, S25 Ultra e acessibilidade

Não há como produzir screenshot real do app ou validar fonte ampliada, insets, teclado, alcance da S Pen, ordem de foco, TalkBack, contraste percebido e alvos físicos de toque neste runtime. `contentDescription` existe em diversos controles principais, mas isso não prova cobertura completa.

**Estado:** `PENDENTE_DISPOSITIVO` / `PENDENTE_TESTE_UI`.

## F7.06–F7.08 — jornadas e upgrade

As duas jornadas obrigatórias devem ser executadas integralmente no mesmo SHA:

`caderno → aula → treino → feedback → resumo → comparação → variante → estilo pessoal`

`Professor → treino prescrito → cópia → assinatura → export → backup → restore → reinício`

Também é obrigatório repetir casos relevantes sobre instalação atualizada com acervo anterior, sem limpar dados.

**Estado:** `PENDENTE_EXECUCAO_LOCAL`.

## F7.09 — suíte, auditoria, build e lint

Não executado nesta sessão. O GitHub Actions está sem cota mensal e o container desta sessão não consegue resolver o proxy/rede necessário para clonar o repositório e executar Gradle. Foi preparado um runner local separado em `docs/product-delivery/run-local-f5-f7-validation.ps1`.

**Estado:** `PENDENTE_EXECUCAO_LOCAL`.

## F7.10 — APK de entrega

Nenhum APK final foi certificado nesta sessão. Debug APK não deve ser promovido como APK final. A entrega exige versão/SHA/hash, assinatura de release fora do repositório, instalação limpa e atualização preservando acervo. Nenhum segredo/keystore deve ser commitado.

**Estado:** `PENDENTE_RELEASE_LOCAL`.

## F7.11 — estado do projeto

Foi criado `docs/product-delivery/CURRENT_STATE_F5_F7.md` como suplemento de estado desta branch porque o topo histórico de `PROJECT_STATE.md` ainda contém texto obsoleto sobre F0/F5. A atualização definitiva do `PROJECT_STATE.md` deve ocorrer após o run local, usando resultados reais e o SHA efetivamente testado.

**Estado:** `PARCIAL`; não reescrever resultado histórico antes da validação.

## F7.12 — índice de evidências

Esta revisão, a matriz F6, o prompt de validação local e o runner local formam o índice de passagem de bastão. Logs/XMLs/screenshots/vídeos físicos ainda precisam ser anexados após execução.

**Estado:** `PARCIAL`.

## Gate F7.G

**ABERTO.** Codex não registra parecer final enquanto F5.G/F6.G, execução local F7.09, jornadas, upgrade, acessibilidade, hardware e APK de entrega não tiverem evidência correspondente. Não converter a branch em entrega aprovada apenas por revisão estática.
