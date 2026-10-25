package app.levelup.cloud

import app.levelup.data.LevelUpRepository
import app.levelup.data.SyncEngine
import app.levelup.data.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Une la cuenta con la base local. Reglas:
 * - Sin cuenta todo funciona igual (modo invitado); nada sale del teléfono.
 * - Al entrar, lo local se fusiona con la cuenta. Si el teléfono estaba enlazado a otra cuenta, antes se
 *   borra lo local (ya está en aquella cuenta) para no mezclar datos de dos personas.
 * - Al salir, se borra lo de este teléfono; sigue en la cuenta.
 */
class CloudService(
    val cloud: Cloud,
    private val engine: SyncEngine,
    private val repo: LevelUpRepository,
    scope: CoroutineScope,
    /** Pide al sistema una sincronización en segundo plano (con red, con reintentos). */
    private val scheduleSync: () -> Unit,
) {
    val account: Flow<AccountState> = cloud.account
    val pending: Flow<Int> = engine.pending
    val syncState: Flow<SyncState?> = engine.state

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    init {
        // Cada cambio local con sesión abierta acaba subiendo, aunque se cierre la app.
        scope.launch {
            combine(account, pending) { a, p -> a is AccountState.SignedIn && p > 0 }
                .distinctUntilChanged()
                .collect { if (it) scheduleSync() }
        }
    }

    /** Al abrir la app: si hay sesión pero la base no está enlazada a ella (p. ej. tras restaurar), enlazar. */
    suspend fun reconcile() {
        val user = cloud.currentUserId() ?: return
        if (engine.userId() != user) link(user)
    }

    private suspend fun link(user: String) {
        val linked = engine.userId()
        if (linked != null && linked != user) engine.detach(wipeLocal = true)
        engine.attach(user)
    }

    private suspend fun afterSignIn() {
        link(cloud.currentUserId() ?: return)
        syncNow()
    }

    suspend fun signIn(email: String, password: String) = runCatching {
        cloud.signIn(email.trim(), password)
        afterSignIn()
    }

    suspend fun signUp(email: String, password: String) = runCatching { cloud.signUp(email.trim(), password) }

    suspend fun confirmSignUp(email: String, code: String) = runCatching {
        cloud.confirmSignUp(email.trim(), code.trim())
        afterSignIn()
    }

    suspend fun resendCode(email: String) = runCatching { cloud.resendSignUpCode(email.trim()) }

    suspend fun requestPasswordReset(email: String) = runCatching { cloud.requestPasswordReset(email.trim()) }

    suspend fun resetPassword(email: String, code: String, newPassword: String) = runCatching {
        cloud.resetPassword(email.trim(), code.trim(), newPassword)
        afterSignIn()
    }

    suspend fun syncNow(): Result<Unit> {
        if (engine.userId() == null) return Result.failure(IllegalStateException("Sin cuenta"))
        _syncing.value = true
        return try {
            engine.sync().onSuccess {
                // Lo que ven los amigos (si tiene perfil y lo permite). Si falla, no es motivo para fallar la sincronización.
                runCatching { cloud.touchProfile(repo.stats().currentStreak, repo.prefs().equippedFrame) }
            }
        } finally {
            _syncing.value = false
        }
    }

    suspend fun hasPending(): Boolean = engine.hasPending()

    /** Cierra la sesión y borra lo de este teléfono. Antes intenta subir lo pendiente. */
    suspend fun signOut() = runCatching {
        if (engine.hasPending()) syncNow()
        engine.detach(wipeLocal = true)
        runCatching { cloud.signOut() }
    }

    suspend fun signOutOtherDevices() = runCatching { cloud.signOutOtherDevices() }

    /** Borra la cuenta y lo guardado en el servidor. Lo de este teléfono se queda como invitado. */
    suspend fun deleteAccount() = runCatching {
        cloud.deleteAccount()
        engine.detach(wipeLocal = false)
    }
}
