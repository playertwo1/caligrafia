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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.scribe.caligrafia.ui.theme.ScribeBluePrimary
import com.scribe.caligrafia.ui.theme.ScribeTheme

/**
 * 4 Abas Canônicas de Navegação (conforme painel geral e docs/design/fluxos-v1/README.md).
 */
enum class ScribeTab(val title: String) {
    NOTEBOOK("Caderno"),
    PRACTICE("Praticar"),
    EVOLUTION("Evolução"),
    MORE("Mais")
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
                var currentTab by rememberSaveable { mutableStateOf(ScribeTab.NOTEBOOK) }
                var lastBackPressTime by remember { mutableStateOf(0L) }

                // Interceptador inteligente do gesto "Voltar":
                // Se estiver fora do Caderno, volta para o Caderno.
                // Se estiver no Caderno, exige toque duplo para sair.
                BackHandler {
                    if (currentTab != ScribeTab.NOTEBOOK) {
                        currentTab = ScribeTab.NOTEBOOK
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 2000L) {
                            finish()
                        } else {
                            lastBackPressTime = now
                            Toast.makeText(this@MainActivity, "Pressione voltar novamente para sair", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScribeTab.NOTEBOOK,
                                onClick = { currentTab = ScribeTab.NOTEBOOK },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = "Caderno") },
                                label = {
                                    Text(
                                        "Caderno",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == ScribeTab.NOTEBOOK) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ScribeBluePrimary,
                                    selectedTextColor = ScribeBluePrimary,
                                    indicatorColor = Color(0xFFEAF1FF)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == ScribeTab.PRACTICE,
                                onClick = { currentTab = ScribeTab.PRACTICE },
                                icon = { Icon(Icons.Default.Edit, contentDescription = "Praticar") },
                                label = {
                                    Text(
                                        "Praticar",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == ScribeTab.PRACTICE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ScribeBluePrimary,
                                    selectedTextColor = ScribeBluePrimary,
                                    indicatorColor = Color(0xFFEAF1FF)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == ScribeTab.EVOLUTION,
                                onClick = { currentTab = ScribeTab.EVOLUTION },
                                icon = { Icon(Icons.Default.ShowChart, contentDescription = "Evolução") },
                                label = {
                                    Text(
                                        "Evolução",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == ScribeTab.EVOLUTION) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ScribeBluePrimary,
                                    selectedTextColor = ScribeBluePrimary,
                                    indicatorColor = Color(0xFFEAF1FF)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == ScribeTab.MORE,
                                onClick = { currentTab = ScribeTab.MORE },
                                icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Mais") },
                                label = {
                                    Text(
                                        "Mais",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == ScribeTab.MORE) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ScribeBluePrimary,
                                    selectedTextColor = ScribeBluePrimary,
                                    indicatorColor = Color(0xFFEAF1FF)
                                )
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentTab) {
                            ScribeTab.NOTEBOOK -> {
                                NotebookPracticeScreen(
                                    viewModel = notebookViewModel,
                                    onNavigateToGuidedPractice = { currentTab = ScribeTab.PRACTICE }
                                )
                            }
                            ScribeTab.PRACTICE -> {
                                GuidedPracticeScreen(
                                    viewModel = guidedPracticeViewModel,
                                    onNavigateBack = { currentTab = ScribeTab.NOTEBOOK }
                                )
                            }
                            ScribeTab.EVOLUTION -> {
                                EvolutionScreen(
                                    viewModel = evolutionViewModel,
                                    onNavigateBack = { currentTab = ScribeTab.NOTEBOOK }
                                )
                            }
                            ScribeTab.MORE -> {
                                ExpansionsScreen(
                                    viewModel = expansionsViewModel,
                                    teacherViewModel = teacherViewModel,
                                    alphabetViewModel = alphabetViewModel,
                                    onBack = { currentTab = ScribeTab.NOTEBOOK },
                                    onNavigateToPracticeWithText = { currentTab = ScribeTab.PRACTICE },
                                    onNavigateToPractice = { targetId ->
                                        guidedPracticeViewModel.selectGlyphBySymbolOrId(targetId)
                                        currentTab = ScribeTab.PRACTICE
                                    },
                                    onNavigateToNotebookWithStyle = { styleId ->
                                        notebookViewModel.selectStyle(styleId, adaptPageGuidelines = true)
                                        currentTab = ScribeTab.NOTEBOOK
                                    }
                                )
                            }
                        }
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
        try {
            guidedPracticeViewModel.onResumeLifecycle()
        } catch (e: Throwable) {
            android.util.Log.e("Scribe", "Erro no onResume do GuidedPractice", e)
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
        try {
            guidedPracticeViewModel.onPauseLifecycle()
        } catch (e: Throwable) {
            android.util.Log.e("Scribe", "Erro no onPause do GuidedPractice", e)
        }
    }
}
