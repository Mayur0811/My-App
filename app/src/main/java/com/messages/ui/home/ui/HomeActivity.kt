package com.messages.ui.home.ui

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.PermissionManager
import com.messages.common.backgroundScope
import com.messages.common.common_utils.ItemDecorator
import com.messages.common.mainScope
import com.messages.data.events.ApplyFilter
import com.messages.data.events.DataSyncDone
import com.messages.data.events.DeleteConversation
import com.messages.data.events.HandleConversation
import com.messages.data.events.MessageEvent
import com.messages.data.events.OnChangeSwipeAction
import com.messages.data.events.OnReceiveMessageConversation
import com.messages.data.events.RefreshConversation
import com.messages.data.events.RefreshMessages
import com.messages.data.events.UpdateConversation
import com.messages.data.models.Conversation
import com.messages.data.models.MainFilterModel
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityHomeBinding
import com.messages.extentions.canDialNumber
import com.messages.extentions.copyToClipboard
import com.messages.extentions.dateFormatDMY
import com.messages.extentions.dateFormatMY
import com.messages.extentions.dateFormatY
import com.messages.extentions.dialNumber
import com.messages.extentions.getActionBG
import com.messages.extentions.getActionDrawableId
import com.messages.extentions.getColorForId
import com.messages.extentions.getFilterName
import com.messages.extentions.getIntValue
import com.messages.extentions.getStringValue
import com.messages.extentions.getSwipeDirection
import com.messages.extentions.launchActivity
import com.messages.extentions.openCustomTab
import com.messages.extentions.rateApp
import com.messages.extentions.shareApp
import com.messages.extentions.toast
import com.messages.ui.app_theme.ui.AppThemeBottomSheet
import com.messages.ui.archived.ui.ArchivedActivity
import com.messages.ui.backup_restore.ui.BackupRestoreActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.ui.exit_dialog.ExitDialog
import com.messages.ui.home.adapter.ConversationAdapter
import com.messages.ui.home.adapter.MainFilterAdapter
import com.messages.ui.home.bottomsheet.FilterBottomSheet
import com.messages.ui.new_chat.ui.ContactActivity
import com.messages.ui.private_chat.pin_ui.ui.EnterPinActivity
import com.messages.ui.scheduled.ScheduledActivity
import com.messages.ui.search.ui.SearchActivity
import com.messages.ui.setting.ui.SettingActivity
import com.messages.ui.spam_blocked.SpamBlockedActivity
import com.messages.ui.swipe_action.SwipeAction
import com.messages.utils.AppLogger
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.HOME_ACTIVITY
import com.messages.utils.KEY_PHONE
import com.messages.utils.SELECTED_FILTER
import com.messages.utils.SELECTED_FILTER_DATA
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import javax.inject.Inject


