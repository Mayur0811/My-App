package com.messages.ui.chat.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.messages.data.events.OnLanguageSelect
import com.messages.databinding.TranslateBottomSheetDialogBinding
import com.messages.ui.chat.adapter.TranslateLanguageAdapter
import com.messages.ui.chat.viewmodel.TranslateViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.util.ArrayList

@AndroidEntryPoint
class TranslateBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var binding: TranslateBottomSheetDialogBinding
    private val viewModel by viewModels<TranslateViewModel>()
    private val translateLanguageAdapter = TranslateLanguageAdapter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TranslateBottomSheetDialogBinding.inflate(inflater, container, false)
        initAdapter()
        initObserver()
        return binding.root
    }


    private fun initAdapter() {
        binding.rcvTranslateLanguages.apply {
            adapter = translateLanguageAdapter
            translateLanguageAdapter.updateData(viewModel.availableLanguages)
            translateLanguageAdapter.onClickLanguage = {
                EventBus.getDefault().post(OnLanguageSelect(it.code))
                dismiss()
            }
        }
    }

    private fun initObserver() {
        binding.apply {
            evSearchLanguage.addTextChangedListener {
                if (!evSearchLanguage.text.isNullOrEmpty() && evSearchLanguage.text.toString()
                        .trim().length >= 2
                ) {
                    searchLanguage(evSearchLanguage.text.toString().trim())
                } else {
                    translateLanguageAdapter.updateData(viewModel.availableLanguages)
                }
            }
        }
    }

    private fun searchLanguage(searchText: String) {
        val searchList = viewModel.availableLanguages.filter { it.displayName.contains(searchText,true) }
        translateLanguageAdapter.updateData(ArrayList(searchList))
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