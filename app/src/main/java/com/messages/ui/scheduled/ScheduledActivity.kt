package com.messages.ui.scheduled

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.cancelScheduleSendPendingIntent
import com.messages.common.message_utils.sendMessageCompat
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityScheduledBinding
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.extentions.toast
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.ui.scheduled.adapter.ScheduleAdapter
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledActivity : BaseActivity() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private val binding by lazy { ActivityScheduledBinding.inflate(layoutInflater) }
    private var scheduleAdapter = ScheduleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initActions()
        initAdapter()
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.scheduled)
            ivToolbarAction.isVisible = false
            ivToolbarAction.loadImageDrawable(R.drawable.ic_add)
        }
        setSupportActionBar(binding.toolbar)
    }

    private fun initAdapter() {
        binding.rcvSchedule.apply {
            adapter = scheduleAdapter
            initData()
        }
    }

    private fun initData() {
        backgroundScope.launch {
            val scheduleConversations = messageRepository.getScheduleConversationWithMessages()
            mainScope.launch {
                binding.apply {
                    if (scheduleConversations.isEmpty()) {
                        rcvSchedule.isVisible = false
                        clNoConversation.isVisible = true
                    } else {
                        scheduleAdapter.updateConversations(scheduleConversations)
                        rcvSchedule.isVisible = true
                        clNoConversation.isVisible = false
                    }
                }
            }
            messageRepository.updateScheduleConversationByThreadId(scheduleConversations) {
                val conversation = messageRepository.getScheduleConversationWithMessages()
                mainScope.launch {
                    scheduleAdapter.updateConversations(conversation)
                }
            }
        }

    }

    private fun initActions() {
        binding.toolbar.getBinding().apply {
            ivToolbarBack.setOnSafeClickListener {
                finish()
            }
            ivActionToolbarBack.setOnSafeClickListener {
                scheduleAdapter.stopSelectionMode()
                binding.toolbar.stopActionMode()
            }
        }

        scheduleAdapter.apply {
            onClickConversation = { conversation ->
                if (binding.toolbar.isActionMode) {
                    updateToolbar()
                } else {
                    launchActivity(ChatActivity::class.java) {
                        putLong(THREAD_ID, conversation.conversation.threadId)
                        putString(CONVERSATION_TITLE, conversation.conversation.name)
                    }
                }
            }

            onItemLongClick = {
                binding.toolbar.startActionMode()
                updateToolbar()
            }
        }

    }

    private fun updateToolbar() {
        binding.toolbar.apply {
            if (scheduleAdapter.selection.isEmpty()) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${scheduleAdapter.selection.size}"
                refreshMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menuInflater.inflate(R.menu.schedule_menu, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menu?.apply {
                findItem(R.id.menuScheduleDelete).isVisible = true
                findItem(R.id.menuSendNow).apply {
                    isVisible = true
                    iconTintList = ColorStateList.valueOf(getColorForId(R.color.app_blue))
                }
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuScheduleDelete -> deleteConversation()
            R.id.menuSendNow -> sendNowScheduleMessage()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteConversation() {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.delete),
            dialogDisc = getStringValue(R.string.want_to_delete_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.delete),
            onPositive = {
                backgroundScope.launch {
                    val conversationToRemove = scheduleAdapter.selectedConversation
                    if (conversationToRemove.isNotEmpty()) {
                        val positions = scheduleAdapter.selectedPos
                        val messageIdToRemove = conversationToRemove.map { it.message.messageId ?: 0 }
                        messageRepository.deleteConversation(conversationToRemove.map { it.conversation.threadId })
                        messageRepository.deleteScheduleMessages(messageIdToRemove)
                        positions.forEach {
                            scheduleAdapter.conversations.removeAt(it)
                        }
                        messageIdToRemove.forEach {
                            cancelScheduleSendPendingIntent(it)
                        }
                        mainScope.launch {
                            if (scheduleAdapter.conversations.size == 0) {
                                binding.apply {
                                    rcvSchedule.isVisible = false
                                    clNoConversation.isVisible = true
                                }
                            } else {
                                scheduleAdapter.removeSelectedItems(positions)
                            }
                            binding.toolbar.stopActionMode()
                            scheduleAdapter.stopSelectionMode()
                        }
                    }
                }
            })
        deleteDialog.show()
    }

    private fun sendNowScheduleMessage() {
        val conversationToSend = scheduleAdapter.selectedConversation
        conversationToSend.forEach { scheduleConversation ->
            try {
                mainScope.launch {
                    sendMessageCompat(
                        scheduleConversation.message.body,
                        scheduleConversation.message.addresses,
                        scheduleConversation.message.subId,
                        scheduleConversation.message.attachmentWithMessageModel?.attachments
                            ?: arrayListOf()
                    )
                }
                messageRepository.deleteScheduledMessage(
                    scheduleConversation.message.messageId ?: 0
                )
                messageRepository.deleteTemporaryThreadId(scheduleConversation.conversation.threadId)
            } catch (e: Exception) {
                toast(e.message.toString())
            } catch (e: Error) {
                toast(e.localizedMessage ?: getStringValue(R.string.unknown_error_occurred))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}