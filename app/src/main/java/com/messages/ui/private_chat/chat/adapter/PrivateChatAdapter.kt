package com.messages.ui.private_chat.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.common.getOtp
import com.messages.data.models.Conversation
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemConversationBinding
import com.messages.extentions.formatDateOrTime
import com.messages.extentions.getColorForId
import com.messages.ui.base.BaseAdapter
import com.messages.utils.AppLogger
import com.messages.utils.setOnSafeClickListener

class PrivateChatAdapter : BaseAdapter<PrivateChatAdapter.PrivateChatViewHolder>() {

    var conversations :ArrayList<Conversation> = arrayListOf()
    var selectedConversation: ArrayList<Conversation> = arrayListOf()
    var onClickCopyOTP: ((String) -> Unit)? = null
    var onClickConversation: ((Conversation,Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateChatViewHolder {
        val binding =
            ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PrivateChatViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    override fun onBindViewHolder(holder: PrivateChatViewHolder, position: Int) {
        holder.bind(conversations[position], position)
    }

    inner class PrivateChatViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(conversation: Conversation, position: Int){
            binding.apply {
                viewDivider.isInvisible = position == (conversations.size - 1)
                tvConversationTitle.text = conversation.name
                tvConversationDesc.text = conversation.snippet
                avatar.recipients = conversation.recipients
                tvDate.apply {
                    text = conversation.date.formatDateOrTime(AppPreferences(root.context).use24Hr)
                }
                clCopyOtp.isVisible = conversation.type == 3
                ivPined.isVisible = conversation.isPined

                if (conversation.read) {
                    clUnreadCount.isVisible = false
                    ivUnread.isVisible = false
                    tvConversationTitle.setTextAppearance(R.style.AppTextRegular)
                    tvConversationDesc.apply {
                        setTextAppearance(R.style.AppTextRegular)
                        setTextColor(root.context.getColorForId(R.color.app_gray))
                    }
                } else {
                    tvConversationTitle.setTextAppearance(R.style.AppTextBold)
                    tvConversationDesc.apply {
                        setTextAppearance(R.style.AppTextMedium)
                        setTextColor(root.context.getColorForId(R.color.app_blue))
                    }
                    clUnreadCount.isVisible = true
                    ivUnread.isVisible = true
                    AppLogger.d("1234","conversation unreadCount -> ${conversation.unreadCount}")
                    tvUnread.text = "${conversation.unreadCount}"
                }

                clMainConversation.setBackgroundColor(
                    root.context.getColorForId(if (isSelected(conversation.threadId)) R.color.selected_conversation_bg else android.R.color.transparent)
                )

                clCopyOtp.setOnSafeClickListener {
                    val otp = conversation.snippet.getOtp()
                    onClickCopyOTP?.invoke(otp)
                }

                root.setOnSafeClickListener {
                    if (toggleSelection(conversation.threadId, position, false)) {
                        if (selectedConversation.contains(conversation)) {
                            selectedConversation.remove(conversation)
                        } else {
                            selectedConversation.add(conversation)
                        }
                    }
                    onClickConversation?.invoke(conversation,position)
                }

                root.setOnLongClickListener {
                    if (selectedConversation.contains(conversation)) {
                        selectedConversation.remove(conversation)
                    } else {
                        selectedConversation.add(conversation)
                    }
                    toggleSelection(conversation.threadId, position)
                    onItemLongClick?.invoke()
                    return@setOnLongClickListener true
                }
            }
        }
    }

    fun updateConversations(conversation: ArrayList<Conversation>) {
        this.conversations = conversation
        conversations.sortByDescending { it.isPined }
        notifyDataSetChanged()
    }

    fun stopSelectionMode(){
        clearSelection()
        selectedConversation = arrayListOf()
    }

    fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
    }

    fun updateConversation(position: Int, conversation: Conversation) {
        conversations[position] = conversation
        conversations.sortByDescending { it.isPined }
        notifyDataSetChanged()
    }

}