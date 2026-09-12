package com.scribe.caligrafia.ink.palm

import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.ToolType

/**
 * Resultado da avaliação de admissão de um evento de toque.
 */
sealed interface PalmDecision {
    data object Allow : PalmDecision
    data class Reject(val reason: String) : PalmDecision
}

/**
 * Máquina de estados comportamental para Palm Rejection (rejeição de palma) e
 * isolamento de ferramentas no Scribe.
 *
 * Funcionalidades centrais:
 * 1. Modo Stylus-Only: dedo nunca gera tinta (regra explícita de caligrafia).
 * 2. Preempção por proximidade: hover do stylus EMR suprime toques de dedo como palma.
 * 3. Preempção por contato ativo: stylus em DOWN suprime toques concorrentes.
 * 4. Janela de cooldown: evita que o descanso da palma entre traços rápidos gere pontos espúrios.
 * 5. Observabilidade completa para telemetria em tempo real.
 */
class PalmRejectionPolicy(
    var inputMode: InputMode = InputMode.STYLUS_ONLY,
    var hoverCooldownMs: Long = 500L
) {

    var isStylusHovering: Boolean = false
        private set

    var isStylusDown: Boolean = false
        private set

    var lastStylusActiveTimeMs: Long = 0L
        private set

    var lastStylusHoverTimeMs: Long = 0L
        private set

    var rejectedPalmTouchesCount: Long = 0L
        private set

    var lastRejectionReason: String? = null
        private set

    /**
     * Avalia se um determinado toque deve ser admitido para criação/continuação de traço
     * ou rejeitado como contato involuntário de palma / ferramenta não autorizada.
     */
    fun evaluateTouch(toolType: ToolType, eventTimeMs: Long): PalmDecision {
        // Caneta e borracha têm passagem irrestrita
        if (toolType == ToolType.STYLUS || toolType == ToolType.ERASER) {
            return PalmDecision.Allow
        }

        // Avaliação para toques de dedo (FINGER)
        if (toolType == ToolType.FINGER) {
            if (inputMode == InputMode.STYLUS_ONLY) {
                return reject("Dedo ignorado: Modo Stylus-Only ativo.")
            }

            // No modo STYLUS_AND_FINGER, aplicar rejeição comportamental
            if (isStylusDown) {
                return reject("Rejeição de palma: Stylus em escrita ativa.")
            }

            if (isStylusHovering) {
                return reject("Rejeição de palma: Stylus em proximidade (hover).")
            }

            if (lastStylusActiveTimeMs > 0 && (eventTimeMs - lastStylusActiveTimeMs) in 0 until hoverCooldownMs) {
                val delta = eventTimeMs - lastStylusActiveTimeMs
                return reject("Rejeição de palma: Cooldown de repouso (${delta}ms < ${hoverCooldownMs}ms).")
            }

            if (lastStylusHoverTimeMs > 0 && (eventTimeMs - lastStylusHoverTimeMs) in 0 until hoverCooldownMs) {
                val delta = eventTimeMs - lastStylusHoverTimeMs
                return reject("Rejeição de palma: Cooldown pós-hover (${delta}ms < ${hoverCooldownMs}ms).")
            }

            return PalmDecision.Allow
        }

        // Outras ferramentas (MOUSE, UNKNOWN)
        return if (inputMode == InputMode.STYLUS_ONLY) {
            reject("Ferramenta ${toolType.name} bloqueada em modo Stylus-Only.")
        } else {
            PalmDecision.Allow
        }
    }

    fun onStylusHoverEnter(eventTimeMs: Long) {
        isStylusHovering = true
        lastStylusHoverTimeMs = eventTimeMs
    }

    fun onStylusHoverMove(eventTimeMs: Long) {
        isStylusHovering = true
        lastStylusHoverTimeMs = eventTimeMs
    }

    fun onStylusHoverExit(eventTimeMs: Long) {
        isStylusHovering = false
        lastStylusHoverTimeMs = eventTimeMs
    }

    fun onStylusDown(eventTimeMs: Long) {
        isStylusDown = true
        lastStylusActiveTimeMs = eventTimeMs
    }

    fun onStylusUp(eventTimeMs: Long) {
        isStylusDown = false
        lastStylusActiveTimeMs = eventTimeMs
    }

    fun onStylusCancel(eventTimeMs: Long) {
        isStylusDown = false
        lastStylusActiveTimeMs = eventTimeMs
    }

    private fun reject(reason: String): PalmDecision.Reject {
        rejectedPalmTouchesCount++
        lastRejectionReason = reason
        return PalmDecision.Reject(reason)
    }

    fun resetMetrics() {
        rejectedPalmTouchesCount = 0L
        lastRejectionReason = null
        isStylusHovering = false
        isStylusDown = false
        lastStylusActiveTimeMs = 0L
        lastStylusHoverTimeMs = 0L
    }
}
