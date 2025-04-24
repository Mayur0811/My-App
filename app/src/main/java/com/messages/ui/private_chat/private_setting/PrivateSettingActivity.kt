package com.messages.ui.private_chat.private_setting

import android.os.Bundle
import com.messages.R
import com.messages.databinding.ActivityPrivateSettingBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.private_chat.pin_ui.ui.EnterPinActivity
import com.messages.utils.CHANGE_PASSWORD
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivateSettingActivity : BaseActivity() {

    private val binding by lazy { ActivityPrivateSettingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        initToolbar()
        initActions()
    }

    private fun initViews() {
        binding.switchManageNotification.isChecked = appPreferences.isPrivateChatNotify
    }

    private fun initActions() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                finish()
            }

            clPrivateChatManageNotify.setOnSafeClickListener {
                switchManageNotification.isChecked = !switchManageNotification.isChecked
                appPreferences.isPrivateChatNotify = switchManageNotification.isChecked
            }

            switchManageNotification.setOnCheckedChangeListener { _, isChecked ->
                appPreferences.isPrivateChatNotify = isChecked
            }

            clChangeSecurity.setOnSafeClickListener {
                launchActivity(EnterPinActivity::class.java) {
                    putBoolean(CHANGE_PASSWORD, true)
                }
            }
        }
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.setting)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}