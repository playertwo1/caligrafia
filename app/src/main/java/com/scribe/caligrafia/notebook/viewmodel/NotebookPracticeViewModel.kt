package com.scribe.caligrafia.notebook.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.core.model.CalligraphyColor
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.PenThickness
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
import com.scribe.caligrafia.expansions.passage.ActiveTextCopySession
import com.scribe.caligrafia.expansions.passage.LocalPassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageCatalog
import com.scribe.caligrafia.expansions.passage.PassageCategory
import com.scribe.caligrafia.expansions.passage.PassageCopyRecord
import com.scribe.caligrafia.expansions.passage.PassageCopyRepository
import com.scribe.caligrafia.expansions.passage.PassageItem
import com.scribe.caligrafia.expansions.passage.PassagePacingEngine
import com.scribe.caligrafia.expansions.transfer.F5DocumentTransferActivity
import com.scribe.caligrafia.expansions.watch.WatchCompanionAdapter
import com.scribe.caligrafia.ink.capture.InMemoryStrokeRepository
import com.scribe.caligrafia.ink.capture.StrokeCapturePipeline
import com.scribe.caligrafia.ink.palm.PalmRejectionPolicy
import com.scribe.caligrafia.ink.renderer.SmoothedReferenceRenderer
import com.scribe.caligrafia.notebook.export.PageExportOptions
import com.scribe.caligrafia.notebook.export.PageExporter
import com.scribe.caligrafia.notebook.repository.LocalNotebookRepository
import com.scribe.caligrafia.notebook.repository.NotebookRepository
import com.scribe.caligrafia.style.engine.StyleEngine
import com.scribe.caligrafia.style.model.BuiltInStyles
import com.scribe.caligrafia.style.model.ScribeStyle
import com.scribe.caligrafia.preferences.ActiveBreakReminder
import com.scribe.caligrafia.preferences.ScribePreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Estado da interface do Caderno de Prática Caligráfica (M1 e M3, F4.12, F4.13).
 */
