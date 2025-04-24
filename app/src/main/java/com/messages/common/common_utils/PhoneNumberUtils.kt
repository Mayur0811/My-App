package com.messages.common.common_utils

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.core.text.isDigitsOnly
import com.messages.extentions.tryOrNull
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import java.util.Locale


class PhoneNumberUtils(context: Context) {

    private val countryCode = Locale.getDefault().displayCountry
    private val phoneNumberUtil = PhoneNumberUtil.createInstance(context)

    /**
     * Android's implementation is too loose and causes false positives
     * libphonenumber is stricter but too slow
     *
     * This method will run successfully stricter checks without compromising much speed
     */
    fun compare(first: String, second: String): Boolean {
        if (first.equals(second, true)) {
            return true
        }

        if (PhoneNumberUtils.compare(first, second)) {
            val matchType = phoneNumberUtil.isNumberMatch(first, second)
            if (matchType >= PhoneNumberUtil.MatchType.SHORT_NSN_MATCH) {
                return true
            }
        }

        return false
    }

    fun isPossibleNumber(number: CharSequence): Boolean {
        return number.isDigitsOnly() && number.length > 7
    }

    fun isReallyDialable(digit: Char): Boolean {
        return PhoneNumberUtils.isReallyDialable(digit)
    }

    fun isWellFormedSmsAddress(number: String): Boolean {
        return PhoneNumberUtils.isWellFormedSmsAddress(number)
    }

    fun formatNumber(number: CharSequence): String {
        // PhoneNumberUtil doesn't maintain country code input
        return PhoneNumberUtils.formatNumber(number.toString(), countryCode) ?: number.toString()
    }

    fun normalizeNumber(number: String): String {
        return PhoneNumberUtils.stripSeparators(number)
    }

    private fun parse(number: CharSequence): Phonenumber.PhoneNumber? {
        return tryOrNull(true) { phoneNumberUtil.parse(number, countryCode) }
    }

}