package com.scribe.caligrafia.guided.model

/**
 * Categoria pedagógica do elemento caligráfico de referência.
 */
enum class GlyphCategory(val displayName: String) {
    BASIC_STROKE("Traços Fundamentais"),
    LOWERCASE("Letras Minúsculas"),
    UPPERCASE("Letras Maiúsculas"),
    CONNECTOR("Ligaduras e Conexões")
}

/**
 * Ponto normalizado dentro do sistema de coordenadas pedagógicas de uma pauta caligráfica.
 *
 * Convenção matemática:
 * - [xRatio]: Posição horizontal normalizada (0.0f = margem esquerda do glyph, 1.0f = margem direita).
 * - [yRatio]: Posição vertical relativa à pauta caligráfica:
 *     0.0f = Linha de base (baseline)
 *     1.0f = Altura-X (x-height)
 *     2.0f = Ascendente (proporção 2:1) ou 1.5f (proporção 3:2)
 *    -1.0f = Descendente (proporção 2:1:2)
 *
 * A conversão para pixels do canvas é dada por:
 *   canvasX = originX + xRatio * glyphWidthPx
 *   canvasY = baselineY - (yRatio * xHeightPx)
 */
data class ReferencePoint(
    val xRatio: Float,
    val yRatio: Float,
    val targetPressure: Float? = null
)

/**
 * Pista visual indicando a ordem do traço e a direção inicial da caneta.
 */
data class DirectionalHint(
    val strokeOrder: Int,
    val startPoint: ReferencePoint,
    val vectorDx: Float,
    val vectorDy: Float,
    val description: String = ""
)

/**
 * Traço caligráfico atômico dentro de um glyph de referência.
 *
 * @param orderIndex Ordem pedagógica de execução (1 = primeiro traço, 2 = segundo traço...).
 * @param points Sequência densa de pontos normalizados descrevendo a curvatura canônica.
 * @param hint Pista visual opcional para auxiliar o início do traçado.
 */
data class ReferenceStroke(
    val orderIndex: Int,
    val points: List<ReferencePoint>,
    val hint: DirectionalHint? = null
) {
    init {
        require(points.isNotEmpty()) { "Um traço de referência deve possuir pelo menos um ponto." }
    }

    val startPoint: ReferencePoint
        get() = points.first()

    val endPoint: ReferencePoint
        get() = points.last()
}

/**
 * Glifo canônico de referência caligráfica para treino guiado.
 *
 * @param id Identificador único do exercício (ex: "basic_slant", "letter_a").
 * @param symbol Caractere ou símbolo representativo (ex: "/", "a", "l").
 * @param name Nome pedagógico do exercício (ex: "Traço Inclinado", "Letra 'a'").
 * @param category Categoria caligráfica do exercício.
 * @param instructions Instruções passo a passo em português para o aluno.
 * @param widthToXHeightRatio Largura total do glifo em relação à Altura-X da pauta (ex: 1.0 = quadrado).
 * @param strokes Lista cronológica dos traços canônicos para desenhar o glifo.
 */
data class ReferenceGlyph(
    val id: String,
    val symbol: String,
    val name: String,
    val category: GlyphCategory,
    val instructions: String,
    val widthToXHeightRatio: Float = 1.0f,
    val strokes: List<ReferenceStroke>
) {
    init {
        require(strokes.isNotEmpty()) { "Um glifo de referência deve possuir pelo menos um traço." }
        require(widthToXHeightRatio > 0.1f) { "Largura do glifo deve ser positiva e maior que 0.1" }
    }

    val strokeCount: Int
        get() = strokes.size
}
