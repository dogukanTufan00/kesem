package io.github.dogukantufan00.kesem.ui.security

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.model.AppLockConfig
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.BiometricHelper
import io.github.dogukantufan00.kesem.util.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(
    appLockConfig: AppLockConfig,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kesemColors = LocalKesemColors.current
    val scope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutUntilElapsedMs by remember { mutableLongStateOf(0L) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    val canUseBiometric = remember(context, appLockConfig.isBiometricEnabled) {
        appLockConfig.isBiometricEnabled && BiometricHelper.isBiometricAvailable(context)
    }

    LaunchedEffect(lockoutUntilElapsedMs) {
        while (lockoutUntilElapsedMs > 0L) {
            val diffMs = lockoutUntilElapsedMs - SystemClock.elapsedRealtime()
            if (diffMs <= 0L) {
                remainingSeconds = 0
                lockoutUntilElapsedMs = 0L
                errorMessage = null
                break
            } else {
                remainingSeconds = ((diffMs + 999L) / 1000L).toInt()
                delay(500L)
            }
        }
    }

    fun launchBiometric() {
        BiometricHelper.authenticate(
            context = context,
            title = "Kesem Kilidini Aç",
            subtitle = "Devam etmek için biyometrik doğrulamayı kullan",
            onSuccess = onUnlocked,
            onError = { err ->
                if (!err.isNullOrBlank()) {
                    errorMessage = err
                }
            }
        )
    }

    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            launchBiometric()
        }
    }

    fun onDigitPressed(digit: Int) {
        if (isVerifying || remainingSeconds > 0 || enteredPin.length >= 4) return
        errorMessage = null
        val next = enteredPin + digit.toString()
        enteredPin = next

        if (next.length == 4) {
            isVerifying = true
            scope.launch {
                val ok = withContext(Dispatchers.Default) {
                    PinHasher.verifyPin(next, appLockConfig.pinSalt, appLockConfig.pinHash)
                }
                isVerifying = false
                if (ok) {
                    failedAttempts = 0
                    enteredPin = ""
                    onUnlocked()
                } else {
                    val attempts = failedAttempts + 1
                    enteredPin = ""
                    if (attempts >= 5) {
                        failedAttempts = 0
                        lockoutUntilElapsedMs = SystemClock.elapsedRealtime() + 30_000L
                        errorMessage = "Çok fazla hatalı deneme. 30 sn bekle."
                    } else {
                        failedAttempts = attempts
                        errorMessage = "Hatalı PIN (${5 - attempts} deneme kaldı)"
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "Kesem Kilitli",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (remainingSeconds > 0) {
                        "Güvenlik için $remainingSeconds saniye bekle"
                    } else {
                        "Devam etmek için 4 haneli PIN kodunu gir"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = kesemColors.subtleText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                PinDotsIndicator(
                    filledCount = enteredPin.length,
                    isError = errorMessage != null
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            PinKeypad(
                enabled = remainingSeconds == 0 && !isVerifying,
                showBiometric = canUseBiometric,
                onDigit = ::onDigitPressed,
                onBackspace = {
                    if (enteredPin.isNotEmpty() && !isVerifying) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onBiometricClick = ::launchBiometric
            )
        }
    }
}

@Composable
internal fun PinDotsIndicator(
    filledCount: Int,
    isError: Boolean
) {
    val activeColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { idx ->
            val filled = idx < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(activeColor)
                        } else {
                            Modifier.border(1.5.dp, borderColor, CircleShape)
                        }
                    )
            )
        }
    }
}

@Composable
internal fun PinKeypad(
    enabled: Boolean,
    showBiometric: Boolean,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { digit ->
                    KeypadDigitButton(
                        digit = digit,
                        enabled = enabled,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (showBiometric) {
                Surface(
                    onClick = onBiometricClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Biyometrik Doğrulama",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(70.dp))
            }

            KeypadDigitButton(
                digit = 0,
                enabled = enabled,
                onClick = { onDigit(0) }
            )

            Surface(
                onClick = onBackspace,
                enabled = enabled,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(70.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .size(70.dp)
            .border(1.dp, LocalKesemColors.current.cardBorder, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    LocalKesemColors.current.subtleText
                }
            )
        }
    }
}
