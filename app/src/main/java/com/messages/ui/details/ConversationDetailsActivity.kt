package com.messages.ui.details

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.getThreadId
import com.messages.data.events.DeleteConversation
import com.messages.data.events.HandleConversation
import com.messages.data.models.Conversation
import com.messages.data.models.Recipient
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivityConversationDetailsBinding
import com.messages.extentions.dialNumber
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.common_dialog.CustomDialog
import com.messages.ui.details.adapter.RecipientAdapter
import com.messages.ui.home.ui.HomeActivity
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.THREAD_ID
import com.messages.utils.THREAD_NUMBER
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@AndroidEntryPoint
class ConversationDetailsActivity : BaseActivity() {

    private val binding by lazy { ActivityConversationDetailsBinding.inflate(layoutInflater) }

    @Inject
    lateinit var messageRepository: MessageRepository

    private var threadId = 0L
    private var conversation: Conversation? = null
    private var recipients: ArrayList<Recipient> = arrayListOf()
    private var isArchive: Boolean = false
    private var isBlock: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initData()
        initActions()
    }

    private fun initActions() {
        binding.apply {
            detailsHeader.ivDetailsCall.setOnSafeClickListener {
                dialNumber(recipients.first().address)
            }
            menuArchive.root.setOnSafeClickListener {
                isArchive = !isArchive
                backgroundScope.launch {
                    messageRepository.handleConversationForArchived(
                        arrayListOf(threadId),
                        isArchive
                    )
                    initArchiveMenu()
                    mainScope.launch {
                        conversation?.let {
                            EventBus.getDefault()
                                .postSticky(HandleConversation(it, Pair(isArchive, isBlock)))
                        }
                    }
                }
            }

            menuBlock.root.setOnSafeClickListener {
                blockConversationAddress()
            }

            menuDelete.root.setOnSafeClickListener {
                deleteConversation()
            }
        }
    }


    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.conversation_details)
            ivToolbarBack.setOnSafeClickListener { finish() }
        }
    }

    private fun initData() {
        backgroundScope.launch {
            threadId = intent.getLongExtra(THREAD_ID, 0)
            conversation = messageRepository.getConversationOfThreadId(threadId)
            recipients = conversation?.recipients as ArrayList
            isArchive = conversation?.isArchived ?: false
            isBlock = conversation?.isBlocked ?: false
            initHeaderView()
            initArchiveMenu()
            initBlockMenu()
            initDeleteMenu()
        }
    }

    private fun initDeleteMenu() {
        mainScope.launch {
            binding.menuDelete.apply {
                ivDetailsIcon.loadImageDrawable(R.drawable.ic_delete)
                tvDetailsTitle.text = getStringValue(R.string.delete_conversation)
            }
        }
    }

    private fun initBlockMenu() {
        mainScope.launch {
            binding.menuBlock.apply {
                root.isVisible = recipients.size == 1
                ivDetailsIcon.loadImageDrawable(R.drawable.ic_block_app_color)
                tvDetailsTitle.text =
                    getStringValue(if (isBlock) R.string.unblock else R.string.block)
            }
        }
    }

    private fun initArchiveMenu() {
        mainScope.launch {
            binding.menuArchive.apply {
                ivDetailsIcon.loadImageDrawable(if (isArchive) R.drawable.ic_unarchive else R.drawable.ic_archive_action)
                tvDetailsTitle.text =
                    getStringValue(if (isArchive) R.string.unarchive else R.string.archive)
            }
        }
    }

    private fun initHeaderView() {
        mainScope.launch {
            binding.detailsHeader.apply {
                if (recipients.size > 1) {
                    clGroupConversation.isVisible = true
                    clSingleRecipient.isVisible = false
                    rcvRecipient.adapter = RecipientAdapter(recipients) { action, recipient ->
                        if (action == 0) {
                            dialNumber(recipient.address)
                        } else {
                            launchThreadActivity(recipient.address, recipient.getDisplayName())
                        }
                    }

                } else {
                    clGroupConversation.isVisible = false
                    clSingleRecipient.isVisible = true
                    avatar.recipients = recipients
                    tvConversationTitle.text =
                        recipients.joinToString { recipient -> recipient.getDisplayName() }
                    tvConversationDesc.text =
                        recipients.joinToString { recipient -> recipient.address }
                }
            }
        }
    }

    private fun launchThreadActivity(phoneNumber: String, name: String) {
        if (phoneNumber.isEmpty()) {
            return
        }
        launchActivity(ChatActivity::class.java) {
            putLong(THREAD_ID, getThreadId(phoneNumber))
            putString(CONVERSATION_TITLE, name)
            putString(THREAD_NUMBER, phoneNumber)
        }
        finish()
    }

    private fun blockConversationAddress() {
        val blockUnblock = getStringValue(if (isBlock) R.string.unblock else R.string.block)
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = blockUnblock,
            dialogDisc = getString(R.string.want_to_block_conversation, blockUnblock),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = blockUnblock,
            onPositive = {
                isBlock = !isBlock
                messageRepository.handleConversationForBlocked(arrayListOf(threadId), isBlock)
                initBlockMenu()
                mainScope.launch {
                    conversation?.let {
                        EventBus.getDefault().postSticky(HandleConversation(it, Pair(isArchive, isBlock)))
                    }
                }
            })
        deleteDialog.show()
    }

    private fun deleteConversation() {
        val deleteDialog = CustomDialog(context = this,
            dialogTitle = getStringValue(R.string.delete),
            dialogDisc = getStringValue(R.string.want_to_delete_conversation),
            negativeBtnText = getStringValue(R.string.cancel),
            positiveBtnText = getStringValue(R.string.delete),
            onPositive = {
                messageRepository.deleteConversation(listOf(threadId))
                EventBus.getDefault().postSticky(DeleteConversation(threadId))
                launchActivity(
                    HomeActivity::class.java,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                finish()
            })
        deleteDialog.show()
    }
}