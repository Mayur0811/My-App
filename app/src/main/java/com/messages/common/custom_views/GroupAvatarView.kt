package com.messages.common.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.messages.R
import com.messages.data.models.Recipient
import com.messages.databinding.GroupAvatarViewBinding
import com.messages.extentions.resolveThemeColor
import com.messages.extentions.setBackgroundTint

class GroupAvatarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    var recipients: List<Recipient> = ArrayList()
        set(value) {
            field = value.sortedWith(compareByDescending { contact -> contact.contact?.lookupKey })
            updateView()
        }
    private val binding: GroupAvatarViewBinding = GroupAvatarViewBinding.bind(
        LayoutInflater.from(context).inflate(R.layout.group_avatar_view, this, true)
    )

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (!isInEditMode) {
            updateView()
        }
    }

    private fun updateView() {
        binding.apply {
            avatar1Frame.setBackgroundTint(
                when (recipients.size > 1) {
                    true -> context.resolveThemeColor(android.R.attr.windowBackground)
                    false -> ContextCompat.getColor(context, android.R.color.transparent)
                }
            )
            avatar1Frame.updateLayoutParams<LayoutParams> {
                matchConstraintPercentWidth = if (recipients.size > 1) 0.75f else 1.0f
            }
            avatar2Frame.isVisible = recipients.size > 1


            recipients.getOrNull(0).run(avatar1::setRecipient)
            recipients.getOrNull(1).run(avatar2::setRecipient)
        }
    }

}
