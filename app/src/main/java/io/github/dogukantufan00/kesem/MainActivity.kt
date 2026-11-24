package io.github.dogukantufan00.kesem

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dogukantufan00.kesem.model.AppLockConfig
import io.github.dogukantufan00.kesem.model.ThemeMode
import io.github.dogukantufan00.kesem.ui.navigation.KesemAppScaffold
import io.github.dogukantufan00.kesem.ui.security.AppLockController
import io.github.dogukantufan00.kesem.ui.security.LocalAppLockController
import io.github.dogukantufan00.kesem.ui.security.LockScreen
import io.github.dogukantufan00.kesem.ui.theme.DarkBackground
import io.github.dogukantufan00.kesem.ui.theme.KesemTheme
import io.github.dogukantufan00.kesem.ui.theme.LightBackground
import kotlinx.coroutines.flow.combine

private data class StartupPrefs(
    val themeMode: ThemeMode,
    val appLockConfig: AppLockConfig
)

class MainActivity : ComponentActivity() {

    companion object {
        private const val BACKGROUND_RELOCK_TIMEOUT_MS = 30_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as KesemApp).container
        setContent {
            val startupPrefs by remember(container) {
                combine(
                    container.settingsRepository.themeMode,
                    container.settingsRepository.appLockConfig,
                    ::StartupPrefs
                )
            }.collectAsStateWithLifecycle(initialValue = null)

            val systemDark = isSystemInDarkTheme()
            val resolvedThemeMode = startupPrefs?.themeMode ?: ThemeMode.SYSTEM
            val useDark = when (resolvedThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            DisposableEffect(useDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (useDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            val currentPrefs = startupPrefs
            if (currentPrefs == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (systemDark) DarkBackground else LightBackground)
                )
                return@setContent
            }

            var initializedLockState by remember { mutableStateOf(false) }
            var wasPinEnabled by remember { mutableStateOf(false) }
            var isUnlocked by remember { mutableStateOf(false) }
            var backgroundedAtElapsedMs by remember { mutableStateOf<Long?>(null) }
            var suppressNextRelock by remember { mutableStateOf(false) }

            val pinEnabled = currentPrefs.appLockConfig.isPinEnabled

            LaunchedEffect(pinEnabled) {
                if (!initializedLockState) {
                    initializedLockState = true
                    wasPinEnabled = pinEnabled
                    isUnlocked = !pinEnabled
                } else {
                    if (!pinEnabled) {
                        isUnlocked = true
                        backgroundedAtElapsedMs = null
                    } else if (!wasPinEnabled) {
                        // Kullanıcı az önce PIN oluşturdu; hemen kilitleme
                        isUnlocked = true
                    }
                    wasPinEnabled = pinEnabled
                }
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, pinEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (!isChangingConfigurations &&
                                pinEnabled &&
                                !suppressNextRelock &&
                                backgroundedAtElapsedMs == null
                            ) {
                                backgroundedAtElapsedMs = SystemClock.elapsedRealtime()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            val suppressed = suppressNextRelock
                            suppressNextRelock = false
                            val stoppedAt = backgroundedAtElapsedMs
                            backgroundedAtElapsedMs = null
                            if (!suppressed && pinEnabled && stoppedAt != null) {
                                val elapsed = SystemClock.elapsedRealtime() - stoppedAt
                                if (elapsed >= BACKGROUND_RELOCK_TIMEOUT_MS) {
                                    isUnlocked = false
                                }
                            }
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val appLockController = remember {
                AppLockController(
                    suppressNextRelock = { suppressNextRelock = true },
                    lockNow = {
                        backgroundedAtElapsedMs = null
                        suppressNextRelock = false
                        isUnlocked = false
                    }
                )
            }

            KesemTheme(themeMode = currentPrefs.themeMode) {
                CompositionLocalProvider(LocalAppLockController provides appLockController) {
                    if (pinEnabled && !isUnlocked) {
                        LockScreen(
                            appLockConfig = currentPrefs.appLockConfig,
                            onUnlocked = { isUnlocked = true }
                        )
                    } else {
                        KesemAppScaffold(container = container)
                    }
                }
            }
        }
    }
}
