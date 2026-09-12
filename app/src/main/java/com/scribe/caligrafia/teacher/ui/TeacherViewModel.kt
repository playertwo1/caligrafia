package com.scribe.caligrafia.teacher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.evolution.repository.LocalPracticeAttemptRepository
import com.scribe.caligrafia.evolution.repository.PracticeAttemptRepository
import com.scribe.caligrafia.teacher.engine.CoachingCurriculumGenerator
import com.scribe.caligrafia.teacher.engine.CoachingFeedbackEngine
import com.scribe.caligrafia.teacher.engine.MotorDiagnosticEngine
import com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight
import com.scribe.caligrafia.teacher.repository.LocalTeacherRepository
import com.scribe.caligrafia.teacher.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da interface do Professor IA (SCR-704).
 */
data class TeacherUiState(
    val isLoading: Boolean = true,
    val diagnostic: BiomechanicalDiagnostic? = null,
    val prescription: PrescribedPracticeSession? = null,
    val insights: List<TeacherInsight> = emptyList(),
    val selectedDimension: BiomechanicalDimension? = null,
    val isAnalyzing: Boolean = false
)

/**
 * ViewModel do Professor IA & Coaching Inteligente (SCR-704).
 *
 * Inclui @JvmOverloads constructor(application: Application, ...) para garantir
 * instanciação reflexiva segura sem falhas no AndroidViewModelFactory.
 */
class TeacherViewModel @JvmOverloads constructor(
    application: Application,
    private val teacherRepository: TeacherRepository = LocalTeacherRepository(application.filesDir),
    private val attemptRepository: PracticeAttemptRepository = LocalPracticeAttemptRepository(application.filesDir),
    private val diagnosticEngine: MotorDiagnosticEngine = MotorDiagnosticEngine(),
    private val curriculumGenerator: CoachingCurriculumGenerator = CoachingCurriculumGenerator(),
    private val feedbackEngine: CoachingFeedbackEngine = CoachingFeedbackEngine()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val diagnostic = teacherRepository.getLatestDiagnostic()
            val prescription = teacherRepository.getLatestPrescription()
            val insights = teacherRepository.getRecentInsights()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    diagnostic = diagnostic,
                    prescription = prescription,
                    insights = insights,
                    selectedDimension = diagnostic.primaryWeakness ?: BiomechanicalDimension.SLANT_STABILITY
                )
            }
        }
    }

    /**
     * Reavalia todo o histórico de tentativas com o motor biomecânico do Professor IA.
     */
    fun reanalyzeAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val attempts = attemptRepository.getAllAttempts()
            val newDiagnostic = diagnosticEngine.diagnoseAttempts(attempts)
            val newPrescription = curriculumGenerator.generatePrescription(newDiagnostic)
            val newInsights = feedbackEngine.generateInsights(newDiagnostic)

            teacherRepository.saveDiagnostic(newDiagnostic)
            teacherRepository.savePrescription(newPrescription)
            teacherRepository.saveInsights(newInsights)

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    diagnostic = newDiagnostic,
                    prescription = newPrescription,
                    insights = newInsights,
                    selectedDimension = newDiagnostic.primaryWeakness ?: BiomechanicalDimension.SLANT_STABILITY
                )
            }
        }
    }

    fun selectDimension(dimension: BiomechanicalDimension) {
        _uiState.update { it.copy(selectedDimension = dimension) }
    }

    fun markPrescriptionCompleted() {
        viewModelScope.launch {
            val currentPrescription = _uiState.value.prescription ?: return@launch
            teacherRepository.markPrescriptionCompleted(currentPrescription.id)
            _uiState.update {
                it.copy(prescription = currentPrescription.copy(isCompleted = true))
            }
        }
    }
}
