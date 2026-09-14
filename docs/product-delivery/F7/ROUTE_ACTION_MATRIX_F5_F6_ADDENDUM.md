# F7 — Adendo da ROUTE_ACTION_MATRIX para F5/F6

> Este arquivo não substitui `docs/product-delivery/ROUTE_ACTION_MATRIX.md`. Ele registra as linhas faltantes encontradas na revisão F7.01 para posterior incorporação à matriz canônica após validação local.

| ID proposto | Origem | Controle / ação | Destino / efeito | Argumentos / estado | Handler principal | Persistência / sistema | Prova ainda exigida | Estado |
|---|---|---|---|---|---|---|---|---|
| F5-RT-01 | Assinaturas | Salvar referência | Referência ativa versionada | raw + metadados | `ExpansionsViewModel` / signature baseline store | `signatures/` | falha entre raw/metadados mantém referência antiga | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-02 | Assinaturas | Exportar SVG | Picker Android | tentativa atual ou referência | `ExpansionsViewModel` → `F5DocumentTransferActivity` | SAF `CreateDocument(image/svg+xml)` | abrir fora do app; bounds negativos | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-03 | Assinaturas | Exportar PNG | Picker Android | tentativa atual ou referência | `ExpansionsViewModel` → `F5DocumentTransferActivity` | SAF `CreateDocument(image/png)` | transparência/dimensões; cancelamento | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-04 | Caderno | Exportar página | Picker Android | marfim+pautas ou branco+sem linhas | `NotebookPracticeViewModel` / transfer hub | SAF `CreateDocument(image/png)` | proporção e abertura externa | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-05 | Backup | Exportar `.scribepack` | Destino Android | conjunto gerenciado | `F5DocumentTransferActivity.prepareBackupExport` / `ScribeBackupManager.exportBackup` | SAF + ZIP | central directory, reabertura e inventário | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-06 | Backup | Selecionar pacote | Inspeção sem mutação | URI externa | `openBackup` → `inspectSelectedBackup` | SAF `OpenDocument` | pacote inválido/cancelamento | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-07 | Backup | Revisar pacote | Diálogo de contagens | `BackupInspectionResult` | `showRestoreConfirmation` | somente leitura | contagens reais e arquivo identificado | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-08 | Backup | Confirmar restauração | Replace + rollback | URI validada | `performRestore` / `ScribeBackupManager.importBackup` | staging + `.restore_rollback` + marker | interrupção/falha/rollback verificado | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F5-RT-09 | Backup | Restore concluído | Reiniciar app | resultado de import | `restartMainActivity` | `FLAG_ACTIVITY_CLEAR_TASK` | caches e dados restaurados após restart | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F6-RT-01 | Preferências | Mão direita/esquerda | lado recomendado | bool | `ScribePreferencesStore` | SharedPreferences | UI + editor físico | NUCLEO_IMPLEMENTADO_UI_PENDENTE |
| F6-RT-02 | Preferências | Lado da barra | dock esquerda/direita | `ToolbarSide?` | `ScribePreferencesStore` | SharedPreferences | aplicar no editor e validar palma | PENDENTE_UI_EDITOR |
| F6-RT-03 | Preferências | Só caneta / caneta+toque | política global do pipeline | `InputMode` | `InputModeRuntime` + `PalmRejectionPolicy` | SharedPreferences | dedo/palma/S Pen no S25 Ultra | SOFTWARE_IMPLEMENTADO |
| F6-RT-04 | Preferências | Escala de texto | `LocalDensity` global | `TextScaleOption` | `MainActivity` + runtime prefs | SharedPreferences | controle/preview e fonte ampliada | EFEITO_IMPLEMENTADO_UI_PENDENTE |
| F6-RT-05 | Preferências | Contraste de guias | alpha derivado | `GuideContrastOption` | `GuidelineRenderer` | SharedPreferences | preview e canvas físico | EFEITO_IMPLEMENTADO_UI_PENDENTE |
| F6-RT-06 | Preferências | Reduzir animações | transições reduzidas | bool | ainda não ligado | SharedPreferences | transições + Replay permanece | PENDENTE_IMPLEMENTACAO |
| F6-RT-07 | S Pen | Curva Linear/Suave/Firme | render derivado | `PressureCurveType` | renderer + `NotebookCanvasView` | SharedPreferences | UI só 3 opções; raw idêntico | NUCLEO_IMPLEMENTADO_UI_PENDENTE |
| F6-RT-08 | Pausas | Lembrete/intervalo | alerta após escrita ativa | enabled + minutos | `ActiveBreakReminder` / Watch bridge | SharedPreferences | integrar sessão real; pausa/fundo | NUCLEO_IMPLEMENTADO_INTEGRACAO_PENDENTE |
| F6-RT-09 | Preferências | Testar vibração | haptic local/fallback | enabled/capability | `WatchCompanionAdapter` | Vibrator API | botão/UI + hardware | NUCLEO_IMPLEMENTADO_UI_PENDENTE |
| F6-RT-10 | S Pen & Watch | Atualizar conexão | conectado/desconectado | permissão + GATT | `WatchCompanionAdapter.isWatchConnected` | Bluetooth | runtime permission + Watch real | PARCIAL_DISPOSITIVO |
| F6-RT-11 | Sessão | Evento fase/pausa | adapter Watch/fallback | fase/tempo | `IWatchCompanionBridge` | broadcast/adapter | recepção Wear e desconexão | PARCIAL_DISPOSITIVO |
| F6-RT-12 | Laboratório | Salvar/reabrir/replay/limpar | sessão isolada | raw strokes | `StylusLabViewModel` | `autosave_storage/`, `spike_storage/` | restart + comparação raw | IMPLEMENTADO_NAO_VALIDADO_LOCAL |
| F6-RT-13 | Laboratório | Capability | disponível/ausente/a verificar | MotionRange + sample | `DeviceCapabilityInspector` | InputDevice/MotionEvent | zeros suportados no S25 Ultra | SOFTWARE_IMPLEMENTADO_UI_COPY_PENDENTE |

## Achado transversal

O contrato F5 de `preferences_snapshot.txt` ainda cobre apenas as preferências históricas. Ver `../F6/F5_PREFERENCE_BACKUP_GAP.md`. Essa lacuna bloqueia afirmar que as novas opções F6 sobrevivem ao round-trip do backup.

## Gate

Para F7.01: incorporar estas linhas (com IDs definitivos) à matriz canônica, ajustar handlers após quaisquer correções locais e só marcar `PRONTO` quando houver teste/evidência real correspondente.
