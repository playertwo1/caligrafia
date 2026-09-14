package com.scribe.caligrafia

import android.app.Application
import com.scribe.caligrafia.preferences.ScribePreferencesStore

class ScribeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Carrega a política de entrada antes de qualquer superfície de escrita ser criada.
        ScribePreferencesStore(this).load()
    }
}
