package com.scribe.caligrafia.expansions.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.expansions.backup.BackupImportResult
import com.scribe.caligrafia.expansions.backup.BackupInspectionResult
import com.scribe.caligrafia.expansions.backup.BackupSummary
import com.scribe.caligrafia.expansions.backup.ScribeBackupManager
import com.scribe.caligrafia.expansions.passage.LocalPassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageCatalog
import com.scribe.caligrafia.expansions.passage.PassageCategory
import com.scribe.caligrafia.expansions.passage.PassageCopyRecord
import com.scribe.caligrafia.expansions.passage.PassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageItem
import com.scribe.caligrafia.expansions.passage.PassagePacingEngine
import com.scribe.caligrafia.expansions.passage.PassagePacingResult
import com.scribe.caligrafia.expansions.signature.SignatureAttempt
import com.scribe.caligrafia.expansions.signature.SignatureBaselineStore
import com.scribe.caligrafia.expansions.signature.SignatureConsistencyEngine
import com.scribe.caligrafia.expansions.signature.SignatureConsistencyReport
import com.scribe.caligrafia.expansions.signature.SignatureExportSource
import com.scribe.caligrafia.expansions.signature.SignatureExporter
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import com.scribe.caligrafia.expansions.watch.IWatchCompanionBridge
import com.scribe.caligrafia.expansions.watch.WatchCompanionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

enum class ExpansionsTab(val title: String) {
    ALPHABET("Meu Alfabeto"),
    TEACHER("Professor"),
    STYLES("Estilos"),
    SIGNATURE("Assinaturas"),
    TEXTS("Cópia de Textos"),
    BACKUP("Backup"),
    SPEN_SETTINGS("S Pen e Watch"),
    STYLUS_LAB("Laboratório"),
    PREFERENCES("Preferências")
}

data class ExpansionsUiState(
    val activeTab: ExpansionsTab = ExpansionsTab.ALPHABET,
    // Assinatura
    val baselineAttempt: SignatureAttempt? = null,
    val currentStrokes: List<Stroke> = emptyList(),
    val consistencyReport: SignatureConsistencyReport? = null,
    val exportedSvgSnippet: String? = null,
    val selectedSignatureExportSource: SignatureExportSource = SignatureExportSource.CURRENT_ATTEMPT,
    // Textos
    val selectedCategory: PassageCategory = PassageCategory.PANGRAMS,
    val selectedPassage: PassageItem = PassageCatalog.allPassages.first(),
    val selectedCopyStyleId: String = "cursiva_escolar_br",
    val copyHistory: List<PassageCopyRecord> = emptyList(),
    val passagePacingResult: PassagePacingResult? = null,
    val passageStartTimeMs: Long = 0L,
    // Backup
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isInspectingBackup: Boolean = false,
    val lastBackupSummary: BackupSummary? = null,
    val lastImportResult: BackupImportResult? = null,
    val pendingBackupInspection: BackupInspectionResult? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRestoreDisplayName: String? = null,
    val restoreRevision: Long = 0L,
    // S Pen & Watch
    val pressureCurve: PressureCurveType = PressureCurveType.LINEAR,
    val postureAlertMinutes: Int = 15,
    val isPostureReminderEnabled: Boolean = true,
    val isWatchConnected: Boolean = false,
    // Preferências (Fluxo 12)
    val isLeftHanded: Boolean = false,
    val isHighContrast: Boolean = false,
    val showGuideNumbers: Boolean = true,
    val dailyPracticeGoalMinutes: Int = 15,
    // Notificações
    val snackbarMessage: String? = null
)

