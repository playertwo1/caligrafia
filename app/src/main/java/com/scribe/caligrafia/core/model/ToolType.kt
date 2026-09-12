package com.scribe.caligrafia.core.model

import android.view.MotionEvent

/**
 * Representação tipada das ferramentas físicas de entrada do Android.
 */
enum class ToolType {
    STYLUS,
    ERASER,
    FINGER,
    MOUSE,
    UNKNOWN;

    companion object {
        /**
         * Resolve a ferramenta considerando o tipo reportado pelo driver e o estado
         * dos botões físicos (ex: botão lateral da S Pen no Galaxy S25 Ultra).
         */
        fun fromMotionEvent(toolType: Int, buttonState: Int = 0): ToolType {
            // Se o driver já reportou como borracha
            if (toolType == MotionEvent.TOOL_TYPE_ERASER) return ERASER

            // No Galaxy S25 Ultra e dispositivos Wacom EMR, pressionar o botão da S Pen
            // reporta BUTTON_STYLUS_PRIMARY ou BUTTON_SECONDARY, ativando a borracha
            val isStylusButtonPressed = (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0) ||
                    (buttonState and MotionEvent.BUTTON_SECONDARY != 0)

            if (toolType == MotionEvent.TOOL_TYPE_STYLUS && isStylusButtonPressed) {
                return ERASER
            }

            return when (toolType) {
                MotionEvent.TOOL_TYPE_STYLUS -> STYLUS
                MotionEvent.TOOL_TYPE_ERASER -> ERASER
                MotionEvent.TOOL_TYPE_FINGER -> FINGER
                MotionEvent.TOOL_TYPE_MOUSE -> MOUSE
                else -> UNKNOWN
            }
        }
    }
}
