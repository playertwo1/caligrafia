package com.scribe.caligrafia.ink.persistence

import com.scribe.caligrafia.core.model.Stroke

/**
 * Interface polimórfica para as estratégias avaliadas no Spike de Persistência (SCR-006).
 */
interface StrokePersistenceStrategy {
    val formatName: String
    val description: String

    /**
     * Persiste a lista de traços de uma determinada sessão.
     * @return O número total de bytes gravados em disco.
     */
    fun save(sessionId: String, strokes: List<Stroke>): Long

    /**
     * Carrega e reconstrói a lista completa de traços a partir do armazenamento.
     */
    fun load(sessionId: String): List<Stroke>

    /**
     * Retorna o tamanho ocupado em disco por essa sessão.
     */
    fun getStorageSizeBytes(sessionId: String): Long

    /**
     * Remove os dados persistidos da sessão.
     */
    fun delete(sessionId: String): Boolean
}
