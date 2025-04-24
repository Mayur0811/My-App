package com.messages.ui.spam_blocked

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.data.events.HandleConversation
import com.messages.data.events.MessageEvent
import com.messages.data.events.UpdateConversation
import com.messages.data.models.Conversation
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivitySpamBlockedBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class SpamBlockedActivity : BaseActivity() {

    private val binding by lazy { ActivitySpamBlockedBinding.inflate(layoutInflater) }
    private val blockNumberAdapter = BlockNumberAdapter()
    private var selectedConversation = ArrayList<Conversation>()
    private var isUpdateConversation = false

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initActions()
        initAdapter()
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isUpdateConversation) {
                    EventBus.getDefault().postSticky(UpdateConversation)
                }
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this@SpamBlockedActivity, callback)
    }

    private fun initAdapter() {
        binding.rcvBlocked.apply {
            adapter = blockNumberAdapter
            getBlockedConversation()
        }
    }

    private fun getBlockedConversation() {
        backgroundScope.launch {
            val conversation = messageRepository.getBlockedConversations()
            mainScope.launch {
                binding.apply {
                    if (conversation.isEmpty()) {
                        rcvBlocked.isVisible = false
                        clNoConversation.isVisible = true
                    } else {
                        blockNumberAdapter.updateConversations(conversation)
                        rcvBlocked.isVisible = true
                        clNoConversation.isVisible = false
                    }
                }
            }
        }
    }

    private fun initToolbar() {
        binding.toolbar.apply {
            getBinding().tvToolbarTitle.text = getStringValue(R.string.blocked_number)
            setSupportActionBar(this)
        }
    }

    private fun initActions() {
        binding.toolbar.getBinding().apply {
            ivToolbarBack.setOnSafeClickListener {
                finish()
            }
            ivActionToolbarBack.setOnSafeClickListener {
                blockNumberAdapter.stopSelectionMode()
                binding.toolbar.stopActionMode()
            }
        }

        blockNumberAdapter.apply {
            onClickConversation = { conversation ->
                if (binding.toolbar.isActionMode) {
                    updateToolbar()
                } else {
                    selectedConversation = arrayListOf(conversation)
                    launchActivity(ChatActivity::class.java) {
                        putLong(THREAD_ID, conversation.threadId)
                        putString(CONVERSATION_TITLE, conversation.name)
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
            if (blockNumberAdapter.selection.isEmpty()) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${blockNumberAdapter.selection.size}"
                selectedConversation = blockNumberAdapter.selectedConversation
                refreshMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menuInflater.inflate(R.menu.menu_block, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menu?.apply {
                findItem(R.id.menuBlockDelete).isVisible = true
                findItem(R.id.menuUnblock).isVisible = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuBlockDelete -> {
                deleteConversation()
            }

            R.id.menuUnblock -> {
                unblockConversationAddress()
            }
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
                val conversationToRemove = blockNumberAdapter.selectedConversation
                if (conversationToRemove.isNotEmpty()) {
                    val positions = blockNumberAdapter.selectedPos
                    messageRepository.deleteConversation(conversationToRemove.map { it.threadId })
                    positions.forEach {
                        blockNumberAdapter.conversations.removeAt(it)
                    }
                    mainScope.launch {
                        if (blockNumberAdapter.conversations.size == 0) {
                            binding.apply {
                                rcvBlocked.isVisible = false
                                clNoConversation.isVisible = true
                            }
                        } else {
                            blockNumberAdapter.removeSelectedItems(positions)
                        }
                        binding.toolbar.stopActionMode()
                        blockNumberAdapter.stopSelectionMode()
                    }
                }
            })
        deleteDialog.show()
    }

    private fun unblockConversationAddress() {
        mainScope.launch {
            val selectedConversationIds =
                blockNumberAdapter.selectedConversation.map { it.threadId }
            backgroundScope.launch {
                messageRepository.handleConversationForBlocked(selectedConversationIds, false)
            }
            blockNumberAdapter.conversations.removeAll(selectedConversation.toSet())
            binding.toolbar.stopActionMode()
            blockNumberAdapter.stopSelectionMode()
            if (blockNumberAdapter.conversations.size == 0) {
                binding.apply {
                    rcvBlocked.isVisible = false
                    clNoConversation.isVisible = true
                }
            }
            isUpdateConversation = true
        }
    }

    override fun onGetEvent(event: MessageEvent) {
        if (event is HandleConversation) {
            getBlockedConversation()
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}