# PRODUCT_SPEC — Scribe v0.1

## Problema
Aplicativos de notas permitem escrever, mas normalmente não ensinam caligrafia. Aplicativos de caligrafia frequentemente tratam a escrita como imagem final e não preservam o movimento real da caneta. O Scribe une prática digital, progressão pedagógica e histórico de strokes.

## Público inicial
Usuários Android com stylus/S Pen que querem melhorar letra cursiva, legibilidade, fluidez ou aprender estilos específicos.

## Jobs to be done
- Treinar caligrafia sem imprimir folhas.
- Escolher um estilo e praticar seguindo referências.
- Ver a evolução da escrita.
- Rever como um traço foi feito, não apenas a imagem final.
- Reduzir progressivamente a assistência.
- Criar uma escrita pessoal consistente.

## Áreas do produto
**Hoje:** treino recomendado, duração, continuidade e evolução rápida.
**Praticar:** letras, conexões, palavras, frases e escrita livre.
**Estilos:** estilos pedagógicos e fontes TTF/OTF como referência visual.
**Evolução:** sessões, before/after, overlay e replay.
**Meu Alfabeto:** construção gradual da escrita pessoal.

## MVP obrigatório
1. Canvas de escrita com stylus.
2. Linhas-guia configuráveis.
3. Undo/redo.
4. Borracha.
5. Salvar/reabrir sessão.
6. Strokes vetoriais + timestamps.
7. Replay.
8. Exercícios com referência visual.
9. Ghost Mode.
10. Biblioteca inicial de estilos.
11. Histórico.
12. Comparação entre sessões.

## Fora do MVP
Professor IA automático; score absoluto de qualidade; nuvem obrigatória; marketplace; autenticação por assinatura.

## Regra central
O bitmap é uma representação derivada. O stroke vetorial bruto é a fonte primária e nunca deve ser destruído por smoothing, export ou análise.
