# PROJECT — Scribe (Caligrafia)

Contrato de visão. Atualize quando o projeto nasce e quando a visão muda; o resto (marcos, estado) fica em `ROADMAP.md` e `STATE.md`. Resumo de [`docs/PRODUCT_SPEC.md`](docs/PRODUCT_SPEC.md) e [`docs/SECURITY_PRIVACY.md`](docs/SECURITY_PRIVACY.md).

## Problema
Aplicativos de notas permitem escrever, mas não ensinam caligrafia; aplicativos de caligrafia tratam a escrita como imagem final e não preservam o movimento real da caneta.

## Para quem
Usuários Android com stylus/S Pen (aparelho-alvo: Samsung Galaxy S25 Ultra) que querem melhorar letra cursiva, legibilidade, fluidez ou aprender estilos específicos.

## MVP — até três fluxos que entregam valor
1. Escrever no caderno com stylus, linhas-guia, undo/redo e borracha, salvando strokes vetoriais com timestamps.
2. Treino guiado com referência visual e Ghost Mode, com estilos pedagógicos e histórico.
3. Rever a evolução: replay, comparação entre sessões e before/after.

## Fora do MVP (escrito para não voltar escondido)
- Professor IA automático; score absoluto de qualidade.
- Nuvem obrigatória; marketplace.
- Autenticação/antifraude por assinatura.

## Restrições
- Plataforma/ambiente: Android nativo em Kotlin (minSdk 26, compileSdk 35), Jetpack Compose nas telas e superfície de escrita nativa; sem WebView no canvas; domínio não acoplado ao SDK Samsung (adapters).
- Dados sensíveis e privacidade: local-first; a escrita pode conter nomes e assinaturas; sem upload automático; backup e export só por ação explícita; logs sem conteúdo escrito.
- Custos, licenças, integrações obrigatórias: Android Ink API avaliada primeiro; SDKs Samsung só quando oficialmente compatíveis.

## Como saberemos que funcionou
Percursos F0–F7 do `ROADMAP.md` funcionando de ponta a ponta com evidência por ID, e validação física no S25 Ultra (hoje `PENDENTE_DISPOSITIVO`).

## Autoridade de produto
O usuário (dono do repositório) decide requisitos; Codex organiza e verifica; Antigravity executa (`AGENTS.md`).

## Alternativas consideradas
Compose Canvas ou View customizada como alternativa à Android Ink API ([`docs/STYLUS_ENGINE.md`](docs/STYLUS_ENGINE.md), [`docs/REFERENCES.md`](docs/REFERENCES.md)).
