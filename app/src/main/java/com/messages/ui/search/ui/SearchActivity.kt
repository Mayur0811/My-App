package com.messages.ui.search.ui

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.data.repository.MessageRepository
import com.messages.databinding.ActivitySearchBinding
import com.messages.extentions.copyToClipboard
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.ui.search.adapter.SearchAdapter
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.THREAD_ID
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    private val binding by lazy { ActivitySearchBinding.inflate(layoutInflater) }

    private val searchAdapter = SearchAdapter()

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initActions()
        initAdapter()
        initObserver()

        mainScope.launch {
            delay(500)
            binding.apply {
                evSearch.requestFocus()
                showKeyboard(evSearch)
            }
        }
    }

    private fun initAdapter() {
        binding.rcvSearchedConversations.apply {
            adapter = searchAdapter
        }
    }

    private fun initActions() {
        binding.apply {
            ivCloseSearch.setOnSafeClickListener {
                evSearch.setText("")
                clNotFoundAny.isVisible = true
                rcvSearchedConversations.isVisible = false
                ivCloseSearch.isVisible = false
            }
        }

        searchAdapter.apply {
            onClickConversation = { conversation ->
                launchActivity(ChatActivity::class.java) {
                    putLong(THREAD_ID, conversation.threadId)
                    putString(CONVERSATION_TITLE, conversation.name)
                }
            }

            onClickCopyOTP = { otp ->
                if (otp.isNotEmpty()) {
                    copyToClipboard(otp)
                }
            }
        }
    }

    private fun initObserver() {
        binding.apply {
            evSearch.addTextChangedListener {
                if (!evSearch.text.isNullOrEmpty() && evSearch.text.toString().trim().length >= 2) {
                    handleSearch(evSearch.text.toString())
                    ivCloseSearch.isVisible = true
                } else {
                    clNotFoundAny.isVisible = true
                    rcvSearchedConversations.isVisible = false
                    ivCloseSearch.isVisible = false
                }
            }
        }
    }

    private fun handleSearch(searchText: String) {
        backgroundScope.launch {
            val conversations = messageRepository.findConversationBySearch(searchText)
            mainScope.launch {
                binding.apply {
                    if (conversations.isNotEmpty()) {
                        searchAdapter.updateConversations(conversations)
                        clNotFoundAny.isVisible = false
                        rcvSearchedConversations.isVisible = true
                    } else {
                        clNotFoundAny.isVisible = true
                        rcvSearchedConversations.isVisible = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}