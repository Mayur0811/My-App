package com.messages.common.month_year_picker

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.messages.R
import com.messages.common.month_year_picker.MonthPickerDialog.OnConfigChangeListener
import com.messages.common.month_year_picker.MonthPickerDialog.OnMonthChangedListener
import com.messages.common.month_year_picker.MonthPickerDialog.OnYearChangedListener
import com.messages.common.month_year_picker.MonthViewAdapter.OnDaySelectedListener
import com.messages.common.month_year_picker.YearPickerView.OnYearSelectedListener
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

class MonthPickerView(@JvmField var context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FrameLayout(context, attrs, defStyleAttr) {
    var yearPickerView: YearPickerView
    var monthList: ListView
    private var monthViewAdapter: MonthViewAdapter
    var monthText: TextView
    var yearText: TextView
    private var title: TextView
    var headerFontColorSelected: Int
    var headerFontColorNormal: Int
    var showMonthOnly: Boolean = false
    var onYearChanged: OnYearChangedListener? = null
    var onMonthChanged: OnMonthChangedListener? = null
    private var onDateSet: OnDateSet? = null
    private var onCancel: OnCancel? = null
    private val monthNames: Array<String>

    var month: Int = 0
    var year: Int = 0


    /*private static final int[] ATTRS_TEXT_COLOR = new int[] {
            com.android.internal.R.attr.textColor};
    private static final int[] ATTRS_DISABLED_ALPHA = new int[] {
            com.android.internal.R.attr.disabledAlpha};*/
    constructor(context: Context) : this(context, null) {
        this@MonthPickerView.context = context
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {
        this@MonthPickerView.context = context
    }

    fun init(year: Int, month: Int) {
        this.year = year
        this.month = month
    }

    fun setMaxMonth(maxMonth: Int) {
        if (maxMonth <= Calendar.DECEMBER && maxMonth >= Calendar.JANUARY) {
            monthViewAdapter.setMaxMonth(maxMonth)
        } else {
            throw IllegalArgumentException(
                "Month out of range please send months between " +
                        "Calendar.JANUARY, Calendar.DECEMBER"
            )
        }
    }


    fun setMinMonth(minMonth: Int) {
        if (minMonth >= Calendar.JANUARY && minMonth <= Calendar.DECEMBER) {
            monthViewAdapter.setMinMonth(minMonth)
        } else {
            throw IllegalArgumentException(
                "Month out of range please send months between" +
                        " Calendar.JANUARY, Calendar.DECEMBER"
            )
        }
    }

    fun setMinYear(minYear: Int) {
        yearPickerView.setMinYear(minYear)
    }

    fun setMaxYear(maxYear: Int) {
        yearPickerView.setMaxYear(maxYear)
    }

    fun showMonthOnly() {
        showMonthOnly = true
        yearText.visibility = GONE
    }

    fun showYearOnly() {
        monthList.visibility = GONE
        yearPickerView.visibility = VISIBLE

        monthText.visibility = GONE
        yearText.setTextColor(headerFontColorSelected)
    }

    fun setActivatedMonth(activatedMonth: Int) {
        if (activatedMonth >= Calendar.JANUARY && activatedMonth <= Calendar.DECEMBER) {
            monthViewAdapter.setActivatedMonth(activatedMonth)
            monthText.text = monthNames[activatedMonth]
        } else {
            throw IllegalArgumentException("Month out of range please send months between Calendar.JANUARY, Calendar.DECEMBER")
        }
    }

    fun setActivatedYear(activatedYear: Int) {
        yearPickerView.setActivatedYear(activatedYear)
        yearText.text = "$activatedYear"
    }

    private fun setMonthRange(minMonth: Int, maxMonth: Int) {
        if (minMonth < maxMonth) {
            setMinMonth(minMonth)
            setMaxYear(maxMonth)
        } else {
            throw IllegalArgumentException("maximum month is less then minimum month")
        }
    }

    private fun setYearRange(minYear: Int, maxYear: Int) {
        if (minYear < maxYear) {
            setMinYear(minYear)
            setMaxYear(maxYear)
        } else {
            throw IllegalArgumentException("maximum year is less then minimum year")
        }
    }

    fun setMonthYearRange(minMonth: Int, maxMonth: Int, minYear: Int, maxYear: Int) {
        setMonthRange(minMonth, maxMonth)
        setYearRange(minYear, maxYear)
    }

    fun setTitle(dialogTitle: String?) {
        if (dialogTitle != null && dialogTitle.trim { it <= ' ' }.isNotEmpty()) {
            title.text = dialogTitle
            title.visibility = VISIBLE
        } else {
            title.visibility = GONE
        }
    }

    fun setOnMonthChangedListener(onMonthChangedListener: OnMonthChangedListener?) {
        if (onMonthChangedListener != null) {
            this.onMonthChanged = onMonthChangedListener
        }
    }

    fun setOnYearChangedListener(onYearChangedListener: OnYearChangedListener?) {
        if (onYearChangedListener != null) {
            this.onYearChanged = onYearChangedListener
        }
    }

    fun setOnDateListener(onDateSet: OnDateSet?) {
        this.onDateSet = onDateSet
    }

    fun setOnCancelListener(onCancel: OnCancel?) {
        this.onCancel = onCancel
    }


    interface OnDateSet {
        fun onDateSet()
    }

    interface OnCancel {
        fun onCancel()
    }

    var configChangeListener: OnConfigChangeListener? = null

    init {
        val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mInflater.inflate(R.layout.month_picker_view, this)
        monthNames = DateFormatSymbols(Locale.getDefault()).shortMonths

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MonthPickerView,
            defStyleAttr, 0
        )

        var headerBgColor = a.getColor(R.styleable.MonthPickerView_headerBgColor, 0)
        headerFontColorNormal = a.getColor(R.styleable.MonthPickerView_headerFontColorNormal, 0)
        headerFontColorSelected =
            a.getColor(R.styleable.MonthPickerView_headerFontColorSelected, 0)
        var monthBgColor = a.getColor(R.styleable.MonthPickerView_monthBgColor, 0)
        var monthBgSelectedColor = a.getColor(R.styleable.MonthPickerView_monthBgSelectedColor, 0)
        var monthFontColorNormal = a.getColor(R.styleable.MonthPickerView_monthFontColorNormal, 0)
        var monthFontColorSelected =
            a.getColor(R.styleable.MonthPickerView_monthFontColorSelected, 0)
        var monthFontColorDisabled =
            a.getColor(R.styleable.MonthPickerView_monthFontColorDisabled, 0)
        var headerTitleColor = a.getColor(R.styleable.MonthPickerView_headerTitleColor, 0)
        val actionButtonColor = a.getColor(R.styleable.MonthPickerView_dialogActionButtonColor, 0)

        if (monthFontColorNormal == 0) {
            monthFontColorNormal = ContextCompat.getColor(context, R.color.fontBlackEnable)
        }

        if (monthFontColorSelected == 0) {
            monthFontColorSelected = ContextCompat.getColor(context, R.color.fontWhiteEnable)
        }

        if (monthFontColorDisabled == 0) {
            monthFontColorDisabled = ContextCompat.getColor(context, R.color.fontBlackDisable)
        }
        if (headerFontColorNormal == 0) {
            headerFontColorNormal = ContextCompat.getColor(context, R.color.fontWhiteDisable)
        }
        if (headerFontColorSelected == 0) {
            headerFontColorSelected = ContextCompat.getColor(context, R.color.fontWhiteEnable)
        }
        if (headerTitleColor == 0) {
            headerTitleColor = ContextCompat.getColor(context, R.color.fontWhiteEnable)
        }
        if (monthBgColor == 0) {
            monthBgColor = ContextCompat.getColor(context, R.color.fontWhiteEnable)
        }

        if (headerBgColor == 0) {
            headerBgColor = android.R.attr.colorAccent
            val outValue = TypedValue()
            context.theme.resolveAttribute(headerBgColor, outValue, true)
            headerBgColor = outValue.data
        }

        if (monthBgSelectedColor == 0) {
            monthBgSelectedColor = android.R.attr.colorAccent
            val outValue = TypedValue()
            context.theme.resolveAttribute(monthBgSelectedColor, outValue, true)
            monthBgSelectedColor = outValue.data
        }

        val map: HashMap<String, Int> = HashMap()
        if (monthBgColor != 0) map["monthBgColor"] = monthBgColor
        if (monthBgSelectedColor != 0) map["monthBgSelectedColor"] = monthBgSelectedColor
        if (monthFontColorNormal != 0) map["monthFontColorNormal"] = monthFontColorNormal
        if (monthFontColorSelected != 0) map["monthFontColorSelected"] = monthFontColorSelected
        if (monthFontColorDisabled != 0) map["monthFontColorDisabled"] = monthFontColorDisabled

        a.recycle()

        monthList = findViewById<View>(R.id.listview) as ListView
        yearPickerView = findViewById<View>(R.id.yearView) as YearPickerView
        monthText = findViewById<View>(R.id.month) as TextView
        yearText = findViewById<View>(R.id.year) as TextView
        title = findViewById<View>(R.id.title) as TextView
        val pickerBg = findViewById<View>(R.id.picker_view) as RelativeLayout
        val header = findViewById<View>(R.id.header) as LinearLayout
        val actionBtnLay = findViewById<View>(R.id.action_btn_lay) as RelativeLayout
        val ok = findViewById<View>(R.id.ok_action) as TextView
        val cancel = findViewById<View>(R.id.cancel_action) as TextView


        if (actionButtonColor != 0) {
            ok.setTextColor(actionButtonColor)
            cancel.setTextColor(actionButtonColor)
        } else {
            ok.setTextColor(headerBgColor)
            cancel.setTextColor(headerBgColor)
        }

        if (headerFontColorSelected != 0) monthText.setTextColor(headerFontColorSelected)
        if (headerFontColorNormal != 0) yearText.setTextColor(headerFontColorNormal)
        if (headerTitleColor != 0) title.setTextColor(headerTitleColor)
        if (monthBgColor != 0) pickerBg.setBackgroundColor(monthBgColor)
        if (monthBgColor != 0) actionBtnLay.setBackgroundColor(monthBgColor)

        if (headerBgColor != 0) header.setBackgroundColor(headerBgColor)

        ok.setOnClickListener { onDateSet?.onDateSet() }
        cancel.setOnClickListener { onCancel?.onCancel() }
        monthViewAdapter = MonthViewAdapter(context)
        monthViewAdapter.setColors(map)
        monthViewAdapter.setOnDaySelectedListener(object : OnDaySelectedListener {
            override fun onDaySelected(view: MonthViewAdapter?, month: Int) {
                Log.d(
                    "----------------",
                    "MonthPickerDialogStyle selected month = $month"
                )
                this@MonthPickerView.month = month
                monthText.text = monthNames[month]
                if (!showMonthOnly) {
                    monthList.visibility = GONE
                    yearPickerView.visibility = VISIBLE
                    monthText.setTextColor(headerFontColorNormal)
                    yearText.setTextColor(headerFontColorSelected)
                }
                if (onMonthChanged != null) {
                    onMonthChanged?.onMonthChanged(month)
                }
            }
        })
        monthList.adapter = monthViewAdapter

        yearPickerView.setRange(minYear, maxYear)
        yearPickerView.setColors(map)
        yearPickerView.setYear(Calendar.getInstance()[Calendar.YEAR])
        yearPickerView.setOnYearSelectedListener(object : OnYearSelectedListener {
            override fun onYearChanged(view: YearPickerView?, year: Int) {
                Log.d("----------------", "selected year = $year")
                this@MonthPickerView.year = year
                yearText.text = "$year"
                yearText.setTextColor(headerFontColorSelected)
                monthText.setTextColor(headerFontColorNormal)
                if (onYearChanged != null) {
                    onYearChanged?.onYearChanged(year)
                }
            }
        })
        monthText.setOnClickListener {
            if (monthList.visibility == GONE) {
                yearPickerView.visibility = GONE
                monthList.visibility = VISIBLE
                yearText.setTextColor(headerFontColorNormal)
                monthText.setTextColor(headerFontColorSelected)
            }
        }
        yearText.setOnClickListener {
            if (yearPickerView.visibility == GONE) {
                monthList.visibility = GONE
                yearPickerView.visibility = VISIBLE
                yearText.setTextColor(headerFontColorSelected)
                monthText.setTextColor(headerFontColorNormal)
            }
        }
    }

    fun setOnConfigurationChanged(configChangeListener: OnConfigChangeListener?) {
        this.configChangeListener = configChangeListener
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        configChangeListener?.onConfigChange()
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        @JvmField
        var minYear: Int = 1900

        @JvmField
        var maxYear: Int = Calendar.getInstance()[Calendar.YEAR]
    }
}
