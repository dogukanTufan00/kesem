package io.github.dogukantufan00.kesem

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KesemApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.walletRepository.ensureSeeded()
            if (!container.settingsRepository.isWarmPaletteApplied()) {
                container.walletRepository.harmonizeLegacyColors()
                container.settingsRepository.markWarmPaletteApplied()
            }
        }
    }
}
