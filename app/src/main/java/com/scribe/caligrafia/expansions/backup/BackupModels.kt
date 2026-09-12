package com.scribe.caligrafia.expansions.backup

/**
 * Manifesto descritivo de um pacote de backup Scribe (.scribepack).
 */
data class BackupManifest(
    val formatVersion: String = "1.0",
    val appVersion: String = "0.8.0",
    val appVersionCode: Int = 10,
    val createdAtMs: Long = System.currentTimeMillis(),
    val deviceInfo: String = "Samsung Galaxy S25 Ultra",
    val notebookCount: Int = 0,
    val pageCount: Int = 0,
    val personalGlyphCount: Int = 0,
    val lessonHistoryCount: Int = 0,
    val practiceAttemptCount: Int = 0,
    val hasTeacherDiagnostic: Boolean = false
)

/**
 * Resumo gerado após uma exportação bem-sucedida de backup.
 */
data class BackupSummary(
    val manifest: BackupManifest,
    val totalBytes: Long,
    val fileCount: Int,
    val exportTimestampMs: Long = System.currentTimeMillis()
)

/**
 * Resultado da importação e restauração de um pacote .scribepack.
 */
data class BackupImportResult(
    val isSuccess: Boolean,
    val manifest: BackupManifest? = null,
    val restoredNotebooks: Int = 0,
    val restoredGlyphs: Int = 0,
    val restoredLessons: Int = 0,
    val restoredAttempts: Int = 0,
    val restoredTeacherData: Boolean = false,
    val errorMessage: String? = null
)
