package com.messages.ui.language.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.data.models.LanguageModel
import com.messages.databinding.ItemLanguagesBinding
import com.messages.extentions.getColorForId
import com.messages.extentions.getColorStateList
import com.messages.extentions.loadImageDrawable
import com.messages.utils.setOnSafeClickListener

class LanguageAdapter(
    private val languageList: ArrayList<LanguageModel>,
    var selectedLanguageCode: String = "en"
) :
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(private val binding: ItemLanguagesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(language: LanguageModel) {
            binding.apply {
                tvLanguage.text = language.name
                tvLanguageEng.text = language.countryName
                val drawable = if (language.code == selectedLanguageCode) {
                    R.drawable.radio_button_checked
                } else {
                    R.drawable.radio_button_unchecked
                }
                ivChecked.loadImageDrawable(drawable)

                if (language.code == selectedLanguageCode) {
                    cvLanguage.apply {
                        backgroundTintList = getColorStateList(R.color.app_light_blue)
                        setStrokeColor(getColorStateList(R.color.app_light_blue))
                    }
                    tvLanguage.setTextColor(root.context.getColorForId(R.color.app_color))
                    tvLanguageEng.setTextColor(root.context.getColorForId(R.color.app_blue))
                } else {
                    cvLanguage.apply {
                        backgroundTintList = getColorStateList(R.color.app_bg)
                        setStrokeColor(getColorStateList(R.color.app_gray))
                    }
                    tvLanguage.setTextColor(root.context.getColorForId(R.color.app_blue))
                    tvLanguageEng.setTextColor(root.context.getColorForId(R.color.language_item_country))
                }

                root.setOnSafeClickListener {
                    selectedLanguageCode = language.code
                    notifyDataSetChanged()
                    //  notifyItemRangeChanged(0, languageList.size)
                }

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding =
            ItemLanguagesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return languageList.size
    }

    override fun onBindViewHolder(viewHolder: LanguageViewHolder, pos: Int) {
        viewHolder.bind(languageList[pos])
    }
}