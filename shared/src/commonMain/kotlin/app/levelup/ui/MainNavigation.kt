package app.levelup.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.levelup.Platform
import app.levelup.content.Cosmetics
import app.levelup.data.CompletionResult
import app.levelup.domain.Achievement
import app.levelup.domain.AchievementState
import app.levelup.domain.GamificationStyle
import app.levelup.domain.XpRules
import app.levelup.ui.challenges.CatalogDetailScreen
import app.levelup.ui.challenges.ChallengesScreen
import app.levelup.ui.challenges.CustomEditScreen
import app.levelup.ui.challenges.InstanceDetailScreen
import app.levelup.ui.components.TrophyBadge
import app.levelup.ui.home.HomeScreen
import app.levelup.ui.profile.PlusScreen
import app.levelup.ui.profile.ProfileScreen
import app.levelup.ui.profile.SettingsScreen
import app.levelup.ui.progress.AchievementsScreen
import app.levelup.ui.progress.ProgressScreen
import androidx.compose.ui.unit.dp
import app.levelup.ui.theme.LocalReduceMotion
import app.levelup.ui.theme.Space
import app.levelup.ui.welcome.WelcomeScreen
import app.levelup.ui.extra.FocusScreen
import app.levelup.ui.extra.GoalsScreen
import app.levelup.ui.extra.SearchScreen
import app.levelup.ui.extra.WeeklyReviewScreen
import app.levelup.cloud.AccountState
import app.levelup.ui.account.AccountScreen
import app.levelup.ui.account.CloudViewModel
import app.levelup.ui.social.CreateGroupScreen
import app.levelup.ui.social.SocialProfileScreen
import app.levelup.ui.social.SocialScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ChallengesRoute
@Serializable object ProgressRoute
@Serializable object ProfileRoute
@Serializable object AchievementsRoute
@Serializable object WelcomeReplayRoute
@Serializable data class InstanceRoute(val id: String)
@Serializable data class CatalogRoute(val challengeId: String)
@Serializable data class CustomEditRoute(val id: String? = null)
@Serializable data class SettingsRoute(val section: String)
@Serializable object AccountRoute
@Serializable object SocialRoute
@Serializable object SocialProfileRoute
@Serializable object CreateGroupRoute
@Serializable object PlusRoute
@Serializable object SearchRoute
@Serializable object GoalsRoute
@Serializable object WeeklyReviewRoute
@Serializable data class FocusRoute(val id: String? = null)

/** Un solo lugar para los avisos: cada pantalla pinta este SnackbarHost en su Scaffold. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Navegación entre pantallas, para no pasar diez lambdas por pantalla. */
class Nav(private val controller: NavHostController) {
    fun back() { controller.popBackStack() }
    fun instance(id: String) = controller.navigate(InstanceRoute(id))
    fun catalog(id: String) = controller.navigate(CatalogRoute(id))
    fun editCustom(id: String? = null) = controller.navigate(CustomEditRoute(id))
    fun achievements() = controller.navigate(AchievementsRoute)
    fun settings(section: String) = controller.navigate(SettingsRoute(section))
    fun welcome() = controller.navigate(WelcomeReplayRoute)
    fun account() = controller.navigate(AccountRoute)
    fun social() = controller.navigate(SocialRoute)
    fun socialProfile() = controller.navigate(SocialProfileRoute)
    fun createGroup() = controller.navigate(CreateGroupRoute)
    fun plus() = controller.navigate(PlusRoute)
    fun search() = controller.navigate(SearchRoute)
    fun goals() = controller.navigate(GoalsRoute)
    fun weeklyReview() = controller.navigate(WeeklyReviewRoute)
    fun focus(id: String? = null) = controller.navigate(FocusRoute(id))

