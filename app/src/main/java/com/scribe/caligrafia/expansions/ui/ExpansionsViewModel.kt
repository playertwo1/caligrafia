package com.scribe.caligrafia.expansions.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.core.model.InputMode
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
import com.scribe.caligrafia.expansions.transfer.F5DocumentTransferActivity
import com.scribe.caligrafia.expansions.watch.IWatchCompanionBridge
import com.scribe.caligrafia.expansions.watch.WatchCompanionAdapter
import com.scribe.caligrafia.preferences.ScribePreferencesStore
import com.scribe.caligrafia.preferences.ScribePreferences
import com.scribe.caligrafia.preferences.ToolbarSide
import com.scribe.caligrafia.preferences.TextScaleOption
import com.scribe.caligrafia.preferences.GuideContrastOption
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
    val baselineAttempt: SignatureAttempt? = null,
    val currentStrokes: List<Stroke> = emptyList(),
    val consistencyReport: SignatureConsistencyReport? = null,
    val exportedSvgSnippet: String? = null,
    val selectedSignatureExportSource: SignatureExportSource = SignatureExportSource.CURRENT_ATTEMPT,
    val selectedCategory: PassageCategory = PassageCategory.PANGRAMS,
    val selectedPassage: PassageItem = PassageCatalog.allPassages.first(),
    val selectedCopyStyleId: String = "cursiva_escolar_br",
    val copyHistory: List<PassageCopyRecord> = emptyList(),
    val passagePacingResult: PassagePacingResult? = null,
    val passageStartTimeMs: Long = 0L,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isInspectingBackup: Boolean = false,
    val lastBackupSummary: BackupSummary? = null,
    val lastImportResult: BackupImportResult? = null,
    val pendingBackupInspection: BackupInspectionResult? = null,
    val pendingRestoreUri: Uri? = null,
    val pendingRestoreDisplayName: String? = null,
    val restoreRevision: Long = 0L,
    val pressureCurve: PressureCurveType = PressureCurveType.LINEAR,
    val postureAlertMinutes: Int = 15,
    val isPostureReminderEnabled: Boolean = true,
    val isWatchConnected: Boolean = false,
    val isLeftHanded: Boolean = false,
    val isHighContrast: Boolean = false,
    val showGuideNumbers: Boolean = true,
    val dailyPracticeGoalMinutes: Int = 15,
    val preferences: ScribePreferences = ScribePreferences(),
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
    private val preferencesStore = ScribePreferencesStore(application)
    private var pendingBaselineReplacementToken: String? = null
    private var pendingBaselineReplacementAtMs: Long = 0L

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
        updatePreferences { it.copy(isLeftHanded = enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        updatePreferences { it.copy(guideContrast = if (enabled) GuideContrastOption.HIGH else GuideContrastOption.NORMAL) }
    }

    fun setShowGuideNumbers(enabled: Boolean) {
        updatePreferences { it.copy(showGuideNumbers = enabled) }
    }

    fun setDailyPracticeGoalMinutes(minutes: Int) {
        updatePreferences { it.copy(dailyPracticeGoalMinutes = minutes) }
    }

    // --- Assinaturas ---

    fun onSignatureStrokesChanged(strokes: List<Stroke>) {
        pendingBaselineReplacementToken = null
        pendingBaselineReplacementAtMs = 0L
        val defensiveCopy = strokes.toList()
        _uiState.update {
            it.copy(
                currentStrokes = defensiveCopy,
                exportedSvgSnippet = null,
                selectedSignatureExportSource = SignatureExportSource.CURRENT_ATTEMPT
            )
        }

        val baseline = _uiState.value.baselineAttempt
        if (baseline != null && defensiveCopy.isNotEmpty()) {
            val baselineMetrics = SignatureConsistencyEngine.computeMetrics(baseline.strokes)
            val attemptMetrics = SignatureConsistencyEngine.computeMetrics(defensiveCopy)
            _uiState.update {
                it.copy(consistencyReport = SignatureConsistencyEngine.evaluateConsistency(baselineMetrics, attemptMetrics))
            }
        } else if (defensiveCopy.isEmpty()) {
            _uiState.update { it.copy(consistencyReport = null) }
        }
    }

    fun startNewSignatureAttempt() {
        pendingBaselineReplacementToken = null
        pendingBaselineReplacementAtMs = 0L
        File(signaturesDir, "current_attempt.scribe").delete()
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

        val baselineExists = _uiState.value.baselineAttempt != null
        val token = replacementToken(strokes)
        val now = System.currentTimeMillis()
        val confirmationStillValid = pendingBaselineReplacementToken == token &&
            now - pendingBaselineReplacementAtMs <= REPLACEMENT_CONFIRM_WINDOW_MS

        if (baselineExists && !confirmationStillValid) {
            pendingBaselineReplacementToken = token
            pendingBaselineReplacementAtMs = now
            _uiState.update {
                it.copy(snackbarMessage = "Já existe uma referência. Toque novamente em Gravar Referência em até 20 s para CONFIRMAR a substituição. A referência atual continua preservada até lá.")
            }
            return false
        }

        pendingBaselineReplacementToken = null
        pendingBaselineReplacementAtMs = 0L

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

        val attempt = SignatureAttempt(
            id = UUID.randomUUID().toString(),
            strokes = strokes.toList(),
            durationMs = (strokes.maxOf { it.endedAtMs } - strokes.minOf { it.startedAtMs }).coerceAtLeast(1L),
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
                    snackbarMessage = if (baselineExists) {
                        "Referência substituída somente após confirmação e verificação da gravação."
                    } else {
                        "Referência de assinatura salva e verificada."
                    }
                )
            }
            true
        } catch (e: Throwable) {
            _uiState.update { it.copy(snackbarMessage = "Falha ao salvar referência; a anterior foi preservada: ${e.message}") }
            false
        }
    }

    private fun replacementToken(strokes: List<Stroke>): String = strokes.joinToString("|") { stroke ->
        "${stroke.id}:${stroke.points.size}:${stroke.startedAtMs}:${stroke.endedAtMs}"
    }

    private fun signatureStrokesFor(source: SignatureExportSource): List<Stroke> = when (source) {
        SignatureExportSource.CURRENT_ATTEMPT -> _uiState.value.currentStrokes
        SignatureExportSource.SAVED_REFERENCE -> _uiState.value.baselineAttempt?.strokes ?: emptyList()
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

    fun writeSvg(outputStream: OutputStream, source: SignatureExportSource = _uiState.value.selectedSignatureExportSource): Boolean {
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

    fun writePng(outputStream: OutputStream, source: SignatureExportSource = _uiState.value.selectedSignatureExportSource): Boolean {
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
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output ->
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

    /**
     * Rota usada pelo botão legado da tela. Em F5, o argumento de diretório é ignorado para destino:
     * arquivos temporários são preparados no cache e o usuário escolhe nome/destino via SAF.
     */
    fun exportSvg(outputDir: File): File? {
        return launchSignaturePickerExport("image/svg+xml", "assinatura.svg")
    }

    fun exportPng(outputDir: File): File? {
        return launchSignaturePickerExport("image/png", "assinatura.png")
    }

    private fun launchSignaturePickerExport(mime: String, suggestedName: String): File? {
        val current = signatureStrokesFor(SignatureExportSource.CURRENT_ATTEMPT)
        val reference = signatureStrokesFor(SignatureExportSource.SAVED_REFERENCE)
        if (current.isEmpty() && reference.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Nenhuma tentativa ou referência disponível para exportar.") }
            return null
        }

        val app = getApplication<Application>()
        val tempDir = File(app.cacheDir, "f5_exports").apply { mkdirs() }
        val extension = if (mime == "image/svg+xml") "svg" else "png"

        fun materialize(source: SignatureExportSource, strokes: List<Stroke>, suffix: String): Pair<File, File>? {
            if (strokes.isEmpty()) return null
            val dataFile = File(tempDir, "signature_${suffix}_${System.nanoTime()}.$extension")
            val previewFile = File(tempDir, "signature_${suffix}_${System.nanoTime()}_preview.png")
            try {
                if (mime == "image/svg+xml") {
                    FileOutputStream(dataFile).use { fos ->
                        val writer = OutputStreamWriter(fos, Charsets.UTF_8)
                        val svg = SignatureExporter.exportToSvg(strokes, 1080, 500)
                        writer.write(svg)
                        writer.flush()
                        fos.fd.sync()
                        if (source == SignatureExportSource.CURRENT_ATTEMPT) {
                            _uiState.update { it.copy(exportedSvgSnippet = svg) }
                        }
                    }
                } else {
                    val bitmap = SignatureExporter.exportToTransparentPng(strokes, 1200, 600)
                    try {
                        FileOutputStream(dataFile).use { fos ->
                            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) error("Falha ao gerar PNG")
                            fos.fd.sync()
                        }
                    } finally { bitmap.recycle() }
                }

                val preview = SignatureExporter.exportToTransparentPng(strokes, 900, 380)
                try {
                    FileOutputStream(previewFile).use { fos ->
                        if (!preview.compress(Bitmap.CompressFormat.PNG, 100, fos)) error("Falha ao gerar preview")
                        fos.fd.sync()
                    }
                } finally { preview.recycle() }
                return dataFile to previewFile
            } catch (_: Throwable) {
                dataFile.delete()
                previewFile.delete()
                return null
            }
        }

        val first = materialize(SignatureExportSource.CURRENT_ATTEMPT, current, "current")
        val second = materialize(SignatureExportSource.SAVED_REFERENCE, reference, "reference")
        val primary = first ?: second ?: return null

        val launchIntent = Intent(app, F5DocumentTransferActivity::class.java).apply {
            putExtra(F5DocumentTransferActivity.EXTRA_MODE, F5DocumentTransferActivity.MODE_EXPORT_CHOICE)
            putExtra(F5DocumentTransferActivity.EXTRA_FILE_PATH, primary.first.absolutePath)
            putExtra(F5DocumentTransferActivity.EXTRA_PREVIEW_PATH, primary.second.absolutePath)
            putExtra(F5DocumentTransferActivity.EXTRA_LABEL, if (first != null) "Tentativa atual" else "Referência salva")
            if (first != null && second != null) {
                putExtra(F5DocumentTransferActivity.EXTRA_FILE_PATH_2, second.first.absolutePath)
                putExtra(F5DocumentTransferActivity.EXTRA_PREVIEW_PATH_2, second.second.absolutePath)
                putExtra(F5DocumentTransferActivity.EXTRA_LABEL_2, "Referência salva")
            }
            putExtra(F5DocumentTransferActivity.EXTRA_MIME, mime)
            putExtra(F5DocumentTransferActivity.EXTRA_SUGGESTED_NAME, suggestedName)
            putExtra(F5DocumentTransferActivity.EXTRA_DIALOG_TITLE, "Fonte da assinatura")
            putExtra(
                F5DocumentTransferActivity.EXTRA_CHOICE_DESCRIPTION,
                "Pré-visualize e escolha explicitamente se o arquivo deve usar a tentativa atual ou a referência salva. Isso não autentica identidade."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(launchIntent)
        return primary.first
    }

    private fun loadBaselineSignature() {
        try {
            _uiState.update { it.copy(baselineAttempt = signatureBaselineStore.load()) }
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
        _uiState.update { it.copy(passagePacingResult = PassagePacingEngine.evaluatePacing(_uiState.value.selectedPassage, durationMs)) }
    }

    fun selectCopyStyle(styleId: String) {
        _uiState.update { it.copy(selectedCopyStyleId = styleId) }
    }

    fun loadCopyHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(copyHistory = copyRepository.listAllRecords()) }
        }
    }

    // --- Backup & Restauração ---

    fun createBackup(destinationFile: File, onComplete: ((Boolean) -> Unit)? = null) {
        // Chamada sem callback oriunda do botão atual da tela: abre o hub SAF F5.
        if (onComplete == null && destinationFile.parentFile?.name == "backups") {
            val app = getApplication<Application>()
            app.startActivity(F5DocumentTransferActivity.backupHubIntent(app))
            return
        }

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
                val summary = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { output ->
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
                val inspection = getApplication<Application>().contentResolver.openInputStream(uri)?.use { backupManager.inspectBackup(it) }
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
                    _uiState.update { it.copy(isInspectingBackup = false, snackbarMessage = "Pacote rejeitado: ${inspection.errorMessage}") }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(isInspectingBackup = false, snackbarMessage = "Falha ao validar backup: ${e.message}") }
            }
        }
    }

    fun cancelPendingRestore() {
        _uiState.update {
            it.copy(pendingBackupInspection = null, pendingRestoreUri = null, pendingRestoreDisplayName = null)
        }
    }

    fun confirmPendingRestore(onComplete: ((Boolean) -> Unit)? = null) {
        val uri = _uiState.value.pendingRestoreUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val result = getApplication<Application>().contentResolver.openInputStream(uri)?.use { backupManager.importBackup(it) }
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
                afterRestore(sourceFile.inputStream().use { backupManager.importBackup(it) }, onComplete)
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
                afterRestore(backupManager.importBackup(inputStream), onComplete)
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
        check(ScribePreferencesStore(app).writeBackupSnapshot(File(app.filesDir, ScribePreferencesStore.BACKUP_FILE_NAME)))
    }

    private fun restorePreferencesFromBackupSnapshot() {
        val app = getApplication<Application>()
        ScribePreferencesStore(app).restoreBackupSnapshot(File(app.filesDir, ScribePreferencesStore.BACKUP_FILE_NAME))
    }

    private fun reloadPreferencesState() {
        val prefs = preferencesStore.load()
        _uiState.update {
            it.copy(
                isWatchConnected = watchBridge.isWatchConnected(),
                postureAlertMinutes = watchBridge.postureAlertThresholdMinutes,
                isPostureReminderEnabled = watchBridge.isPostureReminderEnabled,
                pressureCurve = prefs.pressureCurve,
                isLeftHanded = prefs.isLeftHanded,
                isHighContrast = prefs.guideContrast == GuideContrastOption.HIGH,
                showGuideNumbers = prefs.showGuideNumbers,
                dailyPracticeGoalMinutes = prefs.dailyPracticeGoalMinutes,
                preferences = prefs
            )
        }
    }

    fun setPressureCurve(curve: PressureCurveType) {
        val approved = if (curve == PressureCurveType.SIGMOID_CALLIGRAPHIC) PressureCurveType.LINEAR else curve
        updatePreferences { it.copy(pressureCurve = approved) }
        _uiState.update { it.copy(snackbarMessage = "Curva de pressão configurada: ${approved.displayName}") }
    }

    fun setToolbarSide(side: ToolbarSide?) = updatePreferences { it.copy(toolbarSideOverride = side) }
    fun setInputMode(mode: InputMode) = updatePreferences { it.copy(inputMode = mode) }
    fun setTextScale(scale: TextScaleOption) = updatePreferences { it.copy(textScale = scale) }
    fun setReduceAnimations(enabled: Boolean) = updatePreferences { it.copy(reduceAnimations = enabled) }
    fun setBreakReminder(enabled: Boolean) = updatePreferences { it.copy(breakReminderEnabled = enabled) }
    fun setBreakInterval(minutes: Int) = updatePreferences { it.copy(breakIntervalMinutes = minutes) }
    fun setVibration(enabled: Boolean) = updatePreferences { it.copy(vibrationEnabled = enabled) }

    private fun updatePreferences(transform: (ScribePreferences) -> ScribePreferences) {
        runCatching { preferencesStore.update(transform) }.onSuccess { prefs ->
            _uiState.update { it.copy(preferences = prefs, pressureCurve = prefs.pressureCurve,
                isLeftHanded = prefs.isLeftHanded, isHighContrast = prefs.guideContrast == GuideContrastOption.HIGH,
                showGuideNumbers = prefs.showGuideNumbers, dailyPracticeGoalMinutes = prefs.dailyPracticeGoalMinutes) }
        }.onFailure { _uiState.update { it.copy(snackbarMessage = "Não foi possível salvar a preferência.") } }
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
            it.copy(snackbarMessage = if (success) "Alerta postural vibratório disparado!" else "Vibrador indisponível neste dispositivo.")
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun notifyUser(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    companion object {
        private const val REPLACEMENT_CONFIRM_WINDOW_MS = 20_000L
    }
}
