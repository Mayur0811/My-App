package com.messages.common.common_utils

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.messages.R
import com.messages.common.common_utils.TextViewStyler.Companion.SIZE_PRIMARY
import com.messages.common.common_utils.TextViewStyler.Companion.SIZE_SECONDARY
import com.messages.common.common_utils.TextViewStyler.Companion.SIZE_TERTIARY
import com.messages.common.common_utils.TextViewStyler.Companion.SIZE_TOOLBAR
import com.messages.common.custom_views.MessageEditText
import com.messages.common.custom_views.MessageTextView
import com.messages.data.pref.AppPreferences

class TextViewStyler(private val context: Context) {

    private var textSizeAttr: Int = 0

    companion object {

        const val SIZE_PRIMARY = 0
        const val SIZE_SECONDARY = 1
        const val SIZE_TERTIARY = 2
        const val SIZE_TOOLBAR = 3
        const val SIZE_DIALOG = 4
        const val SIZE_EMOJI = 5
        const val SIZE_DIAL = 6

        fun applyEditModeAttributes(textView: TextView, attrs: AttributeSet?) {
            textView.run {
                var textSizeAttr: Int

                when (this) {
                    is MessageTextView -> context.obtainStyledAttributes(
                        attrs,
                        R.styleable.MessageTextView
                    ).run {
                        textSizeAttr = getInt(R.styleable.MessageTextView_textSize, -1)
                        recycle()
                    }

                    is MessageEditText -> context.obtainStyledAttributes(
                        attrs,
                        R.styleable.MessageEditText
                    ).run {
                        textSizeAttr = getInt(R.styleable.MessageEditText_textSize, -1)
                        recycle()
                    }

                    else -> return
                }

                textSize = when (textSizeAttr) {
                    SIZE_PRIMARY -> 16f
                    SIZE_SECONDARY -> 14f
                    SIZE_TERTIARY -> 12f
                    SIZE_TOOLBAR -> 20f
                    SIZE_DIALOG -> 18f
                    SIZE_EMOJI -> 32f
                    SIZE_DIAL -> 24f
                    else -> textSize / paint.density
                }
            }
        }
    }

    fun applyAttributes(textView: TextView, attrs: AttributeSet?) {
        val appPreferences = AppPreferences(context)

        if (!appPreferences.systemFont) {
            FontProvider(context).getTypeface { typeface ->
                textView.setTypeface(typeface, textView.typeface?.style ?: Typeface.NORMAL)
            }
        }

        when (textView) {
            is MessageTextView -> textView.context.obtainStyledAttributes(
                attrs,
                R.styleable.MessageTextView
            ).run {
                textSizeAttr = getInt(R.styleable.MessageTextView_textSize, -1)
                recycle()
            }

            is MessageEditText -> textView.context.obtainStyledAttributes(
                attrs,
                R.styleable.MessageEditText
            ).run {
                textSizeAttr = getInt(R.styleable.MessageEditText_textSize, -1)
                recycle()
            }

            else -> return
        }

        setTextSize(textView, appPreferences.textSize)

        if (textView is EditText) {
            val drawable = ContextCompat.getDrawable(textView.context, R.drawable.cursor)
            /*  ?.apply { setTint(*//*colors.theme().theme*//*  textView.context.getColor(R.color.app_color)) }*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                textView.textCursorDrawable = drawable
            }
        }
    }

    /**
     * @see SIZE_PRIMARY
     * @see SIZE_SECONDARY
     * @see SIZE_TERTIARY
     * @see SIZE_TOOLBAR
     */
    fun setTextSize(textView: TextView, textSizePref: Int) {
        when (textSizeAttr) {
            SIZE_PRIMARY -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 14f
                AppPreferences.TEXT_SIZE_NORMAL -> 16f
                AppPreferences.TEXT_SIZE_LARGE -> 18f
                AppPreferences.TEXT_SIZE_LARGER -> 20f
                AppPreferences.TEXT_SIZE_SUPER -> 40f
                else -> 16f
            }

            SIZE_SECONDARY -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 12f
                AppPreferences.TEXT_SIZE_NORMAL -> 14f
                AppPreferences.TEXT_SIZE_LARGE -> 16f
                AppPreferences.TEXT_SIZE_LARGER -> 18f
                AppPreferences.TEXT_SIZE_SUPER -> 36f
                else -> 14f
            }

            SIZE_TERTIARY -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 10f
                AppPreferences.TEXT_SIZE_NORMAL -> 12f
                AppPreferences.TEXT_SIZE_LARGE -> 14f
                AppPreferences.TEXT_SIZE_LARGER -> 16f
                AppPreferences.TEXT_SIZE_SUPER -> 32f
                else -> 12f
            }

            SIZE_TOOLBAR -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 18f
                AppPreferences.TEXT_SIZE_NORMAL -> 20f
                AppPreferences.TEXT_SIZE_LARGE -> 22f
                AppPreferences.TEXT_SIZE_LARGER -> 24f
                AppPreferences.TEXT_SIZE_SUPER -> 52f
                else -> 20f
            }

            SIZE_DIALOG -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 16f
                AppPreferences.TEXT_SIZE_NORMAL -> 18f
                AppPreferences.TEXT_SIZE_LARGE -> 20f
                AppPreferences.TEXT_SIZE_LARGER -> 24f
                AppPreferences.TEXT_SIZE_SUPER -> 48f
                else -> 18f
            }

            SIZE_EMOJI -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 28f
                AppPreferences.TEXT_SIZE_NORMAL -> 32f
                AppPreferences.TEXT_SIZE_LARGE -> 34f
                AppPreferences.TEXT_SIZE_LARGER -> 40f
                AppPreferences.TEXT_SIZE_SUPER -> 80f
                else -> 32f
            }

            SIZE_DIAL -> textView.textSize = when (textSizePref) {
                AppPreferences.TEXT_SIZE_SMALL -> 22f
                AppPreferences.TEXT_SIZE_NORMAL -> 24f
                AppPreferences.TEXT_SIZE_LARGE -> 26f
                AppPreferences.TEXT_SIZE_LARGER -> 30f
                AppPreferences.TEXT_SIZE_SUPER -> 60f
                else -> 24f
            }
        }
    }

}