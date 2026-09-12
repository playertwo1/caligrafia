package com.scribe.caligrafia.expansions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.expansions.passage.PassageCatalog
import com.scribe.caligrafia.expansions.passage.PassageCategory
import com.scribe.caligrafia.expansions.passage.PassageItem
import com.scribe.caligrafia.expansions.signature.SignatureCanvasView
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpansionsScreen(
    viewModel: ExpansionsViewModel,
    onBack: () -> Unit,
    onNavigateToPracticeWithText: ((PassageItem) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Estúdio de Expansões",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Milestone M8 — Assinaturas, Cópia, Backup & S Pen",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar ao Caderno",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Abas de navegação
            ScrollableTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = Color.White,
                contentColor = Color(0xFF2563EB),
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                        color = Color(0xFF2563EB),
                        height = 3.dp
                    )
                }
            ) {
                ExpansionsTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.activeTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.activeTab == tab) Color(0xFF2563EB) else Color(0xFF64748B)
                            )
                        }
                    )
                }
            }

            // Conteúdo da aba selecionada
            when (uiState.activeTab) {
                ExpansionsTab.SIGNATURE -> SignatureStudioContent(viewModel = viewModel)
                ExpansionsTab.PASSAGES -> PassagesContent(
                    viewModel = viewModel,
                    onNavigateToPracticeWithText = onNavigateToPracticeWithText
                )
                ExpansionsTab.BACKUP -> BackupContent(viewModel = viewModel)
                ExpansionsTab.SPEN_WATCH -> SpenAndWatchContent(viewModel = viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. ABA ASSINATURAS & MONOGRAMAS
// -------------------------------------------------------------

@Composable
private fun SignatureStudioContent(viewModel: ExpansionsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var canvasRef by remember { mutableStateOf<SignatureCanvasView?>(null) }
    var showSvgDialog by remember { mutableStateOf(false) }

    if (showSvgDialog && uiState.exportedSvgSnippet != null) {
        AlertDialog(
            onDismissRequest = { showSvgDialog = false },
            title = { Text("Código SVG da Assinatura", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = uiState.exportedSvgSnippet ?: "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 15
                )
            },
            confirmButton = {
                Button(onClick = { showSvgDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card de Instruções e Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Laboratório de Assinatura e Rubrica",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Desenhe sua assinatura com a S Pen sobre a linha de base azul. O motor calcula repetibilidade motora, cadência e exporta vetores SVG ou PNG transparente.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Canvas de Assinatura
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        SignatureCanvasView(ctx).apply {
                            canvasRef = this
                            onStrokeFinished = { strokes ->
                                viewModel.onSignatureStrokesChanged(strokes)
                            }
                        }
                    },
                    update = { view ->
                        canvasRef = view
                    }
                )
            }
        }

        // Barra de Ações do Canvas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        canvasRef?.clearCanvas()
                        viewModel.onSignatureStrokesChanged(emptyList())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpar", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        canvasRef?.undoLastStroke()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Desfazer", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.saveAsBaseline() },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gravar Referência", fontSize = 12.sp)
                }
            }
        }

        // Relatório de Consistência
        item {
            AnimatedVisibility(visible = uiState.consistencyReport != null) {
                uiState.consistencyReport?.let { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.isConsistent) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (report.isConsistent) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.feedbackTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (report.isConsistent) Color(0xFF166534) else Color(0xFF92400E)
                                )
                                Text(
                                    text = "Repetibilidade: %.0f%%".format(report.repeatabilityScore),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { report.repeatabilityScore / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (report.isConsistent) Color(0xFF16A34A) else Color(0xFFD97706),
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.feedbackDetails,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Dica Ergonômica: ${report.ergonomicTip}",
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Exportação Profissional
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Exportação Profissional",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.generateSvg()
                                showSvgDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Exportar SVG", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val dir = File(context.filesDir, "signatures")
                                viewModel.exportPng(dir)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                        ) {
                            Text("PNG Transparente", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ABA CÓPIA DE TEXTOS & POEMAS
// -------------------------------------------------------------

@Composable
private fun PassagesContent(
    viewModel: ExpansionsViewModel,
    onNavigateToPracticeWithText: ((PassageItem) -> Unit)?
) {
    val uiState by viewModel.uiState.collectAsState()
    var passageStartTime by remember { mutableStateOf(0L) }
    var isPracticing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Seletor de Categorias
        item {
            Text(
                text = "Gênero Caligráfico",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PassageCategory.values()) { cat ->
                    val isSelected = uiState.selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPassageCategory(cat) },
                        label = { Text(cat.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Seletor de Textos da Categoria
        item {
            val available = PassageCatalog.getByCategory(uiState.selectedCategory)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(available) { passage ->
                    val isSelected = uiState.selectedPassage.id == passage.id
                    Card(
                        onClick = { viewModel.selectPassage(passage) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = passage.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = passage.author,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Card de Visualização do Texto Clássico
        item {
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
                        Column {
                            Text(
                                text = uiState.selectedPassage.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = uiState.selectedPassage.author,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF64748B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Meta: ${uiState.selectedPassage.targetWpm} WPM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        uiState.selectedPassage.lines.forEach { line ->
                            Text(
                                text = line,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!isPracticing) {
                            Button(
                                onClick = {
                                    passageStartTime = System.currentTimeMillis()
                                    isPracticing = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Iniciar Cronômetro de Cópia", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val duration = System.currentTimeMillis() - passageStartTime
                                    isPracticing = false
                                    viewModel.evaluatePassage(duration)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finalizar e Avaliar Cadência", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Relatório de WPM e Ritmo
        item {
            AnimatedVisibility(visible = uiState.passagePacingResult != null) {
                uiState.passagePacingResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = result.diagnosis,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Score: %.0f%%".format(result.pacingScore),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.recommendation,
                                fontSize = 12.sp,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ABA BACKUP & DADOS ATÔMICOS (.SCRIBEPACK)
// -------------------------------------------------------------

@Composable
private fun BackupContent(viewModel: ExpansionsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Backup Completo Local (.scribepack)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "100% Offline • Sem nuvem • Preserva traços vetoriais brutos",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Gera um arquivo ZIP padrão (.scribepack) contendo todos os cadernos, alfabeto pessoal, histórico de aulas e diagnósticos do professor. Você pode guardar no armazenamento interno ou restaurar quando quiser.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val backupsDir = File(context.filesDir, "backups")
                                val dest = File(backupsDir, "scribe_backup_${System.currentTimeMillis()}.scribepack")
                                viewModel.createBackup(dest)
                            },
                            enabled = !uiState.isExporting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(
                                if (uiState.isExporting) "Exportando..." else "Criar Backup Agora",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Resumo do Último Backup
        item {
            AnimatedVisibility(visible = uiState.lastBackupSummary != null) {
                uiState.lastBackupSummary?.let { summary ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Resumo do Backup Criado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Arquivos empacotados: ${summary.fileCount}\n• Tamanho total: ${summary.totalBytes / 1024} KB\n• Cadernos: ${summary.manifest.notebookCount} (${summary.manifest.pageCount} páginas)\n• Glifos do Alfabeto: ${summary.manifest.personalGlyphCount}\n• Tentativas de Prática: ${summary.manifest.practiceAttemptCount}",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. ABA S PEN & GALAXY WATCH
// -------------------------------------------------------------

@Composable
private fun SpenAndWatchContent(viewModel: ExpansionsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calibração de Curva de Pressão
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Curva de Resposta de Pressão S Pen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ajuste a modulação de espessura de acordo com o peso natural da sua mão:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PressureCurveType.values().forEach { curve ->
                        val isSelected = uiState.pressureCurve == curve
                        Card(
                            onClick = { viewModel.setPressureCurve(curve) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = curve.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = curve.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Lembrete Postural e Wear OS / Galaxy Watch
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Lembrete Postural & Galaxy Watch",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Prevenção ergonômica de tensão e DORT no pulso",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ativar Lembrete Postural",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Switch(
                            checked = uiState.isPostureReminderEnabled,
                            onCheckedChange = { viewModel.togglePostureReminder(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Intervalo de descanso: ${uiState.postureAlertMinutes} minutos de escrita contínua",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Slider(
                        value = uiState.postureAlertMinutes.toFloat(),
                        onValueChange = { viewModel.setPostureAlertMinutes(it.toInt()) },
                        valueRange = 10f..45f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.testPostureHaptic() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testar Pulso Háptico no Dispositivo", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
