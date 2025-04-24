package com.messages.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.AppJobScheduler
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.language.ui.LanguageActivity
import com.messages.ui.permission.PermissionAskActivity
import com.messages.utils.IS_FROM_SPLASH
import com.messages.utils.PERMISSION_VIEW
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        updateStatusBarColor(R.color.app_color, isDark = true)

        Handler(Looper.getMainLooper()).postDelayed({
            MessageApplication.isApplyingTheme = false
            if (!appPreferences.isShowOnBoarding) {
                launchActivity(LanguageActivity::class.java) {
                    putBoolean(IS_FROM_SPLASH, true)
                }
                finish()
            } else {
                if (permissionManager.isDefaultSMS()) {
                    MessageApplication.isApplyingTheme = true
                    AppJobScheduler().setScheduleJob(this)
                    launchActivity(
                        HomeActivity::class.java,
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                } else {
                    launchActivity(
                        PermissionAskActivity::class.java,
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    ) {
                        putInt(PERMISSION_VIEW, checkPermission())
                    }
                }
                finish()
            }
        }, 2000)
    }

    private fun checkPermission(): Int {
        return when {
            !permissionManager.isDefaultSMS() -> 0
            !permissionManager.hasReadPhoneState() -> 2
            !permissionManager.hasNotification() -> 1
            else -> -1
        }
    }
}