package com.scribe.caligrafia.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Espelho reativo em memória das preferências persistidas.
 *
 * O armazenamento continua sendo [ScribePreferencesStore]; este objeto só permite que Compose e
 * renderizadores reajam imediatamente sem reler SharedPreferences a cada frame.
 */
object ScribePreferencesRuntime {
    private val _state = MutableStateFlow(ScribePreferences())
    val state: StateFlow<ScribePreferences> = _state.asStateFlow()

    @Volatile
    var current: ScribePreferences = ScribePreferences()
        private set

    fun publish(value: ScribePreferences) {
        current = value
        _state.value = value
    }
}
