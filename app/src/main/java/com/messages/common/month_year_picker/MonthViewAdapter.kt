package com.messages.common.month_year_picker

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import androidx.appcompat.content.res.AppCompatResources
import com.messages.R
import com.messages.common.month_year_picker.MonthView.OnMonthClickListener
import java.util.Calendar

class MonthViewAdapter(private val _context: Context) : BaseAdapter() {
    private var minMonth = 0
    private var maxMonth = 0
    private var activatedMonth = 0
    private var colors: HashMap<String, Int>? = null
    private var mOnDaySelectedListener: OnDaySelectedListener? = null

    override fun getCount(): Int {
        return 1
    }

    override fun getItem(i: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup): View {
        val v: MonthView
        if (convertView != null) {
            v = convertView as MonthView
        } else {
            v = MonthView(_context)
            v.setColors(colors!!)

            // Set up the new view
            val params = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT
            )
            v.layoutParams = params
            v.isClickable = true
            v.setOnMonthClickListener(mOnDayClickListener)
        }
        // If we're running on Honeycomb or newer, then we can use the Theme's
        // selectableItemBackground to ensure that the View has a pressed state
        v.background = AppCompatResources.getDrawable(_context, R.drawable.month_ripple)
        v.setMonthParams(activatedMonth, minMonth, maxMonth)
        v.reuse()
        v.invalidate()
        return v
    }

    private val mOnDayClickListener: OnMonthClickListener = object : OnMonthClickListener {
        override fun onMonthClick(view: MonthView?, month: Int) {
            Log.d("MonthViewAdapter", "onDayClick $month")
            if (isCalendarInRange(month)) {
                Log.d("MonthViewAdapter", "day not null && Calender in range $month")
                setSelectedMonth(month)
                if (mOnDaySelectedListener != null) {
                    mOnDaySelectedListener?.onDaySelected(this@MonthViewAdapter, month)
                }
            }
        }
    }

    init {
        setRange()
    }


    fun isCalendarInRange(value: Int): Boolean {
        return value in minMonth..maxMonth
    }

    /**
     * Updates the selected day and related parameters.
     *
     * @param month The day to highlight
     */
    fun setSelectedMonth(month: Int) {
        Log.d("MonthViewAdapter", "setSelectedMonth : $month")
        activatedMonth = month
        notifyDataSetChanged()
    }

    /* set min and max date and years*/
    fun setRange() {
        minMonth = Calendar.JANUARY
        maxMonth = Calendar.DECEMBER
        activatedMonth = Calendar.AUGUST
        notifyDataSetInvalidated()
    }

    /**
     * Sets the listener to call when the user selects a day.
     *
     * @param listener The listener to call.
     */
    fun setOnDaySelectedListener(listener: OnDaySelectedListener?) {
        mOnDaySelectedListener = listener
    }

    interface OnDaySelectedListener {
        fun onDaySelected(view: MonthViewAdapter?, month: Int)
    }

    fun setMaxMonth(maxMonth: Int) {
        if (maxMonth <= Calendar.DECEMBER && maxMonth >= Calendar.JANUARY) {
            this.maxMonth = maxMonth
        } else {
            throw IllegalArgumentException("Month out of range please send months between Calendar.JANUARY, Calendar.DECEMBER")
        }
    }


    fun setMinMonth(minMonth: Int) {
        if (minMonth >= Calendar.JANUARY && minMonth <= Calendar.DECEMBER) {
            this.minMonth = minMonth
        } else {
            throw IllegalArgumentException("Month out of range please send months between Calendar.JANUARY, Calendar.DECEMBER")
        }
    }

    fun setActivatedMonth(activatedMonth: Int) {
        if (activatedMonth >= Calendar.JANUARY && activatedMonth <= Calendar.DECEMBER) {
            this.activatedMonth = activatedMonth
        } else {
            throw IllegalArgumentException("Month out of range please send months between Calendar.JANUARY, Calendar.DECEMBER")
        }
    }

    fun setColors(map: HashMap<String, Int>?) {
        colors = map
    }
}
