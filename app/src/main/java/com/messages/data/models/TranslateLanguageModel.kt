
package com.messages.data.models

import androidx.annotation.Keep
import java.util.Locale

@Keep
class TranslateLanguageModel(val code: String) : Comparable<TranslateLanguageModel> {

        val displayName: String
            get() = Locale(code).displayName

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }

            if (other !is TranslateLanguageModel) {
                return false
            }

            val otherLang = other as TranslateLanguageModel?
            return otherLang!!.code == code
        }

        override fun toString(): String {
            return "$code - $displayName"
        }

        override fun compareTo(other: TranslateLanguageModel): Int {
            return this.displayName.compareTo(other.displayName)
        }

        override fun hashCode(): Int {
            return code.hashCode()
        }
    }