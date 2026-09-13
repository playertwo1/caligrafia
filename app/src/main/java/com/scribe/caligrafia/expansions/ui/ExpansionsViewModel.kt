package com.scribe.caligrafia.expansions.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.expansions.backup.BackupImportResult
import com.scribe.caligrafia.expansions.backup.BackupSummary
import com.scribe.caligrafia.expansions.backup.ScribeBackupManager
import com.scribe.caligrafia.expansions.passage.PassageCatalog
import com.scribe.caligrafia.expansions.passage.PassageCategory
import com.scribe.caligrafia.expansions.passage.PassageItem
import com.scribe.caligrafia.expansions.passage.PassagePacingEngine
import com.scribe.caligrafia.expansions.passage.PassagePacingResult
import com.scribe.caligrafia.expansions.signature.SignatureAttempt
import com.scribe.caligrafia.expansions.signature.SignatureConsistencyEngine
import com.scribe.caligrafia.expansions.signature.SignatureConsistencyReport
import com.scribe.caligrafia.expansions.signature.SignatureExporter
import com.scribe.caligrafia.expansions.styles.PressureCalibration
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import com.scribe.caligrafia.expansions.watch.IWatchCompanionBridge
import com.scribe.caligrafia.expansions.watch.WatchCompanionAdapter
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.UUID

enum class ExpansionsTab(val title: String) {
    TEACHER("Diagnóstico"),
    ALPHABET("Meu Alfabeto"),
    SIGNATURE("Assinaturas"),
    BACKUP("Backup"),
    SPEN_SETTINGS("Caneta S Pen")
}

data class ExpansionsUiState(
    val activeTab: ExpansionsTab = ExpansionsTab.TEACHER,
    // Assinatura
    val baselineAttempt: SignatureAttempt? = null,
    val currentStrokes: List<Stroke> = emptyList(),
    val consistencyReport: SignatureConsistencyReport? = null,
    val exportedSvgSnippet: String? = null,
    // Textos
    val selectedCategory: PassageCategory = PassageCategory.PANGRAMS,
    val selectedPassage: PassageItem = PassageCatalog.allPassages.first(),
    val passagePacingResult: PassagePacingResult? = null,
    val passageStartTimeMs: Long = 0L,
    // Backup
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastBackupSummary: BackupSummary? = null,
    val lastImportResult: BackupImportResult? = null,
    // S Pen & Watch
    val pressureCurve: PressureCurveType = PressureCurveType.LINEAR,
    val postureAlertMinutes: Int = 15,
    val isPostureReminderEnabled: Boolean = true,
    val isWatchConnected: Boolean = false,
    // Notificações
    val snackbarMessage: String? = null
)

