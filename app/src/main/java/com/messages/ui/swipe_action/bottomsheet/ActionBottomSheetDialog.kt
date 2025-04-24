package com.messages.ui.swipe_action.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messages.R
import com.messages.data.pref.AppPreferences
import com.messages.databinding.DialogActionBottomSheetBinding
import com.messages.extentions.getStringValue
import com.messages.ui.swipe_action.adapter.SwipeActionAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActionBottomSheetDialog(
    private val isRightAction: Boolean,
    private val onPickAction: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogActionBottomSheetBinding

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogActionBottomSheetBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.apply {
            tvActionTitle.text =
                requireContext().getStringValue(if (isRightAction) R.string.right_swipe_action else R.string.left_swipe_action)
            rcvSwipeAction.adapter =
                SwipeActionAdapter(
                    actionList = resources.getStringArray(R.array.swipe_actions).toList(),
                    selectedAction = if (isRightAction) appPreferences.rightSwipeAction else appPreferences.leftSwipeAction
                ) {
                    onPickAction.invoke(it)
                    dismiss()
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