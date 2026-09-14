package com.scribe.caligrafia.core.model

/**
 * Estado de processo da política de entrada escolhida pelo usuário.
 *
 * É inicializado pelo Application a partir da preferência persistida e atualizado pelo store.
 * Pipelines padrão podem segui-lo sem depender de Context/SharedPreferences dentro do motor de ink.
 */
object InputModeRuntime {
    @Volatile
    var current: InputMode = InputMode.STYLUS_ONLY
}
