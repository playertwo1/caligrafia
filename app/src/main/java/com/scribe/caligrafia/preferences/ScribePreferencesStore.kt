package com.scribe.caligrafia.preferences

import android.content.Context
import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.expansions.styles.PressureCurveType

enum class ToolbarSide(val displayName: String) {
    LEFT("Esquerda"),
    RIGHT("Direita")
}

enum class TextScaleOption(val multiplier: Float, val displayName: String) {
    SYSTEM(1.0f, "Sistema"),
    LARGE(1.15f, "Grande"),
    EXTRA_LARGE(1.30f, "Muito grande")
}

enum class GuideContrastOption(val alpha: Float, val displayName: String) {
    NORMAL(0.55f, "Normal"),
    HIGH(0.85f, "Alto contraste")
}

data class ScribePreferences(
    val isLeftHanded: Boolean = false,
    /** null = recomendação automática oposta à mão dominante. */
    val toolbarSideOverride: ToolbarSide? = null,
    val inputMode: InputMode = InputMode.STYLUS_ONLY,
    val textScale: TextScaleOption = TextScaleOption.SYSTEM,
    val guideContrast: GuideContrastOption = GuideContrastOption.NORMAL,
    val reduceAnimations: Boolean = false,
    val pressureCurve: PressureCurveType = PressureCurveType.LINEAR,
    val breakReminderEnabled: Boolean = true,
    val breakIntervalMinutes: Int = 15,
    val vibrationEnabled: Boolean = true,
    val showGuideNumbers: Boolean = true,
    val dailyPracticeGoalMinutes: Int = 15
) {
    val effectiveToolbarSide: ToolbarSide
        get() = toolbarSideOverride ?: if (isLeftHanded) ToolbarSide.RIGHT else ToolbarSide.LEFT
}

/**
 * Fonte única para preferências locais da F6.
 *
 * Mantém as chaves históricas já usadas pelo app para não quebrar instalações existentes e adiciona
 * somente as opções explicitamente previstas no checklist F6. Nenhuma opção altera raw strokes.
 */
class ScribePreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ScribePreferences {
        val pressureCurve = enumOrDefault(
            prefs.getString(KEY_PRESSURE_CURVE, null),
            PressureCurveType.LINEAR
        ).let { curve ->
            // F6 publica somente as três curvas contratadas. Instalações antigas com SIGMOID voltam
            // de forma segura para LINEAR sem tocar em dados brutos.
            when (curve) {
                PressureCurveType.LINEAR,
                PressureCurveType.SOFT,
                PressureCurveType.FIRM -> curve
                PressureCurveType.SIGMOID_CALLIGRAPHIC -> PressureCurveType.LINEAR
            }
        }

        return ScribePreferences(
            isLeftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false),
            toolbarSideOverride = prefs.getString(KEY_TOOLBAR_SIDE, null)?.let {
                runCatching { ToolbarSide.valueOf(it) }.getOrNull()
            },
            inputMode = enumOrDefault(prefs.getString(KEY_INPUT_MODE, null), InputMode.STYLUS_ONLY),
            textScale = enumOrDefault(prefs.getString(KEY_TEXT_SCALE, null), TextScaleOption.SYSTEM),
            guideContrast = if (prefs.contains(KEY_GUIDE_CONTRAST)) {
                enumOrDefault(prefs.getString(KEY_GUIDE_CONTRAST, null), GuideContrastOption.NORMAL)
            } else if (prefs.getBoolean(KEY_LEGACY_HIGH_CONTRAST, false)) {
                GuideContrastOption.HIGH
            } else {
                GuideContrastOption.NORMAL
            },
            reduceAnimations = prefs.getBoolean(KEY_REDUCE_ANIMATIONS, false),
            pressureCurve = pressureCurve,
            breakReminderEnabled = prefs.getBoolean(KEY_BREAK_ENABLED, true),
            breakIntervalMinutes = prefs.getInt(KEY_BREAK_INTERVAL_MINUTES, 15).coerceIn(5, 60),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            showGuideNumbers = prefs.getBoolean(KEY_SHOW_GUIDE_NUMBERS, true),
            dailyPracticeGoalMinutes = prefs.getInt(KEY_DAILY_GOAL_MINUTES, 15).coerceIn(5, 60)
        )
    }

    fun save(value: ScribePreferences): Boolean {
        return prefs.edit()
            .putBoolean(KEY_LEFT_HANDED, value.isLeftHanded)
            .apply {
                if (value.toolbarSideOverride == null) remove(KEY_TOOLBAR_SIDE)
                else putString(KEY_TOOLBAR_SIDE, value.toolbarSideOverride.name)
            }
            .putString(KEY_INPUT_MODE, value.inputMode.name)
            .putString(KEY_TEXT_SCALE, value.textScale.name)
            .putString(KEY_GUIDE_CONTRAST, value.guideContrast.name)
            .putBoolean(KEY_LEGACY_HIGH_CONTRAST, value.guideContrast == GuideContrastOption.HIGH)
            .putBoolean(KEY_REDUCE_ANIMATIONS, value.reduceAnimations)
            .putString(KEY_PRESSURE_CURVE, value.pressureCurve.name)
            .putBoolean(KEY_BREAK_ENABLED, value.breakReminderEnabled)
            .putInt(KEY_BREAK_INTERVAL_MINUTES, value.breakIntervalMinutes.coerceIn(5, 60))
            .putBoolean(KEY_VIBRATION_ENABLED, value.vibrationEnabled)
            .putBoolean(KEY_SHOW_GUIDE_NUMBERS, value.showGuideNumbers)
            .putInt(KEY_DAILY_GOAL_MINUTES, value.dailyPracticeGoalMinutes.coerceIn(5, 60))
            .commit()
    }

    fun update(transform: (ScribePreferences) -> ScribePreferences): ScribePreferences {
        val updated = transform(load())
        check(save(updated)) { "Não foi possível persistir preferências locais" }
        return updated
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        return raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }

    companion object {
        const val PREFS_NAME = "scribe_settings"
        const val KEY_LEFT_HANDED = "is_left_handed"
        const val KEY_TOOLBAR_SIDE = "toolbar_side_override"
        const val KEY_INPUT_MODE = "input_mode"
        const val KEY_TEXT_SCALE = "text_scale"
        const val KEY_GUIDE_CONTRAST = "guide_contrast"
        const val KEY_LEGACY_HIGH_CONTRAST = "is_high_contrast"
        const val KEY_REDUCE_ANIMATIONS = "reduce_animations"
        const val KEY_PRESSURE_CURVE = "pressure_curve"
        const val KEY_BREAK_ENABLED = "break_reminder_enabled"
        const val KEY_BREAK_INTERVAL_MINUTES = "break_interval_minutes"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_SHOW_GUIDE_NUMBERS = "show_guide_numbers"
        const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
    }
}
