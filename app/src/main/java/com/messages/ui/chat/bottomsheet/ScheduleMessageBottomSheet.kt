package com.messages.ui.chat.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.messages.R
import com.messages.data.pref.AppPreferences
import com.messages.databinding.ScheduleMessageBottomSheetDialogBinding
import com.messages.extentions.formatMessageDate
import com.messages.extentions.formatMessageTime
import com.messages.extentions.getStringValue
import com.messages.extentions.toast
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleMessageBottomSheet(
    private var scheduleTime: Long? = null,
    private var callback: (Long) -> Unit
) : BottomSheetDialogFragment() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var binding: ScheduleMessageBottomSheetDialogBinding
    private var selectedTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ScheduleMessageBottomSheetDialogBinding.inflate(inflater, container, false)
        initViews()
        initAction()
        return binding.root
    }

    private fun initAction() {
        binding.apply {
            clScheduleDate.setOnSafeClickListener {
                openDatePicker()
            }
            clScheduleTime.setOnSafeClickListener {
                openTimePicker()
            }

            btnCancel.setOnSafeClickListener {
                dismiss()
            }

            btnDone.setOnSafeClickListener {
                if (validateDateTime()) {
                    callback.invoke(selectedTime.timeInMillis)
                    dismiss()
                }
            }
        }
    }

    private fun initViews() {
        binding.apply {
            if (scheduleTime != null) {
                selectedTime.timeInMillis = scheduleTime ?: 0
                val currentTime = scheduleTime?.div(1000)
                tvScheduleDate.text = currentTime?.formatMessageDate()
                tvScheduleTime.text = currentTime?.formatMessageTime(appPreferences.use24Hr)
            } else {
                openDatePicker()
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            bottomSheet.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isDraggable = false
                behavior.isFitToContents = true
            }
        }
        return bottomSheetDialog
    }

    private fun openDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            binding.tvScheduleDate.text = (selection / 1000).formatMessageDate()
            selectedTime.timeInMillis = selection
            if (scheduleTime == null) {
                openTimePicker()
            }
        }

        datePicker.show(childFragmentManager, requireContext().getStringValue(R.string.date_picker))
    }

    private fun openTimePicker() {
        val timePicker =
            MaterialTimePicker.Builder()
                .setTimeFormat(if (appPreferences.use24Hr) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(10)
                .build()

        timePicker.addOnPositiveButtonClickListener {
            val hour =
                if (appPreferences.use24Hr) timePicker.hour else if (timePicker.hour > 12) timePicker.hour - 12 else timePicker.hour
            val timeAmPm =
                if (appPreferences.use24Hr) "" else if (timePicker.hour >= 12) "PM" else "AM"
            binding.tvScheduleTime.text =
                getString(R.string.schedule_time_show, hour, timePicker.minute, timeAmPm)
            selectedTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedTime.set(Calendar.MINUTE, timePicker.minute)
        }

        timePicker.show(childFragmentManager, requireContext().getStringValue(R.string.time_picker))
    }

    private fun validateDateTime(): Boolean {
        return if (selectedTime.timeInMillis < (System.currentTimeMillis() + 1000L)) {
            requireContext().toast(getString(R.string.must_pick_time_in_the_future))
            false
        } else {
            true
        }
    }
}