    /** Enlaces de atajos, widget y automatizaciones. Solo abren pantallas: nunca completan ni devuelven datos. */
    fun open(link: String) {
        when {
            // Inicio limpio: con tab() volvería a abrirse lo que quedó guardado en esa pestaña.
            link == "hoy" -> controller.navigate(HomeRoute) {
                popUpTo(controller.graph.findStartDestination().id)
                launchSingleTop = true
            }
            link == "enfoque" -> focus()
            link == "buscar" -> search()
            link == "metas" -> goals()
            link == "privacidad" -> settings("privacy")
            link.startsWith("reto/") -> link.removePrefix("reto/").takeIf { it.isNotBlank() }?.let(::instance)
        }
    }
    fun tab(route: Any) = controller.navigate(route) {
        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class Celebration(val achievements: List<Achievement>, val result: CompletionResult?)

@Composable
fun MainNavigation(vm: LevelUpViewModel, cloud: CloudViewModel?, platform: Platform) {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var celebration by remember { mutableStateOf<Celebration?>(null) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            val current = vm.prefs.value ?: return@collect
            val full = current.gamification == GamificationStyle.FULL && !current.lowStimulation
            val off = current.gamification == GamificationStyle.OFF
            when (event) {
                is UiEvent.Completed -> {
                    if (current.haptics) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    val r = event.result
                    if (full && (r.achievements.isNotEmpty() || r.missions.isNotEmpty() || r.levelAfter > r.levelBefore)) {
                        celebration = Celebration(r.achievements, r)
                    }
                    val text = when {
                        off -> "¡Hecho!"
                        r.comeback -> "¡Qué bien que volviste! +${r.xpGained} XP"
                        else -> "¡Hecho! +${r.xpGained} XP"
                    }
                    launch {
                        val action = snackbar.showSnackbar(text, "Deshacer", duration = SnackbarDuration.Short)
                        if (action == SnackbarResult.ActionPerformed) vm.undo(event.instanceId)
                    }
                }
                is UiEvent.Unlocked -> when {
                    off -> Unit // sin juego: los logros se siguen guardando, pero no se anuncian
                    full -> celebration = Celebration(event.achievements, null)
                    else -> launch { snackbar.showSnackbar("Logro: ${event.achievements.joinToString { it.name }}") }
                }
                is UiEvent.Message -> launch { snackbar.showSnackbar(event.text) }
            }
        }
    }

    LaunchedEffect(cloud) {
        cloud?.messages?.collect { launch { snackbar.showSnackbar(it) } }
    }

    val secure = vm.prefs.collectAsStateWithLifecycle().value?.secureScreens == true
    LaunchedEffect(secure) { platform.setSecure(secure) }

    val link by platform.deepLinks.collectAsStateWithLifecycle()
    LaunchedEffect(link) {
        link?.let {
            nav.open(it)
            platform.consumeDeepLink()
        }
    }

    val reduce = LocalReduceMotion.current
    val signedIn = cloud?.account?.collectAsStateWithLifecycle()?.value is AccountState.SignedIn
    val today by vm.today.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        NavHost(
            controller,
            startDestination = HomeRoute,
            enterTransition = { if (reduce) EnterTransition.None else fadeIn() },
            exitTransition = { if (reduce) ExitTransition.None else fadeOut() },
            popEnterTransition = { if (reduce) EnterTransition.None else fadeIn() },
            popExitTransition = { if (reduce) ExitTransition.None else fadeOut() },
        ) {
            composable<HomeRoute> { HomeScreen(vm, nav) { TabBar(HomeRoute, nav) } }
            composable<ChallengesRoute> { ChallengesScreen(vm, nav) { TabBar(ChallengesRoute, nav) } }
            composable<ProgressRoute> { ProgressScreen(vm, nav) { TabBar(ProgressRoute, nav) } }
            composable<ProfileRoute> { ProfileScreen(vm, nav) { TabBar(ProfileRoute, nav) } }
            composable<AchievementsRoute> { AchievementsScreen(vm, nav) }
            composable<InstanceRoute> { InstanceDetailScreen(vm, nav, it.toRoute<InstanceRoute>().id) }
            composable<CatalogRoute> { CatalogDetailScreen(vm, nav, it.toRoute<CatalogRoute>().challengeId) }
            composable<CustomEditRoute> { CustomEditScreen(vm, nav, it.toRoute<CustomEditRoute>().id) }
            composable<SettingsRoute> { SettingsScreen(vm, nav, platform, it.toRoute<SettingsRoute>().section, signedIn) }
            composable<AccountRoute> { AccountScreen(cloud, nav) }
            composable<SocialRoute> { SocialScreen(cloud, vm, nav, today) }
            composable<SocialProfileRoute> { if (cloud != null) SocialProfileScreen(cloud, vm, nav) }
            composable<CreateGroupRoute> { if (cloud != null) CreateGroupScreen(cloud, nav, today) }
            composable<PlusRoute> { PlusScreen(vm, nav) }
            composable<SearchRoute> { SearchScreen(vm, nav) }
            composable<GoalsRoute> { GoalsScreen(vm, nav) }
            composable<WeeklyReviewRoute> { WeeklyReviewScreen(vm, nav) }
            composable<FocusRoute> { FocusScreen(vm, nav, it.toRoute<FocusRoute>().id) }
            composable<WelcomeReplayRoute> { WelcomeScreen(onStart = nav::back, startLabel = "Volver") }
        }
    }
    celebration?.let { c -> CelebrationDialog(c) { celebration = null } }
}

private data class Tab(val route: Any, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, "Inicio", Icons.Filled.Home),
    Tab(ChallengesRoute, "Retos", Icons.AutoMirrored.Filled.List),
    Tab(ProgressRoute, "Progreso", Icons.Filled.Star),
    Tab(ProfileRoute, "Perfil", Icons.Filled.Person),
)

@Composable
private fun TabBar(current: Any, nav: Nav) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.route == current,
                onClick = { if (tab.route != current) nav.tab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                alwaysShowLabel = true,
            )
        }
    }
}

@Composable
private fun CelebrationDialog(c: Celebration, onDismiss: () -> Unit) {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else 0.85f)
    val result = c.result
    val levelUp = result != null && result.levelAfter > result.levelBefore
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Seguir") } },
        title = {
            Text(
                when {
                    levelUp -> "¡Subiste al nivel ${result.levelAfter}!"
                    c.achievements.size > 1 -> "¡Nuevos logros!"
                    c.achievements.size == 1 -> "¡Logro desbloqueado!"
                    else -> "¡Misión cumplida!"
                },
            )
        },
        text = {
            Column(
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                if (levelUp) Text("Tu título ahora es «${XpRules.titleFor(result.levelAfter)}».")
                result?.missions?.forEach { Text("Misión «${it.title}» completada: +${it.xpReward} XP") }
                c.achievements.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                            TrophyBadge(a.tier, AchievementState.UNLOCKED, size = 40.dp)
                        }
                        Spacer(Modifier.size(Space.m))
                        Column {
                            Text(a.name, style = MaterialTheme.typography.titleSmall)
                            val reward = buildList {
                                if (a.xpReward > 0) add("+${a.xpReward} XP")
                                Cosmetics.rewardOf(a.id)?.let { add("marco ${it.name}") }
                            }.joinToString(" · ")
                            Text(listOf(a.description, reward).filter { it.isNotBlank() }.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
    )
}
