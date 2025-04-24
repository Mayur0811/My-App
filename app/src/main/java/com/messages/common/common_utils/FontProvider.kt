package com.messages.common.common_utils

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.messages.R
import com.messages.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontProvider @Inject constructor(context: Context) {

    private var appTypeface: Typeface? = null
    private val pendingCallbacks = ArrayList<(Typeface) -> Unit>()

    init {
        ResourcesCompat.getFont(context, R.font.helvetica_now_display_regular, object : ResourcesCompat.FontCallback() {
            override fun onFontRetrievalFailed(reason: Int) {
                AppLogger.w(message = "Font retrieval failed: $reason")
            }

            override fun onFontRetrieved(typeface: Typeface) {
                appTypeface = typeface

                pendingCallbacks.forEach { appTypeface?.run(it) }
                pendingCallbacks.clear()
            }
        }, null)
    }

    fun getTypeface(callback: (Typeface) -> Unit) {
        appTypeface?.run(callback) ?: pendingCallbacks.add(callback)
    }

}