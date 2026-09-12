package com.scribe.caligrafia.ink.persistence

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.StrokePoint
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.benchmark.PersistenceBenchmarkRunner
import com.scribe.caligrafia.ink.persistence.strategies.CompressedBlobStrategy
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.persistence.strategies.RelationalTableStrategy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PersistenceSpikeTest {

    private lateinit var tempDir: File
    private lateinit var runner: PersistenceBenchmarkRunner

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "scribe_spike_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        runner = PersistenceBenchmarkRunner(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestStrokes(): List<Stroke> {
        return listOf(
            Stroke(
                id = "stroke_1",
                tool = ToolType.STYLUS,
                points = listOf(
                    StrokePoint(10.5f, 20.25f, 1000L, pressure = 0.45f, tiltRad = 0.2f, orientationRad = 1.1f),
                    StrokePoint(15.0f, 25.50f, 1008L, pressure = 0.65f, tiltRad = 0.21f, orientationRad = 1.1f),
                    StrokePoint(20.0f, 30.75f, 1016L, pressure = 0.85f, tiltRad = 0.22f, orientationRad = 1.1f)
                ),
                startedAtMs = 1000L,
                endedAtMs = 1016L,
                isCancelled = false
            ),
            Stroke(
                id = "stroke_2",
                tool = ToolType.ERASER,
                points = listOf(
                    StrokePoint(50f, 50f, 2000L, pressure = null, tiltRad = null, orientationRad = null),
                    StrokePoint(60f, 60f, 2010L, pressure = null, tiltRad = null, orientationRad = null)
                ),
                startedAtMs = 2000L,
                endedAtMs = 2010L,
                isCancelled = false
            )
        )
    }

    @Test
    fun testRelationalTableRoundTripFidelity() {
        val strategy = RelationalTableStrategy(File(tempDir, "relational"))
        val original = createTestStrokes()

        val size = strategy.save("session_1", original)
        assertTrue(size > 0L)

        val loaded = strategy.load("session_1")
        assertEquals(original.size, loaded.size)

        for (i in original.indices) {
            val os = original[i]
            val ls = loaded[i]
            assertEquals(os.id, ls.id)
            assertEquals(os.tool, ls.tool)
            assertEquals(os.startedAtMs, ls.startedAtMs)
            assertEquals(os.endedAtMs, ls.endedAtMs)
            assertEquals(os.points.size, ls.points.size)

            for (j in os.points.indices) {
                val op = os.points[j]
                val lp = ls.points[j]
                assertEquals(op.x, lp.x, 0.0001f)
                assertEquals(op.y, lp.y, 0.0001f)
                assertEquals(op.tMs, lp.tMs)
                assertEquals(op.pressure, lp.pressure)
                assertEquals(op.tiltRad, lp.tiltRad)
                assertEquals(op.orientationRad, lp.orientationRad)
            }
        }
    }

    @Test
    fun testCompressedBlobRoundTripFidelity() {
        val strategy = CompressedBlobStrategy(File(tempDir, "blob"))
        val original = createTestStrokes()

        val size = strategy.save("session_2", original)
        assertTrue(size > 0L)

        val loaded = strategy.load("session_2")
        assertEquals(original.size, loaded.size)

        for (i in original.indices) {
            val os = original[i]
            val ls = loaded[i]
            assertEquals(os.id, ls.id)
            assertEquals(os.tool, ls.tool)
            assertEquals(os.points.size, ls.points.size)

            for (j in os.points.indices) {
                val op = os.points[j]
                val lp = ls.points[j]
                assertEquals(op.x, lp.x, 0.0001f)
                assertEquals(op.y, lp.y, 0.0001f)
                assertEquals(op.tMs, lp.tMs)
                assertEquals(op.pressure, lp.pressure)
                assertEquals(op.tiltRad, lp.tiltRad)
                assertEquals(op.orientationRad, lp.orientationRad)
            }
        }
    }

    @Test
    fun testDedicatedFileRoundTripFidelity() {
        val strategy = DedicatedFileStrategy(File(tempDir, "file"))
        val original = createTestStrokes()

        val size = strategy.save("session_3", original)
        assertTrue(size > 0L)

        val file = strategy.getFile("session_3")
        assertTrue(file.exists())
        val bytes = file.readBytes()
        assertTrue(bytes.take(8).toByteArray().contentEquals(DedicatedFileStrategy.MAGIC_HEADER))

        val loaded = strategy.load("session_3")
        assertEquals(original.size, loaded.size)

        for (i in original.indices) {
            val os = original[i]
            val ls = loaded[i]
            assertEquals(os.id, ls.id)
            assertEquals(os.tool, ls.tool)
            assertEquals(os.points.size, ls.points.size)

            for (j in os.points.indices) {
                val op = os.points[j]
                val lp = ls.points[j]
                assertEquals(op.x, lp.x, 0.0001f)
                assertEquals(op.y, lp.y, 0.0001f)
                assertEquals(op.tMs, lp.tMs)
                assertEquals(op.pressure, lp.pressure)
                assertEquals(op.tiltRad, lp.tiltRad)
                assertEquals(op.orientationRad, lp.orientationRad)
            }
        }
    }

    @Test
    fun testBenchmarkSuiteExecution() {
        val report = runner.runBenchmark(strokeCount = 20, pointsPerStroke = 40)
        assertEquals(20, report.strokeCount)
        assertEquals(800, report.totalPoints)
        assertEquals(3, report.results.size)

        for (r in report.results) {
            assertTrue("Fidelidade deve ser preservada em ${r.strategyName}", r.isFidelityPreserved)
            assertTrue("Tamanho deve ser > 0 em ${r.strategyName}", r.sizeBytes > 0)
            assertTrue("Tempo de escrita deve ser > 0 em ${r.strategyName}", r.writeTimeMs >= 0.0)
            assertTrue("Tempo de leitura deve ser > 0 em ${r.strategyName}", r.readTimeMs >= 0.0)
        }

        val md = report.toMarkdown()
        assertNotNull(md)
        println(md)
    }
}
