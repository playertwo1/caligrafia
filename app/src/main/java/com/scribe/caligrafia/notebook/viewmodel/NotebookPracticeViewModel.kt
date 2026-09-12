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
import java.io.File

/**
 * Estado da interface do Caderno de Prática Caligráfica (M1 e M3).
 */
data class NotebookPracticeUiState(
    val currentNotebook: Notebook? = null,
    val currentPage: NotebookPage? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 1,
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
    private val notebookRepository: NotebookRepository = LocalNotebookRepository(baseFilesDir)
    val styleEngine = StyleEngine(File(baseFilesDir, "custom_fonts"))

    val strokeRepository = InMemoryStrokeRepository()
    val pipeline = StrokeCapturePipeline(
        palmPolicy = PalmRejectionPolicy(),
        toolConfig = ToolConfig()
    )
    val renderer = SmoothedReferenceRenderer()

    private val _uiState = MutableStateFlow(
        NotebookPracticeUiState(
            availableStyles = styleEngine.getAvailableStyles(),
            currentStyle = styleEngine.defaultStyle()
        )
    )
    val uiState: StateFlow<NotebookPracticeUiState> = _uiState.asStateFlow()

    private var pagesList: List<NotebookPage> = emptyList()

    init {
        loadOrCreateDefaultNotebook()
    }

    fun loadOrCreateDefaultNotebook() {
        viewModelScope.launch(Dispatchers.IO) {
            val notebooks = notebookRepository.getNotebooks()
            val notebook = if (notebooks.isEmpty()) {
                notebookRepository.createNotebook("Meu Caderno de Caligrafia")
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
                    currentNotebook = notebook,
                    currentPage = firstPage,
                    currentPageIndex = 0,
                    totalPages = pagesList.size.coerceAtLeast(1),
                    canUndo = strokeRepository.canUndo,
                    canRedo = strokeRepository.canRedo,
                    strokeCount = strokeRepository.count,
                    totalPoints = strokeRepository.totalPointsCount
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
            _uiState.update { it.copy(isSaving = true) }
            try {
                notebookRepository.savePageStrokes(page, currentStrokes)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
            // 1. Salva a página atual
            if (currentPage != null) {
                notebookRepository.savePageStrokes(currentPage, currentStrokes)
            }

            // 2. Carrega a página de destino
            val targetPage = pagesList.getOrNull(targetIndex) ?: return@launch
            val targetStrokes = notebookRepository.loadPageStrokes(targetPage)
            strokeRepository.loadStrokes(targetStrokes)

            _uiState.update {
                it.copy(
                    currentPage = targetPage,
                    currentPageIndex = targetIndex,
                    canUndo = strokeRepository.canUndo,
                    canRedo = strokeRepository.canRedo,
                    strokeCount = strokeRepository.count,
                    totalPoints = strokeRepository.totalPointsCount
                )
            }
        }
    }

    fun addNewPage(guidelineConfig: GuidelineConfig = GuidelineConfig.copperplate()) {
        val notebook = _uiState.value.currentNotebook ?: return
        val currentPage = _uiState.value.currentPage
        val currentStrokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
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
                    canUndo = false,
                    canRedo = false,
                    strokeCount = 0,
                    totalPoints = 0,
                    notificationMessage = "Página ${newIndex + 1} criada com sucesso"
                )
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
        val style = styleEngine.getStyle(styleId)
        _uiState.update { it.copy(currentStyle = style) }
        if (adaptPageGuidelines) {
            setGuidelineConfig(style.toGuidelineConfig())
        }
    }

    /**
     * A06: Flush de segurança quando a tela é pausada ou o app entra em segundo plano.
     */
    fun onPauseLifecycle() {
        pipeline.flushActiveStroke(commitIfValid = true)
        val page = _uiState.value.currentPage ?: return
        val currentStrokes = strokeRepository.allStrokes
        viewModelScope.launch(Dispatchers.IO) {
            notebookRepository.savePageStrokes(page, currentStrokes)
        }
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

    fun exportCurrentPage(context: Context) {
        val page = _uiState.value.currentPage ?: return
        val strokes = strokeRepository.allStrokes

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
                exportsDir.mkdirs()
                val targetFile = File(exportsDir, "pagina_${page.pageIndex + 1}_${System.currentTimeMillis()}.png")

                PageExporter.exportToPng(
                    page = page,
                    strokes = strokes,
                    targetFile = targetFile,
                    options = PageExportOptions(
                        widthPx = 1440,
                        heightPx = 2560,
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
