package com.messages.common.custom_views

import android.content.Context
import android.util.AttributeSet
import androidx.emoji2.widget.EmojiTextView
import com.messages.common.common_utils.TextViewStyler


class MessageTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : EmojiTextView(context, attrs) {


    private var collapseEnabled: Boolean = false

    init {
        if (!isInEditMode) {
            TextViewStyler(context).applyAttributes(this, attrs)
        } else {
            TextViewStyler.applyEditModeAttributes(this, attrs)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (collapseEnabled) {
            layout
                ?.takeIf { layout -> layout.lineCount > 0 }
                ?.let { layout -> layout.getEllipsisCount(layout.lineCount - 1) }
                ?.takeIf { ellipsisCount -> ellipsisCount > 0 }
                ?.let { ellipsisCount -> text.dropLast(ellipsisCount).lastIndexOf(',') }
                ?.takeIf { lastComma -> lastComma >= 0 }
                ?.let { lastComma ->
                    val remainingNames = text.drop(lastComma).count { c -> c == ',' }
                    text = "${text.take(lastComma)}, +$remainingNames"
                }
        }
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        setLinkTextColor(color)
    }

    fun updateTextSize(textSize: Int) {
        TextViewStyler(context).setTextSize(this, textSize)
    }

}