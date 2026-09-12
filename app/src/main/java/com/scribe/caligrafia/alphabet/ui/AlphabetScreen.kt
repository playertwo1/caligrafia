package com.scribe.caligrafia.alphabet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.caligrafia.alphabet.model.AlphabetCategory
import com.scribe.caligrafia.alphabet.model.GlyphVariant
import com.scribe.caligrafia.alphabet.model.PersonalGlyph
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.style.model.ScribeStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Interface completa de "Meu Alfabeto" (Milestone M6 — SCR-601 a SCR-604).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphabetScreen(
    viewModel: AlphabetViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPractice: ((String) -> Unit)? = null,
    onNavigateToNotebook: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Meu Alfabeto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Curadoria da escrita pessoal & PersonalStyle (M6)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(paddingValues)
        ) {
            // 1. Card Superior de Estatísticas e Compilação
            AlphabetSummaryCard(
                totalGlyphs = uiState.alphabet.totalGlyphsCount,
                completedGlyphs = uiState.alphabet.completedGlyphsCount,
                completionPct = uiState.alphabet.completionPercentage,
                averageScore = uiState.alphabet.averageScore,
                isCompiling = uiState.isCompilingStyle,
                onCompile = { viewModel.compilePersonalStyle() }
            )

            // 2. Seletor de Categoria
            CategoryChipsRow(
                selectedCategory = uiState.selectedCategory,
                alphabet = uiState.alphabet,
                onSelectCategory = { viewModel.selectCategory(it) }
            )

            // 3. Grade de Glifos
            val categoryGlyphs = uiState.alphabet.glyphs.values
                .filter { it.category == uiState.selectedCategory }
                .sortedBy { it.symbol }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 105.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categoryGlyphs, key = { it.id }) { glyph ->
                    GlyphCard(
                        glyph = glyph,
                        isSelected = glyph.id == uiState.selectedGlyph?.id,
                        onClick = { viewModel.selectGlyph(glyph) }
                    )
                }
            }
        }
    }

    // Modal de Detalhes e Curadoria do Glifo
    if (uiState.selectedGlyph != null) {
        GlyphDetailModal(
            glyph = uiState.selectedGlyph!!,
            activeStrokes = uiState.selectedVariantStrokes,
            onDismiss = { viewModel.selectGlyph(null) },
            onSetFavorite = { variantId ->
                viewModel.setFavoriteVariant(uiState.selectedGlyph!!.id, variantId)
            },
            onDeleteVariant = { variantId ->
                viewModel.deleteVariant(uiState.selectedGlyph!!.id, variantId)
            },
            onSelectVariantPreview = { variantId ->
                viewModel.loadVariantPreview(variantId)
            },
            onNavigateToPractice = {
                val symbol = uiState.selectedGlyph!!.symbol
                viewModel.selectGlyph(null)
                onNavigateToPractice?.invoke(symbol)
            }
        )
    }

    // Diálogo de Sucesso na Compilação do PersonalStyle
    if (uiState.compilationSuccessDialogVisible && uiState.lastCompiledStyle != null) {
        PersonalStyleCompiledDialog(
            style = uiState.lastCompiledStyle!!,
            onDismiss = { viewModel.dismissCompilationDialog() },
            onUseInNotebook = {
                viewModel.dismissCompilationDialog()
                onNavigateToNotebook?.invoke()
            }
        )
    }
}

/**
 * Card de cabeçalho com estatísticas de conclusão e botão de compilação.
 */
@Composable
private fun AlphabetSummaryCard(
    totalGlyphs: Int,
    completedGlyphs: Int,
    completionPct: Float,
    averageScore: Float,
    isCompiling: Boolean,
    onCompile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                Column {
                    Text(
                        text = "Progresso do Alfabeto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "$completedGlyphs de $totalGlyphs glifos curados (${completionPct.toInt()}%)",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (averageScore > 0f) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Média: ${averageScore.toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (completionPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCompile,
                enabled = !isCompiling && completedGlyphs > 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isCompiling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compilando...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compilar Meu Estilo Pessoal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Linha de Chips de Categoria.
 */
@Composable
private fun CategoryChipsRow(
    selectedCategory: AlphabetCategory,
    alphabet: com.scribe.caligrafia.alphabet.model.PersonalAlphabet,
    onSelectCategory: (AlphabetCategory) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AlphabetCategory.entries.forEach { category ->
            val stats = alphabet.statsFor(category)
            val isSelected = category == selectedCategory

            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = "${category.displayName} (${stats.completedGlyphs}/${stats.totalGlyphs})",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFEFF6FF),
                    selectedLabelColor = Color(0xFF2563EB)
                )
            )
        }
    }
}

/**
 * Card individual de cada glifo na grade.
 */
