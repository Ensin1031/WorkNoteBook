package com.example.worknotebook

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.util.Locale


class CalendarAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    var days: List<CalendarDay> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedDate: LocalDate? = null

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day.date?.dayOfMonth?.toString() ?: ""

        // Стилизация
        holder.itemView.isEnabled = day.date != null
        holder.itemView.alpha = if (day.isCurrentMonth) 1.0f else 0.4f

        // Сброс фонов
        holder.tvDay.background = null
        holder.itemView.isSelected = false

        // Сегодняшний день
        if (day.isToday) {
            holder.tvDay.setBackgroundResource(android.R.drawable.presence_online) // TODO
        }

        // Выбранная дата
        if (day.date == selectedDate) {
            holder.itemView.isSelected = true
        }

        holder.itemView.setOnClickListener {
            day.date?.let(onDayClick)
        }
    }

    override fun getItemCount() = days.size
}


class CalendarFragment : Fragment() {

    private lateinit var rvCalendar: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnPrevMonth: Button
    private lateinit var btnNextMonth: Button

    private lateinit var calendarAdapter: CalendarAdapter
    private var currentMonth = YearMonth.now()
    private var selectedDate: LocalDate? = null

    companion object {
        @JvmStatic
        fun newInstance() = CalendarFragment()
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView(view)

        setupRecyclerView()
        setupListeners()
        updateCalendar()
    }
    private fun initView(view: View) {
        rvCalendar = view.findViewById(R.id.rvCalendar)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
    }
    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter { date ->
            selectedDate = date
            calendarAdapter.selectedDate = date
            updateCalendar() // обновить подсветку
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            tvSelectedDate.text = "Выбрано: ${dateFormat.format(date)}"
        }
        rvCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendar.adapter = calendarAdapter
    }

    private fun setupListeners() {
        btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateCalendar()
        }
        btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateCalendar()
        }
    }

    private fun updateCalendar() {
        tvMonthYear.text = currentMonth.month.getDisplayName(
            java.time.format.TextStyle.FULL_STANDALONE, Locale.getDefault()
        ) + " " + currentMonth.year

        val days = generateDaysForMonth(currentMonth)
        calendarAdapter.days = days
    }

    private fun generateDaysForMonth(month: YearMonth): List<CalendarDay> {
        val today = LocalDate.now()
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()

        // День недели первого числа (1 = понедельник, 7 = воскресенье)
        val firstDayOfWeek = startOfMonth.dayOfWeek.value
        val leadingEmptyCells = (firstDayOfWeek - 1) % 7

        val days = mutableListOf<CalendarDay>()

        // Пустые ячейки
        repeat(leadingEmptyCells) {
            days.add(CalendarDay(null, isCurrentMonth = false))
        }

        // Дни месяца
        for (day in 1..endOfMonth.dayOfMonth) {
            val date = month.atDay(day)
            days.add(
                CalendarDay(
                    date = date,
                    isCurrentMonth = true,
                    isToday = date == today,
                    isSelected = date == selectedDate
                )
            )
        }

        return days
    }
}
