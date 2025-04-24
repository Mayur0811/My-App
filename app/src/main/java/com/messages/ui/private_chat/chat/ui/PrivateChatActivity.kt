package com.messages.ui.private_chat.chat.ui

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.common_utils.ItemDecorator
import com.messages.common.mainScope
import com.messages.data.events.MessageEvent
import com.messages.data.events.OnAddConversationToPrivate
import com.messages.data.events.OnReceiveMessageConversation
import com.messages.data.events.RefreshConversation
import com.messages.data.events.UpdateConversation
import com.messages.data.models.Conversation
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityPrivateChatBinding
import com.messages.extentions.canDialNumber
import com.messages.extentions.copyToClipboard
import com.messages.extentions.dialNumber
import com.messages.extentions.getActionBG
import com.messages.extentions.getActionDrawableId
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.getSwipeDirection
import com.messages.extentions.launchActivity
import com.messages.extentions.toast
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.ui.private_chat.chat.adapter.PrivateChatAdapter
import com.messages.ui.private_chat.chat.bottom_sheet_dialog.ConversationSelectDialog
import com.messages.ui.private_chat.private_setting.PrivateSettingActivity
import com.messages.ui.swipe_action.SwipeAction
import com.messages.utils.AppLogger
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.HOME_ACTIVITY
import com.messages.utils.KEY_PHONE
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@AndroidEntryPoint
class PrivateChatActivity : BaseActivity() {

