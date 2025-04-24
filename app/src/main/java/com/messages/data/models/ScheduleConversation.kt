package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class ScheduleConversation(val conversation: Conversation, val message: Message)