package com.messages.extentions

import android.content.res.ColorStateList
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import com.messages.R

fun View.setBackgroundTint(color: Int?) {
    backgroundTintList =
        when (color) {
            null -> {
                null
            }
            0 -> {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.app_bg))
            }
            else -> ColorStateList.valueOf(color)
        }
}


fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            callback()
        }
    })
}

fun View.getColorStateList(id: Int): ColorStateList {
    return ColorStateList.valueOf(ContextCompat.getColor(context, id))
}
