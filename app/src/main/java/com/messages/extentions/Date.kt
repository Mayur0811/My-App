package com.messages.extentions

import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Calendar
import java.util.Locale

fun Long.formatDateOrTime(
    isUse24: Boolean,
    hideTimeAtOtherDays: Boolean = true,
    showYearEvenIfCurrent: Boolean = false
): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000

    return if (DateUtils.isToday(this * 1000)) {
        DateFormat.format(if (isUse24) "HH:mm" else "hh:mm a", cal).toString()
    } else {
        var format = "MMMM dd, yyyy"
        if (!showYearEvenIfCurrent && isThisYear()) {
            format = format.replace(", yyyy", "").trim().trim('-').trim('.').trim('/')
        }

        if (!hideTimeAtOtherDays) {
            format += if (isUse24) ", HH:mm" else ", hh:mm a"
        }

        DateFormat.format(format, cal).toString()
    }
}

fun Long.isThisYear(): Boolean {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.timeInMillis = this * 1000
    val thenYear = calendar.get(Calendar.YEAR)
    return (thenYear == currentYear)
}

fun Long.formatMessageTime(isUse24: Boolean): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format(if (isUse24) "HH:mm" else "hh:mm aa", cal).toString()
}

fun Long.formatMessageDate(): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format("MMMM dd, yyyy", cal).toString()
}

fun Long.formatMessageFullDate(isUse24: Boolean): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format(if (isUse24) "MMMM dd, yyyy  HH:mm" else "MMMM dd, yyyy  hh:mm a", cal).toString()
}

fun Long.dateFormatDMY(): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format("dd/MM/yyyy", cal).toString()
}

fun Long.dateFormatMY(): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format("M/yyyy", cal).toString()
}

fun Long.dateFormatY(): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this * 1000
    return DateFormat.format("yyyy", cal).toString()
}

fun Long.formatDateLastBackup(isUse24: Boolean): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this
    return DateFormat.format(if (isUse24) "dd/MM/yyyy, HH:mm:ss" else "dd/MM/yyyy, hh:mm:ss aa", cal).toString()
}


