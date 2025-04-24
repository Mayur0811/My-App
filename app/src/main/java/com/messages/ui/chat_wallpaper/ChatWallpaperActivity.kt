package com.messages.ui.chat_wallpaper

import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.databinding.ActivityChatWallpaperBinding
import com.messages.extentions.copyImageToDir
import com.messages.extentions.getColorList
import com.messages.extentions.getPathFromUri
import com.messages.extentions.getStringValue
import com.messages.extentions.toast
import com.messages.ui.appearance.adapter.ChatDataAdapter
import com.messages.ui.base.BaseActivity
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatWallpaperActivity : BaseActivity() {

    private val binding by lazy { ActivityChatWallpaperBinding.inflate(layoutInflater) }
    private var chatWallpaperData: ArrayList<Pair<Int, Int>> = arrayListOf()

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                getPathFromUri(uri) { path ->
                    if (path != null) {
                        copyImageToDir(path) { destPath ->
                            if (destPath != null) {
                                appPreferences.isShowCustomWallpaper = true
                                appPreferences.customWallpaperPath = destPath
                                appPreferences.chatWallpaperBg = 0
                                appPreferences.isShowDesignOnWallpaper = false
                                this@ChatWallpaperActivity.finish()
                            }else{
                                appPreferences.isShowCustomWallpaper = false
                                appPreferences.customWallpaperPath = ""
                                toast(getStringValue(R.string.please_select_another_image))
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initAdapter()
    }

    private fun initToolbar() {
        binding.toolbar.getBinding().apply {
            tvToolbarTitle.text = getStringValue(R.string.chat_wallpaper)
            ivToolbarBack.setOnSafeClickListener {
                finish()
            }
        }
    }

    private fun initAdapter() {
        backgroundScope.launch {
            val chatWallpaperBorder = getColorList(R.array.chat_wallpaper_border)
            val chatWallpaperBg = getColorList(R.array.chat_wallpaper_bg)
            for (i in chatWallpaperBg.indices) {
                chatWallpaperData.add(Pair(chatWallpaperBorder[i], chatWallpaperBg[i]))
            }
            mainScope.launch {
                binding.rcvChatWallpaper.apply {
                    hasFixedSize()
                    adapter = ChatDataAdapter(true).apply {
                        selectedItem = appPreferences.chatWallpaperBg
                        updateData(chatWallpaperData)
                        onClickItem = { color, pos ->
                            appPreferences.chatWallpaperBg = color
                            appPreferences.isShowDesignOnWallpaper = (pos != 0 && pos != 1)
                            appPreferences.isShowCustomWallpaper = false
                            appPreferences.customWallpaperPath = ""
                            this@ChatWallpaperActivity.finish()
                        }

                        pickImage = {
                            pickMedia.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.SingleMimeType(
                                        "image/*"
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

    }
}