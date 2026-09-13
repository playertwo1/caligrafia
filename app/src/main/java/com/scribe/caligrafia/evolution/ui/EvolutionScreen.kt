package com.scribe.caligrafia.evolution.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.evolution.model.BeforeAfterComparison
import com.scribe.caligrafia.evolution.model.CalendarDayRecord
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionScreen(
    viewModel: EvolutionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = com.scribe.caligrafia.ui.theme.ScribeTextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "Sua Evolução",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            color = com.scribe.caligrafia.ui.theme.ScribeTextPrimary
                        )
                        Text(
                            text = "Histórico de escrita, consistência e comparação de traços",
                            fontSize = 11.sp,
                            color = com.scribe.caligrafia.ui.theme.ScribeTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Caderno",
                            tint = com.scribe.caligrafia.ui.theme.ScribeTextPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Seletor das 4 abas pedagógicas do M5
            ScrollableTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                edgePadding = 8.dp,
                containerColor = Color.White,
                contentColor = Color(0xFF2563EB)
            ) {
                EvolutionTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (uiState.activeTab) {
                EvolutionTab.CALENDAR -> CalendarTabContent(
                    monthDays = uiState.monthDays,
                    monthTitle = uiState.currentMonthTitle,
                    summary = uiState.summary
                )
                EvolutionTab.BEFORE_AFTER -> BeforeAfterTabContent(
                    comparisons = uiState.availableComparisons,
                    selected = uiState.selectedComparison,
                    onSelect = { viewModel.selectComparison(it) }
                )
                EvolutionTab.OVERLAY -> OverlayTabContent(
                    comparisons = uiState.availableComparisons,
                    selected = uiState.selectedComparison,
                    overlayAlpha = uiState.overlayAlpha,
                    onSelect = { viewModel.selectComparison(it) },
                    onAlphaChanged = { viewModel.setOverlayAlpha(it) }
                )
                EvolutionTab.DUAL_REPLAY -> DualReplayTabContent(
                    comparisons = uiState.availableComparisons,
                    selected = uiState.selectedComparison,
                    frame = uiState.dualReplayFrame,
                    onSelect = { viewModel.selectComparison(it) },
                    onPlay = { viewModel.playDualReplay() },
                    onPause = { viewModel.pauseDualReplay() },
                    onStop = { viewModel.stopDualReplay() },
                    onSeek = { viewModel.seekDualReplay(it) },
                    onSetSpeed = { viewModel.setDualReplaySpeed(it) }
                )
            }
        }
    }
}

/**
 * Aba 1: Calendário de Consistência e Métricas Não-Punitivas (SCR-504).
 */
