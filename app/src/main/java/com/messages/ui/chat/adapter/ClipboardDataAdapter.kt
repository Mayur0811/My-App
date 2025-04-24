package com.messages.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.databinding.ItemClipboardDataBinding
import com.messages.utils.setOnSafeClickListener

class ClipboardDataAdapter(
    private val clipboardDataList: ArrayList<String>,
    var onClick: (String) -> Unit
) :
    RecyclerView.Adapter<ClipboardDataAdapter.ClipboardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
        return ClipboardViewHolder(
            ItemClipboardDataBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return clipboardDataList.size
    }

    override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
        holder.bind(clipboardDataList[position])
    }

    inner class ClipboardViewHolder(val binding: ItemClipboardDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(clipData: String) {
            binding.apply {
                tvClipboardTitle.text = clipData
                root.setOnSafeClickListener {
                    onClick.invoke(clipData)
                }
            }
        }
    }
}