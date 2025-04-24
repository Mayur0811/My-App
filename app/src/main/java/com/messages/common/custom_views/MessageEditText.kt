package com.messages.common.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.messages.common.common_utils.TextViewStyler
import com.messages.extentions.tryOrNull

class MessageEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatEditText(context, attrs) {


    var onBackspaceListener: (() -> Unit)? = null
    var onInputContentSelectedListener: ((InputContentInfoCompat) -> Unit)? = null
    var supportsInputContent: Boolean = false

    init {
        if (!isInEditMode) {
            TextViewStyler(context).applyAttributes(this, attrs)
        } else {
            TextViewStyler.applyEditModeAttributes(this, attrs)
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {

        val inputConnection =
            object : InputConnectionWrapper(super.onCreateInputConnection(editorInfo), true) {
                override fun sendKeyEvent(event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                        onBackspaceListener?.invoke()
                    }
                    return super.sendKeyEvent(event)
                }


                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                    if (beforeLength == 1 && afterLength == 0) {
                        onBackspaceListener?.invoke()
                    }
                    return super.deleteSurroundingText(beforeLength, afterLength)
                }
            }

        if (supportsInputContent) {
            EditorInfoCompat.setContentMimeTypes(
                editorInfo, arrayOf(
                    "image/jpeg",
                    "image/jpg",
                    "image/png",
                    "image/gif"
                )
            )
        }

        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
                val grantReadPermission =
                    flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0

                if (grantReadPermission) {
                    return@OnCommitContentListener tryOrNull {
                        inputContentInfo.requestPermission()
                        onInputContentSelectedListener?.invoke(inputContentInfo)
                        true
                    } ?: false
                }
                true
            }


        return InputConnectionCompat.createWrapper(inputConnection, editorInfo, callback)
    }

}