package io.github.dogukantufan00.kesem.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class ReceiptStorage(private val context: Context) {

    suspend fun saveFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val maxDim = 1400
            var sampleSize = 1
            while (
                bounds.outWidth / sampleSize > maxDim * 2 ||
                bounds.outHeight / sampleSize > maxDim * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val longestSide = max(decoded.width, decoded.height)
            val finalBitmap = if (longestSide > maxDim) {
                val scale = maxDim.toFloat() / longestSide.toFloat()
                val targetW = (decoded.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            } else {
                decoded
            }

            val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val fileName = "rcpt_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}.jpg"
            val outFile = File(receiptsDir, fileName)

            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 78, fos)
            }
            finalBitmap.recycle()

            "receipts/$fileName"
        }.getOrNull()
    }

    fun createTempCameraUri(): Pair<Uri, File> {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val tempFile = File(cameraDir, "tmp_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return uri to tempFile
    }

    fun resolveFile(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    fun deleteReceipt(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        runCatching {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) file.delete()
        }
    }
}
