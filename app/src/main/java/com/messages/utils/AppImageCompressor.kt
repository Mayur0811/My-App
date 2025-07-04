package com.messages.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.messages.common.backgroundScope
import com.messages.data.models.isImageMimeType
import com.messages.extentions.getFileSizeFromUri
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


class AppImageCompressor(private val context: Context) {
    private val contentResolver = context.contentResolver
    private val outputDirectory = File(context.cacheDir, "compressed").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun compressImage(uri: Uri, compressSize: Long, callback: (compressedFileUri: Uri?) -> Unit) {
        backgroundScope.launch {
            try {
                val fileSize = context.getFileSizeFromUri(uri)
                if (fileSize > compressSize) {
                    val mimeType = contentResolver.getType(uri)!!
                    if (mimeType.isImageMimeType()) {
                        val byteArray = contentResolver.openInputStream(uri)?.readBytes()!!
                        var destinationFile = File(
                            outputDirectory,
                            System.currentTimeMillis().toString()
                                .plus(mimeType.getExtensionFromMimeType())
                        )
                        destinationFile.writeBytes(byteArray)
                        val constraint = SizeConstraint(compressSize)
                        while (constraint.isSatisfied(destinationFile).not()) {
                            destinationFile = constraint.satisfy(destinationFile)
                        }
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            destinationFile
                        )
                        callback.invoke(fileUri)
                    } else {
                        callback.invoke(null)
                    }
                } else {
                    callback.invoke(uri)
                }
            } catch (e: Exception) {
                callback.invoke(null)
            }
        }
    }

    private fun overWrite(
        imageFile: File,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = imageFile.path.getCompressionFormat(),
        quality: Int = 100
    ): File {
        val result = if (format == imageFile.path.getCompressionFormat()) {
            imageFile
        } else {
            File("${imageFile.absolutePath.substringBeforeLast(".")}.${format.extension()}")
        }
        imageFile.delete()
        saveBitmap(bitmap, result, format, quality)
        return result
    }

    private fun saveBitmap(
        bitmap: Bitmap,
        destination: File,
        format: Bitmap.CompressFormat = destination.path.getCompressionFormat(),
        quality: Int = 100
    ) {
        destination.parentFile?.mkdirs()
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(destination.absolutePath)
            bitmap.compress(format, quality, fileOutputStream)
        } finally {
            fileOutputStream?.run {
                flush()
                close()
            }
        }
    }

    private fun loadBitmap(imageFile: File) = BitmapFactory.decodeFile(imageFile.absolutePath).run {
        determineImageRotation(imageFile, this)
    }

    private fun determineImageRotation(imageFile: File, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        when (orientation) {
            6 -> matrix.postRotate(90f)
            3 -> matrix.postRotate(180f)
            8 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private inner class SizeConstraint(
        private val maxFileSize: Long,
        private val stepSize: Int = 10,
        private val maxIteration: Int = 10,
        private val minQuality: Int = 10
    ) {
        private var iteration: Int = 0

        fun isSatisfied(imageFile: File): Boolean {
            return imageFile.length() <= maxFileSize || iteration >= maxIteration
        }

        fun satisfy(imageFile: File): File {
            iteration++
            val quality = (100 - iteration * stepSize).takeIf { it >= minQuality } ?: minQuality
            return overWrite(imageFile, loadBitmap(imageFile), quality = quality)
        }
    }

    private fun String.getExtensionFromMimeType(): String {
        return when (lowercase()) {
            "image/png" -> ".png"
            "image/apng" -> ".apng"
            "image/webp" -> ".webp"
            "image/svg+xml" -> ".svg"
            "image/gif" -> ".gif"
            else -> ".jpg"
        }
    }

    private fun String.getCompressionFormat() = when (substring(lastIndexOf(".") + 1).lowercase()) {
        "png" -> Bitmap.CompressFormat.PNG
        else -> Bitmap.CompressFormat.JPEG
    }

    private fun Bitmap.CompressFormat.extension() = when (this) {
        Bitmap.CompressFormat.PNG -> "png"
        else -> "jpg"
    }

}
