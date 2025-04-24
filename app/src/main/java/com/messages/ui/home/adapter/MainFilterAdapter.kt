package com.messages.ui.home.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.MainFilterModel
import com.messages.databinding.ItemMainFilterBinding
import com.messages.extentions.getColorForId
import com.messages.utils.setOnSafeClickListener

class MainFilterAdapter(
    private var filterList: ArrayList<MainFilterModel>,
    val onClickFilter: ((MainFilterModel) -> Unit)
) :
    RecyclerView.Adapter<MainFilterAdapter.MainFilterViewHolder>() {

    private var selectedFilter = ALL

    inner class MainFilterViewHolder(private val binding: ItemMainFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(filter: MainFilterModel) {
            binding.apply {
                tvFilterName.text = filter.filterName
                if (filter.filterId == selectedFilter) {
                    tvFilterName.apply {
                        setTextColor(root.context.getColorForId(R.color.only_white))
                        setTypeface(this.typeface, Typeface.BOLD)
                    }
                    clFilterHolder.background =
                        ContextCompat.getDrawable(root.context, R.drawable.filter_bg)
                } else {
                    tvFilterName.apply {
                        setTextColor(root.context.getColorForId(R.color.app_gray))
                        setTypeface(this.typeface, Typeface.NORMAL)
                    }
                    clFilterHolder.background = null
                }

                tvFilterName.setOnSafeClickListener {
                    clFilterHolder.performClick()
                }

                clFilterHolder.setOnSafeClickListener {
                    selectedFilter = filter.filterId
                    onClickFilter.invoke(filter)
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainFilterViewHolder {
        val binding =
            ItemMainFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MainFilterViewHolder(binding)
    }

    override fun getItemCount(): Int = filterList.size

    override fun onBindViewHolder(holder: MainFilterViewHolder, position: Int) {
        holder.bind(filterList[position])
    }

    companion object {
        const val ALL = 0
        const val PERSONAL = 1
        const val OTP = 2
        const val TRANSACTIONS = 3
        const val OFFERS = 4
    }
}