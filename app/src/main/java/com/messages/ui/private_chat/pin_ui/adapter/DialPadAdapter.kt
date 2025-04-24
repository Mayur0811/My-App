package com.messages.ui.private_chat.pin_ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.databinding.ItemDialPadBinding
import com.messages.extentions.getDrawableAsString
import com.messages.extentions.getStringValue
import com.messages.utils.setOnSafeClickListener

class DialPadAdapter(
    private val dialPadNumber: ArrayList<String>,
    val onClickItem: (String) -> Unit
) :
    RecyclerView.Adapter<DialPadAdapter.DialPadViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialPadViewHolder {
        return DialPadViewHolder(
            ItemDialPadBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return dialPadNumber.size
    }

    override fun onBindViewHolder(holder: DialPadViewHolder, position: Int) {
        holder.bind(dialPadNumber[position])
    }

    inner class DialPadViewHolder(val binding: ItemDialPadBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(number: String) {
            binding.apply {
                if (number.isEmpty()) {
                    tvDialNumber.isInvisible = true
                } else if (number == root.context.getStringValue(R.string.backspace)) {
                    tvDialNumber.text = root.context.getDrawableAsString(R.drawable.ic_backspace)
                } else {
                    tvDialNumber.text = number
                }

                tvDialNumber.setOnSafeClickListener {
                    onClickItem.invoke(number)
                }

                root.setOnSafeClickListener {
                    tvDialNumber.performClick()
                }
            }
        }
    }
}