package com.scribe.caligrafia.guided.catalog

import com.scribe.caligrafia.guided.model.DirectionalHint
import com.scribe.caligrafia.guided.model.GlyphCategory
import com.scribe.caligrafia.guided.model.ReferenceGlyph
import com.scribe.caligrafia.guided.model.ReferencePoint
import com.scribe.caligrafia.guided.model.ReferenceStroke

/**
 * Catálogo canônico de exercícios pedagógicos caligráficos do Milestone M2.
 *
 * Fornece traços fundamentais e letras cursivas com coordenadas normalizadas
 * rigorosamente alinhadas à linha de base (y=0.0), altura-x (y=1.0) e ascendentes (y=2.0).
 */
object ReferenceGlyphCatalog {

    /**
     * Gera pontos intermediários suaves ao longo de uma curva quadrática de Bézier.
     */
    private fun sampleQuad(
        p0: Pair<Float, Float>,
        p1: Pair<Float, Float>,
        p2: Pair<Float, Float>,
        steps: Int = 12
    ): List<ReferencePoint> {
        val points = mutableListOf<ReferencePoint>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val inv = 1.0f - t
            val x = inv * inv * p0.first + 2 * inv * t * p1.first + t * t * p2.first
            val y = inv * inv * p0.second + 2 * inv * t * p1.second + t * t * p2.second
            points.add(ReferencePoint(x, y))
        }
        return points
    }

    /**
     * Interpola linearmente entre dois pontos normalizados.
     */
    private fun sampleLine(
        p0: Pair<Float, Float>,
        p1: Pair<Float, Float>,
        steps: Int = 10
    ): List<ReferencePoint> {
        val points = mutableListOf<ReferencePoint>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = p0.first + t * (p1.first - p0.first)
            val y = p0.second + t * (p1.second - p0.second)
            points.add(ReferencePoint(x, y))
        }
        return points
    }

    // --- 1. TRAÇOS BÁSICOS FUNDAMENTAIS ---

    val BASIC_SLANT = ReferenceGlyph(
        id = "basic_slant",
        symbol = "/",
        name = "Traço Inclinado Descendente",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Posicione a pena na linha da altura-x e puxe em linha reta até a linha de base com inclinação uniforme.",
        widthToXHeightRatio = 0.8f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = sampleLine(Pair(0.9883f, 1.0f), Pair(0.0117f, 0.0f), steps = 15),
                hint = DirectionalHint(1, ReferencePoint(0.9883f, 1.0f), -0.78f, -1.0f, "Puxe para baixo")
            )
        )
    )

    val BASIC_UNDERTURN = ReferenceGlyph(
        id = "basic_underturn",
        symbol = "∪",
        name = "Curva Inferior (Underturn)",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Desça em linha reta na inclinação da pauta, faça uma curva suave encostando na linha de base e suba até a altura-x.",
        widthToXHeightRatio = 1.0f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Descida reta
                    addAll(sampleLine(Pair(0.3f, 1.0f), Pair(0.3f, 0.25f), steps = 8))
                    // Arco inferior na linha de base
                    addAll(sampleQuad(Pair(0.3f, 0.25f), Pair(0.32f, 0.0f), Pair(0.55f, 0.0f), steps = 8))
                    // Subida até a altura-x
                    addAll(sampleQuad(Pair(0.55f, 0.0f), Pair(0.78f, 0.05f), Pair(0.8f, 1.0f), steps = 8))
                },
                hint = DirectionalHint(1, ReferencePoint(0.3f, 1.0f), 0.0f, -1.0f, "Desça e faça a curva")
            )
        )
    )

    val BASIC_OVERTURN = ReferenceGlyph(
        id = "basic_overturn",
        symbol = "∩",
        name = "Curva Superior (Overturn)",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Inicie na linha de base, suba curvando no teto da altura-x e desça reto na inclinação correta.",
        widthToXHeightRatio = 1.0f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Subida curva
                    addAll(sampleQuad(Pair(0.2f, 0.0f), Pair(0.22f, 0.95f), Pair(0.45f, 1.0f), steps = 8))
                    // Arco superior
                    addAll(sampleQuad(Pair(0.45f, 1.0f), Pair(0.68f, 1.0f), Pair(0.7f, 0.75f), steps = 8))
                    // Descida reta
                    addAll(sampleLine(Pair(0.7f, 0.75f), Pair(0.7f, 0.0f), steps = 8))
                },
                hint = DirectionalHint(1, ReferencePoint(0.2f, 0.0f), 0.0f, 1.0f, "Suba e curve no teto")
            )
        )
    )

    val BASIC_COMPOUND = ReferenceGlyph(
        id = "basic_compound",
        symbol = "∼",
        name = "Curva Composta",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Suba como no overturn, curve no topo, desça no meio e faça a curva inferior até a saída.",
        widthToXHeightRatio = 1.2f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    addAll(sampleQuad(Pair(0.15f, 0.0f), Pair(0.18f, 0.95f), Pair(0.38f, 1.0f), steps = 7))
                    addAll(sampleQuad(Pair(0.38f, 1.0f), Pair(0.55f, 0.95f), Pair(0.55f, 0.25f), steps = 8))
                    addAll(sampleQuad(Pair(0.55f, 0.25f), Pair(0.58f, 0.0f), Pair(0.85f, 1.0f), steps = 8))
                },
                hint = DirectionalHint(1, ReferencePoint(0.15f, 0.0f), 0.2f, 1.0f, "Curva composta fluida")
            )
        )
    )

    val BASIC_OVAL = ReferenceGlyph(
        id = "basic_oval",
        symbol = "o",
        name = "Forma Oval (O-Form)",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Inicie às duas horas, curve à esquerda tocando a altura-x, desça na linha de base e feche no topo.",
        widthToXHeightRatio = 1.0f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Ponto de início às 2 horas (0.75, 0.85)
                    addAll(sampleQuad(Pair(0.75f, 0.85f), Pair(0.6f, 1.0f), Pair(0.4f, 1.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.4f, 1.0f), Pair(0.15f, 0.7f), Pair(0.15f, 0.5f), steps = 6))
                    addAll(sampleQuad(Pair(0.15f, 0.5f), Pair(0.15f, 0.0f), Pair(0.45f, 0.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.45f, 0.0f), Pair(0.75f, 0.0f), Pair(0.75f, 0.5f), steps = 6))
                    addAll(sampleQuad(Pair(0.75f, 0.5f), Pair(0.75f, 0.8f), Pair(0.75f, 0.85f), steps = 4))
                },
                hint = DirectionalHint(1, ReferencePoint(0.75f, 0.85f), -0.5f, 0.5f, "Inicie às 2h e curve à esquerda")
            )
        )
    )

    val BASIC_ASCENDING_LOOP = ReferenceGlyph(
        id = "basic_ascending_loop",
        symbol = "ℓ",
        name = "Laçada Ascendente",
        category = GlyphCategory.BASIC_STROKE,
        instructions = "Suba a partir da linha de base até o topo do ascendente, faça a laçada à esquerda e desça em linha reta.",
        widthToXHeightRatio = 1.1f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Subida diagonal suave da baseline até o ascender (y=2.0)
                    addAll(sampleQuad(Pair(0.2f, 0.0f), Pair(0.4f, 1.0f), Pair(0.65f, 2.0f), steps = 12))
                    // Laçada no topo
                    addAll(sampleQuad(Pair(0.65f, 2.0f), Pair(0.45f, 2.0f), Pair(0.35f, 1.6f), steps = 8))
                    // Descida reta na inclinação até a baseline
                    addAll(sampleLine(Pair(0.35f, 1.6f), Pair(0.35f, 0.0f), steps = 12))
                },
                hint = DirectionalHint(1, ReferencePoint(0.2f, 0.0f), 0.45f, 2.0f, "Suba até a guia superior")
            )
        )
    )

    // --- 2. LETRAS MINÚSCULAS ---

    val LETTER_I = ReferenceGlyph(
        id = "letter_i",
        symbol = "i",
        name = "Letra 'i' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Traço 1: Suba da base até a altura-x, desça na mesma linha e faça a saída. Traço 2: Pingo acima da altura-x.",
        widthToXHeightRatio = 0.9f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Entrada e corpo
                    addAll(sampleQuad(Pair(0.2f, 0.0f), Pair(0.35f, 0.6f), Pair(0.45f, 1.0f), steps = 8))
                    addAll(sampleLine(Pair(0.45f, 1.0f), Pair(0.45f, 0.2f), steps = 8))
                    addAll(sampleQuad(Pair(0.45f, 0.2f), Pair(0.5f, 0.0f), Pair(0.75f, 0.5f), steps = 8))
                },
                hint = DirectionalHint(1, ReferencePoint(0.2f, 0.0f), 0.25f, 1.0f, "Suba e desça com saída")
            ),
            ReferenceStroke(
                orderIndex = 2,
                points = listOf(
                    ReferencePoint(0.45f, 1.35f),
                    ReferencePoint(0.46f, 1.34f)
                ),
                hint = DirectionalHint(2, ReferencePoint(0.45f, 1.35f), 0f, 0f, "Pingo suave")
            )
        )
    )

    val LETTER_T = ReferenceGlyph(
        id = "letter_t",
        symbol = "t",
        name = "Letra 't' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Traço 1: Suba até a altura média (1.5x), desça com curva de saída. Traço 2: Corte horizontal na altura-x.",
        widthToXHeightRatio = 0.9f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    addAll(sampleQuad(Pair(0.2f, 0.0f), Pair(0.35f, 0.8f), Pair(0.45f, 1.5f), steps = 10))
                    addAll(sampleLine(Pair(0.45f, 1.5f), Pair(0.45f, 0.2f), steps = 8))
                    addAll(sampleQuad(Pair(0.45f, 0.2f), Pair(0.5f, 0.0f), Pair(0.75f, 0.4f), steps = 6))
                },
                hint = DirectionalHint(1, ReferencePoint(0.2f, 0.0f), 0.25f, 1.5f, "Suba até meio-ascendente")
            ),
            ReferenceStroke(
                orderIndex = 2,
                points = sampleLine(Pair(0.25f, 1.0f), Pair(0.65f, 1.0f), steps = 8),
                hint = DirectionalHint(2, ReferencePoint(0.25f, 1.0f), 0.4f, 0.0f, "Barra horizontal")
            )
        )
    )

    val LETTER_A = ReferenceGlyph(
        id = "letter_a",
        symbol = "a",
        name = "Letra 'a' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Traço 1: Oval iniciando às 2h. Traço 2: Haste lateral descendente tocando a oval e curvando na saída.",
        widthToXHeightRatio = 1.1f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Oval fechada à esquerda (x=0.2 a 0.65)
                    addAll(sampleQuad(Pair(0.65f, 0.85f), Pair(0.55f, 1.0f), Pair(0.38f, 1.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.38f, 1.0f), Pair(0.18f, 0.7f), Pair(0.18f, 0.5f), steps = 6))
                    addAll(sampleQuad(Pair(0.18f, 0.5f), Pair(0.18f, 0.0f), Pair(0.42f, 0.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.42f, 0.0f), Pair(0.65f, 0.0f), Pair(0.65f, 0.85f), steps = 6))
                },
                hint = DirectionalHint(1, ReferencePoint(0.65f, 0.85f), -0.4f, 0.5f, "Desenhe a oval")
            ),
            ReferenceStroke(
                orderIndex = 2,
                points = buildList {
                    addAll(sampleLine(Pair(0.65f, 1.0f), Pair(0.65f, 0.2f), steps = 8))
                    addAll(sampleQuad(Pair(0.65f, 0.2f), Pair(0.7f, 0.0f), Pair(0.9f, 0.5f), steps = 6))
                },
                hint = DirectionalHint(2, ReferencePoint(0.65f, 1.0f), 0.0f, -1.0f, "Haste e saída")
            )
        )
    )

    val LETTER_L = ReferenceGlyph(
        id = "letter_l",
        symbol = "l",
        name = "Letra 'l' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Suba com laçada até a linha ascendente (2.0x), desça reto na inclinação e finalize com perna de saída.",
        widthToXHeightRatio = 1.0f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    addAll(sampleQuad(Pair(0.18f, 0.0f), Pair(0.35f, 1.0f), Pair(0.55f, 2.0f), steps = 12))
                    addAll(sampleQuad(Pair(0.55f, 2.0f), Pair(0.4f, 2.0f), Pair(0.3f, 1.6f), steps = 8))
                    addAll(sampleLine(Pair(0.3f, 1.6f), Pair(0.3f, 0.2f), steps = 10))
                    addAll(sampleQuad(Pair(0.3f, 0.2f), Pair(0.35f, 0.0f), Pair(0.65f, 0.5f), steps = 6))
                },
                hint = DirectionalHint(1, ReferencePoint(0.18f, 0.0f), 0.37f, 2.0f, "Laçada alta contínua")
            )
        )
    )

    val LETTER_C = ReferenceGlyph(
        id = "letter_c",
        symbol = "c",
        name = "Letra 'c' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Inicie na base, suba até a altura-x, retorne pelo mesmo arco e curve na linha de base com saída.",
        widthToXHeightRatio = 0.9f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    addAll(sampleQuad(Pair(0.2f, 0.0f), Pair(0.45f, 0.7f), Pair(0.65f, 0.95f), steps = 6))
                    addAll(sampleQuad(Pair(0.65f, 0.95f), Pair(0.35f, 1.0f), Pair(0.22f, 0.5f), steps = 8))
                    addAll(sampleQuad(Pair(0.22f, 0.5f), Pair(0.25f, 0.0f), Pair(0.55f, 0.0f), steps = 8))
                    addAll(sampleQuad(Pair(0.55f, 0.0f), Pair(0.72f, 0.05f), Pair(0.8f, 0.5f), steps = 6))
                },
                hint = DirectionalHint(1, ReferencePoint(0.2f, 0.0f), 0.45f, 0.95f, "Entrada e arco do 'c'")
            )
        )
    )

    val LETTER_O = ReferenceGlyph(
        id = "letter_o",
        symbol = "o",
        name = "Letra 'o' Cursiva",
        category = GlyphCategory.LOWERCASE,
        instructions = "Faça a oval completa fechando no topo e adicione uma pequena laçada horizontal de saída.",
        widthToXHeightRatio = 1.0f,
        strokes = listOf(
            ReferenceStroke(
                orderIndex = 1,
                points = buildList {
                    // Oval
                    addAll(sampleQuad(Pair(0.7f, 0.85f), Pair(0.55f, 1.0f), Pair(0.35f, 1.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.35f, 1.0f), Pair(0.18f, 0.7f), Pair(0.18f, 0.5f), steps = 6))
                    addAll(sampleQuad(Pair(0.18f, 0.5f), Pair(0.18f, 0.0f), Pair(0.45f, 0.0f), steps = 6))
                    addAll(sampleQuad(Pair(0.45f, 0.0f), Pair(0.7f, 0.0f), Pair(0.7f, 0.85f), steps = 6))
                    // Laçada de saída no topo
                    addAll(sampleQuad(Pair(0.7f, 0.85f), Pair(0.6f, 0.95f), Pair(0.85f, 1.0f), steps = 6))
                },
                hint = DirectionalHint(1, ReferencePoint(0.7f, 0.85f), -0.4f, 0.5f, "Oval com laço superior")
            )
        )
    )

    /**
     * Lista ordenada de todos os glifos disponíveis para treino guiado.
     */
    val ALL_GLYPHS: List<ReferenceGlyph> = listOf(
        // Traços básicos
        BASIC_SLANT,
        BASIC_UNDERTURN,
        BASIC_OVERTURN,
        BASIC_COMPOUND,
        BASIC_OVAL,
        BASIC_ASCENDING_LOOP,
        // Letras
        LETTER_I,
        LETTER_T,
        LETTER_A,
        LETTER_L,
        LETTER_C,
        LETTER_O
    )

    fun findById(id: String): ReferenceGlyph? = ALL_GLYPHS.find {
        it.id == id ||
        it.id == "basic_$id" ||
        id == "basic_${it.id}" ||
        (id == "underturn" && it.id == "basic_underturn") ||
        (id == "overturn" && it.id == "basic_overturn") ||
        (id == "compound_curve" && it.id == "basic_compound")
    }
}
