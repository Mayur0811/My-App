package com.messages.ui.overlay_permission.dialog

import android.os.Bundle
import android.view.View
import com.messages.R
import com.messages.databinding.DialogOverlayBinding
import com.messages.ui.base.BaseActivity
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverlayDialogActivity : BaseActivity() {

    private val binding by lazy { DialogOverlayBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            tvTitle.text =
                getString(R.string.find_and_allow_permission, getString(R.string.app_name))
            icClose.setOnSafeClickListener { finish() }
            clMainOverlay.setOnSafeClickListener { finish() }
        }
    }

}