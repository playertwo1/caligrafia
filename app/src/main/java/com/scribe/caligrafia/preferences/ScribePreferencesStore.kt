package com.scribe.caligrafia.preferences

import android.content.Context
import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.InputModeRuntime
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
 * Mantém as chaves históricas para preservar upgrades e nunca altera raw strokes.
 */
class ScribePreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ScribePreferences {
        val pressureCurve = enumOrDefault(
            prefs.getString(KEY_PRESSURE_CURVE, null),
            PressureCurveType.LINEAR
        ).let { curve ->
            when (curve) {
                PressureCurveType.LINEAR,
                PressureCurveType.SOFT,
                PressureCurveType.FIRM -> curve
                PressureCurveType.SIGMOID_CALLIGRAPHIC -> PressureCurveType.LINEAR
            }
        }

        val loaded = ScribePreferences(
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
        InputModeRuntime.current = loaded.inputMode
        ScribePreferencesRuntime.publish(loaded)
        return loaded
    }

    fun save(value: ScribePreferences): Boolean {
        val normalized = value.copy(
            breakIntervalMinutes = value.breakIntervalMinutes.coerceIn(5, 60),
            dailyPracticeGoalMinutes = value.dailyPracticeGoalMinutes.coerceIn(5, 60),
            pressureCurve = when (value.pressureCurve) {
                PressureCurveType.LINEAR,
                PressureCurveType.SOFT,
                PressureCurveType.FIRM -> value.pressureCurve
                PressureCurveType.SIGMOID_CALLIGRAPHIC -> PressureCurveType.LINEAR
            }
        )
        val committed = prefs.edit()
            .putBoolean(KEY_LEFT_HANDED, normalized.isLeftHanded)
            .apply {
                if (normalized.toolbarSideOverride == null) remove(KEY_TOOLBAR_SIDE)
                else putString(KEY_TOOLBAR_SIDE, normalized.toolbarSideOverride.name)
            }
            .putString(KEY_INPUT_MODE, normalized.inputMode.name)
            .putString(KEY_TEXT_SCALE, normalized.textScale.name)
            .putString(KEY_GUIDE_CONTRAST, normalized.guideContrast.name)
            .putBoolean(KEY_LEGACY_HIGH_CONTRAST, normalized.guideContrast == GuideContrastOption.HIGH)
            .putBoolean(KEY_REDUCE_ANIMATIONS, normalized.reduceAnimations)
            .putString(KEY_PRESSURE_CURVE, normalized.pressureCurve.name)
            .putBoolean(KEY_BREAK_ENABLED, normalized.breakReminderEnabled)
            .putInt(KEY_BREAK_INTERVAL_MINUTES, normalized.breakIntervalMinutes)
            .putBoolean(KEY_VIBRATION_ENABLED, normalized.vibrationEnabled)
            .putBoolean(KEY_SHOW_GUIDE_NUMBERS, normalized.showGuideNumbers)
            .putInt(KEY_DAILY_GOAL_MINUTES, normalized.dailyPracticeGoalMinutes)
            .commit()
        if (committed) {
            InputModeRuntime.current = normalized.inputMode
            ScribePreferencesRuntime.publish(normalized)
        }
        return committed
    }

    fun update(transform: (ScribePreferences) -> ScribePreferences): ScribePreferences {
        val updated = transform(load())
        check(save(updated)) { "Não foi possível persistir preferências locais" }
        return ScribePreferencesRuntime.current
    }

