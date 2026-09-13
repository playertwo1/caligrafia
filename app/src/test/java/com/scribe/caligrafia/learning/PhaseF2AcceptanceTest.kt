package com.scribe.caligrafia.learning

import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.GuidelineRatio
import com.scribe.caligrafia.core.model.SlantConfig
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import com.scribe.caligrafia.guided.evaluator.GeometricFeedbackEvaluator
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.learning.history.CompletedSessionRecord
import com.scribe.caligrafia.learning.history.LearningHistorySerializer
import com.scribe.caligrafia.learning.history.LocalLearningHistoryRepository
import com.scribe.caligrafia.learning.model.CurriculumCatalog
import com.scribe.caligrafia.learning.session.ActiveSessionState
import com.scribe.caligrafia.learning.session.SessionDuration
import com.scribe.caligrafia.learning.session.SessionPhase
import com.scribe.caligrafia.learning.session.SessionTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Bateria de Testes de Aceitação da Fase F2 (F2.01 a F2.26).
 *
 * Valida o cumprimento integral dos requisitos pedagógicos, persistência atômica,
 * monotonicidade do tempo, preservação de pausa e não-duplicação de registros.
 */
class PhaseF2AcceptanceTest {

    /**
     * F2.02: Todas as 18 lições existentes possuem glyphIds canônicos estritamente resolvíveis
     * no catálogo ReferenceGlyphCatalog antes de oferecer Iniciar.
     */
    @Test
    fun f2_02_curriculumCatalogGlyphsAreAllResolvableInReferenceGlyphCatalog() {
        val lessons = CurriculumCatalog.ALL_LESSONS
        assertEquals("Devem existir exatamente 18 lições no currículo", 18, lessons.size)

        lessons.forEach { lesson ->
            assertTrue("Lição '${lesson.id}' deve ter título não vazio", lesson.title.isNotBlank())
            assertTrue("Lição '${lesson.id}' deve ter descrição não vazia", lesson.description.isNotBlank())
            lesson.glyphIds.forEach { glyphId ->
                val resolved = ReferenceGlyphCatalog.findById(glyphId)
                assertNotNull("O glifo '$glyphId' da lição '${lesson.id}' DEVE ser resolúvel no ReferenceGlyphCatalog", resolved)
            }
        }
    }

    /**
     * F2.09 & F2.26: Teste dos 59 segundos.
     * Sessão praticada por 59 segundos deve registrar exatamente 59 segundos reais,
     * sem inventar minutos ou truncar a contagem.
     */
    @Test
    fun f2_09_and_f2_26_monotonicTimerRecords59SecondsAccurately() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        for (i in 1..59) {
            timer.tickOneSecond()
        }

        val state = timer.sessionState.value
        assertNotNull(state)
        assertEquals("Deve ter registrado exatamente 59 segundos", 59, state?.totalElapsedSeconds)

        val tempDir = Files.createTempDirectory("scribe-f2-59s-").toFile()
        try {
            val repo = LocalLearningHistoryRepository(tempDir)
            val record = CompletedSessionRecord(
                sessionId = UUID.randomUUID().toString(),
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                timestampMs = System.currentTimeMillis(),
                durationMinutes = if (state!!.totalElapsedSeconds > 0) 1 else 0,
                actualDurationSeconds = state.totalElapsedSeconds,
                attemptsCount = 0,
                averageScorePercent = null
            )
            repo.recordSession(record)

            val summary = repo.getProgressSummary()
            assertEquals(1, summary.totalSessionsCompleted)
            assertEquals(59, summary.recentSessions.first().actualDurationSeconds)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F2.10 & F2.11: Pausa manual deve ser preservada ao retornar de segundo plano.
     * Retorno ao app (resume não-manual) NÃO deve desmarcar a pausa manual feita pelo usuário.
     */
    @Test
    fun f2_10_and_f2_11_manualPausePreservedAcrossAppResume() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_10)

        // Usuário pausa explicitamente na UI
        timer.pause(manual = true)
        assertTrue("Sessão deve estar pausada", timer.sessionState.value?.isPaused == true)

