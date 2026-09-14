package com.scribe.caligrafia.ink.palm

import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.InputModeRuntime
import com.scribe.caligrafia.core.model.ToolType

sealed interface PalmDecision {
    data object Allow : PalmDecision
    data class Reject(val reason: String) : PalmDecision
}

/**
 * Máquina de estados para Palm Rejection e isolamento de ferramentas.
 *
 * Pipelines de produto podem iniciar seguindo [InputModeRuntime]. Uma atribuição explícita em
 * [inputMode] desliga esse vínculo para a instância, preservando ferramentas diagnósticas e testes
 * que precisam de uma política local determinística.
 */
class PalmRejectionPolicy(
    inputMode: InputMode = InputMode.STYLUS_ONLY,
    hoverCooldownMs: Long = 500L,
    followRuntimeInputMode: Boolean = false
) {
    private var followsRuntimeInputMode: Boolean = followRuntimeInputMode

    var inputMode: InputMode = inputMode
        set(value) {
            field = value
            followsRuntimeInputMode = false
        }

    var hoverCooldownMs: Long = hoverCooldownMs

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

    fun useRuntimeInputMode() {
        followsRuntimeInputMode = true
    }

    fun evaluateTouch(toolType: ToolType, eventTimeMs: Long): PalmDecision {
        val effectiveInputMode = if (followsRuntimeInputMode) InputModeRuntime.current else inputMode

        if (toolType == ToolType.STYLUS || toolType == ToolType.ERASER) {
            return PalmDecision.Allow
        }

        if (toolType == ToolType.FINGER) {
            if (effectiveInputMode == InputMode.STYLUS_ONLY) {
                return reject("Dedo ignorado: Modo Stylus-Only ativo.")
            }

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

        return if (effectiveInputMode == InputMode.STYLUS_ONLY) {
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
