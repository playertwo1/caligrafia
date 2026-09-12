# STYLE_ENGINE

## Princípio
Uma fonte visual não é automaticamente um modelo pedagógico.

## VisualFont
TTF/OTF usada para mostrar texto de referência. Pode não conter informação sobre ordem ou movimento dos traços.

## ScribeStyle
Pacote pedagógico com glyphs, métricas, guias e stroke models.

## ScribeStyle v1 — conceito
```json
{
  "id": "business_penmanship_v1",
  "name": "Business Penmanship",
  "version": 1,
  "metrics": {"slantDegrees": 55, "xHeight": 1.0, "ascender": 1.8, "descender": 0.8},
  "glyphs": {
    "a": {"referencePath": "glyphs/a.svg", "strokeModel": "strokes/a.json", "connections": ["entry", "exit"]}
  }
}
```

## Biblioteca inicial sugerida
Cursiva prática/escolar; Itálica legível; Business Penmanship inspirada em princípios históricos e ativos adequadamente licenciados.

## Ghost Mode
Trace 100%; Assistido 70%; Leve 40%; Hint 10%; Livre 0%.

## TTF/OTF
Importar e validar arquivo local; usar somente para render de referência; informar quando não há instruções de traço.

## Meu Alfabeto
Salvar glyphs escolhidos pelo usuário como referências pessoais e futuramente gerar um `PersonalStyle` local.
