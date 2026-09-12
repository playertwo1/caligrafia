# DATA_MODEL

## Entidades
`PracticeSession`: sessão, início/fim, estilo, exercício, duração, notas.
`InkPage`: página/canvas, dimensões e guias.
`StrokeEntity`: metadados do stroke + referência aos pontos.
`Exercise`: alvo pedagógico, glyphs/texto, dificuldade.
`StylePack`: nome, versão, origem e licença.
`GlyphAttempt`: tentativa de glyph e strokes associados.
`PersonalGlyph`: variante favorita e versão.
`ProgressSnapshot`: snapshot de evolução por período.

## Pontos
Comparar no M0/M1: tabela point-by-point, blob binário compactado e arquivo binário por page/stroke com índice no Room. Escolher por benchmark de escrita, leitura, tamanho e migração.

## Migração
Toda estrutura persistente deve possuir `schemaVersion`.
