package com.messages.ui.base

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.messages.BuildConfig
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.PermissionManager
import com.messages.common.backgroundScope
import com.messages.data.events.MessageEvent
import com.messages.data.pref.AppPreferences
import com.messages.extentions.applyTheme
import com.messages.extentions.getCurrentDate
import com.messages.extentions.getStringValue
import com.messages.extentions.launchActivity
import com.messages.extentions.toJson
import com.messages.extentions.toast
import com.messages.ui.permission.PermissionAskActivity
import com.messages.utils.AppLogger
import com.messages.utils.DARK_THEME
import com.messages.utils.IS_ASK_DEFAULT
import com.messages.utils.LIGHT_THEME
import com.messages.utils.PERMISSION_VIEW
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Locale
import javax.inject.Inject
import kotlin.system.exitProcess


abstract class BaseActivity : AppCompatActivity() {

    private var currentPermissionName = 0

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        applyLanguage()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateStatusBarColor(R.color.app_white)
        handleAppSession()
    }

    fun updateStatusBarColor(color: Int, isDark: Boolean = getAppIsInDark()) {
        window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, color)
            invertInsets(isDark, window)
        }
    }

    @Suppress("DEPRECATION")
    private fun invertInsets(darkTheme: Boolean, window: Window) {
        if (Build.VERSION.SDK_INT >= 30) {
            val statusBar = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            val navBar = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            if (!darkTheme) {
                window.insetsController?.setSystemBarsAppearance(statusBar, statusBar)
                window.insetsController?.setSystemBarsAppearance(navBar, navBar)
            } else {
                window.insetsController?.setSystemBarsAppearance(0, statusBar)
                window.insetsController?.setSystemBarsAppearance(0, navBar)
            }
        } else {
            val flags =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

            if (!darkTheme) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or flags
            } else {
                window.decorView.systemUiVisibility =
                    (window.decorView.systemUiVisibility.inv() or flags).inv()
            }
        }
    }


    private fun applyLanguage() {
        val config = resources.configuration
        val locale = Locale(AppPreferences(this).appLanguage)
        Locale.setDefault(locale)
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.values.contains(false)) {
            onGetPermission(currentPermissionName)
        } else {
            AppLogger.d(
                "1234",
                "Permission -> ${permissions.keys.toJson()} || ${permissions.values.toJson()}"
            )
            Toast.makeText(this, "Permission not found", Toast.LENGTH_LONG).show()
        }
    }

    fun handleSMSPermission(onHasPermission: () -> Unit) {
        if (permissionManager.hasReadSMS()) {
            onHasPermission.invoke()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS
                )
            )
            currentPermissionName = PermissionManager.PERMISSION_READ_SMS
        }

    }

    fun handlePhoneStatePermission(onHasPermission: () -> Unit) {
        if (permissionManager.hasReadPhoneState()) {
            onHasPermission.invoke()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE
                )
            )
            currentPermissionName = PermissionManager.PERMISSION_READ_PHONE_STATE
        }
    }

    fun handleContactPermission(onHasPermission: () -> Unit) {
        if (permissionManager.hasReadContact()) {
            onHasPermission.invoke()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS
                )
            )
            currentPermissionName = PermissionManager.PERMISSION_READ_CONTACTS
        }

    }


    open fun onGetPermission(permission: Int, isGranted: Boolean = true) {}

    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onSetDefaultSMS()
            } else {
                exitFromApp()
            }
        }


    fun requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    onSetDefaultSMS()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    requestRoleLauncher.launch(intent)
                }
            } else {
                toast(getStringValue(R.string.unknown_error_occurred))
                exitFromApp()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                onSetDefaultSMS()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                requestRoleLauncher.launch(intent)
            }
        }
    }

    open fun onSetDefaultSMS() {}

    fun exitFromApp() {
        moveTaskToBack(true)
        finishAffinity()
        exitProcess(0)
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            currentPermissionName = PermissionManager.PERMISSION_POST_NOTIFICATIONS
        }
    }

    // hide keyboard
    fun dismissKeyboard(view: View?) {
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // show keyboard
    fun showKeyboard(view: View?) {
        if (view == null) return
        view.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (view is EditText) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        else imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }

    //check keyboard is open
    fun isKeyboardOpen(onDone: (Boolean) -> Unit) {
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val isKeyboardVisible =
                decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) ?: false
            onDone.invoke(isKeyboardVisible)
        } else {
            onDone.invoke(false)
        }
    }

    fun handleStoragePermission(onHasPermission: () -> Unit) {
        if (permissionManager.hasStorage()) {
            onHasPermission.invoke()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
            currentPermissionName = PermissionManager.PERMISSION_READ_STORAGE
        }
    }

    fun handleLocationPermission(onHasPermission: () -> Unit) {
        if (permissionManager.hasLocation()) {
            onHasPermission.invoke()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            currentPermissionName = PermissionManager.PERMISSION_ACCESS_FINE_LOCATION
        }
    }

    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    public override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageEvent(event: MessageEvent) {
        onGetEvent(event)
    }

    open fun onGetEvent(event: MessageEvent) {}

    private fun getAppIsInDark(): Boolean {
        return when (appPreferences.appTheme) {
            LIGHT_THEME -> {
                false
            }

            DARK_THEME -> {
                true
            }

            else -> {
                val nightModeFlags: Int =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO -> false
                    else -> false
                }
            }
        }
    }

    fun askForExactAlarmPermissionIfNeeded(callback: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                callback()
            } else {
                val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null as String?)
                val intent = Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
                intent.setData(uri)
                startActivity(intent)
            }
        } else {
            callback()
        }
    }


    fun recreateActivity() {
        (application as MessageApplication).recreateAllActivities()
    }

    private fun handleAppSession() {
        backgroundScope.launch {
            var appSession = appPreferences.appSession

            if (appPreferences.lastUseDate == getCurrentDate()) {
                appPreferences.appSession = appSession + 1
            } else {
                appPreferences.apply {
                    appDay = getCurrentDate()
                    appSession = 1
                }
            }
        }
    }

    fun checkAndAskDefaultApp(){
        if (!permissionManager.isDefaultSMS()) {
            launchActivity(
                PermissionAskActivity::class.java,
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            ) {
                putInt(PERMISSION_VIEW, 0)
                putBoolean(IS_ASK_DEFAULT, true)
            }
        }
    }

    fun cancelNotification(notificationId:Int){
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    companion object {
        var isSyncInProgress = false
    }
}