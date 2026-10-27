package app.levelup.cloud

import app.levelup.data.RemoteRecord
import app.levelup.data.RemoteSync
import app.levelup.data.SyncRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * URL y clave pública del proyecto. Vienen de `supabase.properties` (fuera de git); sin ellas no hay nube.
 * [userAgent]: «LevelUp 0.1.0 (Pixel 7; Android 14)». Supabase lo guarda con cada sesión y es el nombre en la lista de dispositivos.
 */
data class CloudConfig(val url: String, val publishableKey: String, val userAgent: String = "")

/** Un dispositivo con la sesión iniciada. */
@Serializable
data class DeviceSession(val id: String, val userAgent: String, val createdAt: String, val lastUsedAt: String, val current: Boolean) {
    /** «Pixel 7 · Android 14» a partir de [userAgent]; null si no viene de LevelUp. */
    val device: String? get() = Regex("^LevelUp \\S+ \\(([^;]+); ([^)]+)\\)$").find(userAgent)?.destructured?.let { (model, os) -> "$model · $os" }
}

sealed interface AccountState {
    data object Loading : AccountState
    data object Guest : AccountState
    data class SignedIn(val userId: String, val email: String) : AccountState
}

@Serializable
data class MyProfile(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    val frame: String = "frame.basic",
    val discoverable: Boolean = true,
    @SerialName("share_progress") val shareProgress: Boolean = true,
    @SerialName("show_activity") val showActivity: Boolean = false,
    @SerialName("leaderboard_opt_in") val leaderboardOptIn: Boolean = false,
    /** Clave de Avatars (emoji predefinido); vacía = la inicial. */
    val avatar: String = "",
    /** Nivel del título elegido (XpRules.TITLES). El servidor solo lo muestra si se alcanzó. */
    @SerialName("title_level") val titleLevel: Int? = null,
    /** Hasta tres ids de logros. El servidor solo muestra los desbloqueados. */
    val trophies: List<String> = emptyList(),
)

/** Lo que una persona deja ver de sí misma (los campos opcionales llegan vacíos si no lo permite). */
@Serializable
data class ProfileCard(
    val userId: String,
    val username: String,
    val displayName: String,
    val frame: String = "frame.basic",
    val isFriend: Boolean = false,
    val level: Int? = null,
    val streak: Int? = null,
    val weeklyXp: Long? = null,
    val lastActiveAt: String? = null,
    val requestId: String? = null,
    val incoming: Boolean? = null,
    val avatar: String = "",
    val title: Int? = null,
    val trophies: List<String> = emptyList(),
)

/** Un reto en grupo terminado o que se dejó, para el historial de participación. */
@Serializable
data class GroupHistoryItem(
    val id: String,
    val challengeId: String,
    val mode: String,
    val target: Int,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val myStatus: String,
    val members: Int,
    val myCompleted: Int,
    val groupCompleted: Int,
)

@Serializable
data class GroupMember(val userId: String, val username: String, val displayName: String, val status: String, val completed: Int)

@Serializable
data class GroupChallenge(
    val id: String,
    val challengeId: String,
    val mode: String,
    val target: Int,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val ownerId: String,
    val myStatus: String,
    val members: List<GroupMember>,
)

@Serializable
data class LeaderboardRow(val userId: String, val username: String, val displayName: String, val frame: String, val weeklyXp: Long, val isMe: Boolean)

@Serializable
data class NoticeActor(val userId: String, val username: String, val displayName: String)

@Serializable
data class SocialNotice(val id: String, val kind: String, val refId: String? = null, val read: Boolean, val createdAt: String, val actor: NoticeActor? = null)

/** Reacciones predefinidas (sin texto libre). La clave es la que guarda el servidor. */
val Reactions = listOf("aplauso" to "👏", "fuerza" to "💪", "estrella" to "⭐", "fuego" to "🔥", "corazon" to "❤️")

/** Mensajes de ánimo predefinidos: se envían como reacciones, sin texto libre. */
val Encouragements = listOf(
    "animo_puedes" to "¡Tú puedes!",
    "animo_sigue" to "Sigue así",
    "animo_paso" to "Paso a paso",
    "animo_contigo" to "Estoy contigo",
    "animo_descansa" to "Descansa, mañana sigues",
)

val ReportReasons = listOf(
    "nombre_ofensivo" to "Nombre ofensivo",
    "acoso" to "Acoso",
    "spam" to "Spam",
    "suplantacion" to "Se hace pasar por otra persona",
    "otro" to "Otro motivo",
)

/**
 * Se entra con correo o con nombre de usuario. Supabase solo admite correo, así que el nombre se guarda como un
 * correo interno de un dominio propio (con MX, o el servidor lo rechaza) al que nunca se envía nada.
 */
