package io.github.dogukantufan00.kesem.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReceiptViewerDialog(
    receiptFile: File,
    onDismiss: () -> Unit
) {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, receiptFile.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(receiptFile.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Fiş fotoğrafı",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (receiptFile.exists()) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text(
                                text = "Fiş fotoğrafı bulunamadı",
                                color = Color.White
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
