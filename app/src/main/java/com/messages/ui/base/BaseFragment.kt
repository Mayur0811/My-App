package com.messages.ui.base

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.addCallback
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.messages.databinding.FragmentBaseBinding
import com.messages.extentions.getStringValue

abstract class BaseFragment<VB : ViewBinding>(private val bindingInflater: (LayoutInflater) -> VB) :
    Fragment() {

    private lateinit var baseBinding: FragmentBaseBinding

    lateinit var binding: VB

    fun updateStatusBarColor(color: Int, isDark: Boolean = false) {
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color
            invertInsets(isDark, window)
        }
    }


    @Suppress("DEPRECATION")
    fun invertInsets(darkTheme: Boolean, window: Window) {
        if (Build.VERSION.SDK_INT >= 30) {
            val statusBar = APPEARANCE_LIGHT_STATUS_BARS
            val navBar = APPEARANCE_LIGHT_NAVIGATION_BARS
            if (!darkTheme) {
                window.insetsController?.setSystemBarsAppearance(statusBar, statusBar)
                window.insetsController?.setSystemBarsAppearance(navBar, navBar)
            } else {
                window.insetsController?.setSystemBarsAppearance(0, statusBar)
                window.insetsController?.setSystemBarsAppearance(0, navBar)
            }
        } else {
            val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0

            if (!darkTheme) {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or flags
            } else {
                window.decorView.systemUiVisibility =
                    (window.decorView.systemUiVisibility.inv() or flags).inv()
            }
        }
    }


    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        baseBinding = FragmentBaseBinding.inflate(inflater, container, false)
        binding = bindingInflater.invoke(inflater)
        baseBinding.contentContainer.addView(binding.root)
        return baseBinding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAdded) { initViews() }
    }

    abstract fun initViews()

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
    }

    fun getStringValue(id: Int) = requireContext().getStringValue(id)
}