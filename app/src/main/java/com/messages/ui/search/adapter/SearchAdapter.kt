package com.messages.ui.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.common.getOtp
import com.messages.data.models.Conversation
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemConversationBinding
import com.messages.extentions.formatDateOrTime
import com.messages.extentions.getColorForId
import com.messages.utils.setOnSafeClickListener

class SearchAdapter : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    var conversations: ArrayList<Conversation> = arrayListOf()
    var onClickCopyOTP: ((String) -> Unit)? = null
    var onClickConversation: ((Conversation) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            ItemConversationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(conversations[position], position)
    }

    inner class SearchViewHolder(val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(conversation: Conversation, position: Int) {
            binding.apply {
                viewDivider.isVisible = position != (conversations.size - 1)
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
                    tvUnread.text = "${conversation.unreadCount}"
                }

                clCopyOtp.setOnSafeClickListener {
                    val otp = conversation.snippet.getOtp()
                    onClickCopyOTP?.invoke(otp)
                }

                root.setOnSafeClickListener {
                    onClickConversation?.invoke(conversation)
                }
            }
        }
    }

    fun updateConversations(conversation: ArrayList<Conversation>) {
        this.conversations = conversation
        notifyDataSetChanged()
    }
}