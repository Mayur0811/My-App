package com.messages.ui.onboarding.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.messages.ui.onboarding.fragment.OnboardingScreen

class OnBoardingAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingScreen.getInstance(position)
            1 -> OnboardingScreen.getInstance(position)
            else -> OnboardingScreen.getInstance(position)
        }
    }
}