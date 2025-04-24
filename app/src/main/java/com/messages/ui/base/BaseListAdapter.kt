package com.messages.ui.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseListAdapter<T, VH : RecyclerView.ViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) :
    ListAdapter<T, VH>(diffCallback) {

    var selection = listOf<Long>()
    var selectedPos: ArrayList<Int> = arrayListOf()
    var onItemLongClick: (() -> Unit)? = null

    protected fun toggleSelection(id: Long, position: Int, force: Boolean = true): Boolean {
        if (!force && selection.isEmpty()) return false

        selection = when (selection.contains(id)) {
            true -> selection - id
            false -> selection + id
        }
        if (selectedPos.contains(position)) {
            selectedPos.remove(position)
        } else {
            selectedPos.add(position)
        }
        notifyItemChanged(position)
        return true
    }

    protected fun isSelected(id: Long): Boolean {
        return selection.contains(id)
    }

    open fun clearSelection() {
        selection = listOf()
        selectedPos = arrayListOf()
        onItemLongClick?.invoke()
        notifyDataSetChanged()
    }

}