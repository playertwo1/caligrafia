# STYLUS_ENGINE

## Objetivo
Capturar a escrita como movimento vetorial de alta fidelidade usando Android `MotionEvent`.

## Dados por amostra
x/y local, timestamp relativo, pressure, orientation, tilt (`AXIS_TILT`) quando suportado, toolType e pointerId. Dados não fornecidos pelo hardware ficam `null`.

## Histórico
Consumir historical samples de MOVE quando disponíveis para evitar perda de pontos entre frames.

## Modelo inicial
```kotlin
data class StrokePoint(
    val x: Float,
    val y: Float,
    val tMs: Long,
    val pressure: Float?,
    val tiltRad: Float?,
    val orientationRad: Float?,
)

data class Stroke(
    val id: String,
    val tool: ToolType,
    val points: List<StrokePoint>,
    val startedAtMs: Long,
    val endedAtMs: Long,
)
```

## Raw vs Render
Raw points são imutáveis. Renderer pode criar spline/smoothing derivado. Export de imagem nunca substitui stroke data.

## Palm rejection
Quando stylus está ativo/hovering conforme eventos disponíveis, ignorar finger input no canvas. Multi-touch para pan/zoom somente sob regra explícita. Testar no aparelho-alvo.

## Eraser
Suportar `TOOL_TYPE_ERASER` quando identificável. MVP pode usar borracha por stroke.

## Replay
Reproduzir pelos deltas de timestamps em 0.5x, 1x e 2x.

## Gate M0
Nenhuma quebra em escrita normal; reload sem mudança geométrica relevante; replay mantém ordem/timing; dados ausentes não são inventados; finger não produz tinta em stylus-only.

## Direção do renderer no M0

Avaliar primeiro a Android Ink API em superfície nativa no Galaxy S25 Ultra com S Pen original. Manter o modelo de pontos brutos e timestamps independente da biblioteca; validar que a integração preserva amostras históricas, ordem e timing para persistência e replay. Smoothing e eventuais pontos previstos são apenas derivados visuais e não substituem amostras reais. Comparar latência e estabilidade com uma referência em Compose Canvas ou View customizada; documentar a escolha e recorrer à alternativa se necessário. Telas permanecem em Jetpack Compose.
