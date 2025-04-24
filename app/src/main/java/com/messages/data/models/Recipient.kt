package com.messages.data.models

import android.telephony.PhoneNumberUtils
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Keep
@Entity(tableName = "recipient", indices = [(Index(value = ["id"], unique = true))])
data class Recipient(
    @ColumnInfo(name = "recipientId") var recipientId: Long = 0,
    @ColumnInfo(name = "address") var address: String = "",
    @ColumnInfo(name = "contact") var contact: Contact? = null,
    @ColumnInfo(name = "lastUpdate") var lastUpdate: Long = 0L
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    fun getDisplayName(): String = contact?.name?.takeIf { it.isNotBlank() }
        ?: PhoneNumberUtils.formatNumber(address, Locale.getDefault().country) ?: address

}
