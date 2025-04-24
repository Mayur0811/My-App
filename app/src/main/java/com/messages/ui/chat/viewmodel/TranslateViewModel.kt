package com.messages.ui.chat.viewmodel

import android.content.Context
import android.telephony.SmsManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.messages.R
import com.messages.data.models.TranslateLanguageModel
import com.messages.extentions.getStringValue
import com.messages.extentions.toast
import com.messages.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslateViewModel @Inject constructor() : ViewModel() {

    private val _translatedText: MutableStateFlow<TranslateState> =
        MutableStateFlow(TranslateState.Start)
    val translatedText: StateFlow<TranslateState> get() = _translatedText

    val availableLanguages: ArrayList<TranslateLanguageModel> =
        TranslateLanguage.getAllLanguages().map { TranslateLanguageModel(it) } as ArrayList

    private fun getTranslator(
        sourceLanguageCode: String = TranslateLanguage.ENGLISH, targetLanguageCode: String
    ): Translator {
        val options = TranslatorOptions.Builder().setSourceLanguage(sourceLanguageCode)
            .setTargetLanguage(targetLanguageCode).build()
        return Translation.getClient(options)
    }

    private fun checkLanguageModelAvailability(languageCode: String, onCheck: (Boolean) -> Unit) {
        viewModelScope.launch {
            val modelManager = RemoteModelManager.getInstance()
            val remoteModel = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.isModelDownloaded(remoteModel).addOnSuccessListener {
                onCheck.invoke(it)
            }.addOnFailureListener {
                AppLogger.e(message = "LanguageModelAvailability -> ${it.message}")
                onCheck.invoke(false)
            }.addOnCanceledListener {
                onCheck.invoke(false)
            }
        }
    }


    fun translateText(context: Context, languageCode: String, sourceString: String) {
        viewModelScope.launch {
            val translator = getTranslator(targetLanguageCode = languageCode)
            _translatedText.value = TranslateState.Loading
            checkLanguageModelAvailability(languageCode) { isAvailable ->
                if (isAvailable) {
                    startTranslate(context, translator, sourceString)
                } else {
                    downloadLanguageModel(context, languageCode) {
                        startTranslate(context, translator, sourceString)
                    }
                }
            }
        }
    }

    private fun downloadLanguageModel(
        context: Context,
        languageCode: String,
        onDownloadModel: () -> Unit
    ) {
        viewModelScope.launch {
            _translatedText.value = TranslateState.Loading
            val translator = getTranslator(targetLanguageCode = languageCode)
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    onDownloadModel.invoke()
                }.addOnFailureListener {
                    AppLogger.d(message = "downloadModel fail-> ${it.message}")
                    context.toast(msg = context.getStringValue(R.string.language_model_error))
                }.addOnCanceledListener {
                    context.toast(msg = context.getStringValue(R.string.language_model_error))
                }
        }
    }

    private fun startTranslate(context: Context, translator: Translator, sourceString: String) {
        viewModelScope.launch {
            translator.translate(sourceString)
                .addOnSuccessListener { translatedText ->
                    _translatedText.value = TranslateState.OnTranslate(translatedText)
                }.addOnFailureListener { exception ->
                    AppLogger.d(message = "downloadModel fail-> ${exception.message}")
                    context.toast(msg = context.getStringValue(R.string.language_translate_error))
                }.addOnCompleteListener {
                    translator.close()
                }
        }
    }

}

sealed class TranslateState {
    data object Start : TranslateState()
    data object Loading : TranslateState()
    data class OnTranslate(val text: String? = null) : TranslateState()
}