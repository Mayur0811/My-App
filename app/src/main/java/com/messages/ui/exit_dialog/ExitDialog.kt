package com.messages.ui.exit_dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messages.databinding.ExitDialogBinding
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExitDialog(private var onClickYes: () -> Unit) : BottomSheetDialogFragment() {

    private lateinit var binding: ExitDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ExitDialogBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.apply {
            btnYes.setOnSafeClickListener { onClickYes.invoke() }
            btnNo.setOnSafeClickListener { dismiss() }
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