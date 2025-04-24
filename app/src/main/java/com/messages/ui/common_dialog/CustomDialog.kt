package com.messages.ui.common_dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.view.Window
import android.view.WindowManager
import androidx.core.view.isVisible
import com.messages.R
import com.messages.databinding.CustomDialogBinding
import com.messages.extentions.getStringValue
import com.messages.utils.setOnSafeClickListener

class CustomDialog(
    context: Context,
    private val dialogTitle: String = "",
    private val dialogDisc: String = "",
    private val negativeBtnText: String = context.getStringValue(R.string.cancel),
    private val positiveBtnText: String = context.getStringValue(R.string.done),
    private val isShowSingleBtn: Boolean = false,
    private val onDialogCancel: () -> Unit = {},
    private val onNegative: () -> Unit = {},
    private val onPositive: () -> Unit = {}
) : Dialog(context, R.style.CustomDialog) {

    private val dialogBinding: CustomDialogBinding = CustomDialogBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.gravity = Gravity.BOTTOM
            setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                dimAmount = 0.6f
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
        }
        setContentView(dialogBinding.root)
        initView()
    }

    private fun initView() {
        dialogBinding.apply {
            tvDialogTitle.text = dialogTitle
            tvDialogDisc.text = dialogDisc

            btnNegative.text = negativeBtnText
            btnPositive.text = positiveBtnText

            btnNegative.isVisible = !isShowSingleBtn

            btnNegative.setOnSafeClickListener {
                onNegative.invoke()
                dismiss()
            }

            btnPositive.setOnSafeClickListener {
                onPositive.invoke()
                dismiss()
            }
        }

        setOnCancelListener { onDialogCancel.invoke() }
    }
}