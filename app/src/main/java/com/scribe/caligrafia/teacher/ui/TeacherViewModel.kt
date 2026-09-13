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
import com.scribe.caligrafia.guided.catalog.ReferenceGlyphCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Estado da interface do Professor IA (SCR-704, F4.01, F4.05, F4.07).
 */
data class TeacherUiState(
    val isLoading: Boolean = true,
    val diagnostic: BiomechanicalDiagnostic? = null,
    val prescription: PrescribedPracticeSession? = null,
    val insights: List<TeacherInsight> = emptyList(),
    val selectedDimension: BiomechanicalDimension? = null,
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val isPrescriptionValid: Boolean = false
)

/**
 * ViewModel do Professor IA & Coaching Inteligente (SCR-704).
 *
 * Inclui @JvmOverloads constructor(application: Application, ...) para garantir
 * instanciação reflexiva segura sem falhas no AndroidViewModelFactory.
 */
class TeacherViewModel @JvmOverloads constructor(
    application: Application,
    private val teacherRepository: TeacherRepository = LocalTeacherRepository(
        application.filesDir ?: java.io.File(System.getProperty("java.io.tmpdir", "."), "scribe_teacher_test")
    ),
    private val attemptRepository: PracticeAttemptRepository = LocalPracticeAttemptRepository(
        application.filesDir ?: java.io.File(System.getProperty("java.io.tmpdir", "."), "scribe_teacher_test")
    ),
    private val diagnosticEngine: MotorDiagnosticEngine = MotorDiagnosticEngine(),
    private val curriculumGenerator: CoachingCurriculumGenerator = CoachingCurriculumGenerator(),
    private val feedbackEngine: CoachingFeedbackEngine = CoachingFeedbackEngine(),
    externalScope: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val scope = externalScope ?: viewModelScope
    private val reanalysisMutex = Mutex()

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        scope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val diagnostic = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    teacherRepository.getLatestDiagnostic()
                }
                val prescription = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    teacherRepository.getLatestPrescription()
                }
                val insights = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    teacherRepository.getRecentInsights()
                }

                val isValid = prescription?.let {
                    ReferenceGlyphCatalog.findById(it.focusExerciseId) != null &&
                    ReferenceGlyphCatalog.findById(it.warmupExerciseId) != null
                } ?: false

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        diagnostic = diagnostic,
                        prescription = prescription,
                        insights = insights,
                        selectedDimension = diagnostic.primaryWeakness ?: BiomechanicalDimension.SLANT_STABILITY,
                        isPrescriptionValid = isValid,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Falha ao carregar dados do Professor: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Reavalia todo o histórico de tentativas com o motor biomecânico do Professor IA.
     * Serializado via Mutex para evitar condições de corrida (F4.05).
     */
    fun reanalyzeAllData() {
        scope.launch(Dispatchers.Default) {
            reanalysisMutex.withLock {
                _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
                try {
                    val attempts = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        attemptRepository.getAllAttempts()
                    }
                    val (newDiagnostic, newPrescription, newInsights) = kotlinx.coroutines.withContext(Dispatchers.Default) {
                        val diag = diagnosticEngine.diagnoseAttempts(attempts)
                        val presc = curriculumGenerator.generatePrescription(diag)
                        val ins = feedbackEngine.generateInsights(diag, attempts)
                        Triple(diag, presc, ins)
                    }

                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        teacherRepository.saveDiagnostic(newDiagnostic)
                        teacherRepository.savePrescription(newPrescription)
                        teacherRepository.saveInsights(newInsights)
                    }

                    val isValid = ReferenceGlyphCatalog.findById(newPrescription.focusExerciseId) != null &&
                                  ReferenceGlyphCatalog.findById(newPrescription.warmupExerciseId) != null

                    _uiState.update {
                        it.copy(
                            diagnostic = newDiagnostic,
                            prescription = newPrescription,
                            insights = newInsights,
                            selectedDimension = newDiagnostic.primaryWeakness ?: BiomechanicalDimension.SLANT_STABILITY,
                            isPrescriptionValid = isValid,
                            errorMessage = null
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Erro ao processar análise biomecânica: ${e.message}")
                    }
                } finally {
                    _uiState.update { it.copy(isAnalyzing = false) }
                }
            }
        }
    }

    fun selectDimension(dimension: BiomechanicalDimension) {
        _uiState.update { it.copy(selectedDimension = dimension) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun markPrescriptionCompleted() {
        scope.launch(Dispatchers.Default) {
            val currentPrescription = _uiState.value.prescription ?: return@launch
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                teacherRepository.markPrescriptionCompleted(currentPrescription.id)
            }
            _uiState.update {
                it.copy(prescription = currentPrescription.copy(isCompleted = true))
            }
        }
    }
}
