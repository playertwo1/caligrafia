package com.scribe.caligrafia.ink.capture

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint

/**
 * Representa uma ação reversível no histórico da sessão de escrita.
 */
sealed interface SessionAction {
    data class Added(val stroke: Stroke) : SessionAction
    data class Erased(val strokesWithIndices: List<Pair<Int, Stroke>>) : SessionAction
}

/**
 * Repositório em memória para armazenar e gerenciar a lista de traços da sessão.
 * Fornece operações de adição, borracha por traço (stroke eraser), histórico de undo e limpeza.
 */
class InMemoryStrokeRepository {

    private val strokes = mutableListOf<Stroke>()
    private val undoStack = mutableListOf<SessionAction>()
    private val redoStack = mutableListOf<SessionAction>()

    val allStrokes: List<Stroke>
        get() = strokes.toList()

    val count: Int
        get() = strokes.size

    val isEmpty: Boolean
        get() = strokes.isEmpty()

    val totalPointsCount: Int
        get() = strokes.sumOf { it.points.size }

    val canUndo: Boolean
        get() = undoStack.isNotEmpty() || strokes.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Inicializa o repositório com uma lista de traços carregada do disco/página,
     * reiniciando as pilhas de histórico.
     */
    fun loadStrokes(initialStrokes: List<Stroke>) {
        strokes.clear()
        strokes.addAll(initialStrokes)
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Adiciona um novo traço vetorial à sessão e invalida o histórico de redo.
     */
    fun addStroke(stroke: Stroke) {
        strokes.add(stroke)
        undoStack.add(SessionAction.Added(stroke))
        redoStack.clear()
    }

    /**
     * Apaga todos os traços que interceptam o caminho percorrido pela borracha.
     * @return Lista de traços que foram removidos da sessão.
     */
    fun eraseStrokesIntersecting(
        eraserPoints: List<StrokePoint>,
        eraserRadius: Float = 24f
    ): List<Stroke> {
        if (eraserPoints.isEmpty() || strokes.isEmpty()) return emptyList()

        val erasedEntries = mutableListOf<Pair<Int, Stroke>>()
        val iterator = strokes.indices.reversed().iterator()

        while (iterator.hasNext()) {
            val index = iterator.next()
            val candidate = strokes[index]
            if (StrokeEraserHelper.intersects(eraserPoints, candidate, eraserRadius)) {
                erasedEntries.add(index to candidate)
                strokes.removeAt(index)
            }
        }

        if (erasedEntries.isNotEmpty()) {
            // Salva no histórico mantendo a ordem original dos índices
            erasedEntries.reverse()
            undoStack.add(SessionAction.Erased(erasedEntries))
            redoStack.clear()
        }

        return erasedEntries.map { it.second }
    }

    /**
     * Desfaz a última ação realizada (traço adicionado ou apagado).
     * @return true se alguma ação foi desfeita com sucesso.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) {
            // Fallback para traços carregados sem histórico explícito
            if (strokes.isNotEmpty()) {
                val last = strokes.removeAt(strokes.lastIndex)
                redoStack.add(SessionAction.Added(last))
                return true
            }
            return false
        }

        val action = undoStack.removeAt(undoStack.lastIndex)
        when (action) {
            is SessionAction.Added -> {
                strokes.remove(action.stroke)
                redoStack.add(action)
            }
            is SessionAction.Erased -> {
                // Restaura os traços apagados nas suas posições originais
                for ((index, stroke) in action.strokesWithIndices) {
                    val safeIndex = index.coerceIn(0, strokes.size)
                    strokes.add(safeIndex, stroke)
                }
                redoStack.add(action)
            }
        }
        return true
    }

    /**
     * Refaz a última ação desfeita.
     * @return true se alguma ação foi refeita com sucesso.
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false

        val action = redoStack.removeAt(redoStack.lastIndex)
        when (action) {
            is SessionAction.Added -> {
                strokes.add(action.stroke)
                undoStack.add(action)
            }
            is SessionAction.Erased -> {
                // Re-apaga os traços
                for ((_, stroke) in action.strokesWithIndices.reversed()) {
                    strokes.remove(stroke)
                }
                undoStack.add(action)
            }
        }
        return true
    }

    /**
     * Desfaz a última ação da sessão e retorna o traço afetado (compatibilidade com M0).
     */
    fun removeLastStroke(): Stroke? {
        val lastStroke = strokes.lastOrNull()
        if (undo()) {
            return lastStroke
        }
        return null
    }

    fun clear() {
        strokes.clear()
        undoStack.clear()
        redoStack.clear()
    }

    fun getStroke(index: Int): Stroke? {
        return strokes.getOrNull(index)
    }
}
