# F6 — Matriz opção → efeito → persistência → teste

**Branch de implementação:** `phase-f6-f7-finalization`

> Esta matriz separa implementação de software de validação local e validação física. GitHub Actions não foi usado.

| ID | Opção / capacidade | Efeito esperado | Persistência / fonte | Evidência de código | Estado atual |
|---|---|---|---|---|---|
| F6.01 | Preferências | Grupos Escrita, Leitura e Pausas; Voltar/Concluir coerentes | `ScribePreferencesStore` | `PreferencesContent` expõe todos os campos F6 | **SOFTWARE_IMPLEMENTADO** |
| F6.02 | Mão direita/esquerda | Recomenda barra no lado oposto à mão dominante, sem impedir override | `is_left_handed` + `toolbar_side_override` | `effectiveToolbarSide` aplicado no editor | **SOFTWARE_IMPLEMENTADO; físico pendente** |
| F6.03 | Barra esquerda/direita | Mover dock da ferramenta sem cobrir área da palma | `toolbar_side_override` | dock usa `BottomStart`/`BottomEnd` | **SOFTWARE_IMPLEMENTADO; físico pendente** |
| F6.04 | Só caneta / caneta + toque | Padrão só caneta; modo toque é explícito; palma continua rejeitada sob stylus | `input_mode`; `InputModeRuntime`; `PalmRejectionPolicy` | pipeline padrão segue preferência; override local preservado | **SOFTWARE_IMPLEMENTADO; validar editor físico** |
| F6.05 | Escala de texto | Multiplicar fontScale do sistema; controles essenciais permanecem acessíveis | `text_scale` | controle UI + `system fontScale × multiplier` | **SOFTWARE_IMPLEMENTADO; visual físico pendente** |
| F6.06 | Contraste das guias | Alterar somente alpha do Paint das pautas | `guide_contrast` + compatibilidade `is_high_contrast` | renderer e preview usam o alpha real | **SOFTWARE_IMPLEMENTADO** |
| F6.07 | Reduzir animações | Reduzir transições automáticas; replay voluntário continua disponível | `reduce_animations` | painel de ferramentas remove transições | **SOFTWARE_IMPLEMENTADO** |
| F6.08 | Curva Linear/Suave/Firme | Alterar apenas largura/render derivado | `pressure_curve` | seletores limitados às três curvas | **SOFTWARE_IMPLEMENTADO** |
| F6.09 | Raw de pressão | Raw antes/depois deve ser idêntico nas três curvas | raw em `StrokePoint`; transformação pura | `PhaseF6AcceptanceTest` | **TESTE_LOCAL_APROVADO** |
| F6.10 | Lembrete de pausa | Contar somente escrita ativa em foreground; pausa/fundo não acumulam | `break_reminder_enabled`, `break_interval_minutes` | canvas → ViewModel → `ActiveBreakReminder`; lifecycle pausa contador | **SOFTWARE_IMPLEMENTADO** |
| F6.11 | Vibração | Habilitar/desabilitar; retornar false se aparelho/permissão indisponível | `vibration_enabled` | toggle, teste e fallback implementados | **SOFTWARE_IMPLEMENTADO; físico pendente** |
| F6.12 | Watch conectado | Nunca alegar conexão sem sinal real; fallback permanece no telefone | probe de GATT + `BLUETOOTH_CONNECT` | permissão runtime + status por conexão observada | **SOFTWARE_IMPLEMENTADO; Watch físico pendente** |
| F6.13 | Eventos fase/pausa | Adapter não pode travar sessão sem Watch | bridge desacoplada | `sendTimerSync`, haptics e fallback | **PARCIAL — recepção Wear/Data Layer não comprovada** |
| F6.14 | Laboratório | Captura, limpar, salvar/reabrir, replay | `autosave_storage/`, `spike_storage/` | controles explícitos Gravar/Parar/Salvar/Recarregar/Replay/Limpar | **SOFTWARE_IMPLEMENTADO; físico pendente** |
| F6.15 | Pressão/tilt/orientação | Disponível/ausente/a verificar pela capability; zero suportado continua zero | `InputDevice.MotionRange` | `DeviceCapabilityInspector.extractSample` corrigido | **SOFTWARE_IMPLEMENTADO; linguagem 'a verificar' em UI pendente** |
| F6.16 | Isolamento do laboratório | Amostras não entram em evolução/SRS/alfabeto | diretórios exclusivos de laboratório | `StylusLabViewModel` usa repositório local independente | **ESTRUTURALMENTE_VERIFICADO; teste de regressão local recomendado** |
| F6.17 | Reinício | Preferências e raw do laboratório sobrevivem reinício | SharedPreferences + `.scribe` de laboratório | snapshot central inclui todas as chaves e aceita legado | **SOFTWARE_IMPLEMENTADO; E2E físico pendente** |
| F6.18 | S25 Ultra + Watch | Evidência física separada | bancada física | não fabricada | **PENDENTE_DISPOSITIVO** |

## Testes adicionados nesta branch

`PhaseF6AcceptanceTest` e `ScribePreferencesBackupTest` cobrem lado da barra, Stylus Only, raw imutável, pausa/background/reset e formatos completo/legado do snapshot.

## Gate F6.G

**NÃO FECHADO.** O software foi implementado; validação no S25 Ultra e Galaxy Watch continua `PENDENTE_DISPOSITIVO`.
