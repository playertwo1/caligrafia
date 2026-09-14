package com.scribe.caligrafia.expansions.signature

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SignatureBaselineStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun referenceRoundTrip_preservesRawStrokeAndSupportedZeroSensorValues() {
        val dir = tempFolder.newFolder("signatures")
        val store = SignatureBaselineStore(dir)
        val attempt = attempt("baseline_a", -25f)

        store.save(attempt)
        val loaded = SignatureBaselineStore(dir).load()

        assertNotNull(loaded)
        assertEquals(attempt.id, loaded?.id)
        assertEquals(attempt.durationMs, loaded?.durationMs)
        assertEquals(attempt.minX, loaded?.minX)
        assertEquals(attempt.maxX, loaded?.maxX)
        assertEquals(1, loaded?.strokes?.size)
        val first = loaded!!.strokes.first().points.first()
        assertEquals(0f, first.pressure ?: -1f, 0.0001f)
        assertEquals(0f, first.tiltRad ?: -1f, 0.0001f)
        assertEquals(0f, first.orientationRad ?: -1f, 0.0001f)
    }

    @Test
    fun failedReplacement_beforePointerPublish_keepsPreviousReferenceActive() {
        val dir = tempFolder.newFolder("signatures_failure")
        val original = attempt("baseline_original", 0f)
        SignatureBaselineStore(dir).save(original)

        val failingStore = SignatureBaselineStore(dir) {
            throw IllegalStateException("simulated publish failure")
        }
        var failed = false
        try {
            failingStore.save(attempt("baseline_new", 100f))
        } catch (_: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
        val loaded = SignatureBaselineStore(dir).load()
        assertEquals("baseline_original", loaded?.id)
        assertFalse(File(dir, "baseline_versions/baseline_new").exists())
    }

    private fun attempt(id: String, offsetX: Float): SignatureAttempt {
        val stroke = Stroke(
            id = "stroke_$id",
            tool = ToolType.STYLUS,
            points = listOf(
                StrokePoint(offsetX, 10f, 1000L, pressure = 0f, tiltRad = 0f, orientationRad = 0f),
                StrokePoint(offsetX + 40f, 30f, 1050L, pressure = 0.5f, tiltRad = 0.2f, orientationRad = -0.1f),
                StrokePoint(offsetX + 90f, 25f, 1100L, pressure = 1f, tiltRad = 0f, orientationRad = 0f)
            ),
            startedAtMs = 1000L,
            endedAtMs = 1100L,
            isCancelled = false,
            color = 0xFF000000.toInt(),
            baseWidthPx = 4f
        )
        return SignatureAttempt(
            id = id,
            timestampMs = 1234L,
            strokes = listOf(stroke),
            durationMs = 100L,
            minX = offsetX,
            minY = 10f,
            maxX = offsetX + 90f,
            maxY = 30f
        )
    }
}
