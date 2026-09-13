package com.scribe.caligrafia.teacher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.teacher.model.BiomechanicalDimension
import com.scribe.caligrafia.teacher.model.DimensionEvaluation
import com.scribe.caligrafia.teacher.model.EvaluationStatus
import com.scribe.caligrafia.teacher.model.InsightType
import com.scribe.caligrafia.teacher.model.PrescribedPracticeSession
import com.scribe.caligrafia.teacher.model.TeacherInsight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    viewModel: TeacherViewModel,
    onBack: () -> Unit,
    onStartPractice: (exerciseId: String) -> Unit,
    onStartPrescribedPractice: ((PrescribedPracticeSession) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Professor IA & Coaching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Diagnóstico Biomecânico & Treino Personalizado (M7)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.reanalyzeAllData() },
                        enabled = !state.isAnalyzing
                    ) {
                        if (state.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reavaliar Dados"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val diagnostic = state.diagnostic
            val prescription = state.prescription
            val scrollState = rememberScrollState()
            var selectedEvidenceInsight by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<TeacherInsight?>(null)
            }

            // Diálogo de Evidência da Observação (F4.03)
            selectedEvidenceInsight?.let { insight ->
                InsightEvidenceDialog(
                    insight = insight,
                    onDismiss = { selectedEvidenceInsight = null }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner de Erro com Tentar Novamente (F4.05)
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Erro no Diagnóstico",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = error,
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                            Button(
                                onClick = { viewModel.reanalyzeAllData() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Tentar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 1. Estado Vazio Transparente (F4.01) ou Card de Maturidade
                if (diagnostic == null || diagnostic.totalAttemptsAnalyzed == 0) {
                    TeacherEmptyCard(
                        onStartFirstPractice = { onStartPractice("basic_slant") }
                    )
                } else {
                    MaturityHeaderCard(diagnostic = diagnostic)
                }

                // 2. Card de Treino Prescrito pelo Professor (F4.06, F4.07, F4.08)
                prescription?.let { presc ->
                    PrescriptionCard(
                        prescription = presc,
                        isCatalogValid = state.isPrescriptionValid,
                        onStartPractice = {
                            if (onStartPrescribedPractice != null) {
                                onStartPrescribedPractice(presc)
                            } else {
                                onStartPractice(presc.focusExerciseId)
                            }
                        }
                    )
                }

                // 3. Seção das 4 Dimensões Biomecânicas (F4.01: sem valores inventados)
                diagnostic?.let { diag ->
                    Text(
                        text = "Dimensões Biomecânicas do Traço",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    diag.dimensions.forEach { (dim, eval) ->
                        val isSelected = state.selectedDimension == dim
                        DimensionCard(
                            evaluation = eval,
                            isSelected = isSelected,
                            onClick = { viewModel.selectDimension(dim) }
                        )
                    }
                }

                // 4. Seção de Insights Pedagógicos com "Ver detalhe" (F4.03)
                if (state.insights.isNotEmpty()) {
                    Text(
                        text = "Observações e Dicas do Mestre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    state.insights.forEach { insight ->
                        InsightCard(
                            insight = insight,
                            onViewDetail = { selectedEvidenceInsight = insight }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MaturityHeaderCard(
    diagnostic: com.scribe.caligrafia.teacher.model.BiomechanicalDiagnostic
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Maturidade Caligráfica",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = diagnostic.maturityLevel.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%.0f".format(diagnostic.overallScore),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = "Análise calculada a partir de ${diagnostic.totalAttemptsAnalyzed} sessões e ${diagnostic.totalStrokesAnalyzed} traços vetoriais brutos gravados no S25 Ultra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                diagnostic.primaryStrength?.let { str ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF16A34A).copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "★ Forte: ${str.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                diagnostic.primaryWeakness?.let { wkn ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD97706).copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "▲ Foco: ${wkn.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherEmptyCard(
    onStartFirstPractice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Professor IA Aguardando Primeiro Treino",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "O Professor avalia suas dimensões motoras (inclinação, contenção de pauta, cadência e modulação de pressão) exclusivamente a partir de traços reais de caneta sem inventar valores. Complete sua primeira sessão prática para gerar o diagnóstico biomecânico.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color(0xFF64748B),
                lineHeight = 20.sp
            )

            Button(
                onClick = onStartFirstPractice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fazer Primeiro Treino", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    prescription: PrescribedPracticeSession,
    isCatalogValid: Boolean,
    onStartPractice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Treino Prescrito pelo Professor",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = prescription.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = prescription.rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(label = "Duração", value = "${prescription.recommendedMinutes} min")
                InfoBadge(label = "Aquecimento", value = prescription.warmupExerciseId)
                InfoBadge(label = "Foco", value = prescription.focusExerciseId)
                InfoBadge(label = "Ghost Opacidade", value = "${(prescription.recommendedGhostLevel * 100).toInt()}%")
            }

            // Fases / Séries reais (F4.06)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "📋 ${prescription.seriesCount} séries: ${prescription.stages.joinToString(" → ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🎯 Meta: ${prescription.targetGoalDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Validação de catálogo (F4.07)
            if (!isCatalogValid) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⚠️ O exercício prescrito (${prescription.focusExerciseId}) não foi encontrado no catálogo canônico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Button(
                onClick = onStartPractice,
                enabled = isCatalogValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCatalogValid) "Iniciar Treino com o Professor" else "Exercício Indisponível",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DimensionCard(
    evaluation: DimensionEvaluation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (evaluation.status) {
        EvaluationStatus.EXCELLENT -> Color(0xFF16A34A)
        EvaluationStatus.GOOD -> Color(0xFF2563EB)
        EvaluationStatus.NEEDS_ATTENTION -> Color(0xFFD97706)
        EvaluationStatus.CRITICAL -> Color(0xFFDC2626)
        EvaluationStatus.INSUFFICIENT_DATA -> Color(0xFF6B7280)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(statusColor)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = evaluation.dimension.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (evaluation.status == EvaluationStatus.INSUFFICIENT_DATA) {
                            evaluation.status.label
                        } else {
                            "${evaluation.status.label} (%.0f%%)".format(evaluation.score)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (evaluation.score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.2f)
            )

            if (isSelected) {
                Text(
                    text = evaluation.shortDiagnosis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: TeacherInsight,
    onViewDetail: () -> Unit
) {
    val (iconColor, containerColor, iconVector) = when (insight.type) {
        InsightType.PRAISE -> Triple(Color(0xFF16A34A), Color(0xFF16A34A).copy(alpha = 0.1f), Icons.Default.CheckCircle)
        InsightType.CORRECTION -> Triple(Color(0xFFD97706), Color(0xFFD97706).copy(alpha = 0.1f), Icons.Default.Warning)
        InsightType.ERGONOMIC_TIP -> Triple(Color(0xFF2563EB), Color(0xFF2563EB).copy(alpha = 0.1f), Icons.Default.Edit)
        InsightType.CHALLENGE -> Triple(Color(0xFF9333EA), Color(0xFF9333EA).copy(alpha = 0.1f), Icons.Default.Star)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )

                    insight.metricDelta?.let { delta ->
                        Text(
                            text = delta,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }

                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Botão "Ver detalhe" quando vinculado a uma tentativa real (F4.03)
                if (insight.relatedAttemptId != null || insight.relatedTargetTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Ver detalhe",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightEvidenceDialog(
    insight: TeacherInsight,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        },
        title = {
            Text(
                text = "Evidência da Avaliação",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "A observação pedagógica '${insight.title}' é sustentada pela seguinte tentativa registrada:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                insight.relatedTargetTitle?.let { title ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Exercício:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(title, fontSize = 12.sp)
                    }
                }

                insight.relatedScore?.let { score ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Nota Real Obtida:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("$score%", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), fontSize = 12.sp)
                    }
                }

                insight.metricDelta?.let { delta ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Métrica Observada:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(delta, fontSize = 12.sp)
                    }
                }

                insight.relatedAttemptId?.let { id ->
                    Text(
                        text = "ID da Tentativa: ${id.take(12)}...",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}
