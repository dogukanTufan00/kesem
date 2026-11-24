package io.github.dogukantufan00.kesem.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val manager = context.getSystemService(BiometricManager::class.java) ?: return false
                manager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                isBiometricAvailableApi29(context)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            }
            else -> false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("DEPRECATION")
    private fun isBiometricAvailableApi29(context: Context): Boolean {
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Bu cihazda biyometrik doğrulama desteklenmiyor")
            return
        }
        authenticateApi28(context, title, subtitle, onSuccess, onError)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticateApi28(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val cancellationSignal = CancellationSignal()
        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton("PIN Kullan", context.mainExecutor) { _, _ ->
                onError(null)
            }
            .build()

        prompt.authenticate(
            cancellationSignal,
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    val userCancelled =
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                    onError(if (userCancelled) null else errString?.toString())
                }

                override fun onAuthenticationFailed() {
                    // Sistem arayüzü tekrar denemeye izin verir
                }
            }
        )
    }
}
