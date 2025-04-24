package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class ConversationDetailsModel(
    var viewType: Int,
    val menuIcon: Int = 0,
    val menuTitle: String = "",
    val recipients: List<Recipient>? = null,
    val isArchived: Boolean = false
)