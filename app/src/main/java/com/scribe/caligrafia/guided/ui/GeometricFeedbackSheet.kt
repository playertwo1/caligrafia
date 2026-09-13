package com.scribe.caligrafia.guided.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.guided.model.FeedbackEvaluation
import com.scribe.caligrafia.ui.theme.ScribeBluePrimary
import com.scribe.caligrafia.ui.theme.ScribePaper
import com.scribe.caligrafia.ui.theme.ScribeSuccess
import com.scribe.caligrafia.ui.theme.ScribeSurfaceBorder
import com.scribe.caligrafia.ui.theme.ScribeTextMuted
import com.scribe.caligrafia.ui.theme.ScribeTextPrimary
import com.scribe.caligrafia.ui.theme.ScribeTextSecondary
import kotlin.math.roundToInt

/**
 * Modal de Feedback Geométrico Detalhado — "Entenda seu traço" (Fluxo 05 dos PNGs).
 *
 * Apresenta a análise determinística de inclinação canônica (52°), respeito às pautas,
 * ductus/direção e proximidade espacial, permitindo salvar no Alfabeto Pessoal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometricFeedbackSheet(
    evaluation: FeedbackEvaluation,
    symbolName: String,
    onRetry: () -> Unit,
    onAdvance: () -> Unit,
    onSaveToAlphabet: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val scoreColor = when {
        evaluation.scorePercent >= 85 -> ScribeSuccess
        evaluation.scorePercent >= 65 -> ScribeBluePrimary
        else -> Color(0xFFE11D48)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. Cabeçalho
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Entenda seu traço",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = ScribeTextPrimary
                    )
                    Text(
                        text = "Análise geométrica da letra '$symbolName'",
                        fontSize = 13.sp,
                        color = ScribeTextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = ScribeTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Banner Principal de Pontuação
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ScribePaper),
                border = BorderStroke(1.dp, ScribeSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(scoreColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${evaluation.scorePercent}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = evaluation.gradeBadge,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScribeTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (evaluation.isPassed) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ScribeSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = if (evaluation.isMastered) {
                                "Traço fluido e consistente com ângulo harmônico."
                            } else if (evaluation.isPassed) {
                                "Bom progresso! Pequenos ajustes nas pautas e inclinação recomendados."
                            } else {
                                "Requer atenção ao ângulo e aos limites da pauta."
                            },
                            fontSize = 12.sp,
                            color = ScribeTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Telemetria Detalhada dos Critérios (Flow 05)
            Text(
                text = "Detalhamento Geométrico",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScribeTextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Critério A: Inclinação Canônica (Slant 52°)
            val measuredSlant = evaluation.slant.measuredAngleDegrees?.roundToInt()
            val targetSlant = evaluation.slant.targetAngleDegrees.roundToInt()
            val slantScore = evaluation.slant.scorePercent

            MetricDetailCard(
                icon = Icons.Default.Straighten,
                title = "Inclinação (Slant)",
                badge = if (measuredSlant != null) "${measuredSlant}° (Alvo: ${targetSlant}°)" else "Não detectado",
                score = slantScore,
                description = evaluation.slant.feedback
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Critério B: Pautas Caligráficas
            MetricDetailCard(
                icon = Icons.Default.VerticalAlignBottom,
                title = "Limites de Pauta",
                badge = if (evaluation.guideline.isWithinBounds) "Dentro dos limites" else "Ultrapassou limite",
                score = evaluation.guideline.scorePercent,
                description = evaluation.guideline.feedback
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Critério C: Ordem & Sentido dos Traços (Ductus)
            MetricDetailCard(
                icon = Icons.Default.Timeline,
                title = "Ordem & Sentido do Ductus",
                badge = "${evaluation.direction.strokesCountReceived}/${evaluation.direction.strokesCountExpected} traços",
                score = evaluation.direction.scorePercent,
                description = evaluation.direction.feedback
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Ações Finais
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Salvar no Alfabeto Pessoal (R03/R04/R10)
                Button(
                    onClick = {
                        onSaveToAlphabet()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar no Meu Alfabeto", fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onRetry()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tentar de novo")
                    }

                    Button(
                        onClick = {
                            onAdvance()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Próxima")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricDetailCard(
    icon: ImageVector,
    title: String,
    badge: String,
    score: Int,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, ScribeSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = ScribeBluePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScribeTextPrimary)
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ScribeBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (score >= 70) ScribeSuccess else Color(0xFFF59E0B),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = ScribeTextSecondary
            )
        }
    }
}
