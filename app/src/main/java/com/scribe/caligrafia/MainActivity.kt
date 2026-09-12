package com.scribe.caligrafia

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.scribe.caligrafia.alphabet.ui.AlphabetScreen
import com.scribe.caligrafia.alphabet.ui.AlphabetViewModel
import com.scribe.caligrafia.evolution.ui.EvolutionScreen
import com.scribe.caligrafia.evolution.ui.EvolutionViewModel
import com.scribe.caligrafia.guided.ui.GuidedPracticeScreen
import com.scribe.caligrafia.guided.ui.GuidedPracticeViewModel
import com.scribe.caligrafia.inspector.ui.InspectorScreen
import com.scribe.caligrafia.inspector.viewmodel.StylusLabViewModel
import com.scribe.caligrafia.learning.ui.LearningHubScreen
import com.scribe.caligrafia.learning.ui.LearningViewModel
import com.scribe.caligrafia.notebook.ui.NotebookPracticeScreen
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import com.scribe.caligrafia.expansions.ui.ExpansionsScreen
import com.scribe.caligrafia.expansions.ui.ExpansionsViewModel
import com.scribe.caligrafia.teacher.ui.TeacherScreen
import com.scribe.caligrafia.teacher.ui.TeacherViewModel
import com.scribe.caligrafia.ui.theme.ScribeTheme

enum class ScribeScreen {
    NOTEBOOK,
    GUIDED_PRACTICE,
    LEARNING_HUB,
    EVOLUTION,
    ALPHABET,
    TEACHER_AI,
    EXPANSIONS,
    STYLUS_LAB
}

class MainActivity : ComponentActivity() {

    private val stylusLabViewModel: StylusLabViewModel by viewModels()
    private val notebookViewModel: NotebookPracticeViewModel by viewModels()
    private val guidedPracticeViewModel: GuidedPracticeViewModel by viewModels()
    private val learningViewModel: LearningViewModel by viewModels()
    private val evolutionViewModel: EvolutionViewModel by viewModels()
    private val alphabetViewModel: AlphabetViewModel by viewModels()
    private val teacherViewModel: TeacherViewModel by viewModels()
    private val expansionsViewModel: ExpansionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configura comportamento imersivo transitório similar ao Samsung Notes:
        // Gestos rápidos nas bordas não minimizam a tela de escrita acidentalmente.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            ScribeTheme {
                var currentScreen by rememberSaveable { mutableStateOf(ScribeScreen.NOTEBOOK) }
                var lastBackPressTime by remember { mutableStateOf(0L) }

                // Interceptador inteligente do gesto "Voltar":
                // Previne fechamento involuntário do app ao escrever nas laterais.
                BackHandler {
                    when (currentScreen) {
                        ScribeScreen.STYLUS_LAB -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.GUIDED_PRACTICE -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.LEARNING_HUB -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.EVOLUTION -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.ALPHABET -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.TEACHER_AI -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.EXPANSIONS -> currentScreen = ScribeScreen.NOTEBOOK
                        ScribeScreen.NOTEBOOK -> {
                            val now = System.currentTimeMillis()
                            if (now - lastBackPressTime < 2000L) {
                                finish()
                            } else {
                                lastBackPressTime = now
                                Toast.makeText(this@MainActivity, "Pressione voltar novamente para sair", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                when (currentScreen) {
                    ScribeScreen.NOTEBOOK -> {
                        NotebookPracticeScreen(
                            viewModel = notebookViewModel,
                            onNavigateToLab = { currentScreen = ScribeScreen.STYLUS_LAB },
                            onNavigateToGuidedPractice = { currentScreen = ScribeScreen.GUIDED_PRACTICE },
                            onNavigateToLearningHub = { currentScreen = ScribeScreen.LEARNING_HUB },
                            onNavigateToEvolution = { currentScreen = ScribeScreen.EVOLUTION },
                            onNavigateToAlphabet = { currentScreen = ScribeScreen.ALPHABET },
                            onNavigateToTeacher = { currentScreen = ScribeScreen.TEACHER_AI },
                            onNavigateToExpansions = { currentScreen = ScribeScreen.EXPANSIONS }
                        )
                    }
                    ScribeScreen.GUIDED_PRACTICE -> {
                        GuidedPracticeScreen(
                            viewModel = guidedPracticeViewModel,
                            onNavigateBack = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                    ScribeScreen.LEARNING_HUB -> {
                        LearningHubScreen(
                            viewModel = learningViewModel,
                            onNavigateBack = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                    ScribeScreen.EVOLUTION -> {
                        EvolutionScreen(
                            viewModel = evolutionViewModel,
                            onNavigateBack = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                    ScribeScreen.ALPHABET -> {
                        AlphabetScreen(
                            viewModel = alphabetViewModel,
                            onNavigateBack = { currentScreen = ScribeScreen.NOTEBOOK },
                            onNavigateToPractice = { currentScreen = ScribeScreen.GUIDED_PRACTICE },
                            onNavigateToNotebook = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                    ScribeScreen.TEACHER_AI -> {
                        TeacherScreen(
                            viewModel = teacherViewModel,
                            onBack = { currentScreen = ScribeScreen.NOTEBOOK },
                            onStartPractice = { exerciseId ->
                                guidedPracticeViewModel.selectGlyphById(exerciseId)
                                currentScreen = ScribeScreen.GUIDED_PRACTICE
                            }
                        )
                    }
                    ScribeScreen.EXPANSIONS -> {
                        ExpansionsScreen(
                            viewModel = expansionsViewModel,
                            onBack = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                    ScribeScreen.STYLUS_LAB -> {
                        InspectorScreen(
                            viewModel = stylusLabViewModel,
                            onNavigateToNotebook = { currentScreen = ScribeScreen.NOTEBOOK }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            stylusLabViewModel.onResumeLifecycle(this)
        } catch (e: Throwable) {
            android.util.Log.e("Scribe", "Erro no onResume do StylusLab", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            stylusLabViewModel.onPauseLifecycle(this)
        } catch (e: Throwable) {
            android.util.Log.e("Scribe", "Erro no onPause do StylusLab", e)
        }
        try {
            notebookViewModel.onPauseLifecycle()
        } catch (e: Throwable) {
            android.util.Log.e("Scribe", "Erro no onPause do Notebook", e)
        }
    }
}
