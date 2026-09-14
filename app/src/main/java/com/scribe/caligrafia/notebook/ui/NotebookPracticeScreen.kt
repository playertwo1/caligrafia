package com.scribe.caligrafia.notebook.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.zIndex
import com.scribe.caligrafia.expansions.passage.ActiveTextCopySession
import com.scribe.caligrafia.preferences.ScribePreferencesRuntime
import com.scribe.caligrafia.preferences.ToolbarSide
import kotlinx.coroutines.launch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.core.model.CalligraphyColor
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.PenThickness
import com.scribe.caligrafia.core.model.SlantConfig
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.notebook.viewmodel.NotebookPracticeViewModel
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.ui.theme.ScribeBluePrimary
import com.scribe.caligrafia.ui.theme.ScribePaper
import com.scribe.caligrafia.ui.theme.ScribeSurfaceBorder
import com.scribe.caligrafia.ui.theme.ScribeTextMuted
import com.scribe.caligrafia.ui.theme.ScribeTextPrimary
import com.scribe.caligrafia.ui.theme.ScribeTextSecondary
import kotlin.math.roundToInt

private enum class ActiveToolPanel {
    NONE,
    PEN,
    ERASER,
    GUIDELINES,
    PAGES
}

/**
 * Tela do Caderno de Prática Caligráfica (Fluxo 02 & Fluxo 03 dos PNGs).
 *
 * Apresenta a folha caligráfica como elemento herói em tela cheia com barra flutuante
 * inferior de ferramentas (Caneta, Borracha, Guias 52°, Páginas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookPracticeScreen(
    viewModel: NotebookPracticeViewModel,
    onNavigateToGuidedPractice: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by ScribePreferencesRuntime.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var canvasViewRef by remember { mutableStateOf<NotebookCanvasView?>(null) }
    var showPagesSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(ActiveToolPanel.NONE) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    // Se estiver no modo biblioteca de cadernos (Fluxo 01)
    if (uiState.isLibraryView) {
        NotebookLibraryScreen(
            notebooks = uiState.allNotebooks,
            currentNotebookId = uiState.currentNotebook?.id,
            onSelectNotebook = { viewModel.selectNotebook(it) },
            onCreateNotebook = { name, cfg, coverStyle -> viewModel.createNotebook(name, cfg, coverStyle) },
            onRenameNotebook = { nb, title -> viewModel.renameNotebook(nb, title) },
            onDeleteNotebook = { viewModel.deleteNotebook(it) }
        )
        return
    }

    // Modal de Organização de Páginas (Fluxo 02)
    if (showPagesSheet) {
        NotebookPagesBottomSheet(
            pages = uiState.pages,
            currentPageIndex = uiState.currentPageIndex,
            onSelectPage = { index ->
                viewModel.goToPage(index)
                canvasViewRef?.requestRedraw()
            },
            onAddPage = { config ->
                viewModel.addNewPage(config)
                canvasViewRef?.requestRedraw()
            },
            onDuplicatePage = { pageId ->
                viewModel.duplicatePage(pageId)
                canvasViewRef?.requestRedraw()
            },
            onDeletePage = { pageId ->
                viewModel.deleteTargetPage(pageId)
                canvasViewRef?.requestRedraw()
            },
            onDismiss = { showPagesSheet = false }
        )
    }

    // Diálogo de confirmação para limpar página
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Limpar página?",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            },
            text = {
                Text("Todos os traços da página atual serão apagados.", color = ScribeTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPage()
                        canvasViewRef?.requestRedraw()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Limpar tudo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = ScribeTextSecondary)
                }
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
                    IconButton(onClick = { viewModel.showLibrary() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Biblioteca de cadernos",
                            tint = ScribeTextPrimary
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.clickable { showPagesSheet = true }
                    ) {
                        Text(
                            text = uiState.currentNotebook?.title ?: "Meu Caderno",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = ScribeTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Página ${uiState.currentPageIndex + 1} de ${uiState.totalPages}",
                                fontSize = 12.sp,
                                color = ScribeTextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Ver todas as páginas",
                                tint = ScribeTextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.undo()
                            canvasViewRef?.requestRedraw()
                        },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Desfazer",
                            tint = if (uiState.canUndo) ScribeTextPrimary else Color(0xFFCBD5E1)
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.redo()
                            canvasViewRef?.requestRedraw()
                        },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Refazer",
                            tint = if (uiState.canRedo) ScribeTextPrimary else Color(0xFFCBD5E1)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = ScribeTextPrimary)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Organizar páginas...") },
                                onClick = {
                                    showMoreMenu = false
                                    showPagesSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar página como imagem...") },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.exportCurrentPage(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpar página", color = Color(0xFFDC2626)) },
                                onClick = {
                                    showMoreMenu = false
                                    showClearDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            // 1. Folha de Escrita Principal (NotebookCanvasView nativa)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ScribeSurfaceBorder, RoundedCornerShape(8.dp))
                    .shadow(2.dp, RoundedCornerShape(8.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        NotebookCanvasView(
                            context = ctx,
                            pipeline = viewModel.pipeline,
                            strokeRepository = viewModel.strokeRepository,
                            renderer = viewModel.renderer,
                            onStrokeChanged = { viewModel.onStrokesModified() },
                            onActiveWriting = viewModel::onActiveWriting
                        ).apply {
                            guidelineConfig = uiState.currentPage?.guidelineConfig
                            canvasViewRef = this
                        }
                    },
                    update = { view ->
                        view.guidelineConfig = uiState.currentPage?.guidelineConfig
                        canvasViewRef = view
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            viewModel.updateCanvasDimensions(size.width.toFloat(), size.height.toFloat())
                        }
                )
            }

            // Card Flutuante e Recolhível de Cópia de Texto (F4.13)
            uiState.activeTextCopy?.let { copySession ->
                ActiveTextCopyOverlayCard(
                    session = copySession,
                    onToggleCollapse = { viewModel.toggleTextCopyCollapse() },
                    onTogglePause = {
                        if (copySession.isPaused) viewModel.resumeTextCopy()
                        else viewModel.pauseTextCopy()
                    },
                    onFinish = {
                        coroutineScope.launch {
                            viewModel.finishTextCopyPractice()
                            canvasViewRef?.requestRedraw()
                        }
                    },
                    onCancel = { viewModel.cancelTextCopyPractice() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .zIndex(10f)
                )
            }

            // 2. Painel de Controle Ativo (quando um botão da barra flutuante for expandido)
            AnimatedVisibility(
                visible = activePanel != ActiveToolPanel.NONE,
                enter = if (preferences.reduceAnimations) EnterTransition.None else slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = if (preferences.reduceAnimations) ExitTransition.None else slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ScribeSurfaceBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (activePanel) {
                        ActiveToolPanel.PEN -> {
                            PenSettingsCard(
                                toolConfig = uiState.toolConfig,
                                onSelectThickness = {
                                    viewModel.setPenThickness(it)
                                    viewModel.setToolMode(ToolMode.PEN)
                                },
                                onSelectColor = {
                                    viewModel.setCalligraphyColor(it)
                                    viewModel.setToolMode(ToolMode.PEN)
                                }
                            )
                        }
                        ActiveToolPanel.ERASER -> {
                            EraserSettingsCard(
                                onClearPage = {
                                    showClearDialog = true
                                    activePanel = ActiveToolPanel.NONE
                                }
                            )
                        }
                        ActiveToolPanel.GUIDELINES -> {
                            GuidelinesSettingsCard(
                                currentConfig = uiState.currentPage?.guidelineConfig ?: GuidelineConfig.copperplate(),
                                styles = uiState.availableStyles,
                                currentStyle = uiState.currentStyle,
                                onSelectStyle = { styleId ->
                                    viewModel.selectStyle(styleId, adaptPageGuidelines = true)
                                    canvasViewRef?.guidelineConfig = viewModel.uiState.value.currentPage?.guidelineConfig
                                    canvasViewRef?.requestRedraw()
                                },
                                onConfigChanged = { cfg ->
                                    viewModel.setGuidelineConfig(cfg)
                                    canvasViewRef?.guidelineConfig = cfg
                                    canvasViewRef?.requestRedraw()
                                }
                            )
                        }
                        ActiveToolPanel.PAGES -> {
                            PagesQuickNavigationCard(
                                currentPageIndex = uiState.currentPageIndex,
                                totalPages = uiState.totalPages,
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
                                onOpenGrid = {
                                    activePanel = ActiveToolPanel.NONE
                                    showPagesSheet = true
                                }
                            )
                        }
                        ActiveToolPanel.NONE -> {}
                    }
                }
            }

            // 3. Barra Flutuante de Ferramentas (Docked Floating Toolbar - Fluxo 03)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, ScribeSurfaceBorder),
                modifier = Modifier
                    .align(if (preferences.effectiveToolbarSide == ToolbarSide.LEFT) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão 1: Caneta
                    FloatingToolbarItem(
                        icon = Icons.Default.Edit,
                        label = "Caneta",
                        isSelected = activePanel == ActiveToolPanel.PEN || (activePanel == ActiveToolPanel.NONE && uiState.toolConfig.mode == ToolMode.PEN),
                        badgeColor = Color(uiState.toolConfig.color.argb),
                        onClick = {
                            viewModel.setToolMode(ToolMode.PEN)
                            activePanel = if (activePanel == ActiveToolPanel.PEN) ActiveToolPanel.NONE else ActiveToolPanel.PEN
                        }
                    )

                    // Botão 2: Borracha
                    FloatingToolbarItem(
                        icon = Icons.Default.Clear,
                        label = "Borracha",
                        isSelected = activePanel == ActiveToolPanel.ERASER || (activePanel == ActiveToolPanel.NONE && uiState.toolConfig.mode == ToolMode.ERASER),
                        onClick = {
                            viewModel.setToolMode(ToolMode.ERASER)
                            activePanel = if (activePanel == ActiveToolPanel.ERASER) ActiveToolPanel.NONE else ActiveToolPanel.ERASER
                        }
                    )

                    // Botão 3: Guias
                    FloatingToolbarItem(
                        icon = Icons.Default.Straighten,
                        label = "Guias (52°)",
                        isSelected = activePanel == ActiveToolPanel.GUIDELINES,
                        onClick = {
                            activePanel = if (activePanel == ActiveToolPanel.GUIDELINES) ActiveToolPanel.NONE else ActiveToolPanel.GUIDELINES
                        }
                    )

                    // Botão 4: Páginas
                    FloatingToolbarItem(
                        icon = Icons.Default.GridOn,
                        label = "Páginas",
                        isSelected = activePanel == ActiveToolPanel.PAGES,
                        badgeText = "${uiState.currentPageIndex + 1}/${uiState.totalPages}",
                        onClick = {
                            activePanel = if (activePanel == ActiveToolPanel.PAGES) ActiveToolPanel.NONE else ActiveToolPanel.PAGES
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componentes Auxiliares da Toolbar Flutuante
// -------------------------------------------------------------

@Composable
private fun FloatingToolbarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeColor: Color? = null,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) ScribeBluePrimary else ScribeTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                if (badgeColor != null) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                            .border(1.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) ScribeBluePrimary else ScribeTextSecondary
            )

            if (badgeText != null) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScribeBluePrimary,
                    modifier = Modifier
                        .background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun PenSettingsCard(
    toolConfig: ToolConfig,
    onSelectThickness: (PenThickness) -> Unit,
    onSelectColor: (CalligraphyColor) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pena & Espessura", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeTextPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PenThickness.values().forEach { thickness ->
                val isSelected = toolConfig.penThickness == thickness
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectThickness(thickness) },
                    label = { Text(thickness.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ScribeBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Text("Tinta Caligráfica", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeTextPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalligraphyColor.values().forEach { color ->
                val isSelected = toolConfig.color == color
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectColor(color) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(color.argb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) ScribeBluePrimary else Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = color.displayName.take(8),
                        fontSize = 10.sp,
                        color = if (isSelected) ScribeBluePrimary else ScribeTextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun EraserSettingsCard(
    onClearPage: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Borracha por Traço", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeTextPrimary)
        Text(
            text = "Modo inteligente de caligrafia: ao tocar em qualquer ponto de uma haste ou curva, o traço completo é removido preservando a fluidez.",
            fontSize = 12.sp,
            color = ScribeTextSecondary
        )

        Button(
            onClick = onClearPage,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Limpar toda a página", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuidelinesSettingsCard(
    currentConfig: GuidelineConfig,
    styles: List<ScribeStyle> = emptyList(),
    currentStyle: ScribeStyle? = null,
    onSelectStyle: (String) -> Unit = {},
    onConfigChanged: (GuidelineConfig) -> Unit
) {
    var slantAngle by remember { mutableStateOf(currentConfig.slant?.angleDegrees ?: 52.0f) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pautas e Estilos Caligráficos",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ScribeTextPrimary
            )
            Text(
                text = currentStyle?.let { "${it.name}: ${it.defaultSlantAngle.toInt()}°" } ?: "${slantAngle.roundToInt()}°",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScribeBluePrimary
            )
        }

        // Seletor de estilos caligráficos (inclui canônicos, expandidos e fontes importadas R17)
        if (styles.isNotEmpty()) {
            Text(text = "Estilo Caligráfico:", fontSize = 11.sp, color = ScribeTextSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                styles.forEach { style ->
                    val isSelected = currentStyle?.id == style.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStyle(style.id) },
                        label = { Text("${style.name} (${style.defaultSlantAngle.toInt()}°)", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Slant slider (Copperplate canônico a 52°)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ângulo de inclinação:", fontSize = 12.sp, color = ScribeTextSecondary)
                Text("${slantAngle.roundToInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ScribeTextPrimary)
            }
            Slider(
                value = slantAngle,
                onValueChange = { slantAngle = it },
                onValueChangeFinished = {
                    val updated = currentConfig.copy(slant = SlantConfig(angleDegrees = slantAngle))
                    onConfigChanged(updated)
                },
                valueRange = 30f..90f,
                steps = 60,
                colors = SliderDefaults.colors(
                    thumbColor = ScribeBluePrimary,
                    activeTrackColor = ScribeBluePrimary
                )
            )
        }

        // Presets rápidos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = (slantAngle.roundToInt() == 52),
                onClick = {
                    slantAngle = 52f
                    onConfigChanged(GuidelineConfig.copperplate())
                },
                label = { Text("52° Copperplate", fontSize = 11.sp) }
            )

            FilterChip(
                selected = (slantAngle.roundToInt() == 68),
                onClick = {
                    slantAngle = 68f
                    onConfigChanged(GuidelineConfig.spencerian())
                },
                label = { Text("68° Spencerian", fontSize = 11.sp) }
            )

            FilterChip(
                selected = currentConfig.slant == null,
                onClick = {
                    onConfigChanged(GuidelineConfig.school())
                },
                label = { Text("Vertical Escolar", fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun PagesQuickNavigationCard(
    currentPageIndex: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddPage: () -> Unit,
    onOpenGrid: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Página ${currentPageIndex + 1} de $totalPages",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScribeTextPrimary
            )

            Button(
                onClick = onOpenGrid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, tint = ScribeBluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver todas em grade", color = ScribeBluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentPageIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Anterior")
            }

            OutlinedButton(
                onClick = onNext,
                enabled = currentPageIndex < totalPages - 1,
                modifier = Modifier.weight(1f)
            ) {
                Text("Próxima")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            Button(
                onClick = onAddPage,
                colors = ButtonDefaults.buttonColors(containerColor = ScribeBluePrimary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova")
            }
        }
    }
}

/**
 * Card recolhível com o texto para cópia, timer em tempo real e controle de pausa (F4.13).
 */
@Composable
private fun ActiveTextCopyOverlayCard(
    session: ActiveTextCopySession,
    onToggleCollapse: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha de Cabeçalho / Controles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(14.dp)
                            )
                            val minutes = session.elapsedSeconds / 60
                            val seconds = session.elapsedSeconds % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = session.passage.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = ScribeTextPrimary
                        )
                        Text(
                            text = session.passage.author,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = ScribeTextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onTogglePause,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (session.isPaused) "Retomar" else "Pausar",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (session.isCollapsed) "Expandir" else "Recolher",
                            tint = ScribeTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancelar cópia",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Conteúdo do Texto (quando não recolhido)
            if (!session.isCollapsed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        session.passage.lines.forEach { line ->
                            Text(
                                text = line,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Meta: ${session.passage.targetWpm} WPM • Estilo: ${session.styleId}",
                        fontSize = 10.sp,
                        color = ScribeTextMuted
                    )
                    Text(
                        text = "Escreva na pauta abaixo. Use '+' para novas páginas.",
                        fontSize = 10.sp,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }
    }
}
