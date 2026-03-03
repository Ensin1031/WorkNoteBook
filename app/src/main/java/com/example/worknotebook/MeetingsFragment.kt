package com.example.worknotebook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.getValue


fun formatMeetingDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy E", Locale.getDefault())
    return date.format(formatter)
}

fun formatMeetingTime(offsetMillis: Long?): String {
    if (offsetMillis == null) return ""
    val hours = offsetMillis / (3600 * 1000)
    val minutes = (offsetMillis % (3600 * 1000)) / (60 * 1000)
    return String.format("%02d:%02d", hours, minutes)
}

class DayMeetingsAdapter(
    var onMeetingClick: (Meeting) -> Unit
) : RecyclerView.Adapter<DayMeetingsAdapter.MeetingViewHolder>() {

    var meetings: List<Meeting> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class MeetingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIsSync: ImageView = itemView.findViewById(R.id.iv_is_sync_meeting)
        private val ivIsActive: ImageView = itemView.findViewById(R.id.iv_is_active_meeting)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tv_meeting_card_start_time)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tv_meeting_card_end_time)
        private val tvTimeDash: TextView = itemView.findViewById(R.id.tv_meeting_card_time_dash)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_meeting_card_title)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_meeting_card_location)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_meeting_card_description)

        fun bind(meeting: Meeting, clickListener: (Meeting) -> Unit) {
            ivIsSync.apply {
                visibility = if (!meeting.isSync) { View.VISIBLE } else { View.GONE }
            }
            ivIsActive.apply {
                visibility = if (!meeting.isActive) { View.VISIBLE } else { View.GONE }
            }
            tvTitle.apply {
                text = meeting.title
                visibility = if (!meeting.title.isEmpty()) { View.VISIBLE } else { View.GONE }
            }
            tvLocation.apply {
                text = meeting.location
                visibility = if (!meeting.location.isEmpty()) { View.VISIBLE } else { View.GONE }
            }
            tvDescription.apply {
                text = meeting.description
                visibility = if (!meeting.description.isEmpty()) { View.VISIBLE } else { View.GONE }
            }
            tvStartTime.apply {
                text = formatMeetingTime(meeting.startTime)
                visibility = if (meeting.startTime != null) { View.VISIBLE } else { View.GONE }
            }
            tvEndTime.apply {
                text = formatMeetingTime(meeting.endTime)
                visibility = if (meeting.endTime != null) { View.VISIBLE } else { View.GONE }
            }
            tvTimeDash.visibility = if (meeting.startTime != null && meeting.endTime != null) { View.VISIBLE } else { View.GONE }

            itemView.setOnClickListener { clickListener(meeting) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_meeting, parent, false)
        return MeetingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MeetingViewHolder, position: Int) {
        holder.bind(meetings[position], onMeetingClick)
    }

    override fun getItemCount() = meetings.size
}


class DayCardAdapter(
    private val onMeetingClick: (Meeting) -> Unit
) : RecyclerView.Adapter<DayCardAdapter.DayViewHolder>() {

    var items: List<DayCardItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHeader: TextView = itemView.findViewById(R.id.tv_meetings_group_by_day)
        private val rvMeetings: RecyclerView = itemView.findViewById(R.id.rv_meetings_by_day_arr)
        private val addMeetingBTN: ImageButton = itemView.findViewById(R.id.btn_add_meeting)
        private val tvEmpty: TextView = itemView.findViewById(R.id.tvEmptyMeetings)
        val meetingsAdapter: DayMeetingsAdapter

        init {
            meetingsAdapter = DayMeetingsAdapter { meeting ->
                // Передать клик наружу  // TODO
                // onMeetingClick(meeting)
            }
            rvMeetings.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = meetingsAdapter
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(item: DayCardItem, onMeetingClick: (Meeting) -> Unit) {
            tvHeader.text = formatMeetingDate(item.dateTimestamp)
            addMeetingBTN.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    MeetingItemActivity.startForCreate(itemView.context, date = item.dateTimestamp)
                }
            }
            meetingsAdapter.onMeetingClick = onMeetingClick

            if (item.meetings.isEmpty()) {
                rvMeetings.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            } else {
                rvMeetings.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
                meetingsAdapter.meetings = item.meetings
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_card, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(items[position], onMeetingClick)
    }

    override fun getItemCount() = items.size
}


