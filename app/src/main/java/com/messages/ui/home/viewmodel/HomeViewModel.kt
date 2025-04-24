package com.messages.ui.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messages.data.database.dao.MessageDao
import com.messages.data.repository.MessageRepository
import com.messages.data.models.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val messageDao: MessageDao
) : ViewModel() {

//    private val _homeScreenState: MutableStateFlow<HomeScreenState> =
//        MutableStateFlow(HomeScreenState.OnStart)
//    val homeScreenState: MutableStateFlow<HomeScreenState> = _homeScreenState

}


sealed class HomeScreenState {

    data object OnStart : HomeScreenState()
    data object OnLoading : HomeScreenState()

    data class OnGetConversation(val conversation: ArrayList<Conversation>? = null) :
        HomeScreenState()

    data class OnFail(val message: String? = null) : HomeScreenState()

}