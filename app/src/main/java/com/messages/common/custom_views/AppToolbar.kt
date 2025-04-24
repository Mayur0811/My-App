package com.messages.common.custom_views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.messages.R
import com.messages.databinding.AppToolbarBinding


class AppToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Toolbar(context, attrs) {

    private var binding: AppToolbarBinding = AppToolbarBinding.bind(
        LayoutInflater.from(context).inflate(R.layout.app_toolbar, this, true)
    )

    var isActionMode = false

    fun getBinding(): AppToolbarBinding {
        return binding
    }

    fun startActionMode() {
        binding.apply {
            if (!isActionMode) {
                isActionMode = true
                mainToolbar.isVisible = false
                actionToolbar.isVisible = true
                tvActionToolbarTitle.text = "0"
                refreshMenu()
            }
        }
    }


    fun stopActionMode() {
        binding.apply {
            isActionMode = false
            mainToolbar.isVisible = true
            actionToolbar.isVisible = false
            tvActionToolbarTitle.text = "0"
            refreshMenu()
        }
    }

    fun refreshMenu() {
        (context as? Activity)?.invalidateOptionsMenu()
    }


}