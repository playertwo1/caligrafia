package com.scribe.caligrafia.guided.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.scribe.caligrafia.learning.ui.LearningHubScreen
import com.scribe.caligrafia.learning.ui.LearningViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import com.scribe.caligrafia.ui.theme.ScribeBluePrimary
import com.scribe.caligrafia.ui.theme.ScribePaper
import com.scribe.caligrafia.ui.theme.ScribeSuccess
import com.scribe.caligrafia.ui.theme.ScribeSurfaceBorder
import com.scribe.caligrafia.ui.theme.ScribeTextMuted
import com.scribe.caligrafia.ui.theme.ScribeTextPrimary
import com.scribe.caligrafia.ui.theme.ScribeTextSecondary

/**
 * Tela de Treino Guiado de Caligrafia (Fluxo 04 & Fluxo 05 dos PNGs).
 *
 * Características:
 * - Sessão ativa com cronômetro real (R01/R08) e pausável.
 * - Barra de Ghost Mode de opacidade progressiva: 100% -> 70% -> 40% -> 10% -> 0%.
 * - Instrução pedagógica do ductus e anatomia da letra.
 * - Avaliação geométrica determinística acoplada à folha de feedback detalhado (Fluxo 05).
 * - Integração direta com salvamento no Alfabeto Pessoal (R03/R04/R10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedPracticeScreen(
    viewModel: GuidedPracticeViewModel,
    learningViewModel: LearningViewModel? = null,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var canvasViewRef by remember { mutableStateOf<GuidedPracticeCanvasView?>(null) }
    var showGlyphMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLearningHub by rememberSaveable { mutableStateOf(false) }

    val learningUiState = learningViewModel?.uiState?.collectAsState()?.value
    val activeCurriculumSession = learningUiState?.activeSession

    if (showLearningHub && learningViewModel != null) {
        LearningHubScreen(
            viewModel = learningViewModel,
            onNavigateBack = { showLearningHub = false },
            onNavigateToPractice = { targetId ->
                viewModel.selectGlyphBySymbolOrId(targetId)
                showLearningHub = false
            }
        )
        return
    }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedbackMessage()
        }
    }

    // Abre automaticamente a folha de feedback quando uma avaliação for concluída
    LaunchedEffect(state.evaluation) {
        if (state.evaluation != null) {
            showFeedbackSheet = true
        }
    }

    // Folha de Feedback Geométrico — "Entenda seu traço" (Fluxo 05)
    if (showFeedbackSheet && state.evaluation != null) {
        GeometricFeedbackSheet(
            evaluation = state.evaluation!!,
            symbolName = state.selectedGlyph.symbol,
            onRetry = {
                viewModel.clearAttempt()
                showFeedbackSheet = false
            },
            onAdvance = {
                viewModel.advanceProgress()
                showFeedbackSheet = false
            },
            onSaveToAlphabet = {
                viewModel.saveAttemptToPersonalAlphabet()
            },
            onDismiss = {
                showFeedbackSheet = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = ScribeTextPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Caderno",
                            tint = ScribeTextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showGlyphMenu = true }
                        ) {
                            Text(
                                text = "Treinando: '${state.selectedGlyph.symbol}'",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = ScribeTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Trocar letra",
                                tint = ScribeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${state.currentStyle.name} • ${state.guidelineConfig.slant?.angleDegrees?.toInt() ?: 52}°",
                            fontSize = 11.sp,
                            color = ScribeTextMuted
                        )
                    }
                },
                actions = {
                    // Pílula do Cronômetro da Sessão Ativa (R01/R08)
                    val minutes = state.elapsedSeconds / 60
                    val seconds = state.elapsedSeconds % 60
                    val timerFormatted = "%02d:%02d".format(minutes, seconds)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isTimerRunning) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (state.isTimerRunning) Color(0xFFBFDBFE) else ScribeSurfaceBorder),
                        modifier = Modifier
                            .clickable { viewModel.toggleTimer() }
                            .padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Pausar / Retomar sessão",
                                tint = if (state.isTimerRunning) ScribeBluePrimary else ScribeTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = timerFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isTimerRunning) ScribeBluePrimary else ScribeTextMuted
                            )
                        }
                    }

                    // Seletor de Glifo / Letra Dropdown
                    Box {
                        DropdownMenu(
                            expanded = showGlyphMenu,
                            onDismissRequest = { showGlyphMenu = false }
                        ) {
                            state.availableGlyphs.forEach { glyph ->
                                DropdownMenuItem(
                                    text = { Text("${glyph.symbol} — ${glyph.name}") },
                                    onClick = {
                                        viewModel.selectGlyph(glyph)
                                        showGlyphMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Seletor de Estilo Dropdown
                    Box {
                        IconButton(onClick = { showStyleMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Estilos",
                                tint = ScribeTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showStyleMenu,
                            onDismissRequest = { showStyleMenu = false }
                        ) {
                            state.availableStyles.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text("${style.name} (${style.defaultSlantAngle.toInt()}°)") },
                                    onClick = {
                                        viewModel.selectStyle(style.id)
                                        showStyleMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Botão Aulas e Currículo M4
                    if (learningViewModel != null) {
                        IconButton(onClick = { showLearningHub = true }) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Aulas e Currículo (M4)",
                                tint = if (activeCurriculumSession != null) ScribeBluePrimary else ScribeTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            // Banner de Sessão Ativa do Currículo M4 (se houver aula em andamento)
            if (activeCurriculumSession != null) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLearningHub = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = ScribeBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Aula M4: ${activeCurriculumSession.lesson.title} • ${activeCurriculumSession.currentPhase.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScribeBluePrimary
                            )
                        }
                        Text(
                            text = "${activeCurriculumSession.phaseElapsedSeconds}s / ${activeCurriculumSession.phaseTotalSeconds}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScribeBluePrimary
                        )
                    }
                }
            }

            // 1. Barra de Opacidade do Modelo (Ghost Mode - 100% a 0% do Flow 04)
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assistência Visual (Ghost Mode):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScribeTextPrimary
                        )
                        Text(
                            text = "${state.ghostModeLevel.percentageLabel} (${state.ghostModeLevel.title})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScribeBluePrimary
                        )
                    }

                    // Botões de Nível de Opacidade (Flow 04)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GhostModeLevel.values().forEach { level ->
                            val isSelected = state.ghostModeLevel == level
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGhostMode(level) },
                                label = {
                                    Text(
                                        text = "${level.percentageLabel} ${level.title}",
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScribeBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. Instrução do Ductus
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = state.selectedGlyph.instructions,
                    fontSize = 12.sp,
                    color = ScribeTextSecondary
                )
            }

            // 3. Canvas de Escrita Nativo Central
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ScribeSurfaceBorder, RoundedCornerShape(8.dp))
                    .shadow(2.dp, RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        GuidedPracticeCanvasView(
                            context = ctx,
                            pipeline = viewModel.pipeline,
                            strokeRepository = viewModel.strokeRepository,
                            onStrokeChanged = { viewModel.notifyStrokeChanged() }
                        ).also { view ->
                            canvasViewRef = view
                            view.guidelineConfig = state.guidelineConfig
                            view.currentGlyph = state.selectedGlyph
                            view.currentStage = state.currentStage
                            view.ghostLevel = state.ghostModeLevel
                        }
                    },
                    update = { view ->
                        canvasViewRef = view
                        view.guidelineConfig = state.guidelineConfig
                        view.currentGlyph = state.selectedGlyph
                        view.currentStage = state.currentStage
                        view.ghostLevel = state.ghostModeLevel
                        view.invalidate()
                    }
                )
            }

            // 4. Barra de Ações Inferior (Desfazer, Limpar, Analisar traço)
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = state.strokeCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Desfazer",
                                tint = if (state.strokeCount > 0) ScribeTextPrimary else Color(0xFFCBD5E1)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearAttempt() },
                            enabled = state.strokeCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Limpar",
                                tint = if (state.strokeCount > 0) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Botão Principal: Analisar Traço (Flow 04 -> Flow 05)
                    Button(
                        onClick = {
                            val view = canvasViewRef
                            if (view != null) {
                                val params = view.getActiveBandAndOrigin()
                                if (params != null) {
                                    viewModel.evaluateCurrentAttempt(
                                        band = params.first,
                                        originX = params.second,
                                        glyphWidthPx = params.third,
                                        slant = view.guidelineConfig.slant
                                    )
                                    showFeedbackSheet = true
                                }
                            }
                        },
                        enabled = state.strokeCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.strokeCount > 0) "Analisar traço" else "Escreva para analisar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
