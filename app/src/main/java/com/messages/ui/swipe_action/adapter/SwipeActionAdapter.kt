package com.messages.ui.swipe_action.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.databinding.ItemSwipeActionBinding
import com.messages.extentions.getStringValue
import com.messages.ui.swipe_action.SwipeAction
import com.messages.utils.setOnSafeClickListener

class SwipeActionAdapter(
    val actionList: List<String>,
    var selectedAction: Int,
    var onClickAction: (Int) -> Unit
) : RecyclerView.Adapter<SwipeActionAdapter.SwipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        return SwipeViewHolder(
            ItemSwipeActionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return actionList.size
    }

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        holder.bind(actionName = actionList[position], position)
    }

    inner class SwipeViewHolder(val binding: ItemSwipeActionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(actionName: String, position: Int) {
            binding.apply {
                tvActionName.text = actionName
                cbAction.isChecked = actionName == root.context.getStringValue(
                    SwipeAction.fromPosition(selectedAction).stringResId
                )
                viewAction.isInvisible = position == (actionList.size - 1)
                root.setOnSafeClickListener {
                    selectedAction = position
                    onClickAction.invoke(position)
                    notifyDataSetChanged()
                }
            }
        }
    }
}