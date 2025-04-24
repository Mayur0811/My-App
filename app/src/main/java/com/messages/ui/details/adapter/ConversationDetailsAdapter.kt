package com.messages.ui.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.ConversationDetailsModel
import com.messages.data.models.Recipient
import com.messages.databinding.ItemConversationDetailHeaderBinding
import com.messages.databinding.ItemConversationDetailsMenuBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.loadImageDrawable
import com.messages.utils.setOnSafeClickListener

class ConversationDetailsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var detailsList: ArrayList<ConversationDetailsModel> = arrayListOf()
    var onClickMenu: ((String) -> Unit)? = null
    var onClickHeader: ((Int, Recipient) -> Unit)? = null

    val detailsHeader = 0
    val detailsMenu = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder: RecyclerView.ViewHolder = when (viewType) {
            detailsMenu -> ConversationDetailsMenuViewHolder(
                ItemConversationDetailsMenuBinding.inflate(inflater, parent, false)
            )

            detailsHeader -> ConversationDetailsHeaderViewHolder(
                ItemConversationDetailHeaderBinding.inflate(
                    inflater, parent, false
                )
            )

            else -> ConversationDetailsHeaderViewHolder(
                ItemConversationDetailHeaderBinding.inflate(
                    inflater, parent, false
                )
            )
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return detailsList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            detailsHeader -> {
                (holder as ConversationDetailsHeaderViewHolder).bind(detailsList[position])
            }

            detailsMenu -> {
                (holder as ConversationDetailsMenuViewHolder).bind(detailsList[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return detailsList[position].viewType
    }

    fun updateDetails(detailsList: ArrayList<ConversationDetailsModel>) {
        this.detailsList = detailsList
        notifyDataSetChanged()
    }


    inner class ConversationDetailsHeaderViewHolder(val binding: ItemConversationDetailHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(conversationDetailsModel: ConversationDetailsModel) {
            binding.apply {
                if ((conversationDetailsModel.recipients?.size ?: 1) > 1) {
                    clGroupConversation.isVisible = true
                    clSingleRecipient.isVisible = false
                    rcvRecipient.adapter = RecipientAdapter(
                        conversationDetailsModel.recipients ?: listOf()
                    ) { action, recipient ->
                        onClickHeader?.invoke(action, recipient)
                    }

                } else {
                    clGroupConversation.isVisible = false
                    clSingleRecipient.isVisible = true
                    avatar.recipients = conversationDetailsModel.recipients ?: listOf()
                    tvConversationTitle.text =
                        conversationDetailsModel.recipients?.joinToString { recipient -> recipient.getDisplayName() }
                    tvConversationDesc.text =
                        conversationDetailsModel.recipients?.joinToString { recipient -> recipient.address }

                    ivDetailsCall.setOnSafeClickListener {
                        conversationDetailsModel.recipients?.first()
                            ?.let { it1 -> onClickHeader?.invoke(0, it1) }
                    }
                }
            }
        }
    }

    inner class ConversationDetailsMenuViewHolder(val binding: ItemConversationDetailsMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(conversationDetailsModel: ConversationDetailsModel) {
            binding.apply {
                if (conversationDetailsModel.menuTitle == root.context.getStringValue(R.string.archive) && conversationDetailsModel.isArchived) {
                    ivDetailsIcon.loadImageDrawable(R.drawable.ic_unarchive)
                    tvDetailsTitle.text = root.context.getStringValue(R.string.unarchive)
                } else {
                    ivDetailsIcon.loadImageDrawable(conversationDetailsModel.menuIcon)
                    tvDetailsTitle.text = conversationDetailsModel.menuTitle
                }
                root.setOnSafeClickListener {
                    onClickMenu?.invoke(conversationDetailsModel.menuTitle)
                }
            }
        }
    }
}