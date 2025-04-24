package com.messages.ui.chat.fragment

import android.os.Bundle
import com.messages.data.events.OnLanguageSelect
import com.messages.data.events.OnSelectClipboardData
import com.messages.databinding.FragmentClipboardDataBinding
import com.messages.extentions.getClipboardData
import com.messages.ui.base.BaseFragment
import com.messages.ui.chat.adapter.ClipboardDataAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus


@AndroidEntryPoint
class ClipboardDataFragment :
    BaseFragment<FragmentClipboardDataBinding>(FragmentClipboardDataBinding::inflate) {

    private var clipBoardDataPosition = "clipBoardDataPosition"

    override fun initViews() {
        val position = arguments?.getInt(clipBoardDataPosition)
        val clipboardData = requireContext().getClipboardData(position ?: 0)
        binding.rcvClipData.adapter = ClipboardDataAdapter(clipboardData) {
            EventBus.getDefault().post(OnSelectClipboardData(it))
        }
    }
    companion object {
        fun getInstance(position: Int): ClipboardDataFragment {
            return ClipboardDataFragment().apply {
                arguments = Bundle().apply {
                    putInt(clipBoardDataPosition, position)
                }
            }
        }
    }
}