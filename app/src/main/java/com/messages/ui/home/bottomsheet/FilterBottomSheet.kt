package com.messages.ui.home.bottomsheet

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.messages.R
import com.messages.common.month_year_picker.MonthPickerDialog
import com.messages.data.events.ApplyFilter
import com.messages.databinding.FilterBottomSheetBinding
import com.messages.extentions.dateFormatDMY
import com.messages.extentions.getIntValue
import com.messages.extentions.getStringValue
import com.messages.ui.home.adapter.FilterAdapter
import com.messages.utils.SELECTED_FILTER
import com.messages.utils.SELECTED_FILTER_DATA
import com.messages.utils.setOnSafeClickListener
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@AndroidEntryPoint
class FilterBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FilterBottomSheetBinding
    private var selectedFilter = 0
    private val filterAdapter = FilterAdapter()
    private var selectedMonth = 0
    private var selectedYear = 0
    private var selectedDateRange: Pair<Long, Long> = Pair(0, 0)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FilterBottomSheetBinding.inflate(
            inflater, container, false
        )
        initViews()
        return binding.root
    }

    private fun initViews() {
        initData()
        initAdapter()
        initActions()
    }

    private fun initData() {
        selectedFilter = arguments?.getInt(SELECTED_FILTER) ?: 0
        val selectedFilterData =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arguments?.getSerializable(
                SELECTED_FILTER_DATA,
                Triple::class.java
            ) else arguments?.getSerializable(SELECTED_FILTER_DATA) as Triple<Int, Int, Pair<Long, Long>>
        selectedMonth = (selectedFilterData?.first ?: 0) as Int
        selectedYear = (selectedFilterData?.second ?: 0) as Int
        selectedDateRange = (selectedFilterData?.third ?: Pair(0, 0)) as Pair<Long, Long>
    }

    private fun initAdapter() {
        binding.rcvFilter.apply {
            hasFixedSize()
            adapter = filterAdapter
            filterAdapter.selectedFilter = selectedFilter
            filterAdapter.addFilterData(
                resources.getStringArray(R.array.filterName).toList() as ArrayList
            )
            showSelectedFilterData()
        }
    }

    private fun initActions() {
        binding.apply {
            btnCancel.setOnSafeClickListener {
                dismiss()
            }

            btnApply.setOnSafeClickListener {
                EventBus.getDefault().post(
                    ApplyFilter(
                        filter = selectedFilter,
                        month = selectedMonth,
                        year = selectedYear,
                        dateRange = selectedDateRange
                    )
                )
                dismiss()
            }
        }

        filterAdapter.onClickFilter = { filter ->
            handleFilterRange(filter)
        }
    }

    private fun handleFilterRange(filter: Int) {
        when (filter) {
            requireContext().getIntValue(R.integer.today_filter) -> {
                selectedFilter = requireContext().getIntValue(R.integer.today_filter)
                showSelectedFilterData()
            }

            requireContext().getIntValue(R.integer.month_filter) -> {
                openMonthPicker { month, year ->
                    selectedMonth = month
                    selectedYear = year
                    selectedFilter = requireContext().getIntValue(R.integer.month_filter)
                    showSelectedFilterData()
                }
            }

            requireContext().getIntValue(R.integer.year_filter) -> {
                openYearPicker { year ->
                    selectedYear = year
                    selectedFilter = requireContext().getIntValue(R.integer.year_filter)
                    showSelectedFilterData()
                }
            }

            requireContext().getIntValue(R.integer.date_range_filter) -> {
                openDateRangePicker { startDate, endDate ->
                    selectedDateRange = Pair(startDate / 1000, endDate / 1000)
                    selectedFilter = requireContext().getIntValue(R.integer.date_range_filter)
                    showSelectedFilterData()
                }
            }

            else -> {
                selectedFilter = requireContext().getIntValue(R.integer.default_filter)
                showSelectedFilterData()
            }
        }
    }

    private fun showSelectedFilterData() {
        binding.tvFilterDate.apply {
            when (selectedFilter) {
                requireContext().getIntValue(R.integer.today_filter) -> {
                    text = requireContext().getString(
                        R.string.date_show,
                        (Calendar.getInstance().timeInMillis / 1000).dateFormatDMY()
                    )
                }

                requireContext().getIntValue(R.integer.month_filter) -> {
                    text = requireContext().getString(
                        R.string.month_show,
                        "$selectedMonth/$selectedYear"
                    )
                }

                requireContext().getIntValue(R.integer.year_filter) -> {
                    text = requireContext().getString(R.string.year_show, selectedYear)
                }

                requireContext().getIntValue(R.integer.date_range_filter) -> {
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val startDateString: String = sdf.format(Date(selectedDateRange.first * 1000))
                    val endDateString: String = sdf.format(Date(selectedDateRange.second * 1000))
                    text = requireContext().getString(
                        R.string.range_show,
                        "$startDateString to $endDateString"
                    )
                }

                else -> {
                    text = ""
                }
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

    private fun openDateRangePicker(onSelectDate: (Long, Long) -> Unit) {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText(requireContext().getStringValue(R.string.select_range))
        builder.setCalendarConstraints(
            CalendarConstraints.Builder().setEnd(System.currentTimeMillis()).build()
        )

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate: Long = selection.first
            val endDate: Long = selection.second
            onSelectDate.invoke(startDate, endDate)
        }
        datePicker.show(
            childFragmentManager, requireContext().getStringValue(R.string.date_range_picker)
        )
    }

    private fun openYearPicker(onPick: (Int) -> Unit) {
        val today = Calendar.getInstance()
        val builder = MonthPickerDialog.Builder(context = requireContext(),
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH),
            callBack = object : MonthPickerDialog.OnDateSetListener {
                override fun onDateSet(selectedMonth: Int, selectedYear: Int) {
                    onPick.invoke(selectedYear)
                }
            })
        builder.showYearOnly().setYearRange(1990, today.get(Calendar.YEAR)).build().show()
    }


    private fun openMonthPicker(onPick: (Int, Int) -> Unit) {
        val today = Calendar.getInstance()
        val builder = MonthPickerDialog.Builder(context = requireContext(),
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH),
            callBack = object : MonthPickerDialog.OnDateSetListener {
                override fun onDateSet(selectedMonth: Int, selectedYear: Int) {
                    onPick.invoke(selectedMonth + 1, selectedYear)
                }
            })

        builder.setActivatedMonth(today.get(Calendar.MONTH))
            .setActivatedYear(today.get(Calendar.YEAR)).setYearRange(1990, today.get(Calendar.YEAR))
            .build().show()
    }


}