    private val binding by lazy { ActivityPrivateChatBinding.inflate(layoutInflater) }
    private var privateChatAdapter = PrivateChatAdapter()
    private var dialNumber = ""
    private var isUpdateConversation = false

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initData()
        initAdapter()
        initActions()
        handleBackPress()

    }

    private fun handleBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isUpdateConversation) {
                    EventBus.getDefault().postSticky(UpdateConversation)
                }
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }


    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.private_chat)
            ivToolbarAction.isVisible = true
            setSupportActionBar(binding.toolbar)
        }
    }

    private fun initActions() {
        binding.apply {
            toolbar.getBinding().apply {
                ivToolbarBack.setOnSafeClickListener {
                    onBackPressedDispatcher.onBackPressed()
                }
                ivToolbarAction.setOnSafeClickListener {
                    launchActivity(PrivateSettingActivity::class.java)
                }
                ivActionToolbarBack.setOnSafeClickListener {
                    privateChatAdapter.stopSelectionMode()
                    binding.toolbar.stopActionMode()
                }
            }

            fbStartPrivateChat.setOnSafeClickListener {
                ConversationSelectDialog().apply {
                    show(supportFragmentManager, ConversationSelectDialog::class.simpleName)
                }
            }
        }

        privateChatAdapter.apply {
            onClickConversation = { conversation, position ->
                if (binding.toolbar.isActionMode) {
                    updateToolbar()
                } else {
                    backgroundScope.launch {
                        messageRepository.markThreadMessagesReadUnRead(
                            listOf(conversation.threadId), true
                        ) {
                            mainScope.launch {
                                privateChatAdapter.conversations[position].read = true
                                privateChatAdapter.notifyItemChanged(position)
                            }
                        }
                    }
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
            if (privateChatAdapter.selection.isEmpty()) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${privateChatAdapter.selection.size}"
                refreshMenu()
            }
        }
    }

    private fun initAdapter() {
        binding.rcvPrivate.apply {
            adapter = privateChatAdapter
            attachSwipeToRecycleView()
        }
    }

    private fun attachSwipeToRecycleView() {
        val simpleCallback =
            object : ItemTouchHelper.SimpleCallback(
                0,
                getSwipeDirection(appPreferences, HOME_ACTIVITY)
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val rightActionDrawable = getActionDrawableId(appPreferences.rightSwipeAction)
                    val leftActionDrawable = getActionDrawableId(appPreferences.leftSwipeAction)

                    val defaultWhiteColor = getColorForId(R.color.only_white)

                    ItemDecorator.Builder(c, recyclerView, viewHolder, dX, actionState).set(
                        backgroundColorFromStartToEnd = getActionBG(appPreferences.rightSwipeAction),
                        backgroundColorFromEndToStart = getActionBG(appPreferences.leftSwipeAction),
                        iconTintColorFromStartToEnd = defaultWhiteColor,
                        iconTintColorFromEndToStart = defaultWhiteColor,
                        iconResIdFromStartToEnd = rightActionDrawable,
                        iconResIdFromEndToStart = leftActionDrawable
                    )

                    super.onChildDraw(
                        c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                    )
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    when (direction) {
                        ItemTouchHelper.LEFT -> performSwipeAction(
                            appPreferences.leftSwipeAction,
                            position
                        )

                        ItemTouchHelper.RIGHT -> performSwipeAction(
                            appPreferences.rightSwipeAction,
                            position
                        )
                    }
                }
            }
        ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.rcvPrivate)
    }

    private fun performSwipeAction(swipeAction: Int, position: Int) {
        when (getStringValue(SwipeAction.fromPosition(swipeAction).stringResId)) {
            getStringValue(R.string.action_archive) -> {
                messageRepository.handleConversationForArchived(
                    arrayListOf(privateChatAdapter.conversations[position].threadId), true
                )
                privateChatAdapter.conversations.removeAt(position)
                privateChatAdapter.notifyItemRemoved(position)
            }

            getStringValue(R.string.action_delete) -> {
                deleteConversation(isFromAction = true, position = position)
            }

            getStringValue(R.string.action_call) -> {
                val canDialNumber =
                    canDialNumber(privateChatAdapter.conversations[position].recipients as? ArrayList)
                if (canDialNumber.first) {
                    dialNumber(canDialNumber.second)
                } else {
                    toast(getStringValue(R.string.sender_does_not_support_call))
                }
            }

            getStringValue(R.string.action_mark_read) -> {
                handleMarkReadUnreadConversation(
                    isRead = true,
                    isFromSwipeAction = true,
                    position
                )
            }

            getStringValue(R.string.action_mark_unread) -> {
                handleMarkReadUnreadConversation(
                    isRead = false,
                    isFromSwipeAction = true,
                    position
                )
            }

            getStringValue(R.string.action_add_private) -> {
                messageRepository.handleConversationForPrivate(
                    arrayListOf(privateChatAdapter.conversations[position].threadId), true
                )
                privateChatAdapter.conversations.removeAt(position)
                privateChatAdapter.notifyItemRemoved(position)
            }

            getStringValue(R.string.action_remove_private) -> {
                privateChatAdapter.notifyItemChanged(position)
            }

            else -> {
                privateChatAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun initData() {
        backgroundScope.launch {
            val conversations = messageRepository.getPrivateConversations()
            if (conversations.size == 0) {
                mainScope.launch {
                    binding.apply {
                        rcvPrivate.isVisible = false
                        clNoPrivateConversation.isVisible = true
                    }
                }
            } else {
                loadConversationToAdapter(conversations)
            }
        }

    }

    private fun loadConversationToAdapter(conversations: ArrayList<Conversation>) {
        backgroundScope.launch {
            conversations.sortByDescending { it.date }
            mainScope.launch {
                privateChatAdapter.updateConversations(conversations)
                binding.rcvPrivate.isVisible = true
                binding.clNoPrivateConversation.isVisible = false
            }
        }
    }


    private fun deleteConversation(isFromAction: Boolean = false, position: Int = 0) {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.delete),
            dialogDisc = getStringValue(R.string.want_to_delete_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.delete),
            onPositive = {
                if (isFromAction) {
                    messageRepository.deleteConversation(listOf(privateChatAdapter.conversations[position].threadId))
                    privateChatAdapter.conversations.removeAt(position)
                    privateChatAdapter.notifyItemRemoved(position)
                } else {
                    val conversationToRemove = privateChatAdapter.selectedConversation
                    if (conversationToRemove.isNotEmpty()) {
                        val positions = privateChatAdapter.selectedPos
                        messageRepository.deleteConversation(conversationToRemove.map { it.threadId })
                        positions.forEach {
                            privateChatAdapter.conversations.removeAt(it)
                        }
                        mainScope.launch {
                            if (privateChatAdapter.conversations.size == 0) {
                                finish()
                            } else {
                                privateChatAdapter.removeSelectedItems(positions)
                            }
                            binding.toolbar.stopActionMode()
                            privateChatAdapter.stopSelectionMode()
                        }
                    }
                }
            })
        deleteDialog.show()
    }

    private fun handleMarkReadUnreadConversation(
        isRead: Boolean,
        isFromSwipeAction: Boolean = false,
        position: Int = 0
    ) {
        val selectedThreadIds =
            if (isFromSwipeAction) listOf(privateChatAdapter.conversations[position].threadId) else privateChatAdapter.selectedConversation.map { it.threadId }
        messageRepository.markThreadMessagesReadUnRead(selectedThreadIds, isRead) {
            if (isFromSwipeAction) {
                mainScope.launch {
                    privateChatAdapter.apply {
                        conversations[position].read = isRead
                        notifyItemChanged(position)
                    }
                }
            } else {
                privateChatAdapter.selectedPos.forEach {
                    privateChatAdapter.conversations[it].read = isRead
                }
                mainScope.launch {
                    privateChatAdapter.stopSelectionMode()
                    binding.toolbar.stopActionMode()
                }
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
                val conversations = privateChatAdapter.selectedConversation

                findItem(R.id.manuPin).isVisible = conversations.any { !it.isPined }
                findItem(R.id.menuUnpin).isVisible = conversations.all { it.isPined }

                val canDialNumber = canDialNumber(conversations.first().recipients as? ArrayList)
                dialNumber = canDialNumber.second

                findItem(R.id.menuAddContact).isVisible =
                    privateChatAdapter.selection.size == 1 && canDialNumber.first && conversations.first().recipients.any { it.contact == null }
                findItem(R.id.menuDial).isVisible =
                    privateChatAdapter.selection.size == 1 && canDialNumber.first

                findItem(R.id.menuMarkAsRead).isVisible = conversations.none { it.read }
                findItem(R.id.menuMarkAsUnread).isVisible = conversations.all { it.read }
                findItem(R.id.menuCopyTo).isVisible = privateChatAdapter.selection.size == 1

                findItem(R.id.menuPrivate).isVisible = false
                findItem(R.id.menuUnPrivate).isVisible = true

            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateUI() {
        mainScope.launch {
            binding.apply {
                if (privateChatAdapter.conversations.size == 0) {
                    rcvPrivate.isVisible = false
                    clNoPrivateConversation.isVisible = true
                }
            }
        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.manuPin -> {
                handlePinedConversations(true)
            }

            R.id.menuUnpin -> {
                handlePinedConversations(false)
            }

            R.id.menuArchive -> {
                val selectedConversationIds =
                    privateChatAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForArchived(selectedConversationIds, true)
                privateChatAdapter.conversations.removeAll(privateChatAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                privateChatAdapter.stopSelectionMode()
                updateUI()
            }

            R.id.menuUnPrivate -> {
                val selectedConversationIds =
                    privateChatAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForPrivate(selectedConversationIds, false)
                privateChatAdapter.conversations.removeAll(privateChatAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                privateChatAdapter.stopSelectionMode()
                updateUI()
                isUpdateConversation = true
            }

            R.id.menuHomeDelete -> {
                deleteConversation()
            }

            R.id.menuAddContact -> {
                if (privateChatAdapter.selectedConversation.isNotEmpty()) {
                    val phoneNo =
                        privateChatAdapter.selectedConversation.first().recipients.first().address
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
                if (privateChatAdapter.selectedConversation.isNotEmpty()) {
                    copyToClipboard(privateChatAdapter.selectedConversation.first().snippet)
                    binding.toolbar.stopActionMode()
                    privateChatAdapter.stopSelectionMode()
                }
            }

            R.id.menuDial -> {
                if (privateChatAdapter.selectedConversation.isNotEmpty()) {
                    binding.toolbar.stopActionMode()
                    privateChatAdapter.stopSelectionMode()
                    dialNumber(dialNumber)
                }
            }

        }

        return super.onOptionsItemSelected(item)
    }


    private fun handlePinedConversations(isPined: Boolean) {
        val selectedConversationId = privateChatAdapter.selectedConversation.map { it.threadId }

        /*  val pinedConversation = appPreferences.pinedConversation.fromJson<ArrayList<Long>>()
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
        privateChatAdapter.stopSelectionMode()
        messageRepository.updateConversationPinedData(selectedConversationId, isPined)

        mainScope.launch {
            loadConversationToAdapter(
                getConversationWithPinedData(
                    privateChatAdapter.conversations,
                    selectedConversationId, isPined
                )
            )
        }
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

    private fun blockConversationAddress() {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.block),
            dialogDisc = getStringValue(R.string.want_to_block_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.block),
            onPositive = {
                val selectedConversationIds =
                    privateChatAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForBlocked(selectedConversationIds, true)
                privateChatAdapter.conversations.removeAll(privateChatAdapter.selectedConversation.toSet())
                binding.toolbar.stopActionMode()
                privateChatAdapter.stopSelectionMode()
            })
        deleteDialog.show()
    }

    override fun onGetEvent(event: MessageEvent) {
        when (event) {
            OnAddConversationToPrivate -> {
                initData()
                isUpdateConversation = true
            }

            is RefreshConversation -> {
                try {
                    backgroundScope.launch {
                        val conversation =
                            privateChatAdapter.conversations.find { it.threadId == event.threadId }
                        if (conversation == null) {
                            messageRepository.syncRecipients {
                                messageRepository.syncConversation {
                                    initData()
                                }
                            }
                        } else {
                            val index = privateChatAdapter.conversations.indexOf(conversation)
                            messageRepository.syncSelectedConversation(event.threadId) { newConversation, isDelete ->
                                mainScope.launch {
                                    if (isDelete && newConversation == null) {
                                        privateChatAdapter.conversations.removeAt(index)
                                        privateChatAdapter.notifyItemRemoved(index)
                                    } else {

                                        privateChatAdapter.updateConversation(
                                            index,
                                            newConversation!!
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("1234", "RefreshConversation -> ${e.message}")
                }
                EventBus.getDefault().removeStickyEvent(event)
            }

            is OnReceiveMessageConversation -> {
                backgroundScope.launch {
                    val conversations =
                        privateChatAdapter.conversations.filter { it.threadId == event.message.threadId }
                    if (conversations.isNotEmpty()) {
                        messageRepository.updateConversationData(
                            event.message.threadId,
                            event.message.body,
                            event.message.date
                        )
                        val index = privateChatAdapter.conversations.indexOf(conversations.first())
                        val newConversation = conversations.first().apply {
                            snippet =
                                if (event.message.isMMS) getStringValue(R.string.attachment) else event.message.body
                            date = event.message.date
                            AppLogger.d("1234", "unreadCount -> $unreadCount")
                            if (!event.isFromChat) {
                                unreadCount += 1
                                read = event.message.read
                            } else {
                                unreadCount = 0
                                read = true
                            }
                            simSlot = event.message.simSlot
                            subId = event.message.subId
                        }
                        mainScope.launch {
                            privateChatAdapter.updateConversation(index, newConversation)
                        }
                    }
                }
                EventBus.getDefault().removeStickyEvent(event)
            }

            else -> {}
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }

}