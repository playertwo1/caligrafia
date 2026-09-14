# F5 ↔ F6 — Gap de compatibilidade das preferências

## Achado

A F6 introduziu novas preferências persistidas em `scribe_settings`: `toolbar_side_override`, `input_mode`, `text_scale`, `guide_contrast`, `reduce_animations`, `break_reminder_enabled`, `break_interval_minutes` e `vibration_enabled`, além das chaves históricas.

A implementação atual da F5 ainda cria/valida `preferences_snapshot.txt` apenas com:

- `pressure_curve`
- `is_left_handed`
- `is_high_contrast`
- `show_guide_numbers`
- `daily_goal_minutes`

Isso aparece em `F5DocumentTransferActivity`, `ExpansionsViewModel` e na whitelist `ScribeBackupManager.validatePreferencesSnapshot`.

## Consequência

Um `.scribepack` criado após F6 pode preservar as preferências antigas, mas **não há evidência de que todas as novas preferências F6 sobrevivam ao round-trip**. Portanto F5.12/F5.G e F6.17 não devem ser fechados enquanto esse contrato não for migrado.

## Correção exigida no run local

Centralizar snapshot/restore no `ScribePreferencesStore` ou em um codec único de preferências. O pacote deve incluir todas as chaves F6 reconhecidas, com validação estrita de nome/tipo/enum/faixa e compatibilidade com pacotes antigos que só tenham as cinco chaves históricas.

Depois executar round-trip com valores não-default para **todas** as opções e provar, após restart, que o estado efetivo foi restaurado. Pacote antigo deve restaurar chaves históricas e usar defaults seguros nas novas, sem rejeição indevida.

**Estado:** `BLOQUEIA_F5.G_E_F6.17` até correção + teste executado.
