package com.messages.ui.private_chat.confirm_n_security_pin

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.messages.R
import com.messages.databinding.ActivitySecurityPrivateBinding
import com.messages.extentions.encrypt
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.private_chat.chat.ui.PrivateChatActivity
import com.messages.ui.private_chat.pin_ui.ui.EnterPinActivity
import com.messages.utils.IS_FORGOT_PASSWORD
import com.messages.utils.IS_SHOW_SECURITY_QA
import com.messages.utils.PRIVATE_VAULT_PASSWORD
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SecurityPrivateActivity : BaseActivity() {

    private val binding by lazy { ActivitySecurityPrivateBinding.inflate(layoutInflater) }
    private var privateVaultPassword = ""
    private var isShowSecurityQA = false
    private var isForgotPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initData()
        initToolbar()
        initActions()
        initSpinner()
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text =
                getStringValue(if (isShowSecurityQA) R.string.secret_question else R.string.set_pin)
        }
    }

    private fun initActions() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                finish()
            }

            spinnerSecurityQA.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    wrongQaAndAnswer.isVisible = false
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

            evAnswer.addTextChangedListener{
                wrongQaAndAnswer.isVisible = false
            }

            btnSetNow.setOnSafeClickListener {
                appPreferences.privateVaultPassword =
                    privateVaultPassword.encrypt() ?: privateVaultPassword
                launchActivity(SecurityPrivateActivity::class.java) {
                    putString(PRIVATE_VAULT_PASSWORD, privateVaultPassword)
                    putBoolean(IS_SHOW_SECURITY_QA, true)
                }
                finish()
            }

            btnSetSubmit.setOnSafeClickListener {
                if (!isForgotPassword) {
                    appPreferences.apply {
                        secretQuestion = spinnerSecurityQA.selectedItem.toString()
                        secretAnswer = evAnswer.text.toString()
                        isPinSet = true
                    }
                    launchActivity(PrivateChatActivity::class.java)
                    finish()
                } else {
                    val secretQuestion = spinnerSecurityQA.selectedItem.toString()
                    val secretAnswer = evAnswer.text.toString()
                    if (appPreferences.secretQuestion == secretQuestion && appPreferences.secretAnswer == secretAnswer) {
                        launchActivity(EnterPinActivity::class.java) {
                            putBoolean(IS_FORGOT_PASSWORD, true)
                        }
                        finish()
                    } else {
                        wrongQaAndAnswer.isVisible = true
                    }
                }
            }
        }
    }

    private fun initData() {
        privateVaultPassword = intent?.getStringExtra(PRIVATE_VAULT_PASSWORD) ?: ""
        isShowSecurityQA = intent?.getBooleanExtra(IS_SHOW_SECURITY_QA, false) == true
        isForgotPassword = intent?.getBooleanExtra(IS_FORGOT_PASSWORD, false) == true
        binding.apply {
            if (!isShowSecurityQA) {
                nsvConfirmPassword.isVisible = true
                nsvSecurityQA.isVisible = false
                if (privateVaultPassword != "") {
                    tvPasswordFirst.text = privateVaultPassword[0].toString()
                    tvPasswordSecond.text = privateVaultPassword[1].toString()
                    tvPasswordThird.text = privateVaultPassword[2].toString()
                    tvPasswordForth.text = privateVaultPassword[3].toString()
                }
            } else {
                nsvConfirmPassword.isVisible = false
                nsvSecurityQA.isVisible = true
                verifyYourIdentity.isVisible = isForgotPassword
            }
        }
    }

    private fun initSpinner() {
        if (isShowSecurityQA) {
            val adapter = ArrayAdapter.createFromResource(
                this,
                R.array.security_question, R.layout.spinner_item
            )
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerSecurityQA.setAdapter(adapter)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}