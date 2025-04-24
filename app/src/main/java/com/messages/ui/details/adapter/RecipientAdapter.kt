package com.messages.ui.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.data.models.Recipient
import com.messages.databinding.ItemDetailsRecipientBinding
import com.messages.utils.setOnSafeClickListener

class RecipientAdapter(val recipients: List<Recipient>, val onClick: ((Int, Recipient) -> Unit)) :
    RecyclerView.Adapter<RecipientAdapter.RecipientViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipientViewHolder {
        return RecipientViewHolder(
            ItemDetailsRecipientBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return recipients.size
    }

    override fun onBindViewHolder(holder: RecipientViewHolder, position: Int) {
        holder.bind(recipients[position])
    }

    inner class RecipientViewHolder(val binding: ItemDetailsRecipientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipient: Recipient) {
            binding.apply {
                avatar.setRecipient(recipient)

                tvRecipientTitle.text = recipient.getDisplayName()
                tvRecipientDesc.text = recipient.address

                ivRecipientCall.setOnSafeClickListener {
                    onClick.invoke(0,recipient)
                }
                ivRecipientMessage.setOnSafeClickListener {
                    onClick.invoke(1,recipient)
                }
            }
        }
    }


}