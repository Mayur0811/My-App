package com.messages.ui.onboarding.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.messages.R
import com.messages.databinding.ActivityOnBoardingBinding
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.onboarding.adapter.OnBoardingAdapter
import com.messages.ui.permission.PermissionAskActivity
import com.messages.utils.PERMISSION_VIEW
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class OnBoardingActivity : BaseActivity() {

    private val binding by lazy { ActivityOnBoardingBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateStatusBarColor(R.color.app_bg)

        initView()
        initActions()

    }

    private fun initView() {
        binding.apply {
            viewPager.adapter = OnBoardingAdapter(this@OnBoardingActivity)
            dotsIndicator.attachTo(viewPager)
        }
    }

    private fun initActions() {
        binding.apply {
            tvSkip.setOnSafeClickListener {
                moveToHome()
            }

            tvBtnText.setOnSafeClickListener {
                btnNext.setPressed(true)
                btnNext.performClick()
                btnNext.postDelayed({
                    btnNext.isPressed = false
                }, 200)
            }

            ivBtnImage.setOnSafeClickListener {
                btnNext.setPressed(true)
                btnNext.performClick()
                btnNext.postDelayed({
                    btnNext.isPressed = false
                }, 200)
            }

            btnNext.setOnSafeClickListener {
                when (viewPager.currentItem) {
                    0 -> {
                        tvSkip.isVisible = true
                        viewPager.setCurrentItem(1, true)
                    }

                    1 -> {
                        viewPager.setCurrentItem(2, true)
                    }

                    else -> {
                        moveToHome()
                    }
                }
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tvSkip.isVisible = position != 2
                }

            })
        }
    }

    private fun moveToHome() {
        appPreferences.isShowOnBoarding = true
        if (permissionManager.isDefaultSMS()) {
            launchActivity(HomeActivity::class.java, Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            launchActivity(PermissionAskActivity::class.java, Intent.FLAG_ACTIVITY_CLEAR_TOP) {
                putInt(PERMISSION_VIEW, checkPermission())
            }
        }
        finish()
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