package com.scribe.caligrafia.notebook.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scribe.caligrafia.core.model.CalligraphyColor
import com.scribe.caligrafia.core.model.GuidelineConfig
import com.scribe.caligrafia.core.model.Notebook
import com.scribe.caligrafia.core.model.NotebookPage
import com.scribe.caligrafia.core.model.PenThickness
import com.scribe.caligrafia.core.model.ToolConfig
import com.scribe.caligrafia.core.model.ToolMode
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
 * Estado da interface do Caderno de Prática Caligráfica (M1 e M3).
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
    val currentStyle: ScribeStyle = BuiltInStyles.CURSIVA_ESCOLAR
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
                // Salva a página atual antes de trocar de caderno
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
                // 1. Salva a página atual com bloqueio exclusivo garantindo ordem estrita
                if (currentPage != null) {
                    notebookRepository.savePageStrokes(currentPage, currentStrokes)
                }

                // 2. Carrega a página de destino
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
            // A15: Atualiza a lista interna e a lista em StateFlow para que navegação futura use a pauta correta
            pagesList = pagesList.map { if (it.id == updatedPage.id) updatedPage else it }
            _uiState.update { state ->
                state.copy(
                    currentPage = updatedPage
                )
            }
        }
    }

    /**
     * Seleciona o estilo caligráfico ativo (M3 — SCR-023).
     *
     * @param styleId Identificador do estilo desejado.
     * @param adaptPageGuidelines Se true, recalcula e aplica automaticamente as pautas da página para
     *                            refletir a proporção e a inclinação recomendadas do estilo.
     */
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

    /**
     * R17: Importa uma fonte local TTF/OTF, registra como estilo e disponibiliza para o caderno.
     */
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
            if (imported != null) {
                selectStyle(imported.id, adaptPageGuidelines = true)
            }
        } else {
            _uiState.update {
                it.copy(notificationMessage = "Erro ao importar fonte: ${result.exceptionOrNull()?.message}")
            }
        }
        return result
    }

    /**
     * A06: Flush de segurança quando a tela é pausada ou o app entra em segundo plano.
     */
    fun onPauseLifecycle() {
        pipeline.flushActiveStroke(commitIfValid = true)
        val page = _uiState.value.currentPage ?: return
        val currentStrokes = strokeRepository.allStrokes
        viewModelScope.launch(Dispatchers.IO) {
            pagePersistenceMutex.withLock {
                notebookRepository.savePageStrokes(page, currentStrokes)
            }
        }
    }

    /**
     * Recarrega preferências atualizadas de hardware e curva de pressão da S Pen.
     */
    fun onResumeLifecycle() {
        val savedCurve = try {
            val prefs = getApplication<Application>().getSharedPreferences("scribe_settings", android.content.Context.MODE_PRIVATE)
            prefs.getString("pressure_curve", null)?.let { com.scribe.caligrafia.expansions.styles.PressureCurveType.valueOf(it) }
        } catch (_: Throwable) { null } ?: com.scribe.caligrafia.expansions.styles.PressureCurveType.LINEAR
        renderer.pressureCurve = savedCurve
    }

    fun undo() {
        if (strokeRepository.undo()) {
            onStrokesModified()
        }
    }

    fun redo() {
        if (strokeRepository.redo()) {
            onStrokesModified()
        }
    }

    fun clearPage() {
        strokeRepository.clear()
        onStrokesModified()
    }

    // R16: Dimensões físicas reais do canvas de escrita da tela para exportação fiel sem distorção
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

    fun exportCurrentPage(context: Context) {
        val page = _uiState.value.currentPage ?: return
        val strokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
                exportsDir.mkdirs()
                val targetFile = File(exportsDir, "pagina_${page.pageIndex + 1}_${System.currentTimeMillis()}.png")

                // R16: Preserva aspect ratio do canvas da tela se disponível, evitando distorções
                val targetW = 1440
                val targetH = if (canvasWidthPx > 0f && canvasHeightPx > 0f) {
                    (targetW * (canvasHeightPx / canvasWidthPx)).toInt().coerceIn(720, 4000)
                } else {
                    2560
                }

                PageExporter.exportToPng(
                    page = page,
                    strokes = strokes,
                    targetFile = targetFile,
                    options = PageExportOptions(
                        widthPx = targetW,
                        heightPx = targetH,
                        sourceWidthPx = if (canvasWidthPx > 0f) canvasWidthPx else null,
                        sourceHeightPx = if (canvasHeightPx > 0f) canvasHeightPx else null,
                        includeGuidelines = true
                    )
                )

                _uiState.update {
                    it.copy(notificationMessage = "Página exportada para: ${targetFile.name}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(notificationMessage = "Erro ao exportar: ${e.localizedMessage}")
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
