package com.scribe.caligrafia.ink.lifecycle

import com.scribe.caligrafia.core.model.Stroke
import com.scribe.caligrafia.ink.persistence.strategies.DedicatedFileStrategy
import java.io.File

/**
 * Gerenciador de ciclo de vida e contingência de sessão de escrita.
 *
 * Utiliza a estratégia de arquivo binário dedicado (`DedicatedFileStrategy` / .scribe)
 * com compressão Deflate e Magic Bytes SCRIBE01 para garantir persistência atômica
 * de alta performance durante pausas de ciclo de vida (onPause / background / suspensão).
 */
class SessionLifecycleManager(
    storageDir: File,
    private val sessionKey: String = "session_autosave"
) {

    private val strategy = DedicatedFileStrategy(storageDir)

    /**
     * Salva atomicamente a lista de traços no arquivo de contingência.
     * @return quantidade de bytes gravados em disco.
     */
    fun autoSave(strokes: List<Stroke>): Long {
        if (strokes.isEmpty()) {
            clearAutoSave()
            return 0L
        }
        return strategy.save(sessionKey, strokes)
    }

    /**
     * Restaura a última sessão salva do disco.
     * @return lista de traços recuperados com integridade de 100%.
     */
    fun restore(): List<Stroke> {
        return strategy.load(sessionKey)
    }

    /**
     * Verifica se existe um arquivo de contingência de auto-save válido.
     */
    fun hasAutoSave(): Boolean {
        val targetFile = strategy.getFile(sessionKey)
        return targetFile.exists() && targetFile.length() > 0
    }

    /**
     * Remove o arquivo de auto-save (utilizado quando o usuário limpa a tela voluntariamente).
     */
    fun clearAutoSave(): Boolean {
        val targetFile = strategy.getFile(sessionKey)
        return if (targetFile.exists()) {
            targetFile.delete()
        } else {
            false
        }
    }

    /**
     * Retorna o tamanho em bytes do arquivo de auto-save.
     */
    fun getAutoSaveSize(): Long {
        val targetFile = strategy.getFile(sessionKey)
        return if (targetFile.exists()) targetFile.length() else 0L
    }
}
