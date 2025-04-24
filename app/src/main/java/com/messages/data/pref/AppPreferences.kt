package com.messages.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.messages.R
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    companion object {
        const val TEXT_SIZE_SMALL = 0
        const val TEXT_SIZE_NORMAL = 1
        const val TEXT_SIZE_LARGE = 2
        const val TEXT_SIZE_LARGER = 3
        const val TEXT_SIZE_SUPER = 4
    }

    private val messagePreferences = "MessagePreferences"

    private val isShowExitDialogKey = "isShowExitDialog"

    private val textSizeKey = "textSize"
    private val systemFontKey = "systemFont"
    private val themeKey = "theme"
    private val appThemeKey = "appTheme"
    private val appLanguageKey = "appLanguage"
    private val isShowOnBoardingKey = "isShowOnBoarding"
    private val pinedConversationKey = "pinedConversation"
    private val chatBubbleBgReceivedKey = "chatBubbleBgReceived"
    private val chatBubbleBgSendKey = "chatBubbleBgSend"
    private val chatWallpaperBgKey = "chatWallpaperBg"
    private val isShowDesignOnWallpaperKey = "isShowDesignOnWallpaper"
    private val isShowCustomWallpaperKey = "isShowCustomWallpaper"
    private val customWallpaperPathKey = "customWallpaperPath"
    private val isPinSetKey = "isPinSet"
    private val privateVaultPasswordKey = "privateVaultPassword"
    private val secretQuestionKey = "secretQuestion"
    private val secretAnswerKey = "secretAnswer"
    private val isPrivateChatNotifyKey = "isPrivateChatNotify"
    private val lockScreenNotifyKey = "lockScreenNotify"
    private val mmsLimitKey = "mmsLimit"
    private val isShowCallScreenKey = "isShowCallScreen"
    private val deliveryReportsKey = "deliveryReports"
    private val use24HrKey = "use24Hr"
    private val showCharCountKey = "showCharCount"
    private val removeAccentsKey = "removeAccents"
    private val rightSwipeActionKey = "rightSwipeAction"
    private val leftSwipeActionKey = "leftSwipeAction"

    private val isRecipientSyncKey = "isRecipientSync"
    private val lastExportDateKey = "lastExportDate"

    private val appDayKey = "appDay"
    private val appSessionKey = "appSession"
    private val lastUseDateKey = "lastUseDate"

    private val privacyPolicyKey = "privacyPolicy"

    private val prefs: SharedPreferences =
        context.getSharedPreferences(messagePreferences, Context.MODE_PRIVATE)

    val isShowExitDialog by BooleanPreferenceDelegate(isShowExitDialogKey, true)

    var appDay: Int by IntPreferenceDelegate(appDayKey, 0)
    var appSession: Int by IntPreferenceDelegate(appSessionKey, 1)
    var lastUseDate: Int by IntPreferenceDelegate(lastUseDateKey, 0)

    var appTheme: Int by IntPreferenceDelegate(appThemeKey, 0)
    var textSize: Int by IntPreferenceDelegate(textSizeKey, 1)
    var systemFont: Boolean by BooleanPreferenceDelegate(systemFontKey)
    var appLanguage: String by StringPreferenceDelegate(appLanguageKey, "en")
    var isShowOnBoarding: Boolean by BooleanPreferenceDelegate(isShowOnBoardingKey, false)
    var chatBubbleBgReceived: Int by IntPreferenceDelegate(chatBubbleBgReceivedKey, 0)
    var chatBubbleBgSend: Int by IntPreferenceDelegate(chatBubbleBgSendKey, 0)
    var chatWallpaperBg: Int by IntPreferenceDelegate(chatWallpaperBgKey, 0)
    var isShowDesignOnWallpaper: Boolean by BooleanPreferenceDelegate(
        isShowDesignOnWallpaperKey,
        false
    )
    var isShowCustomWallpaper: Boolean by BooleanPreferenceDelegate(isShowCustomWallpaperKey, false)
    var isPinSet: Boolean by BooleanPreferenceDelegate(isPinSetKey, false)
    var privateVaultPassword: String by StringPreferenceDelegate(privateVaultPasswordKey)
    var secretQuestion: String by StringPreferenceDelegate(secretQuestionKey)
    var secretAnswer: String by StringPreferenceDelegate(secretAnswerKey)
    var isPrivateChatNotify: Boolean by BooleanPreferenceDelegate(isPrivateChatNotifyKey, true)

    var customWallpaperPath by StringPreferenceDelegate(customWallpaperPathKey, "")

    var lockScreenNotify: Int by IntPreferenceDelegate(lockScreenNotifyKey, 0)
    var mmsLimit: String by StringPreferenceDelegate(mmsLimitKey, "300KB")
    var isShowCallScreen: Boolean by BooleanPreferenceDelegate(isShowCallScreenKey, true)
    var deliveryReports: Boolean by BooleanPreferenceDelegate(deliveryReportsKey, false)
    var use24Hr: Boolean by BooleanPreferenceDelegate(use24HrKey, false)
    var showCharCount: Boolean by BooleanPreferenceDelegate(showCharCountKey, false)
    var removeAccents: Boolean by BooleanPreferenceDelegate(removeAccentsKey, false)

    var lastExportDate: Long by LongPreferenceDelegate(lastExportDateKey)

    var rightSwipeAction by IntPreferenceDelegate(rightSwipeActionKey, 0)
    var leftSwipeAction by IntPreferenceDelegate(leftSwipeActionKey, 0)

    var isRecipientSync: Boolean by BooleanPreferenceDelegate(isRecipientSyncKey, false)

    var theme: Int by IntPreferenceDelegate(themeKey, 0xFF0097A7.toInt())

    var pinedConversation: String by StringPreferenceDelegate(pinedConversationKey, "[]")

    var privacyPolicy by StringPreferenceDelegate(privacyPolicyKey,"https://")


    private inner class StringPreferenceDelegate(
        private val key: String, private val defaultValue: String = ""
    ) : ReadWriteProperty<Any, String> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            prefs.edit {
                putString(key, value)
            }
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return prefs.getString(key, defaultValue) ?: defaultValue
        }
    }


    private inner class IntPreferenceDelegate(
        private val key: String,
        private val defaultValue: Int = 0,
    ) : ReadWriteProperty<Any, Int> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            prefs.edit {
                putInt(key, value)
            }
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): Int {
            return prefs.getInt(key, defaultValue)
        }
    }

    private inner class LongPreferenceDelegate(
        private val key: String,
        private val defaultValue: Long = 0,
    ) : ReadWriteProperty<Any, Long> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
            prefs.edit {
                putLong(key, value)
            }
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): Long {
            return prefs.getLong(key, defaultValue)
        }
    }

    private inner class FloatPreferenceDelegate(
        private val key: String,
        private val defaultValue: Float = 0f,
    ) : ReadWriteProperty<Any, Float> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) {
            prefs.edit {
                putFloat(key, value)
            }
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): Float {
            return prefs.getFloat(key, defaultValue)
        }
    }

    private inner class BooleanPreferenceDelegate(
        private val key: String,
        private val defaultValue: Boolean = false,
    ) : ReadWriteProperty<Any, Boolean> {
        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            prefs.edit {
                putBoolean(key, value)
            }
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return prefs.getBoolean(key, defaultValue)
        }
    }

    private inner class KeyExistPreferenceDelegate(
        private val keyProvider: () -> String,
    ) : ReadOnlyProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return prefs.contains(keyProvider())
        }
    }

}

