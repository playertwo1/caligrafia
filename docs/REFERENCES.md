# Referências técnicas

Consultadas durante a especificação inicial (setembro de 2026):

- Android Developers — `MotionEvent`: tool type, pressure, orientation, `AXIS_TILT` e historical motion samples.
- Android Developers — Jetpack Compose e APIs relacionadas a stylus/handwriting.
- Samsung Developer — S Pen Remote SDK, tratado como integração opcional para recursos Samsung específicos, não como fundação da captura de tinta.

## Decisão
O core do ink engine usa APIs padrão Android e inspeciona as capacidades reais do dispositivo em runtime.

## Aparelho-alvo e verificação de SDK (2026-09-12)
- [Samsung — S Pen remote control features](https://www.samsung.com/us/support/answer/ANS10002954/): a S Pen do S25 Ultra não possui Bluetooth nem funções remotas.
- [Samsung — S Pen Remote SDK](https://developer.samsung.com/galaxy-spen-remote/s-pen-remote-sdk.html): SDK voltado a recursos remotos, não adotado como motor de escrita do S25 Ultra.
- [Android — Ink API](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/about-ink-api): primeira opção para escrita de baixa latência a avaliar no M0, conforme direção aprovada pelo usuário.
- Preferência do usuário: utilizar SDKs Samsung compatíveis para recursos aplicáveis; identificar e validar a integração no M0, sem presumir suporte de um SDK de escrita ainda não selecionado.