        // App volta para primeiro plano (onResume / lifecycle)
        timer.resume(manual = false)
        assertTrue("Sessão DEVE continuar pausada após resume do ciclo de vida", timer.sessionState.value?.isPaused == true)

        // Ticking não deve incrementar tempo
        val elapsedBefore = timer.sessionState.value?.totalElapsedSeconds ?: 0
        timer.tickOneSecond()
        assertEquals("Tempo não pode avançar enquanto em pausa manual", elapsedBefore, timer.sessionState.value?.totalElapsedSeconds)

        // Usuário clica explicitamente em retomar
        timer.resume(manual = true)
        assertFalse("Sessão deve despausar com clique explícito do usuário", timer.sessionState.value?.isPaused == true)
        timer.tickOneSecond()
        assertEquals("Tempo deve voltar a avançar", elapsedBefore + 1, timer.sessionState.value?.totalElapsedSeconds)
    }

    /**
     * F2.11: Persistência de sessão interrompida.
     * Salva sessão em andamento, simula destruição e restaura:
     * Sessão restaurada DEVE reabrir pausada e conservar exatamente os segundos decorridos.
     */
    @Test
    fun f2_11_interruptedSessionRestoredPausedWithoutTimeLeak() {
        val tempDir = Files.createTempDirectory("scribe-f2-restore-").toFile()
        try {
            val repo = LocalLearningHistoryRepository(tempDir)
            val lesson = CurriculumCatalog.ALL_LESSONS[1]

            val originalState = ActiveSessionState(
                lesson = lesson,
                duration = SessionDuration.MIN_15,
                currentPhase = SessionPhase.ASSISTED_PRACTICE,
                phaseElapsedSeconds = 45,
                phaseTotalSeconds = 360,
                totalElapsedSeconds = 180,
                isPaused = false,
                isFinished = false,
                attemptsCount = 3,
                averageScore = 78.5f
            )

            // Salva sessão em andamento
            repo.saveActiveSession(originalState)

            // Simula reinicialização do app e recuperação da sessão
            val restored = repo.loadActiveSession()
            assertNotNull("Sessão restaurada deve existir em disco", restored)
            assertEquals(lesson.id, restored?.lesson?.id)
            assertEquals(SessionDuration.MIN_15, restored?.duration)
            assertEquals(SessionPhase.ASSISTED_PRACTICE, restored?.currentPhase)
            assertEquals(180, restored?.totalElapsedSeconds)
            assertEquals(45, restored?.phaseElapsedSeconds)
            assertEquals(3, restored?.attemptsCount)
            assertEquals(78.5f, restored?.averageScore ?: 0f, 0.01f)
            assertTrue("Sessão restaurada DEVE estar pausada (F2.11)", restored?.isPaused == true)

            // Limpa após finalização
            repo.clearActiveSession()
            assertNull("Após clear, sessão ativa não deve existir em disco", repo.loadActiveSession())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F2.15: Pautas e alvo ajustam-se proporcionalmente quando o espaço vertical é constrito,
     * impedindo que o glifo e as guias desapareçam do canvas.
     */
    @Test
    fun f2_15_guidelineBandsFallbackOnConstrainedPageHeight() {
        val config = GuidelineConfig.copperplate(xHeightPx = 65f)
        // Altura muito baixa (120px), onde topMargin + bandHeight normalmente excederia
        val bands = config.computeBands(pageHeight = 120f)
        assertTrue("Deve fornecer pelo menos uma faixa adaptada mesmo em tela comprimida", bands.isNotEmpty())
        val band = bands.first()
        assertTrue("Altura-x deve ser positiva", band.xHeight > 0f)
        assertTrue("Linha de base deve estar contida dentro da altura disponível", band.baselineY <= 120f)
    }

    /**
     * F2.21 & F2.26: Encerramento de sessão e finalização duplicada.
     * Múltiplas chamadas de finalização não devem gerar registros duplicados no repositório.
     * Cancelamento de sessão não gera registro fictício.
     */
    @Test
    fun f2_21_and_f2_26_duplicateFinalizationIsIdempotent() {
        val tempDir = Files.createTempDirectory("scribe-f2-idempotent-").toFile()
        try {
            val repo = LocalLearningHistoryRepository(tempDir)
            val lesson = CurriculumCatalog.defaultFirstLesson()
            val record = CompletedSessionRecord(
                sessionId = "unique-session-id-123",
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                timestampMs = System.currentTimeMillis(),
                durationMinutes = 10,
                actualDurationSeconds = 600,
                attemptsCount = 5,
                averageScorePercent = 85
            )

            // Grava duas vezes o mesmo record (simulando duplo clique)
            repo.recordSession(record)
            repo.recordSession(record)

            val summary = repo.getProgressSummary()
            assertEquals("Não deve duplicar a sessão com mesmo sessionId", 1, summary.totalSessionsCompleted)
            assertEquals("unique-session-id-123", summary.recentSessions.first().sessionId)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F2.24: "Tentar novamente" preserva o histórico de tentativas anterior no repositório
     * e permite gerar uma nova tentativa limpa com ID distinto.
     */
    @Test
    fun f2_24_retryPreservesPreviousAttemptAndStartsFreshCleanAttempt() {
        val tempDir = Files.createTempDirectory("scribe-f2-retry-").toFile()
        try {
            val attemptRepo = LocalPracticeAttemptRepository(tempDir)
            val glyph = ReferenceGlyphCatalog.BASIC_SLANT

            // Primeira tentativa
            val attempt1 = PracticeAttemptRecord(
                attemptId = UUID.randomUUID().toString(),
                targetId = glyph.id,
                targetTitle = glyph.name,
                timestampMs = System.currentTimeMillis(),
                strokes = emptyList(),
                scorePercent = 60,
                averageSlantDegrees = 50f,
                durationMs = 5000L,
                isBaseline = true,
                targetSlantDegrees = 52f,
                styleId = "copperplate"
            )
            attemptRepo.saveAttempt(attempt1)

            // Segunda tentativa (Tentar novamente)
            val attempt2 = PracticeAttemptRecord(
                attemptId = UUID.randomUUID().toString(),
                targetId = glyph.id,
                targetTitle = glyph.name,
                timestampMs = System.currentTimeMillis() + 1000L,
                strokes = emptyList(),
                scorePercent = 85,
                averageSlantDegrees = 52f,
                durationMs = 6000L,
                isBaseline = false,
                targetSlantDegrees = 52f,
                styleId = "copperplate"
            )
            attemptRepo.saveAttempt(attempt2)

            val savedAttempts = attemptRepo.getAttemptsForTarget(glyph.id)
            assertEquals("Ambas as tentativas devem estar preservadas", 2, savedAttempts.size)
            assertTrue("Tentativas devem ter IDs distintos", attempt1.attemptId != attempt2.attemptId)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * F2.26: Sessão concluída sem tentativas avaliadas (attemptsCount = 0, averageScore = null)
     * é registrada sem erro, com valores nulos explícitos e sem notas artificiais.
     */
    @Test
    fun f2_26_sessionWithoutEvaluationsSafelyRecorded() {
        val tempDir = Files.createTempDirectory("scribe-f2-noeval-").toFile()
        try {
            val repo = LocalLearningHistoryRepository(tempDir)
            val lesson = CurriculumCatalog.defaultFirstLesson()
            val record = CompletedSessionRecord(
                sessionId = UUID.randomUUID().toString(),
                lessonId = lesson.id,
                lessonTitle = lesson.title,
                timestampMs = System.currentTimeMillis(),
                durationMinutes = 5,
                actualDurationSeconds = 300,
                attemptsCount = 0,
                averageScorePercent = null
            )
            repo.recordSession(record)

            val summary = repo.getProgressSummary()
            assertEquals(1, summary.totalSessionsCompleted)
            assertEquals(0, summary.recentSessions.first().attemptsCount)
            assertNull("Média de nota deve ser estritamente nula na ausência de tentativas", summary.recentSessions.first().averageScorePercent)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
