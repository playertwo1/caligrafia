# Scribe — Caligrafia com S Pen / Stylus

> **Status:** MVP Android existente; prioridade atual é entregar os percursos funcionais dos mockups. Consulte o [roadmap executável para o Antigravity](docs/ANTIGRAVITY_FUNCTIONAL_ROADMAP.md). Funcionalidade existente não equivale a aprovação integral dos gates históricos.

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

## Direção técnica aprovada

Aparelho-alvo: Samsung Galaxy S25 Ultra com S Pen original. Usar telas em Jetpack Compose e superfície de escrita nativa, avaliando primeiro a Android Ink API no M0. A escolha final depende de benchmark no aparelho e da preservação dos dados brutos, persistência e replay; Compose Canvas ou View customizada são alternativas. SDKs Samsung serão usados quando oficialmente compatíveis e aplicáveis, atrás de adapters.

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

`STATE.md` → `AGENTS.md` → `ROADMAP.md` (marco ativo) → `PROJECT.md`; abrir `docs/PRODUCT_SPEC.md`, `docs/ARCHITECTURE.md`, `docs/STYLUS_ENGINE.md` e `docs/BACKLOG.md` quando a tarefa pedir.

## Como rodar
```bash
# Requisitos: JDK 17 e Android SDK (compileSdk 35, minSdk 26)
./gradlew assembleDebug     # APK em app/build/outputs/apk/debug/
./gradlew installDebug      # instala no aparelho conectado
```

## Como verificar
```bash
./gradlew testDebugUnitTest assembleDebug
```
A CI (`.github/workflows/build-release.yml`) roda `testDebugUnitTest` e `assembleRelease assembleDebug`. `scripts/watchdog.ps1` faz verificações auxiliares de regras.

## Documentação
| Pergunta | Arquivo |
|---|---|
| Por que existe, MVP, fora de escopo | [PROJECT.md](PROJECT.md) · detalhe em [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) |
| Onde paramos, próxima ação | [STATE.md](STATE.md) |
| Marcos e fila | [ROADMAP.md](ROADMAP.md) · [checklist F0–F7](docs/ANTIGRAVITY_EXECUTION_CHECKLIST.md) |
| Regras para agentes | [AGENTS.md](AGENTS.md) (`CLAUDE.md` só importa este), [WATCHDOG.md](WATCHDOG.md), [docs/AUDIT.md](docs/AUDIT.md) |
| Arquitetura e motores | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/STYLUS_ENGINE.md](docs/STYLUS_ENGINE.md), [docs/STYLE_ENGINE.md](docs/STYLE_ENGINE.md), [LEARNING_SYSTEM.md](LEARNING_SYSTEM.md), [docs/DATA_MODEL.md](docs/DATA_MODEL.md) |
| UX, privacidade, referências | [docs/UI_UX.md](docs/UI_UX.md), [docs/SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md), [docs/REFERENCES.md](docs/REFERENCES.md) |
| Relatórios de auditoria | `docs/audit-reports/`, `docs/audit*/` |
| Histórico de mudanças | [CHANGELOG.md](CHANGELOG.md) |
| Histórico (não editar) | `docs/archive/` |

## Estado atual

Ver [STATE.md](STATE.md). Histórico: a documentação v0.1 foi a pedra fundamental do projeto, começando por **SCR-001 — Bootstrap Android** e **SCR-002 — Device Capability Inspector**; o app já passou pelos marcos M0–M8 e pelas fases funcionais F0–F4.