object Login {
    private const val PREFIX = "levelup-"
    private const val DOMAIN = "@sackito.online"

    fun email(login: String): String = login.trim().lowercase().let { if ('@' in it) it else PREFIX + it + DOMAIN }

    /** Lo que ve la persona: su nombre de usuario o su correo. */
    fun shown(email: String): String =
        if (email.startsWith(PREFIX) && email.endsWith(DOMAIN)) email.removePrefix(PREFIX).removeSuffix(DOMAIN) else email
}

/** Errores del servidor y de la red, en palabras para la persona. Nunca muestra datos internos. */
object CloudErrors {
    fun message(e: Throwable): String {
        if (e is HttpRequestException) return "Sin conexión. Inténtalo cuando tengas internet."
        val code = when (e) {
            is AuthRestException -> e.errorCode?.value ?: e.error
            is RestException -> e.error
            else -> null
        }
        return when (code) {
            "invalid_credentials" -> "Correo, usuario o contraseña incorrectos."
            "user_already_exists", "email_exists" -> "Ese correo o nombre de usuario ya tiene cuenta."
            "weak_password" -> "La contraseña necesita al menos 8 caracteres, con letras y números."
            "over_email_send_rate_limit", "over_request_rate_limit" -> "Demasiados intentos. Espera un minuto."
            "email_address_invalid", "validation_failed" -> "Revisa el correo: no parece válido."
            "same_password" -> "La contraseña nueva tiene que ser distinta de la anterior."
            "user_not_found" -> "No encontramos a nadie con ese nombre de usuario."
            "already_friends" -> "Ya sois amigos."
            "already_requested" -> "Ya le enviaste una solicitud."
            "recently_declined" -> "Esa persona rechazó tu solicitud hace poco. Inténtalo más adelante."
            "rate_limited" -> "Has hecho eso muchas veces hoy. Prueba mañana."
            "profile_required" -> "Primero crea tu perfil social."
            "not_friends" -> "Solo puedes hacer eso con tus amigos."
            "request_not_found", "invite_not_found" -> "Esa solicitud ya no está disponible."
            "invalid_members" -> "Invita a entre 1 y 9 amigos."
            "invalid_dates" -> "Revisa las fechas del reto."
            "23505" -> "Ese nombre de usuario ya está en uso."
            else -> if (e is RestException && e.statusCode == 409) "Ese nombre de usuario ya está en uso."
            else "Algo falló en el servidor. Inténtalo de nuevo más tarde."
        }
    }
}

/**
 * Todo lo que habla con Supabase. Solo usa la clave pública: lo que cada persona puede leer o escribir lo
 * deciden las políticas RLS y las funciones del servidor (supabase/migrations), no esta clase.
 */
class Cloud(config: CloudConfig, sessionManager: SessionManager?) : RemoteSync {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val client: SupabaseClient = createSupabaseClient(config.url, config.publishableKey) {
        defaultSerializer = KotlinXSerializer(json)
        install(Auth) {
            if (sessionManager != null) this.sessionManager = sessionManager
            // El de serie necesita multiplatform-settings con androidx.startup y revienta si no arrancó. Solo lo usa
            // el flujo PKCE (OAuth y enlaces mágicos), que LevelUp no usa: con correo y código basta la memoria.
            codeVerifierCache = MemoryCodeVerifierCache()
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        // httpConfig es «interna» en supabase-kt, pero es la única forma de poner el User-Agent desde código común.
        @OptIn(SupabaseInternal::class)
        if (config.userAgent.isNotBlank()) httpConfig { install(UserAgent) { agent = config.userAgent } }
    }

    private val auth get() = client.auth
    private val db get() = client.postgrest

