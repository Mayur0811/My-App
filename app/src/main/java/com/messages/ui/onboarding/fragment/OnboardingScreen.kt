package com.messages.ui.onboarding.fragment

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.messages.R
import com.messages.databinding.FragmentOnboardingSceenBinding
import com.messages.extentions.loadImageDrawable
import com.messages.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingScreen : BaseFragment<FragmentOnboardingSceenBinding>(
    FragmentOnboardingSceenBinding::inflate
) {

    private var positionKey = "position"

    override fun initViews() {
        val position = arguments?.getInt(positionKey)

        binding.apply {
            when (position) {
                0 -> {
                    tvOnBoardingTitle.text = getStringValue(R.string.first_onboarding_title)
                    tvOnBoardingMsg.text = getStringValue(R.string.first_onboarding_msg)
                    ivOnBoarding.loadImageDrawable(R.drawable.onboarding_first)
                }

                1 -> {
                    tvOnBoardingTitle.text = getStringValue(R.string.second_onboarding_title)
                    tvOnBoardingMsg.text = getStringValue(R.string.second_onboarding_msg)
                    ivOnBoarding.loadImageDrawable(R.drawable.onboarding_seond)
                }

                else -> {
                    tvOnBoardingTitle.text = getStringValue(R.string.third_onboarding_title)
                    tvOnBoardingMsg.text = getStringValue(R.string.third_onboarding_msg)
                    ivOnBoarding.loadImageDrawable(R.drawable.onboarding_third)
                }
            }
        }
    }

    companion object {
        fun getInstance(position: Int): OnboardingScreen {
            return OnboardingScreen().apply {
                arguments = Bundle().apply {
                    putInt(positionKey, position)
                }
            }
        }
    }

}