package com.messages.ui.appearance.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.databinding.ItemChatColorDataBinding
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.loadImageDrawable
import com.messages.utils.setOnSafeClickListener

class ChatDataAdapter(private val isWallPaper: Boolean) :
    RecyclerView.Adapter<ChatDataAdapter.ChatDataViewHolder>() {

    private var chatColorData: ArrayList<Pair<Int, Int>> = arrayListOf()
    var selectedItem = 0
    var onClickItem: ((Int, Int) -> Unit)? = null
    var pickImage: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatDataViewHolder {
        return ChatDataViewHolder(
            ItemChatColorDataBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return chatColorData.size
    }

    override fun onBindViewHolder(holder: ChatDataViewHolder, position: Int) {
        holder.bind(chatColorData[position], position)
    }

    inner class ChatDataViewHolder(val binding: ItemChatColorDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(colorItem: Pair<Int, Int>, position: Int) {
            binding.apply {
                cvChatData.setStrokeColor(ColorStateList.valueOf(colorItem.first))
                viewColor.setBackgroundColor(colorItem.second)

                if (!isWallPaper) {
                    ivIcon.loadImageDrawable(R.drawable.ic_check)
                    ivIcon.isVisible = selectedItem == colorItem.second
                    tvColorData.isVisible = false
                    ivDesign.isVisible = false
                } else {
                    when (position) {
                        0 -> {
                            ivIcon.loadImageDrawable(R.drawable.ic_block_app_color)
                            tvColorData.text = root.context.getStringValue(R.string.none)
                            tvColorData.setTextColor(root.context.getColorForId(R.color.app_color))
                            ivIcon.isVisible = true
                            tvColorData.isVisible = true
                            ivDesign.isVisible = false
                        }

                        1 -> {
                            ivIcon.loadImageDrawable(R.drawable.ic_add)
                            tvColorData.text = root.context.getStringValue(R.string.add_photos)
                            tvColorData.setTextColor(root.context.getColorForId(R.color.app_color))
                            ivIcon.isVisible = true
                            tvColorData.isVisible = true
                            ivDesign.isVisible = false
                        }

                        else -> {
                            ivIcon.isVisible = false
                            tvColorData.isVisible = false
                            ivDesign.isVisible = true
                        }
                    }
                    if (selectedItem == colorItem.second && position != 1) {
                        cvChatData.strokeColor = root.context.getColorForId(R.color.app_color)
                    } else if (selectedItem == 0 && position == 0) {
                        cvChatData.strokeColor = root.context.getColorForId(R.color.app_color)
                    }
                }

                root.setOnSafeClickListener {
                    if (!(position == 1 && isWallPaper)) {
                        selectedItem = colorItem.second
                    }
                    if (position == 1 && isWallPaper) {
                        pickImage?.invoke()
                    } else {
                        onClickItem?.invoke(colorItem.second, position)
                    }
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun updateData(chatColorData: ArrayList<Pair<Int, Int>>) {
        this.chatColorData = chatColorData
        notifyDataSetChanged()
    }

}