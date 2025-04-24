package com.messages.ui.new_chat.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import androidx.activity.OnBackPressedCallback
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.getThreadIdOfGroup
import com.messages.data.events.OnPickedContact
import com.messages.data.models.Contact
import com.messages.data.models.PhoneNumber
import com.messages.data.models.SelectionModel
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityContactBinding
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomSelectionDialog
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.new_chat.adapter.ContactAdapter
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.IS_CONTACT_PICK
import com.messages.utils.IS_FROM_CALL_END
import com.messages.utils.IS_PRIVATE_CONVERSATION
import com.messages.utils.SEND_TO_NUMBER
import com.messages.utils.SMS_BODY
import com.messages.utils.THREAD_ATTACHMENT_URI
import com.messages.utils.THREAD_ATTACHMENT_URIS
import com.messages.utils.THREAD_ID
import com.messages.utils.THREAD_NUMBER
import com.messages.utils.THREAD_TEXT
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@AndroidEntryPoint
class ContactActivity : BaseActivity() {

    private val binding by lazy { ActivityContactBinding.inflate(layoutInflater) }
    private var contactAdapter = ContactAdapter()
    private val searchAdapter = ContactAdapter()
    private var isNumericKeyboard = false
    private var smsBody: String = ""
    private var searchText: Editable? = null
    private var isFromCallEnd = false
    private var number: String? = null

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
        if (number == null) {
            setContentView(binding.root)
            initToolbar()
            initAdapter()
            initAction()
            initObserver()
            val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isFromCallEnd) {
                        launchActivity(
                            HomeActivity::class.java,
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                    finish()
                }
            }
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }

    private fun initObserver() {
        binding.apply {
            evSearchContact.addTextChangedListener {
                searchText = evSearchContact.text
                if (!searchText.isNullOrEmpty() && searchText.toString().trim().length >= 2
                ) {
                    handleSearch()
                    rcvSearchContactsIndex.isVisible = true
                    rcvContactsIndex.isVisible = false
                } else {
                    rcvSearchContactsIndex.isVisible = false
                    rcvContactsIndex.isVisible = true
                }
            }
        }
    }

    private fun handleShowSendTo(isShow: Boolean) {
        binding.apply {
            if (searchText?.isDigitsOnly() == true && isShow) {
                clSendToNumber.isVisible = true
                tvSendToName.text = resources.getString(R.string.sens_to, searchText)
                tvSendToNumber.text = searchText
            } else {
                clSendToNumber.isVisible = false
            }
        }
    }

    private fun handleSearch() {
        backgroundScope.launch {
            val contacts = messageRepository.findContactBySearch(searchText.toString())
            mainScope.launch {
                handleShowSendTo(contacts.isEmpty())
                searchAdapter.contacts.apply {
                    clear()
                    addAll(contacts)
                }
                searchAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun initAction() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                finish()
            }

            ivKeyboardChange.setOnSafeClickListener {
                if (isNumericKeyboard) {
                    evSearchContact.setInputType(InputType.TYPE_CLASS_TEXT)
                    ivKeyboardChange.loadImageDrawable(R.drawable.ic_numeric_keyboard)
                } else {
                    evSearchContact.setInputType(InputType.TYPE_CLASS_NUMBER)
                    ivKeyboardChange.loadImageDrawable(R.drawable.ic_qwerty_keyboard)
                }
                isNumericKeyboard = !isNumericKeyboard
            }

            clSendToNumber.setOnSafeClickListener {
                dismissKeyboard(evSearchContact)
                launchThreadActivity(searchText.toString(), searchText.toString(), true)
            }

            contactAdapter.apply {
                onClickContact = { contact ->
                    if (intent.getBooleanExtra(IS_CONTACT_PICK, false)) {
                        onPickContact(contact)
                    } else {
                        onclickContract(contact)
                    }
                }
            }

            searchAdapter.onClickContact = { contact ->
                if (intent.getBooleanExtra(IS_CONTACT_PICK, false)) {
                    onPickContact(contact)
                } else {
                    onclickContract(contact)
                }
            }
        }


    }

    private fun onPickContact(contact: Contact) {
        val phoneNumbers = contact.phoneNumbers
        if (phoneNumbers.size > 1) {
            val items = ArrayList<SelectionModel>()
            phoneNumbers.forEachIndexed { index, phoneNumber ->
                items.add(SelectionModel(index, phoneNumber.address, phoneNumber))
            }

            CustomSelectionDialog(activity = this@ContactActivity, items = items) {
                val pickedContactData =
                    getString(R.string.picked_contact, contact.name, (it as PhoneNumber).address)
                EventBus.getDefault().postSticky(OnPickedContact(pickedContactData))
                finish()
            }
        } else {
            val pickedContactData =
                getString(R.string.picked_contact, contact.name, phoneNumbers.first().address)
            EventBus.getDefault().postSticky(OnPickedContact(pickedContactData))
            finish()
        }
    }

    private fun onclickContract(contact: Contact) {
        dismissKeyboard(binding.evSearchContact)
        val phoneNumbers = contact.phoneNumbers
        if (phoneNumbers.size > 1) {
            val items = ArrayList<SelectionModel>()
            phoneNumbers.forEachIndexed { index, phoneNumber ->
                items.add(SelectionModel(index, phoneNumber.address, phoneNumber))
            }

            CustomSelectionDialog(activity = this@ContactActivity, items = items) {
                launchThreadActivity((it as PhoneNumber).address, contact.name)
            }
        } else {
            launchThreadActivity(phoneNumbers.first().address, contact.name)
        }
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.new_chat)
        }
    }

    private fun initAdapter() {
        binding.apply {
            rcvSearchContactsIndex.apply {
                layoutManager = LinearLayoutManager(this@ContactActivity)
                adapter = searchAdapter
                setIndexTextSize(14)
                setIndexBarColor(R.color.app_gray)
                setIndexBarCornerRadius(10)
                setIndexBarTransparentValue(0.4f)
                setIndexBarTopMargin(30f)
                setIndexBarBottomMargin(30f)
                setPreviewPadding(5)
                setIndexBarHorizontalMargin(10f)
                setIndexBarTextColor(R.color.app_blue)
                setPreviewTextSize(50)
                setPreviewColor(R.color.app_color)
                setPreviewTextColor(R.color.only_white)
                setPreviewTransparentValue(0.6f)
                setIndexBarStrokeVisibility(false)
                setIndexBarWidth(40F)
                setIndexBarVisibility(true)
            }

            rcvContactsIndex.apply {
                layoutManager = LinearLayoutManager(this@ContactActivity)
                adapter = contactAdapter
                setIndexTextSize(14)
                setIndexBarColor(R.color.app_gray)
                setIndexBarCornerRadius(10)
                setIndexBarTransparentValue(0.4f)
                setIndexBarTopMargin(30f)
                setIndexBarBottomMargin(30f)
                setIndexBarHorizontalMargin(10f)
                setPreviewPadding(5)
                setIndexBarTextColor(R.color.app_blue)
                setPreviewTextSize(50)
                setPreviewColor(R.color.app_color)
                setPreviewTextColor(R.color.only_white)
                setPreviewTransparentValue(0.6f)
                setIndexBarStrokeVisibility(false)
                setIndexBarWidth(40F)
                setIndexBarVisibility(true)
            }
        }
    }

    private fun initData() {
        number = intent.dataString
        isFromCallEnd = intent.getBooleanExtra(IS_FROM_CALL_END, false)
        if (number != null) {
            var phoneNumber = number!!.split(":").last()
            if (phoneNumber.startsWith("%2B")) {
                phoneNumber = phoneNumber.replace("%2B", "+")
            }
            if (phoneNumber.startsWith("+91")) {
                phoneNumber = phoneNumber.removePrefix("+91")
            }
            launchThreadActivity(phoneNumber, phoneNumber)
        } else {
            backgroundScope.launch {
                val allContacts = messageRepository.getContacts()
                mainScope.launch {
                    contactAdapter.contacts.addAll(allContacts)
                    contactAdapter.notifyItemInserted(contactAdapter.contacts.size)
                    binding.rcvContactsIndex.updateSections()
                }
            }
        }
    }

    private fun launchThreadActivity(
        phoneNumber: String,
        name: String,
        isFromSendToNumber: Boolean = false
    ) {
        backgroundScope.launch {
            if (phoneNumber.isEmpty()) {
                return@launch
            }
            if (intent.hasExtra(SMS_BODY)) {
                smsBody = intent.getStringExtra(SMS_BODY) ?: ""
            }
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val numbers = phoneNumber.split(";").toSet()
            val number = if (numbers.size == 1) phoneNumber else Gson().toJson(numbers)
            val threadId = getThreadIdOfGroup(numbers)
            val conversation = messageRepository.getConversationFromThreadId(threadId)

            mainScope.launch {
                launchActivity(ChatActivity::class.java) {
                    putLong(THREAD_ID, threadId)
                    putString(
                        CONVERSATION_TITLE,
                        if (name != phoneNumber) name else conversation?.name ?: name
                    )
                    putString(THREAD_TEXT, text + smsBody)
                    putString(THREAD_NUMBER, number)
                    putBoolean(
                        SEND_TO_NUMBER,
                        if (!isFromSendToNumber) conversation == null else true
                    )
                    putBoolean(
                        IS_PRIVATE_CONVERSATION,
                        intent.getBooleanExtra(IS_PRIVATE_CONVERSATION, false)
                    )
                    putBoolean(IS_FROM_CALL_END, isFromCallEnd)
                    if (intent.action == Intent.ACTION_SEND && intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        putString(THREAD_ATTACHMENT_URI, uri?.toString())
                    } else if (intent.action == Intent.ACTION_SEND_MULTIPLE && intent.extras?.containsKey(
                            Intent.EXTRA_STREAM
                        ) == true
                    ) {
                        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                        putSerializable(THREAD_ATTACHMENT_URIS, uris)
                    }
                }
                finish()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }

}