package com.messages.ui.chat.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Telephony
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.PermissionManager
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.createTemporaryThread
import com.messages.common.message_utils.isLongMmsMessage
import com.messages.common.message_utils.scheduleMessage
import com.messages.common.message_utils.sendMessageCompat
import com.messages.data.events.MessageEvent
import com.messages.data.events.OnLanguageSelect
import com.messages.data.events.OnPickedContact
import com.messages.data.events.OnReceiveMessage
import com.messages.data.events.OnReceiveMessageConversation
import com.messages.data.events.OnSelectClipboardData
import com.messages.data.events.RefreshConversation
import com.messages.data.events.RefreshScheduleMessage
import com.messages.data.events.UpdateLastSendMessageStatus
import com.messages.data.models.AttachmentData
import com.messages.data.models.AttachmentWithMessageModel
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.MessagesWithDate
import com.messages.data.models.SimModel
import com.messages.data.models.isGifMimeType
import com.messages.data.models.isImageMimeType
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityChatBinding
import com.messages.extentions.autoScrollToStart
import com.messages.extentions.canDialNumber
import com.messages.extentions.copyToClipboard
import com.messages.extentions.dialNumber
import com.messages.extentions.formatMessageFullDate
import com.messages.extentions.generateRandomId
import com.messages.extentions.getAttachmentsDir
import com.messages.extentions.getByteForMmsLimit
import com.messages.extentions.getClipboardTypeData
import com.messages.extentions.getColorForId
import com.messages.extentions.getFileSizeFromUri
import com.messages.extentions.getFilenameFromUri
import com.messages.extentions.getPathFromUri
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.extentions.loadImageWithOutCache
import com.messages.extentions.normalizePhoneNumber
import com.messages.extentions.normalizeString
import com.messages.extentions.setBackgroundTint
import com.messages.extentions.shareTextIntent
import com.messages.extentions.toJson
import com.messages.extentions.toast
import com.messages.ui.appearance.ui.AppearanceActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.adapter.AttachmentAdapter
import com.messages.ui.chat.adapter.ChatAdapter
import com.messages.ui.chat.adapter.ClipboardViewPagerAdapter
import com.messages.ui.chat.bottomsheet.ScheduleMessageBottomSheet
import com.messages.ui.chat.bottomsheet.TranslateBottomSheetDialog
import com.messages.ui.chat.viewmodel.TranslateState
import com.messages.ui.chat.viewmodel.TranslateViewModel
import com.messages.ui.chat_wallpaper.ChatWallpaperActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.ui.details.ConversationDetailsActivity
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.new_chat.ui.ContactActivity
import com.messages.utils.AppLogger
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.FILE_SIZE_NONE
import com.messages.utils.IS_CONTACT_PICK
import com.messages.utils.IS_FROM_CALL_END
import com.messages.utils.IS_FROM_NOTIFICATION
import com.messages.utils.IS_PRIVATE_CONVERSATION
import com.messages.utils.RECIPIENTS
import com.messages.utils.SEND_TO_NUMBER
import com.messages.utils.SMS_BODY
import com.messages.utils.THREAD_ID
import com.messages.utils.THREAD_NUMBER
import com.messages.utils.THREAD_TEXT
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private val binding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<TranslateViewModel>()
    private var chatAdapter: ChatAdapter? = null
    private var attachmentAdapter = AttachmentAdapter()
    private var isSenderSupportReply = false
    private var isPrivateConversation = false
    private var threadId = 0L
    private var conversationTitle = ""
    private var conversation: Conversation? = null
    private var translateMessage: Pair<Int, Message> = Pair(0, Message())
    private var isScheduledMessage: Boolean = false
    private val availableSIMCards = ArrayList<SimModel>()
    private var currentSIMIndex = 0
    private var refreshedSinceSent = false
    private var sendAddresses = arrayListOf<String>()
    private var isSendToNumber = false
    private var scheduledDateTime: Long = 0
    private var scheduledMessage: Message? = null
    private var capturedImageUri: Uri? = null
    private var canDialNumber: Pair<Boolean, String>? = null
    private var isOpenHomeScreen = false
    private var isConversationUpdate = false

    private var currentChatBubbleBgSend = 0
    private var currentChatBubbleBgReceived = 0


    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                getPathFromUri(uri) {
                    addAttachmentToAdapter(uri)
                }
            }
        }

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        mainScope.launch {
            initAdapter()
            initData()
            delay(250)
            initActions()
            initToolbar()
            checkAllowReply()
            initObserver()
            setupSIMSelector()
            handleBackPress()
            initClipboardData()
        }
    }

    private fun handleBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mainScope.launch {
                    binding.clChatBottomItem.apply {
                        ivAddToMessage.apply {
                            rotation = 45F
                            loadImageDrawable(R.drawable.ic_close)
                        }
                        if (emojiPicker.isVisible) {
                            emojiPicker.isVisible = false
                        } else if (clAttachments.isVisible) {
                            clAttachments.isVisible = false
                        } else if (clClipboardData.isVisible) {
                            clClipboardData.isVisible = false
                        } else {
                            isKeyboardOpen {
                                if (it) {
                                    dismissKeyboard(evMessage)
                                } else {
                                    openHomeOrFinish()
                                }
                            }
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this@ChatActivity, callback)
    }

    private fun openHomeOrFinish() {
        if (isOpenHomeScreen) {
            if (!MessageApplication.isAppInBackground) {
                launchActivity(HomeActivity::class.java)
            } else {
                if (isConversationUpdate) {
                    EventBus.getDefault().postSticky(RefreshConversation(threadId))
                }
            }
        } else {
            if (isConversationUpdate) {
                EventBus.getDefault().postSticky(RefreshConversation(threadId))
            }
        }
        this@ChatActivity.finish()
    }


    private fun initObserver() {
        binding.clChatBottomItem.apply {
            evMessage.addTextChangedListener {
                tvMessageTextCount.text = "${evMessage.text?.count()}"
                handleSendButtonVisibility()
            }

            evMessage.setOnSafeClickListener {
                clAttachments.isVisible = false
                clClipboardData.isVisible = false
                emojiPicker.isVisible = false
                ivAddToMessage.apply {
                    rotation = 45F
                    loadImageDrawable(R.drawable.ic_close)
                    showKeyboard(ivAddToMessage)
                }
            }

            evMessage.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    evMessage.performClick()
                }
            }

        }

        lifecycleScope.launch {
            viewModel.translatedText.collect { state ->
                when (state) {
                    is TranslateState.Loading -> {
                        chatAdapter?.messages?.forEachIndexed { index, messagesWithDate ->
                            if (index == translateMessage.first) {
                                if (messagesWithDate is MessagesWithDate.SendMessage) {
                                    messagesWithDate.message.isLoading = true
                                } else if (messagesWithDate is MessagesWithDate.ReceivedMessage) {
                                    messagesWithDate.message.isLoading = true
                                }
                            }
                            return@forEachIndexed
                        }
                        chatAdapter?.notifyItemChanged(translateMessage.first)
                    }

                    is TranslateState.OnTranslate -> {
                        if (!state.text.isNullOrEmpty()) {
                            chatAdapter?.messages?.forEachIndexed { index, messagesWithDate ->
                                if (index == translateMessage.first) {
                                    if (messagesWithDate is MessagesWithDate.SendMessage) {
                                        messagesWithDate.message.translatedBody = state.text
                                        messagesWithDate.message.isShowTranslateBody = true
                                        messagesWithDate.message.isLoading = false
                                    } else if (messagesWithDate is MessagesWithDate.ReceivedMessage) {
                                        messagesWithDate.message.translatedBody = state.text
                                        messagesWithDate.message.isShowTranslateBody = true
                                        messagesWithDate.message.isLoading = false
                                    }
                                }
                                return@forEachIndexed
                            }
                            chatAdapter?.notifyItemChanged(translateMessage.first)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private val speechRecognitionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.get(0) ?: ""
                mainScope.launch {
                    binding.clChatBottomItem.evMessage.append(recognizedText)
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (capturedImageUri != null) {
                    addAttachmentToAdapter(capturedImageUri!!)
                }
            }
        }

    private val voiceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val audioUri = result.data?.data
                if (audioUri != null) {
                    getPathFromUri(audioUri) {
                        addAttachmentToAdapter(audioUri)
                    }
                } else {
                    toast(msg = "No audio file received")
                }
            }
        }

    private fun initActions() {
        binding.apply {
            chatToolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                openHomeOrFinish()
            }

            chatToolbar.getBinding().ivActionToolbarBack.setOnSafeClickListener {
                chatAdapter?.stopSelectionMode()
                chatToolbar.stopActionMode()
            }

            clChatBottomItem.apply {
                clAddToMessage.setOnSafeClickListener {
                    if (clAttachments.isVisible || clClipboardData.isVisible) {
                        ivAddToMessage.apply {
                            rotation = 45F
                            loadImageDrawable(R.drawable.ic_close)
                        }
                        clAttachments.isVisible = false
                        clClipboardData.isVisible = false
                        clClipboardData.isVisible = false
                        emojiPicker.isVisible = false
                        showKeyboard(clAddToMessage)
                    } else {
                        ivAddToMessage.apply {
                            rotation = 0F
                            loadImageDrawable(R.drawable.ic_keyboard)
                        }
                        clAttachments.isVisible = true
                        clClipboardData.isVisible = false
                        emojiPicker.isVisible = false
                        dismissKeyboard(clAddToMessage)
                    }
                }

                clImage.setOnSafeClickListener {
                    pickMedia.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.SingleMimeType(
                                "image/*"
                            )
                        )
                    )
                }

                clFile.setOnSafeClickListener {
                    launchGetContentIntent()
                }

                btnSendMessage.setOnSafeClickListener {
                    if (permissionManager.isDefaultSMS()) {
                        sendMessage()
                    } else {
                        requestDefaultSmsApp()
                    }
                }

                ivSim.setOnSafeClickListener {
                    if (availableSIMCards.isNotEmpty()) {
                        currentSIMIndex = (currentSIMIndex + 1) % availableSIMCards.size
                        val currentSIMCard = availableSIMCards[currentSIMIndex]
                        tvSim.text = "${currentSIMCard.id}"
                        toast(currentSIMCard.label)
                    }
                }

                clVoice.setOnSafeClickListener {
                    try {
                        val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                        voiceLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        toast(msg = getStringValue(R.string.device_not_support_this_feature))
                    } catch (e: Exception) {
                        toast(msg = getStringValue(R.string.something_went_wrong_please_try_after_some_time))
                    }
                }

                clContact.setOnSafeClickListener {
                    launchActivity(ContactActivity::class.java) {
                        putBoolean(IS_CONTACT_PICK, true)
                    }
                }

                clClipboard.setOnSafeClickListener {
                    clClipboardData.isVisible = true
                    clAttachments.isVisible = false
                }

                clCamera.setOnSafeClickListener {
                    val imageFile = File.createTempFile("attachment_", ".jpg", getAttachmentsDir())
                    capturedImageUri = FileProvider.getUriForFile(
                        this@ChatActivity,
                        "$packageName.provider",
                        imageFile
                    )
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
                    }
                    cameraLauncher.launch(intent)
                }

                clSchedule.setOnSafeClickListener {
                    if (isScheduledMessage) {
                        launchScheduleSendDialog(scheduledDateTime)
                    } else {
                        launchScheduleSendDialog()
                    }
                }

                ivCloseSchedule.setOnSafeClickListener {
                    clMessageAdditional.isVisible = false
                    clMessageAdditional.background = null
                    groupScheduleTime.isVisible = false
                    isScheduledMessage = false
                    updateSendButtonDrawable()
                }

                clLocation.setOnSafeClickListener {
                    getCurrentLocation()
                }

                ivMic.setOnSafeClickListener {
                    dismissKeyboard(evMessage)
                    if (SpeechRecognizer.isRecognitionAvailable(this@ChatActivity)) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
                        }
                        speechRecognitionLauncher.launch(intent)
                    } else {
                        AppLogger.e(message = "Speech recognition not supported on this device")
                    }
                }

                ivEmoji.setOnSafeClickListener {
                    dismissKeyboard(evMessage)
                    emojiPicker.isVisible = !emojiPicker.isVisible
                }

                emojiPicker.setOnEmojiPickedListener {
                    evMessage.append(it.emoji)
                }
            }
        }

        chatAdapter?.apply {
            onItemLongClick = {
                binding.chatToolbar.startActionMode()
                updateToolbar()
            }
        }

        attachmentAdapter.apply {
            onRemoveAttachment = {
                handleSendButtonVisibility()
                binding.clChatBottomItem.rcvAttachments.isVisible =
                    attachmentAdapter.attachmentList.size != 0
                binding.clChatBottomItem.clMessageAdditional.isVisible =
                    attachmentAdapter.attachmentList.size != 0
                binding.clChatBottomItem.clMessageAdditional.background = null
            }
            onImageCompress = {
                handleSendButtonVisibility()
                binding.clChatBottomItem.rcvAttachments.isVisible =
                    attachmentAdapter.attachmentList.size != 0
                binding.clChatBottomItem.clMessageAdditional.isVisible =
                    attachmentAdapter.attachmentList.size != 0
                binding.clChatBottomItem.clMessageAdditional.background = null
            }
        }

    }

    private fun handleSendButtonVisibility() {
        binding.clChatBottomItem.apply {
            if (!evMessage.text.isNullOrEmpty() || attachmentAdapter.attachmentList.isNotEmpty()) {
                btnSendMessage.isVisible = true
                ivMic.isVisible = false
            } else {
                btnSendMessage.isVisible = false
                ivMic.isVisible = true
            }
        }
    }

    private fun sendMessage() {
        val msg = binding.clChatBottomItem.evMessage.text.toString()
        var msgText = if (appPreferences.removeAccents) msg.normalizeString() else msg
        if (msgText.isEmpty() && attachmentAdapter.attachmentList.isEmpty()) {
            toast(getStringValue(R.string.unknown_error_occurred))
            return
        }

        if (binding.clChatBottomItem.evMessageSubject.text?.isNotEmpty() == true) {
            msgText =
                "${getStringValue(R.string.subject)} ${binding.clChatBottomItem.evMessageSubject.text}\n$msgText"
        }

        val subscriptionId = availableSIMCards.getOrNull(currentSIMIndex)?.subscriptionId
            ?: SmsManager.getDefaultSmsSubscriptionId()

        if (isScheduledMessage) {
            sendScheduledMessage(msgText, subscriptionId)
        } else {
            sendNormalMessage(msgText, subscriptionId)
        }
    }

    private fun updateToolbar() {
        binding.chatToolbar.apply {
            if (chatAdapter?.selection?.isEmpty() == true) {
                stopActionMode()
            } else {
                getBinding().tvActionToolbarTitle.text = "${chatAdapter?.selection?.size}"
                refreshMenu()
            }
        }
    }

    private fun initToolbar() {
        binding.chatToolbar.apply {
            getBinding().tvToolbarTitle.text = conversationTitle
            setSupportActionBar(this)
        }
    }

    private fun initData() {
        isOpenHomeScreen = if (intent.getBooleanExtra(IS_FROM_CALL_END, false)) {
            true
        } else {
            intent.getBooleanExtra(IS_FROM_NOTIFICATION, false)
        }
        threadId = intent.getLongExtra(THREAD_ID, 0L)
        conversationTitle = intent.getStringExtra(CONVERSATION_TITLE) ?: ""
        isPrivateConversation = intent.getBooleanExtra(IS_PRIVATE_CONVERSATION, false)
        isSendToNumber = intent.getBooleanExtra(SEND_TO_NUMBER, false)
        backgroundScope.launch {
            messageRepository.markThreadMessagesReadUnRead(listOf(threadId), true) {}
        }
        if (!isSendToNumber) {
            backgroundScope.launch {
                sendAddresses =
                    messageRepository.getThreadRecipient(threadId).map { it.address } as ArrayList
                if (sendAddresses.isEmpty()) {
                    sendAddresses.add(intent.getStringExtra(THREAD_NUMBER) ?: "")
                }
                val messages = messageRepository.getMessagesFromThreadId(threadId)
                messages.reverse()
                mainScope.launch {
                    binding.apply {
                        if (messages.isNotEmpty()) {
                            chatAdapter?.addMessages(messages)
                            tvNoMessage.isVisible = false
                            rcvChats.isVisible = true
                        } else {
                            tvNoMessage.isVisible = true
                            rcvChats.isVisible = false
                        }
                    }
                }
            }
        } else {
            sendAddresses.add(intent.getStringExtra(THREAD_NUMBER) ?: "")
        }
    }

    private fun initAdapter() {
        binding.rcvChats.apply {
            currentChatBubbleBgSend = appPreferences.chatBubbleBgSend
            currentChatBubbleBgReceived = appPreferences.chatBubbleBgReceived
            chatAdapter = ChatAdapter(appPreferences, onClickTranslate = { message, position ->
                translateMessage = Pair(position, message)
                TranslateBottomSheetDialog().apply {
                    show(supportFragmentManager, TranslateBottomSheetDialog::class.simpleName)
                }
            }, onItemClickMessage = {
                updateToolbar()
            }, onClickResend = {
                sendMessageCompat(
                    it.body,
                    it.addresses,
                    it.subId,
                    it.attachmentWithMessageModel?.attachments ?: arrayListOf(),
                    it.id
                )
            })
            adapter = chatAdapter
            chatAdapter?.autoScrollToStart(this)
        }
        binding.clChatBottomItem.rcvAttachments.apply {
            adapter = attachmentAdapter
        }
    }

    private fun checkAllowReply() {
        binding.clChatBottomItem.apply {
            if (!isSendToNumber) {
                backgroundScope.launch {
                    conversation = messageRepository.getConversationFromThreadId(threadId)
                    canDialNumber = canDialNumber(conversation?.recipients as? ArrayList)
                    mainScope.launch {
                        if (canDialNumber?.first == false) {
                            isSenderSupportReply = false
                            clNotSupportReplies.isVisible = true
                            clSupportReplies.isVisible = false
                        } else {
                            isSenderSupportReply = true
                            clSupportReplies.isVisible = true
                            clNotSupportReplies.isVisible = false
                            evMessage.setText(intent.getStringExtra(THREAD_TEXT))
                            ivMic.isVisible =
                                SpeechRecognizer.isRecognitionAvailable(this@ChatActivity)
                            tvMessageTextCount.isVisible = appPreferences.showCharCount
                        }
                    }
                }
            } else {
                isSenderSupportReply = true
                clSupportReplies.isVisible = true
                clNotSupportReplies.isVisible = false
                evMessage.setText(intent.getStringExtra(THREAD_TEXT))
                canDialNumber = Pair(true, sendAddresses.first())
                tvMessageTextCount.isVisible = true
            }
        }

    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    addAttachmentToAdapter(uri)
                }
            }
        }

    private fun launchGetContentIntent() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("*/*"))
            resultLauncher.launch(this)
        }
    }

    private fun addAttachmentToAdapter(uri: Uri) {
        val mimeType = contentResolver.getType(uri)
        if (mimeType == null) {
            toast(getStringValue(R.string.unknown_error_occurred))
            return
        }
        val isImage = mimeType.isImageMimeType()
        val isGif = mimeType.isGifMimeType()
        if (isGif || !isImage) {
            val fileSize = getFileSizeFromUri(uri)
            val mmsLimit = getByteForMmsLimit(appPreferences.mmsLimit)
            if (mmsLimit != FILE_SIZE_NONE && fileSize > mmsLimit) {
                toast(getStringValue(R.string.attachment_sized_exceeds_max_limit))
                return
            }
        }

        binding.clChatBottomItem.apply {
            attachmentAdapter.apply {
                if (attachmentList.size == 0) {
                    rcvAttachments.isVisible = true
                    clMessageAdditional.isVisible = true
                    clMessageAdditional.background = null
                }
                val attachment = AttachmentData(
                    id = uri.toString(),
                    uri = uri,
                    mimetype = mimeType,
                    filename = getFilenameFromUri(uri),
                    isPending = isImage && !isGif
                )
                addAttachment(attachment)
                handleSendButtonVisibility()
            }
        }
    }

    override fun onSetDefaultSMS() {
        sendMessage()
    }

    override fun onGetPermission(permission: Int, isGranted: Boolean) {
        if (permission == PermissionManager.PERMISSION_READ_STORAGE) {
            launchGetContentIntent()
        } else if (permission == PermissionManager.PERMISSION_ACCESS_FINE_LOCATION) {
            getCurrentLocation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            findItem(R.id.menuSubjectField).isVisible =
                !binding.chatToolbar.isActionMode && canDialNumber?.first == true
            findItem(R.id.menuCall).isVisible =
                !binding.chatToolbar.isActionMode && canDialNumber?.first == true
            findItem(R.id.menuChatWallpaper).isVisible = !binding.chatToolbar.isActionMode
            findItem(R.id.menuDetails).isVisible = !binding.chatToolbar.isActionMode
            findItem(R.id.menuAppearance).isVisible = !binding.chatToolbar.isActionMode

            findItem(R.id.menuCopy).isVisible =
                binding.chatToolbar.isActionMode && chatAdapter?.selection?.size == 1
            findItem(R.id.menuDelete).isVisible = binding.chatToolbar.isActionMode
            findItem(R.id.menuShare).isVisible =
                binding.chatToolbar.isActionMode && chatAdapter?.selection?.size == 1
            findItem(R.id.menuForward).isVisible = binding.chatToolbar.isActionMode
            findItem(R.id.menuViewDetails).isVisible =
                binding.chatToolbar.isActionMode && chatAdapter?.selection?.size == 1

        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuCall -> {
                dialNumber(canDialNumber?.second ?: "")
            }

            R.id.menuDetails -> {
                launchActivity(ConversationDetailsActivity::class.java) {
                    putLong(THREAD_ID, conversation?.threadId ?: 0)
                    putString(RECIPIENTS, conversation?.recipients?.toJson())
                }
            }

            R.id.menuAppearance -> {
                launchActivity(AppearanceActivity::class.java)
            }

            R.id.menuChatWallpaper -> {
                launchActivity(ChatWallpaperActivity::class.java)
            }

            R.id.menuSubjectField -> {
                binding.clChatBottomItem.apply {
                    if (evMessageSubject.isVisible) {
                        clMessageAdditional.isVisible = false
                        evMessageSubject.isVisible = false
                        clMessageAdditional.isVisible = false
                        tvScheduleTime.isVisible = false
                        viewMessageSubjectDivider.isVisible = false
                    } else {
                        evMessageSubject.isVisible = true
                        viewMessageSubjectDivider.isVisible = true
                        viewMessageSubjectDivider.isVisible = true
                        clMessageAdditional.isVisible = true
                        clMessageAdditional.background = null
                    }
                }
            }

            R.id.menuCopy -> {
                if (chatAdapter?.selectedMessage?.isNotEmpty() == true) {
                    copyToClipboard(chatAdapter?.selectedMessage?.first()?.body ?: "")
                    binding.chatToolbar.stopActionMode()
                    chatAdapter?.stopSelectionMode()
                }
            }

            R.id.menuShare -> {
                if (chatAdapter?.selectedMessage?.isNotEmpty() == true) {
                    shareTextIntent(chatAdapter?.selectedMessage?.first()?.body ?: "")
                    binding.chatToolbar.stopActionMode()
                    chatAdapter?.stopSelectionMode()
                }
            }

            R.id.menuForward -> {
                launchActivity(ContactActivity::class.java) {
                    putString(SMS_BODY, chatAdapter?.selectedMessage?.first()?.body)
                }
            }

            R.id.menuDelete -> {
                deleteMessage()
            }

            R.id.menuViewDetails -> {
                viewMessageDetails()
                binding.chatToolbar.stopActionMode()
                chatAdapter?.stopSelectionMode()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun deleteMessage() {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.delete),
            dialogDisc = getStringValue(R.string.are_you_sure_you_want_to_delete_this_message),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.delete),
            onPositive = {
                isConversationUpdate = true
                chatAdapter?.let { adapter ->
                    val messagesToRemove = adapter.selectedMessage
                    if (messagesToRemove.isNotEmpty()) {
                        val positions = adapter.selectedPos
                        messagesToRemove.forEach {
                            messageRepository.deleteMessage(it.messageId ?: 0)
                        }
                        positions.forEach {
                            adapter.messages.removeAt(it)
                        }
                        messageRepository.updateLastConversationMessage(threadId)
                        mainScope.launch {
                            if (adapter.messages.size == 0) {
                                binding.tvNoMessage.isVisible = true
                                binding.rcvChats.isVisible = false
                            } else {
                                adapter.removeSelectedItems(positions)
                            }
                            binding.chatToolbar.stopActionMode()
                            adapter.stopSelectionMode()
                        }
                    }
                }
            })
        deleteDialog.show()
    }

    private fun viewMessageDetails() {
        val selectedMessage = chatAdapter?.selectedMessage?.first()
        val disc = getString(
            if (selectedMessage?.isReceivedMessage() == true) R.string.message_details_received else R.string.message_details_send,
            if (selectedMessage?.isMMS == true) "MMS" else "SMS",
            selectedMessage?.addresses?.joinToString(),
            selectedMessage?.date?.formatMessageFullDate(appPreferences.use24Hr)
        )
        val deleteDialog = CustomDialog(
            context = this,
            dialogTitle = getStringValue(R.string.message_details),
            dialogDisc = disc,
            positiveBtnText = getStringValue(R.string.ok),
            isShowSingleBtn = true,
        )
        deleteDialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun setupSIMSelector() {
        val subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val availableSIMs = subscriptionManager.activeSubscriptionInfoList ?: return
        if (availableSIMs.size > 1) {
            availableSIMs.forEachIndexed { index, subscriptionInfo ->
                var label = subscriptionInfo.displayName?.toString() ?: ""
                if (subscriptionInfo.number?.isNotEmpty() == true) {
                    label += " (${subscriptionInfo.number})"
                }
                val simCard = SimModel(index + 1, subscriptionInfo.subscriptionId, label)
                availableSIMCards.add(simCard)
            }
            binding.clChatBottomItem.apply {
                groupSim.isVisible = true
                tvSim.text = "${availableSIMCards[currentSIMIndex].id}"
            }
        } else {
            binding.clChatBottomItem.groupSim.isVisible = false
        }
    }

    private fun launchScheduleSendDialog(originalDateTime: Long? = null) {
        askForExactAlarmPermissionIfNeeded {
            ScheduleMessageBottomSheet(if (originalDateTime != null) (originalDateTime * 1000) else null) { selectedTime ->
                binding.clChatBottomItem.apply {
                    if (selectedTime > System.currentTimeMillis()) {
                        isScheduledMessage = true
                        scheduledDateTime = selectedTime / 1000
                        clMessageAdditional.isVisible = true
                        groupScheduleTime.isVisible = true
                        clMessageAdditional.background = ContextCompat.getDrawable(
                            this@ChatActivity, R.drawable.schedule_time_bg
                        )
                        tvScheduleTime.text =
                            scheduledDateTime.formatMessageFullDate(appPreferences.use24Hr)
                    } else {
                        isScheduledMessage = false
                        clMessageAdditional.isVisible = false
                        groupScheduleTime.isVisible = false
                    }
                    updateSendButtonDrawable()
                }
            }.apply {
                show(supportFragmentManager, ScheduleMessageBottomSheet::class.simpleName)
            }
        }
    }

    private fun sendScheduledMessage(text: String, subscriptionId: Int) {
        if ((scheduledDateTime * 1000) < (System.currentTimeMillis() + 1000L)) {
            toast(getStringValue(R.string.must_pick_time_in_the_future))
            launchScheduleSendDialog(scheduledDateTime)
            return
        }
        refreshedSinceSent = false
        var isTemporaryThread = false
        try {
            backgroundScope.launch {
                val messageId = scheduledMessage?.id ?: generateRandomId()
                val message = buildScheduledMessage(text, subscriptionId, messageId)
                if (chatAdapter?.messages?.isEmpty() == true) {
                    threadId = message.threadId
                    createTemporaryThread(message, message.threadId, sendAddresses, conversation) {
                        messageRepository.insertOrUpdateConversation(it)
                        isTemporaryThread = true
                    }
                }
                scheduleMessage(message, isTemporaryThread)
                messageRepository.insertOrUpdateMessage(message)

                mainScope.launch {
                    clearCurrentMessage()
                    binding.clChatBottomItem.ivCloseSchedule.performClick()
                    scheduledMessage = null
                    chatAdapter?.apply {
                        addMessage(MessagesWithDate.SendMessage(message))
                        binding.rcvChats.smoothScrollToPosition(messages.lastIndex)
                        binding.tvNoMessage.isVisible = false
                        binding.rcvChats.isVisible = true
                    }
                }
            }
        } catch (e: Exception) {
            toast(e.localizedMessage ?: getString(R.string.unknown_error_occurred))
        }
    }


    private fun buildScheduledMessage(
        text: String, subscriptionId: Int, messageId: Long
    ): Message {
        val threadId = if (chatAdapter?.messages?.isEmpty() == true) messageId else threadId
        return Message(
            messageId = messageId,
            body = text,
            type = Telephony.Sms.MESSAGE_TYPE_QUEUED,
            date = scheduledDateTime,
            read = false,
            threadId = threadId,
            addresses = sendAddresses.map { it.normalizePhoneNumber() },
            name = "",
            subId = subscriptionId,
            isMMS = isMmsMessage(text),
            isScheduled = true,
            attachmentWithMessageModel = AttachmentWithMessageModel(
                messageId, text, attachmentAdapter.attachmentList
            )
        )
    }

    private fun isMmsMessage(text: String): Boolean {
        val isLongMmsMessage = isLongMmsMessage(text)
        return attachmentAdapter.attachmentList.isNotEmpty() || isLongMmsMessage
    }

    private fun sendNormalMessage(text: String, subscriptionId: Int) {
        val attachments = attachmentAdapter.attachmentList
        try {
            refreshedSinceSent = false
            sendMessageCompat(text, sendAddresses, subscriptionId, attachments)
            backgroundScope.launch {
                messageRepository.syncMessages(threadId) { newMessages ->
                    mainScope.launch {
                        chatAdapter?.apply {
                            val lastMessageTime = if (messages.size > 0) {
                                messages.last().message.date
                            } else 0
                            val messagesToAdd = newMessages.filter { it.date > lastMessageTime }
                            messagesToAdd.forEach {
                                addMessage(MessagesWithDate.SendMessage(it))
                            }
                            messages.lastIndex.let {
                                binding.rcvChats.smoothScrollToPosition(it)
                            }
                        }
                        binding.tvNoMessage.isVisible = false
                        binding.rcvChats.isVisible = true
                    }
                }
            }
            isConversationUpdate = true
            clearCurrentMessage()
        } catch (e: Exception) {
            toast(msg = e.message.toString())
        } catch (e: Error) {
            toast(e.localizedMessage ?: getString(R.string.unknown_error_occurred))
        }
    }

    private fun updateSendButtonDrawable() {
        val drawableResId = if (isScheduledMessage) {
            R.drawable.ic_schedule_send
        } else {
            R.drawable.ic_send
        }
        ResourcesCompat.getDrawable(resources, drawableResId, theme)?.apply {
            binding.clChatBottomItem.ivSend.setImageDrawable(this)
        }

    }

    private fun clearCurrentMessage() {
        binding.clChatBottomItem.apply {
            attachmentAdapter.attachmentList.clear()
            evMessage.setText("")
            clMessageAdditional.isVisible = false
            rcvAttachments.isVisible = false
            emojiPicker.isVisible = false
            clAttachments.isVisible = false
            clClipboardData.isVisible = false
            dismissKeyboard(evMessage)
            ivAddToMessage.apply {
                rotation = 45F
                loadImageDrawable(R.drawable.ic_close)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        handleLocationPermission {
            if (permissionManager.hasLocation()) {
                val locationTask: Task<Location> =
                    LocationServices.getFusedLocationProviderClient(this).lastLocation
                locationTask.addOnSuccessListener { location ->
                    if (location != null) {
                        // Get latitude and longitude
                        val latitude = location.latitude
                        val longitude = location.longitude

                        // Get address using Geocoder
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses =
                            geocoder.getFromLocation(latitude, longitude, 1) as MutableList

                        if (addresses.isNotEmpty()) {
                            val address = addresses[0].getAddressLine(0)
                            addMessageToEditView(address)
                        } else {
                            toast("Unable to fetch address")
                        }
                    } else {
                        toast("Location not found")
                    }
                }
                locationTask.addOnFailureListener {
                    toast("Failed to get location")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mainScope.launch {
            initWallpaper()
            if ((appPreferences.chatBubbleBgSend != currentChatBubbleBgSend)
                || (appPreferences.chatBubbleBgReceived != currentChatBubbleBgReceived)
            ) {
                chatAdapter?.refreshChatAdapter()
            }
        }
        checkAndAskDefaultApp()
    }

    private fun initWallpaper() {
        binding.apply {
            if (appPreferences.isShowDesignOnWallpaper) {
                ivChatDesign.isVisible = true
                clChatMain.setBackgroundTint(appPreferences.chatWallpaperBg)
                ivChatDesign.loadImageDrawable(R.drawable.ic_wallpaper_bg)
            } else if (appPreferences.isShowCustomWallpaper) {
                ivChatDesign.isVisible = true
                clChatMain.setBackgroundTint(getColorForId(R.color.app_bg))
                ivChatDesign.loadImageWithOutCache(appPreferences.customWallpaperPath)
            } else {
                ivChatDesign.isVisible = false
                clChatMain.setBackgroundTint(getColorForId(R.color.app_bg))
            }
        }
    }

    override fun onGetEvent(event: MessageEvent) {
        when (event) {
            is OnLanguageSelect -> {
                viewModel.translateText(
                    this@ChatActivity,
                    event.code,
                    if (translateMessage.second.isMMS) translateMessage.second.attachmentWithMessageModel?.text
                        ?: "" else translateMessage.second.body
                )
            }

            is OnSelectClipboardData -> {
                addMessageToEditView(event.clipText)
            }

            is OnPickedContact -> {
                addMessageToEditView(event.contactData)
                EventBus.getDefault().removeStickyEvent(event)
            }

            is UpdateLastSendMessageStatus -> {
                backgroundScope.launch {
                    delay(2500)
                    if ((chatAdapter?.messages?.size ?: 0) > 1) {
                        val sendMessage =
                            chatAdapter?.messages?.find { it.message.messageId == event.messageId }
                        if (sendMessage is MessagesWithDate.SendMessage) {
                            mainScope.launch {
                                chatAdapter?.apply {
                                    sendMessage.message.type = event.type
                                    sendMessage.message.deliveryStatus = event.status
                                    notifyItemChanged(messages.size - 1)
                                }
                            }
                        }
                    }
                }
            }

            is OnReceiveMessage -> {
                if (event.message.threadId == threadId) {
                    backgroundScope.launch {
                        messageRepository.updateMessageAsRead(event.message.threadId)
                    }
                    mainScope.launch {
                        chatAdapter?.addMessage(MessagesWithDate.ReceivedMessage(message = event.message))
                        chatAdapter?.messages?.lastIndex?.let {
                            binding.rcvChats.smoothScrollToPosition(it)
                        }
                        binding.tvNoMessage.isVisible = false
                        binding.rcvChats.isVisible = true
                        isConversationUpdate = true
                    }
                    EventBus.getDefault()
                        .postSticky(OnReceiveMessageConversation(message = event.message, true))
                }
            }

            is RefreshScheduleMessage -> {
                backgroundScope.launch {
                    messageRepository.syncMessages(threadId) { newMessages ->
                        mainScope.launch {
                            chatAdapter?.apply {
                                val scheduleMessage =
                                    messages.first { it.message.messageId == event.messageId }
                                val sendScheduleMessage =
                                    newMessages.first { it.body == scheduleMessage.message.body && it.addresses == scheduleMessage.message.addresses }

                                val index = messages.indexOf(scheduleMessage)
                                messages.removeAt(index)
                                messages.add(
                                    index,
                                    MessagesWithDate.SendMessage(sendScheduleMessage)
                                )
                                refreshChatAdapter()
                            }
                            binding.tvNoMessage.isVisible = false
                            binding.rcvChats.isVisible = true
                        }
                    }
                }
            }

            else -> {}
        }
    }

    private fun addMessageToEditView(msg: String) {
        mainScope.launch {
            binding.clChatBottomItem.apply {
                clClipboardData.isVisible = false
                clAttachments.isVisible = false
                evMessage.setText(msg)
                ivAddToMessage.apply {
                    rotation = 45F
                    loadImageDrawable(R.drawable.ic_close)
                }
            }
        }
    }

    private fun initClipboardData() {
        binding.clChatBottomItem.apply {
            vpClipboardData.adapter = ClipboardViewPagerAdapter(this@ChatActivity)
            clipboardDataTabs.tabMode = TabLayout.MODE_SCROLLABLE
            TabLayoutMediator(
                clipboardDataTabs, vpClipboardData
            ) { tab: TabLayout.Tab, position: Int ->
                tab.setText(getClipboardTypeData(position))
            }.attach()
        }
    }

}