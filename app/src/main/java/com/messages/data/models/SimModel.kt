package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class SimModel(val id: Int, val subscriptionId: Int, val label: String) {
}