data class NotebookPracticeUiState(
    val isLibraryView: Boolean = false,
    val allNotebooks: List<Notebook> = emptyList(),
    val currentNotebook: Notebook? = null,
    val currentPage: NotebookPage? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 1,
    val pages: List<NotebookPage> = emptyList(),
    val toolConfig: ToolConfig = ToolConfig(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val strokeCount: Int = 0,
    val totalPoints: Int = 0,
    val isSaving: Boolean = false,
    val notificationMessage: String? = null,
    val availableStyles: List<ScribeStyle> = emptyList(),
    val currentStyle: ScribeStyle = BuiltInStyles.CURSIVA_ESCOLAR,
    val activeTextCopy: ActiveTextCopySession? = null
)

/**
 * ViewModel que orquestra a experiência de Caderno de Caligrafia (M1 e M3).
 */
class NotebookPracticeViewModel(application: Application) : AndroidViewModel(application) {

    private val baseFilesDir: File = application.filesDir
        ?: File(System.getProperty("java.io.tmpdir", "."), "scribe_notebook_test_${System.currentTimeMillis()}").apply { mkdirs() }
    private val notebookRepository: NotebookRepository = LocalNotebookRepository(baseFilesDir)
    val styleEngine = StyleEngine(File(baseFilesDir, "custom_fonts"))

    val strokeRepository = InMemoryStrokeRepository()
    val pipeline = StrokeCapturePipeline(
        palmPolicy = PalmRejectionPolicy(),
        toolConfig = ToolConfig()
    )
    val renderer = SmoothedReferenceRenderer()
    private val preferencesStore = ScribePreferencesStore(application)
    private val watchBridge = WatchCompanionAdapter(application)
    private val breakReminder = preferencesStore.load().let {
        ActiveBreakReminder(it.breakReminderEnabled, it.breakIntervalMinutes)
    }

    private val pagePersistenceMutex = Mutex()

    private val _uiState = MutableStateFlow(
        NotebookPracticeUiState(
            availableStyles = styleEngine.getAvailableStyles(),
            currentStyle = styleEngine.defaultStyle()
        )
    )
    val uiState: StateFlow<NotebookPracticeUiState> = _uiState.asStateFlow()

    private var pagesList: List<NotebookPage> = emptyList()

    init {
        val savedCurve = try {
            val prefs = application.getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.getString("pressure_curve", null)?.let { com.scribe.caligrafia.expansions.styles.PressureCurveType.valueOf(it) }
        } catch (_: Throwable) { null } ?: com.scribe.caligrafia.expansions.styles.PressureCurveType.LINEAR
        renderer.pressureCurve = savedCurve

        loadOrCreateDefaultNotebook()
    }

    fun loadOrCreateDefaultNotebook() {
        viewModelScope.launch(Dispatchers.IO) {
            val notebooks = notebookRepository.getNotebooks()
            val notebook = if (notebooks.isEmpty()) {
                notebookRepository.createNotebook("Prática Diária")
            } else {
                notebooks.first()
            }

            pagesList = notebookRepository.getPages(notebook.id)
            val firstPage = pagesList.firstOrNull()

            if (firstPage != null) {
                val initialStrokes = notebookRepository.loadPageStrokes(firstPage)
                strokeRepository.loadStrokes(initialStrokes)
            }

            _uiState.update {
                it.copy(
                    allNotebooks = notebooks,
                    currentNotebook = notebook,
                    currentPage = firstPage,
                    currentPageIndex = 0,
                    totalPages = pagesList.size.coerceAtLeast(1),
                    pages = pagesList,
                    canUndo = strokeRepository.canUndo,
                    canRedo = strokeRepository.canRedo,
                    strokeCount = strokeRepository.count,
                    totalPoints = strokeRepository.totalPointsCount
                )
            }
        }
    }

    fun showLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val notebooks = notebookRepository.getNotebooks()
            _uiState.update { it.copy(isLibraryView = true, allNotebooks = notebooks) }
        }
    }

    fun closeLibrary() {
        _uiState.update { it.copy(isLibraryView = false) }
    }

    fun selectNotebook(notebook: Notebook) {
        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                val currentPage = _uiState.value.currentPage
                if (currentPage != null) {
                    notebookRepository.savePageStrokes(currentPage, strokeRepository.allStrokes)
                }

                pagesList = notebookRepository.getPages(notebook.id)
                val firstPage = pagesList.firstOrNull() ?: notebookRepository.addPage(notebook.id, GuidelineConfig.copperplate())
                pagesList = notebookRepository.getPages(notebook.id)

                val initialStrokes = notebookRepository.loadPageStrokes(firstPage)
                strokeRepository.loadStrokes(initialStrokes)

                _uiState.update {
                    it.copy(
                        isLibraryView = false,
                        currentNotebook = notebook,
                        currentPage = firstPage,
                        currentPageIndex = 0,
                        totalPages = pagesList.size.coerceAtLeast(1),
                        pages = pagesList,
                        canUndo = strokeRepository.canUndo,
                        canRedo = strokeRepository.canRedo,
                        strokeCount = strokeRepository.count,
                        totalPoints = strokeRepository.totalPointsCount
                    )
                }
            }
        }
    }

    fun createNotebook(
        name: String,
        guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate(),
        coverStyle: String = "PAPEL_ARTESANAL"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newNotebook = notebookRepository.createNotebook(
                title = name.ifBlank { "Novo Caderno" },
                initialGuideline = guidelineConfig,
                coverStyle = coverStyle
            )
            pagesList = notebookRepository.getPages(newNotebook.id)
            val firstPage = pagesList.firstOrNull() ?: notebookRepository.addPage(newNotebook.id, guidelineConfig)
            pagesList = notebookRepository.getPages(newNotebook.id)
            strokeRepository.loadStrokes(emptyList())

            val notebooks = notebookRepository.getNotebooks()
            _uiState.update {
                it.copy(
                    isLibraryView = false,
                    allNotebooks = notebooks,
                    currentNotebook = newNotebook,
                    currentPage = firstPage,
                    currentPageIndex = 0,
                    totalPages = pagesList.size.coerceAtLeast(1),
                    pages = pagesList,
                    canUndo = false,
                    canRedo = false,
                    strokeCount = 0,
                    totalPoints = 0,
                    notificationMessage = "Caderno '${newNotebook.title}' criado com sucesso."
                )
            }
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.deleteNotebook(notebook.id)
            val remaining = notebookRepository.getNotebooks()
            val nextNotebook = remaining.firstOrNull() ?: notebookRepository.createNotebook("Meu Caderno")
            pagesList = notebookRepository.getPages(nextNotebook.id)
            val firstPage = pagesList.firstOrNull() ?: notebookRepository.addPage(nextNotebook.id, GuidelineConfig.copperplate())
            pagesList = notebookRepository.getPages(nextNotebook.id)
            val initialStrokes = notebookRepository.loadPageStrokes(firstPage)
            strokeRepository.loadStrokes(initialStrokes)

            _uiState.update {
                it.copy(
                    allNotebooks = remaining,
                    currentNotebook = nextNotebook,
                    currentPage = firstPage,
                    currentPageIndex = 0,
                    totalPages = pagesList.size.coerceAtLeast(1),
                    pages = pagesList,
                    canUndo = strokeRepository.canUndo,
                    canRedo = strokeRepository.canRedo,
                    strokeCount = strokeRepository.count,
                    totalPoints = strokeRepository.totalPointsCount,
                    notificationMessage = "Caderno excluído."
                )
            }
        }
    }

    fun renameNotebook(notebook: Notebook, newTitle: String) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 40) return
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.renameNotebook(notebook.id, cleanTitle)
            val updatedList = notebookRepository.getNotebooks()
            val current = if (_uiState.value.currentNotebook?.id == notebook.id) {
                _uiState.value.currentNotebook?.copy(title = cleanTitle)
            } else {
                _uiState.value.currentNotebook
            }
            _uiState.update {
                it.copy(
                    allNotebooks = updatedList,
                    currentNotebook = current,
                    notificationMessage = "Caderno renomeado para '$cleanTitle'."
                )
            }
        }
    }

    fun onStrokesModified() {
        _uiState.update {
            it.copy(
                canUndo = strokeRepository.canUndo,
                canRedo = strokeRepository.canRedo,
                strokeCount = strokeRepository.count,
                totalPoints = strokeRepository.totalPointsCount
            )
        }
        autoSaveCurrentPage()
    }

    fun onActiveWriting(deltaMs: Long) {
        val preferences = preferencesStore.load()
        breakReminder.enabled = preferences.breakReminderEnabled
        breakReminder.intervalMinutes = preferences.breakIntervalMinutes
        if (breakReminder.onActiveWriting(deltaMs)) {
            watchBridge.sendPostureAlertHaptic()
            _uiState.update { it.copy(notificationMessage = "Hora de uma pausa para postura e descanso da mão.") }
        }
    }

    private fun autoSaveCurrentPage() {
        val page = _uiState.value.currentPage ?: return
        val currentStrokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                _uiState.update { it.copy(isSaving = true) }
                try {
                    notebookRepository.savePageStrokes(page, currentStrokes)
                } finally {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun previousPage() {
        val currentIndex = _uiState.value.currentPageIndex
        if (currentIndex <= 0) return
        switchPage(currentIndex - 1)
    }

    fun nextPage() {
        val currentIndex = _uiState.value.currentPageIndex
        if (currentIndex >= pagesList.size - 1) return
        switchPage(currentIndex + 1)
    }

    private fun switchPage(targetIndex: Int) {
        val currentPage = _uiState.value.currentPage
        val currentStrokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                if (currentPage != null) {
                    notebookRepository.savePageStrokes(currentPage, currentStrokes)
                }

                val targetPage = pagesList.getOrNull(targetIndex) ?: return@withLock
                val targetStrokes = notebookRepository.loadPageStrokes(targetPage)
                strokeRepository.loadStrokes(targetStrokes)

                _uiState.update {
                    it.copy(
                        currentPage = targetPage,
                        currentPageIndex = targetIndex,
                        pages = pagesList,
                        canUndo = strokeRepository.canUndo,
                        canRedo = strokeRepository.canRedo,
                        strokeCount = strokeRepository.count,
                        totalPoints = strokeRepository.totalPointsCount
                    )
                }
            }
        }
    }

    fun goToPage(index: Int) {
        if (index in pagesList.indices && index != _uiState.value.currentPageIndex) {
            switchPage(index)
        }
    }

    fun deleteTargetPage(pageId: String) {
        val notebook = _uiState.value.currentNotebook ?: return
        if (pagesList.size <= 1) return

        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                notebookRepository.deletePage(notebook.id, pageId)
                pagesList = notebookRepository.getPages(notebook.id)
                val newIndex = _uiState.value.currentPageIndex.coerceAtMost(pagesList.size - 1)
                val targetPage = pagesList.getOrNull(newIndex)
                if (targetPage != null) {
                    val initialStrokes = notebookRepository.loadPageStrokes(targetPage)
                    strokeRepository.loadStrokes(initialStrokes)
                    _uiState.update {
                        it.copy(
                            currentPage = targetPage,
                            currentPageIndex = newIndex,
                            totalPages = pagesList.size,
                            pages = pagesList,
                            canUndo = strokeRepository.canUndo,
                            canRedo = strokeRepository.canRedo,
                            strokeCount = strokeRepository.count,
                            totalPoints = strokeRepository.totalPointsCount,
                            notificationMessage = "Página removida."
                        )
                    }
                }
            }
        }
    }

    fun addNewPage(guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate()) {
        val notebook = _uiState.value.currentNotebook ?: return
        val currentPage = _uiState.value.currentPage
        val currentStrokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                if (currentPage != null) {
                    notebookRepository.savePageStrokes(currentPage, currentStrokes)
                }

                val newPage = notebookRepository.addPage(notebook.id, guidelineConfig)
                pagesList = notebookRepository.getPages(notebook.id)
                val newIndex = pagesList.indexOfFirst { it.id == newPage.id }.coerceAtLeast(0)

                strokeRepository.loadStrokes(emptyList())

                _uiState.update {
                    it.copy(
                        currentPage = newPage,
                        currentPageIndex = newIndex,
                        totalPages = pagesList.size,
                        pages = pagesList,
                        canUndo = false,
                        canRedo = false,
                        strokeCount = 0,
                        totalPoints = 0,
                        notificationMessage = "Página ${newIndex + 1} criada com sucesso"
                    )
                }
            }
        }
    }

    fun duplicatePage(pageId: String) {
        val notebook = _uiState.value.currentNotebook ?: return
        val currentPage = _uiState.value.currentPage
        val currentStrokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                if (currentPage != null) {
                    notebookRepository.savePageStrokes(currentPage, currentStrokes)
                }

                val duplicated = notebookRepository.duplicatePage(notebook.id, pageId)
                if (duplicated != null) {
                    pagesList = notebookRepository.getPages(notebook.id)
                    val newIndex = pagesList.indexOfFirst { it.id == duplicated.id }.coerceAtLeast(0)
                    val targetStrokes = notebookRepository.loadPageStrokes(duplicated)
                    strokeRepository.loadStrokes(targetStrokes)

                    _uiState.update {
                        it.copy(
                            currentPage = duplicated,
                            currentPageIndex = newIndex,
                            totalPages = pagesList.size,
                            pages = pagesList,
                            canUndo = strokeRepository.canUndo,
                            canRedo = strokeRepository.canRedo,
                            strokeCount = strokeRepository.count,
                            totalPoints = strokeRepository.totalPointsCount,
                            notificationMessage = "Página duplicada com sucesso."
                        )
                    }
                }
            }
        }
    }

    fun setToolMode(mode: ToolMode) {
        val newConfig = _uiState.value.toolConfig.copy(mode = mode)
        pipeline.toolConfig = newConfig
        _uiState.update { it.copy(toolConfig = newConfig) }
    }

    fun setPenThickness(thickness: PenThickness) {
        val newConfig = _uiState.value.toolConfig.copy(
            mode = ToolMode.PEN,
            penThickness = thickness,
            customStrokeWidthPx = null
        )
        pipeline.toolConfig = newConfig
        _uiState.update { it.copy(toolConfig = newConfig) }
    }

    fun setCalligraphyColor(color: CalligraphyColor) {
        val newConfig = _uiState.value.toolConfig.copy(
            mode = ToolMode.PEN,
            color = color
        )
        pipeline.toolConfig = newConfig
        _uiState.update { it.copy(toolConfig = newConfig) }
    }

    fun setGuidelineConfig(config: GuidelineConfig) {
        val page = _uiState.value.currentPage ?: return
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.updatePageGuidelines(page.id, config)
            val updatedPage = page.copy(guidelineConfig = config)
            pagesList = pagesList.map { if (it.id == updatedPage.id) updatedPage else it }
            _uiState.update { state -> state.copy(currentPage = updatedPage) }
        }
    }

    fun selectStyle(styleId: String, adaptPageGuidelines: Boolean = false) {
        styleEngine.loadPersonalStyles()
        val style = styleEngine.getStyle(styleId)
        _uiState.update { it.copy(currentStyle = style, availableStyles = styleEngine.getAvailableStyles()) }
        if (adaptPageGuidelines) {
            val page = _uiState.value.currentPage
            val xHeight = page?.guidelineConfig?.xHeightPx ?: 50f
            setGuidelineConfig(GuidelineConfig.fromStyle(style, xHeight))
        }
    }

    fun importCustomFont(fontFile: File, name: String? = null, slantAngle: Float = 60.0f): Result<ScribeStyle> {
        val result = styleEngine.importCustomFont(fontFile, name, slantAngle)
        if (result.isSuccess) {
            val imported = result.getOrNull()
            _uiState.update {
                it.copy(
                    availableStyles = styleEngine.getAvailableStyles(),
                    currentStyle = imported ?: it.currentStyle,
                    notificationMessage = "Fonte importada com sucesso: ${imported?.name}"
                )
            }
            if (imported != null) selectStyle(imported.id, adaptPageGuidelines = true)
        } else {
            _uiState.update { it.copy(notificationMessage = "Erro ao importar fonte: ${result.exceptionOrNull()?.message}") }
        }
        return result
    }

    fun onPauseLifecycle() {
        breakReminder.onBackground()
        pipeline.flushActiveStroke(commitIfValid = true)
        val page = _uiState.value.currentPage ?: return
        val currentStrokes = strokeRepository.allStrokes
        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                notebookRepository.savePageStrokes(page, currentStrokes)
            }
        }
    }

    fun onResumeLifecycle() {
        breakReminder.onForeground()
        val savedCurve = try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.getString("pressure_curve", null)?.let { com.scribe.caligrafia.expansions.styles.PressureCurveType.valueOf(it) }
        } catch (_: Throwable) { null } ?: com.scribe.caligrafia.expansions.styles.PressureCurveType.LINEAR
        renderer.pressureCurve = savedCurve
    }

    fun undo() {
        if (strokeRepository.undo()) onStrokesModified()
    }

    fun redo() {
        if (strokeRepository.redo()) onStrokesModified()
    }

    fun clearPage() {
        strokeRepository.clear()
        onStrokesModified()
    }

    var canvasWidthPx: Float = 0f
        private set
    var canvasHeightPx: Float = 0f
        private set

    fun updateCanvasDimensions(width: Float, height: Float) {
        if (width > 0f && height > 0f) {
            canvasWidthPx = width
            canvasHeightPx = height
        }
    }

    /**
     * F5.08-F5.10: gera duas variantes fiéis e envia a escolha para o Storage Access Framework.
     * O sucesso só é mostrado pela Activity de transferência depois que o URI escolhido é gravado.
     */
    fun exportCurrentPage(context: Context) {
        val page = _uiState.value.currentPage ?: return
        val strokes = strokeRepository.allStrokes.toList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetW = 1440
                val targetH = if (canvasWidthPx > 0f && canvasHeightPx > 0f) {
                    (targetW * (canvasHeightPx / canvasWidthPx)).toInt().coerceIn(720, 4000)
                } else {
                    2560
                }
                val sourceW = canvasWidthPx.takeIf { it > 0f }
                val sourceH = canvasHeightPx.takeIf { it > 0f }
                val tempDir = File(context.cacheDir, "f5_exports").apply { mkdirs() }
                val stamp = System.nanoTime()
                val guidedFile = File(tempDir, "page_${page.id}_${stamp}_ivory_guides.png")
                val cleanFile = File(tempDir, "page_${page.id}_${stamp}_white_clean.png")

                PageExporter.exportToPng(
                    page = page,
                    strokes = strokes,
                    targetFile = guidedFile,
                    options = PageExportOptions(
                        widthPx = targetW,
                        heightPx = targetH,
                        sourceWidthPx = sourceW,
                        sourceHeightPx = sourceH,
                        includeGuidelines = true,
                        backgroundColor = Color.rgb(255, 252, 240),
                        compressQuality = 100
                    )
                )

                PageExporter.exportToPng(
                    page = page,
                    strokes = strokes,
                    targetFile = cleanFile,
                    options = PageExportOptions(
                        widthPx = targetW,
                        heightPx = targetH,
                        sourceWidthPx = sourceW,
                        sourceHeightPx = sourceH,
                        includeGuidelines = false,
                        backgroundColor = Color.WHITE,
                        compressQuality = 100
                    )
                )

                val appContext = context.applicationContext
                val transferIntent = Intent(appContext, F5DocumentTransferActivity::class.java).apply {
                    putExtra(F5DocumentTransferActivity.EXTRA_MODE, F5DocumentTransferActivity.MODE_EXPORT_CHOICE)
                    putExtra(F5DocumentTransferActivity.EXTRA_FILE_PATH, guidedFile.absolutePath)
                    putExtra(F5DocumentTransferActivity.EXTRA_PREVIEW_PATH, guidedFile.absolutePath)
                    putExtra(F5DocumentTransferActivity.EXTRA_LABEL, "Papel marfim + pautas")
                    putExtra(F5DocumentTransferActivity.EXTRA_FILE_PATH_2, cleanFile.absolutePath)
                    putExtra(F5DocumentTransferActivity.EXTRA_PREVIEW_PATH_2, cleanFile.absolutePath)
                    putExtra(F5DocumentTransferActivity.EXTRA_LABEL_2, "Branco sem pautas")
                    putExtra(F5DocumentTransferActivity.EXTRA_MIME, "image/png")
                    putExtra(F5DocumentTransferActivity.EXTRA_SUGGESTED_NAME, "pagina_${page.pageIndex + 1}.png")
                    putExtra(F5DocumentTransferActivity.EXTRA_DIALOG_TITLE, "Aparência da página exportada")
                    putExtra(
                        F5DocumentTransferActivity.EXTRA_CHOICE_DESCRIPTION,
                        "As duas opções preservam a mesma proporção lógica do canvas. Escolha a aparência e depois o nome/destino do PNG."
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(transferIntent)

                _uiState.update {
                    it.copy(notificationMessage = "Escolha a aparência e o destino no seletor do Android.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(notificationMessage = "Erro ao preparar exportação: ${e.localizedMessage}") }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    // =========================================================================
    // Cópia de Textos Clássicos e Longos no Caderno (F4.11 a F4.16)
    // =========================================================================

    val copyRepository: PassageCopyRepository = LocalPassageCopyRepository(baseFilesDir)
    private var textCopyTimerJob: kotlinx.coroutines.Job? = null

    fun startTextCopyPractice(
        passage: PassageItem,
        styleId: String = passage.recommendedStyleId,
        existingRecord: PassageCopyRecord? = null
    ) {
        selectStyle(styleId, adaptPageGuidelines = true)

        if (existingRecord != null) {
            val pageStrokes = existingRecord.strokesByPage[0] ?: emptyList()
            strokeRepository.clear()
            pageStrokes.forEach { strokeRepository.addStroke(it) }
            _uiState.update {
                it.copy(
                    activeTextCopy = ActiveTextCopySession(
                        passage = passage,
                        styleId = styleId,
                        isCollapsed = false,
                        isPaused = false,
                        elapsedSeconds = existingRecord.durationMs / 1000L,
                        recordId = existingRecord.id,
                        strokesByPage = existingRecord.strokesByPage
                    ),
                    strokeCount = pageStrokes.size,
                    totalPoints = pageStrokes.sumOf { s -> s.points.size }
                )
            }
        } else {
            _uiState.update {
                it.copy(activeTextCopy = ActiveTextCopySession(passage = passage, styleId = styleId))
            }
        }

        startTextCopyTimer()
    }

    private fun startTextCopyTimer() {
        textCopyTimerJob?.cancel()
        textCopyTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val current = _uiState.value.activeTextCopy ?: break
                if (!current.isPaused) {
                    _uiState.update { state ->
                        state.copy(
                            activeTextCopy = state.activeTextCopy?.copy(
                                elapsedSeconds = state.activeTextCopy.elapsedSeconds + 1
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleTextCopyCollapse() {
        _uiState.update {
            it.copy(activeTextCopy = it.activeTextCopy?.copy(isCollapsed = !it.activeTextCopy.isCollapsed))
        }
    }

    fun pauseTextCopy() {
        _uiState.update { it.copy(activeTextCopy = it.activeTextCopy?.copy(isPaused = true)) }

        viewModelScope.launch(Dispatchers.IO) {
            val session = _uiState.value.activeTextCopy ?: return@launch
            val currentStrokes = strokeRepository.allStrokes
            val pageIndex = _uiState.value.currentPageIndex
            val updatedMap = session.strokesByPage.toMutableMap().apply { put(pageIndex, currentStrokes) }
            val record = PassageCopyRecord(
                id = session.recordId,
                textId = session.passage.id,
                title = session.passage.title,
                author = session.passage.author,
                textContent = session.passage.lines.joinToString("\n"),
                styleId = session.styleId,
                timestampMs = System.currentTimeMillis(),
                durationMs = session.elapsedSeconds * 1000L,
                strokeCount = updatedMap.values.sumOf { it.size },
                pageCount = (updatedMap.keys.maxOrNull() ?: 0) + 1,
                strokesByPage = updatedMap,
                isCompleted = false,
                targetWpm = session.passage.targetWpm
            )
            copyRepository.saveRecord(record)
        }
    }

    fun resumeTextCopy() {
        _uiState.update { it.copy(activeTextCopy = it.activeTextCopy?.copy(isPaused = false)) }
    }

    suspend fun finishTextCopyPractice(): PassageCopyRecord? {
        val session = _uiState.value.activeTextCopy ?: return null
        textCopyTimerJob?.cancel()

        val currentStrokes = strokeRepository.allStrokes
        val pageIndex = _uiState.value.currentPageIndex
        val updatedMap = session.strokesByPage.toMutableMap().apply { put(pageIndex, currentStrokes) }
        val durationMs = (session.elapsedSeconds * 1000L).coerceAtLeast(1000L)
        val pacing = PassagePacingEngine.evaluatePacing(session.passage, durationMs)

        val record = PassageCopyRecord(
            id = session.recordId,
            textId = session.passage.id,
            title = session.passage.title,
            author = session.passage.author,
            textContent = session.passage.lines.joinToString("\n"),
            styleId = session.styleId,
            timestampMs = System.currentTimeMillis(),
            durationMs = durationMs,
            strokeCount = updatedMap.values.sumOf { it.size },
            pageCount = (updatedMap.keys.maxOrNull() ?: 0) + 1,
            strokesByPage = updatedMap,
            isCompleted = true,
            actualWpm = pacing.actualWpm,
            targetWpm = session.passage.targetWpm
        )

        copyRepository.saveRecord(record)
        _uiState.update {
            it.copy(
                activeTextCopy = null,
                notificationMessage = "Cópia concluída: ${session.passage.title} (${pacing.actualWpm.toInt()} WPM)"
            )
        }
        return record
    }

    fun openExistingCopyRecord(record: PassageCopyRecord) {
        val passage = PassageCatalog.getById(record.textId) ?: PassageItem(
            id = record.textId,
            title = record.title,
            author = record.author,
            lines = record.textContent.split("\n"),
            category = PassageCategory.CUSTOM,
            targetWpm = record.targetWpm
        )
        selectStyle(record.styleId, adaptPageGuidelines = true)

        val restoredSession = ActiveTextCopySession(
            passage = passage,
            styleId = record.styleId,
            recordId = record.id,
            elapsedSeconds = (record.durationMs / 1000L).coerceAtLeast(0L),
            isPaused = true,
            strokesByPage = record.strokesByPage
        )

        val firstPageStrokes = record.strokesByPage[0] ?: emptyList()
        strokeRepository.loadStrokes(firstPageStrokes)

        _uiState.update {
            it.copy(
                activeTextCopy = restoredSession,
                strokeCount = strokeRepository.count,
                totalPoints = strokeRepository.totalPointsCount,
                canUndo = strokeRepository.canUndo,
                canRedo = strokeRepository.canRedo,
                notificationMessage = "Cópia restaurada: ${record.title}"
            )
        }
    }

    fun cancelTextCopyPractice() {
        textCopyTimerJob?.cancel()
        _uiState.update { it.copy(activeTextCopy = null) }
    }
}
