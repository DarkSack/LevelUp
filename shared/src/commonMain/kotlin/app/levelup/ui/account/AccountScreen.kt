package app.levelup.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levelup.cloud.AccountState
import app.levelup.ui.Fmt
import app.levelup.ui.Nav
import app.levelup.ui.components.InfoCard
import app.levelup.ui.components.PrimaryButton
import app.levelup.ui.components.ScreenScaffold
import app.levelup.ui.components.SecondaryButton
import app.levelup.ui.components.SectionTitle
import app.levelup.ui.components.ToggleRow
import app.levelup.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.time.Instant

private enum class Mode { SIGN_IN, SIGN_UP, CONFIRM, FORGOT, RESET }

/** Mismas reglas que el servidor (config.toml): al menos 8 caracteres, con letras y números. */
internal fun passwordProblem(password: String): String? = when {
    password.length < 8 -> "Al menos 8 caracteres."
    password.none { it.isLetter() } || password.none { it.isDigit() } -> "Usa letras y números."
    else -> null
}

internal fun emailLooksValid(email: String): Boolean {
    val e = email.trim()
    val at = e.indexOf('@')
    return at > 0 && e.indexOf('.', at) > at + 1 && !e.endsWith('.') && ' ' !in e
}

@Composable
fun AccountScreen(vm: CloudViewModel?, nav: Nav) {
    ScreenScaffold("Cuenta y sincronización", onBack = nav::back) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            if (vm == null) {
                Text("Esta versión de la app no tiene la nube configurada. Todo lo demás funciona sin cuenta.")
                return@Column
            }
            val account by vm.account.collectAsStateWithLifecycle()
            when (val a = account) {
                AccountState.Loading -> Box(Modifier.fillMaxWidth().padding(Space.xl), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                AccountState.Guest -> GuestAccount(vm)
                is AccountState.SignedIn -> SignedInAccount(vm, a)
            }
        }
    }
}

