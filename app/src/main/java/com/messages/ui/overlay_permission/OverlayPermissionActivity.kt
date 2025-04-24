package com.messages.ui.overlay_permission

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.mainScope
import com.messages.databinding.ActivityOverlayPermissionBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.toast
import com.messages.ui.base.BaseActivity
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.overlay_permission.dialog.OverlayDialogActivity
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OverlayPermissionActivity : BaseActivity() {

    private val binding by lazy { ActivityOverlayPermissionBinding.inflate(layoutInflater) }


    private val overlayLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            mainScope.launch {
                if (permissionManager.hasOverlay()) {
                    launchActivity(HomeActivity::class.java)
                    finish()
                } else {
                    toast(msg = getStringValue(R.string.overlay_permission_denied))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateStatusBarColor(R.color.app_bg)

        binding.btnEnablePermission.setOnSafeClickListener {
            if (!permissionManager.hasOverlay()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayLauncher.launch(intent)
                mainScope.launch {
                    delay(1000)
                    launchActivity(OverlayDialogActivity::class.java)
                }
            } else {
                launchActivity(
                    HomeActivity::class.java,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                )
                finish()
            }
        }

    }
}