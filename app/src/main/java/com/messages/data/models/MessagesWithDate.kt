package com.messages.data.models

import androidx.annotation.Keep

@Keep
sealed class MessagesWithDate(open val message: Message) {
    data class ReceivedMessage(override val message: Message) : MessagesWithDate(message)
    data class SendMessage(override val message: Message) : MessagesWithDate(message)
}