@Composable
private fun GuestAccount(vm: CloudViewModel) {
    var mode by rememberSaveable { mutableStateOf(Mode.SIGN_IN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val busy by vm.busy.collectAsStateWithLifecycle()

    InfoCard {
        Text("La cuenta es opcional", style = MaterialTheme.typography.titleSmall)
        Text(
            "Sin cuenta, LevelUp funciona igual y todo se queda en este teléfono. Con cuenta, tu progreso se guarda " +
                "en la nube para recuperarlo o usarlo en otro teléfono, y puedes conectar con amigos.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    val emailError = email.isNotEmpty() && !emailLooksValid(email)
    val emailField: @Composable () -> Unit = {
        OutlinedTextField(
            email, { email = it.take(254) }, label = { Text("Correo") }, singleLine = true,
            isError = emailError, supportingText = if (emailError) ({ Text("Revisa el correo.") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val passwordField: @Composable (String, String, (String) -> Unit, String?) -> Unit = { label, value, onValue, problem ->
        OutlinedTextField(
            value, { onValue(it.take(72)) }, label = { Text(label) }, singleLine = true,
            isError = problem != null, supportingText = problem?.let { { Text(it) } },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    val codeField: @Composable () -> Unit = {
        OutlinedTextField(
            code, { v -> code = v.filter(Char::isDigit).take(6) }, label = { Text("Código de 6 cifras") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    when (mode) {
        Mode.SIGN_IN -> {
            SectionTitle("Entrar")
            emailField()
            passwordField("Contraseña", password, { password = it }, null)
            ToggleRow("Mostrar contraseña", showPassword, { showPassword = it })
            PrimaryButton("Entrar", { vm.signIn(email, password) }, enabled = !busy && emailLooksValid(email) && password.isNotEmpty())
            TextButton(onClick = { mode = Mode.FORGOT }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Olvidé mi contraseña") }
            SecondaryButton("Crear una cuenta", { mode = Mode.SIGN_UP })
        }
        Mode.SIGN_UP -> {
            SectionTitle("Crear cuenta")
            emailField()
            val problem = password.takeIf { it.isNotEmpty() }?.let(::passwordProblem)
            passwordField("Contraseña", password, { password = it }, problem)
            val mismatch = repeat.isNotEmpty() && repeat != password
            passwordField("Repite la contraseña", repeat, { repeat = it }, if (mismatch) "No coinciden." else null)
            ToggleRow("Mostrar contraseña", showPassword, { showPassword = it })
            Text(
                "Te enviaremos un código para confirmar el correo. Al crear la cuenta aceptas las condiciones de uso " +
                    "(shop.sackito.online/condiciones/levelup) y la política de privacidad (Perfil → Privacidad).",
                style = MaterialTheme.typography.bodySmall,
            )
            PrimaryButton(
                "Crear cuenta",
                { vm.signUp(email, password) { mode = Mode.CONFIRM } },
                enabled = !busy && emailLooksValid(email) && passwordProblem(password) == null && repeat == password,
            )
            SecondaryButton("Ya tengo cuenta", { mode = Mode.SIGN_IN })
        }
        Mode.CONFIRM -> {
            SectionTitle("Confirma tu correo")
            Text("Escribe el código que enviamos a $email. Puede tardar un par de minutos; mira también en spam.")
            codeField()
            PrimaryButton("Confirmar", { vm.confirm(email, code) }, enabled = !busy && code.length == 6)
            TextButton(onClick = { vm.resendCode(email) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Enviar otro código")
            }
            TextButton(onClick = { mode = Mode.SIGN_UP }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cambiar el correo") }
        }
        Mode.FORGOT -> {
            SectionTitle("Recuperar la contraseña")
            Text("Te enviaremos un código para elegir una contraseña nueva.")
            emailField()
            PrimaryButton("Enviar código", { vm.requestReset(email) { mode = Mode.RESET } }, enabled = !busy && emailLooksValid(email))
            SecondaryButton("Volver", { mode = Mode.SIGN_IN })
        }
        Mode.RESET -> {
            SectionTitle("Contraseña nueva")
            Text("Escribe el código que enviamos a $email y tu contraseña nueva.")
            codeField()
            val problem = password.takeIf { it.isNotEmpty() }?.let(::passwordProblem)
            passwordField("Contraseña nueva", password, { password = it }, problem)
            ToggleRow("Mostrar contraseña", showPassword, { showPassword = it })
            PrimaryButton(
                "Cambiar contraseña",
                { vm.resetPassword(email, code, password) },
                enabled = !busy && code.length == 6 && passwordProblem(password) == null,
            )
            SecondaryButton("Volver", { mode = Mode.SIGN_IN })
        }
    }
    if (busy) CircularProgressIndicator(Modifier.padding(Space.m))
}

/** Estado de la sincronización en una línea, para Inicio. Nada mientras no se haya sincronizado nunca. */
@Composable
fun SyncStatusLine(vm: CloudViewModel, onClick: () -> Unit) {
    val pending by vm.pending.collectAsStateWithLifecycle()
    val state by vm.syncState.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val text = when {
        syncing -> "Sincronizando…"
        pending > 0 -> "${Fmt.plural(pending, "cambio", "cambios")} por subir"
        state?.lastError != null -> "Sin sincronizar: se volverá a intentar"
        state?.lastSyncedAt != null -> "Todo sincronizado"
        else -> return
    }
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).padding(bottom = Space.s)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SignedInAccount(vm: CloudViewModel, account: AccountState.SignedIn) {
    val pending by vm.pending.collectAsStateWithLifecycle()
    val state by vm.syncState.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var confirmSignOut by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Text("Sesión iniciada como", style = MaterialTheme.typography.labelLarge)
    Text(account.email, style = MaterialTheme.typography.titleMedium)

    SectionTitle("Sincronización")
    InfoCard {
        val status = when {
            syncing -> "Sincronizando…"
            pending > 0 -> "${Fmt.plural(pending, "cambio pendiente", "cambios pendientes")} de subir. Se subirán solos cuando haya conexión."
            state?.lastError != null -> "No se pudo sincronizar la última vez. Se volverá a intentar."
            state?.lastSyncedAt != null -> "Todo sincronizado."
            else -> "Aún no se ha sincronizado."
        }
        Text(status, style = MaterialTheme.typography.bodyLarge)
        state?.lastSyncedAt?.let { Text("Última vez: ${Fmt.dateTime(it)}", style = MaterialTheme.typography.bodySmall) }
    }
    SecondaryButton("Sincronizar ahora", vm::syncNow, enabled = !syncing && !busy)

    SectionTitle("Seguridad")
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadSessions() }
    if (sessions.isNotEmpty()) {
        Text("Dispositivos con tu sesión iniciada", style = MaterialTheme.typography.labelLarge)
        sessions.forEach { s ->
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.device ?: "Otro dispositivo", style = MaterialTheme.typography.bodyLarge)
                    val used = runCatching { Fmt.dateTime(Instant.parse(s.lastUsedAt).toEpochMilliseconds()) }.getOrNull()
                    Text(if (s.current) "Este dispositivo" else used?.let { "Último uso: $it" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!s.current) TextButton(onClick = { vm.endSession(s.id) }, enabled = !busy) { Text("Cerrar") }
            }
        }
    }
    SecondaryButton("Cerrar sesión en mis otros dispositivos", vm::signOutOthers, enabled = !busy)
    SecondaryButton("Cerrar sesión", { scope.launch { confirmSignOut = vm.hasPending() } }, enabled = !busy)

    SectionTitle("Borrar la cuenta")
    Text(
        "Borra tu cuenta y todo lo guardado en la nube: progreso, perfil, amistades y retos en grupo. No se puede deshacer.",
        style = MaterialTheme.typography.bodyMedium,
    )
    TextButton(onClick = { confirmDelete = 1 }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Borrar mi cuenta", color = MaterialTheme.colorScheme.error)
    }

    confirmSignOut?.let { unsynced ->
        AlertDialog(
            onDismissRequest = { confirmSignOut = null },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text(
                    if (unsynced) "Hay cambios que aún no se subieron (¿sin conexión?). Si cierras sesión ahora, se perderán. " +
                        "Lo demás sigue en tu cuenta."
                    else "Se borran de este teléfono tus datos; siguen en tu cuenta y vuelven al iniciar sesión.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmSignOut = null; vm.signOut() }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = null }) { Text("Cancelar") } },
        )
    }
    if (confirmDelete > 0) {
        AlertDialog(
            onDismissRequest = { confirmDelete = 0 },
            title = { Text(if (confirmDelete == 1) "¿Borrar tu cuenta?" else "¿Seguro? Es definitivo") },
            text = {
                Text(
                    if (confirmDelete == 1) "Se borrará todo lo que hay en la nube. En este teléfono tu progreso seguirá, como invitado."
                    else "Tu cuenta, tu perfil, tus amistades y tu progreso en la nube se borrarán para siempre.",
                )
            },
            confirmButton = {
                TextButton(onClick = { if (confirmDelete == 1) confirmDelete = 2 else { confirmDelete = 0; vm.deleteAccount() } }) {
                    Text(if (confirmDelete == 1) "Continuar" else "Borrar para siempre", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = 0 }) { Text("Cancelar") } },
        )
    }
}

@Composable
internal fun SignInRequired(nav: Nav, what: String) {
    Column(Modifier.fillMaxSize().padding(Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Text("Necesitas una cuenta", style = MaterialTheme.typography.titleLarge)
        Text("$what necesita una cuenta. Lo personal (retos, XP, logros) funciona igual sin ella.")
        PrimaryButton("Entrar o crear cuenta", nav::account)
    }
}
