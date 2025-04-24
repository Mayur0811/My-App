package com.messages.ui.appearance.ui

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.databinding.ActivityAppearanceBinding
import com.messages.extentions.copyImageToDir
import com.messages.extentions.getColorForId
import com.messages.extentions.getColorList
import com.messages.extentions.getPathFromUri
import com.messages.extentions.getStringValue
import com.messages.extentions.isSameBitmap
import com.messages.extentions.loadImage
import com.messages.extentions.loadImageDrawable
import com.messages.extentions.loadImageWithOutCache
import com.messages.extentions.setBackgroundTint
import com.messages.extentions.toast
import com.messages.ui.appearance.adapter.ChatDataAdapter
import com.messages.ui.base.BaseActivity
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppearanceActivity : BaseActivity() {

    private val binding by lazy { ActivityAppearanceBinding.inflate(layoutInflater) }
    private var toggleChatData = false
    private var chatColorData: ArrayList<Pair<Int, Int>> = arrayListOf()
    private var chatWallpaperData: ArrayList<Pair<Int, Int>> = arrayListOf()
    private var chatDataAdapter = ChatDataAdapter(false)
    private var chatWallpaperAdapter = ChatDataAdapter(true)
    private var isSendBubble = true
    private var selectedChatBubbleBgSend = 0
    private var selectedChatBubbleBgReceived = 0
    private var selectedChatWallpaperBg = 0
    private var selectedIsShowDesignOnWallpaper = false
    private var selectedTextSize = 0
    private var showCustomWallpaper = false
    private var customWallpaper = ""
    private var isDataLoad= false

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                getPathFromUri(uri) { path ->
                    if (path != null) {
                        showCustomWallpaper = true
                        selectedIsShowDesignOnWallpaper = false
                        selectedChatWallpaperBg = 0
                        customWallpaper = path
                        binding.ivDesign.apply {
                            loadImage(path)
                            isVisible = true
                        }
                    } else {
                        showCustomWallpaper = false
                        customWallpaper = ""
                        toast(getStringValue(R.string.please_select_another_image))
                    }
                    handleSaveButtonVisibility()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initToolbar()
        initData()
        initView()
        initAdapter()
        initToggleButton()
        initAction()
    }

    private fun handleSaveButtonVisibility() {
        mainScope.launch {
            binding.toolbar.getBinding().apply {
                val visible =
                    (appPreferences.chatBubbleBgSend != selectedChatBubbleBgSend)
                            || (appPreferences.chatBubbleBgReceived != selectedChatBubbleBgReceived)
                            || (appPreferences.chatWallpaperBg != selectedChatWallpaperBg)
                            || (appPreferences.isShowDesignOnWallpaper != selectedIsShowDesignOnWallpaper)
                            || (appPreferences.textSize != selectedTextSize)
                            || (appPreferences.isShowCustomWallpaper != showCustomWallpaper)
                            || (!isSameBitmap(appPreferences.customWallpaperPath, customWallpaper))
                btnToolbarAction.isVisible = visible
            }
        }
    }

    private fun initView() {
        binding.apply {
            clPreviewSend.setBackgroundTint(
                if (appPreferences.chatBubbleBgSend == 0) getColorForId(R.color.app_white) else appPreferences.chatBubbleBgSend
            )
            clPreviewReceive.setBackgroundTint(
                if (appPreferences.chatBubbleBgReceived == 0) getColorForId(R.color.app_white) else appPreferences.chatBubbleBgReceived
            )

            if (appPreferences.isShowDesignOnWallpaper) {
                ivDesign.isVisible = true
                clWallpaperPreview.setBackgroundTint(appPreferences.chatWallpaperBg)
                ivDesign.loadImageDrawable(R.drawable.ic_wallpaper_bg)
            } else if (appPreferences.isShowCustomWallpaper) {
                ivDesign.isVisible = true
                clWallpaperPreview.setBackgroundTint(getColorForId(R.color.app_bg))
                ivDesign.loadImageWithOutCache(appPreferences.customWallpaperPath)
            } else {
                ivDesign.isVisible = false
                clWallpaperPreview.setBackgroundTint(getColorForId(R.color.app_bg))
            }
            sliderFontSize.value = when (appPreferences.textSize) {
                0 -> 0f
                1 -> 33f
                2 -> 66f
                else -> 99f
            }
        }
    }

    private fun initAction() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener { finish() }

            toolbar.getBinding().btnToolbarAction.setOnSafeClickListener {
                if (showCustomWallpaper) {
                    copyImageToDir(customWallpaper) { destPath ->
                        appPreferences.apply {
                            chatBubbleBgSend = selectedChatBubbleBgSend
                            chatBubbleBgReceived = selectedChatBubbleBgReceived
                            chatWallpaperBg = selectedChatWallpaperBg
                            isShowDesignOnWallpaper = selectedIsShowDesignOnWallpaper
                            textSize = selectedTextSize
                            isShowCustomWallpaper = showCustomWallpaper
                            customWallpaperPath = destPath ?: ""
                        }
                        finish()
                    }
                } else {
                    appPreferences.apply {
                        chatBubbleBgSend = selectedChatBubbleBgSend
                        chatBubbleBgReceived = selectedChatBubbleBgReceived
                        chatWallpaperBg = selectedChatWallpaperBg
                        isShowDesignOnWallpaper = selectedIsShowDesignOnWallpaper
                        textSize = selectedTextSize
                        isShowCustomWallpaper = showCustomWallpaper
                        customWallpaperPath = customWallpaper
                    }
                    finish()
                }
            }

            ivSwipe.setOnSafeClickListener {
                if (isSendBubble) {
                    isSendBubble = false
                    chatDataAdapter.selectedItem = selectedChatBubbleBgReceived
                } else {
                    isSendBubble = true
                    chatDataAdapter.selectedItem = selectedChatBubbleBgSend
                }
                chatDataAdapter.notifyDataSetChanged()
            }

            clPreviewSend.setOnSafeClickListener {
                if (!isSendBubble) {
                    ivSwipe.performClick()
                }
            }
            clPreviewReceive.setOnSafeClickListener {
                if (isSendBubble) {
                    ivSwipe.performClick()
                }
            }

            chatDataAdapter.onClickItem = { color, _ ->
                if (isSendBubble) {
                    clPreviewSend.backgroundTintList = ColorStateList.valueOf(color)
                    selectedChatBubbleBgSend = color
                    handleSaveButtonVisibility()
                } else {
                    clPreviewReceive.backgroundTintList = ColorStateList.valueOf(color)
                    selectedChatBubbleBgReceived = color
                    handleSaveButtonVisibility()
                }

            }

            chatWallpaperAdapter.onClickItem = { color, pos ->
                clWallpaperPreview.backgroundTintList = ColorStateList.valueOf(color)
                selectedIsShowDesignOnWallpaper = (pos != 0 && pos != 1)
                ivDesign.loadImageDrawable(R.drawable.ic_wallpaper_bg)
                ivDesign.isVisible = (pos != 0 && pos != 1)
                selectedChatWallpaperBg = color
                handleSaveButtonVisibility()
            }

            chatWallpaperAdapter.pickImage = {
                pickMedia.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.SingleMimeType(
                            "image/*"
                        )
                    )
                )
            }

            sliderFontSize.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    selectedTextSize = (value / 33).toInt()
                    handleSaveButtonVisibility()
                    tvPreviewReceive.updateTextSize((value / 33).toInt())
                    tvPreviewSend.updateTextSize((value / 33).toInt())
                }
            }
        }
    }

    private fun initData() {
        backgroundScope.launch {
            selectedChatBubbleBgSend = appPreferences.chatBubbleBgSend
            selectedChatBubbleBgReceived = appPreferences.chatBubbleBgReceived
            selectedChatWallpaperBg = appPreferences.chatWallpaperBg
            selectedIsShowDesignOnWallpaper = appPreferences.isShowDesignOnWallpaper
            selectedTextSize = appPreferences.textSize

            val chatBorder = getColorList(R.array.chat_border_color)
            val chatBg = getColorList(R.array.chat_bg_color)
            val chatWallpaperBorder = getColorList(R.array.chat_wallpaper_border)
            val chatWallpaperBg = getColorList(R.array.chat_wallpaper_bg)

            for (i in chatBg.indices) {
                chatColorData.add(Pair(chatBorder[i], chatBg[i]))
            }
            for (i in chatWallpaperBg.indices) {
                chatWallpaperData.add(Pair(chatWallpaperBorder[i], chatWallpaperBg[i]))
            }
            mainScope.launch {
                chatDataAdapter.apply {
                    selectedItem = selectedChatBubbleBgSend
                    updateData(chatColorData)
                    binding.rcvChatData.isVisible = true
                }
                chatWallpaperAdapter.apply {
                    selectedItem = selectedChatWallpaperBg
                    updateData(chatWallpaperData)
                }
                binding.progressbar.isVisible = false
                isDataLoad = true
                handleSaveButtonVisibility()
            }
        }
    }

    private fun initToolbar() {
        binding.toolbar.apply {
            getBinding().tvToolbarTitle.text = getStringValue(R.string.appearance)
            setSupportActionBar(this)
            /* getBinding().btnToolbarAction.isVisible*/
        }
    }

    private fun initToggleButton() {
        binding.apply {
            tvChatColor.setOnSafeClickListener {
                if (toggleChatData) {
                    toggleChatData = false
                    tvChatColor.apply {
                        background =
                            ContextCompat.getDrawable(this@AppearanceActivity, R.drawable.filter_bg)
                        setTextColor(getColorForId(R.color.only_white))
                    }
                    tvChatWallpaper.apply {
                        background = null
                        setTextColor(getColorForId(R.color.app_gray))
                    }
                    rcvChatData.isVisible = isDataLoad
                    rcvChatWallpaperData.isVisible = false
                    binding.progressbar.isVisible = !isDataLoad
                }
            }

            tvChatWallpaper.setOnSafeClickListener {
                if (!toggleChatData) {
                    toggleChatData = true
                    tvChatWallpaper.apply {
                        background =
                            ContextCompat.getDrawable(this@AppearanceActivity, R.drawable.filter_bg)
                        setTextColor(getColorForId(R.color.only_white))
                    }
                    tvChatColor.apply {
                        background = null
                        setTextColor(getColorForId(R.color.app_gray))
                    }
                    rcvChatData.isVisible = false
                    rcvChatWallpaperData.isVisible = isDataLoad
                    binding.progressbar.isVisible = !isDataLoad
                }
            }
        }
    }

    private fun initAdapter() {
        binding.apply {
            rcvChatData.apply {
                hasFixedSize()
                adapter = chatDataAdapter
            }
            rcvChatWallpaperData.apply {
                hasFixedSize()
                adapter = chatWallpaperAdapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndAskDefaultApp()
    }
}