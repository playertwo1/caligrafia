package com.scribe.caligrafia.inspector.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.scribe.caligrafia.core.model.InputMode
import com.scribe.caligrafia.core.model.ToolType
import com.scribe.caligrafia.ink.persistence.benchmark.BenchmarkSuiteReport
import com.scribe.caligrafia.ink.persistence.benchmark.PersistenceBenchmarkRunner
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import com.scribe.caligrafia.ink.replay.ReplayFrame
import com.scribe.caligrafia.ink.replay.ReplaySpeed
import com.scribe.caligrafia.ink.replay.ReplayStatus
import com.scribe.caligrafia.ink.replay.StrokeReplayEngine
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scribe.caligrafia.ink.lifecycle.SPenSlotState
import com.scribe.caligrafia.inspector.viewmodel.StylusLabViewModel
import com.scribe.caligrafia.inspector.DeviceCapabilityInspector
import com.scribe.caligrafia.inspector.model.DeviceCapabilities
import com.scribe.caligrafia.inspector.model.InputDeviceInfo
import com.scribe.caligrafia.inspector.model.LiveProbeSample
import com.scribe.caligrafia.inspector.model.MotionRangeInfo
import com.scribe.caligrafia.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    modifier: Modifier = Modifier,
    viewModel: StylusLabViewModel = viewModel(),
    onNavigateToNotebook: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var capabilities by remember { mutableStateOf(DeviceCapabilityInspector.inspect(context)) }
    var latestSample by remember { mutableStateOf<LiveProbeSample?>(null) }
    var showAllDevices by remember { mutableStateOf(false) }
    val sPenSlotState by viewModel.sPenSlotState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scribe — Stylus Lab",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "M0: Stylus Lab • Hardware & Latência",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                },
                actions = {
                    if (onNavigateToNotebook != null) {
                        FilledTonalButton(
                            onClick = onNavigateToNotebook,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Caderno (M1)", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    FilledTonalButton(
                        onClick = { capabilities = DeviceCapabilityInspector.inspect(context) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Reescanear", fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScribeSurface
                )
            )
        },
        containerColor = ScribeBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hardware Capability Summary Card
            CapabilitySummaryCard(
                capabilities = capabilities,
                showAllDevices = showAllDevices,
                onToggleDevices = { showAllDevices = !showAllDevices },
                sPenSlotState = sPenSlotState
            )

            // Live Probe Touch Target & Real-time Readout
            LiveProbeSection(
                viewModel = viewModel,
                latestSample = latestSample,
                onSampleReceived = { sample -> latestSample = sample },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CapabilitySummaryCard(
    capabilities: DeviceCapabilities,
    showAllDevices: Boolean,
    onToggleDevices: () -> Unit,
    sPenSlotState: SPenSlotState = SPenSlotState.UNKNOWN
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ScribeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hardware de Entrada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (capabilities.hasStylusHardware) StylusActive.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (capabilities.hasStylusHardware) "Stylus Detectada (${capabilities.stylusDevicesCount})" else "Sem Stylus Dedicada",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (capabilities.hasStylusHardware) Color(0xFF047857) else InkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (sPenSlotState != SPenSlotState.UNKNOWN) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sPenSlotState == SPenSlotState.INSERTED) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (sPenSlotState == SPenSlotState.INSERTED) Color(0xFFFCD34D) else Color(0xFFA7F3D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Silo S Pen (Hardware EMR):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sPenSlotState == SPenSlotState.INSERTED) Color(0xFFB45309) else Color(0xFF047857)
                        )
                        Text(
                            text = sPenSlotState.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sPenSlotState == SPenSlotState.INSERTED) Color(0xFFB45309) else Color(0xFF047857)
                        )
                    }
                }
            }

            // Target Device & Samsung SDK Assessment Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = ScribeBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, ScribeBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dispositivo: ${capabilities.samsungAssessment.manufacturer} ${capabilities.samsungAssessment.model}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (capabilities.samsungAssessment.isTargetS25Ultra) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StylusActive.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Target S25 Ultra",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Diagnóstico Samsung SDK: ${capabilities.samsungAssessment.notes}",
                        fontSize = 11.sp,
                        color = InkSecondary,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "Android Ink API (M0): ${capabilities.inkApiAssessment.notes}",
                        fontSize = 11.sp,
                        color = ScribeAccent,
                        lineHeight = 15.sp
                    )
                }
            }

            // Motion ranges info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeBadge(
                    label = "Pressão",
                    range = capabilities.pressureRange,
                    modifier = Modifier.weight(1f)
                )
                RangeBadge(
                    label = "Tilt (Inclinação)",
                    range = capabilities.tiltRange,
                    modifier = Modifier.weight(1f)
                )
                RangeBadge(
                    label = "Orientação",
                    range = capabilities.orientationRange,
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(
                onClick = onToggleDevices,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (showAllDevices) "Ocultar detalhes dos dispositivos (${capabilities.devices.size})"
                    else "Ver detalhes de todos os dispositivos (${capabilities.devices.size})",
                    fontSize = 12.sp
                )
            }


            if (showAllDevices) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .background(ScribeBackground, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(capabilities.devices) { device ->
                        DeviceDetailItem(device)
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeBadge(
    label: String,
    range: MotionRangeInfo?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ScribeBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, ScribeBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, fontSize = 11.sp, color = InkSecondary, fontWeight = FontWeight.Medium)
            if (range != null) {
                Text(
                    text = "${range.min} .. ${range.max}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Text(
                    text = "Não suportado",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailItem(device: InputDeviceInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${device.id}: ${device.name}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Fontes: ${device.sources.joinToString(", ")} | Stylus: ${if (device.hasStylusSource) "SIM" else "NÃO"}",
            fontSize = 11.sp,
            color = InkSecondary
        )
    }
}

@Composable
private fun LiveProbeSection(
    viewModel: StylusLabViewModel,
    latestSample: LiveProbeSample?,
    onSampleReceived: (LiveProbeSample) -> Unit,
    modifier: Modifier = Modifier
) {
    val repository = viewModel.repository
    val rendererManager = viewModel.rendererManager
    val pipeline = viewModel.pipeline
    val replayEngine = viewModel.replayEngine

    val strokeCount by viewModel.strokeCount.collectAsState()
    val totalPoints by viewModel.totalPoints.collectAsState()
    val historicalAbsorbed by viewModel.historicalAbsorbed.collectAsState()
    val lastStrokeDuration by viewModel.lastStrokeDuration.collectAsState()
    val palmRejectionsCount by viewModel.palmRejectionsCount.collectAsState()
    val lastRejectionReason by viewModel.lastRejectionReason.collectAsState()
    val isStylusHovering by viewModel.isStylusHovering.collectAsState()
    val selectedRendererType by viewModel.selectedRendererType.collectAsState()
    val selectedInputMode by viewModel.selectedInputMode.collectAsState()
    val isReplayMode by viewModel.isReplayMode.collectAsState()
    val replayFrame by viewModel.replayFrame.collectAsState()
    val autoSaveFeedback by viewModel.autoSaveFeedback.collectAsState()
    val persistenceFeedback by viewModel.persistenceFeedback.collectAsState()
    val benchmarkReport by viewModel.benchmarkReport.collectAsState()
    val showBenchmarkDialog by viewModel.showBenchmarkDialog.collectAsState()
    val guidelineConfig by viewModel.guidelineConfig.collectAsState()
    val selectedGuidelinePreset by viewModel.selectedGuidelinePreset.collectAsState()

    var surfaceViewRef by remember { mutableStateOf<ProbeSurfaceView?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(replayFrame, isReplayMode) {
        surfaceViewRef?.isReplayMode = isReplayMode
        surfaceViewRef?.currentReplayFrame = replayFrame
        surfaceViewRef?.invalidate()
    }

    LaunchedEffect(guidelineConfig) {
        surfaceViewRef?.guidelineConfig = guidelineConfig
        surfaceViewRef?.invalidate()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ScribeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stylus Canvas & Live Renderer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SCR-005: Palm Rejection & Roteamento Stylus/Borracha",
                        fontSize = 11.sp,
                        color = InkSecondary
                    )
                }

                // Tool Type Badge
                val tool = latestSample?.toolType ?: ToolType.UNKNOWN
                val (badgeColor, textColor) = when (tool) {
                    ToolType.STYLUS -> StylusActive.copy(alpha = 0.15f) to Color(0xFF047857)
                    ToolType.ERASER -> EraserActive.copy(alpha = 0.15f) to Color(0xFFB91C1C)
                    ToolType.FINGER -> FingerActive.copy(alpha = 0.15f) to Color(0xFFB45309)
                    ToolType.MOUSE -> ScribeAccent.copy(alpha = 0.15f) to Color(0xFF1D4ED8)
                    ToolType.UNKNOWN -> Color(0xFFF1F5F9) to InkSecondary
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = tool.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mode Selector Row & Engine Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Mode Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                    InputMode.values().forEach { mode ->
                        val isSelected = selectedInputMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setInputMode(mode) },
                            label = { Text(if (mode == InputMode.STYLUS_ONLY) "Stylus Only" else "Stylus + Dedo", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (mode == InputMode.STYLUS_ONLY) Color(0xFF047857).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                selectedLabelColor = if (mode == InputMode.STYLUS_ONLY) Color(0xFF047857) else Color(0xFFB45309)
                            )
                        )
                    }
                }

                // Renderer Selector Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Motor:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                    com.scribe.caligrafia.ink.renderer.RendererType.values().forEach { rType ->
                        val isSelected = selectedRendererType == rType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setRenderer(rType)
                                surfaceViewRef?.invalidate()
                            },
                            label = { Text(rType.displayName, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ScribeAccent.copy(alpha = 0.15f),
                                selectedLabelColor = ScribeAccent
                            )
                        )
                    }
                }
            }

            // Guideline Preset Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pauta:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                com.scribe.caligrafia.inspector.viewmodel.GuidelinePreset.values().forEach { preset ->
                    val isSelected = selectedGuidelinePreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setGuidelinePreset(preset)
                            surfaceViewRef?.invalidate()
                        },
                        label = { Text(preset.displayName, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF4338CA)
                        )
                    )
                }
            }

            if (autoSaveFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = autoSaveFeedback ?: "",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Palm Rejection & Hover Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (palmRejectionsCount > 0) Color(0xFFFEF3C7) else ScribeBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (palmRejectionsCount > 0) Color(0xFFFCD34D) else ScribeBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rejeições Palma: $palmRejectionsCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (palmRejectionsCount > 0) Color(0xFFB45309) else InkSecondary
                    )
                    Text(
                        text = "Hover S Pen: ${if (isStylusHovering) "EMR DETECTADO" else "AFASTADA"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStylusHovering) Color(0xFF047857) else InkSecondary
                    )
                }
                if (lastRejectionReason != null) {
                    Text(
                        text = lastRejectionReason ?: "",
                        fontSize = 10.sp,
                        color = Color(0xFFB45309),
                        maxLines = 1
                    )
                }
            }

            // Real-time telemetry row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScribeBackground, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryColumn(
                    label = "Ação",
                    value = latestSample?.action ?: "AGUARDANDO"
                )
                TelemetryColumn(
                    label = "Coord (x, y)",
                    value = if (latestSample != null) "${latestSample.x.roundToInt()}, ${latestSample.y.roundToInt()}" else "--, --"
                )
                TelemetryColumn(
                    label = "Pressão",
                    value = latestSample?.pressure?.let { String.format("%.3f", it) } ?: "null"
                )
                TelemetryColumn(
                    label = "Tilt (Graus)",
                    value = latestSample?.tiltRad?.let {
                        String.format("%.1f°", Math.toDegrees(it.toDouble()))
                    } ?: "null"
                )
                TelemetryColumn(
                    label = "Historical",
                    value = "${latestSample?.historicalCount ?: 0} pts"
                )
            }


            // Pipeline Stats & Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Strokes: $strokeCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Pontos: $totalPoints",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Histórico: $historicalAbsorbed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    if (lastStrokeDuration > 0) {
                        Text(
                            text = "${lastStrokeDuration}ms",
                            fontSize = 12.sp,
                            color = ScribeAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.undo()
                            surfaceViewRef?.invalidate()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        enabled = repository.canUndo
                    ) {
                        Text("Desfazer", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.saveManual() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        enabled = strokeCount > 0
                    ) {
                        Text("Salvar", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.loadManual()
                            surfaceViewRef?.invalidate()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Recarregar", fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.toggleReplay(coroutineScope)
                            surfaceViewRef?.invalidate()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        enabled = strokeCount > 0,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isReplayMode) ScribeAccent else ScribeSurface,
                            contentColor = if (isReplayMode) Color.White else InkPrimary
                        )
                    ) {
                        Text(if (isReplayMode) "Sair Replay" else "Replay", fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.runPersistenceBenchmark() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Spike", fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.clearSession()
                            surfaceViewRef?.invalidate()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        enabled = strokeCount > 0 || palmRejectionsCount > 0
                    ) {
                        Text("Limpar", fontSize = 10.sp)
                    }
                }
            }

            if (isReplayMode) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        if (replayFrame.status == ReplayStatus.PLAYING) {
                                            viewModel.pauseReplay()
                                        } else {
                                            viewModel.playReplay(coroutineScope)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (replayFrame.status == ReplayStatus.PLAYING) "Pausar" else "Reproduzir", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.seekReplay(0f)
                                        viewModel.pauseReplay()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Reiniciar", fontSize = 11.sp)
                                }
                            }

                            // Seletor de Velocidade (0.5x, 1.0x, 2.0x)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Vel:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                                ReplaySpeed.values().forEach { spd ->
                                    FilterChip(
                                        selected = replayFrame.speed == spd,
                                        onClick = { viewModel.setReplaySpeed(spd) },
                                        label = { Text(spd.displayName, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ScribeAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = ScribeAccent
                                        )
                                    )
                                }
                            }
                        }

                        // Slider de Progresso Temporal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = replayFrame.progressFraction,
                                onValueChange = { viewModel.seekReplay(it) },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${String.format("%.1f", replayFrame.currentPositionMs / 1000f)}s / ${String.format("%.1f", replayFrame.totalDurationMs / 1000f)}s",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ScribeAccent
                            )
                        }
                    }
                }
            }

            if (persistenceFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = persistenceFeedback ?: "",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showBenchmarkDialog && benchmarkReport != null) {
                val rep = benchmarkReport!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissBenchmarkDialog() },
                    title = { Text("SCR-006: Benchmark de Persistência", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Carga: ${rep.strokeCount} traços, ${rep.totalPoints} pontos vetoriais.",
                                fontSize = 11.sp,
                                color = InkSecondary
                            )
                            rep.results.forEach { r ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ScribeBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(r.strategyName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Gravação: ${String.format("%.2f", r.writeTimeMs)} ms", fontSize = 10.sp)
                                            Text("Leitura: ${String.format("%.2f", r.readTimeMs)} ms", fontSize = 10.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Tamanho: ${String.format("%.2f", r.sizeBytes / 1024.0)} KB", fontSize = 10.sp)
                                            Text("Densidade: ${String.format("%.1f", r.bytesPerPoint)} B/pt", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Decisão: Arquivo Binário Dedicado (.scribe) para cadernos e Room/BLOB para traços individuais.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScribeAccent
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissBenchmarkDialog() }) {
                            Text("Fechar")
                        }
                    }
                )
            }

            // Interactive Drawing & Capture Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ScribeBorder, RoundedCornerShape(8.dp))
                    .background(Color(0xFFFAFAFA))
            ) {
                AndroidView(
                    factory = { ctx ->
                        ProbeSurfaceView(
                            ctx,
                            pipeline,
                            repository,
                            rendererManager,
                            onFocusLostCallback = { viewModel.onWindowFocusLost() }
                        ).apply {
                            surfaceViewRef = this
                            this.guidelineConfig = guidelineConfig
                            onSampleListener = { sample ->
                                onSampleReceived(sample)
                            }
                        }
                    },
                    update = { view ->
                        view.isReplayMode = isReplayMode
                        view.currentReplayFrame = replayFrame
                        view.guidelineConfig = guidelineConfig
                        view.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (latestSample == null && strokeCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Escreva aqui com a S Pen / Stylus para testar a captura vetorial de alta fidelidade",
                            color = InkSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Default
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryColumn(
    label: String,
    value: String
) {
    Column {
        Text(text = label, fontSize = 10.sp, color = InkSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Native Android View conectada ao StrokeCapturePipeline, InMemoryStrokeRepository e RendererManager.
 * Renderiza traços vetoriais utilizando o renderizador selecionado (Android Ink API, Bézier ou Raw).
 * Implementa resiliência de ciclo de vida para perda de foco e rotação de tela.
 */
@SuppressLint("ViewConstructor")
private class ProbeSurfaceView(
    context: Context,
    private val pipeline: com.scribe.caligrafia.ink.capture.StrokeCapturePipeline,
    private val repository: com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository,
    private val rendererManager: com.scribe.caligrafia.ink.renderer.RendererManager,
    private val onFocusLostCallback: (() -> Unit)? = null
) : View(context) {

    var onSampleListener: ((LiveProbeSample) -> Unit)? = null
    var isReplayMode: Boolean = false
    var currentReplayFrame: com.scribe.caligrafia.ink.replay.ReplayFrame? = null
    private val guidelineRenderer = com.scribe.caligrafia.ink.renderer.GuidelineRenderer()
    var guidelineConfig: com.scribe.caligrafia.core.model.GuidelineConfig? = null

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) {
            onFocusLostCallback?.invoke()
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onFocusLostCallback?.invoke()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    private var currentX = -1f
    private var currentY = -1f
    private var currentPressure = 0f
    private var currentTool = ToolType.UNKNOWN

    private val probePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = AndroidColor.BLUE
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = AndroidColor.rgb(226, 232, 240)
    }

    private val eraserCursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = AndroidColor.rgb(239, 68, 68)
    }

    private val eraserFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.argb(35, 239, 68, 68)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Se estiver em modo de replay, desabilita nova captura de toques para evitar sobreposição
        if (isReplayMode) {
            return false
        }

        // 1. Ingestão segura pelo pipeline de captura
        pipeline.onMotionEvent(event)

        // 2. Extração de amostra de telemetria
        val sample = DeviceCapabilityInspector.extractSample(event)
        currentX = sample.x
        currentY = sample.y
        currentPressure = sample.pressure ?: 0.5f
        currentTool = sample.toolType

        probePaint.color = when (currentTool) {
            ToolType.STYLUS -> AndroidColor.rgb(16, 185, 129)
            ToolType.ERASER -> AndroidColor.rgb(239, 68, 68)
            ToolType.FINGER -> AndroidColor.rgb(245, 158, 11)
            ToolType.MOUSE -> AndroidColor.rgb(37, 99, 235)
            ToolType.UNKNOWN -> AndroidColor.DKGRAY
        }

        onSampleListener?.invoke(sample)
        invalidate()
        return true
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (isReplayMode) {
            return false
        }

        pipeline.onHoverEvent(event)
        val sample = DeviceCapabilityInspector.extractSample(event)
        currentX = sample.x
        currentY = sample.y
        currentPressure = sample.pressure ?: 0f
        currentTool = sample.toolType

        probePaint.color = AndroidColor.rgb(16, 185, 129)
        onSampleListener?.invoke(sample)
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 0. Renderizar as pautas caligráficas no plano de fundo
        guidelineRenderer.draw(canvas, width.toFloat(), height.toFloat(), guidelineConfig)

        // Modo Replay: renderiza exatamente o frame temporal computado
        if (isReplayMode && currentReplayFrame != null) {
            val rf = currentReplayFrame!!
            rendererManager.activeRenderer.renderAll(
                canvas = canvas,
                strokes = rf.completedStrokes,
                activeStroke = rf.activeStroke
            )
            return
        }

        val activeStroke = pipeline.getActiveStrokePreview()

        // 1. Delegar a renderização de todos os traços ao motor ativo (Ink API, Bézier ou Raw)
        rendererManager.activeRenderer.renderAll(
            canvas = canvas,
            strokes = repository.allStrokes,
            activeStroke = if (activeStroke?.tool != ToolType.ERASER) activeStroke else null
        )

        // 2. Feedback visual da borracha quando em uso
        if (currentTool == ToolType.ERASER || activeStroke?.tool == ToolType.ERASER) {
            if (currentX >= 0 && currentY >= 0) {
                canvas.drawCircle(currentX, currentY, 28f, eraserFillPaint)
                canvas.drawCircle(currentX, currentY, 28f, eraserCursorPaint)
            }
        } else if (currentX >= 0 && currentY >= 0) {
            // 3. Desenhar mira e raio de pressão em tempo real para o stylus
            canvas.drawLine(0f, currentY, width.toFloat(), currentY, crosshairPaint)
            canvas.drawLine(currentX, 0f, currentX, height.toFloat(), crosshairPaint)

            val radius = 8f + (currentPressure * 24f)
            canvas.drawCircle(currentX, currentY, radius, probePaint)
        }
    }
}


