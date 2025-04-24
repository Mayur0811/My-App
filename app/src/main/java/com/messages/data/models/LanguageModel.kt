package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class LanguageModel(
    val code: String,
    val name: String,
    val countryName: String
)