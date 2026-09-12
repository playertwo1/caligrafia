# ROADMAP — Scribe

## M0 — Stylus Lab
Objetivo: eliminar o maior risco técnico antes de construir o produto.

**Aparelho-alvo:** Samsung Galaxy S25 Ultra com a S Pen fornecida com o aparelho. A validação de escrita e os benchmarks do M0 devem ser realizados nesse dispositivo.

**Integração Samsung:** por preferência do usuário, utilizar SDKs Samsung para recursos de escrita/S Pen quando houver suporte oficial ao S25 Ultra e benefício aplicável. No M0, identificar o SDK e sua versão, verificar disponibilidade/licença e compatibilidade, e integrar os recursos suportados por adapter. Caso não exista SDK aplicável, registrar a limitação e manter a captura por `MotionEvent`; não adicionar dependência sem função utilizável. O S Pen Remote SDK citado nas referências trata de controle remoto; a S Pen do S25 Ultra não possui Bluetooth nem suporta funções remotas, portanto ele não deve ser adotado como motor de tinta.

**Canvas — direção aprovada:** usar superfície nativa e avaliar primeiro a Android Ink API como solução de escrita de baixa latência, com telas em Jetpack Compose. No M0, validar integração, preservação dos pontos brutos/timestamps, persistência e replay no S25 Ultra. Comparar com uma implementação de referência em Compose Canvas ou Android View customizada; recorrer a essas alternativas se a Ink API não atender aos critérios. Registrar medições e decisão antes do Gate M0. Não usar WebView na superfície de escrita.

Entregas: captura `ACTION_DOWN/MOVE/UP/CANCEL`, historical samples, tool type, x/y/timestamps, pressure/orientation/tilt quando disponíveis, rendering em tempo real, palm rejection comportamental, persistência e replay 0.5x/1x/2x.

**Gate M0:** não avançar se houver perda perceptível de pontos, atraso de tinta inaceitável, strokes quebrados ou replay inconsistente.

Registrar também o renderer escolhido, as medições no S25 Ultra e os recursos Samsung integrados ou indisponíveis, com a justificativa de compatibilidade.

## M1 — Caderno
Páginas/sessões, guias baseline/x-height/ascender/descender, espessura, undo/redo, borracha por stroke, salvar/restaurar e export PNG sem substituir dados vetoriais.

## M2 — Treino Guiado
Glyph de referência, Ghost Mode 100/70/40/10/0%, trace → copiar → sozinho, exercícios por letra e feedback determinístico.

## M3 — Style Engine
`ScribeStyle v1`, três estilos pedagógicos iniciais, importação TTF/OTF como referência visual, preview e fallback de glyph.

## M4 — Learning System
Currículo: traços → famílias → letras → conexões → palavras → frases; sessões de 5/10/15/20 min; histórico e revisão por regras locais.

## M5 — Evolução
Before/after, overlay, replay lado a lado, calendário, tempo praticado e indicadores não punitivos.

## M6 — Meu Alfabeto
Salvar melhor tentativa por glyph, variantes favoritas, versões v1/v2/v3 e PersonalStyle local.

## M7 — Professor IA
Somente após volume suficiente de sessões reais: interpretar padrões, recomendar foco e gerar próximo treino sem inventar métricas.

## M8 — Expansões
Galaxy Watch para timer/haptics, backup/sync opcionais, pacotes de estilos, laboratório de assinatura, modo copiar textos, acessibilidade e telas maiores.