class MeetingsFragment : Fragment() {

    private val sharedViewModel: MainSharedViewModel by activityViewModels()

    private lateinit var session: UserSessionManager
    private var hasConnection: Boolean = false
    private var filters: MeetingFilters = MeetingFilters(
        viewOnlyActive = true,
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var dayAdapter: DayCardAdapter
//    private lateinit var addMeetingBTN: ImageButton  // TODO

    // Пагинация
    private var currentStartDate: Long = 0L
    private var currentEndDate: Long = 0L
    private val loadedMeetingsMap = mutableMapOf<Long, List<Meeting>>()
    private var isLoading = false

    // Корневая дата
    private var rootDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            rootDate = it.getLong(ARG_ROOT_DATE, 0L)
        }
        // Если корневая дата не передана, используем сегодняшнюю
        if (rootDate == 0L) {
            rootDate = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meetings, container, false)
    }
    companion object {
        private const val ARG_ROOT_DATE = "root_date"
        @JvmStatic
        fun newInstance(rootDate: Long? = null) = MeetingsFragment().apply {
            arguments = Bundle().apply {
                if (rootDate != null) {
                    putLong(ARG_ROOT_DATE, rootDate)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        reloadCurrentRange()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        observeViewModel()
        initDateRange()
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    sharedViewModel.hasConnection,
                    sharedViewModel.meetingFilters
                ) { isConnected, filters ->
                    Pair(isConnected, filters)
                }.collect { (isConnected, filters) ->
                    hasConnection = isConnected
                    if (filters.goToDate != null && rootDate != filters.goToDate && this@MeetingsFragment.filters.goToDate != filters.goToDate) {
                        jumpToDate(dateTs = filters.goToDate!!)
                        this@MeetingsFragment.filters = filters
                    } else if (this@MeetingsFragment.filters.goToDate != filters.goToDate) {
                        jumpToDate(dateTs = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                    } else {
                        this@MeetingsFragment.filters = filters
                        reloadCurrentRange()
                    }
                }
            }
        }
    }
    private fun initView(view: View) {
        session = UserSessionManager.getInstance(requireContext())
        RetrofitClient.init(requireContext())

        recyclerView = view.findViewById(R.id.rv_page_meetings_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        dayAdapter = DayCardAdapter { meeting ->
            MeetingItemActivity.startForEdit(requireContext(), meeting, true)
        }
        recyclerView.adapter = dayAdapter

//        addMeetingBTN = view.findViewById(R.id.btn_add_meetings)  // TODO
//        addMeetingBTN.setOnClickListener {
//            MeetingItemActivity.startForCreate(requireContext())
//        }

        // Слушатель для подгрузки при скролле
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val firstVisible = layoutManager.findFirstVisibleItemPosition()

                if (lastVisible >= dayAdapter.itemCount - 1 && !isLoading) {
                    loadMoreDaysForward()
                }
                if (firstVisible <= 0 && !isLoading) {
                    loadMoreDaysBackward()
                }
            }
        })
    }

    /**
     * Публичный метод для перехода к указанной дате.
     * Перезагружает данные вокруг новой даты и прокручивает к ней.
     */
    fun jumpToDate(dateTs: Long) {
        rootDate = dateTs
        loadedMeetingsMap.clear()
        val rootLocalDate = LocalDate.ofEpochDay(dateTs / 86400000)
        currentStartDate = rootLocalDate.minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        currentEndDate = rootLocalDate.plusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        lifecycleScope.launch {
            loadMeetingsForNewRange(currentStartDate, currentEndDate)
            scrollToDate(toDate = dateTs)
        }
    }

    private fun scrollToDate(toDate: Long) {
        val position = dayAdapter.items.indexOfFirst { it.dateTimestamp == toDate }
        if (position != -1) {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
        }
    }

    private fun initDateRange() {
        val rootLocalDate = LocalDate.ofEpochDay(rootDate / 86400000)
        currentStartDate = rootLocalDate.minusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        currentEndDate = rootLocalDate.plusDays(10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        lifecycleScope.launch {
            loadMeetingsForNewRange(currentStartDate, currentEndDate)
            scrollToDate(toDate = rootDate) // после первой загрузки прокручиваем к корневой дате
        }
    }

    private fun reloadCurrentRange() {
        loadedMeetingsMap.clear()
        lifecycleScope.launch {
            loadMeetingsForNewRange(currentStartDate, currentEndDate)
        }
    }

    private fun loadMoreDaysForward() {
        if (isLoading) return
        isLoading = true
        val newEnd = LocalDate.ofEpochDay(currentEndDate / 86400000)
            .plusDays(10)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        currentEndDate = newEnd
        lifecycleScope.launch {
            loadMeetingsForNewRange(currentStartDate, currentEndDate)
            isLoading = false
        }
    }

    private fun loadMoreDaysBackward() {
        if (isLoading) return
        isLoading = true
        val newStart = LocalDate.ofEpochDay(currentStartDate / 86400000)
            .minusDays(10)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        currentStartDate = newStart
        lifecycleScope.launch {
            loadMeetingsForNewRange(currentStartDate, currentEndDate)
            isLoading = false
        }
    }

    private suspend fun loadMeetingsForNewRange(start: Long, end: Long) {
        val existingDates = loadedMeetingsMap.keys
        val allDates = generateDateRange(start, end)
        val missingDates = allDates.filter { it !in existingDates }.toSet()

        if (missingDates.isNotEmpty()) {
            val minMissing = missingDates.minOrNull() ?: start
            val maxMissing = missingDates.maxOrNull() ?: end

            val newMeetings = withContext(Dispatchers.IO) {
                session.getMeetingsInDateRange(minMissing, maxMissing, filters = filters)
            }

            // Строим карту: дата -> список встреч, которые длятся в этот день
            val grouped = mutableMapOf<Long, MutableList<Meeting>>()
            // Группируем по дате и добавляем в карту
            for (meeting in newMeetings) {
                val meetingStart = meeting.startDate ?: continue
                val meetingEnd = meeting.endDate ?: meetingStart // если endDate нет, то только один день

                // Перебираем все дни от meetingStart до meetingEnd включительно
                var current = meetingStart
                while (current <= meetingEnd) {
                    if (current in missingDates) { // добавляем только для отсутствующих дат
                        grouped.getOrPut(current) { mutableListOf() }.add(meeting)
                    }
                    current += 86400000L // +1 день
                }
            }
            loadedMeetingsMap.putAll(grouped)
        }
        updateDayCards()
    }

    private fun generateDateRange(start: Long, end: Long): List<Long> {
        val result = mutableListOf<Long>()
        var current = start
        while (current <= end) {
            result.add(current)
            current = LocalDate.ofEpochDay(current / 86400000)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        return result
    }

    private fun updateDayCards() {
        val cards = buildDayCards()
        dayAdapter.items = cards
    }

    private fun buildDayCards(): List<DayCardItem> {
        val result = mutableListOf<DayCardItem>()
        var current = currentStartDate
        while (current <= currentEndDate) {
            val meetings = loadedMeetingsMap[current] ?: emptyList()
            result.add(DayCardItem(current, meetings))
            current = LocalDate.ofEpochDay(current / 86400000)
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        return result
    }

}
