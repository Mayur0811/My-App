package com.messages.common.month_year_picker

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.ListView
import com.messages.R
import java.text.DateFormatSymbols
import java.util.Locale

internal class MonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.style.MonthPickerDialogStyle
) :
    ListView(context, attrs, defStyleAttr) {
    // days to display
    private val numDays = DEFAULT_NUM_DAYS
    private var numCells = numDays
    private var numRows = DEFAULT_NUM_ROWS

    // layout padding
    private var padding = 40
    private var width = 0
    private var _rowHeight = DEFAULT_HEIGHT

    // paints
    private var monthNumberPaint: Paint? = null
    private var monthNumberDisabledPaint: Paint? = null
    private var monthNumberSelectedPaint: Paint? = null

    // month
    private val monthNames: Array<String> =
        DateFormatSymbols(Locale.getDefault()).shortMonths
    private val monthTextSize: Int
    private val monthHeaderSize: Int
    private var monthSelectedCircleSize = 0
    private var monthBgSelectedColor = 0
    private var monthFontColorNormal = 0
    private var monthFontColorSelected = 0
    private var monthFontColorDisabled = 0
    private var maxMonth = 0
    private var minMonth = 0
    private val rowHeightKey: Int
    private var selectedMonth = -1

    // listener
    private var _onMonthClickListener: OnMonthClickListener? = null

    init {
        val displayMetrics = context.resources.displayMetrics

        monthTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f, displayMetrics
        ).toInt()
        monthHeaderSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f, displayMetrics
        ).toInt()

        monthSelectedCircleSize =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    43f, displayMetrics
                ).toInt()
            } else {
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    43f, displayMetrics
                ).toInt()
            }

        rowHeightKey = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            250f, displayMetrics
        ).toInt()
        _rowHeight = (rowHeightKey - monthHeaderSize) / MAX_NUM_ROWS

        padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f, displayMetrics
        ).toInt()
    }

    /**
     * Sets up the text and style properties for painting.
     */
    private fun initView() {
        monthNumberSelectedPaint = Paint()
        monthNumberSelectedPaint?.isAntiAlias = true
        if (monthBgSelectedColor != 0) monthNumberSelectedPaint?.color = monthBgSelectedColor
        // _monthNumberSelectedPaint.setAlpha(200);
        monthNumberSelectedPaint?.textAlign = Paint.Align.CENTER
        monthNumberSelectedPaint?.style = Paint.Style.FILL
        monthNumberSelectedPaint?.isFakeBoldText = true

        monthNumberPaint = Paint()
        monthNumberPaint?.isAntiAlias = true
        if (monthFontColorNormal != 0) monthNumberPaint?.color = monthFontColorNormal
        monthNumberPaint?.textSize = monthTextSize.toFloat()
        monthNumberPaint?.textAlign = Paint.Align.CENTER
        monthNumberPaint?.style = Paint.Style.FILL
        monthNumberPaint?.isFakeBoldText = false

        monthNumberDisabledPaint = Paint()
        monthNumberDisabledPaint?.isAntiAlias = true
        if (monthFontColorDisabled != 0) monthNumberDisabledPaint?.color =
            monthFontColorDisabled
        monthNumberDisabledPaint?.textSize = monthTextSize.toFloat()
        monthNumberDisabledPaint?.textAlign = Paint.Align.CENTER
        monthNumberDisabledPaint?.style = Paint.Style.FILL
        monthNumberDisabledPaint?.isFakeBoldText = false
    }

    override fun onDraw(canvas: Canvas) {
        drawDays(canvas)
    }

    /**
     * Draws the month days.
     */
    private fun drawDays(canvas: Canvas) {
        var y = (((_rowHeight + monthTextSize) / 2) - DAY_SEPARATOR_WIDTH) + monthHeaderSize
        val dayWidthHalf = (width - padding * 2) / (numDays * 2)
        var j = 0
        for (month in monthNames.indices) {
            val x = (2 * j + 1) * dayWidthHalf + padding
            if (selectedMonth == month) {
                canvas.drawCircle(
                    x.toFloat(),
                    (y - (monthTextSize / 3)).toFloat(),
                    monthSelectedCircleSize.toFloat(),
                    monthNumberSelectedPaint!!
                )
                if (monthFontColorSelected != 0) monthNumberPaint?.color =
                    monthFontColorSelected
            } else {
                if (monthFontColorNormal != 0) monthNumberPaint?.color = monthFontColorNormal
            }

            val paint =
                if ((month < minMonth || month > maxMonth)) monthNumberDisabledPaint else monthNumberPaint
            if (paint != null) {
                canvas.drawText(monthNames[month], x.toFloat(), y.toFloat(), paint)
            }
            j++
            if (j == numDays) {
                j = 0
                y += _rowHeight
            }
        }
    }


    /**
     * Calculates the day that the given x position is in, accounting for week
     * number. Returns the day or -1 if the position wasn't in a day.
     *
     * @param x The x position of the touch event
     * @return The day number, or -1 if the position wasn't in a day
     */
    private fun getMonthFromLocation(x: Float, y: Float): Int {
        val dayStart = padding
        if (x < dayStart || x > width - padding) {
            return -1
        }
        // Selection is (x - start) / (pixels/day) == (x -s) * day / pixels
        val row = (y - monthHeaderSize).toInt() / _rowHeight
        val column = ((x - dayStart) * numDays / (width - dayStart - padding)).toInt()
        var day = column + 1
        day += row * numDays
        if (day < 0 || day > numCells) {
            return -1
        }
        // position - 1 to match with Calender.JANUARY and Calender.DECEMBER
        return day - 1
    }

    /**
     * Called when the user clicks on a day. Handles callbacks to the
     * [OnMonthClickListener] if one is set.
     *
     * @param day The day that was clicked
     */
    private fun onDayClick(day: Int) {
        if (_onMonthClickListener != null) {
            _onMonthClickListener?.onMonthClick(this, day)
        }
    }

    fun setColors(colors: HashMap<String, Int>) {
        if (colors.containsKey("monthBgSelectedColor")) monthBgSelectedColor =
            colors["monthBgSelectedColor"] ?: 0
        if (colors.containsKey("monthFontColorNormal")) monthFontColorNormal =
            colors["monthFontColorNormal"] ?: 0
        if (colors.containsKey("monthFontColorSelected")) monthFontColorSelected =
            colors["monthFontColorSelected"] ?: 0
        if (colors.containsKey("monthFontColorDisabled")) monthFontColorDisabled =
            colors["monthFontColorDisabled"] ?: 0
        initView()
    }

    /**
     * Handles callbacks when the user clicks on a time object.
     */
    interface OnMonthClickListener {
        fun onMonthClick(view: MonthView?, month: Int)
    }

    fun setOnMonthClickListener(listener: OnMonthClickListener?) {
        _onMonthClickListener = listener
    }

    fun setMonthParams(selectedMonth: Int, minMonth: Int, maxMonth: Int) {
        this.selectedMonth = selectedMonth
        this.minMonth = minMonth
        this.maxMonth = maxMonth
        numCells = 12
    }

    fun reuse() {
        numRows = DEFAULT_NUM_ROWS
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec), _rowHeight * numRows
                    + (monthHeaderSize * 2)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        width = w
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val day = getMonthFromLocation(event.x, event.y)
                if (day >= 0) {
                    onDayClick(day)
                }
            }
        }
        return true
    }

    companion object {
        // constants
        private const val DEFAULT_HEIGHT = 100
        private const val DEFAULT_NUM_DAYS = 4
        private const val DEFAULT_NUM_ROWS = 3
        private const val MAX_NUM_ROWS = 3
        private const val DAY_SEPARATOR_WIDTH = 1
    }
}
