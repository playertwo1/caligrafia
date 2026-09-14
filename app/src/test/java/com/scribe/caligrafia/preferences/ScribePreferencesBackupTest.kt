package com.scribe.caligrafia.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScribePreferencesBackupTest {
    @Test
    fun `snapshot F6 accepts every contracted preference`() {
        val text = """
            pressure_curve=FIRM
            is_left_handed=true
            toolbar_side_override=RIGHT
            input_mode=STYLUS_AND_FINGER
            text_scale=EXTRA_LARGE
            guide_contrast=HIGH
            is_high_contrast=true
            reduce_animations=true
            break_reminder_enabled=false
            break_interval_minutes=30
            vibration_enabled=false
            show_guide_numbers=false
            daily_goal_minutes=25
        """.trimIndent()

        assertEquals(ScribePreferencesStore.BACKUP_KEYS, ScribePreferencesStore.parseBackupSnapshot(text)?.keys)
    }

    @Test
    fun `legacy snapshot remains valid and unknown or duplicated keys fail`() {
        assertNotNull(ScribePreferencesStore.parseBackupSnapshot("pressure_curve=LINEAR\nis_left_handed=false\nis_high_contrast=false\nshow_guide_numbers=true\ndaily_goal_minutes=15"))
        assertNull(ScribePreferencesStore.parseBackupSnapshot("secret=value"))
        assertNull(ScribePreferencesStore.parseBackupSnapshot("input_mode=STYLUS_ONLY\ninput_mode=STYLUS_AND_FINGER"))
    }
}
