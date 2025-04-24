package com.messages.ui.spam_blocked

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.Conversation
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemConversationBinding
import com.messages.extentions.formatDateOrTime
import com.messages.extentions.getColorForId
import com.messages.ui.base.BaseAdapter
import com.messages.utils.setOnSafeClickListener

class BlockNumberAdapter : BaseAdapter<BlockNumberAdapter.BlockNumberViewHolder>() {

    var conversations: ArrayList<Conversation> = arrayListOf()
    var selectedConversation: ArrayList<Conversation> = arrayListOf()
    var onClickConversation: ((Conversation) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockNumberViewHolder {
        val binding =
            ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockNumberViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    override fun onBindViewHolder(holder: BlockNumberViewHolder, position: Int) {
        holder.bind(conversations[position], position)
    }

    inner class BlockNumberViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(conversation: Conversation, position: Int) {
            binding.apply {
                tvConversationTitle.text = conversation.name
                tvConversationDesc.text = conversation.snippet
                avatar.isInvisible = true
                ivBlocked.isVisible = true
                tvDate.apply {
                    text = conversation.date.formatDateOrTime(AppPreferences(root.context).use24Hr)
                }

                tvConversationTitle.setTextAppearance(R.style.AppTextRegular)
                tvConversationDesc.apply {
                    setTextAppearance(R.style.AppTextRegular)
                    setTextColor(root.context.getColorForId(R.color.app_gray))
                }
                clUnreadCount.isVisible = false
                ivUnread.isVisible = false

                clMainConversation.setBackgroundColor(
                    root.context.getColorForId(
                        if (isSelected(conversation.threadId)) R.color.selected_conversation_bg else android.R.color.transparent
                    )
                )

                root.setOnSafeClickListener {
                    if (toggleSelection(conversation.threadId, position, false)) {
                        if (selectedConversation.contains(conversation)) {
                            selectedConversation.remove(conversation)
                        } else {
                            selectedConversation.add(conversation)
                        }
                    }
                    onClickConversation?.invoke(conversation)
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
        notifyDataSetChanged()
    }

    fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
    }

    fun stopSelectionMode() {
        clearSelection()
        selectedConversation = arrayListOf()
    }

}