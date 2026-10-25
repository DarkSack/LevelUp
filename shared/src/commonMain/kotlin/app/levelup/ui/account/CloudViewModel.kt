package app.levelup.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levelup.cloud.AccountState
import app.levelup.cloud.CloudErrors
import app.levelup.cloud.CloudService
import app.levelup.cloud.GroupChallenge
import app.levelup.cloud.LeaderboardRow
import app.levelup.cloud.MyProfile
import app.levelup.cloud.ProfileCard
import app.levelup.cloud.SocialNotice
import app.levelup.data.SyncState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Cuenta, sincronización y lo social. Los datos sociales se piden al abrir cada pantalla (no hay caché local). */
class CloudViewModel(private val service: CloudService) : ViewModel() {
    val account: StateFlow<AccountState> = service.account.stateIn(viewModelScope, SharingStarted.Eagerly, AccountState.Loading)
    val pending: StateFlow<Int> = service.pending.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val syncState: StateFlow<SyncState?> = service.syncState.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val syncing: StateFlow<Boolean> = service.syncing

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Avisos para mostrar en la barra inferior (errores ya traducidos y confirmaciones). */
    val messages: SharedFlow<String> = _messages

    val profile = MutableStateFlow<MyProfile?>(null)
    val profileLoaded = MutableStateFlow(false)
    val friends = MutableStateFlow<List<ProfileCard>>(emptyList())
    val requests = MutableStateFlow<List<ProfileCard>>(emptyList())
    val searchResults = MutableStateFlow<List<ProfileCard>>(emptyList())
    val groups = MutableStateFlow<List<GroupChallenge>>(emptyList())
    val groupHistory = MutableStateFlow<List<app.levelup.cloud.GroupHistoryItem>>(emptyList())
    val leaderboard = MutableStateFlow<List<LeaderboardRow>>(emptyList())
    val notices = MutableStateFlow<List<SocialNotice>>(emptyList())

    private fun <T> run(done: String? = null, block: suspend () -> Result<T>, onOk: (T) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block().fold(
                    onSuccess = { onOk(it); done?.let { m -> _messages.emit(m) } },
                    onFailure = { _messages.emit(CloudErrors.message(it)) },
                )
            } finally {
                _busy.value = false
            }
        }
    }

    // ── Cuenta ────────────────────────────────────────────────────────────────

    fun onAppOpened() = viewModelScope.launch {
        runCatching { service.reconcile() }
        if (account.value is AccountState.SignedIn) service.syncNow()
    }

    fun signIn(email: String, password: String) = run("Sesión iniciada. Tus datos se están sincronizando.", { service.signIn(email, password) })
    fun signUp(email: String, password: String, onCodeSent: () -> Unit) = run(null, { service.signUp(email, password) }) { onCodeSent() }
    fun confirm(email: String, code: String) = run("Cuenta creada. Tus datos ya se guardan en la nube.", { service.confirmSignUp(email, code) })
    fun resendCode(email: String) = run("Te enviamos un código nuevo.", { service.resendCode(email) })
    fun requestReset(email: String, onSent: () -> Unit) = run(null, { service.requestPasswordReset(email) }) { onSent() }
    fun resetPassword(email: String, code: String, password: String) =
        run("Contraseña cambiada.", { service.resetPassword(email, code, password) })

    fun syncNow() = run("Sincronizado.", { service.syncNow() })
    fun signOutOthers() = run("Se cerró la sesión en tus otros dispositivos.", { service.signOutOtherDevices() })
    fun signOut() = run("Sesión cerrada.", { service.signOut() }) { clearSocial() }
    fun deleteAccount() = run("Cuenta borrada. Lo de este teléfono sigue aquí como invitado.", { service.deleteAccount() }) { clearSocial() }

    suspend fun hasPending() = service.hasPending()

    private fun clearSocial() {
        profile.value = null; profileLoaded.value = false
        friends.value = emptyList(); requests.value = emptyList(); searchResults.value = emptyList()
        groups.value = emptyList(); leaderboard.value = emptyList(); notices.value = emptyList()
        groupHistory.value = emptyList()
    }

    // ── Social ────────────────────────────────────────────────────────────────

    private val cloud get() = service.cloud

    fun refreshSocial() = viewModelScope.launch {
        runCatching {
            val p = cloud.myProfile()
            profile.value = p
            profileLoaded.value = true
            if (p != null) {
                val f = async { cloud.friends() }
                val r = async { cloud.pendingRequests() }
                val g = async { cloud.groups() }
                val l = async { cloud.leaderboard() }
                val n = async { cloud.notices() }
                // Aparte: si el servidor aún no tiene el historial, lo demás sigue funcionando.
                val h = async { runCatching { cloud.groupHistory() }.getOrDefault(emptyList()) }
                friends.value = f.await(); requests.value = r.await(); groups.value = g.await()
                leaderboard.value = l.await(); notices.value = n.await(); groupHistory.value = h.await()
            }
        }.onFailure { _messages.emit(CloudErrors.message(it)) }
    }

    fun saveProfile(value: MyProfile, onOk: () -> Unit = {}) =
        run("Perfil guardado.", { runCatching { cloud.saveProfile(value) } }) { profile.value = value; onOk(); refreshSocial() }

    fun search(query: String) = viewModelScope.launch {
        runCatching { cloud.search(query) }.fold({ searchResults.value = it }, { _messages.emit(CloudErrors.message(it)) })
    }

    fun sendRequest(username: String) = run("Solicitud enviada.", { runCatching { cloud.sendFriendRequest(username) } }) { refreshSocial() }
    fun respond(requestId: String, accept: Boolean) =
        run(if (accept) "Ahora sois amigos." else "Solicitud rechazada.", { runCatching { cloud.respondFriendRequest(requestId, accept) } }) { refreshSocial() }
    fun cancelRequest(requestId: String) = run("Solicitud retirada.", { runCatching { cloud.cancelFriendRequest(requestId) } }) { refreshSocial() }
    fun removeFriend(userId: String) = run("Ya no sois amigos.", { runCatching { cloud.removeFriend(userId) } }) { refreshSocial() }
    fun block(userId: String) = run("Bloqueado. No podrá encontrarte ni escribirte.", { runCatching { cloud.block(userId) } }) { refreshSocial() }
    fun report(userId: String, reason: String) = run("Denuncia enviada. La revisaremos.", { runCatching { cloud.report(userId, reason) } })
    fun react(userId: String, kind: String) = run("Enviado.", { runCatching { cloud.react(userId, kind) } })
    fun markNoticesRead() = viewModelScope.launch { runCatching { cloud.markNoticesRead() } }

    fun createGroup(challengeId: String, mode: String, target: Int, startsOn: LocalDate, endsOn: LocalDate, members: List<String>, onOk: () -> Unit) =
        run("Reto en grupo creado.", { runCatching { cloud.createGroup(challengeId, mode, target, startsOn, endsOn, members) } }) { onOk(); refreshSocial() }
    fun respondGroup(groupId: String, accept: Boolean) =
        run(if (accept) "Te uniste al reto." else "Invitación rechazada.", { runCatching { cloud.respondGroup(groupId, accept) } }) { refreshSocial() }
    fun leaveGroup(groupId: String) = run("Saliste del reto.", { runCatching { cloud.leaveGroup(groupId) } }) { refreshSocial() }
}
