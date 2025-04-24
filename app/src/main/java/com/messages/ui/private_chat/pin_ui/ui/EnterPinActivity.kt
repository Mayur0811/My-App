package com.messages.ui.private_chat.pin_ui.ui

import android.os.Bundle
import android.widget.EditText
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.mainScope
import com.messages.databinding.ActivityEnterPinBinding
import com.messages.extentions.decrypt
import com.messages.extentions.encrypt
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.setBackgroundTint
import com.messages.extentions.toast
import com.messages.ui.base.BaseActivity
import com.messages.ui.private_chat.chat.ui.PrivateChatActivity
import com.messages.ui.private_chat.confirm_n_security_pin.SecurityPrivateActivity
import com.messages.ui.private_chat.pin_ui.adapter.DialPadAdapter
import com.messages.utils.CHANGE_PASSWORD
import com.messages.utils.IS_FORGOT_PASSWORD
import com.messages.utils.IS_SHOW_SECURITY_QA
import com.messages.utils.PRIVATE_VAULT_PASSWORD
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EnterPinActivity : BaseActivity() {

    private val binding by lazy { ActivityEnterPinBinding.inflate(layoutInflater) }
    private var passwordLetter = ArrayList<String>(4)
    private var userPassword = ""
    private val editTextViews = arrayListOf<EditText>()
    private var isFromChangePassword = false
    private var isForgotPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initData()
        initViews()
        initDailPad()
    }

    private fun initData() {
        isFromChangePassword = intent?.getBooleanExtra(CHANGE_PASSWORD, false) == true
        isForgotPassword = intent?.getBooleanExtra(IS_FORGOT_PASSWORD, false) == true
    }


    private fun initViews() {
        binding.apply {
            toolbar.getBinding().apply {
                tvToolbarTitle.text =
                    getStringValue(if (appPreferences.isPinSet && (!isFromChangePassword && !isForgotPassword)) R.string.private_chat else R.string.set_pin)

                ivToolbarBack.setOnSafeClickListener {
                    finish()
                }
            }
            tvForgotPassword.apply {
                isVisible = appPreferences.isPinSet && !isFromChangePassword && !isForgotPassword
                setOnSafeClickListener {
                    launchActivity(SecurityPrivateActivity::class.java) {
                        putBoolean(IS_SHOW_SECURITY_QA, true)
                        putBoolean(IS_FORGOT_PASSWORD, true)
                    }
                }
            }
        }
    }

    private fun initDailPad() {
        editTextViews.add(binding.evPasswordFirstLetter)
        editTextViews.add(binding.evPasswordSecondLetter)
        editTextViews.add(binding.evPasswordThirdLetter)
        editTextViews.add(binding.evPasswordForthLetter)
        val dialPadButtons: ArrayList<String> = arrayListOf(
            getStringValue(R.string.number_1),
            getStringValue(R.string.number_2),
            getStringValue(R.string.number_3),
            getStringValue(R.string.number_4),
            getStringValue(R.string.number_5),
            getStringValue(R.string.number_6),
            getStringValue(R.string.number_7),
            getStringValue(R.string.number_8),
            getStringValue(R.string.number_9),
            getStringValue(R.string.number_null),
            getStringValue(R.string.number_0),
            getStringValue(R.string.backspace)
        )

        binding.rcvDialPad.apply {
            hasFixedSize()
            adapter = DialPadAdapter(dialPadButtons) { number ->
                if (number == getStringValue(R.string.backspace)) {
                    handleBackSpacePassword()
                } else {
                    handlePasswordLetters(number)
                }
            }
            binding.evPasswordFirstLetter.requestFocus()
        }
    }

    private fun handlePasswordLetters(number: String) {
        if (passwordLetter.size == 4) {
            passwordLetter.clear()
        }
        binding.apply {
            if (evPasswordFirstLetter.isFocused) {
                passwordLetter.add(number)
                setBackgroundColorOnEdittext(editText = evPasswordFirstLetter, isFill = true)
                evPasswordSecondLetter.requestFocus()
            } else if (evPasswordSecondLetter.isFocused) {
                passwordLetter.add(number)
                setBackgroundColorOnEdittext(editText = evPasswordSecondLetter, isFill = true)
                evPasswordThirdLetter.requestFocus()
            } else if (evPasswordThirdLetter.isFocused) {
                passwordLetter.add(number)
                setBackgroundColorOnEdittext(editText = evPasswordThirdLetter, isFill = true)
                evPasswordForthLetter.requestFocus()
            } else if (evPasswordForthLetter.isFocused) {
                passwordLetter.add(number)
                setBackgroundColorOnEdittext(editText = evPasswordForthLetter, isFill = true)

                if (!appPreferences.isPinSet || isFromChangePassword || isForgotPassword) {
                    mainScope.launch {
                        delay(500)
                        evPasswordFirstLetter.requestFocus()
                        tvEnterPassword.apply {
                            text = getStringValue(R.string.confirm_your_password)
                            setTextColor(getColorForId(R.color.app_blue))
                        }
                        setBackgroundColorOnEdittext(evPasswordFirstLetter, isAll = true)
                    }
                }

            }

            handlePassword()
        }
    }

    private fun handlePassword() {
        if (passwordLetter.size == 4) {
            if (appPreferences.isPinSet && !isFromChangePassword && !isForgotPassword) {
                val enteredPassword = passwordLetter.joinToString("")
                if (enteredPassword == appPreferences.privateVaultPassword.decrypt()) {
                    launchActivity(PrivateChatActivity::class.java)
                    finish()
                } else {
                    mainScope.launch {
                        binding.apply {
                            tvEnterPassword.apply {
                                text = getStringValue(R.string.incorrect_password)
                                setTextColor(getColorForId(R.color.app_error))
                            }
                            setBackgroundColorOnEdittext(evPasswordFirstLetter, isAll = true)
                            evPasswordFirstLetter.requestFocus()
                            passwordLetter.clear()
                            delay(1000)
                            tvEnterPassword.apply {
                                text = getStringValue(R.string.enter_correct_password)
                                setTextColor(getColorForId(R.color.app_blue))
                            }
                        }
                    }
                }
            } else {
                if (userPassword == "") {
                    userPassword = passwordLetter.joinToString("")
                } else {
                    val confirmPassword = passwordLetter.joinToString("")
                    if (confirmPassword == userPassword) {
                        if (!isFromChangePassword && !isForgotPassword) {
                            launchActivity(SecurityPrivateActivity::class.java) {
                                putString(PRIVATE_VAULT_PASSWORD, userPassword)
                                putBoolean(IS_SHOW_SECURITY_QA, false)
                            }
                        } else {
                            appPreferences.privateVaultPassword =
                                userPassword.encrypt() ?: userPassword
                            toast(msg = getStringValue(R.string.password_change_successfully))
                        }
                        finish()
                    } else {
                        binding.apply {
                            tvEnterPassword.apply {
                                text = getStringValue(R.string.incorrect_password)
                                setTextColor(getColorForId(R.color.app_error))
                            }
                            setBackgroundColorOnEdittext(evPasswordFirstLetter, isAll = true)
                            evPasswordFirstLetter.requestFocus()
                            passwordLetter.clear()
                        }
                    }
                }
            }
        }
    }

    private fun handleBackSpacePassword() {
        binding.apply {
            if (passwordLetter.size > 0) {
                passwordLetter.removeAt(passwordLetter.lastIndex)
            }
            if (evPasswordSecondLetter.isFocused) {
                setBackgroundColorOnEdittext(editText = evPasswordFirstLetter, isFill = false)
                evPasswordFirstLetter.requestFocus()
            } else if (evPasswordThirdLetter.isFocused) {
                setBackgroundColorOnEdittext(editText = evPasswordSecondLetter, isFill = false)
                evPasswordSecondLetter.requestFocus()
            } else if (evPasswordForthLetter.isFocused) {
                setBackgroundColorOnEdittext(editText = evPasswordThirdLetter, isFill = false)
                evPasswordThirdLetter.requestFocus()
            }
        }
    }

    private fun setBackgroundColorOnEdittext(
        editText: EditText,
        isFill: Boolean = false,
        isAll: Boolean = false,
    ) {
        editTextViews.forEach {
            if (isAll) {
                it.setBackgroundTint(getColorForId(if (isFill) R.color.app_blue else R.color.app_light_gray))
            } else {
                if (it.id == editText.id) {
                    it.setBackgroundTint(getColorForId(if (isFill) R.color.app_blue else R.color.app_light_gray))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}