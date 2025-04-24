package com.messages.ui.setting.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.messages.R
import com.messages.common.mainScope
import com.messages.data.models.SelectionModel
import com.messages.databinding.ActivitySettingBinding
import com.messages.extentions.getFontSizeName
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.openCustomTab
import com.messages.extentions.openPlayStoreReview
import com.messages.extentions.rateApp
import com.messages.extentions.shareApp
import com.messages.ui.appearance.ui.AppearanceActivity
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat_wallpaper.ChatWallpaperActivity
import com.messages.ui.common_dialog.CustomSelectionDialog
import com.messages.ui.language.ui.LanguageActivity
import com.messages.ui.swipe_action.SwipeActionActivity
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingActivity : BaseActivity() {

    private val binding by lazy { ActivitySettingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initData()
        initAction()
    }

    private fun initToolbar() {
        binding.settingToolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.setting)
            ivToolbarBack.setOnSafeClickListener { finish() }
        }
    }

    private fun initData() {
        binding.apply {
            settingFontSize.getBinding().customViewSubtitle.text =
                getFontSizeName(appPreferences.textSize)

            settingUseSystemFont.getBinding().switchToggle.apply {
                mainScope.launch {
                    delay(30)
                    isChecked = appPreferences.systemFont
                }
            }

            settingLanguage.getBinding().apply {
                val languageCode = resources.getStringArray(R.array.language_code)
                val languageName = resources.getStringArray(R.array.language_name)
                customViewSubtitle.text =
                    languageName[languageCode.indexOf(appPreferences.appLanguage)]
            }

            settingLockScreenNotify.getBinding().customViewSubtitle.text =
                resources.getStringArray(R.array.lock_screen_notify)[appPreferences.lockScreenNotify]

            settingResizeSendMMS.getBinding().customViewSubtitle.text = appPreferences.mmsLimit

            settingPostCallScreen.getBinding().switchToggle.isChecked = appPreferences.isShowCallScreen

            settingDeliveryReports.getBinding().switchToggle.isChecked = appPreferences.deliveryReports

            settingUse24Hr.getBinding().switchToggle.isChecked = appPreferences.use24Hr

            settingShowCount.getBinding().switchToggle.isChecked = appPreferences.showCharCount

            settingRemoveAccents.getBinding().switchToggle.isChecked = appPreferences.removeAccents

        }
    }

    private fun initAction() {
        binding.apply {
            settingFontSize.setOnSafeClickListener {
                val items = arrayListOf<SelectionModel>()
                val itemList = resources.getStringArray(R.array.font_size)
                for (i in itemList.indices) {
                    items.add(SelectionModel(id = i, title = itemList[i]))
                }
                CustomSelectionDialog(
                    activity = this@SettingActivity,
                    isShowTitle = true,
                    title = getStringValue(R.string.font_size),
                    items = items,
                    checkedItemId = appPreferences.textSize
                ) {
                    appPreferences.textSize = it as Int
                    recreateActivity()
                }
            }

            settingUseSystemFont.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.systemFont = getBinding().switchToggle.isChecked
                    recreateActivity()
                }
            }

            settingLanguage.setOnSafeClickListener {
                launchActivity(LanguageActivity::class.java)
            }

            settingLockScreenNotify.setOnSafeClickListener {
                val items = arrayListOf<SelectionModel>()
                val itemList = resources.getStringArray(R.array.lock_screen_notify)
                for (i in itemList.indices) {
                    items.add(SelectionModel(id = i, title = itemList[i]))
                }
                CustomSelectionDialog(
                    activity = this@SettingActivity,
                    isShowTitle = true,
                    title = getStringValue(R.string.lock_screen_notification),
                    items = items,
                    checkedItemId = appPreferences.lockScreenNotify
                ) {
                    appPreferences.lockScreenNotify = it as Int
                    settingLockScreenNotify.getBinding().customViewSubtitle.text = itemList[it]
                }
            }

            settingAppearance.setOnSafeClickListener {
                launchActivity(AppearanceActivity::class.java)
            }

            settingChatWallpaper.setOnSafeClickListener {
                launchActivity(ChatWallpaperActivity::class.java)
            }

            settingResizeSendMMS.setOnSafeClickListener {
                val itemList = resources.getStringArray(R.array.mms_size)
                openSelectionDialog(
                    itemList = itemList,
                    title = getStringValue(R.string.resize_sent_mms_image),
                    checkedItemId = itemList.indexOf(appPreferences.mmsLimit)
                ) {
                    appPreferences.mmsLimit = itemList[it]
                    settingResizeSendMMS.getBinding().customViewSubtitle.text = itemList[it]
                }
            }

            settingCustomizeNotify.setOnSafeClickListener {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(this)
                }
            }

            settingPostCallScreen.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.isShowCallScreen = getBinding().switchToggle.isChecked
                }
            }

            settingDeliveryReports.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.deliveryReports = getBinding().switchToggle.isChecked
                }
            }

            settingUse24Hr.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.use24Hr = getBinding().switchToggle.isChecked
                }
            }

            settingShowCount.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.showCharCount = getBinding().switchToggle.isChecked
                }
            }

            settingRemoveAccents.apply {
                setOnSafeClickListener {
                    getBinding().switchToggle.toggle()
                    appPreferences.removeAccents = getBinding().switchToggle.isChecked
                }
            }

            settingRateUs.setOnSafeClickListener {
                rateApp()
            }

            settingShareApp.setOnSafeClickListener {
                shareApp()
            }

            settingSwipeAction.setOnSafeClickListener {
                launchActivity(SwipeActionActivity::class.java)
            }

            settingFeedback.setOnSafeClickListener {
                openPlayStoreReview()
            }

            settingPrivacyPolicy.setOnSafeClickListener {
                openCustomTab(appPreferences.privacyPolicy)
            }

        }
    }

    private fun openSelectionDialog(
        itemList: Array<String>,
        title: String = "",
        checkedItemId: Int,
        callback: (Int) -> Unit
    ) {
        val items = arrayListOf<SelectionModel>()
        for (i in itemList.indices) {
            items.add(SelectionModel(id = i, title = itemList[i]))
        }
        CustomSelectionDialog(
            activity = this@SettingActivity,
            isShowTitle = title.isNotEmpty(),
            title = title,
            items = items,
            checkedItemId = checkedItemId
        ) {
            callback.invoke(it as Int)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}