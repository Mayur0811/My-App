package com.messages.extentions

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.res.getColorOrThrow
import androidx.recyclerview.widget.ItemTouchHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.klinker.android.logger.Log
import com.messages.BuildConfig
import com.messages.R
import com.messages.application.MessageApplication
import com.messages.common.backgroundScope
import com.messages.data.pref.AppPreferences
import com.messages.ui.home.adapter.MainFilterAdapter
import com.messages.ui.swipe_action.SwipeAction
import com.messages.utils.AppLogger
import com.messages.utils.DARK_THEME
import com.messages.utils.FILE_SIZE_NONE
import com.messages.utils.HOME_ACTIVITY
import com.messages.utils.LIGHT_THEME
import com.messages.utils.PRIVATE_ACTIVITY
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


fun Context.getBackupDir(): String {
    val dir = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    } else {
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    }
    val fileDir = File(dir, "Messages")
    if (!fileDir.exists()) {
        fileDir.mkdirs()
    }
    return fileDir.absolutePath
}

fun Context.getBackupFile(name: String): String {
    val backupFile = File(getBackupDir(), name)
    if (backupFile.exists()) {
        backupFile.delete()
    }
    backupFile.createNewFile()
    return backupFile.absolutePath
}

fun Context.getThreadPhoneNumbers(recipientIds: List<Long>): ArrayList<String> {
    val numbers = ArrayList<String>()
    recipientIds.forEach {
        numbers.add(getPhoneNumberFromAddressId(it))
    }
    return numbers
}

fun Context.getPhoneNumberFromAddressId(canonicalAddressId: Long): String {
    val uri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_URI, "canonical-addresses")
    val projection = arrayOf(
        Telephony.Mms.Addr.ADDRESS
    )

    val selection = "${Telephony.Mms._ID} = ?"
    val selectionArgs = arrayOf(canonicalAddressId.toString())
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS))
            }
        }
    } catch (e: Exception) {
        AppLogger.e(message = "${e.message}")
    }
    return ""
}

fun Context.resolveThemeColor(attributeId: Int, default: Int = 0): Int {
    val outValue = TypedValue()
    val wasResolved = theme.resolveAttribute(attributeId, outValue, true)

    return if (wasResolved) getColorForId(outValue.resourceId) else default
}

fun <T> Context.launchActivity(it: Class<T>, extras: Bundle.() -> Unit = {}) {
    val intent = Intent(this, it)
    intent.putExtras(Bundle().apply(extras))
    startActivity(intent)
}

fun <T> Context.launchActivity(it: Class<T>, flags: Int, extras: Bundle.() -> Unit = {}) {
    val intent = Intent(this, it)
    intent.setFlags(flags)
    intent.putExtras(Bundle().apply(extras))
    startActivity(intent)
}

fun Context.getStringValue(id: Int): String {
    return resources.getString(id)
}

fun Context.getIntValue(id: Int): Int {
    return resources.getInteger(id)
}

fun Context.getColorForId(id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(this, msg, length).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, msg, length).show()
            }
        }
    } catch (e: Exception) {
        AppLogger.e("Exception", e.toString())
    }
}

fun Context.getPathFromUri(uri: Uri?, onGetPath: (String?) -> Unit) {
    if (uri == null) {
        return
    }
    if (DocumentsContract.isDocumentUri(this, uri)) {
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                onGetPath.invoke(
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                )
            }
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            if (id.startsWith("raw:")) {
                onGetPath.invoke(id.removePrefix("raw:"))
            }
            val contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), id.toLong()
            )
            onGetPath.invoke(getDataColumn(contentUri, null, null))
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            val contentUri = when (type) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            if (contentUri != null) {
                onGetPath.invoke(getDataColumn(contentUri, selection, selectionArgs))
            }
        }
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        if (isGooglePhotosUri(uri)) onGetPath.invoke(uri.lastPathSegment)
        onGetPath.invoke(getDataColumn(uri, null, null))
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        onGetPath.invoke(uri.path)
    }


}

fun Context.getFilenameFromUri(uri: Uri): String? {
    var filename: String? = null
    if (uri.scheme == "content") {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use { mCursor ->
            val nameIndex = mCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                mCursor.moveToFirst()
                filename = mCursor.getString(nameIndex)
            }
        }
    } else if (uri.scheme == "file") {
        filename = File(uri.path.toString()).getName()
    }
    return filename
}

