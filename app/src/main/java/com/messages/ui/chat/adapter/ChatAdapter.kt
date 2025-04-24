package com.messages.ui.chat.adapter

import android.provider.Telephony.Sms
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.Message
import com.messages.data.models.MessagesWithDate
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemChatReceivedBinding
import com.messages.databinding.ItemChatSentBinding
import com.messages.extentions.formatDateOrTime
import com.messages.extentions.formatMessageTime
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.loadImage
import com.messages.extentions.setBackgroundTint
import com.messages.ui.base.BaseAdapter
import com.messages.utils.setOnSafeClickListener
import java.io.File


class ChatAdapter(
    private val appPreferences: AppPreferences,
    private var onClickTranslate: ((Message, Int) -> Unit),
    private var onItemClickMessage: (() -> Unit),
    private var onClickResend: (Message) -> Unit
) : BaseAdapter<RecyclerView.ViewHolder>() {

    var selectedMessage: ArrayList<Message> = arrayListOf()

    var messages: ArrayList<MessagesWithDate> = arrayListOf()

    private val msgReceived = 1
    private val megSend = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder: RecyclerView.ViewHolder = when (viewType) {
            msgReceived -> MessageReceivedHolder(
                ItemChatReceivedBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            megSend -> MessageSendHolder(ItemChatSentBinding.inflate(inflater, parent, false))
            else -> MessageSendHolder(ItemChatSentBinding.inflate(inflater, parent, false))
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is MessagesWithDate.ReceivedMessage -> {
                (holder as MessageReceivedHolder).bind(message.message, position)
            }

            is MessagesWithDate.SendMessage -> {
                (holder as MessageSendHolder).bind(message.message, position)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is MessagesWithDate.ReceivedMessage -> msgReceived
            is MessagesWithDate.SendMessage -> megSend
        }
    }

    fun addMessage(message: MessagesWithDate) {
        messages.add(message)
        notifyDataSetChanged()
    }

    fun addMessages(message: ArrayList<MessagesWithDate>) {
        messages.addAll(message)
        notifyItemInserted(message.size)
    }

    inner class MessageReceivedHolder(val binding: ItemChatReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message, position: Int) {
            binding.apply {
                clMessageTime.isVisible = message.isShowDate
                if (message.isShowDate) {
                    tvMsgTime.text = message.date.formatDateOrTime(appPreferences.use24Hr)
                }
                tvMessageTime.text = message.date.formatMessageTime(appPreferences.use24Hr)

                ivMessage.isVisible = false
                ivAttachFile.isVisible = false
                tvAttachFileName.isVisible = false
                if (message.isMMS) {
                    tvMessageText.isVisible = false
                    ivTranslate.isVisible = false
                    progressBar.isVisible = false
                    if (message.attachmentWithMessageModel?.attachments?.isNotEmpty() == true) {
                        val mimetype =
                            message.attachmentWithMessageModel?.attachments?.first()?.mimetype ?: ""
                        if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
                            ivMessage.loadImage(uri = message.attachmentWithMessageModel?.attachments?.first()?.uri)
                            ivMessage.isVisible = true
                        } else {
                            ivMessage.isVisible = false
                            ivAttachFile.isVisible = true
                            tvAttachFileName.isVisible = true
                            tvAttachFileName.text = File(
                                message.attachmentWithMessageModel?.attachments?.first()?.filename
                                    ?: ""
                            ).name
                        }
                    }
                    if (message.attachmentWithMessageModel?.text?.isNotEmpty() == true) {
                        tvMessageText.isVisible = true
                        ivTranslate.isVisible = true
                        tvMessageText.text =
                            if (message.isShowTranslateBody) message.translatedBody.trim() else message.attachmentWithMessageModel?.text?.trim()
                    }
                } else {
                    tvMessageText.isVisible = true
                    ivTranslate.isVisible = true
                    tvMessageText.text =
                        if (message.isShowTranslateBody) message.translatedBody.trim() else message.body.trim()

                    if (message.isLoading) {
                        ivTranslate.isInvisible = true
                        progressBar.isVisible = true
                    }
                    if (message.isShowTranslateBody) {
                        progressBar.isVisible = false
                        ivTranslate.isVisible = true
                    }
                }

                clMessageBg.setBackgroundTint(
                    if (isSelected(message.id))
                        root.context.getColorForId(R.color.selected_conversation_bg)
                    else if (appPreferences.chatBubbleBgReceived == 0) root.context.getColorForId(R.color.app_white) else appPreferences.chatBubbleBgReceived
                )

                progressBar.setOnSafeClickListener {
                    if (!toggleSelection(message.id, position)) {
                        ivTranslate.performClick()
                    }
                }
                ivTranslate.setOnSafeClickListener {
                    if (!toggleSelection(message.id, position)) {
                        onClickTranslate.invoke(message, position)
                    }
                }

                root.setOnSafeClickListener {
                    if (toggleSelection(message.id, position, false)) {
                        if (selectedMessage.contains(message)) {
                            selectedMessage.remove(message)
                        } else {
                            selectedMessage.add(message)
                        }
                    }
                    onItemClickMessage.invoke()
                }

                root.setOnLongClickListener {
                    toggleSelection(message.id, position)
                    if (selectedMessage.contains(message)) {
                        selectedMessage.remove(message)
                    } else {
                        selectedMessage.add(message)
                    }
                    onItemLongClick?.invoke()
                    return@setOnLongClickListener true
                }
            }
        }
    }

    inner class MessageSendHolder(val binding: ItemChatSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message, position: Int) {
            binding.apply {
                clMessageTime.isVisible = message.isShowDate
                if (message.isShowDate) {
                    tvMsgTime.text = message.date.formatDateOrTime(appPreferences.use24Hr)
                }
                ivSchedule.isVisible = message.isScheduled
                if (message.deliveryStatus == Sms.Sent.STATUS_PENDING) {
                    tvMessageTime.text = root.context.getStringValue(R.string.sending)
                    tvMessageTime.setTextColor(root.context.getColorForId(R.color.app_gray))
                } else {
                    if (message.type == Sms.MESSAGE_TYPE_FAILED || message.type == Sms.MESSAGE_TYPE_OUTBOX) {
                        tvMessageTime.text =
                            root.context.getStringValue(R.string.message_not_sent_touch_retry)
                        tvMessageTime.setTextColor(root.context.getColorForId(R.color.app_error))
                    } else {
                        tvMessageTime.text = message.date.formatMessageTime(appPreferences.use24Hr)
                        tvMessageTime.setTextColor(root.context.getColorForId(R.color.app_gray))
                    }
                }
                ivMessage.isVisible = false
                ivAttachFile.isVisible = false
                tvAttachFileName.isVisible = false
                if (message.isMMS) {
                    tvMessageText.isVisible = false
                    ivTranslate.isVisible = false
                    progressBar.isVisible = false

                    if (message.attachmentWithMessageModel?.attachments?.isNotEmpty() == true) {
                        val mimetype =
                            message.attachmentWithMessageModel?.attachments?.first()?.mimetype ?: ""
                        if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
                            ivMessage.loadImage(
                                uri = message.attachmentWithMessageModel?.attachments?.first()?.uri
                            )
                            ivMessage.isVisible = true
                        } else {
                            ivMessage.isVisible = false
                            ivAttachFile.isVisible = true
                            tvAttachFileName.isVisible = true
                            tvAttachFileName.text = File(
                                message.attachmentWithMessageModel?.attachments?.first()?.filename ?: ""
                            ).name
                        }
                    }
                    if (message.attachmentWithMessageModel?.text?.isNotEmpty() == true) {
                        tvMessageText.isVisible = true
                        ivTranslate.isVisible = true
                        tvMessageText.text =
                            if (message.isShowTranslateBody) message.translatedBody.trim() else message.attachmentWithMessageModel?.text?.trim()
                    }
                } else {
                    tvMessageText.isVisible = true
                    ivTranslate.isVisible = true
                    tvMessageText.text =
                        if (message.isShowTranslateBody) message.translatedBody.trim() else message.body.trim()

                    if (message.isLoading) {
                        ivTranslate.isInvisible = true
                        progressBar.isVisible = true
                    }
                    if (message.isShowTranslateBody) {
                        progressBar.isVisible = false
                        ivTranslate.isVisible = true
                    }
                }

                clMessageBg.setBackgroundTint(
                    if (isSelected(message.id))
                        root.context.getColorForId(R.color.selected_conversation_bg)
                    else if (appPreferences.chatBubbleBgSend == 0) root.context.getColorForId(R.color.app_white) else appPreferences.chatBubbleBgSend
                )


                progressBar.setOnSafeClickListener {
                    if (!toggleSelection(message.id, position)) {
                        ivTranslate.performClick()
                    }
                }
                ivTranslate.setOnSafeClickListener {
                    if (!toggleSelection(message.id, position)) {
                        onClickTranslate.invoke(message, position)
                    }
                }

                tvMessageTime.setOnSafeClickListener {
                    if (selectedMessage.isEmpty()) {
                        onClickResend.invoke(message)
                    }
                }

                root.setOnSafeClickListener {
                   if (toggleSelection(message.id, position, false)) {
                       if (selectedMessage.contains(message)) {
                           selectedMessage.remove(message)
                       } else {
                           selectedMessage.add(message)
                       }
                   }
                    onItemClickMessage.invoke()
                }

                root.setOnLongClickListener {
                    toggleSelection(message.id, position)
                    if (selectedMessage.contains(message)) {
                        selectedMessage.remove(message)
                    } else {
                        selectedMessage.add(message)
                    }
                    onItemLongClick?.invoke()
                    true
                }
            }
        }
    }

    fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
    }

    fun stopSelectionMode() {
        clearSelection()
        selectedMessage = arrayListOf()
    }

    fun refreshChatAdapter(){
        notifyDataSetChanged()
    }

}