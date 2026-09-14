# F6 — Matriz opção → efeito → persistência → teste

**Branch de implementação:** `phase-f6-f7-finalization`

> Esta matriz separa implementação de software de validação executada. GitHub Actions está sem cota mensal e o runtime desta sessão não consegue clonar o repositório para executar Gradle. Portanto, testes novos marcados como `ADICIONADO_NAO_EXECUTADO` não são tratados como evidência verde.

| ID | Opção / capacidade | Efeito esperado | Persistência / fonte | Evidência de código | Estado atual |
|---|---|---|---|---|---|
| F6.01 | Preferências | Grupos Escrita, Leitura e Pausas; Voltar/Concluir coerentes | `ScribePreferencesStore` | `ExpansionsScreen` já possui destino Preferências; store unificado criado | **PARCIAL — UI ainda não expõe todos os campos** |
| F6.02 | Mão direita/esquerda | Recomenda barra no lado oposto à mão dominante, sem impedir override | `is_left_handed` + `toolbar_side_override` | `ScribePreferences.effectiveToolbarSide` | **SOFTWARE_IMPLEMENTADO; efeito visual da barra pendente** |
| F6.03 | Barra esquerda/direita | Mover dock da ferramenta sem cobrir área da palma | `toolbar_side_override` | Contrato persistido | **PENDENTE_UI_EDITOR** |
| F6.04 | Só caneta / caneta + toque | Padrão só caneta; modo toque é explícito; palma continua rejeitada sob stylus | `input_mode`; `InputModeRuntime`; `PalmRejectionPolicy` | pipeline padrão segue preferência; override local preservado | **SOFTWARE_IMPLEMENTADO; validar editor físico** |
| F6.05 | Escala de texto | Multiplicar fontScale do sistema; controles essenciais permanecem acessíveis | `text_scale` | contrato persistido | **PENDENTE_APLICACAO_UI + teste visual** |
| F6.06 | Contraste das guias | Alterar somente alpha do Paint das pautas | `guide_contrast` + compatibilidade `is_high_contrast` | `GuidelineRenderer` usa `ScribePreferencesRuntime.current.guideContrast` | **SOFTWARE_IMPLEMENTADO; preview UI pendente** |
| F6.07 | Reduzir animações | Reduzir transições automáticas; replay voluntário continua disponível | `reduce_animations` | contrato persistido | **PENDENTE_APLICACAO_UI** |
| F6.08 | Curva Linear/Suave/Firme | Alterar apenas largura/render derivado | `pressure_curve` | `SmoothedReferenceRenderer`, `NotebookCanvasView`, `StylusLabViewModel` | **SOFTWARE_IMPLEMENTADO; seletor antigo ainda oferece SIGMOID e precisa ser limitado na UI** |
| F6.09 | Raw de pressão | Raw antes/depois deve ser idêntico nas três curvas | raw em `StrokePoint`; transformação pura | `PhaseF6AcceptanceTest.f6_09_pressureCurves_neverMutateRawPressureSamples` | **ADICIONADO_NAO_EXECUTADO** |
| F6.10 | Lembrete de pausa | Contar somente escrita ativa em foreground; pausa/fundo não acumulam | `break_reminder_enabled`, `break_interval_minutes` | `ActiveBreakReminder`; Watch adapter usa tempo monotônico | **NÚCLEO_IMPLEMENTADO; integração sessão/UI pendente** |
| F6.11 | Vibração | Habilitar/desabilitar; retornar false se aparelho/permissão indisponível | `vibration_enabled` | `WatchCompanionAdapter.triggerDeviceVibration` | **SOFTWARE_IMPLEMENTADO; botão/UI e teste físico pendentes** |
| F6.12 | Watch conectado | Nunca alegar conexão sem sinal real; fallback permanece no telefone | probe de GATT + `BLUETOOTH_CONNECT` | `WatchCompanionAdapter.isWatchConnected()` | **PARCIAL — probe real implementado; permissão runtime e validação Watch pendentes** |
| F6.13 | Eventos fase/pausa | Adapter não pode travar sessão sem Watch | bridge desacoplada | `sendTimerSync`, haptics e fallback | **PARCIAL — recepção Wear/Data Layer não comprovada** |
| F6.14 | Laboratório | Captura, limpar, salvar/reabrir, replay | `autosave_storage/`, `spike_storage/` | `InspectorScreen`, `StylusLabViewModel` | **PARCIAL — recursos existem; controle explícito Gravar/Parar pendente** |
| F6.15 | Pressão/tilt/orientação | Disponível/ausente/a verificar pela capability; zero suportado continua zero | `InputDevice.MotionRange` | `DeviceCapabilityInspector.extractSample` corrigido | **SOFTWARE_IMPLEMENTADO; linguagem 'a verificar' em UI pendente** |
| F6.16 | Isolamento do laboratório | Amostras não entram em evolução/SRS/alfabeto | diretórios exclusivos de laboratório | `StylusLabViewModel` usa repositório local independente | **ESTRUTURALMENTE_VERIFICADO; teste de regressão local recomendado** |
| F6.17 | Reinício | Preferências e raw do laboratório sobrevivem reinício | SharedPreferences + `.scribe` de laboratório | store + save/load existentes | **PENDENTE_TESTE_EXECUTADO** |
| F6.18 | S25 Ultra + Watch | Evidência física separada | bancada física | não fabricada | **PENDENTE_DISPOSITIVO** |

## Testes adicionados nesta branch

`app/src/test/java/com/scribe/caligrafia/PhaseF6AcceptanceTest.kt` cobre de forma determinística: recomendação de lado da barra, padrão Stylus Only, não mutação de pressão raw, relógio de pausa sem vazamento em pausa/background e reset ao desabilitar. **Os testes ainda não foram executados nesta sessão.**

## Gate F6.G

**NÃO FECHADO.** O núcleo de persistência e várias políticas de render/entrada foram implementados, porém há itens de UI, integração de sessão e hardware físico pendentes. Esta matriz é a fonte de passagem de bastão para concluir o gate sem criar falso positivo.
