package com.messages.common.receivers

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.messages.R
import com.messages.common.CallEndUtils
import com.messages.common.CallEndUtils.CallEndCompanion
import com.messages.data.pref.AppPreferences
import com.messages.extentions.launchActivity
import com.messages.ui.call_end.CallEndActivity
import com.messages.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.jvm.internal.Intrinsics


@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val updateTimer: Runnable = object : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            val companion = CallEndUtils.Companion
            companion.setElapsedTime(SystemClock.uptimeMillis() - companion.getStartTime())
            val elapsedTime = companion.getElapsedTime() / 3600000
            val elapsedTime2 = (companion.getElapsedTime() / (60000L)) % 60
            val elapsedTime3 = (companion.getElapsedTime() / (1000L)) % 60
            val format = String.format(
                "%02d:%02d:%02d",
                *arrayOf<Any>(elapsedTime, elapsedTime2, elapsedTime3).copyOf(3)
            )
            companion.setFormattedTime(format)
            if (companion.getTimerRunning()) {
                val handler = companion.getHandler()
                handler?.postDelayed(this, 1000L)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra("incoming_number")
        AppLogger.w("1234", "phoneNumber -> $phoneNumber")
        if (appPreferences.isShowCallScreen) {
            if (Intrinsics.areEqual(intent.action, "android.intent.action.PHONE_STATE")) {
                val stringExtra = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val companion = CallEndUtils.Companion
                companion.setHandler(Handler(Looper.getMainLooper()))
                if (Intrinsics.areEqual(stringExtra, TelephonyManager.EXTRA_STATE_RINGING)) {
                    companion.setRinging(true)
                } else if (Intrinsics.areEqual(stringExtra, TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    companion.setTimerRunning(true)
                    companion.setStartTime(SystemClock.uptimeMillis())
                    companion.setElapsedTime(0L)
                    val handler = companion.getHandler()
                    companion.setFormattedTime("")
                    handler?.post(this.updateTimer)
                } else if (!Intrinsics.areEqual(stringExtra, TelephonyManager.EXTRA_STATE_IDLE)) {
                    AppLogger.d(message = "onReceive -> $stringExtra")
                } else {
                    companion.setPhoneNumber(phoneNumber ?: "")
                    companion.setTimerRunning(false)
                    val removeHandler = companion.getHandler()
                    removeHandler?.removeCallbacks(this.updateTimer)
                    AppLogger.d("1234", "call end -> ${Settings.canDrawOverlays(context)}")
                    if (Settings.canDrawOverlays(context)) {
                        try {
                            AppLogger.d("1234", "call 1")
                            context.launchActivity(
                                CallEndActivity::class.java,
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            ) {
                                putString(CallEndActivity.TIMER, companion.getFormattedTime())
                                putBoolean(CallEndActivity.IS_RINGING, companion.isRinging())
                            }
                        } catch (e: Exception) {
                            AppLogger.d("1234", "call 2 -> ${e.message}")
                        }
                    } else {
                        showOverlayPermissionNotification(context, companion)
                    }
                }
            }
        }
    }


    // Helper method to show a notification
    private fun showOverlayPermissionNotification(context: Context, companion: CallEndCompanion) {
        val channelId = "PostCallScreen"

        // Create notification channel
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return  // Exit if permission is not granted yet
            }
        }

        // Create an intent to open the system settings for overlay permissions
        val intent = Intent(context, CallEndActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(CallEndActivity.TIMER, companion.getFormattedTime())
        intent.putExtra(CallEndActivity.IS_RINGING, companion.isRinging())
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("This Call")
            .setContentText("Private number")
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1, notification)
    }

    // Create the notification channel (Android 8.0+)
    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "PostCallScreen",
            "PostCallScreen",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

