package com.messages.common

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.core.content.ContextCompat


class PermissionManager(private val context: Context) {

    fun isDefaultSMS(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) as RoleManager
            roleManager.isRoleAvailable(RoleManager.ROLE_SMS) && roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    fun hasNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasSMS(): Boolean {
        return hasReadSMS() && hasSendSMS()
    }

    fun hasReadSMS(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSendSMS(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadPhoneState(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadContact(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStorage(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStorageForDownloadDir(): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun hasPermission(permId: Int) = ContextCompat.checkSelfPermission(
        context,
        getPermissionString(permId)
    ) == PackageManager.PERMISSION_GRANTED

    fun getPermissionString(id: Int) = when (id) {
        PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
        PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        PERMISSION_CAMERA -> Manifest.permission.CAMERA
        PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
        PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
        PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
        PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
        PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
        PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
        PERMISSION_READ_CALL_LOG -> Manifest.permission.READ_CALL_LOG
        PERMISSION_WRITE_CALL_LOG -> Manifest.permission.WRITE_CALL_LOG
        PERMISSION_GET_ACCOUNTS -> Manifest.permission.GET_ACCOUNTS
        PERMISSION_READ_SMS -> Manifest.permission.READ_SMS
        PERMISSION_SEND_SMS -> Manifest.permission.SEND_SMS
        PERMISSION_READ_PHONE_STATE -> Manifest.permission.READ_PHONE_STATE
        else -> ""
    }

    companion object {
        // permissions
        const val PERMISSION_READ_STORAGE = 1
        const val PERMISSION_WRITE_STORAGE = 2
        const val PERMISSION_CAMERA = 3
        const val PERMISSION_RECORD_AUDIO = 4
        const val PERMISSION_READ_CONTACTS = 5
        const val PERMISSION_WRITE_CONTACTS = 6
        const val PERMISSION_READ_CALENDAR = 7
        const val PERMISSION_WRITE_CALENDAR = 8
        const val PERMISSION_CALL_PHONE = 9
        const val PERMISSION_READ_CALL_LOG = 10
        const val PERMISSION_WRITE_CALL_LOG = 11
        const val PERMISSION_GET_ACCOUNTS = 12
        const val PERMISSION_READ_SMS = 13
        const val PERMISSION_SEND_SMS = 14
        const val PERMISSION_READ_PHONE_STATE = 15
        const val PERMISSION_POST_NOTIFICATIONS = 16
        const val PERMISSION_ACCESS_FINE_LOCATION = 17
    }
}