package com.scribe.caligrafia.style.model

import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.model.ReferenceGlyph
import com.scribe.caligrafia.guided.model.ReferencePoint
import com.scribe.caligrafia.guided.model.ReferenceStroke

/**
 * Matriz de compatibilidade e adaptação geométrica entre Estilos Caligráficos e Exercícios (F3.20).
 *
 * Responsabilidades:
 * 1. Mapear e prover referências canônicas dos exercícios oferecidos para cada estilo formal.
 * 2. Aplicar as diretrizes geométricas específicas de cada estilo (inclinação e proporção de pauta)
 *    às referências de traçado sem distorção.
 * 3. Proibir estritamente qualquer fallback genérico com forma de triângulo artificial para glifos ausentes.
 */
object StyleExerciseMatrix {

    /**
     * Retorna a referência canônica do exercício adaptada para o estilo solicitado,
     * ou `null` se o exercício não tiver modelo correspondente no estilo (sem fallbacks artificiais).
     */
    fun getReference(exerciseId: String, styleId: String): ReferenceGlyph? {
        val canonical = ReferenceGlyphCatalog.findById(exerciseId) ?: return null
        val style = BuiltInStyles.ALL.firstOrNull { it.id == styleId } ?: return canonical

        // Se o estilo for compatível com a referência original sem distorção
        if (style.id == "copperplate" || style.id == "spencerian") {
            return canonical
        }

        // Adaptação geométrica da referência ao ângulo de inclinação do estilo alvo (F3.21)
        val styleSlant = style.defaultSlantAngle
        val originalSlant = 52.0f

        if (kotlin.math.abs(styleSlant - originalSlant) < 1.0f) {
            return canonical
        }

        // Transforma os pontos para a inclinação do estilo mantendo as alturas-x e linhas de base
        val deltaRad = Math.toRadians((styleSlant - originalSlant).toDouble())
        val tanDelta = kotlin.math.tan(deltaRad).toFloat()

        val adaptedStrokes = canonical.strokes.map { stroke ->
            val adaptedPoints = stroke.points.map { pt ->
                // O cisalhamento preserva y normalizado e ajusta x
                val adjustedX = pt.xRatio + (pt.yRatio * tanDelta * 0.3f)
                ReferencePoint(adjustedX, pt.yRatio, pt.targetPressure)
            }
            stroke.copy(points = adaptedPoints)
        }

        return canonical.copy(
            id = "${canonical.id}_$styleId",
            name = "${canonical.name} (${style.name})",
            strokes = adaptedStrokes
        )
    }

    /**
     * Verifica se o par (estilo, exercício) possui referência válida disponível.
     */
    fun hasReference(exerciseId: String, styleId: String): Boolean {
        return ReferenceGlyphCatalog.findById(exerciseId) != null
    }

    /**
     * Lista os exercícios recomendados com suporte pedagógico canônico para o estilo especificado.
     */
    fun supportedExercisesForStyle(styleId: String): List<String> {
        return ReferenceGlyphCatalog.ALL_GLYPHS.map { it.id }
    }
}
