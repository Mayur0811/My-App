package com.messages.ui.archived.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.data.models.Conversation
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityArchivedBinding
import com.messages.extentions.canDialNumber
import com.messages.extentions.copyToClipboard
import com.messages.extentions.dialNumber
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.ui.archived.adapter.ArchivedAdapter
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.KEY_PHONE
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArchivedActivity : BaseActivity() {

    private val binding by lazy { ActivityArchivedBinding.inflate(layoutInflater) }
    private var archivedAdapter = ArchivedAdapter()
    private var dialNumber = ""

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initActions()
        initAdapter()
    }

    private fun initToolbar() {
        binding.toolbar.apply {
            getBinding().tvToolbarTitle.text = getStringValue(R.string.archived)
            setSupportActionBar(this)
        }
    }

    private fun initData() {
        backgroundScope.launch {
            val conversations = messageRepository.getArchivedConversations()
            if (conversations.isEmpty()) {
                mainScope.launch {
                    binding.apply {
                        rcvArchive.isVisible = false
                        clNoConversation.isVisible = true
                    }
                }
            } else {
                loadConversationToAdapter(conversations)
            }
        }

    }

    private fun initAdapter() {
        binding.rcvArchive.apply {
            adapter = archivedAdapter
            initData()
        }
    }

    private fun loadConversationToAdapter(conversations: ArrayList<Conversation>) {
        backgroundScope.launch {
            mainScope.launch {
                conversations.sortByDescending { it.isPined }
                archivedAdapter.updateConversations(conversations)
                binding.apply {
                    rcvArchive.isVisible = true
                    clNoConversation.isVisible = false
                }
            }
        }

    }


    private fun initActions() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                finish()
            }
            toolbar.getBinding().ivActionToolbarBack.setOnSafeClickListener {
                archivedAdapter.stopSelectionMode()
                binding.toolbar.stopActionMode()
            }
        }

        archivedAdapter.apply {
            onClickConversation = { conversation ->
                if (binding.toolbar.isActionMode) {
                    updateToolbar()
                } else {
                    launchActivity(ChatActivity::class.java) {
                        putLong(THREAD_ID, conversation.threadId)
                        putString(CONVERSATION_TITLE, conversation.name)
                    }
                }
            }

            onClickCopyOTP = { otp ->
                if (otp.isNotEmpty()) {
                    copyToClipboard(otp)
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
            if (archivedAdapter.selection.isEmpty()) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${archivedAdapter.selection.size}"
                refreshMenu()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menuInflater.inflate(R.menu.home_menu_action, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (binding.toolbar.isActionMode) {
            menu?.apply {
                val conversations = archivedAdapter.selectedConversation

                findItem(R.id.menuUnarchive).isVisible = true
                findItem(R.id.menuArchive).isVisible = false
                findItem(R.id.manuPin).isVisible = conversations.any { !it.isPined }
                findItem(R.id.menuUnpin).isVisible = conversations.all { it.isPined }

                val canDialNumber = canDialNumber(conversations.first().recipients as? ArrayList)
                dialNumber = canDialNumber.second

                findItem(R.id.menuAddContact).isVisible =
                    archivedAdapter.selection.size == 1 && canDialNumber.first && conversations.first().recipients.any { it.contact == null }
                findItem(R.id.menuDial).isVisible =
                    archivedAdapter.selection.size == 1 && canDialNumber.first

                findItem(R.id.menuMarkAsRead).isVisible = conversations.none { it.read }
                findItem(R.id.menuMarkAsUnread).isVisible = conversations.all { it.read }
                findItem(R.id.menuCopyTo).isVisible = archivedAdapter.selection.size == 1

            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.manuPin -> {
                handlePinedConversations(true)
            }

            R.id.menuUnpin -> {
                handlePinedConversations(false)
            }

            R.id.menuUnarchive -> {
                val selectedConversationIds =
                    archivedAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForArchived(selectedConversationIds, false)
                archivedAdapter.archivedConversation.removeAll(archivedAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                archivedAdapter.stopSelectionMode()
                mainScope.launch {
                    binding.apply {
                        if (archivedAdapter.archivedConversation.size == 0) {
                            rcvArchive.isVisible = false
                            clNoConversation.isVisible = true
                        }
                    }
                }
            }

            R.id.menuPrivate -> {
                val selectedConversationIds =
                    archivedAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForPrivate(selectedConversationIds, true)
                archivedAdapter.archivedConversation.removeAll(archivedAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                archivedAdapter.stopSelectionMode()
            }

            R.id.menuHomeDelete -> {
                deleteConversation()
            }

            R.id.menuAddContact -> {
                if (archivedAdapter.selectedConversation.isNotEmpty()) {
                    val phoneNo =
                        archivedAdapter.selectedConversation.first().recipients.first().address
                    Intent().apply {
                        action = Intent.ACTION_INSERT_OR_EDIT
                        type = "vnd.android.cursor.item/contact"
                        putExtra(KEY_PHONE, phoneNo)
                        startActivity(this)
                    }
                }
            }

            R.id.menuMarkAsRead -> {
                handleMarkReadUnreadConversation(true)
            }

            R.id.menuMarkAsUnread -> {
                handleMarkReadUnreadConversation(false)
            }

            R.id.menuBlock -> {
                blockConversationAddress()
            }

            R.id.menuCopyTo -> {
                if (archivedAdapter.selectedConversation.isNotEmpty()) {
                    copyToClipboard(archivedAdapter.selectedConversation.first().snippet)
                    binding.toolbar.stopActionMode()
                    archivedAdapter.stopSelectionMode()
                }
            }

            R.id.menuDial -> {
                if (archivedAdapter.selectedConversation.isNotEmpty()) {
                    binding.toolbar.stopActionMode()
                    archivedAdapter.stopSelectionMode()
                    dialNumber(dialNumber)
                }
            }

        }

        return super.onOptionsItemSelected(item)
    }

    private fun handlePinedConversations(isPined: Boolean) {
        val selectedConversationId = archivedAdapter.selectedConversation.map { it.threadId }

        /* val pinedConversation = appPreferences.pinedConversation.fromJson<ArrayList<Long>>()
         if (isPined) {
             selectedConversationId.filter {
                 !pinedConversation.contains(
                     it
                 )
             }.let { pinedConversation.addAll(it) }
         } else {
             pinedConversation.removeAll(selectedConversationId.toSet())
         }
         appPreferences.pinedConversation = pinedConversation.toJson()*/

        binding.toolbar.stopActionMode()
        archivedAdapter.stopSelectionMode()
        messageRepository.updateConversationPinedData(selectedConversationId, isPined)
        loadConversationToAdapter(
            getConversationWithPinedData(
                archivedAdapter.archivedConversation,
                selectedConversationId, isPined
            )
        )
    }

    private fun getConversationWithPinedData(
        conversations: ArrayList<Conversation>,
        pinedConversationId: List<Long>,
        isPined: Boolean
    ): ArrayList<Conversation> {
        val pinedConversation = conversations.filter { pinedConversationId.contains(it.threadId) }
        pinedConversation.forEach { it.isPined = isPined }
        val conversation = conversations.filter { !pinedConversationId.contains(it.threadId) }
        return ArrayList(pinedConversation + conversation)
    }

    private fun deleteConversation() {
        val deleteDialog =
            CustomDialog(
                context = this,
                dialogTitle = getStringValue(R.string.delete),
                dialogDisc = getStringValue(R.string.want_to_delete_conversation),
                negativeBtnText = getStringValue(R.string.cancel),
                positiveBtnText = getStringValue(R.string.delete),
                onPositive = {
                    val conversationToRemove = archivedAdapter.selectedConversation
                    if (conversationToRemove.isNotEmpty()) {
                        val positions = archivedAdapter.selectedPos
                        messageRepository.deleteConversation(conversationToRemove.map { it.threadId })
                        positions.forEach {
                            archivedAdapter.archivedConversation.removeAt(it)
                        }
                        mainScope.launch {
                            if (archivedAdapter.archivedConversation.size == 0) {
                                binding.apply {
                                    rcvArchive.isVisible = false
                                    clNoConversation.isVisible = true
                                }
                            } else {
                                archivedAdapter.removeSelectedItems(positions)
                            }
                            binding.toolbar.stopActionMode()
                            archivedAdapter.stopSelectionMode()
                        }
                    }
                }
            )
        deleteDialog.show()
    }

    private fun handleMarkReadUnreadConversation(isRead: Boolean) {
        val selectedThreadIds = archivedAdapter.selectedConversation.map { it.threadId }
        messageRepository.markThreadMessagesReadUnRead(selectedThreadIds, isRead) {
            archivedAdapter.selectedPos.forEach {
                archivedAdapter.archivedConversation[it].read = isRead
            }
            mainScope.launch {
                archivedAdapter.stopSelectionMode()
                binding.toolbar.stopActionMode()
            }
        }
    }

    private fun blockConversationAddress() {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.block),
            dialogDisc = getStringValue(R.string.want_to_block_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.block),
            onPositive = {
                val selectedConversationIds =
                    archivedAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForBlocked(selectedConversationIds, true)
                archivedAdapter.archivedConversation.removeAll(archivedAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                archivedAdapter.stopSelectionMode()
                if (archivedAdapter.archivedConversation.size == 0) {
                    binding.apply {
                        rcvArchive.isVisible = false
                        clNoConversation.isVisible = true
                    }
                }
            })
        deleteDialog.show()
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}