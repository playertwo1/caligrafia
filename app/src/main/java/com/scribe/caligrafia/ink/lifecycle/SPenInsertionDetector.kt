package com.scribe.caligrafia.ink.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado do silo físico da S Pen (aparelhos Samsung Galaxy Note e Ultra, ex: Galaxy S25 Ultra).
 */
enum class SPenSlotState(val displayName: String) {
    INSERTED("No Silo (Encaixada)"),
    DETACHED("Em Uso (Desencaixada)"),
    UNKNOWN("Indisponível / Externo")
}

/**
 * Adapter de hardware para detecção de inserção e remoção da S Pen no silo físico do Galaxy S25 Ultra.
 *
 * A Samsung One UI emite uma transmissão pública de sistema:
 * Action: "com.samsung.pen.INSERT"
 * Extra: "penInsert" (Boolean: true = inserida no silo, false = removida/em uso).
 *
 * Resiliência:
 * 1. Opera com segurança em qualquer aparelho Android (caso não seja Samsung ou não possua silo, permanece UNKNOWN).
 * 2. Método `handleIntent(intent)` desacoplado para testes unitários na JVM.
 * 3. Notifica callbacks reativos para cancelamento/flush de traços em andamento caso a caneta seja guardada.
 */
class SPenInsertionDetector {

    companion object {
        const val ACTION_PEN_INSERTION = "com.samsung.pen.INSERT"
        const val EXTRA_PEN_INSERT = "penInsert"
    }

    private val _state = MutableStateFlow(SPenSlotState.UNKNOWN)
    val state: StateFlow<SPenSlotState> = _state.asStateFlow()

    var onPenInserted: (() -> Unit)? = null
    var onPenDetached: (() -> Unit)? = null

    private var receiver: BroadcastReceiver? = null
    private var isRegistered = false

    /**
     * Processa um evento bruto de inserção/remoção da S Pen (desacoplado do framework Android para testes).
     */
    fun handleRawInsertionEvent(action: String?, isInserted: Boolean): Boolean {
        if (action != ACTION_PEN_INSERTION) {
            return false
        }

        val newState = if (isInserted) SPenSlotState.INSERTED else SPenSlotState.DETACHED
        _state.value = newState

        if (isInserted) {
            onPenInserted?.invoke()
        } else {
            onPenDetached?.invoke()
        }
        return true
    }

    /**
     * Processa um Intent de transmissão de inserção da S Pen emitido pelo sistema Samsung One UI.
     * @return true se o intent foi reconhecido e o estado atualizado.
     */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        return handleRawInsertionEvent(
            action = intent.action,
            isInserted = intent.getBooleanExtra(EXTRA_PEN_INSERT, false)
        )
    }

    /**
     * Registra o BroadcastReceiver no Contexto do Android com proteção contra exceções.
     */
    fun register(context: Context) {
        if (isRegistered) return

        try {
            val filter = IntentFilter(ACTION_PEN_INSERTION)
            val newReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    handleIntent(intent)
                }
            }

            ContextCompat.registerReceiver(
                context,
                newReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )

            receiver = newReceiver
            isRegistered = true
        } catch (e: Exception) {
            // Em dispositivos sem suporte ou restrições de permissão do fabricante,
            // mantém o estado UNKNOWN sem causar interrupção do app.
            _state.value = SPenSlotState.UNKNOWN
        }
    }

    /**
     * Remove o BroadcastReceiver de forma segura.
     */
    fun unregister(context: Context) {
        if (!isRegistered || receiver == null) return

        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignora se o receiver já tiver sido liberado pelo sistema
        } finally {
            receiver = null
            isRegistered = false
        }
    }
}