fun Context.getDataColumn(
    uri: Uri,
    selection: String? = null,
    selectionArgs: Array<String>? = null
): String? {
    var cursor: Cursor? = null
    val column = MediaStore.Images.Media.DATA
    val projection = arrayOf(column)

    try {
        cursor = contentResolver.query(
            uri, projection, selection, selectionArgs, null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(index)
        }
    } catch (e: Exception) {
        AppLogger.d("1234", "getDataColumn ->error ${e.message}")
    } finally {
        cursor?.close()
    }
    return null
}


/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is Google Photos.
 */
fun isGooglePhotosUri(uri: Uri): Boolean {
    return "com.google.android.apps.photos.content" == uri.authority
}

fun Context.getFileSizeFromUri(uri: Uri?): Long {
    if (uri == null) {
        return -1L
    }
    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(uri, "r")
    } catch (e: FileNotFoundException) {
        null
    }

    val length = assetFileDescriptor?.use { it.length } ?: -1L
    if (length != -1L) {
        return length
    }

    if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex == -1) {
                    return@use -1L
                }
                cursor.moveToFirst()
                return try {
                    cursor.getLong(sizeIndex)
                } catch (_: Throwable) {
                    -1L
                }
            } ?: -1L
    } else {
        return -1L
    }
}

fun Context.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.simple_commons), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    toast(getStringValue(R.string.copied_to_clipboard))
}

fun Context.shareTextIntent(text: String) {
    backgroundScope.launch {
        Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)

            try {
                startActivity(Intent.createChooser(this, getString(R.string.share_via)))
            } catch (e: ActivityNotFoundException) {
                toast(getStringValue(R.string.no_app_found))
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(getStringValue(R.string.maximum_share_reached))
                } else {
                    toast(msg = e.message.toString())
                }
            } catch (e: Exception) {
                toast(msg = e.message.toString())
            }
        }
    }
}

fun Context.dialNumber(phoneNumber: String, callback: (() -> Unit)? = null) {
    Intent(Intent.ACTION_DIAL).apply {
        data = Uri.fromParts("tel", phoneNumber, null)

        try {
            startActivity(this)
            callback?.invoke()
        } catch (e: ActivityNotFoundException) {
            toast(getStringValue(R.string.no_app_found))
        } catch (e: Exception) {
            toast(msg = e.message.toString())
        }
    }
}

fun Context.getColoredDrawableWithColor(drawableId: Int, color: Int, alpha: Int = 255): Drawable? {
    val drawable = AppCompatResources.getDrawable(this, drawableId)
    drawable?.mutate()?.setTint(color)
    drawable?.mutate()?.alpha = alpha
    return drawable
}


fun Context.getColorList(arrayId: Int): ArrayList<Int> {
    val typedArray = resources.obtainTypedArray(arrayId)
    val colorList = ArrayList<Int>()
    for (i in 0 until typedArray.length()) {
        colorList.add(typedArray.getColorOrThrow(i))
    }
    typedArray.recycle()
    return colorList
}

fun Context.getDrawableAsString(id: Int): SpannableString {
    val drawable = ContextCompat.getDrawable(this, id)
    return drawable?.let {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        val spannableString = SpannableString("\uFFFC")
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        spannableString
    } ?: SpannableString("")
}

fun Context.applyTheme() {
    if (MessageApplication.isApplyingTheme) {
        val theme = AppPreferences(this).appTheme
        when (theme) {
            LIGHT_THEME -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            DARK_THEME -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}

fun Context.getFontSizeName(fontSize: Int): String {
    return when (fontSize) {
        AppPreferences.TEXT_SIZE_SMALL -> getStringValue(R.string.small)
        AppPreferences.TEXT_SIZE_NORMAL -> getStringValue(R.string.normal)
        AppPreferences.TEXT_SIZE_LARGE -> getStringValue(R.string.large)
        AppPreferences.TEXT_SIZE_LARGER -> getStringValue(R.string.larger)
        else -> getStringValue(R.string.normal)
    }
}

fun Context.rateApp() {
    try {
        startActivity(
            Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
            )
        )
    } catch (e: ActivityNotFoundException) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
            )
        )
    }
}

