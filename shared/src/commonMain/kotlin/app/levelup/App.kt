package app.levelup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.levelup.cloud.CloudService
import app.levelup.data.LevelUpRepository
import app.levelup.ui.account.CloudViewModel
import app.levelup.domain.Preferences
import app.levelup.ui.LevelUpViewModel
import app.levelup.ui.MainNavigation
import app.levelup.ui.components.LevelUpLogo
import app.levelup.ui.onboarding.OnboardingFlow
import app.levelup.ui.theme.LevelUpTheme
import app.levelup.ui.theme.LocalHasAccount
import app.levelup.ui.welcome.WelcomeScreen

/** Punto de entrada de la interfaz, igual en todas las plataformas. */
@Composable
fun App(repo: LevelUpRepository, platform: Platform, cloud: CloudService? = null) {
    val vm = viewModel { LevelUpViewModel(repo, platform) }
    val cloudVm = cloud?.let { viewModel { CloudViewModel(it) } }
    App(vm, platform, cloudVm)
}

@Composable
fun App(vm: LevelUpViewModel, platform: Platform, cloud: CloudViewModel? = null) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    LifecycleResumeEffect(vm) {
        vm.refreshDay()
        cloud?.onAppOpened()
        platform.refreshPlus()
        onPauseOrDispose {}
    }
    val current = prefs
    val plus by platform.plus.collectAsStateWithLifecycle()
    LevelUpTheme(current ?: Preferences(), platform.systemReducedMotion, plus.owned) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CompositionLocalProvider(LocalHasAccount provides (cloud != null)) {
                when {
                    current == null -> LoadingScreen()
                    !current.onboardingDone && current.onboardingStep == 0 ->
                        WelcomeScreen(onStart = { vm.updatePrefs { it.copy(onboardingStep = 1) } })
                    !current.onboardingDone -> OnboardingFlow(vm, current)
                    else -> MainNavigation(vm, cloud, platform)
                }
            }
        }
    }
}

/** Solo se ve un instante mientras Room entrega las preferencias: logo quieto, sin texto ni animación. */
@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LevelUpLogo(Modifier.size(96.dp))
    }
}
