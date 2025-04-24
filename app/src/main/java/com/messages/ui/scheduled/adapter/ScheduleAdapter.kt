package com.messages.ui.scheduled.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.ScheduleConversation
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemConversationBinding
import com.messages.extentions.formatDateOrTime
import com.messages.extentions.getColorForId
import com.messages.ui.base.BaseAdapter
import com.messages.utils.setOnSafeClickListener

class ScheduleAdapter : BaseAdapter<ScheduleAdapter.ScheduleViewHolder>() {

    var conversations: ArrayList<ScheduleConversation> = arrayListOf()
    var onClickConversation: ((ScheduleConversation) -> Unit)? = null
    var selectedConversation: ArrayList<ScheduleConversation> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding =
            ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScheduleViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return conversations.size
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(conversations[position],position)
    }

    fun updateConversations(conversation: ArrayList<ScheduleConversation>) {
        this.conversations = conversation
        notifyDataSetChanged()
    }

    inner class ScheduleViewHolder(val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(scheduleConversation: ScheduleConversation, position: Int) {
            binding.apply {
                tvConversationTitle.text = scheduleConversation.conversation.name
                tvConversationDesc.text = scheduleConversation.message.body
                avatar.recipients = scheduleConversation.conversation.recipients
                tvDate.apply {
                    text = scheduleConversation.message.date.formatDateOrTime(
                        AppPreferences(root.context).use24Hr,
                        hideTimeAtOtherDays = false
                    )
                }
                clCopyOtp.isVisible = false
                ivPined.isVisible = false

                tvConversationTitle.setTextAppearance(R.style.AppTextBold)
                tvConversationDesc.apply {
                    setTextAppearance(R.style.AppTextMedium)
                    setTextColor(root.context.getColorForId(R.color.app_blue))
                }
                clUnreadCount.isVisible = false
                ivUnread.isVisible = false

                clMainConversation.setBackgroundColor(
                    root.context.getColorForId(if (isSelected(scheduleConversation.conversation.threadId)) R.color.selected_conversation_bg else android.R.color.transparent)
                )

                root.setOnSafeClickListener {
                    if (toggleSelection(scheduleConversation.conversation.threadId, position, false)) {
                        if (selectedConversation.contains(scheduleConversation)) {
                            selectedConversation.remove(scheduleConversation)
                        } else {
                            selectedConversation.add(scheduleConversation)
                        }
                    }
                    onClickConversation?.invoke(scheduleConversation)
                }

                root.setOnLongClickListener {
                    if (selectedConversation.contains(scheduleConversation)) {
                        selectedConversation.remove(scheduleConversation)
                    } else {
                        selectedConversation.add(scheduleConversation)
                    }
                    toggleSelection(scheduleConversation.conversation.threadId, position)
                    onItemLongClick?.invoke()
                    return@setOnLongClickListener true
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
        selectedConversation = arrayListOf()
    }
}