fun Context.openPlayStoreReview() {
    try {
        // Open in Play Store app
        val uri = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback to browser if Play Store is not installed
        val uri =
            Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}

fun Context.shareApp() {
    try {
        val intent3 = Intent("android.intent.action.SEND")
        intent3.setType("text/plain")
        intent3.putExtra(Intent.EXTRA_SUBJECT, getStringValue(R.string.app_name))
        intent3.putExtra(
            Intent.EXTRA_TEXT, """
                ${getStringValue(R.string.app_name)}
                https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
                """.trimIndent()
        )
        startActivity(Intent.createChooser(intent3, "Share App using"))
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

fun Context.openCustomTab(url: String?) {
    try {
        val builder = CustomTabsIntent.Builder()

        val defaultColors =
            CustomTabColorSchemeParams.Builder().setToolbarColor(getColorForId(R.color.app_color))
                .build()

        builder.setDefaultColorSchemeParams(defaultColors)

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    } catch (e: Exception) {
        openUrlInChrome(url)
    } catch (_: ActivityNotFoundException) {
    }
}

fun Context.openUrlInChrome(url: String?) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun Context.getActionDrawable(action: Int): Drawable? {
    return when (getStringValue(SwipeAction.fromPosition(action).stringResId)) {
        getStringValue(R.string.action_archive) -> ContextCompat.getDrawable(
            this, R.drawable.ic_archive_action
        )

        getStringValue(R.string.action_delete) -> ContextCompat.getDrawable(
            this, R.drawable.ic_delete
        )

        getStringValue(R.string.action_call) -> ContextCompat.getDrawable(
            this, R.drawable.ic_call
        )

        getStringValue(R.string.action_mark_read) -> ContextCompat.getDrawable(
            this, R.drawable.ic_read
        )

        getStringValue(R.string.action_mark_unread) -> ContextCompat.getDrawable(
            this, R.drawable.ic_unread
        )

        getStringValue(R.string.action_add_private) -> ContextCompat.getDrawable(
            this, R.drawable.ic_lock
        )

        getStringValue(R.string.action_remove_private) -> ContextCompat.getDrawable(
            this, R.drawable.ic_unlock
        )

        else -> null
    }
}

fun Context.getActionDrawableId(action: Int): Int {
    return when (getStringValue(SwipeAction.fromPosition(action).stringResId)) {
        getStringValue(R.string.action_archive) -> R.drawable.ic_archive_action

        getStringValue(R.string.action_delete) -> R.drawable.ic_delete

        getStringValue(R.string.action_call) -> R.drawable.ic_call

        getStringValue(R.string.action_mark_read) -> R.drawable.ic_read

        getStringValue(R.string.action_mark_unread) -> R.drawable.ic_unread

        getStringValue(R.string.action_add_private) -> R.drawable.ic_lock

        getStringValue(R.string.action_remove_private) -> R.drawable.ic_unlock

        else -> 0
    }
}

fun Context.getActionBG(action: Int): Int {
    return when (getStringValue(SwipeAction.fromPosition(action).stringResId)) {
        getStringValue(R.string.action_delete) -> getColorForId(R.color.app_error)
        else -> getColorForId(R.color.app_color)
    }
}

fun Context.getSwipeDirection(appPreferences: AppPreferences, activityInt: Int): Int {
    val rightAction = getValidAction(appPreferences.rightSwipeAction, activityInt)
    val leftAction = getValidAction(appPreferences.leftSwipeAction, activityInt)
    return if (leftAction != getStringValue(R.string.action_none) && rightAction != getStringValue(R.string.action_none)) {
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    } else if (leftAction == getStringValue(R.string.action_none) && rightAction != getStringValue(R.string.action_none)) {
        ItemTouchHelper.RIGHT
    } else if (leftAction != getStringValue(R.string.action_none) && rightAction == getStringValue(R.string.action_none)) {
        ItemTouchHelper.LEFT
    } else {
        0
    }

}

fun Context.getValidAction(action: Int, activityInt: Int): String {
    return when (activityInt) {
        HOME_ACTIVITY -> {
            if (action == SwipeAction.REMOVE_PRIVATE.ordinal  /*getStringValue(R.string.action_remove_private)*/) {
                getStringValue(R.string.action_none)
            } else {
                getStringValue(SwipeAction.fromPosition(action).stringResId)
            }
        }

        PRIVATE_ACTIVITY -> {
            if (action == SwipeAction.ADD_PRIVATE.ordinal /*getStringValue(R.string.action_add_private)*/) {
                getStringValue(R.string.action_none)
            } else {
                getStringValue(SwipeAction.fromPosition(action).stringResId)
            }
        }

        else -> getStringValue(R.string.action_none)
    }
}


fun Context.getContactLetterIcon(name: String?): Bitmap {
    val letter =
        name?.normalizeString()?.toCharArray()?.getOrNull(0)?.toString()?.uppercase() ?: "A"
    val size = resources.getDimension(R.dimen._48).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val view = TextView(this)
    view.layout(0, 0, size, size)

    val circlePaint = Paint().apply {
        color = getColorForId(R.color.app_bg)
        isAntiAlias = true
    }

    val wantedTextSize = size / 2f
    val textPaint = Paint().apply {
        color = getColorForId(R.color.app_color)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = wantedTextSize
        style = Paint.Style.FILL
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

    val xPos = canvas.width / 2f
    val yPos = canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
    letter.let { canvas.drawText(it, xPos, yPos, textPaint) }
    view.draw(canvas)
    return bitmap

}

fun Context.getNotificationBitmap(photoUri: String): Bitmap? {
    val size = resources.getDimension(R.dimen._72).toInt()
    if (photoUri.isEmpty()) {
        return null
    }

    val options = RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop()

    return try {
        Glide.with(this).asBitmap().load(photoUri).apply(options)
            .apply(RequestOptions.circleCropTransform()).submit(size, size).get()
    } catch (e: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
fun Context.getSimSlotForSubscription(subscriptionId: Int): Int {
    val subscriptionManager =
        getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager?
    if (subscriptionManager != null) {
        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
        if (subscriptionInfoList != null) {
            for (info in subscriptionInfoList) {
                if (info.subscriptionId == subscriptionId) {
                    return info.simSlotIndex + 1 // Convert 0-based index to 1-based (SIM1 or SIM2)
                }
            }
        }
    }
    return -1 // Return -1 if not found
}


fun Context.copyImageToDir(sourceFilePath: String, onSave: (String?) -> Unit) {
    val sourceFile = File(sourceFilePath)
    val destDir = File(filesDir.absolutePath, "wallpaper")
    if (!destDir.exists()) {
        destDir.mkdirs()
    }
    val destFile = File(destDir, "chat_wallpaper.png")

    if (destFile.exists()) {
        destFile.delete()
    }

    try {
        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(destFile).use { fos ->
                val buffer = ByteArray(1024)
                var length: Int
                while ((fis.read(buffer).also { length = it }) > 0) {
                    fos.write(buffer, 0, length)
                }
                onSave.invoke(destFile.absolutePath)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        onSave.invoke(null)
    }
}

fun Context.getByteForMmsLimit(size: String): Long {
    return when (size) {
        getStringValue(R.string.kb_100) -> 100 * 1024
        getStringValue(R.string.kb_200) -> 200 * 1024
        getStringValue(R.string.kb_300) -> 300 * 1024
        getStringValue(R.string.kb_600) -> 600 * 1024
        getStringValue(R.string.mb_1) -> 1 * 1024 * 1024
        getStringValue(R.string.mb_2) -> 2 * 1024 * 1024
        else -> FILE_SIZE_NONE
    }
}

fun Context.getFilterName(filter: Int): String {
    return when (filter) {
        MainFilterAdapter.ALL -> getStringValue(R.string.all)
        MainFilterAdapter.PERSONAL -> getStringValue(R.string.personal)
        MainFilterAdapter.OTP -> getStringValue(R.string.otp_s)
        MainFilterAdapter.TRANSACTIONS -> getStringValue(R.string.transactions)
        MainFilterAdapter.OFFERS -> getStringValue(R.string.offers)
        else -> getStringValue(R.string.all)
    }
}


