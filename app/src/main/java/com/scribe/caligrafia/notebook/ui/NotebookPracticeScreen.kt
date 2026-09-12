package com.scribe.caligrafia.notebook.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.core.model.CalligraphyColor
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.PenThickness
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookPracticeScreen(
    viewModel: NotebookPracticeViewModel,
    onNavigateToLab: () -> Unit,
    onNavigateToGuidedPractice: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var canvasViewRef by remember { mutableStateOf<NotebookCanvasView?>(null) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var adaptGuidelinesOnSelect by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = {
                Text(
                    text = "Selecionar Estilo Caligráfico (M3)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Escolha a família formal de escrita para nortear pautas, inclinações e ductus:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    uiState.availableStyles.forEach { style ->
                        val isSelected = uiState.currentStyle.id == style.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectStyle(style.id, adaptPageGuidelines = adaptGuidelinesOnSelect)
                                    canvasViewRef?.guidelineConfig = uiState.currentPage?.guidelineConfig
                                    canvasViewRef?.requestRedraw()
                                    showStyleDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = style.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${style.defaultSlantAngle.toInt()}°",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Proporção: ${style.recommendedRatio.displayName} • Traço: ${style.recommendedStrokeWidthPx}px",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = style.description,
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { adaptGuidelinesOnSelect = !adaptGuidelinesOnSelect }
                            .padding(top = 4.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = adaptGuidelinesOnSelect,
                            onCheckedChange = { adaptGuidelinesOnSelect = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ajustar pautas da página para o estilo",
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleDialog = false }) {
                    Text("Fechar")
                }
            }
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
                            text = uiState.currentNotebook?.title ?: "Caderno de Caligrafia",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estilo: ${uiState.currentStyle.name} • ${uiState.strokeCount} traços",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    // Botão seletor de estilo (M3)
                    FilterChip(
                        selected = true,
                        onClick = { showStyleDialog = true },
                        label = { Text("Estilo: ${uiState.currentStyle.name}", fontSize = 11.sp, color = Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Botão alternador para Treino Guiado (M2)
                    FilterChip(
                        selected = false,
                        onClick = onNavigateToGuidedPractice,
                        label = { Text("Treino (M2)", fontSize = 11.sp, color = Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF3F51B5), // Indigo
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Botão alternador para o Stylus Lab (M0)
                    FilterChip(
                        selected = false,
                        onClick = onNavigateToLab,
                        label = { Text("Lab (M0)", fontSize = 11.sp, color = Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF334155),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
            // 1. Barra de Navegação de Páginas do Caderno
            NotebookPaginationBar(
                currentPageIndex = uiState.currentPageIndex,
                totalPages = uiState.totalPages,
                isSaving = uiState.isSaving,
                onPrevious = {
                    viewModel.previousPage()
                    canvasViewRef?.requestRedraw()
                },
                onNext = {
                    viewModel.nextPage()
                    canvasViewRef?.requestRedraw()
                },
                onAddPage = {
                    viewModel.addNewPage()
                    canvasViewRef?.requestRedraw()
                },
                onExport = { viewModel.exportCurrentPage(context) }
            )

            // 2. Toolbar de Ferramentas Caligráficas (Espessura, Cor, Pauta/Estilos, Borracha, Undo/Redo)
            NotebookToolbar(
                toolConfig = uiState.toolConfig,
                currentGuideline = uiState.currentPage?.guidelineConfig,
                availableStyles = uiState.availableStyles,
                currentStyle = uiState.currentStyle,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onSelectTool = { viewModel.setToolMode(it) },
                onSelectThickness = { viewModel.setPenThickness(it) },
                onSelectColor = { viewModel.setCalligraphyColor(it) },
                onSelectStyle = { style ->
                    viewModel.selectStyle(style.id, adaptPageGuidelines = true)
                    canvasViewRef?.guidelineConfig = uiState.currentPage?.guidelineConfig
                    canvasViewRef?.requestRedraw()
                },
                onSelectGuideline = {
                    viewModel.setGuidelineConfig(it)
                    canvasViewRef?.guidelineConfig = it
                },
                onUndo = {
                    viewModel.undo()
                    canvasViewRef?.requestRedraw()
                },
                onRedo = {
                    viewModel.redo()
                    canvasViewRef?.requestRedraw()
                },
                onClear = {
                    viewModel.clearPage()
                    canvasViewRef?.requestRedraw()
                }
            )

            // 3. Área da Folha Caligráfica Nativa (NotebookCanvasView)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        NotebookCanvasView(
                            context = ctx,
                            pipeline = viewModel.pipeline,
                            strokeRepository = viewModel.strokeRepository,
                            renderer = viewModel.renderer,
                            onStrokeChanged = { viewModel.onStrokesModified() }
                        ).apply {
                            guidelineConfig = uiState.currentPage?.guidelineConfig
                            canvasViewRef = this
                        }
                    },
                    update = { view ->
                        view.guidelineConfig = uiState.currentPage?.guidelineConfig
                        canvasViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun NotebookPaginationBar(
    currentPageIndex: Int,
    totalPages: Int,
    isSaving: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddPage: () -> Unit,
    onExport: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Controles de folheamento
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = currentPageIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Página Anterior",
                        tint = if (currentPageIndex > 0) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                    )
                }

                Text(
                    text = "Página ${currentPageIndex + 1} de $totalPages",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )

                IconButton(
                    onClick = onNext,
                    enabled = currentPageIndex < totalPages - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Próxima Página",
                        tint = if (currentPageIndex < totalPages - 1) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                    )
                }

                IconButton(onClick = onAddPage) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nova Página",
                        tint = Color(0xFF2563EB)
                    )
                }
            }

            // Status de Gravação e Botão de Exportação
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSaving) {
                    Text(
                        text = "Salvando...",
                        fontSize = 11.sp,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onExport) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Exportar PNG",
                        tint = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotebookToolbar(
    toolConfig: ToolConfig,
    currentGuideline: GuidelineConfig?,
    availableStyles: List<com.scribe.caligrafia.style.model.ScribeStyle>,
    currentStyle: com.scribe.caligrafia.style.model.ScribeStyle,
    canUndo: Boolean,
    canRedo: Boolean,
    onSelectTool: (ToolMode) -> Unit,
    onSelectThickness: (PenThickness) -> Unit,
    onSelectColor: (CalligraphyColor) -> Unit,
    onSelectStyle: (com.scribe.caligrafia.style.model.ScribeStyle) -> Unit,
    onSelectGuideline: (GuidelineConfig) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha 1: Ferramenta (Caneta vs Borracha) + Espessuras + Undo/Redo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alternador Caneta / Borracha
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = toolConfig.mode == ToolMode.PEN,
                        onClick = { onSelectTool(ToolMode.PEN) },
                        label = { Text("Caneta", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = toolConfig.mode == ToolMode.ERASER,
                        onClick = { onSelectTool(ToolMode.ERASER) },
                        label = { Text("Borracha", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                // Espessuras de Caneta (Fina, Média, Grossa)
                if (toolConfig.mode == ToolMode.PEN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PenThickness.values().forEach { thickness ->
                            FilterChip(
                                selected = toolConfig.penThickness == thickness,
                                onClick = { onSelectThickness(thickness) },
                                label = { Text(thickness.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Ações: Undo, Redo, Limpar
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Desfazer",
                            tint = if (canUndo) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Refazer",
                            tint = if (canRedo) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                        )
                    }
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpar Página",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Linha 2: Paleta de Cores Caligráficas + Presets de Estilos/Pautas (M3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cores da tinta
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tinta:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    CalligraphyColor.values().forEach { col ->
                        val isSelected = toolConfig.color == col && toolConfig.mode == ToolMode.PEN
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(col.argb))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                                .clickable { onSelectColor(col) }
                        )
                    }
                }

                // Presets de Estilos Caligráficos (M3)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estilo:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                    availableStyles.forEach { style ->
                        val isSelected = currentStyle.id == style.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectStyle(style) },
                            label = { Text(style.name, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}
