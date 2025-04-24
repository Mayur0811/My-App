package com.messages.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.databinding.ItemFilterBinding
import com.messages.utils.setOnSafeClickListener

class FilterAdapter : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private var filterList: ArrayList<String> = arrayListOf()
    var selectedFilter = 0
    var onClickFilter: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        return FilterViewHolder(
            ItemFilterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return filterList.size
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filterList[position], position)
    }

    inner class FilterViewHolder(private val binding: ItemFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(filter: String, position: Int) {
            binding.apply {
                tvFilterName.text = filter

                cbFilter.isChecked = selectedFilter == position

                root.setOnSafeClickListener {
                    selectedFilter = position
                    onClickFilter?.invoke(position)
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun addFilterData(filterList: ArrayList<String>) {
        this.filterList = filterList
        notifyDataSetChanged()
    }
}