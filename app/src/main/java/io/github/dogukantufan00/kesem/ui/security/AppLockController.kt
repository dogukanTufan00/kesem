package io.github.dogukantufan00.kesem.ui.security

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
class AppLockController(
    val suppressNextRelock: () -> Unit = {},
    val lockNow: () -> Unit = {}
)

val LocalAppLockController = staticCompositionLocalOf { AppLockController() }
