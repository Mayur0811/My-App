package com.messages.ui.call_end.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.databinding.ItemInstantMsgBinding
import com.messages.extentions.loadImageDrawable
import com.messages.utils.setOnSafeClickListener

class InstantMsgAdapter(
    private val instantMsgList: ArrayList<String>,
    var onClick: (String) -> Unit
) : RecyclerView.Adapter<InstantMsgAdapter.InstantMsgViewHolder>() {

    private var selectedItem = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstantMsgViewHolder {
        return InstantMsgViewHolder(
            ItemInstantMsgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return instantMsgList.size
    }

    override fun onBindViewHolder(holder: InstantMsgViewHolder, position: Int) {
        holder.bind(instantMsg = instantMsgList[position], position)
    }

    inner class InstantMsgViewHolder(val binding: ItemInstantMsgBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(instantMsg: String, position: Int) {
            binding.apply {
                tvInstantTitle.text = instantMsg
                viewDivider.isVisible = position != (instantMsgList.size - 1)
                ivInstantMsg.loadImageDrawable(
                    if (selectedItem == position) R.drawable.radio_button_checked else R.drawable.radio_button_unchecked
                )

                root.setOnSafeClickListener {
                    onClick.invoke(instantMsg)
                    selectedItem = position
                    notifyDataSetChanged()
                }
            }
        }
    }
}