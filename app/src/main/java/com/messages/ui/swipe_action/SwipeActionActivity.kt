package com.messages.ui.swipe_action

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.messages.R
import com.messages.data.events.OnChangeSwipeAction
import com.messages.databinding.ActivitySwipeActionBinding
import com.messages.extentions.getActionDrawable
import com.messages.extentions.getColorForId
import com.messages.extentions.getStringValue
import com.messages.ui.base.BaseActivity
import com.messages.ui.swipe_action.bottomsheet.ActionBottomSheetDialog
import com.messages.utils.AppLogger
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class SwipeActionActivity : BaseActivity() {

    private val binding by lazy { ActivitySwipeActionBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initData()
        initActions()
    }


    private fun initData() {
        binding.apply {
            toolbar.getBinding().apply {
                tvToolbarTitle.text = getStringValue(R.string.swipe_actions)
            }
            tvRightAction.text =
                getStringValue(SwipeAction.fromPosition(appPreferences.rightSwipeAction).stringResId)
            loadActionDrawable(ivRightActionIcon, appPreferences.rightSwipeAction)
            tvLeftAction.text =
                getStringValue(SwipeAction.fromPosition(appPreferences.leftSwipeAction).stringResId)
            loadActionDrawable(ivLeftActionIcon, appPreferences.leftSwipeAction)
        }
    }

    private fun initActions() {
        binding.apply {
            toolbar.getBinding().ivToolbarBack.setOnSafeClickListener {
                finish()
            }
            mcvRightAction.setOnSafeClickListener {
                openChangeAction(true)
            }
            mcvLeftAction.setOnSafeClickListener {
                openChangeAction(false)
            }
        }
    }

    private fun openChangeAction(isRight: Boolean) {
        ActionBottomSheetDialog(isRight) { action ->
            binding.apply {
                AppLogger.d("1234","action -. $action")
                if (isRight) {
                    tvRightAction.text =
                        getStringValue(SwipeAction.fromPosition(action).stringResId)
                    appPreferences.rightSwipeAction = action
                    loadActionDrawable(ivRightActionIcon, action)
                } else {
                    tvLeftAction.text = getStringValue(SwipeAction.fromPosition(action).stringResId)
                    appPreferences.leftSwipeAction = action
                    loadActionDrawable(ivLeftActionIcon, action)
                }
            }
            EventBus.getDefault().postSticky(OnChangeSwipeAction)
        }.apply {
            show(supportFragmentManager, this::class.simpleName)
        }
    }

    private fun loadActionDrawable(imageview: ImageView, action: Int) {
        val actionDrawable = getActionDrawable(action)
        imageview.apply {
            if (actionDrawable == null) {
                isInvisible = true
            } else {
                isVisible = true
                setImageDrawable(actionDrawable)
                backgroundTintList = ColorStateList.valueOf(
                    getColorForId(
                        if (action == SwipeAction.DELETE.ordinal /*getStringValue(R.string.action_delete)*/) {
                            R.color.app_error
                        } else {
                            R.color.app_color
                        }
                    )
                )
            }
        }

    }

}

enum class SwipeAction(val stringResId: Int) {
    NONE(R.string.action_none),
    ARCHIVE(R.string.action_archive),
    DELETE(R.string.action_delete),
    CALL(R.string.action_call),
    MARK_READ(R.string.action_mark_read),
    MARK_UNREAD(R.string.action_mark_unread),
    ADD_PRIVATE(R.string.action_add_private),
    REMOVE_PRIVATE(R.string.action_remove_private);

    companion object {
        fun fromPosition(position: Int): SwipeAction {
            return entries.getOrNull(position) ?: NONE
        }
    }
}