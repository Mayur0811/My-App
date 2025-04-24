package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class BlockedNumberModel(
    val id: Long,
    val number: String,
    val normalizedNumber: String,
    val numberToCompare: String
)