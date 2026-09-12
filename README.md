# Scribe — Caligrafia com S Pen / Stylus

> **Status:** Pedra fundamental — documentação inicial pronta; implementação ainda não iniciada.

Scribe é um aplicativo Android para prática de caligrafia diretamente na tela com S Pen ou stylus. O diferencial é preservar a escrita como **strokes vetoriais temporais**, permitindo replay, comparação, Ghost Mode, estilos pedagógicos e, futuramente, um professor de IA.

## Visão

**Treinar → escrever → rever → comparar → evoluir.**

O produto deve ensinar movimento e consistência, não apenas reproduzir fontes visuais.

## Princípios

1. O stroke original nunca é descartado.
2. O núcleo funciona offline.
3. IA é uma camada futura, não dependência do treino básico.
4. Fontes TTF/OTF e estilos pedagógicos `ScribeStyle` são conceitos diferentes.
5. Pressão, tilt e orientação só são usados quando o hardware realmente fornece os dados.
6. O domínio não deve depender de SDK proprietário da Samsung.
7. A avaliação deve orientar, não punir.
8. A escrita pessoal do usuário é um objetivo válido.

## Primeira meta técnica — M0 Stylus Lab

Antes de telas finais, cursos ou IA, provar:
- captura confiável de stylus;
- pontos e timestamps;
- pressure/tilt/orientation quando disponíveis;
- distinção entre caneta, dedo e borracha;
- palm rejection comportamental;
- persistência e reabertura de strokes;
- replay com ordem e timing relativos.

## Ordem de leitura para agentes

`README.md` → `PRODUCT_SPEC.md` → `ROADMAP.md` → `ARCHITECTURE.md` → `STYLUS_ENGINE.md` → `PROJECT_STATE.md` → `BACKLOG.md` → `AGENTS.md`.

## Estado atual

A documentação v0.1 é a pedra fundamental do projeto. Quando o desenvolvimento começar, a primeira tarefa é **SCR-001 — Bootstrap Android**, seguida de **SCR-002 — Device Capability Inspector**. Não avançar além do M0 até o Gate M0 ser aprovado.