class ExpansionsViewModel @JvmOverloads constructor(
    application: Application,
    private val backupManager: ScribeBackupManager = ScribeBackupManager(
        application.filesDir ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_test_files")
    ),
    private val watchBridge: IWatchCompanionBridge = WatchCompanionAdapter(application)
) : AndroidViewModel(application) {

    private val signaturesDir = File(
        application.filesDir ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_test_files"),
        "signatures"
    )
    private val signaturePersistence = DedicatedFileStrategy(signaturesDir)

    private val _uiState = MutableStateFlow(ExpansionsUiState())
    val uiState: StateFlow<ExpansionsUiState> = _uiState.asStateFlow()

    init {
        loadBaselineSignature()
        _uiState.update {
            it.copy(
                isWatchConnected = watchBridge.isWatchConnected(),
                postureAlertMinutes = watchBridge.postureAlertThresholdMinutes,
                isPostureReminderEnabled = watchBridge.isPostureReminderEnabled
            )
        }
    }

    fun setTab(tab: ExpansionsTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // --- Assinaturas ---

    fun onSignatureStrokesChanged(strokes: List<Stroke>) {
        _uiState.update { it.copy(currentStrokes = strokes) }

        val baseline = _uiState.value.baselineAttempt
        if (baseline != null && strokes.isNotEmpty()) {
            val baselineMetrics = SignatureConsistencyEngine.computeMetrics(baseline.strokes)
            val attemptMetrics = SignatureConsistencyEngine.computeMetrics(strokes)
            val report = SignatureConsistencyEngine.evaluateConsistency(baselineMetrics, attemptMetrics)
            _uiState.update { it.copy(consistencyReport = report) }
        } else if (strokes.isEmpty()) {
            _uiState.update { it.copy(consistencyReport = null) }
        }
    }

    fun saveAsBaseline() {
        val strokes = _uiState.value.currentStrokes
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Desenhe sua assinatura antes de salvar como referência.") }
            return
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (s in strokes) {
            for (p in s.points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
        }

        val duration = (strokes.maxOf { it.endedAtMs } - strokes.first().startedAtMs).coerceAtLeast(1L)
        val attempt = SignatureAttempt(
            id = UUID.randomUUID().toString(),
            strokes = strokes.toList(),
            durationMs = duration,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY
        )

        try {
            if (!signaturesDir.exists()) signaturesDir.mkdirs()
            val scribeFile = File(signaturesDir, "baseline.scribe")
            signaturePersistence.save(scribeFile, attempt.strokes, attempt.id)
            val metaFile = File(signaturesDir, "baseline_meta.txt")
            metaFile.writeText("${attempt.id}|${attempt.durationMs}|${attempt.minX}|${attempt.minY}|${attempt.maxX}|${attempt.maxY}")
        } catch (_: Throwable) {}

        _uiState.update {
            it.copy(
                baselineAttempt = attempt,
                snackbarMessage = "Assinatura gravada como referência de calibração!"
            )
        }
    }

    fun generateSvg(): String {
        val strokes = _uiState.value.currentStrokes.ifEmpty {
            _uiState.value.baselineAttempt?.strokes ?: emptyList()
        }
        val svg = SignatureExporter.exportToSvg(strokes, 1080, 500)
        _uiState.update {
            it.copy(
                exportedSvgSnippet = svg,
                snackbarMessage = "Código SVG vetorial gerado com sucesso!"
            )
        }
        return svg
    }

    fun exportSvg(outputDir: File): File? {
        val strokes = _uiState.value.currentStrokes.ifEmpty {
            _uiState.value.baselineAttempt?.strokes ?: emptyList()
        }
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Nenhum traço de assinatura para exportar.") }
            return null
        }
        return try {
            val svg = generateSvg()
            outputDir.mkdirs()
            val file = File(outputDir, "assinatura_${System.currentTimeMillis()}.svg")
            FileOutputStream(file).use { fos ->
                val writer = OutputStreamWriter(fos, Charsets.UTF_8)
                writer.write(svg)
                writer.flush()
                fos.fd.sync()
            }
            _uiState.update { it.copy(snackbarMessage = "Arquivo SVG salvo em: ${file.name}") }
            file
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "Erro ao exportar SVG: ${e.message}") }
            null
        }
    }

    fun exportPng(outputDir: File): File? {
        val strokes = _uiState.value.currentStrokes.ifEmpty {
            _uiState.value.baselineAttempt?.strokes ?: emptyList()
        }
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Nenhum traço de assinatura para exportar.") }
            return null
        }

        return try {
            val bitmap = SignatureExporter.exportToTransparentPng(strokes, 1200, 600)
            outputDir.mkdirs()
            val file = File(outputDir, "assinatura_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.fd.sync()
            }
            _uiState.update { it.copy(snackbarMessage = "Assinatura PNG transparente salva em: ${file.name}") }
            file
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "Erro ao exportar PNG: ${e.message}") }
            null
        }
    }

    private fun loadBaselineSignature() {
        try {
            val scribeFile = File(signaturesDir, "baseline.scribe")
            val metaFile = File(signaturesDir, "baseline_meta.txt")
            if (scribeFile.exists() && metaFile.exists()) {
                val strokes = signaturePersistence.load(scribeFile)
                if (strokes.isNotEmpty()) {
                    val parts = metaFile.readText().split("|")
                    if (parts.size >= 6) {
                        val attempt = SignatureAttempt(
                            id = parts[0],
                            strokes = strokes,
                            durationMs = parts[1].toLongOrNull() ?: 1000L,
                            minX = parts[2].toFloatOrNull() ?: 0f,
                            minY = parts[3].toFloatOrNull() ?: 0f,
                            maxX = parts[4].toFloatOrNull() ?: 100f,
                            maxY = parts[5].toFloatOrNull() ?: 100f
                        )
                        _uiState.update { it.copy(baselineAttempt = attempt) }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    // --- Cópia de Textos ---

    fun selectPassageCategory(category: PassageCategory) {
        val passages = PassageCatalog.getByCategory(category)
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedPassage = passages.firstOrNull() ?: it.selectedPassage,
                passagePacingResult = null
            )
        }
    }

    fun selectPassage(passage: PassageItem) {
        _uiState.update {
            it.copy(
                selectedPassage = passage,
                passagePacingResult = null
            )
        }
    }

    fun evaluatePassage(durationMs: Long) {
        val result = PassagePacingEngine.evaluatePacing(_uiState.value.selectedPassage, durationMs)
        _uiState.update { it.copy(passagePacingResult = result) }
    }

    // --- Backup & Restauração ---

    fun createBackup(destinationFile: File, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true) }
            try {
                destinationFile.parentFile?.mkdirs()
                val summary = FileOutputStream(destinationFile).use { fos ->
                    val s = backupManager.exportBackup(fos)
                    fos.fd.sync()
                    s
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastBackupSummary = summary,
                        snackbarMessage = "Backup exportado com sucesso: ${destinationFile.name} (${summary.totalBytes / 1024} KB)"
                    )
                }
                onComplete?.invoke(true)
            } catch (e: Throwable) {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        snackbarMessage = "Falha ao criar backup: ${e.message}"
                    )
                }
                onComplete?.invoke(false)
            }
        }
    }

    fun restoreBackup(sourceFile: File, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val result = sourceFile.inputStream().use { input ->
                    backupManager.importBackup(input)
                }
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        lastImportResult = result,
                        snackbarMessage = if (result.isSuccess) {
                            "Backup restaurado com sucesso! (${result.restoredNotebooks} cadernos, ${result.restoredGlyphs} glifos)"
                        } else {
                            "Erro na restauração: ${result.errorMessage}"
                        }
                    )
                }
                onComplete?.invoke(result.isSuccess)
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        snackbarMessage = "Erro ao ler arquivo de backup: ${e.message}"
                    )
                }
                onComplete?.invoke(false)
            }
        }
    }

    // --- S Pen & Watch ---

    fun setPressureCurve(curve: PressureCurveType) {
        _uiState.update {
            it.copy(
                pressureCurve = curve,
                snackbarMessage = "Curva de pressão configurada: ${curve.displayName}"
            )
        }
    }

    fun setPostureAlertMinutes(minutes: Int) {
        val m = minutes.coerceIn(5, 60)
        watchBridge.postureAlertThresholdMinutes = m
        _uiState.update { it.copy(postureAlertMinutes = m) }
    }

    fun togglePostureReminder(enabled: Boolean) {
        watchBridge.isPostureReminderEnabled = enabled
        _uiState.update { it.copy(isPostureReminderEnabled = enabled) }
    }

    fun testPostureHaptic() {
        val success = watchBridge.sendPostureAlertHaptic()
        _uiState.update {
            it.copy(
                snackbarMessage = if (success) "Alerta postural vibratório disparado!" else "Vibrador indisponível neste dispositivo."
            )
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