@AndroidEntryPoint
class HomeActivity : BaseActivity() {

    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private var conversationAdapter = ConversationAdapter()
    private var allConversation = arrayListOf<Conversation>()
    private var selectedFilter = MainFilterAdapter.ALL
    private var appliedFilter = 0
    private var selectedFilterData: Triple<Int, Int, Pair<Long, Long>> = Triple(0, 0, Pair(0, 0))
    private var dialNumber = ""
    private var timeBack: Long = 0

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MessageApplication.isApplyingTheme = true
        setContentView(binding.root)
        updateStatusBarColor(R.color.app_bg)
        initAdapter()
        initToolbar()
        initFilterAdapter()
        initActions()
        checkPermissionAndSyncData()
        handleBackPress()
    }

    private fun handleBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.mainDrawer.isDrawerOpen(GravityCompat.START)) {
                    closeDrawer()
                } else if (binding.homeScreen.homeToolbar.isActionMode) {
                    binding.homeScreen.homeToolbar.getBinding().ivActionToolbarBack.performClick()
                } else if (appPreferences.isShowExitDialog) {
                    showExitDialog()
                } else {
                    if (System.currentTimeMillis() - timeBack > 2000) {
                        timeBack = System.currentTimeMillis()
                        toast(getStringValue(R.string.please_click_back_again_to_exit))
                        return
                    }
                    exitFromApp()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun initToolbar() {
        binding.apply {
            setSupportActionBar(homeScreen.homeToolbar)
        }
    }

    private fun initActions() {
        binding.apply {
            homeScreen.apply {
                homeToolbar.getBinding().apply {
                    clDrawerMenu.setOnSafeClickListener {
                        mainDrawer.openDrawer(GravityCompat.START)
                    }

                    clSearch.setOnSafeClickListener {
                        launchActivity(SearchActivity::class.java)
                    }

                    clFilter.setOnSafeClickListener {
                        val bottomSheetDialog = FilterBottomSheet().apply {
                            val bundle = Bundle()
                            bundle.putInt(SELECTED_FILTER, appliedFilter)
                            bundle.putSerializable(SELECTED_FILTER_DATA, selectedFilterData)
                            arguments = bundle
                        }

                        bottomSheetDialog.show(
                            supportFragmentManager, FilterBottomSheet::class.simpleName
                        )
                    }

                    ivActionToolbarBack.setOnSafeClickListener {
                        conversationAdapter.stopSelectionMode()
                        homeToolbar.stopActionMode()
                    }
                }
                fbStartChat.setOnSafeClickListener {
                    launchActivity(ContactActivity::class.java)
                }
            }

            drawer.apply {
                clInbox.setOnSafeClickListener {
                    closeDrawer()
                }

                clArchived.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(ArchivedActivity::class.java)
                }

                clSetting.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(SettingActivity::class.java)
                }

                clPrivateChat.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(EnterPinActivity::class.java)
                }
                clTheme.setOnSafeClickListener {
                    AppThemeBottomSheet().apply {
                        show(supportFragmentManager, AppThemeBottomSheet::class.simpleName)
                    }
                }

                clBackupRestore.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(BackupRestoreActivity::class.java)
                }

                clScheduled.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(ScheduledActivity::class.java)
                }

                clSpamBlocked.setOnSafeClickListener {
                    closeDrawer()
                    launchActivity(SpamBlockedActivity::class.java)
                }

                clShare.setOnSafeClickListener {
                    shareApp()
                }
                clRateApp.setOnSafeClickListener {
                    rateApp()
                }

                clPrivacyPolicy.setOnSafeClickListener {
                    openCustomTab(appPreferences.privacyPolicy)
                }
            }
        }

        conversationAdapter.apply {
            onClickConversation = { conversation, position ->
                if (binding.homeScreen.homeToolbar.isActionMode) {
                    updateToolbar()
                } else {
                    backgroundScope.launch {
                        messageRepository.markThreadMessagesReadUnRead(
                            listOf(conversation.threadId), true
                        ) {
                            mainScope.launch {
                                conversationAdapter.conversations[position].apply {
                                    read = true
                                    unreadCount = 0
                                }
                                conversationAdapter.notifyItemChanged(position)
                            }
                        }
                        cancelNotification(conversation.threadId.toInt())
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
                binding.homeScreen.homeToolbar.startActionMode()
                updateToolbar()
            }
        }
    }

    private fun updateToolbar() {
        binding.homeScreen.homeToolbar.apply {
            if (conversationAdapter.selection.isEmpty()) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${conversationAdapter.selection.size}"
                refreshMenu()
            }
        }
    }

    private fun closeDrawer() {
        binding.mainDrawer.closeDrawer(GravityCompat.START)
    }

    private fun initFilterAdapter() {
        val mainFilterList = arrayListOf(
            MainFilterModel(MainFilterAdapter.ALL, getStringValue(R.string.all)),
            MainFilterModel(MainFilterAdapter.PERSONAL, getStringValue(R.string.personal)),
            MainFilterModel(MainFilterAdapter.OTP, getStringValue(R.string.otp_s)),
            MainFilterModel(MainFilterAdapter.TRANSACTIONS, getStringValue(R.string.transactions)),
            MainFilterModel(MainFilterAdapter.OFFERS, getStringValue(R.string.offers))
        )
        binding.homeScreen.rcvMainFilter.apply {
            val mainFilterAdapter = MainFilterAdapter(mainFilterList) { filterList ->
                backgroundScope.launch {
                    appliedFilter = 0
                    selectedFilter = filterList.filterId
                    loadConversationToAdapter(getCurrentShowedConversations(filterList.filterId))
                }
            }
            adapter = mainFilterAdapter
        }
    }

    private fun checkPermissionAndSyncData() {
        if (!isSyncInProgress) {
            isSyncInProgress = true
            messageRepository.syncRecipients {
                messageRepository.syncConversation {
                    getConversation()
                }
            }
            handleContactPermission {
                messageRepository.syncContacts {
                    if (appPreferences.isRecipientSync) {
                        messageRepository.updateContactToRecipient()
                    } else {
                        messageRepository.syncRecipients {
                            appPreferences.isRecipientSync = true
                            messageRepository.updateRecipientInConversation()
                        }
                    }
                    messageRepository.syncConversation {
                        getConversation()
                    }
                }
            }
        } else {
            mainScope.launch {
                binding.homeScreen.syncProgress.isVisible = isSyncInProgress

            }
        }
    }

    private fun initAdapter() {
        binding.homeScreen.rcvConversations.apply {
            adapter = conversationAdapter
            getConversation()
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
        ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.homeScreen.rcvConversations)
    }

    private fun performSwipeAction(swipeAction: Int, position: Int) {
        when (getStringValue(SwipeAction.fromPosition(swipeAction).stringResId)) {
            getStringValue(R.string.action_archive) -> {
                messageRepository.handleConversationForArchived(
                    arrayListOf(conversationAdapter.conversations[position].threadId), true
                )
                allConversation.remove(conversationAdapter.conversations[position])
                conversationAdapter.conversations.removeAt(position)
                conversationAdapter.notifyItemRemoved(position)
            }

            getStringValue(R.string.action_delete) -> {
                deleteConversation(isFromAction = true, position = position)
            }

            getStringValue(R.string.action_call) -> {
                val canDialNumber =
                    canDialNumber(conversationAdapter.conversations[position].recipients as? ArrayList)
                if (canDialNumber.first) {
                    dialNumber(canDialNumber.second)
                } else {
                    toast(getStringValue(R.string.sender_does_not_support_call))
                    conversationAdapter.notifyItemChanged(position)
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
                    arrayListOf(conversationAdapter.conversations[position].threadId), true
                )
                allConversation.remove(conversationAdapter.conversations[position])
                conversationAdapter.conversations.removeAt(position)
                conversationAdapter.notifyItemRemoved(position)
            }

            getStringValue(R.string.action_remove_private) -> {
                conversationAdapter.notifyItemChanged(position)
            }

            else -> {
                conversationAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun getConversation() {
        backgroundScope.launch {
            allConversation = messageRepository.getConversations()
            loadConversationToAdapter(allConversation)
            delay(2000)
            messageRepository.syncMessages()
        }
    }

    private fun handleViewVisibility(isShowNoData: Boolean = false, filterName: String) {
        mainScope.launch {
            binding.homeScreen.apply {
                clNoConversation.isVisible = isShowNoData
                rcvConversations.isVisible = !isShowNoData
                tvEmptyPrivateTitle.text =
                    getString(R.string.empty_filter_conversation_title, filterName)
            }
        }
    }

    private fun getCurrentShowedConversations(filter: Int = MainFilterAdapter.ALL): ArrayList<Conversation> {
        return when (filter) {
            MainFilterAdapter.ALL -> allConversation
            MainFilterAdapter.PERSONAL -> allConversation.filter { it.type == 1 } as ArrayList
            MainFilterAdapter.TRANSACTIONS -> allConversation.filter { it.type == 2 } as ArrayList
            MainFilterAdapter.OTP -> allConversation.filter { it.type == 3 } as ArrayList
            MainFilterAdapter.OFFERS -> allConversation.filter { it.type == 4 } as ArrayList
            else -> allConversation
        }
    }

    private fun loadConversationToAdapter(conversations: ArrayList<Conversation>) {
        mainScope.launch {
            if (conversations.isNotEmpty()) {
                conversations.sortByDescending { it.date }
                conversationAdapter.updateConversations(conversations)
                binding.homeScreen.syncProgress.visibility = View.GONE
                handleViewVisibility(false, getStringValue(R.string.all))
            } else {
                handleViewVisibility(true, getFilterName(selectedFilter))
            }
        }
    }


    override fun onGetPermission(permission: Int, isGranted: Boolean) {
        when (permission) {
            PermissionManager.PERMISSION_READ_SMS -> {
                messageRepository.syncConversation {
                    getConversation()
                }
            }

            PermissionManager.PERMISSION_READ_CONTACTS -> {
                messageRepository.syncContacts {
                    handleSMSPermission {
                        messageRepository.syncConversation {
                            getConversation()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (binding.homeScreen.homeToolbar.isActionMode) {
            menuInflater.inflate(R.menu.home_menu_action, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (binding.homeScreen.homeToolbar.isActionMode) {
            menu?.apply {
                val conversations = conversationAdapter.selectedConversation

                findItem(R.id.manuPin).isVisible = conversations.any { !it.isPined }
                findItem(R.id.menuUnpin).isVisible = conversations.all { it.isPined }

                val canDialNumber = canDialNumber(conversations.first().recipients as? ArrayList)
                dialNumber = canDialNumber.second

                findItem(R.id.menuAddContact).isVisible =
                    conversationAdapter.selection.size == 1 && canDialNumber.first && conversations.first().recipients.any { it.contact == null }
                findItem(R.id.menuDial).isVisible =
                    conversationAdapter.selection.size == 1 && canDialNumber.first

                findItem(R.id.menuMarkAsRead).isVisible = conversations.none { it.read }
                findItem(R.id.menuMarkAsUnread).isVisible = conversations.all { it.read }
                findItem(R.id.menuCopyTo).isVisible = conversationAdapter.selection.size == 1

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

            R.id.menuArchive -> {
                val selectedConversationIds =
                    conversationAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForArchived(selectedConversationIds, true)
                conversationAdapter.conversations.removeAll(conversationAdapter.selectedConversation.toSet())
                allConversation.removeAll(conversationAdapter.selectedConversation.toSet())
                binding.homeScreen.homeToolbar.stopActionMode()
                conversationAdapter.stopSelectionMode()
            }

            R.id.menuPrivate -> {
                if (appPreferences.isPinSet) {
                    val selectedConversationIds =
                        conversationAdapter.selectedConversation.map { it.threadId }
                    messageRepository.handleConversationForPrivate(selectedConversationIds, true)
                    conversationAdapter.conversations.removeAll(conversationAdapter.selectedConversation.toSet())
                    allConversation.removeAll(conversationAdapter.selectedConversation.toSet())
                    binding.homeScreen.homeToolbar.stopActionMode()
                    conversationAdapter.stopSelectionMode()
                } else {
                    mainScope.launch {
                        toast(getStringValue(R.string.set_private_pin_toast))
                        delay(500)
                        binding.homeScreen.homeToolbar.stopActionMode()
                        conversationAdapter.stopSelectionMode()
                        launchActivity(EnterPinActivity::class.java)
                    }
                }
            }

            R.id.menuHomeDelete -> {
                deleteConversation()
            }

            R.id.menuAddContact -> {
                if (conversationAdapter.selectedConversation.isNotEmpty()) {
                    val phoneNo =
                        conversationAdapter.selectedConversation.first().recipients.first().address
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
                if (conversationAdapter.selectedConversation.isNotEmpty()) {
                    copyToClipboard(conversationAdapter.selectedConversation.first().snippet)
                    binding.homeScreen.homeToolbar.stopActionMode()
                    conversationAdapter.stopSelectionMode()
                }
            }

            R.id.menuDial -> {
                if (conversationAdapter.selectedConversation.isNotEmpty()) {
                    binding.homeScreen.homeToolbar.stopActionMode()
                    conversationAdapter.stopSelectionMode()
                    dialNumber(dialNumber)
                }
            }

        }

        return super.onOptionsItemSelected(item)
    }

    private fun handlePinedConversations(isPined: Boolean) {
        val selectedConversationId = conversationAdapter.selectedConversation.map { it.threadId }

        binding.homeScreen.homeToolbar.stopActionMode()
        conversationAdapter.stopSelectionMode()
        messageRepository.updateConversationPinedData(selectedConversationId, isPined)

        allConversation =
            getConversationWithPinedData(allConversation, selectedConversationId, isPined)

        mainScope.launch {
            loadConversationToAdapter(getCurrentShowedConversations(selectedFilter))
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

    private fun deleteConversation(isFromAction: Boolean = false, position: Int = 0) {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.delete),
            dialogDisc = getStringValue(R.string.want_to_delete_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.delete),
            onPositive = {
                if (isFromAction) {
                    val conversationToDelete = conversationAdapter.conversations[position]
                    messageRepository.deleteConversation(listOf(conversationAdapter.conversations[position].threadId))
                    allConversation.remove(conversationToDelete)
                    conversationAdapter.conversations.removeAt(position)
                    conversationAdapter.notifyItemRemoved(position)
                    cancelNotification(conversationToDelete.threadId.toInt())
                } else {
                    val conversationToRemove = conversationAdapter.selectedConversation
                    if (conversationToRemove.isNotEmpty()) {
                        val positions = conversationAdapter.selectedPos
                        messageRepository.deleteConversation(conversationToRemove.map { it.threadId })
                        positions.forEach {
                            allConversation.removeAll(conversationToRemove.toSet())
                            conversationAdapter.conversations.removeAt(it)
                            conversationAdapter.notifyItemRemoved(it)
                        }
                        mainScope.launch {
                            if (conversationAdapter.conversations.size == 0) {
                                handleViewVisibility(true, getFilterName(selectedFilter))
                            }
                            binding.homeScreen.homeToolbar.stopActionMode()
                            conversationAdapter.stopSelectionMode()
                        }
                        conversationToRemove.map { it.threadId }.forEach { threadId ->
                            cancelNotification(threadId.toInt())
                        }
                    }
                }
            }, onNegative = {
                if (isFromAction) {
                    conversationAdapter.notifyItemChanged(position)
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
            if (isFromSwipeAction) listOf(conversationAdapter.conversations[position].threadId) else conversationAdapter.selectedConversation.map { it.threadId }
        messageRepository.markThreadMessagesReadUnRead(selectedThreadIds, isRead) {
            if (isFromSwipeAction) {
                mainScope.launch {
                    conversationAdapter.apply {
                        conversations[position].read = isRead
                        notifyItemChanged(position)
                    }
                }
            } else {
                conversationAdapter.selectedPos.forEach {
                    conversationAdapter.conversations[it].read = isRead
                }
                mainScope.launch {
                    conversationAdapter.stopSelectionMode()
                    binding.homeScreen.homeToolbar.stopActionMode()
                }
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
                    conversationAdapter.selectedConversation.map { it.threadId }
                messageRepository.handleConversationForBlocked(selectedConversationIds, true)
                conversationAdapter.conversations.removeAll(conversationAdapter.selectedConversation.toSet())
                allConversation.removeAll(conversationAdapter.selectedConversation.toSet())
                binding.homeScreen.homeToolbar.stopActionMode()
                conversationAdapter.stopSelectionMode()
            })
        deleteDialog.show()
    }

    override fun onGetEvent(event: MessageEvent) {
        when (event) {
            is ApplyFilter -> {
                appliedFilter = event.filter
                selectedFilterData = Triple(event.month, event.year, event.dateRange)
                val currentConversation = getCurrentShowedConversations(selectedFilter)
                loadConversationToAdapter(when (event.filter) {
                    getIntValue(R.integer.today_filter) -> {
                        val filterConversations =
                            currentConversation.filter { it.date.dateFormatDMY() == (Calendar.getInstance().timeInMillis / 1000).dateFormatDMY() }
                        filterConversations as ArrayList

                    }

                    getIntValue(R.integer.month_filter) -> {
                        val filterConversations =
                            currentConversation.filter { it.date.dateFormatMY() == "${selectedFilterData.first}/${selectedFilterData.second}" }
                        filterConversations as ArrayList
                    }

                    getIntValue(R.integer.year_filter) -> {
                        val filterConversations =
                            currentConversation.filter { it.date.dateFormatY() == "${selectedFilterData.second}" }
                        filterConversations as ArrayList

                    }

                    getIntValue(R.integer.date_range_filter) -> {
                        val startDate = selectedFilterData.third.first
                        val endDate = selectedFilterData.third.second + (24 * 3600)
                        val filterConversations =
                            currentConversation.filter { it.date in startDate..endDate }
                        filterConversations as ArrayList
                    }

                    else -> {
                        currentConversation
                    }
                })
            }

            DataSyncDone -> {
                isSyncInProgress = false
                getConversation()
            }

            RefreshMessages -> {
                if (!isSyncInProgress) {
                    messageRepository.syncConversation { getConversation() }
                }
                EventBus.getDefault().removeStickyEvent(event)
            }

            UpdateConversation -> {
                getConversation()
                EventBus.getDefault().removeStickyEvent(event)
            }

            is RefreshConversation -> {
                try {
                    backgroundScope.launch {
                        val conversation =
                            conversationAdapter.conversations.find { it.threadId == event.threadId }
                        if (conversation == null) {
                            messageRepository.syncRecipients {
                                messageRepository.syncConversation { getConversation() }
                            }
                        } else {
                            val index = conversationAdapter.conversations.indexOf(conversation)
                            messageRepository.syncSelectedConversation(event.threadId) { newConversation, isDelete ->
                                mainScope.launch {
                                    if (isDelete && newConversation == null) {
                                        allConversation.remove(conversation)
                                        conversationAdapter.conversations.removeAt(index)
                                        conversationAdapter.notifyItemRemoved(index)
                                    } else {
                                        conversationAdapter.updateConversation(
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

            OnChangeSwipeAction -> {
                attachSwipeToRecycleView()
                EventBus.getDefault().removeStickyEvent(event)
            }

            is HandleConversation -> {
                val conversation = event.conversation.apply {
                    isArchived = event.updateData.first
                    isBlocked = event.updateData.second
                }
                updateConversationData(event.conversation.threadId, conversation)
                EventBus.getDefault().removeStickyEvent(event)
            }

            is DeleteConversation -> {
                allConversation.mapIndexed { index, item ->
                    if (item.threadId == event.threadId) {
                        allConversation.removeAt(index)
                    }
                }
                mainScope.launch {
                    loadConversationToAdapter(getCurrentShowedConversations(selectedFilter))
                }
                EventBus.getDefault().removeStickyEvent(event)
            }

            is OnReceiveMessageConversation -> {
                backgroundScope.launch {
                    val conversations =
                        conversationAdapter.conversations.filter { it.threadId == event.message.threadId }
                    if (conversations.isNotEmpty()) {
                        messageRepository.updateConversationData(
                            event.message.threadId,
                            event.message.body,
                            event.message.date
                        )
                        val index = conversationAdapter.conversations.indexOf(conversations.first())
                        val newConversation = conversations.first().apply {
                            snippet =
                                if (event.message.isMMS) getStringValue(R.string.attachment) else event.message.body
                            date = event.message.date
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
                            conversationAdapter.updateConversation(index, newConversation)
                        }
                    } else {
                        val conversation =
                            messageRepository.getConversationsUsingThreadId(event.message.threadId)

                        if (conversation.isNotEmpty()) {
                            allConversation.add(0, conversation.first())
                            mainScope.launch {
                                loadConversationToAdapter(
                                    getCurrentShowedConversations(
                                        selectedFilter
                                    )
                                )
                            }
                        }
                    }
                }
                EventBus.getDefault().removeStickyEvent(event)
            }

            else -> {}
        }
    }

    private fun updateConversationData(threadId: Long, conversation: Conversation) {
        backgroundScope.launch {
            allConversation.mapIndexed { index, item ->
                if (item.threadId == threadId) {
                    allConversation[index] = conversation
                }
            }
            allConversation =
                allConversation.filter { (!it.isArchived && !it.isBlocked) } as ArrayList
            mainScope.launch {
                loadConversationToAdapter(getCurrentShowedConversations(selectedFilter))
            }
        }
    }

    private fun showExitDialog() {
        ExitDialog(onClickYes = { exitFromApp() }).apply {
            show(supportFragmentManager, FilterBottomSheet::class.simpleName)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}