    val account: Flow<AccountState> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.let { AccountState.SignedIn(it.id, it.email.orEmpty()) }
                ?: AccountState.Guest
            is SessionStatus.Initializing -> AccountState.Loading
            // Sin conexión al refrescar: la sesión sigue guardada y se reintenta sola.
            is SessionStatus.RefreshFailure -> auth.currentUserOrNull()?.let { AccountState.SignedIn(it.id, it.email.orEmpty()) }
                ?: AccountState.Guest
            is SessionStatus.NotAuthenticated -> AccountState.Guest
        }
    }

    suspend fun currentUserId(): String? {
        auth.awaitInitialization()
        return auth.currentUserOrNull()?.id
    }

    // ── Cuenta ────────────────────────────────────────────────────────────────

    /** Sin verificación de correo («Confirm email» apagado en Supabase): el registro ya deja la sesión iniciada. */
    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) { this.email = email; this.password = password }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) { this.email = email; this.password = password }
    }

    /** Con la sesión iniciada; sin correo es la única forma de cambiarla. */
    suspend fun changePassword(password: String) {
        auth.updateUser { this.password = password }
    }

    suspend fun signOut() = auth.signOut(SignOutScope.LOCAL)

    suspend fun signOutOtherDevices() = auth.signOut(SignOutScope.OTHERS)

    suspend fun sessions(): List<DeviceSession> = db.rpc("my_sessions").decodeList()

    suspend fun endSession(id: String) {
        db.rpc("end_session", buildJsonObject { put("session", id) })
    }

    /** Borra la cuenta y todo lo guardado en el servidor (en cascada). Después cierra la sesión local. */
    suspend fun deleteAccount() {
        db.rpc("delete_account")
        runCatching { auth.signOut(SignOutScope.LOCAL) }
    }

    // ── Sincronización ────────────────────────────────────────────────────────

    override suspend fun push(records: List<SyncRecord>) {
        db.rpc("push_records", buildJsonObject { put("records", json.encodeToJsonElement<List<SyncRecord>>(records)) })
    }

    override suspend fun pull(after: Long, limit: Int): List<RemoteRecord> =
        db.from("user_records").select(Columns.list("entity", "id", "data", "client_updated_at", "deleted", "seq")) {
            filter { gt("seq", after) }
            order("seq", Order.ASCENDING)
            limit(limit.toLong())
        }.decodeList()

    // ── Social ────────────────────────────────────────────────────────────────

    suspend fun myProfile(): MyProfile? {
        val me = currentUserId() ?: return null
        return db.from("profiles").select { filter { eq("user_id", me) } }.decodeSingleOrNull()
    }

    suspend fun saveProfile(profile: MyProfile) {
        db.from("profiles").upsert(profile)
    }

    suspend fun touchProfile(streak: Int, frame: String, avatar: String) {
        db.rpc("touch_profile", buildJsonObject { put("streak", streak); put("avatar_frame", frame); put("avatar_key", avatar) })
    }

    suspend fun search(query: String): List<ProfileCard> =
        db.rpc("search_profiles", buildJsonObject { put("query", query) }).decodeList()

    suspend fun friends(): List<ProfileCard> = db.rpc("friends").decodeList()

    suspend fun pendingRequests(): List<ProfileCard> = db.rpc("pending_requests").decodeList()

    suspend fun sendFriendRequest(username: String) {
        db.rpc("send_friend_request", buildJsonObject { put("target_username", username) })
    }

    suspend fun respondFriendRequest(requestId: String, accept: Boolean) {
        db.rpc("respond_friend_request", buildJsonObject { put("request_id", requestId); put("accept", accept) })
    }

    suspend fun cancelFriendRequest(requestId: String) {
        db.rpc("cancel_friend_request", buildJsonObject { put("request_id", requestId) })
    }

    suspend fun removeFriend(userId: String) {
        db.rpc("remove_friend", buildJsonObject { put("friend", userId) })
    }

    suspend fun block(userId: String) {
        db.rpc("block_user", buildJsonObject { put("target", userId) })
    }

    suspend fun report(userId: String, reason: String) {
        db.rpc("report_user", buildJsonObject { put("target", userId); put("why", reason) })
    }

    suspend fun react(userId: String, kind: String) {
        db.rpc("send_reaction", buildJsonObject { put("target", userId); put("reaction", kind) })
    }

    suspend fun notices(): List<SocialNotice> = db.rpc("my_notifications").decodeList()

    suspend fun markNoticesRead() {
        val me = currentUserId() ?: return
        db.from("notifications").update({ set("read", true) }) { filter { eq("user_id", me); eq("read", false) } }
    }

    suspend fun groups(): List<GroupChallenge> = db.rpc("my_groups").decodeList()

    suspend fun groupHistory(): List<GroupHistoryItem> = db.rpc("my_group_history").decodeList()

    suspend fun createGroup(challengeId: String, mode: String, target: Int, startsOn: LocalDate, endsOn: LocalDate, friends: List<String>) {
        db.rpc("create_group_challenge", buildJsonObject {
            put("challenge", challengeId)
            put("group_mode", mode)
            put("goal", target)
            put("starts", startsOn.toString())
            put("ends", endsOn.toString())
            putJsonArray("invitees") { friends.forEach { add(it) } }
        })
    }

    suspend fun respondGroup(groupId: String, accept: Boolean) {
        db.rpc("respond_group_invite", buildJsonObject { put("group_id", groupId); put("accept", accept) })
    }

    suspend fun leaveGroup(groupId: String) {
        db.rpc("leave_group", buildJsonObject { put("group_id", groupId) })
    }

    suspend fun leaderboard(): List<LeaderboardRow> = db.rpc("friends_leaderboard").decodeList()
}
