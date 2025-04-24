package com.messages.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.common.mainScope
import com.messages.data.models.AttachmentData
import com.messages.data.models.isGifMimeType
import com.messages.data.models.isImageMimeType
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ItemAttachmentDocumentBinding
import com.messages.databinding.ItemAttachmentMediaBinding
import com.messages.databinding.ItemAttachmentVcardBinding
import com.messages.extentions.formatSize
import com.messages.extentions.getByteForMmsLimit
import com.messages.extentions.getFileSizeFromUri
import com.messages.extentions.getStringValue
import com.messages.extentions.loadImage
import com.messages.extentions.loadImageWithOutCache
import com.messages.extentions.toast
import com.messages.utils.AppImageCompressor
import com.messages.utils.FILE_SIZE_NONE
import com.messages.utils.parseNameFromVCard
import com.messages.utils.parseVCardFromUri
import com.messages.utils.setOnSafeClickListener
import kotlinx.coroutines.launch
import java.io.File

class AttachmentAdapter(var attachmentList: ArrayList<AttachmentData> = arrayListOf()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onRemoveAttachment: (() -> Unit)? = null
    var onImageCompress: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder: RecyclerView.ViewHolder = when (viewType) {
            AttachmentData.ATTACHMENT_MEDIA -> AttachmentMediaViewHolder(
                ItemAttachmentMediaBinding.inflate(
                    inflater, parent, false
                )
            )

            AttachmentData.ATTACHMENT_DOCUMENT -> AttachmentDocumentViewHolder(
                ItemAttachmentDocumentBinding.inflate(inflater, parent, false)
            )

            AttachmentData.ATTACHMENT_VCARD -> AttachmentVCardViewHolder(
                ItemAttachmentVcardBinding.inflate(
                    inflater, parent, false
                )
            )

            else -> AttachmentMediaViewHolder(
                ItemAttachmentMediaBinding.inflate(
                    inflater, parent, false
                )
            )
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return attachmentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val attachment = attachmentList[position]
        when (attachment.viewType) {
            AttachmentData.ATTACHMENT_MEDIA -> {
                (holder as AttachmentMediaViewHolder).bind(attachment, position)
            }

            AttachmentData.ATTACHMENT_DOCUMENT -> {
                (holder as AttachmentDocumentViewHolder).bind(attachment, position)
            }

            AttachmentData.ATTACHMENT_VCARD -> {
                (holder as AttachmentVCardViewHolder).bind(attachment, position)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return attachmentList[position].viewType
    }

    inner class AttachmentMediaViewHolder(val binding: ItemAttachmentMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(attachment: AttachmentData, position: Int) {
            binding.apply {
                val mmsLimit =
                    root.context.getByteForMmsLimit(AppPreferences(root.context).mmsLimit)
                val compressImage =
                    attachment.mimetype.isImageMimeType() && !attachment.mimetype.isGifMimeType()
                if (attachment.uri != null) {
                    if (compressImage && attachment.isPending && mmsLimit != FILE_SIZE_NONE) {
                        ivAttachmentMedia.isVisible = false
                        compressionProgress.isVisible = true

                        AppImageCompressor(root.context).compressImage(
                            attachment.uri, mmsLimit
                        ) { compressedUri ->
                            mainScope.launch {
                                when (compressedUri) {
                                    attachment.uri -> {
                                        attachmentList.find { it.uri == attachment.uri }?.isPending =
                                            false
                                        ivAttachmentMedia.loadImage(uri = attachment.uri)
                                        compressionProgress.isVisible = false
                                        clRemove.isVisible = true
                                        ivAttachmentMedia.isVisible = true
                                    }

                                    null -> {
                                        root.context.toast(root.context.getStringValue(R.string.compress_error))
                                        removeAttachment(position)
                                    }

                                    else -> {
                                        attachmentList.remove(attachment)
                                        addAttachment(
                                            attachment.copy(
                                                uri = compressedUri,
                                                isPending = false
                                            )
                                        )
                                    }
                                }
                                onImageCompress?.invoke()
                            }
                        }
                    } else {
                        ivAttachmentMedia.loadImageWithOutCache(uri = attachment.uri)
                        compressionProgress.isVisible = false
                        clRemove.isVisible = true
                    }
                } else {
                    root.context.toast(root.context.getStringValue(R.string.compress_error))
                    removeAttachment(position)
                }
                ivAttachmentMedia.isVisible = true
                clRemove.setOnSafeClickListener {
                    removeAttachment(position)
                }
            }
        }
    }

    inner class AttachmentDocumentViewHolder(val binding: ItemAttachmentDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(attachment: AttachmentData, position: Int) {
            binding.apply {
                tvDocumentTitle.text = File(attachment.filename ?: "").name
                tvDocumentSize.text = root.context.getFileSizeFromUri(attachment.uri).formatSize()
                clRemove.setOnSafeClickListener {
                    removeAttachment(position)
                }
            }
        }
    }

    inner class AttachmentVCardViewHolder(val binding: ItemAttachmentVcardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(attachment: AttachmentData, position: Int) {
            binding.apply {
                parseVCardFromUri(root.context, attachment.uri) { vCards ->
                    mainScope.launch {
                        if (vCards.isEmpty()) {
                            vcardTitle.isVisible = true
                            vcardTitle.text =
                                root.context.getString(R.string.unknown_error_occurred)
                        } else {
                            val title = vCards.firstOrNull()?.parseNameFromVCard()
                            vcardTitle.text = title
                            if (vCards.size > 1) {
                                vcardSubtitle.isVisible = true
                                val quantity = vCards.size - 1
                                vcardSubtitle.text = root.context.resources.getQuantityString(
                                    R.plurals.and_other_contacts, quantity, quantity
                                )
                            } else {
                                vcardSubtitle.isVisible = false
                            }
                        }
                    }
                }
                clRemove.setOnSafeClickListener {
                    removeAttachment(position)
                }
            }
        }
    }

    fun addAttachment(attachment: AttachmentData) {
        attachmentList.add(attachment)
        notifyDataSetChanged()
    }

    fun removeAttachment(position: Int) {
        attachmentList.removeAt(position)
        onRemoveAttachment?.invoke()
        notifyDataSetChanged()
    }

}