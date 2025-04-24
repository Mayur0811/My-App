package com.messages.data.models

import androidx.annotation.Keep

@Keep
open class PhoneNumber(
    var id: Long = 0,
    var accountType: String? = "",
    var address: String = "",
    var normalizeAddress: String = "",
    var type: String = "",
    var isDefault: Boolean = false
)

