package com.messages.ui.private_chat.chat.bottom_sheet_dialog

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
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.data.events.OnAddConversationToPrivate
import com.messages.data.repository.MessageRepository
import com.messages.databinding.DialogConversationSelectBinding
import com.messages.extentions.getStringValue
import com.messages.ui.private_chat.chat.adapter.ConversationSelectAdapter
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class ConversationSelectDialog : BottomSheetDialogFragment() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private lateinit var binding: DialogConversationSelectBinding
    private var conversationAdapter = ConversationSelectAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogConversationSelectBinding.inflate(inflater, container, false)
        initAdapter()
        getConversation()
        initAction()
        updateSelectedData()
        return binding.root
    }

    private fun initAction() {
        binding.btnAddTo.setOnSafeClickListener { updateConversationToPrivate() }
    }

    private fun initAdapter() {
        binding.rcvSelectConversations.apply {
            adapter = conversationAdapter

            conversationAdapter.onClickConversation = {
                updateSelectedData()
            }

            conversationAdapter.onItemLongClick = {
                updateSelectedData()
            }
        }
    }

    private fun updateSelectedData() {
        binding.apply {
            tvSelectedConversation.text = requireContext().getString(
                R.string.add_conversation_to,
                conversationAdapter.selection.size,
                requireContext().getStringValue(R.string.private_chat)
            )
        }
    }

    private fun getConversation() {
        backgroundScope.launch {
            val allConversation = messageRepository.getConversations()
            mainScope.launch {
                conversationAdapter.updateConversations(allConversation)
            }
        }
    }

    private fun updateConversationToPrivate() {
        val selectedConversationIds = conversationAdapter.selectedConversation.map { it.threadId }
        messageRepository.handleConversationForPrivate(selectedConversationIds, true)
        conversationAdapter.conversations.removeAll(conversationAdapter.selectedConversation.toSet())
        conversationAdapter.stopSelectionMode()
        EventBus.getDefault().post(OnAddConversationToPrivate)
        dismiss()
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

