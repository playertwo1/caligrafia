package com.scribe.caligrafia

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.scribe.caligrafia.preferences.ScribePreferencesStore

/**
 * Inicializa as preferências F6 antes das superfícies de escrita e mantém compatibilidade reativa
 * com pontos legados que ainda escrevem diretamente em SharedPreferences.
 */
class ScribeApplication : Application() {
    private lateinit var preferencesStore: ScribePreferencesStore

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        // load() normaliza enums/faixas e republica ScribePreferencesRuntime/InputModeRuntime.
        preferencesStore.load()
    }

    override fun onCreate() {
        super.onCreate()
        preferencesStore = ScribePreferencesStore(this)
        preferencesStore.load()
        getSharedPreferences(ScribePreferencesStore.PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(preferenceListener)
    }
}
