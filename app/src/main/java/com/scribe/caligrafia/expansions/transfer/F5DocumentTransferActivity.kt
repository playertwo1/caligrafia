package com.scribe.caligrafia.expansions.transfer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.scribe.caligrafia.MainActivity
import com.scribe.caligrafia.expansions.backup.BackupInspectionResult
import com.scribe.caligrafia.expansions.backup.ScribeBackupManager
import com.scribe.caligrafia.expansions.styles.PressureCurveType
import com.scribe.caligrafia.preferences.ScribePreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Hub F5 isolado para operações SAF (Storage Access Framework).
 *
 * Motivo arquitetural: mantém ActivityResultContracts fora das telas grandes e permite que os
 * ViewModels atuais iniciem exportações sem gravar silenciosamente em diretórios internos.
 * Nenhuma operação é anunciada como concluída antes do ContentResolver terminar a escrita.
 */
class F5DocumentTransferActivity : ComponentActivity() {

    private var pendingExportFile: File? = null
    private var pendingExportMime: String = "application/octet-stream"
    private var pendingSuggestedName: String = "scribe_export"

    private val createSvg = registerForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        handleExportDestination(uri)
    }

    private val createPng = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        handleExportDestination(uri)
    }

    private val createZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        handleExportDestination(uri)
    }

    private val createAny = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        handleExportDestination(uri)
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            toast("Restauração cancelada. Nenhum dado foi alterado.")
            finish()
        } else {
            inspectSelectedBackup(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_EXPORT_FILE -> configureSingleExportAndLaunch()
            MODE_EXPORT_CHOICE -> showExportChoice()
            MODE_BACKUP_HUB -> showBackupHub()
            else -> {
                toast("Operação de arquivo não reconhecida.")
                finish()
            }
        }
    }

    private fun configureSingleExportAndLaunch() {
        val path = intent.getStringExtra(EXTRA_FILE_PATH)
        val file = path?.let(::File)
        if (file == null || !file.isFile) {
            toast("Arquivo temporário de exportação indisponível.")
            finish()
            return
        }
        pendingExportFile = file
        pendingExportMime = intent.getStringExtra(EXTRA_MIME) ?: "application/octet-stream"
        pendingSuggestedName = intent.getStringExtra(EXTRA_SUGGESTED_NAME) ?: file.name
        launchCreateDocument()
    }

    private fun showExportChoice() {
        val first = intent.getStringExtra(EXTRA_FILE_PATH)?.let(::File)
        val second = intent.getStringExtra(EXTRA_FILE_PATH_2)?.let(::File)
        val available = buildList {
            if (first?.isFile == true) {
                add(
                    ExportChoice(
                        label = intent.getStringExtra(EXTRA_LABEL) ?: "Opção 1",
                        file = first,
                        preview = intent.getStringExtra(EXTRA_PREVIEW_PATH)?.let(::File)?.takeIf { it.isFile }
                    )
                )
            }
            if (second?.isFile == true) {
                add(
                    ExportChoice(
                        label = intent.getStringExtra(EXTRA_LABEL_2) ?: "Opção 2",
                        file = second,
                        preview = intent.getStringExtra(EXTRA_PREVIEW_PATH_2)?.let(::File)?.takeIf { it.isFile }
                    )
                )
            }
        }

        if (available.isEmpty()) {
            toast("Nenhuma fonte disponível para exportação.")
            finish()
            return
        }

        if (available.size == 1) {
            pendingExportFile = available.first().file
            pendingExportMime = intent.getStringExtra(EXTRA_MIME) ?: "application/octet-stream"
            pendingSuggestedName = intent.getStringExtra(EXTRA_SUGGESTED_NAME) ?: available.first().file.name
            launchCreateDocument()
            return
        }

        var selectedIndex = 0
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), 0)
        }
        val explanation = TextView(this).apply {
            text = intent.getStringExtra(EXTRA_CHOICE_DESCRIPTION)
                ?: "Selecione explicitamente qual conteúdo será exportado."
            setPadding(0, 0, 0, dp(12))
        }
        val preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(180)
            )
        }
        container.addView(explanation)
        container.addView(preview)
        updatePreview(preview, available[0].preview)

        val dialog = AlertDialog.Builder(this)
            .setTitle(intent.getStringExtra(EXTRA_DIALOG_TITLE) ?: "Escolher conteúdo")
            .setSingleChoiceItems(available.map { it.label }.toTypedArray(), 0) { _, which ->
                selectedIndex = which
                updatePreview(preview, available[which].preview)
            }
            .setView(container)
            .setNegativeButton("Cancelar") { _, _ ->
                cleanupChoiceFiles()
                toast("Exportação cancelada.")
                finish()
            }
            .setPositiveButton("Escolher destino", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                pendingExportFile = available[selectedIndex].file
                pendingExportMime = intent.getStringExtra(EXTRA_MIME) ?: "application/octet-stream"
                val baseName = intent.getStringExtra(EXTRA_SUGGESTED_NAME) ?: available[selectedIndex].file.name
                pendingSuggestedName = if (available[selectedIndex].label.contains("referência", ignoreCase = true)) {
                    appendBeforeExtension(baseName, "_referencia")
                } else baseName
                dialog.dismiss()
                launchCreateDocument()
            }
        }
        dialog.setOnCancelListener {
            cleanupChoiceFiles()
            finish()
        }
        dialog.show()
    }

    private fun showBackupHub() {
        AlertDialog.Builder(this)
            .setTitle("Backup & recuperação")
            .setMessage("Exporte o conjunto completo para um arquivo .scribepack ou escolha um pacote externo para validar antes da restauração.")
            .setItems(arrayOf("Exportar backup .scribepack", "Restaurar backup externo")) { _, which ->
                when (which) {
                    0 -> prepareBackupExport()
                    1 -> openBackup.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "application/x-zip-compressed"
                        )
                    )
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun prepareBackupExport() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                snapshotPreferences()
                val tempDir = File(cacheDir, "f5_exports").apply { mkdirs() }
                val temp = File(tempDir, "scribe_backup_${System.currentTimeMillis()}.scribepack")
                FileOutputStream(temp).use { output ->
                    ScribeBackupManager(filesDir).exportBackup(output)
                    output.fd.sync()
                }
                withContext(Dispatchers.Main) {
                    pendingExportFile = temp
                    pendingExportMime = "application/zip"
                    pendingSuggestedName = temp.name
                    createZip.launch(temp.name)
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    toast("Falha ao preparar backup: ${e.message}")
                    finish()
                }
            }
        }
    }

    private fun inspectSelectedBackup(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val displayName = resolveDisplayName(uri)
            val inspection = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    ScribeBackupManager(filesDir).inspectBackup(input)
                } ?: BackupInspectionResult(isValid = false, errorMessage = "Arquivo não pôde ser aberto")
            } catch (e: Throwable) {
                BackupInspectionResult(isValid = false, errorMessage = e.message)
            }

            withContext(Dispatchers.Main) {
                if (!inspection.isValid || inspection.manifest == null) {
                    AlertDialog.Builder(this@F5DocumentTransferActivity)
                        .setTitle("Pacote rejeitado")
                        .setMessage(inspection.errorMessage ?: "O pacote não passou pela validação estrutural.")
                        .setPositiveButton("Fechar") { _, _ -> finish() }
                        .show()
                } else {
                    showRestoreConfirmation(uri, displayName, inspection)
                }
            }
        }
    }

    private fun showRestoreConfirmation(uri: Uri, displayName: String, inspection: BackupInspectionResult) {
        val m = inspection.manifest ?: return
        val message = buildString {
            append("Arquivo: ").append(displayName).append('\n')
            append("Cadernos: ").append(m.notebookCount).append(" • Páginas: ").append(m.pageCount).append('\n')
            append("Glifos pessoais: ").append(m.personalGlyphCount).append('\n')
            append("Sessões: ").append(m.lessonHistoryCount)
                .append(" • SRS: ").append(m.spacedRepetitionCount)
                .append(" • Tentativas: ").append(m.practiceAttemptCount).append('\n')
            append("Cópias: ").append(m.passageCopyCount)
                .append(" • Estilos: ").append(m.personalStyleCount)
                .append(" • Fontes: ").append(m.importedFontCount).append('\n')
            append("Referência de assinatura: ").append(if (m.signatureReferenceCount > 0) "sim" else "não").append('\n')
            append("Professor: ").append(if (m.hasTeacherDiagnostic) "incluído" else "sem diagnóstico").append('\n')
            append("\nATENÇÃO: confirmar restauração SUBSTITUI o conjunto local gerenciado. Itens locais ausentes no pacote serão removidos. Um rollback persistente é criado antes do commit.")
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar substituição dos dados?")
            .setMessage(message)
            .setNegativeButton("Cancelar") { _, _ ->
                toast("Restauração cancelada. Nenhum dado foi alterado.")
                finish()
            }
            .setPositiveButton("Substituir e restaurar") { _, _ -> performRestore(uri) }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun performRestore(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = try {
                contentResolver.openInputStream(uri)?.use { input ->
                    ScribeBackupManager(filesDir).importBackup(input)
                }
            } catch (e: Throwable) {
                null
            }

            withContext(Dispatchers.Main) {
                if (result?.isSuccess == true) {
                    restorePreferences()
                    AlertDialog.Builder(this@F5DocumentTransferActivity)
                        .setTitle("Restauração concluída")
                        .setMessage("O pacote foi restaurado e verificado. O app será reiniciado para invalidar caches e reabrir os repositórios sobre o novo conjunto.")
                        .setCancelable(false)
                        .setPositiveButton("Reabrir app") { _, _ -> restartMainActivity() }
                        .show()
                } else {
                    val message = result?.errorMessage
                        ?: "Falha ao restaurar o pacote. O app não confirmou alteração dos dados."
                    AlertDialog.Builder(this@F5DocumentTransferActivity)
                        .setTitle("Restauração não concluída")
                        .setMessage(message)
                        .setPositiveButton("Fechar") { _, _ -> finish() }
                        .show()
                }
            }
        }
    }

    private fun launchCreateDocument() {
        when (pendingExportMime) {
            "image/svg+xml" -> createSvg.launch(ensureExtension(pendingSuggestedName, ".svg"))
            "image/png" -> createPng.launch(ensureExtension(pendingSuggestedName, ".png"))
            "application/zip" -> createZip.launch(ensureExtension(pendingSuggestedName, ".scribepack"))
            else -> createAny.launch(pendingSuggestedName)
        }
    }

    private fun handleExportDestination(uri: Uri?) {
        val source = pendingExportFile
        if (uri == null) {
            cleanupPendingExport()
            toast("Exportação cancelada. Nenhum sucesso foi registrado.")
            finish()
            return
        }
        if (source == null || !source.isFile) {
            toast("Arquivo temporário não está disponível.")
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val success = try {
                contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                    output.flush()
                } ?: error("Destino não pôde ser aberto")
                true
            } catch (_: Throwable) {
                false
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    showOpenExportedDialog(uri, pendingExportMime)
                } else {
                    cleanupPendingExport()
                    AlertDialog.Builder(this@F5DocumentTransferActivity)
                        .setTitle("Falha na exportação")
                        .setMessage("O destino escolhido não pôde ser gravado. Nenhum sucesso foi registrado.")
                        .setPositiveButton("Fechar") { _, _ -> finish() }
                        .show()
                }
            }
        }
    }

    private fun showOpenExportedDialog(uri: Uri, mime: String) {
        cleanupPendingExport()
        AlertDialog.Builder(this)
            .setTitle("Arquivo exportado")
            .setMessage("A gravação no destino escolhido foi concluída. Deseja abrir o arquivo agora em outro aplicativo?")
            .setNegativeButton("Fechar") { _, _ -> finish() }
            .setPositiveButton("Abrir arquivo") { _, _ ->
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (openIntent.resolveActivity(packageManager) != null) {
                    startActivity(openIntent)
                } else {
                    toast("Nenhum aplicativo disponível para abrir este formato.")
                }
                finish()
            }
            .show()
    }

    private fun restartMainActivity() {
        val restart = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restart)
        finish()
    }

    private fun snapshotPreferences() {
        check(ScribePreferencesStore(this).writeBackupSnapshot(File(filesDir, ScribePreferencesStore.BACKUP_FILE_NAME)))
    }

    private fun restorePreferences() {
        ScribePreferencesStore(this).restoreBackupSnapshot(File(filesDir, ScribePreferencesStore.BACKUP_FILE_NAME))
    }

    private fun resolveDisplayName(uri: Uri): String {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: "backup selecionado"
        } catch (_: Throwable) {
            uri.lastPathSegment ?: "backup selecionado"
        }
    }

    private fun updatePreview(imageView: ImageView, previewFile: File?) {
        if (previewFile?.isFile == true) {
            val bitmap = BitmapFactory.decodeFile(previewFile.absolutePath)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.visibility = ImageView.VISIBLE
                return
            }
        }
        imageView.setImageDrawable(null)
        imageView.visibility = ImageView.GONE
    }

    private fun cleanupPendingExport() {
        pendingExportFile?.takeIf { isManagedTemp(it) }?.delete()
        cleanupChoiceFiles()
        pendingExportFile = null
    }

    private fun cleanupChoiceFiles() {
        listOf(
            EXTRA_FILE_PATH,
            EXTRA_FILE_PATH_2,
            EXTRA_PREVIEW_PATH,
            EXTRA_PREVIEW_PATH_2
        ).mapNotNull { intent.getStringExtra(it) }
            .map(::File)
            .filter(::isManagedTemp)
            .forEach { it.delete() }
    }

    private fun isManagedTemp(file: File): Boolean {
        return try {
            file.canonicalPath.startsWith(File(cacheDir, "f5_exports").canonicalPath + File.separator)
        } catch (_: Throwable) {
            false
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun ensureExtension(name: String, extension: String): String {
        return if (name.lowercase().endsWith(extension.lowercase())) name else name + extension
    }

    private fun appendBeforeExtension(name: String, suffix: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) + suffix + name.substring(dot) else name + suffix
    }

    private data class ExportChoice(
        val label: String,
        val file: File,
        val preview: File?
    )

    companion object {
        const val EXTRA_MODE = "f5_mode"
        const val EXTRA_FILE_PATH = "f5_file_path"
        const val EXTRA_FILE_PATH_2 = "f5_file_path_2"
        const val EXTRA_PREVIEW_PATH = "f5_preview_path"
        const val EXTRA_PREVIEW_PATH_2 = "f5_preview_path_2"
        const val EXTRA_LABEL = "f5_label"
        const val EXTRA_LABEL_2 = "f5_label_2"
        const val EXTRA_MIME = "f5_mime"
        const val EXTRA_SUGGESTED_NAME = "f5_suggested_name"
        const val EXTRA_DIALOG_TITLE = "f5_dialog_title"
        const val EXTRA_CHOICE_DESCRIPTION = "f5_choice_description"

        const val MODE_EXPORT_FILE = "export_file"
        const val MODE_EXPORT_CHOICE = "export_choice"
        const val MODE_BACKUP_HUB = "backup_hub"

        fun exportFileIntent(
            context: Context,
            file: File,
            mime: String,
            suggestedName: String
        ): Intent = Intent(context, F5DocumentTransferActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_EXPORT_FILE)
            putExtra(EXTRA_FILE_PATH, file.absolutePath)
            putExtra(EXTRA_MIME, mime)
            putExtra(EXTRA_SUGGESTED_NAME, suggestedName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        fun backupHubIntent(context: Context): Intent =
            Intent(context, F5DocumentTransferActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_BACKUP_HUB)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
