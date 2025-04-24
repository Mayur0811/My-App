package com.messages.data.database.type_converters

import android.net.Uri
import androidx.annotation.Keep
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.messages.data.models.AttachmentWithMessageModel
import com.messages.data.models.Contact
import com.messages.data.models.PhoneNumber
import com.messages.data.models.Recipient
import java.lang.reflect.Type


@Keep
class AppTypeConverters {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Uri::class.java, UriTypeAdapter())
        .create()

    @TypeConverter
    fun jsonToContact(value: String) = gson.fromJson<Contact>(
        value,
        object : TypeToken<Contact>() {}.type
    )

    @TypeConverter
    fun contactToJson(contact: Contact) = gson.toJson(contact)



    @TypeConverter
    fun jsonToPhoneNumberList(value: String) = gson.fromJson<List<PhoneNumber>>(
        value,
        object : TypeToken<List<PhoneNumber>>() {}.type
    )

    @TypeConverter
    fun PhoneNumberListToJson(list: List<PhoneNumber>) = gson.toJson(list)



    @TypeConverter
    fun jsonToRecipient(value: String) = gson.fromJson<List<Recipient>>(
        value,
        object : TypeToken<List<Recipient>>() {}.type
    )

    @TypeConverter
    fun recipientToJson(list: List<Recipient>) = gson.toJson(list)




    @TypeConverter
    fun jsonToStringList(value: String) = gson.fromJson<List<String>>(
        value,
        object : TypeToken<List<String>>() {}.type
    )

    @TypeConverter
    fun stringListToJson(list: List<String>) = gson.toJson(list)



    @TypeConverter
    fun jsonToMessageAttachment(value: String) = gson.fromJson<AttachmentWithMessageModel>(
        value,
        object : TypeToken<AttachmentWithMessageModel>() {}.type
    )

    @TypeConverter
    fun messageAttachmentToJson(messageAttachment: AttachmentWithMessageModel?) =
        gson.toJson(messageAttachment)
}


class UriTypeAdapter : JsonSerializer<Uri?>,
    JsonDeserializer<Uri?> {
    override fun serialize(
        src: Uri?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.toString())
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Uri {
        return Uri.parse(json.asString)
    }
}