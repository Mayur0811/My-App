package com.messages.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.data.models.TranslateLanguageModel
import com.messages.databinding.ItemTranslateLanguageBinding
import com.messages.utils.setOnSafeClickListener

class TranslateLanguageAdapter :
    RecyclerView.Adapter<TranslateLanguageAdapter.TranslateLanguageViewHolder>() {

    var translateLanguage: ArrayList<TranslateLanguageModel> = arrayListOf()
    var onClickLanguage: ((TranslateLanguageModel) -> Unit)? = null

    inner class TranslateLanguageViewHolder(val binding: ItemTranslateLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(language: TranslateLanguageModel, position: Int) {
            binding.apply {
                tvLanguageTitle.text = language.displayName
                viewDivider.isVisible = position != (translateLanguage.size - 1)

                root.setOnSafeClickListener {
                    onClickLanguage?.invoke(language)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): TranslateLanguageViewHolder {
        val binding =
            ItemTranslateLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TranslateLanguageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return translateLanguage.size
    }

    override fun onBindViewHolder(holder: TranslateLanguageViewHolder, position: Int) {
        holder.bind(translateLanguage[position], position)
    }

    fun updateData(translateLanguage: ArrayList<TranslateLanguageModel>){
        this.translateLanguage = translateLanguage
        notifyDataSetChanged()
    }

}