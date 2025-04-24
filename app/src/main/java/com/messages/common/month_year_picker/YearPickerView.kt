package com.messages.common.month_year_picker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.messages.R

class YearPickerView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    ListView(context, attrs, defStyleAttr) {
    private val yearAdapter: YearAdapter
    private val viewSize: Int
    private val childSize: Int
    private var onYearSelectedListener: OnYearSelectedListener? = null
    private var colors: HashMap<String, Int>? = null

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : this(
        context,
        attrs,
        R.style.PickerStyle
    ) {
        super.setSelector(android.R.color.transparent)
    }

    init {
        val frame = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        )
        layoutParams = frame
        val res = context.resources
        viewSize = res.getDimensionPixelOffset(R.dimen.datepicker_view_animator_height)
        childSize = res.getDimensionPixelOffset(R.dimen.datepicker_year_label_height)
        yearAdapter = YearAdapter(context)
        onItemClickListener =
            OnItemClickListener { _, _, position, _ ->
                val year = yearAdapter.getYearForPosition(position)
                yearAdapter.setSelection(year)
                if (onYearSelectedListener != null) {
                    onYearSelectedListener?.onYearChanged(this@YearPickerView, year)
                }
            }
        adapter = yearAdapter
    }

    fun setOnYearSelectedListener(listener: OnYearSelectedListener?) {
        onYearSelectedListener = listener
    }

    /**
     * Sets the currently selected year. Jumps immediately to the new year.
     *
     * @param year the target year
     */
    fun setYear(year: Int) {
        yearAdapter.setSelection(year)
        post {
            val position = yearAdapter.getPositionForYear(year)
            if (position >= 0 /*&& position < getCount()*/) {
                setSelectionCentered(position)
            }
        }
    }

    fun setSelectionCentered(position: Int) {
        val offset = viewSize / 2 - childSize / 2
        setSelectionFromTop(position, offset)
    }

    fun setRange(min: Int, max: Int) {
        yearAdapter.setRange(min, max)
    }

    fun setColors(colors: HashMap<String, Int>?) {
        this.colors = colors
    }

    val firstPositionOffset: Int
        get() {
            val firstChild = getChildAt(0) ?: return 0
            return firstChild.top
        }

    fun setMinYear(minYear: Int) {
        yearAdapter.setMinYear(minYear)
    }

    fun setMaxYear(maxYear: Int) {
        yearAdapter.setMaxYear(maxYear)
    }

    fun setActivatedYear(activatedYear: Int) {
        yearAdapter.setActivatedYear(activatedYear)
    }

    /**
     * The callback used to indicate the user changed the year.
     */
    interface OnYearSelectedListener {
        /**
         * Called upon a year change.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         */
        fun onYearChanged(view: YearPickerView?, year: Int)
    }

    inner class YearAdapter(context: Context?) : BaseAdapter() {
        private val itemLayout = R.layout.year_label_text_view
        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private var activatedYear = 0
        private var minYear = 0
        private var maxYear = 0
        private var count = 0

        fun setRange(min: Int, max: Int) {
            val mCount = max - min + 1
            if (minYear != min || maxYear != max || count != mCount) {
                minYear = min
                maxYear = max
                count = mCount
                notifyDataSetInvalidated()
            }
        }

        fun setSelection(year: Int): Boolean {
            if (activatedYear != year) {
                activatedYear = year
                notifyDataSetChanged()
                return true
            }
            return false
        }

        override fun getCount(): Int {
            return count
        }

        override fun getItem(position: Int): Int {
            return getYearForPosition(position)
        }

        override fun getItemId(position: Int): Long {
            return getYearForPosition(position).toLong()
        }

        fun getPositionForYear(year: Int): Int {
            return year - minYear
        }

        fun getYearForPosition(position: Int): Int {
            return minYear + position
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v: TextView
            val hasNewView = convertView == null
            v = if (hasNewView) {
                inflater.inflate(itemLayout, parent, false) as TextView
            } else {
                convertView as TextView
            }
            val year = getYearForPosition(position)
            val activated = activatedYear == year

            if (hasNewView || v.tag != null || v.tag == activated) {
                if (activated) {
                    if (colors?.containsKey("monthBgSelectedColor") == true) {
                        colors?.get("monthBgSelectedColor")?.let { v.setTextColor(it) }
                    }
                    v.textSize = 25f
                } else {
                    if (colors?.containsKey("monthFontColorNormal") == true) {
                        colors?.get("monthFontColorNormal")?.let { v.setTextColor(it) }
                    }
                    v.textSize = 20f
                }
                v.tag = activated
            }
            v.text = year.toString()
            return v
        }

        override fun getItemViewType(position: Int): Int {
            return 0
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun isEmpty(): Boolean {
            return false
        }

        override fun areAllItemsEnabled(): Boolean {
            return true
        }

        override fun isEnabled(position: Int): Boolean {
            return true
        }

        fun setMaxYear(maxYear: Int) {
            this.maxYear = maxYear
            count = this.maxYear - minYear + 1
            notifyDataSetInvalidated()
        }

        fun setMinYear(minYear: Int) {
            this.minYear = minYear
            count = maxYear - this.minYear + 1
            notifyDataSetInvalidated()
        }

        fun setActivatedYear(activatedYear: Int) {
            if (activatedYear in minYear..maxYear) {
                this.activatedYear = activatedYear
                setYear(activatedYear)
            } else {
                throw IllegalArgumentException("activated date is not in range")
            }
        }
    }
}

