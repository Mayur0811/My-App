package com.messages.common.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.messages.R
import com.messages.data.models.Recipient
import com.messages.databinding.AvatarViewBinding

class AvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding: AvatarViewBinding = AvatarViewBinding.bind(
        LayoutInflater.from(context).inflate(R.layout.avatar_view, this, true)
    )

    private var lookupKey: String? = null
    private var fullName: String? = null
    private var photoUri: String? = null
    private var lastUpdated: Long? = null

    init {
        setBackgroundResource(R.drawable.avatar_bg)
        clipToOutline = true
    }

    fun setRecipient(recipient: Recipient?) {
        lookupKey = recipient?.contact?.lookupKey
        fullName = recipient?.contact?.name
        photoUri = recipient?.contact?.photoUri
        lastUpdated = recipient?.contact?.lastUpdate
        updateView()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        binding.apply {
            val initials = fullName
                ?.substringBefore(',')
                ?.split(" ").orEmpty()
                .filter { name -> name.isNotEmpty() }
                .map { name -> name[0] }
                .filter { initial -> initial.isLetterOrDigit() }
                .map { initial -> initial.toString() }

            if (initials.isNotEmpty()) {
                initial.text =
                   /* if (initials.size > 1) initials.first() + initials.last() else*/ initials.first()
                icon.visibility = GONE
            } else {
                initial.text = null
                icon.visibility = VISIBLE
            }

            photo.setImageDrawable(null)
            photoUri?.let { photoUri ->
                Glide.with(photo)
                    .load(photoUri)
                    .into(photo)
            }
        }
    }

}