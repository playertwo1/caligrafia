package com.scribe.caligrafia.core.model

/**
 * Modos de entrada suportados pelo motor de escrita do Scribe.
 */
enum class InputMode(val displayName: String, val description: String) {
    /**
     * Modo estrito de caligrafia: apenas S Pen / Stylus ou Borracha geram traços.
     * Qualquer toque de dedo ou contato de palma é sumariamente ignorado e registrado.
     */
    STYLUS_ONLY(
        displayName = "Stylus Only (Caligrafia)",
        description = "Toques de dedo são ignorados. Apenas a caneta escreve."
    ),

    /**
     * Modo permissivo: toques de dedo podem produzir traços, porém a proximidade (hover)
     * ou toque ativo do stylus tem prioridade absoluta, bloqueando dedos como palma.
     */
    STYLUS_AND_FINGER(
        displayName = "Stylus + Dedo",
        description = "Permite dedo, mas prioriza a caneta e suprime toques sob proximidade."
    )
}
