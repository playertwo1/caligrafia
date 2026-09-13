package com.scribe.caligrafia.expansions.passage

/**
 * Repositório de sessões de cópia de texto com strokes vetoriais e métricas (F4.14, F4.15).
 */
interface PassageCopyRepository {
    suspend fun saveRecord(record: PassageCopyRecord)
    suspend fun getAllRecords(): List<PassageCopyRecord>
    suspend fun getRecordById(id: String): PassageCopyRecord?
    suspend fun deleteRecord(id: String)

    suspend fun listAllRecords(): List<PassageCopyRecord> = getAllRecords()
    suspend fun loadRecord(id: String): PassageCopyRecord? = getRecordById(id)
}
