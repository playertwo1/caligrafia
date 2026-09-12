package com.scribe.caligrafia.evolution.repository

import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.PracticeAttemptRecord

/**
 * Contrato de repositório para persistência de tentativas de prática e consultas de evolução (SCR-501 a SCR-504).
 */
interface PracticeAttemptRepository {
    fun saveAttempt(attempt: PracticeAttemptRecord)
    fun getAttemptsForTarget(targetId: String): List<PracticeAttemptRecord>
    fun getAllAttempts(): List<PracticeAttemptRecord>
    fun getBeforeAndAfter(targetId: String): Pair<PracticeAttemptRecord, PracticeAttemptRecord>?
    fun getAllComparisons(): List<BeforeAfterComparison>
}
