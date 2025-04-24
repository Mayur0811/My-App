package com.messages.ui.call_end

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.messages.R
import com.messages.databinding.ActivityCallEndBinding
import com.messages.databinding.ItemCallMenuBinding
import com.messages.extentions.formatMessageTime
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.loadImageDrawable
import com.messages.extentions.openCustomTab
import com.messages.extentions.toast
import com.messages.ui.base.BaseActivity
import com.messages.ui.call_end.adapter.InstantMsgAdapter
import com.messages.ui.home.ui.HomeActivity
import com.messages.ui.new_chat.ui.ContactActivity
import com.messages.utils.AppLogger
import com.messages.utils.IS_FROM_CALL_END
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallEndActivity : BaseActivity() {

    private val binding by lazy { ActivityCallEndBinding.inflate(layoutInflater) }

    private val callMenuOption = ArrayList<Pair<ConstraintLayout, ItemCallMenuBinding>>()
    private var selectedInstantMsg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateStatusBarColor(R.color.app_white)

        initToolbar()
        initViews()
        initActions()
    }

    private fun handleSendButtonVisibility(isSendBtnShow: Boolean) {
        binding.apply {
            btnSendMessage.isVisible = isSendBtnShow
        }
    }


    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            ivToolbarBack.loadImageDrawable(R.drawable.ic_close_big)
            tvToolbarTitle.text = getStringValue(R.string.app_name)
        }
    }

    private fun initViews() {
        binding.apply {
            callMenuOption.add(Pair(callOption.root, callOption))
            callMenuOption.add(Pair(callMessage.root, callMessage))
            callMenuOption.add(Pair(callMore.root, callMore))

            callOption.apply {
                callMenuBg.backgroundTintList = ColorStateList.valueOf(getColorForId(R.color.app_color))
                callMenuIcon.apply {
                    loadImageDrawable(R.drawable.ic_call_option)
                    imageTintList = ColorStateList.valueOf(getColorForId(R.color.app_white))
                }
                callMenuTitle.apply {
                    setTextAppearance(R.style.AppTextBlack)
                    text = getStringValue(R.string.option)
                    setTextColor(getColorForId(R.color.app_white))
                }

            }

            callMessage.callMenuIcon.loadImageDrawable(R.drawable.ic_call_message)
            callMessage.callMenuTitle.text = getStringValue(R.string.messages)
            callMore.callMenuIcon.loadImageDrawable(R.drawable.ic_manu_dot)
            callMore.callMenuTitle.text = getStringValue(R.string.more)


            val booleanExtra = intent.getBooleanExtra(IS_RINGING, false)
            val stringExtra = intent.getStringExtra(TIMER)

            tvCallTime.text = (System.currentTimeMillis() / 1000).formatMessageTime(appPreferences.use24Hr)
            tvCallTitle.text =
                intent.getStringExtra(NAME) ?: getStringValue(R.string.private_number)

            tvCallType.text =
                getStringValue(if (booleanExtra) R.string.incoming_call else R.string.outgoing_call)

            tvCallDuration.text = getString(R.string.duration_sec, stringExtra)

            val instantMsgList = arrayListOf(
                getStringValue(R.string.instant_msg_1),
                getStringValue(R.string.instant_msg_2),
                getStringValue(R.string.instant_msg_3),
                getStringValue(R.string.instant_msg_4),
                getStringValue(R.string.instant_msg_5)
            )

            rcvInstantMsg.adapter = InstantMsgAdapter(instantMsgList) { instantMsg ->
                selectedInstantMsg = instantMsg
                evMessage.setText(instantMsg)
            }

        }


    }

    private fun initActions() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                exitFromApp()
            }

            evMessage.addTextChangedListener {
                handleSendButtonVisibility(!evMessage.text.isNullOrEmpty())
            }

            ivEmoji.setOnSafeClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(evMessage, InputMethodManager.SHOW_IMPLICIT)
            }

            ivDetailsCall.setOnSafeClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:"))
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    startActivity(Intent.createChooser(intent, null))
                }
            }

            callOption.root.setOnSafeClickListener {
                clCallOption.isVisible = true
                clCallMessage.isVisible = false
                clCallMore.isVisible = false
                handleMenuOption(callOption.root)
            }

            callMessage.root.setOnSafeClickListener {
                clCallOption.isVisible = false
                clCallMessage.isVisible = true
                clCallMore.isVisible = false
                handleMenuOption(callMessage.root)
            }

            callMore.root.setOnSafeClickListener {
                clCallOption.isVisible = false
                clCallMessage.isVisible = false
                clCallMore.isVisible = true
                handleMenuOption(callMore.root)
            }

            callSendMsg.setOnSafeClickListener {
                launchActivity(ContactActivity::class.java) {
                    putBoolean(IS_FROM_CALL_END, true)
                }
                finish()
            }

            callViewMsg.setOnSafeClickListener {
                launchActivity(HomeActivity::class.java) {}
                finish()
            }

            callEditContact.setOnSafeClickListener {
                val withAppendedId =
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, 10L)
                try {
                    val intent = Intent("android.intent.action.EDIT")
                    intent.setData(withAppendedId)
                    intent.putExtra("finishActivityOnSaveCompleted", true)
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }

            callMessages.setOnSafeClickListener {
                launchActivity(HomeActivity::class.java) {}
                finish()
            }

            btnSendMessage.setOnSafeClickListener {
                if ((binding.evMessage.text?.length ?: 0) > 0) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
                    intent.putExtra("sms_body", binding.evMessage.text.toString())
                    startActivity(intent)
                }
            }

            callSendMail.setOnSafeClickListener {
                try {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("mailto:your_email"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, "your_subject")
                    intent.putExtra(Intent.EXTRA_TEXT, "your_text")
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                }
            }

            callCalender.setOnSafeClickListener {
                try {
                    val intent = Intent(Intent.ACTION_INSERT)
                    intent.setData(CalendarContract.Events.CONTENT_URI)
                    startActivity(intent)
                } catch (unused: java.lang.Exception) {
                    toast(getStringValue(R.string.no_app_found))
                }
            }

            callWeb.setOnSafeClickListener {
                openCustomTab("https://www.google.com/")
            }
        }
    }

    private fun handleMenuOption(clickedView: ConstraintLayout) {
        val selectedBgColor =
            ColorStateList.valueOf(getColorForId(R.color.app_color))
        val unselectedBgColor =
            ColorStateList.valueOf(getColorForId(R.color.app_light_gray))

        val selectedTextColor = getColorForId(R.color.app_white)
        val unselectedTextColor = getColorForId(R.color.app_gray)

        callMenuOption.forEach {
            if (it.first.id == clickedView.id) {
                it.second.apply {
                    callMenuTitle.setTextAppearance(R.style.AppTextBlack)
                    callMenuBg.backgroundTintList = selectedBgColor
                    callMenuTitle.setTextColor(selectedTextColor)
                    callMenuIcon.imageTintList = ColorStateList.valueOf(selectedTextColor)
                }
            } else {
                it.second.apply {
                    callMenuTitle.setTextAppearance(R.style.AppTextRegular)
                    callMenuBg.backgroundTintList = unselectedBgColor
                    callMenuTitle.setTextColor(unselectedTextColor)
                    callMenuIcon.imageTintList = ColorStateList.valueOf(unselectedTextColor)
                }
            }
        }
    }

    companion object {
        const val IS_RINGING = "isRinging"
        const val TIMER = "timer"
        const val NAME = "name"
    }
}