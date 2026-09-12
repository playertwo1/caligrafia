package com.scribe.caligrafia.ink.renderer

/**
 * Gerenciador de motores de renderização para o Stylus Lab.
 * Permite alternar e comparar em tempo de execução os renderizadores no Samsung Galaxy S25 Ultra.
 */
class RendererManager(
    val smoothedRenderer: SmoothedReferenceRenderer = SmoothedReferenceRenderer(),
    val rawRenderer: RawPolylineRenderer = RawPolylineRenderer(),
    val inkApiAdapter: AndroidInkRendererAdapter = AndroidInkRendererAdapter(smoothedRenderer)
) {

    var activeRenderer: InkRenderer = smoothedRenderer
        private set

    val availableRenderers: List<InkRenderer> = listOf(
        smoothedRenderer,
        inkApiAdapter,
        rawRenderer
    )

    fun selectRenderer(type: RendererType): InkRenderer {
        activeRenderer = when (type) {
            RendererType.SMOOTHED_REFERENCE -> smoothedRenderer
            RendererType.ANDROID_INK_API -> inkApiAdapter
            RendererType.RAW_POLYLINE -> rawRenderer
        }
        return activeRenderer
    }
}
