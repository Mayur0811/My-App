package com.messages.common.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.messages.R
import com.messages.databinding.CustomSettingsItemViewBinding


class CustomSettingsItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: CustomSettingsItemViewBinding = CustomSettingsItemViewBinding.bind(
        LayoutInflater.from(context).inflate(R.layout.custom_settings_item_view, this, true)
    )

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CustomSettingsItemView)

            val titleText = typedArray.getString(R.styleable.CustomSettingsItemView_titleText)
            val subtitleText = typedArray.getString(R.styleable.CustomSettingsItemView_subtitleText)
            val iconRes = typedArray.getResourceId(R.styleable.CustomSettingsItemView_iconSrc, 0)
            val showSwitch =
                typedArray.getBoolean(R.styleable.CustomSettingsItemView_showSwitch, false)
            val showArrowNext =
                typedArray.getBoolean(R.styleable.CustomSettingsItemView_showArrowNext, false)

            binding.apply {
                customViewTitle.text = titleText
                customViewSubtitle.text = subtitleText
                customViewSubtitle.isVisible = !subtitleText.isNullOrEmpty()
                customViewIcon.setImageResource(iconRes)
                switchToggle.isVisible = showSwitch
                arrowNext.isVisible = showArrowNext
            }

            typedArray.recycle()
        }
    }

    fun getBinding(): CustomSettingsItemViewBinding {
        return binding
    }

}
