package com.messages.common

import android.content.Context
import android.os.Handler
import android.provider.Settings

object CallEndUtils {
    private var elapsedTime: Long = 0
    private var handler: Handler? = null
    private var isRinging = false
    private var startTime: Long = 0
    private var timerRunning = false
    private var phoneNumber = ""

    @JvmField
    val Companion: CallEndCompanion = CallEndCompanion()
    private var formattedTime = ""

    fun isOverlayAllowed(context: Context?): Boolean {
        return Settings.canDrawOverlays(context)
    }

    class CallEndCompanion {

        fun setPhoneNumber(phoneNumber: String) {
            CallEndUtils.phoneNumber = phoneNumber
        }

        fun getPhoneNumber(): String {
            return phoneNumber
        }

        fun getElapsedTime(): Long {
            return elapsedTime
        }

        fun getFormattedTime(): String {
            return formattedTime
        }

        fun getHandler(): Handler? {
            return handler
        }

        fun getStartTime(): Long {
            return startTime
        }

        fun getTimerRunning(): Boolean {
            return timerRunning
        }

        fun isRinging(): Boolean {
            return isRinging
        }

        fun setElapsedTime(elapsedTime: Long) {
            CallEndUtils.elapsedTime = elapsedTime
        }

        fun setFormattedTime(formattedTime: String) {
            CallEndUtils.formattedTime = formattedTime
        }

        fun setHandler(handler: Handler?) {
            CallEndUtils.handler = handler
        }

        fun setRinging(isRinging: Boolean) {
            CallEndUtils.isRinging = isRinging
        }

        fun setStartTime(startTime: Long) {
            CallEndUtils.startTime = startTime
        }

        fun setTimerRunning(timerRunning: Boolean) {
            CallEndUtils.timerRunning = timerRunning
        }
    }
}