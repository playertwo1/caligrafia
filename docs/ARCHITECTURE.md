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
Jetpack Compose para navegação/telas. Superfície de escrita nativa com Android Ink API como primeira opção a avaliar no M0, conforme direção aprovada pelo usuário. Comparar com uma referência em Compose Canvas ou Android View customizada no S25 Ultra; essas são alternativas se a Ink API não atender aos critérios. A adoção final depende de latência/estabilidade, integração com dados brutos, persistência e replay. A Ink API é uma biblioteca Android, distinta dos SDKs Samsung.

## Persistência
Room para entidades relacionais. Pontos podem usar tabelas, blobs compactados ou arquivos associados. A escolha depende do benchmark SCR-006.

## Estado
ViewModel + StateFlow. Eventos de ink de alta frequência não devem recompor a tela inteira.

## Testes
Unitários, instrumentados, golden/screenshot e benchmarks de latência/memória.

## Compatibilidade
Samsung Galaxy S25 Ultra com S Pen original é o aparelho-alvo. APIs Android padrão de stylus são a fundação de captura; por solicitação do usuário, utilizar SDKs Samsung oficialmente compatíveis para recursos aplicáveis, atrás de adapters. O M0 deve identificar SDK/versão e validar suporte real. A S Pen desse aparelho não tem Bluetooth nem funções remotas, portanto S Pen Remote SDK não é a fundação de tinta. Se nenhum SDK Samsung for aplicável, registrar essa limitação. Avaliar primeiro a Android Ink API e comparar com uma referência nativa no aparelho antes de fechar a escolha.
