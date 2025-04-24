package com.messages.data.events

import com.messages.data.models.Conversation
import com.messages.data.models.Message


sealed class MessageEvent

data class ApplyFilter(
    val filter: Int,
    val month: Int = 0,
    val year: Int = 0,
    val dateRange: Pair<Long, Long> = Pair(0, 0)
) : MessageEvent()

data class OnLanguageSelect(val code: String) : MessageEvent()

data class OnSelectClipboardData(val clipText: String) : MessageEvent()

data class OnPickedContact(val contactData: String) : MessageEvent()

data object OnChangeFontSize : MessageEvent()

data object RefreshMessages : MessageEvent()

data class RefreshConversation(val threadId: Long) : MessageEvent()

data object DataSyncDone : MessageEvent()

data object DataSyncStart : MessageEvent()

data object OnChangeSwipeAction : MessageEvent()

data object UpdateConversation : MessageEvent()

data class HandleConversation(
    val conversation: Conversation,
    val updateData: Pair<Boolean, Boolean> = Pair(false, false)
) : MessageEvent()

data class DeleteConversation(val threadId: Long) : MessageEvent()

data class UpdateLastSendMessageStatus(val messageId: Long,val type:Int,val status:Int) : MessageEvent()

data class OnReceiveMessage(val message: Message) : MessageEvent()

data class OnReceiveMessageConversation(val message: Message, var isFromChat: Boolean = false) :
    MessageEvent()

data object OnAddConversationToPrivate : MessageEvent()

data class RefreshScheduleMessage(val messageId: Long):MessageEvent()


