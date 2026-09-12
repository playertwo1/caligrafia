package com.scribe.caligrafia.guided.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.guided.model.FeedbackEvaluation
import com.scribe.caligrafia.guided.model.GhostModeLevel
import com.scribe.caligrafia.guided.model.PracticeStage
import com.scribe.caligrafia.guided.model.ReferenceGlyph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedPracticeScreen(
    viewModel: GuidedPracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var canvasViewRef by remember { mutableStateOf<GuidedPracticeCanvasView?>(null) }
    var showGlyphMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.selectedGlyph.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.selectedGlyph.category.displayName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Text(
                            text = "Estilo: ${state.currentStyle.name} (${state.currentStyle.defaultSlantAngle.toInt()}°)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Caderno"
                        )
                    }
                },
                actions = {
                    // Botão seletor de estilo (M3)
                    Box {
                        OutlinedButton(
                            onClick = { showStyleMenu = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(text = "Estilo: ${state.currentStyle.name}")
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

                    // Botão seletor de exercício
                    Box {
                        OutlinedButton(
                            onClick = { showGlyphMenu = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(text = "Glifo: ${state.selectedGlyph.symbol}")
                        }
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

                    // Ações de desenho
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = state.strokeCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Desfazer"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.clearAttempt() },
                        enabled = state.strokeCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpar Tentativa"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Seletor de Estágios Pedagógicos (Cobrir -> Copiar -> Sozinho)
            TabRow(
                selectedTabIndex = state.currentStage.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                PracticeStage.entries.forEach { stage ->
                    Tab(
                        selected = state.currentStage == stage,
                        onClick = { viewModel.selectStage(stage) },
                        text = {
                            Text(
                                text = "${stage.stepNumber}. ${stage.title}",
                                fontWeight = if (state.currentStage == stage) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 2. Barra de Ghost Mode e Instruções
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Instrução pedagógica do glifo
                Text(
                    text = state.selectedGlyph.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Chips de Ghost Mode (apenas relevante no modo Cobrir)
                if (state.currentStage == PracticeStage.TRACE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ghost:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        GhostModeLevel.entries.forEach { level ->
                            FilterChip(
                                selected = state.ghostModeLevel == level,
                                onClick = { viewModel.setGhostMode(level) },
                                label = { Text("${level.percentageLabel} (${level.title})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3F51B5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Canvas de Escrita Nativo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                // Botão flutuante para Avaliar Traço quando houver traços capturados e sem avaliação aberta
                if (state.strokeCount > 0 && state.evaluation == null) {
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
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)) // Green 600
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificar Caligrafia", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Painel de Feedback Determinístico (quando avaliado)
            val evaluation = state.evaluation
            if (evaluation != null) {
                FeedbackCard(
                    evaluation = evaluation,
                    onRetry = { viewModel.clearAttempt() },
                    onAdvance = { viewModel.advanceProgress() }
                )
            }
        }
    }
}

@Composable
fun FeedbackCard(
    evaluation: FeedbackEvaluation,
    onRetry: () -> Unit,
    onAdvance: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabeçalho da avaliação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    evaluation.scorePercent >= 85 -> Color(0xFF16A34A) // Verde
                                    evaluation.scorePercent >= 65 -> Color(0xFF2563EB) // Azul
                                    else -> Color(0xFFE11D48) // Vermelho
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${evaluation.scorePercent}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = evaluation.gradeBadge,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (evaluation.isPassed) "Apto para avançar!" else "Requer mais treino e ajuste.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row {
                    OutlinedButton(onClick = onRetry) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Repetir")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAdvance,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                    ) {
                        Text("Avançar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detalhamento das métricas
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("• ${evaluation.guideline.feedback}", fontSize = 12.sp, color = Color(0xFF334155))
                Text("• ${evaluation.slant.feedback}", fontSize = 12.sp, color = Color(0xFF334155))
                Text("• ${evaluation.direction.feedback}", fontSize = 12.sp, color = Color(0xFF334155))
                Text("• ${evaluation.proximity.feedback}", fontSize = 12.sp, color = Color(0xFF334155))
            }
        }
    }
}
