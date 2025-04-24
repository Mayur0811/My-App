package com.messages.ui.permission

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.AppJobScheduler
import com.messages.common.PermissionManager
import com.messages.common.backgroundScope
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityPermissionAskBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.ui.base.BaseActivity
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.overlay_permission.OverlayPermissionActivity
import com.messages.utils.IS_ASK_DEFAULT
import com.messages.utils.PERMISSION_VIEW
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermissionAskActivity : BaseActivity() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val defaultPermission = 0
    private val notificationPermission = 1
    private val phoneStatePermission = 2

    private var currentView = -1

    private var isAskDefault = false

    private val binding by lazy { ActivityPermissionAskBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateStatusBarColor(R.color.app_bg)
        currentView = intent.getIntExtra(PERMISSION_VIEW, -1)
        isAskDefault = intent.getBooleanExtra(IS_ASK_DEFAULT, false)
        initViews()
        initActions()
    }

    private fun initActions() {
        binding.btnPermission.setOnSafeClickListener {
            when (currentView) {
                defaultPermission -> {
                    requestDefaultSmsApp()
                }

                notificationPermission -> {
                    requestNotificationPermission()
                }

                else -> {
                    handlePhoneStatePermission {
                        moveToNext()
                    }
                }
            }
        }
    }

    private fun initViews() {
        binding.apply {
            when (currentView) {
                defaultPermission -> {
                    tvPermissionTitle.text = getStringValue(R.string.set_as_default_title)
                    tvPermissionMsg.text = getStringValue(R.string.set_as_default_msg)
                    btnPermission.text = getStringValue(R.string.set_as_default_btn_text)
                    clPhoneStatePermissionGuide.isVisible = false
                    ivPermissionView.loadImageDrawable(R.drawable.ic_default_app)
                }

                notificationPermission -> {
                    tvPermissionTitle.text = getStringValue(R.string.notification_per_title)
                    tvPermissionMsg.text = getStringValue(R.string.notification_per_msg)
                    btnPermission.text = getStringValue(R.string.notification_per_btn_text)
                    clPhoneStatePermissionGuide.isVisible = false
                    ivPermissionView.loadImageDrawable(R.drawable.ic_notification)

                }

                phoneStatePermission -> {
                    tvPermissionTitle.text = getStringValue(R.string.phone_state_per_title)
                    tvPermissionMsg.text = getStringValue(R.string.phone_state_per_msg)
                    btnPermission.text = getStringValue(R.string.phone_state_per_btn_text)
                    clPhoneStatePermissionGuide.isVisible = true
                    ivPermissionView.loadImageDrawable(R.drawable.ic_phone_state)
                }
            }
        }
    }

    override fun onSetDefaultSMS() {
        backgroundScope.launch {
            isSyncInProgress = true
            messageRepository.syncRecipients {
                messageRepository.syncConversation {}
            }
            messageRepository.syncContacts {}
        }
        if (isAskDefault) {
            finish()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!permissionManager.hasNotification()) {
                    launchActivity(PermissionAskActivity::class.java) {
                        putInt(PERMISSION_VIEW, notificationPermission)
                    }
                    finish()
                } else {
                    handleReadPhoneStatePermission()
                }
            } else {
                handleReadPhoneStatePermission()
            }
        }
    }

    private fun handleReadPhoneStatePermission() {
        if (!permissionManager.hasReadPhoneState()) {
            launchActivity(PermissionAskActivity::class.java) {
                putInt(PERMISSION_VIEW, phoneStatePermission)
            }
            finish()
        } else {
            AppJobScheduler().setScheduleJob(this)
            moveToNext()
        }
    }


    override fun onGetPermission(permission: Int, isGranted: Boolean) {
        when (permission) {
            PermissionManager.PERMISSION_POST_NOTIFICATIONS -> {
                if (isGranted) {
                    handleReadPhoneStatePermission()
                }
            }

            PermissionManager.PERMISSION_READ_PHONE_STATE -> {
                AppJobScheduler().setScheduleJob(this)
                if (isGranted) {
                    moveToNext()
                }
            }
        }
    }

    private fun moveToNext() {
        if (!permissionManager.hasOverlay()) {
            launchActivity(
                OverlayPermissionActivity::class.java,
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            )
        } else {
            launchActivity(
                HomeActivity::class.java,
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        finish()
    }


}