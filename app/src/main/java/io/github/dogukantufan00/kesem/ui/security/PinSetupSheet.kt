package io.github.dogukantufan00.kesem.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dogukantufan00.kesem.ui.theme.LocalKesemColors
import io.github.dogukantufan00.kesem.util.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinSetupMode {
    CREATE,
    CHANGE,
    DISABLE
}

private enum class PinStage {
    VERIFY_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupSheet(
    mode: PinSetupMode,
    currentPinSalt: String,
    currentPinHash: String,
    onPinConfigured: (String) -> Unit,
    onPinDisabled: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val kesemColors = LocalKesemColors.current
    val scope = rememberCoroutineScope()

    var stage by remember(mode) {
        mutableStateOf(
            when (mode) {
                PinSetupMode.CREATE -> PinStage.ENTER_NEW
                PinSetupMode.CHANGE, PinSetupMode.DISABLE -> PinStage.VERIFY_CURRENT
            }
        )
    }
    var enteredPin by remember { mutableStateOf("") }
    var draftNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val (title, subtitle) = when (stage) {
        PinStage.VERIFY_CURRENT -> {
            if (mode == PinSetupMode.DISABLE) {
                "PIN Kilidini Kaldır" to "Kilidi kapatmak için mevcut 4 haneli PIN'ini gir"
            } else {
                "Mevcut PIN'i Doğrula" to "Devam etmek için mevcut 4 haneli PIN'ini gir"
            }
        }
        PinStage.ENTER_NEW -> "Yeni PIN Oluştur" to "Uygulama kilidi için 4 haneli rakam belirle"
        PinStage.CONFIRM_NEW -> "Yeni PIN'i Onayla" to "Belirlediğin 4 haneli PIN'i tekrar gir"
    }

    fun onDigitPressed(digit: Int) {
        if (isProcessing || enteredPin.length >= 4) return
        errorMessage = null
        val next = enteredPin + digit.toString()
        enteredPin = next

        if (next.length < 4) return

        when (stage) {
            PinStage.VERIFY_CURRENT -> {
                isProcessing = true
                scope.launch {
                    val ok = withContext(Dispatchers.Default) {
                        PinHasher.verifyPin(next, currentPinSalt, currentPinHash)
                    }
                    isProcessing = false
                    enteredPin = ""
                    if (!ok) {
                        errorMessage = "Mevcut PIN hatalı"
                    } else if (mode == PinSetupMode.DISABLE) {
                        onPinDisabled()
                    } else {
                        stage = PinStage.ENTER_NEW
                    }
                }
            }
            PinStage.ENTER_NEW -> {
                draftNewPin = next
                enteredPin = ""
                stage = PinStage.CONFIRM_NEW
            }
            PinStage.CONFIRM_NEW -> {
                if (next == draftNewPin) {
                    onPinConfigured(next)
                } else {
                    enteredPin = ""
                    errorMessage = "PIN'ler eşleşmiyor, tekrar dene"
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = kesemColors.subtleText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            PinDotsIndicator(
                filledCount = enteredPin.length,
                isError = errorMessage != null
            )

            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.height(20.dp)
            )

            PinKeypad(
                enabled = !isProcessing,
                showBiometric = false,
                onDigit = ::onDigitPressed,
                onBackspace = {
                    if (enteredPin.isNotEmpty() && !isProcessing) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onBiometricClick = {}
            )

            if (stage == PinStage.CONFIRM_NEW) {
                TextButton(
                    onClick = {
                        enteredPin = ""
                        draftNewPin = ""
                        errorMessage = null
                        stage = PinStage.ENTER_NEW
                    }
                ) {
                    Text("Baştan Belirle")
                }
            }
        }
    }
}