class ExpansionsViewModel @JvmOverloads constructor(
    application: Application,
    private val backupManager: ScribeBackupManager = ScribeBackupManager(
        application.filesDir ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_test_files")
    ),
    private val watchBridge: IWatchCompanionBridge = WatchCompanionAdapter(application),
    private val copyRepository: PassageCopyRepository = LocalPassageCopyRepository(
        File(application.filesDir ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_test_files"), "passage_copies")
    )
) : AndroidViewModel(application) {

    private val signaturesDir = File(
        application.filesDir ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_test_files"),
        "signatures"
    )
    private val signatureBaselineStore = SignatureBaselineStore(signaturesDir)

    private val _uiState = MutableStateFlow(ExpansionsUiState())
    val uiState: StateFlow<ExpansionsUiState> = _uiState.asStateFlow()

    init {
        loadBaselineSignature()
        loadCopyHistory()
        reloadPreferencesState()
    }

    fun setTab(tab: ExpansionsTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setLeftHanded(enabled: Boolean) {
        _uiState.update { it.copy(isLeftHanded = enabled) }
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_left_handed", enabled).apply()
        } catch (_: Throwable) {}
    }

    fun setHighContrast(enabled: Boolean) {
        _uiState.update { it.copy(isHighContrast = enabled) }
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_high_contrast", enabled).apply()
        } catch (_: Throwable) {}
    }

    fun setShowGuideNumbers(enabled: Boolean) {
        _uiState.update { it.copy(showGuideNumbers = enabled) }
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("show_guide_numbers", enabled).apply()
        } catch (_: Throwable) {}
    }

    fun setDailyPracticeGoalMinutes(minutes: Int) {
        _uiState.update { it.copy(dailyPracticeGoalMinutes = minutes) }
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putInt("daily_goal_minutes", minutes).apply()
        } catch (_: Throwable) {}
    }

    // --- Assinaturas ---

    fun onSignatureStrokesChanged(strokes: List<Stroke>) {
        val defensiveCopy = strokes.toList()
        _uiState.update { it.copy(currentStrokes = defensiveCopy, exportedSvgSnippet = null) }

        val baseline = _uiState.value.baselineAttempt
        if (baseline != null && defensiveCopy.isNotEmpty()) {
            val baselineMetrics = SignatureConsistencyEngine.computeMetrics(baseline.strokes)
            val attemptMetrics = SignatureConsistencyEngine.computeMetrics(defensiveCopy)
            val report = SignatureConsistencyEngine.evaluateConsistency(baselineMetrics, attemptMetrics)
            _uiState.update { it.copy(consistencyReport = report) }
        } else if (defensiveCopy.isEmpty()) {
            _uiState.update { it.copy(consistencyReport = null) }
        }
    }

    fun startNewSignatureAttempt() {
        _uiState.update {
            it.copy(
                currentStrokes = emptyList(),
                consistencyReport = null,
                exportedSvgSnippet = null,
                selectedSignatureExportSource = SignatureExportSource.CURRENT_ATTEMPT
            )
        }
    }

    fun selectSignatureExportSource(source: SignatureExportSource) {
        _uiState.update { it.copy(selectedSignatureExportSource = source, exportedSvgSnippet = null) }
    }

    fun saveAsBaseline(): Boolean {
        val strokes = _uiState.value.currentStrokes
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Desenhe a assinatura antes de salvar como referência.") }
            return false
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

        if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE || maxX == -Float.MAX_VALUE || maxY == -Float.MAX_VALUE) {
            _uiState.update { it.copy(snackbarMessage = "A tentativa não contém pontos válidos para referência.") }
            return false
        }

        val firstStart = strokes.minOf { it.startedAtMs }
        val lastEnd = strokes.maxOf { it.endedAtMs }
        val duration = (lastEnd - firstStart).coerceAtLeast(1L)
        val attempt = SignatureAttempt(
            id = UUID.randomUUID().toString(),
            strokes = strokes.toList(),
            durationMs = duration,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY
        )

        return try {
            val persisted = signatureBaselineStore.save(attempt)
            _uiState.update {
                it.copy(
                    baselineAttempt = persisted,
                    selectedSignatureExportSource = SignatureExportSource.SAVED_REFERENCE,
                    snackbarMessage = "Referência de assinatura salva e verificada."
                )
            }
            true
        } catch (e: Throwable) {
            _uiState.update {
                it.copy(snackbarMessage = "Falha ao salvar referência; a anterior foi preservada: ${e.message}")
            }
            false
        }
    }

    private fun signatureStrokesFor(source: SignatureExportSource): List<Stroke> {
        return when (source) {
            SignatureExportSource.CURRENT_ATTEMPT -> _uiState.value.currentStrokes
            SignatureExportSource.SAVED_REFERENCE -> _uiState.value.baselineAttempt?.strokes ?: emptyList()
        }
    }

    fun generateSvg(source: SignatureExportSource = _uiState.value.selectedSignatureExportSource): String? {
        val strokes = signatureStrokesFor(source)
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "${source.displayName} não possui traços para exportar.") }
            return null
        }
        val svg = SignatureExporter.exportToSvg(strokes, 1080, 500)
        _uiState.update { it.copy(exportedSvgSnippet = svg) }
        return svg
    }

    fun writeSvg(
        outputStream: OutputStream,
        source: SignatureExportSource = _uiState.value.selectedSignatureExportSource
    ): Boolean {
        val svg = generateSvg(source) ?: return false
        return try {
            val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
            writer.write(svg)
            writer.flush()
            outputStream.flush()
            _uiState.update { it.copy(snackbarMessage = "SVG exportado com sucesso.") }
            true
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "Erro ao exportar SVG: ${e.message}") }
            false
        }
    }

    fun writePng(
        outputStream: OutputStream,
        source: SignatureExportSource = _uiState.value.selectedSignatureExportSource
    ): Boolean {
        val strokes = signatureStrokesFor(source)
        if (strokes.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "${source.displayName} não possui traços para exportar.") }
            return false
        }

        return try {
            val bitmap = SignatureExporter.exportToTransparentPng(strokes, 1200, 600)
            try {
                val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                if (!compressed) throw java.io.IOException("Falha na compressão PNG")
            } finally {
                bitmap.recycle()
            }
            _uiState.update { it.copy(snackbarMessage = "PNG transparente exportado com sucesso.") }
            true
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "Erro ao exportar PNG: ${e.message}") }
            false
        }
    }

    fun exportSignatureToUri(uri: Uri, mimeType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<Application>().contentResolver
                resolver.openOutputStream(uri, "wt")?.use { output ->
                    val ok = when (mimeType) {
                        "image/svg+xml" -> writeSvg(output)
                        "image/png" -> writePng(output)
                        else -> false
                    }
                    if (!ok) throw java.io.IOException("Formato ou conteúdo indisponível")
                } ?: throw java.io.IOException("Destino não pôde ser aberto para escrita")
            } catch (e: Throwable) {
                _uiState.update { it.copy(snackbarMessage = "Falha ao gravar arquivo escolhido: ${e.message}") }
            }
        }
    }

    // Compatibilidade com rotas internas antigas; a UI F5 usa URI externo.
    fun exportSvg(outputDir: File): File? {
        val source = _uiState.value.selectedSignatureExportSource
        val strokes = signatureStrokesFor(source)
        if (strokes.isEmpty()) return null
        return try {
            outputDir.mkdirs()
            val file = File(outputDir, "assinatura_${System.currentTimeMillis()}.svg")
            FileOutputStream(file).use { fos ->
                val svg = SignatureExporter.exportToSvg(strokes, 1080, 500)
                val writer = OutputStreamWriter(fos, Charsets.UTF_8)
                writer.write(svg)
                writer.flush()
                fos.fd.sync()
                _uiState.update { it.copy(exportedSvgSnippet = svg) }
            }
            file
        } catch (_: Throwable) { null }
    }

    fun exportPng(outputDir: File): File? {
        val source = _uiState.value.selectedSignatureExportSource
        val strokes = signatureStrokesFor(source)
        if (strokes.isEmpty()) return null
        return try {
            val bitmap = SignatureExporter.exportToTransparentPng(strokes, 1200, 600)
            outputDir.mkdirs()
            val file = File(outputDir, "assinatura_${System.currentTimeMillis()}.png")
            try {
                FileOutputStream(file).use { fos ->
                    val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.fd.sync()
                    if (!compressed) throw java.io.IOException("Falha na compressão PNG")
                }
            } finally {
                bitmap.recycle()
            }
            file
        } catch (_: Throwable) { null }
    }

    private fun loadBaselineSignature() {
        try {
            val attempt = signatureBaselineStore.load()
            _uiState.update { it.copy(baselineAttempt = attempt) }
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "A referência salva não pôde ser carregada: ${e.message}") }
        }
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
        _uiState.update { it.copy(selectedPassage = passage, passagePacingResult = null) }
    }

    fun evaluatePassage(durationMs: Long) {
        val result = PassagePacingEngine.evaluatePacing(_uiState.value.selectedPassage, durationMs)
        _uiState.update { it.copy(passagePacingResult = result) }
    }

    fun selectCopyStyle(styleId: String) {
        _uiState.update { it.copy(selectedCopyStyleId = styleId) }
    }

    fun loadCopyHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = copyRepository.listAllRecords()
            _uiState.update { it.copy(copyHistory = records) }
        }
    }

    // --- Backup & Restauração ---

    fun createBackup(destinationFile: File, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true) }
            try {
                snapshotPreferencesForBackup()
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
                if (destinationFile.exists()) destinationFile.delete()
                _uiState.update { it.copy(isExporting = false, snackbarMessage = "Falha ao criar backup: ${e.message}") }
                onComplete?.invoke(false)
            }
        }
    }

    fun createBackup(uri: Uri, displayName: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true) }
            try {
                snapshotPreferencesForBackup()
                val resolver = getApplication<Application>().contentResolver
                val summary = resolver.openOutputStream(uri, "wt")?.use { output ->
                    backupManager.exportBackup(output)
                } ?: throw java.io.IOException("Destino do backup não pôde ser aberto")
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastBackupSummary = summary,
                        snackbarMessage = "Backup exportado com sucesso: $displayName (${summary.totalBytes / 1024} KB)"
                    )
                }
                onComplete?.invoke(true)
            } catch (e: Throwable) {
                _uiState.update { it.copy(isExporting = false, snackbarMessage = "Falha ao criar backup: ${e.message}") }
                onComplete?.invoke(false)
            }
        }
    }

    fun inspectBackup(uri: Uri, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isInspectingBackup = true,
                    pendingBackupInspection = null,
                    pendingRestoreUri = null,
                    pendingRestoreDisplayName = null
                )
            }
            try {
                val resolver = getApplication<Application>().contentResolver
                val inspection = resolver.openInputStream(uri)?.use { backupManager.inspectBackup(it) }
                    ?: BackupInspectionResult(isValid = false, errorMessage = "Arquivo não pôde ser aberto")
                if (inspection.isValid) {
                    _uiState.update {
                        it.copy(
                            isInspectingBackup = false,
                            pendingBackupInspection = inspection,
                            pendingRestoreUri = uri,
                            pendingRestoreDisplayName = displayName
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isInspectingBackup = false,
                            snackbarMessage = "Pacote rejeitado: ${inspection.errorMessage}"
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isInspectingBackup = false, snackbarMessage = "Falha ao validar backup: ${e.message}") }
            }
        }
    }

    fun cancelPendingRestore() {
        _uiState.update {
            it.copy(
                pendingBackupInspection = null,
                pendingRestoreUri = null,
                pendingRestoreDisplayName = null
            )
        }
    }

    fun confirmPendingRestore(onComplete: ((Boolean) -> Unit)? = null) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val resolver = getApplication<Application>().contentResolver
                val result = resolver.openInputStream(uri)?.use { backupManager.importBackup(it) }
                    ?: BackupImportResult(isSuccess = false, errorMessage = "Arquivo não pôde ser reaberto")
                afterRestore(result, onComplete)
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingBackupInspection = null,
                        pendingRestoreUri = null,
                        pendingRestoreDisplayName = null,
                        snackbarMessage = "Erro ao restaurar backup: ${e.message}"
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
                val result = sourceFile.inputStream().use { backupManager.importBackup(it) }
                afterRestore(result, onComplete)
            } catch (e: Throwable) {
                _uiState.update { it.copy(isImporting = false, snackbarMessage = "Erro ao ler arquivo de backup: ${e.message}") }
                onComplete?.invoke(false)
            }
        }
    }

    fun restoreBackup(inputStream: InputStream, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val result = backupManager.importBackup(inputStream)
                afterRestore(result, onComplete)
            } catch (e: Throwable) {
                _uiState.update { it.copy(isImporting = false, snackbarMessage = "Erro ao ler arquivo de backup: ${e.message}") }
                onComplete?.invoke(false)
            }
        }
    }

    private fun afterRestore(result: BackupImportResult, onComplete: ((Boolean) -> Unit)?) {
        if (result.isSuccess) {
            restorePreferencesFromBackupSnapshot()
            loadBaselineSignature()
            loadCopyHistory()
            reloadPreferencesState()
        }
        _uiState.update {
            it.copy(
                isImporting = false,
                pendingBackupInspection = null,
                pendingRestoreUri = null,
                pendingRestoreDisplayName = null,
                lastImportResult = result,
                restoreRevision = if (result.isSuccess) it.restoreRevision + 1 else it.restoreRevision,
                snackbarMessage = if (result.isSuccess) {
                    "Backup restaurado e verificado. O aplicativo recarregará os repositórios locais."
                } else {
                    result.errorMessage ?: "Restauração não concluída."
                }
            )
        }
        onComplete?.invoke(result.isSuccess)
    }

    private fun snapshotPreferencesForBackup() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
        val file = File(app.filesDir, "preferences_snapshot.txt")
        val temp = File.createTempFile("preferences_snapshot_", ".tmp", app.filesDir)
        val entries = listOf(
            "pressure_curve=${prefs.getString("pressure_curve", PressureCurveType.LINEAR.name)}",
            "is_left_handed=${prefs.getBoolean("is_left_handed", false)}",
            "is_high_contrast=${prefs.getBoolean("is_high_contrast", false)}",
            "show_guide_numbers=${prefs.getBoolean("show_guide_numbers", true)}",
            "daily_goal_minutes=${prefs.getInt("daily_goal_minutes", 15)}"
        )
        try {
            FileOutputStream(temp).use { fos ->
                fos.write(entries.joinToString("\n").toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            try {
                Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Throwable) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun restorePreferencesFromBackupSnapshot() {
        val app = getApplication<Application>()
        val file = File(app.filesDir, "preferences_snapshot.txt")
        if (!file.exists()) return
        val values = file.readLines(Charsets.UTF_8)
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .toMap()
        val prefs = app.getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("pressure_curve", values["pressure_curve"] ?: PressureCurveType.LINEAR.name)
            .putBoolean("is_left_handed", values["is_left_handed"]?.toBooleanStrictOrNull() ?: false)
            .putBoolean("is_high_contrast", values["is_high_contrast"]?.toBooleanStrictOrNull() ?: false)
            .putBoolean("show_guide_numbers", values["show_guide_numbers"]?.toBooleanStrictOrNull() ?: true)
            .putInt("daily_goal_minutes", values["daily_goal_minutes"]?.toIntOrNull() ?: 15)
            .commit()
    }

    private fun reloadPreferencesState() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
        val curve = try {
            prefs.getString("pressure_curve", PressureCurveType.LINEAR.name)?.let(PressureCurveType::valueOf)
        } catch (_: Throwable) { PressureCurveType.LINEAR } ?: PressureCurveType.LINEAR

        _uiState.update {
            it.copy(
                isWatchConnected = watchBridge.isWatchConnected(),
                postureAlertMinutes = watchBridge.postureAlertThresholdMinutes,
                isPostureReminderEnabled = watchBridge.isPostureReminderEnabled,
                pressureCurve = curve,
                isLeftHanded = prefs.getBoolean("is_left_handed", false),
                isHighContrast = prefs.getBoolean("is_high_contrast", false),
                showGuideNumbers = prefs.getBoolean("show_guide_numbers", true),
                dailyPracticeGoalMinutes = prefs.getInt("daily_goal_minutes", 15)
            )
        }
    }

    // --- S Pen & Watch ---

    fun setPressureCurve(curve: PressureCurveType) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("pressure_curve", curve.name).apply()
        } catch (_: Throwable) {}

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

    fun notifyUser(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}
