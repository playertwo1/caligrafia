# Scribe — Caligrafia com S Pen / Stylus

Scribe é um aplicativo Android local-first para prática e evolução da caligrafia diretamente na tela com S Pen ou stylus. O núcleo preserva a escrita como **strokes vetoriais temporais**, permitindo replay, comparação, treino guiado, estilos pessoais e feedback baseado em dados reais.

## Visão
**Treinar → escrever → rever → comparar → evoluir.**

O produto busca ensinar movimento e consistência, não apenas reproduzir fontes visuais.

## Princípios do produto
1. O stroke original nunca é descartado ou substituído por bitmap.
2. O núcleo funciona offline.
3. IA/backend/cloud não são dependências do treino básico e exigem autorização explícita quando adicionados.
4. Fontes TTF/OTF e estilos pedagógicos `ScribeStyle` são conceitos diferentes.
5. Pressure, tilt e orientation só são usados quando o hardware realmente os fornece.
6. O domínio não depende diretamente de SDK proprietário; integrações específicas usam adapters.
7. A avaliação deve orientar, não punir.
8. A escrita pessoal do usuário é um objetivo válido.
9. Dados existentes precisam sobreviver a atualização, backup e restore ou a operação deve falhar de forma recuperável.

## Plataforma
- Kotlin nativo;
- Jetpack Compose para telas;
- superfície de escrita Android nativa;
- aparelho-alvo principal para validações físicas: Samsung Galaxy S25 Ultra com S Pen original;
- raw strokes preservados como fonte de verdade.

## Estado atual
O README **não mantém status dinâmico**. Para saber exatamente branch, fase, gates, bloqueios e próxima ação, leia:

1. `PROJECT_STATE.md` — resume card operacional;
2. `PHASE_CURRENT.md` — autorização e critérios de aceite vigentes.

Relatórios históricos ou contagens antigas de testes não substituem essas fontes.

## Ordem de leitura para agentes
Bootstrap mínimo:

`AGENTS.md` → `PROJECT_STATE.md` → pedido atual.

Depois use `AI_CONTEXT_INDEX.md` / `context-manifest.json` para carregar apenas o contexto necessário. Não leia todo o roadmap ou todas as auditorias por padrão.

## Contratos principais
- produto: `PRODUCT_SPEC.md`;
- arquitetura: `ARCHITECTURE.md`;
- motor de escrita: `STYLUS_ENGINE.md`;
- estilos: `STYLE_ENGINE.md`;
- aprendizado: `LEARNING_SYSTEM.md`;
- dados: `DATA_MODEL.md` e `docs/product-delivery/DATA_CONTRACT.md`;
- segurança: `SECURITY_PRIVACY.md`;
- evolução: `ROADMAP.md`;
- governança: `AGENTS.md`, `INVARIANTS.yaml`, `AUDIT.md`, `WATCHDOG.md`.

## Referências visuais
As 13 imagens em `docs/design/` são referências legítimas de produto. Elas orientam composição e fluxo, mas não são evidência de implementação funcionando.

## Ideias Standard
Este repositório adota o Ideias Standard em perfil **DEEP** com packs `android`, `multi-agent` e `sensitive-data`. O contrato está em `.idea-standard/project-manifest.json` e `.idea-standard/standard.lock`.

**Regra de governança:** implementação não equivale a aprovação; `NOT_RUN != PASS`.