package com.scribe.caligrafia.learning.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.learning.model.CurriculumLesson
import com.scribe.caligrafia.learning.model.CurriculumStage
import com.scribe.caligrafia.learning.session.ActiveSessionState
import com.scribe.caligrafia.learning.session.SessionDuration
import com.scribe.caligrafia.learning.session.SessionPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningHubScreen(
    viewModel: LearningViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    if (uiState.isSessionDialogVisible && uiState.activeSession != null) {
        ActiveSessionDialog(
            session = uiState.activeSession!!,
            onPause = { viewModel.pauseSession() },
            onResume = { viewModel.resumeSession() },
            onSkipPhase = { viewModel.skipToNextPhase() },
            onRecordAttempt = { score -> viewModel.recordAttempt(score) },
            onFinishAndSave = { viewModel.finishAndSaveSession() },
            onCancel = { viewModel.cancelSession() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = "Trilha de Aprendizado (M4)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sessões deliberadas, retenção SRS e consistência",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Card de Progresso Não-Punitivo (SCR-404)
            ProgressOverviewCard(summary = uiState.progressSummary)

            // 2. Card de Recomendação Diária (SRS Determinístico - SCR-403)
            DailyRecommendationCard(
                recommendation = uiState.dailyRecommendation,
                selectedDuration = uiState.selectedDuration,
                onSelectDuration = { viewModel.selectDuration(it) },
                onStartSession = { lesson, duration -> viewModel.startSession(lesson, duration) }
            )

            // 3. Trilha Curricular Completa (18 Lições em 5 Estágios - SCR-401)
            CurriculumStageSelector(
                selectedStage = uiState.selectedStage,
                onSelectStage = { viewModel.selectStage(it) }
            )

            val stageLessons = uiState.allLessons.filter { it.stage == uiState.selectedStage }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stageLessons.forEach { lesson ->
                    CurriculumLessonCard(
                        lesson = lesson,
                        onStartLesson = { viewModel.startSession(lesson, uiState.selectedDuration) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Card com métricas acumulativas de prática sem punição de streaks.
 */
@Composable
private fun ProgressOverviewCard(summary: com.scribe.caligrafia.learning.history.LearningProgressSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seu Progresso Caligráfico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Não-punitivo",
                    fontSize = 11.sp,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetricItem(label = "Prática Total", value = "${summary.totalMinutesPracticed} min")
                StatMetricItem(label = "Sessões Feitas", value = "${summary.totalSessionsCompleted}")
                StatMetricItem(label = "Lições Únicas", value = "${summary.uniqueLessonsPracticed}/18")
                StatMetricItem(label = "Dias Ativos (30d)", value = "${summary.daysActiveLast30Days}")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "“Cada minuto de escrita deliberada fortalece sua memória muscular.”",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun StatMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF2563EB)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Card de recomendação adaptada pelo algoritmo de repetição espaçada (SCR-403).
 */
@Composable
private fun DailyRecommendationCard(
    recommendation: com.scribe.caligrafia.learning.review.DailyRecommendation?,
    selectedDuration: SessionDuration,
    onSelectDuration: (SessionDuration) -> Unit,
    onStartSession: (CurriculumLesson, SessionDuration) -> Unit
) {
    if (recommendation == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recomendação do Dia (SRS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E3A8A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recommendation.reason,
                fontSize = 12.sp,
                color = Color(0xFF1E40AF)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Foco: ${recommendation.focusLesson.title}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = recommendation.focusLesson.description,
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )

            if (recommendation.reviewLesson != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Revisão sugerida: ${recommendation.reviewLesson.title}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Escolha a duração:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(4.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SessionDuration.entries.forEach { duration ->
                    FilterChip(
                        selected = duration == selectedDuration,
                        onClick = { onSelectDuration(duration) },
                        label = { Text(duration.label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onStartSession(recommendation.focusLesson, selectedDuration) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Iniciar Treino Deliberado (${selectedDuration.minutes} min)")
            }
        }
    }
}

/**
 * Seletor de estágios pedagógicos do currículo (SCR-401).
 */
@Composable
private fun CurriculumStageSelector(
    selectedStage: CurriculumStage,
    onSelectStage: (CurriculumStage) -> Unit
) {
    Column {
        Text(
            text = "Trilha Pedagógica (5 Estágios)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedStage.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.White,
            contentColor = Color(0xFF2563EB)
        ) {
            CurriculumStage.entries.forEach { stage ->
                Tab(
                    selected = selectedStage == stage,
                    onClick = { onSelectStage(stage) },
                    text = {
                        Text(
                            text = "${stage.stageNumber}. ${stage.title}",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStage == stage) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}

/**
 * Card individual de lição curricular com metadados de estilo e duração.
 */
@Composable
private fun CurriculumLessonCard(
    lesson: CurriculumLesson,
    onStartLesson: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lesson.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = lesson.targetStyleId.replace("_", " ").uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lesson.description,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            if (lesson.targetText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Texto alvo: “${lesson.targetText}”",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F766E)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sugerido: ${lesson.recommendedMinutes} min",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                OutlinedButton(
                    onClick = onStartLesson,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Treinar", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Diálogo de sessão ativa cronometrada com controle por fases pedagógicas (SCR-402).
 */
@Composable
private fun ActiveSessionDialog(
    session: ActiveSessionState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkipPhase: () -> Unit,
    onRecordAttempt: (Int) -> Unit,
    onFinishAndSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column {
                Text(
                    text = "Sessão Deliberada: ${session.lesson.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Duração Total: ${session.duration.minutes} min",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Indicador de Fase Pedagógica
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fase ${session.currentPhase.phaseIndex}/5: ${session.currentPhase.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B)
                            )
                            val remainingSec = session.phaseRemainingSeconds
                            Text(
                                text = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2563EB)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = session.currentPhase.description,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { session.phaseProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF2563EB),
                            trackColor = Color(0xFFCBD5E1)
                        )
                    }
                }

                // Progresso Global da Sessão
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tempo Total Restante:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    val totalRemainingSec = session.totalRemainingSeconds
                    Text(
                        text = String.format("%02d:%02d", totalRemainingSec / 60, totalRemainingSec % 60),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                LinearProgressIndicator(
                    progress = { session.sessionProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFE2E8F0)
                )

                // Registro de Tentativas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tentativas: ${session.attemptsCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Média: ${session.averageScore?.let { "${it.toInt()}%" } ?: "—"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Avaliar tentativa de traço:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRecordAttempt(65) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("65%", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onRecordAttempt(80) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("80%", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { onRecordAttempt(95) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("95%", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Controles de Pausa e Avanço
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = if (session.isPaused) onResume else onPause,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (session.isPaused) "Retomar" else "Pausar", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onSkipPhase,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Próx. Fase", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onFinishAndSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("Finalizar e Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}
