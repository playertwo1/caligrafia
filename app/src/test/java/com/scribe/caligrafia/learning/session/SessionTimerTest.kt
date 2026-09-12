package com.scribe.caligrafia.learning.session

import com.scribe.caligrafia.learning.model.CurriculumCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do temporizador de sessão caligráfica estruturada (M4 — SCR-402).
 */
class SessionTimerTest {

    @Test
    fun startSession_initializesWithWarmUpPhase() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        val state = timer.sessionState.value
        assertNotNull(state)
        assertEquals(SessionPhase.WARM_UP, state?.currentPhase)
        assertEquals(0, state?.totalElapsedSeconds)
        assertEquals(0, state?.phaseElapsedSeconds)
        assertEquals(300, state?.totalSeconds)
        assertFalse(state?.isPaused ?: true)
        assertFalse(state?.isFinished ?: true)
    }

    @Test
    fun tickOneSecond_incrementsSeconds() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        timer.tickOneSecond()
        val state = timer.sessionState.value
        assertEquals(1, state?.totalElapsedSeconds)
        assertEquals(1, state?.phaseElapsedSeconds)
    }

    @Test
    fun pauseAndResume_controlsTicking() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        timer.pause()
        assertTrue(timer.sessionState.value?.isPaused ?: false)

        timer.tickOneSecond()
        assertEquals(0, timer.sessionState.value?.totalElapsedSeconds) // Não deve ter incrementado

        timer.resume()
        assertFalse(timer.sessionState.value?.isPaused ?: true)

        timer.tickOneSecond()
        assertEquals(1, timer.sessionState.value?.totalElapsedSeconds)
    }

    @Test
    fun skipToNextPhase_advancesPhaseSequentially() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        assertEquals(SessionPhase.WARM_UP, timer.sessionState.value?.currentPhase)

        timer.skipToNextPhase()
        assertEquals(SessionPhase.DEMO_FOCUS, timer.sessionState.value?.currentPhase)

        timer.skipToNextPhase()
        assertEquals(SessionPhase.ASSISTED_PRACTICE, timer.sessionState.value?.currentPhase)

        timer.skipToNextPhase()
        assertEquals(SessionPhase.SOLO_PRACTICE, timer.sessionState.value?.currentPhase)

        timer.skipToNextPhase()
        assertEquals(SessionPhase.REVIEW_SUMMARY, timer.sessionState.value?.currentPhase)

        timer.skipToNextPhase()
        assertTrue(timer.sessionState.value?.isFinished ?: false)
    }

    @Test
    fun recordAttempt_updatesCountAndAverage() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        timer.recordAttempt(70f)
        assertEquals(1, timer.sessionState.value?.attemptsCount)
        assertEquals(70f, timer.sessionState.value?.averageScore ?: 0f, 0.01f)

        timer.recordAttempt(90f)
        assertEquals(2, timer.sessionState.value?.attemptsCount)
        assertEquals(80f, timer.sessionState.value?.averageScore ?: 0f, 0.01f)
    }

    @Test
    fun naturalPhaseTransition_whenSecondsElapse() {
        val timer = SessionTimer()
        val lesson = CurriculumCatalog.defaultFirstLesson()
        timer.startSession(lesson, SessionDuration.MIN_5)

        val warmUpTotal = timer.sessionState.value?.phaseTotalSeconds ?: 0
        assertTrue("Fase inicial deve ter ao menos 10 segundos", warmUpTotal >= 10)

        // Simula passagem completa da fase
        for (i in 0 until warmUpTotal) {
            timer.tickOneSecond()
        }

        // Deve ter transicionado automaticamente para DEMO_FOCUS
        assertEquals(SessionPhase.DEMO_FOCUS, timer.sessionState.value?.currentPhase)
        assertEquals(0, timer.sessionState.value?.phaseElapsedSeconds)
        assertEquals(warmUpTotal, timer.sessionState.value?.totalElapsedSeconds)
    }
}
