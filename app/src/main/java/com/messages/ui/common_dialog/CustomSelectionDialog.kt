package com.messages.ui.common_dialog

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.messages.R
import com.messages.data.models.SelectionModel
import com.messages.databinding.CustomSelectionDialogBinding
import com.messages.extentions.onGlobalLayout
import com.messages.extentions.setupDialogStuff

class CustomSelectionDialog(
    val activity: Activity,
    val isShowTitle: Boolean = false,
    val items: ArrayList<SelectionModel>,
    val title: String = "",
    val checkedItemId: Int = -1,
    val titleId: Int = 0,
    showOKButton: Boolean = false,
    val cancelCallback: (() -> Unit)? = null,
    val callback: (newValue: Any) -> Unit,
) {
    private val dialog: AlertDialog
    private var wasInit = false
    private var selectedItemId = -1

    init {
        val binding = CustomSelectionDialogBinding.inflate(activity.layoutInflater)
        if (isShowTitle) {
            binding.tvRadioTitleTitle.text = title
            binding.tvRadioTitleTitle.isVisible = true
        } else {
            binding.tvRadioTitleTitle.isVisible = false
        }
        val buttonColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(
                ContextCompat.getColor(activity, R.color.app_color),
                ContextCompat.getColor(activity, R.color.app_gray)
            )
        )
        binding.radioGroup.apply {
            for (i in 0 until items.size) {
                val radioButton = (activity.layoutInflater.inflate(
                    R.layout.custom_dialog_radio_btn,
                    null
                ) as RadioButton).apply {
                    text = items[i].title
                    isChecked = items[i].id == checkedItemId
                    id = i
                    buttonTintList = buttonColorStateList
                    setOnClickListener { itemSelected(i) }
                }

                if (items[i].id == checkedItemId) {
                    selectedItemId = i
                }

                addView(
                    radioButton,
                    RadioGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        val builder = AlertDialog.Builder(activity)
            .setOnCancelListener { cancelCallback?.invoke() }

        if (selectedItemId != -1 && showOKButton) {
            builder.setPositiveButton(R.string.ok) { dialog, which ->
                itemSelected(selectedItemId)
            }
        }

        dialog = builder.create().apply {
            activity.setupDialogStuff(binding.root, this, titleId)
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (selectedItemId != -1) {
            binding.radioGroupScrollParent.apply {
                onGlobalLayout {
                    scrollY = binding.radioGroup.findViewById<View>(selectedItemId).bottom - height
                }
            }
        }

        wasInit = true
    }

    private fun itemSelected(checkedId: Int) {
        if (wasInit) {
            callback(items[checkedId].value)
            dialog.dismiss()
        }
    }

    companion object {
        const val FONT_SIZE = 1
    }

}