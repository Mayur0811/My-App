package com.messages.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.data.models.PopupMenuModel
import com.messages.databinding.ItemPopupBinding
import com.messages.extentions.loadImageDrawable
import com.messages.utils.setOnSafeClickListener


class PopupAdapter(
    private val menuList: ArrayList<PopupMenuModel>,
    val onClickMenuItem: (PopupMenuModel) -> Unit
) :
    RecyclerView.Adapter<PopupAdapter.PopupViewHolder>() {

    inner class PopupViewHolder(val binding: ItemPopupBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: PopupMenuModel, position: Int) {
            binding.apply {
                tvMenuTitle.text = menuItem.menuName
                if (menuItem.menuIcon > 0) {
                    ivMenu.loadImageDrawable(
                        menuItem.menuIcon
                    )
                    ivMenu.isVisible = true
                }

                viewDivider.isVisible = position != (menuList.size - 1)

                root.setOnSafeClickListener {
                    onClickMenuItem.invoke(menuItem)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopupViewHolder {
        val binding = ItemPopupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopupViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return menuList.size
    }

    override fun onBindViewHolder(holder: PopupViewHolder, position: Int) {
        holder.bind(menuList[position], position)
    }
}