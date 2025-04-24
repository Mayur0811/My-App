package com.messages.utils

import android.content.Context
import android.net.Uri
import com.messages.common.backgroundScope
import ezvcard.Ezvcard
import ezvcard.VCard
import kotlinx.coroutines.launch

fun parseVCardFromUri(context: Context, uri: Uri?, callback: (vCards: List<VCard>) -> Unit) {
    backgroundScope.launch {
        if (uri == null) {
            return@launch
        }
        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            callback(emptyList())
            return@launch
        }
        val vCards = Ezvcard.parse(inputStream).all()
        callback(vCards)
    }
}

fun VCard?.parseNameFromVCard(): String? {
    if (this == null) return null
    var fullName = formattedName?.value
    if (fullName.isNullOrEmpty()) {
        val structured = structuredName ?: return null
        val nameComponents = arrayListOf<String?>().apply {
            addAll(structured.prefixes)
            add(structured.given)
            addAll(structured.additionalNames)
            add(structured.family)
            addAll(structured.suffixes)
        }
        fullName = nameComponents.filter { !it.isNullOrEmpty() }.joinToString(separator = " ")
    }
    return fullName
}
