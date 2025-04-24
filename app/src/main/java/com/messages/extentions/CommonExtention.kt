package com.messages.extentions

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.DateFormat
import com.messages.data.models.Recipient
import com.messages.utils.AppLogger
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.random.Random


fun <T> tryOrNull(logOnError: Boolean = true, body: () -> T?): T? {
    return try {
        body()
    } catch (e: Exception) {
        if (logOnError) {
            AppLogger.w(message = e.message.toString())
        }

        null
    }
}

fun Long.formatSize(): String {
    if (this <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(toDouble()) / log10(1024.0)).toInt()
    return "${
        DecimalFormat("#,##0.#").format(
            this / (1024.0).pow(digitGroups.toDouble())
        )
    } ${units[digitGroups]}"
}

fun generateRandomId(length: Int = 9): Long {
    val millis = System.currentTimeMillis()
    val random = abs(Random(millis).nextLong())
    return random.toString().takeLast(length).toLong()
}


fun Intent.getUri(): Uri? {
    try {
        val uri = Uri.parse(this.getStringExtra("message_uri"))
        return if (uri.path == "") null else uri
    } catch (var4: java.lang.Exception) {
        return null
    }
}

fun getCurrentDate(): Int {
    return DateFormat.format("dd", System.currentTimeMillis()).toString().toInt()
}

fun canDialNumber(recipients: ArrayList<Recipient>?): Pair<Boolean, String> {
    if (recipients.isNullOrEmpty()) return Pair(false, "")
    if (recipients.first().address.length <= 7) return Pair(false, "")
    recipients.forEach { recipient ->
        for (c in recipient.address.toCharArray()) {
            if (!android.telephony.PhoneNumberUtils.isDialable(c)) {
                return Pair(false, "")
            }
        }
    }
    return Pair(true, recipients.first().address)
}

fun isSameBitmap(firstImage: String, secondImage: String): Boolean {
    try {
        val firstBitmap = BitmapFactory.decodeFile(firstImage)
        val secondBitmap = BitmapFactory.decodeFile(secondImage)
        return firstBitmap.getBitmapHash() == secondBitmap.getBitmapHash()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        return false
    }
}

fun Bitmap.getBitmapHash(): String? {
    try {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()

        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(byteArray)
        val sb = java.lang.StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        return null
    }
}

