# Referências técnicas

Consultadas durante a especificação inicial (setembro de 2026):

- Android Developers — `MotionEvent`: tool type, pressure, orientation, `AXIS_TILT` e historical motion samples.
- Android Developers — Jetpack Compose e APIs relacionadas a stylus/handwriting.
- Samsung Developer — S Pen Remote SDK, tratado como integração opcional para recursos Samsung específicos, não como fundação da captura de tinta.

## Decisão
O core do ink engine usa APIs padrão Android e inspeciona as capacidades reais do dispositivo em runtime.
