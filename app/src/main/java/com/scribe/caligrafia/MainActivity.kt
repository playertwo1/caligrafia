package com.scribe.caligrafia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.scribe.caligrafia.inspector.ui.InspectorScreen
import com.scribe.caligrafia.inspector.viewmodel.StylusLabViewModel
import com.scribe.caligrafia.notebook.ui.NotebookPracticeScreen
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import com.scribe.caligrafia.ui.theme.ScribeTheme

enum class ScribeScreen {
    NOTEBOOK,
    STYLUS_LAB
}

class MainActivity : ComponentActivity() {

    private val stylusLabViewModel: StylusLabViewModel by viewModels()
    private val notebookViewModel: NotebookPracticeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScribeTheme {
                var currentScreen by rememberSaveable { mutableStateOf(ScribeScreen.NOTEBOOK) }

                when (currentScreen) {
                    ScribeScreen.NOTEBOOK -> {
                        NotebookPracticeScreen(
                            viewModel = notebookViewModel,
                            onNavigateToLab = { currentScreen = ScribeScreen.STYLUS_LAB }
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
        stylusLabViewModel.onResumeLifecycle(this)
    }

    override fun onPause() {
        super.onPause()
        stylusLabViewModel.onPauseLifecycle()
    }
}