@Composable
private fun GlyphCard(
    glyph: PersonalGlyph,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB))
        } else if (glyph.isCompleted) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(if (glyph.isCompleted) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabeçalho com Símbolo e Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = glyph.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (glyph.isCompleted) Color(0xFF1E293B) else Color(0xFF94A3B8)
                )

                if (glyph.isCompleted) {
                    val active = glyph.activeVariant
                    val badgeText = "${active?.label ?: "v1"} • ${glyph.bestScore.toInt()}%"
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                } else {
                    Text(
                        text = "Vazio",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Canvas de Pré-visualização Vetorial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (glyph.activeVariant != null && glyph.activeVariant!!.strokes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawGuidelineBackground(size.width, size.height, 52f)
                        drawScaledStrokeSequence(glyph.activeVariant!!.strokes, overrideColor = Color(0xFF1E293B))
                    }
                } else if (glyph.isCompleted) {
                    // Variante salva sem cache em memória imediato: ícone de concluído
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Não praticado: marca d'água
                    Text(
                        text = glyph.symbol,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quantidade de Versões
            val versionsCount = glyph.variants.size
            Text(
                text = if (versionsCount == 1) "1 versão" else if (versionsCount > 1) "$versionsCount versões" else "Pendente",
                fontSize = 10.sp,
                color = if (glyph.isCompleted) Color(0xFF64748B) else Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modal completo para curadoria e inspeção de versões do glifo selecionado.
 */
@Composable
private fun GlyphDetailModal(
    glyph: PersonalGlyph,
    activeStrokes: List<Stroke>,
    onDismiss: () -> Unit,
    onSetFavorite: (String) -> Unit,
    onDeleteVariant: (String) -> Unit,
    onSelectVariantPreview: (String) -> Unit,
    onNavigateToPractice: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${glyph.name} ('${glyph.symbol}')",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${glyph.variants.size} versões registradas",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Canvas Ampliado de Pré-visualização com Pautas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeStrokes.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawGuidelineBackground(size.width, size.height, 52f)
                            drawScaledStrokeSequence(activeStrokes, overrideColor = Color(0xFF1E293B))
                        }
                    } else {
                        Text(
                            text = "Nenhum traçado selecionado",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lista de Versões Gravadas
                Text(
                    text = "Versões e Histórico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (glyph.variants.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Nenhuma variante gravada ainda para este caractere. Pratique no Treino Guiado para adicionar sua primeira versão!",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    glyph.variants.sortedByDescending { it.version }.forEach { variant ->
                        val isFav = variant.isFavorite || variant.id == glyph.selectedVariantId
                        VariantRowItem(
                            variant = variant,
                            isFavorite = isFav,
                            onSetFavorite = { onSetFavorite(variant.id) },
                            onDelete = { onDeleteVariant(variant.id) },
                            onSelect = { onSelectVariantPreview(variant.id) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botão de Treino Direto
                OutlinedButton(
                    onClick = onNavigateToPractice,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Praticar este Glifo no Treino Guiado", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Concluído")
            }
        }
    )
}

/**
 * Linha de variante individual no modal de curadoria.
 */
@Composable
private fun VariantRowItem(
    variant: GlyphVariant,
    isFavorite: Boolean,
    onSetFavorite: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isFavorite) Color(0xFFEFF6FF) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFavorite) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = variant.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${variant.score.toInt()}%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                    if (isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Favorita",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
                Text(
                    text = "Slant: ${variant.slantAngleDegrees.toInt()}° • ${formatDate(variant.createdAtTimestamp)}",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            Row {
                IconButton(onClick = onSetFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color(0xFFEAB308) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo comemorativo e informativo da compilação do PersonalStyle.
 */
@Composable
private fun PersonalStyleCompiledDialog(
    style: ScribeStyle,
    onDismiss: () -> Unit,
    onUseInNotebook: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Estilo Pessoal Compilado!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = style.description,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Parâmetros Calculados:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "• Inclinação Média: ${style.defaultSlantAngle.toInt()}°", fontSize = 11.sp, color = Color(0xFF334155))
                        Text(text = "• Proporção: ${style.recommendedRatio.displayName}", fontSize = 11.sp, color = Color(0xFF334155))
                        Text(text = "• Espessura Sugerida: ${style.recommendedStrokeWidthPx}px", fontSize = 11.sp, color = Color(0xFF334155))
                        Text(text = "• Modulação/Contraste: ${style.contrastRatio}x", fontSize = 11.sp, color = Color(0xFF334155))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Este estilo já foi registrado no Style Engine e está disponível para seleção no Caderno e no Treino Guiado.",
                    fontSize = 11.sp,
                    color = Color(0xFF059669)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUseInNotebook,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Usar no Caderno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

/**
 * Desenha as pautas caligráficas clássicas de fundo.
 */
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

    var currentX = 15f
    while (currentX < width + deltaX) {
        drawLine(slantColor, Offset(currentX, descenderY), Offset(currentX - deltaX, ascenderY), strokeWidth = 1f)
        currentX += 35f
    }
}

/**
 * Projeta e escala os traços vetoriais perfeitamente centralizados dentro do Canvas.
 */
private fun DrawScope.drawScaledStrokeSequence(
    strokes: List<Stroke>,
    overrideColor: Color? = null,
    alpha: Float = 1.0f
) {
    if (strokes.isEmpty()) return
    val allPoints = strokes.flatMap { it.points }
    if (allPoints.size < 2) return

    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }

    val rawW = (maxX - minX).coerceAtLeast(1f)
    val rawH = (maxY - minY).coerceAtLeast(1f)

    val padding = 10f
    val targetW = size.width - (padding * 2)
    val targetH = size.height - (padding * 2)

    val scale = minOf(targetW / rawW, targetH / rawH, 1.4f).coerceAtLeast(0.1f)
    val offsetX = (size.width - rawW * scale) / 2f - minX * scale
    val offsetY = (size.height - rawH * scale) / 2f - minY * scale

    for (stroke in strokes) {
        if (stroke.points.size < 2) continue
        val strokeColor = overrideColor ?: Color(stroke.color ?: android.graphics.Color.BLACK)
        val baseWidth = (stroke.baseWidthPx ?: 4.0f) * scale

        for (i in 0 until stroke.points.size - 1) {
            val p1 = stroke.points[i]
            val p2 = stroke.points[i + 1]
            val x1 = p1.x * scale + offsetX
            val y1 = p1.y * scale + offsetY
            val x2 = p2.x * scale + offsetX
            val y2 = p2.y * scale + offsetY
            val width = (baseWidth * (p1.pressure ?: 0.5f) * 1.5f).coerceIn(1.2f, 10f)
            drawLine(
                color = strokeColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
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
