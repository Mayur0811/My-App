package com.messages.extentions

import android.telephony.PhoneNumberUtils
import android.util.Base64
import com.messages.BuildConfig
import com.messages.utils.AppLogger
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


fun String.normalizeString() = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

fun String.trimToComparableNumber(): String {
    val normalizedNumber = this.normalizeString()
    val startIndex = 0.coerceAtLeast(normalizedNumber.length - 9)
    return normalizedNumber.substring(startIndex)
}


fun String.normalizePhoneNumber(): String {
    val phoneNumber = if (this.startsWith("0")) {
        this.drop(0)
    } else {
        this
    }
    return PhoneNumberUtils.normalizeNumber(phoneNumber)
}

fun String.isNumeric(): Boolean = this
    .removePrefix("-")
    .removePrefix("+")
    .all { it in '0'..'9' }


fun String.encrypt(): String? {
    try {
        val keyBytes =
            MessageDigest.getInstance("SHA-256").digest(BuildConfig.SECRET_KEY.toByteArray())
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encrypted = cipher.doFinal(this.toByteArray())
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    } catch (ex: Exception) {
        ex.printStackTrace()
        AppLogger.e("1234", "encrypt -> ${ex.message}")
    }
    return null
}

fun String.decrypt(): String? {
    try {
        val keyBytes =
            MessageDigest.getInstance("SHA-256").digest(BuildConfig.SECRET_KEY.toByteArray())
        val keySpec =
            SecretKeySpec(keyBytes, "AES")

        val combined = Base64.decode(this, Base64.DEFAULT)

        val iv = combined.copyOfRange(0, 16)
        val encryptedBytes = combined.copyOfRange(16, combined.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val original = cipher.doFinal(encryptedBytes)
        return String(original)
    } catch (ex: Exception) {
        ex.printStackTrace()
        AppLogger.e("1234", "decrypt -> ${ex.message}")
    }

    return null
}
