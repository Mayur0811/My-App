package com.messages.ui.new_chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.messages.data.models.PhoneNumber
import com.messages.databinding.ItemPhoneNoBinding

class PhoneNoAdapter(private val phoneNoList: ArrayList<PhoneNumber>) :
    RecyclerView.Adapter<PhoneNoAdapter.PhoneNoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneNoViewHolder {
        return PhoneNoViewHolder(
            ItemPhoneNoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return phoneNoList.size
    }

    override fun onBindViewHolder(holder: PhoneNoViewHolder, position: Int) {
        holder.bind(phoneNoList[position])
    }

    inner class PhoneNoViewHolder(val binding: ItemPhoneNoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(phoneNumber: PhoneNumber) {
            binding.apply {
                tvMobileNo.text = phoneNumber.address
                tvMobileNoType.text = phoneNumber.type
            }
        }
    }
}