@Composable
private fun CalendarTabContent(
    monthDays: List<CalendarDayRecord>,
    monthTitle: String,
    summary: com.scribe.caligrafia.evolution.model.EvolutionSummary
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card de Métricas Globais Não-Punitivas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consistência Acumulada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "Tempo Total", value = "${summary.totalMinutesPracticed} min")
                    MetricItem(label = "Sessões", value = "${summary.totalSessionsCompleted}")
                    MetricItem(label = "Dias Ativos", value = "${summary.activeDaysCount}")
                    MetricItem(label = "Ganho Médio", value = "+${summary.averageAccuracyGainPercent}%")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Sem streaks ou punições. Cada minuto dedicado consolida sua memória motora e seu progresso permanece protegido.",
                        fontSize = 11.sp,
                        color = Color(0xFF15803D)
                    )
                }
            }
        }

        // Grade do Calendário Mensal
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${monthDays.count { it.hasActivity }} dias de escrita",
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(monthDays) { day ->
                        val bgColor = when {
                            day.totalMinutesPracticed >= 20 -> Color(0xFF1D4ED8)
                            day.totalMinutesPracticed >= 10 -> Color(0xFF3B82F6)
                            day.totalMinutesPracticed > 0 -> Color(0xFF93C5FD)
                            else -> Color(0xFFF1F5F9)
                        }
                        val textColor = if (day.hasActivity) Color.White else Color(0xFF64748B)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${day.dayOfMonth}",
                                    fontSize = 12.sp,
                                    fontWeight = if (day.hasActivity) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                                if (day.hasActivity) {
                                    Text(
                                        text = "${day.totalMinutesPracticed}m",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvolutionEmptyState(
    title: String = "Nenhum comparativo disponível ainda",
    description: String = "Pratique exercícios no Treino Guiado para acompanhar sua evolução lado a lado com sua caligrafia real!",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Aba 2: Comparador Antes & Depois (SCR-501).
 */
@Composable
private fun BeforeAfterTabContent(
    comparisons: List<BeforeAfterComparison>,
    selected: BeforeAfterComparison?,
    onSelect: (String) -> Unit
) {
    if (comparisons.isEmpty()) {
        EvolutionEmptyState()
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seletor de Glifo/Exercício
        ComparisonTargetSelector(comparisons = comparisons, selectedId = selected?.targetId, onSelect = onSelect)

        if (selected != null) {
            // Comparação Lado a Lado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Painel Antes
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Antes (Inicial)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFE11D48)
                        )
                        Text(
                            text = formatDate(selected.beforeAttempt.timestampMs),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        StrokePreviewCanvas(
                            strokes = selected.beforeAttempt.strokes,
                            strokeColor = Color(0xFFE11D48),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Nota: ${selected.beforeAttempt.scorePercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = "Ângulo: ${selected.beforeAttempt.averageSlantDegrees.toInt()}°", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                // Painel Depois
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Depois (Atual)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2563EB)
                        )
                        Text(
                            text = formatDate(selected.afterAttempt.timestampMs),
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        StrokePreviewCanvas(
                            strokes = selected.afterAttempt.strokes,
                            strokeColor = Color(0xFF2563EB),
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Nota: ${selected.afterAttempt.scorePercent}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669))
                        Text(text = "Ângulo: ${selected.afterAttempt.averageSlantDegrees.toInt()}°", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // Card de Ganhos Objetivos
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Deltas de Evolução",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E3A8A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ganho de Precisão: +${selected.scoreGainPercent}%",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Correção de Slant: +${String.format(Locale.US, "%.1f", selected.slantImprovementDegrees)}°",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selected.summaryInsight,
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }
    }
}

/**
 * Aba 3: Sobreposição com Slider de Transparência (SCR-502).
 */
@Composable
private fun OverlayTabContent(
    comparisons: List<BeforeAfterComparison>,
    selected: BeforeAfterComparison?,
    overlayAlpha: Float,
    onSelect: (String) -> Unit,
    onAlphaChanged: (Float) -> Unit
) {
    if (comparisons.isEmpty()) {
        EvolutionEmptyState()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ComparisonTargetSelector(comparisons = comparisons, selectedId = selected?.targetId, onSelect = onSelect)

        if (selected != null) {
            // Canvas de Sobreposição Vetorial
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sobreposição: ${selected.targetTitle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendDot(color = Color(0xFFE11D48), label = "Antes")
                            LegendDot(color = Color(0xFF2563EB), label = "Depois")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawGuidelineBackground(size.width, size.height, 52f)
                        // Traço Antes (Coral) com transparência modulada pelo slider
                        drawStrokeSequence(
                            strokes = selected.beforeAttempt.strokes,
                            overrideColor = Color(0xFFE11D48),
                            alpha = (1.0f - overlayAlpha).coerceIn(0.15f, 1f)
                        )
                        // Traço Depois (Azul) com transparência modulada pelo slider
                        drawStrokeSequence(
                            strokes = selected.afterAttempt.strokes,
                            overrideColor = Color(0xFF2563EB),
                            alpha = overlayAlpha.coerceIn(0.15f, 1f)
                        )
                    }
                }
            }

            // Controle do Slider de Transparência
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "100% Antes", fontSize = 11.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                        Text(text = "Mistura (50%)", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(text = "100% Depois", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = overlayAlpha,
                        onValueChange = onAlphaChanged,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Aba 4: Dual Replay Sincronizado Lado a Lado (SCR-503).
 */
@Composable
private fun DualReplayTabContent(
    comparisons: List<BeforeAfterComparison>,
    selected: BeforeAfterComparison?,
    frame: com.scribe.caligrafia.evolution.engine.DualReplayFrame,
    onSelect: (String) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    onSetSpeed: (ReplaySpeed) -> Unit
) {
    if (comparisons.isEmpty()) {
        EvolutionEmptyState()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ComparisonTargetSelector(comparisons = comparisons, selectedId = selected?.targetId, onSelect = onSelect)

        if (selected != null) {
            // Telas de Replay Lado a Lado
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Replay Track A (Antes)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Antes (Inicial)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE11D48))
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawGuidelineBackground(size.width, size.height, 52f)
                            drawStrokeSequence(frame.visibleStrokesA, overrideColor = Color(0xFFE11D48))
                        }
                    }
                }

                // Replay Track B (Depois)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = "Depois (Atual)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawGuidelineBackground(size.width, size.height, 52f)
                            drawStrokeSequence(frame.visibleStrokesB, overrideColor = Color(0xFF2563EB))
                        }
                    }
                }
            }

            // Controles de Transporte de Replay
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Slider(
                        value = frame.progress,
                        onValueChange = onSeek,
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = if (frame.isPlaying) onPause else onPlay) {
                                Icon(
                                    imageVector = if (frame.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color(0xFF2563EB)
                                )
                            }
                            IconButton(onClick = onStop) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }

                        // Seletor de Velocidades (0.5x, 1x, 2x)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReplaySpeed.entries.forEach { speed ->
                                FilterChip(
                                    selected = frame.speed == speed,
                                    onClick = { onSetSpeed(speed) },
                                    label = { Text(speed.displayName, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonTargetSelector(
    comparisons: List<BeforeAfterComparison>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        comparisons.forEach { comp ->
            FilterChip(
                selected = comp.targetId == selectedId,
                onClick = { onSelect(comp.targetId) },
                label = { Text(comp.targetTitle, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun StrokePreviewCanvas(
    strokes: List<Stroke>,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawGuidelineBackground(size.width, size.height, 52f)
        drawStrokeSequence(strokes, overrideColor = strokeColor)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2563EB))
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
    }
}

private fun DrawScope.drawGuidelineBackground(width: Float, height: Float, slantAngleDeg: Float = 52.0f) {
    val baselineY = height * 0.65f
    val xHeightY = height * 0.45f
    val ascenderY = height * 0.20f
    val descenderY = height * 0.85f

    val guideColor = Color(0xFFE2E8F0)
    val baseColor = Color(0xFFCBD5E1)

    drawLine(guideColor, Offset(0f, ascenderY), Offset(width, ascenderY), strokeWidth = 1f)
    drawLine(guideColor, Offset(0f, xHeightY), Offset(width, xHeightY), strokeWidth = 1f)
    drawLine(baseColor, Offset(0f, baselineY), Offset(width, baselineY), strokeWidth = 1.5f)
    drawLine(guideColor, Offset(0f, descenderY), Offset(width, descenderY), strokeWidth = 1f)

    val rad = Math.toRadians(slantAngleDeg.toDouble())
    val tanTheta = kotlin.math.tan(rad).toFloat()
    val deltaX = (descenderY - ascenderY) / tanTheta
    val slantColor = Color(0xFFF1F5F9)

    var currentX = 20f
    while (currentX < width + deltaX) {
        drawLine(slantColor, Offset(currentX, descenderY), Offset(currentX - deltaX, ascenderY), strokeWidth = 1f)
        currentX += 45f
    }
}

private fun DrawScope.drawStrokeSequence(
    strokes: List<Stroke>,
    overrideColor: Color? = null,
    alpha: Float = 1.0f
) {
    if (strokes.isEmpty()) return

    val allPts = strokes.flatMap { it.points }
    if (allPts.isEmpty()) return

    val minX = allPts.minOf { it.x }
    val maxX = allPts.maxOf { it.x }
    val minY = allPts.minOf { it.y }
    val maxY = allPts.maxOf { it.y }
    val strokeW = (maxX - minX).coerceAtLeast(1f)
    val strokeH = (maxY - minY).coerceAtLeast(1f)

    // Escala e centraliza preservando o aspecto do traço dentro do preview
    val padding = 16f
    val availableW = (size.width - 2 * padding).coerceAtLeast(1f)
    val availableH = (size.height - 2 * padding).coerceAtLeast(1f)

    val shouldScale = strokeW > size.width || strokeH > size.height || minX < 0f || minY < 0f || maxX > size.width || maxY > size.height
    val scale = if (shouldScale) {
        minOf(availableW / strokeW, availableH / strokeH).coerceAtMost(1.0f)
    } else 1.0f

    val offsetX = if (shouldScale) padding + (availableW - strokeW * scale) * 0.5f - minX * scale else 0f
    val offsetY = if (shouldScale) padding + (availableH - strokeH * scale) * 0.5f - minY * scale else 0f

    fun transform(x: Float, y: Float): Offset {
        return if (shouldScale) Offset(x * scale + offsetX, y * scale + offsetY) else Offset(x, y)
    }

    for (stroke in strokes) {
        val strokeColor = overrideColor ?: Color(stroke.color ?: android.graphics.Color.BLACK)
        val baseWidth = (stroke.baseWidthPx ?: 5.0f) * scale

        // R13: Preservação de pontos isolados (pingos no 'i', acentos, pontuações)
        if (stroke.points.size == 1) {
            val p = stroke.points[0]
            val width = (baseWidth * (p.pressure ?: 0.5f) * 1.5f).coerceAtLeast(2.0f)
            val mapped = transform(p.x, p.y)
            drawCircle(
                color = strokeColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = width * 0.5f,
                center = mapped
            )
            continue
        }

        for (i in 0 until stroke.points.size - 1) {
            val p1 = stroke.points[i]
            val p2 = stroke.points[i + 1]
            val width = (baseWidth * (p1.pressure ?: 0.5f) * 1.5f).coerceAtLeast(1.5f)
            val mapped1 = transform(p1.x, p1.y)
            val mapped2 = transform(p2.x, p2.y)
            drawLine(
                color = strokeColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                start = mapped1,
                end = mapped2,
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun formatDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return sdf.format(Date(timestampMs))
}
