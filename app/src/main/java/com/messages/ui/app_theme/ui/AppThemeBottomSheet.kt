package com.messages.ui.app_theme.ui

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messages.R
import com.messages.data.pref.AppPreferences
import com.messages.databinding.AppThemeBottomSheetBinding
import com.messages.extentions.applyTheme
import com.messages.extentions.getColorForId
import com.messages.extentions.loadImageDrawable
import com.messages.utils.AppLogger
import com.messages.utils.DARK_THEME
import com.messages.utils.LIGHT_THEME
import com.messages.utils.SYSTEM_THEME
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppThemeBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: AppThemeBottomSheetBinding
    private var selectedTheme = 0

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AppThemeBottomSheetBinding.inflate(inflater, container, false)
        selectedTheme = appPreferences.appTheme
        handleThemeSelection(selectedTheme)
        initAction()
        return binding.root
    }

    private fun initAction() {
        binding.apply {
            clThemeDialogClose.setOnSafeClickListener { dismiss() }

            mcvSystemTheme.setOnSafeClickListener { handleThemeSelection(SYSTEM_THEME) }
            mcvLightTheme.setOnSafeClickListener { handleThemeSelection(LIGHT_THEME) }
            mcvDarkTheme.setOnSafeClickListener { handleThemeSelection(DARK_THEME) }

            btnThemeDone.setOnSafeClickListener {
                if (selectedTheme != appPreferences.appTheme) {
                    appPreferences.appTheme = selectedTheme
                    requireContext().applyTheme()
                }
                dismiss()
            }
        }
    }

    private fun handleThemeSelection(theme: Int) {
        binding.apply {
            val strokeSelectedColor = ColorStateList.valueOf(
                requireContext().getColorForId(R.color.app_color)
            )
            val strokeUnselectedColor = ColorStateList.valueOf(
                requireContext().getColorForId(R.color.app_white)
            )

            val selectedTextColor =  requireContext().getColorForId(R.color.app_color)
            val unselectedTextColor =  requireContext().getColorForId(R.color.app_blue)

            selectedTheme = theme
            when (theme) {
                LIGHT_THEME -> {
                    mcvSystemTheme.setStrokeColor(strokeUnselectedColor)
                    mcvLightTheme.setStrokeColor(strokeSelectedColor)
                    mcvDarkTheme.setStrokeColor(strokeUnselectedColor)

                    tvThemeSystem.setTextColor(unselectedTextColor)
                    tvThemeLight.setTextColor(selectedTextColor)
                    tvThemeDark.setTextColor(unselectedTextColor)

                    ivRadioSystemTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                    ivRadioLightTheme.loadImageDrawable(R.drawable.radio_button_checked)
                    ivRadioDarkTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                }

                DARK_THEME -> {
                    mcvSystemTheme.setStrokeColor(strokeUnselectedColor)
                    mcvLightTheme.setStrokeColor(strokeUnselectedColor)
                    mcvDarkTheme.setStrokeColor(strokeSelectedColor)

                    tvThemeSystem.setTextColor(unselectedTextColor)
                    tvThemeLight.setTextColor(unselectedTextColor)
                    tvThemeDark.setTextColor(selectedTextColor)

                    ivRadioSystemTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                    ivRadioLightTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                    ivRadioDarkTheme.loadImageDrawable(R.drawable.radio_button_checked)
                }

                else -> {
                    mcvSystemTheme.setStrokeColor(strokeSelectedColor)
                    mcvLightTheme.setStrokeColor(strokeUnselectedColor)
                    mcvDarkTheme.setStrokeColor(strokeUnselectedColor)

                    tvThemeSystem.setTextColor(selectedTextColor)
                    tvThemeLight.setTextColor(unselectedTextColor)
                    tvThemeDark.setTextColor(unselectedTextColor)

                    ivRadioSystemTheme.loadImageDrawable(R.drawable.radio_button_checked)
                    ivRadioLightTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                    ivRadioDarkTheme.loadImageDrawable(R.drawable.radio_button_unchecked)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            bottomSheet.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isDraggable = false
                behavior.isFitToContents = true
            }
        }
        return bottomSheetDialog
    }
}