package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class SelectionModel(val id: Int, val title: String, val value: Any = id)