    fun writeBackupSnapshot(target: File): Boolean {
        val value = load()
        val text = listOf(
            "$KEY_PRESSURE_CURVE=${value.pressureCurve.name}",
            "$KEY_LEFT_HANDED=${value.isLeftHanded}",
            "$KEY_TOOLBAR_SIDE=${value.toolbarSideOverride?.name.orEmpty()}",
            "$KEY_INPUT_MODE=${value.inputMode.name}",
            "$KEY_TEXT_SCALE=${value.textScale.name}",
            "$KEY_GUIDE_CONTRAST=${value.guideContrast.name}",
            "$KEY_LEGACY_HIGH_CONTRAST=${value.guideContrast == GuideContrastOption.HIGH}",
            "$KEY_REDUCE_ANIMATIONS=${value.reduceAnimations}",
            "$KEY_BREAK_ENABLED=${value.breakReminderEnabled}",
            "$KEY_BREAK_INTERVAL_MINUTES=${value.breakIntervalMinutes}",
            "$KEY_VIBRATION_ENABLED=${value.vibrationEnabled}",
            "$KEY_SHOW_GUIDE_NUMBERS=${value.showGuideNumbers}",
            "$KEY_DAILY_GOAL_MINUTES=${value.dailyPracticeGoalMinutes}"
        ).joinToString("\n")
        val temp = File.createTempFile("preferences_snapshot_", ".tmp", target.parentFile)
        return try {
            FileOutputStream(temp).use { output ->
                output.write(text.toByteArray(Charsets.UTF_8)); output.flush(); output.fd.sync()
            }
            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Throwable) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun restoreBackupSnapshot(source: File): Boolean {
        if (!source.isFile) return false
        val values = parseBackupSnapshot(source.readText(Charsets.UTF_8)) ?: return false
        val defaults = ScribePreferences()
        fun bool(key: String, fallback: Boolean) = values[key]?.toBooleanStrictOrNull() ?: fallback
        fun int(key: String, fallback: Int) = values[key]?.toIntOrNull() ?: fallback
        fun <T : Enum<T>> enum(key: String, fallback: T, parse: (String) -> T) =
            values[key]?.takeIf(String::isNotBlank)?.let { runCatching { parse(it) }.getOrNull() } ?: fallback
        val restored = defaults.copy(
            pressureCurve = enum(KEY_PRESSURE_CURVE, defaults.pressureCurve, PressureCurveType::valueOf),
            isLeftHanded = bool(KEY_LEFT_HANDED, defaults.isLeftHanded),
            toolbarSideOverride = values[KEY_TOOLBAR_SIDE]?.takeIf(String::isNotBlank)?.let { runCatching { ToolbarSide.valueOf(it) }.getOrNull() },
            inputMode = enum(KEY_INPUT_MODE, defaults.inputMode, InputMode::valueOf),
            textScale = enum(KEY_TEXT_SCALE, defaults.textScale, TextScaleOption::valueOf),
            guideContrast = values[KEY_GUIDE_CONTRAST]?.let { runCatching { GuideContrastOption.valueOf(it) }.getOrNull() }
                ?: if (bool(KEY_LEGACY_HIGH_CONTRAST, false)) GuideContrastOption.HIGH else defaults.guideContrast,
            reduceAnimations = bool(KEY_REDUCE_ANIMATIONS, defaults.reduceAnimations),
            breakReminderEnabled = bool(KEY_BREAK_ENABLED, defaults.breakReminderEnabled),
            breakIntervalMinutes = int(KEY_BREAK_INTERVAL_MINUTES, defaults.breakIntervalMinutes),
            vibrationEnabled = bool(KEY_VIBRATION_ENABLED, defaults.vibrationEnabled),
            showGuideNumbers = bool(KEY_SHOW_GUIDE_NUMBERS, defaults.showGuideNumbers),
            dailyPracticeGoalMinutes = int(KEY_DAILY_GOAL_MINUTES, defaults.dailyPracticeGoalMinutes)
        )
        return save(restored)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        return raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }

    companion object {
        const val BACKUP_FILE_NAME = "preferences_snapshot.txt"
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

        val BACKUP_KEYS = setOf(KEY_PRESSURE_CURVE, KEY_LEFT_HANDED, KEY_TOOLBAR_SIDE,
            KEY_INPUT_MODE, KEY_TEXT_SCALE, KEY_GUIDE_CONTRAST, KEY_LEGACY_HIGH_CONTRAST,
            KEY_REDUCE_ANIMATIONS, KEY_BREAK_ENABLED, KEY_BREAK_INTERVAL_MINUTES,
            KEY_VIBRATION_ENABLED, KEY_SHOW_GUIDE_NUMBERS, KEY_DAILY_GOAL_MINUTES)

        fun parseBackupSnapshot(text: String): Map<String, String>? {
            val pairs = linkedMapOf<String, String>()
            for (line in text.lineSequence().filter(String::isNotBlank)) {
                val index = line.indexOf('=')
                if (index <= 0) return null
                val key = line.substring(0, index)
                if (key !in BACKUP_KEYS || pairs.put(key, line.substring(index + 1)) != null) return null
            }
            return pairs.takeIf { it.isNotEmpty() }
        }
    }
}
