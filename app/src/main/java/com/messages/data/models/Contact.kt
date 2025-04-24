package com.messages.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "contacts", indices = [(Index(value = ["lookupKey"], unique = true))])
data class Contact(
    @PrimaryKey @ColumnInfo(name = "lookupKey") val lookupKey: String,
    @ColumnInfo(name = "contactId") val contactId: Long,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "photoUri") var photoUri: String? = "",
    @ColumnInfo(name = "phoneNumbers") var phoneNumbers: MutableList<PhoneNumber> = arrayListOf(),
    @ColumnInfo(name = "lastUpdate") val lastUpdate: Long,
    @ColumnInfo(name = "isFavorite") val isFavorite: Boolean = false
) {
    @Ignore
    var isShowAlphabet: Boolean = false
}