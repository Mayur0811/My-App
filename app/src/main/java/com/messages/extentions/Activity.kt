package com.messages.extentions

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.messages.R
import com.messages.databinding.AppDTitleBinding

fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: (() -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    var binding: AppDTitleBinding? = null
    if (titleId != 0 || titleText.isNotEmpty()) {
        binding = AppDTitleBinding.inflate(layoutInflater)
        binding.dialogTitleTextview.apply {
            if (titleText.isNotEmpty()) {
                text = titleText
            } else {
                setText(titleId)
            }
        }
    }

    // if we use the same primary and background color, use the text color for dialog confirmation buttons
    val dialogButtonColor = view.context.getColorForId(R.color.app_color)

    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(binding?.root)
        setCanceledOnTouchOutside(cancelOnTouchOutside)
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

        val bgDrawable =
            getColoredDrawableWithColor(R.drawable.apk_background_dialog, Color.WHITE)
        window?.setBackgroundDrawable(bgDrawable)
    }
    callback?.invoke()
}
