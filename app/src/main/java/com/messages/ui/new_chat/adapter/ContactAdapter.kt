package com.messages.ui.new_chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SectionIndexer
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.data.models.Contact
import com.messages.data.models.Recipient
import com.messages.databinding.ItemContactBinding
import com.messages.utils.setOnSafeClickListener


class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), SectionIndexer {

    var contacts: ArrayList<Contact> = arrayListOf()
    var onClickContact: ((Contact) -> Unit)? = null


    private var mSectionPositions: ArrayList<Int> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        return ContactViewHolder(
            ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], position)
    }

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact, position: Int) {
            binding.apply {
                tvContactName.text = contact.name
                contactAvatar.setRecipient(Recipient(contact = contact))
                rcvNumbers.apply {
                    adapter = PhoneNoAdapter(contact.phoneNumbers as ArrayList)
                }

                if (contact.isShowAlphabet) {
                    tvContactAlphabet.apply {
                        text = if (contact.isFavorite) "❤" else if (contact.name.first().isLetter())
                            contact.name.first().toString() else "#"
                    }

                    tvContactAlphabet.isVisible = true
                    viewDividerTop.isVisible = position != 0
                    viewDivider.isInvisible = true
                } else {
                    tvContactAlphabet.isVisible = false
                    viewDividerTop.isVisible = false
                    viewDivider.isVisible = true
                }

                viewClick.setOnSafeClickListener {
                    root.performClick()
                }

                root.setOnSafeClickListener {
                    onClickContact?.invoke(contact)
                }
            }
        }

    }

    override fun getSections(): Array<String> {
        val sections: MutableList<String> = ArrayList()
        mSectionPositions = ArrayList()
        for (i in contacts.indices) {
            val contact = contacts[i]
            val section = if (contact.isFavorite) "❤" else if (contact.name.first()
                    .isLetter()
            ) contact.name.first().toString() else "#"
            if (!sections.contains(section)) {
                sections.add(section)
                mSectionPositions.add(i)
            }
        }
        return sections.toTypedArray<String>()
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        return mSectionPositions[sectionIndex]
    }

    override fun getSectionForPosition(position: Int): Int {
        return 0
    }
}