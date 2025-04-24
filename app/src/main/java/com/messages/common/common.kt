package com.messages.common

import androidx.core.text.isDigitsOnly
import com.messages.extentions.isNumeric
import com.messages.extentions.trimToComparableNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.regex.Pattern


var backgroundScope = CoroutineScope(Dispatchers.IO)

var mainScope = CoroutineScope(Dispatchers.Main)


const val transactionRegex =
    "\\b(?:credited|debited|balance|transaction|txn|transferred|sent|received|withdrawn|paid|deposited)\\s*(?:of|with|for)?\\s*([₹\$€£¥₱₦₹₭₮₩₲₵₽]|INR|USD|GBP|EUR|JPY|CNY|SGD|AUD)?\\s*\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?\\b"
const val otpRegex =
    "(?i)(?:otp|verification|use|password|one-time|auth|authentication|pin).{0,20}(\\b[a-zA-Z0-9-]{4,6}\\b)"
const val offerRegex =
    "(?i)\\b(offer|discount|sale|deal|promo|promotion|cashback|limited time|special price|buy one get one|coupon|voucher|redeem|save|exclusive)\\b"

fun getMessageType(address: String, message: String): Int {
    return if (address.trimToComparableNumber().isDigitsOnly() && address.length > 8) {
        0
    } else {
        if (!address.isNumeric()) {
            if (message.isOfferType()) {
                4
            } else if (message.isOtpType()) {
                3
            } else if (message.isTransactionType()) {
                2
            } else {
                4
            }
        } else {
            if (message.isOtpType()) {
                3
            } else if (message.isTransactionType()) {
                2
            } else {
                0
            }
        }
    }
}

fun String.isOtpType(): Boolean {
    val pattern = Pattern.compile(otpRegex)
    val matcher = pattern.matcher(this.lowercase())
    return matcher.find()
}

fun String.isOfferType(): Boolean {
    val pattern = Pattern.compile(offerRegex)
    val matcher = pattern.matcher(this.lowercase())
    return matcher.find()
}

fun String.isTransactionType(): Boolean {
    val pattern = Pattern.compile(transactionRegex)
    val matcher = pattern.matcher(this.lowercase())
    return matcher.find()
}

fun String.getOtp(): String {
    val pattern = Pattern.compile(otpRegex)
    val matcher = pattern.matcher(this.lowercase())
    if (matcher.find()) {
        return matcher.group(1) ?: ""
    }
    return ""
}

