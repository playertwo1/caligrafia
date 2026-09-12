# ARCHITECTURE — Scribe

## Camadas
UI/Compose → Presentation/ViewModels → Domain Use Cases → Repositories → Room/files/style packs/future AI.

O motor de ink deve ser isolado da UI de produto.

## Módulos sugeridos
`app`, `core:model`, `core:database`, `core:designsystem`, `feature:home`, `feature:practice`, `feature:styles`, `feature:progress`, `feature:alphabet`, `ink:engine`, `ink:renderer`, `style:engine`, `learning:engine`.

No primeiro protótipo pode existir um único módulo, preservando as fronteiras por packages.

## Ink Engine
Ingestão de MotionEvent; normalização; StrokePoint/Stroke; resampling opcional; smoothing visual derivado; rendering; replay. Raw points nunca são substituídos pelos suavizados.

## UI
Jetpack Compose para navegação/telas. Canvas pode usar Compose Canvas ou Android View customizada. A decisão é por benchmark de latência/estabilidade no M0.

## Persistência
Room para entidades relacionais. Pontos podem usar tabelas, blobs compactados ou arquivos associados. A escolha depende do benchmark SCR-006.

## Estado
ViewModel + StateFlow. Eventos de ink de alta frequência não devem recompor a tela inteira.

## Testes
Unitários, instrumentados, golden/screenshot e benchmarks de latência/memória.

## Compatibilidade
APIs Android padrão de stylus são a fundação. Recursos Samsung ficam atrás de adapters opcionais.
