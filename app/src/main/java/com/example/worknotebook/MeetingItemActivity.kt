package com.example.worknotebook

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale


class MeetingItemActivity : AppCompatActivity() {

    private lateinit var session: UserSessionManager
    private var syncMeetingsImmediately: Boolean = false

    private lateinit var btnMenu: ImageButton
    private lateinit var btnAlarmSync: ImageButton
    private lateinit var btnGoToEditMode: ImageButton
    private lateinit var btnGoToBack: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSaveMeeting: ImageView

    private lateinit var meetingNotesContentContainer: ConstraintLayout
    private lateinit var btnCreateMeetingNote: ImageView

    private var currentMeeting: Meeting? = null
    private var startDate: Long? = null
    private var endDate: Long? = null
    private var startTime: Long? = null
    private var endTime: Long? = null

    private lateinit var tvUpdatedAt: TextView
    private lateinit var tvIsArchieRecord: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var containerCreatedAt: LinearLayout
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText

    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText

    private var isViewMode = true
    private var isCreateMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meeting_item)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.page_meeting_item_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setupListeners()
        determineMode()

        // Прослушиваем событие попытки удаления
        supportFragmentManager.setFragmentResultListener(
            DialogConfirmDelete.RESULT_KEY,
            this
        ) { _, bundle ->
            val deleteMode = bundle.getString(DialogConfirmDelete.RESULT_KEY)
            when (deleteMode) {
                DialogConfirmDelete.RESULT_ARCHIVE -> deleteMeeting(archive = true)
                DialogConfirmDelete.RESULT_DELETE -> deleteMeeting(archive = false)
            }
        }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_meeting_notes, fragment)
            .commit()
    }
    companion object {
        const val EXTRA_MEETING_ID = "extra_meeting_id"
        const val EXTRA_MEETING_DATA = "extra_meeting_data"
        const val EXTRA_MEETING_DATE_DATA = "extra_meeting_date_data"
        const val NEED_VIEW_MODE = "view_mode"

        fun startForCreate(context: Context, date: Long? = null) {
            val intent = Intent(context, MeetingItemActivity::class.java)
            if (date != null) {
                intent.putExtra(EXTRA_MEETING_DATE_DATA, date)
            }
            intent.putExtra(NEED_VIEW_MODE, false)
            context.startActivity(intent)
        }
        fun startForEdit(context: Context, meetingId: Int, viewMode: Boolean = true) {
            val intent = Intent(context, MeetingItemActivity::class.java)
            intent.putExtra(EXTRA_MEETING_ID, meetingId)
            intent.putExtra(NEED_VIEW_MODE, viewMode)
            context.startActivity(intent)
        }
        fun startForEdit(context: Context, meeting: Meeting, viewMode: Boolean = true) {
            val intent = Intent(context, MeetingItemActivity::class.java)
            intent.putExtra(EXTRA_MEETING_DATA, meeting)
            intent.putExtra(NEED_VIEW_MODE, viewMode)
            context.startActivity(intent)
        }
    }
    private fun initViews() {
        session = UserSessionManager.getInstance(this)
        syncMeetingsImmediately = session.getUserSettings()?.syncMeetingsImmediately ?: false
        btnMenu = findViewById(R.id.btn_meeting_item_header_menu)
        btnAlarmSync = findViewById(R.id.btn_meeting_alarm_sync_header)
        btnGoToEditMode = findViewById(R.id.btn_edit_meeting)
        btnGoToBack = findViewById(R.id.tv_meeting_item_header_back)
        progressBar = findViewById(R.id.meeting_item_header_progress_bar)

        tvUpdatedAt = findViewById(R.id.tv_meeting_item_update_at)
        tvIsArchieRecord = findViewById(R.id.tv_meeting_item_is_archive_record)
        tvCreatedAt = findViewById(R.id.tv_meeting_item_create_at)
        containerCreatedAt = findViewById(R.id.meeting_item_footer)
        etTitle = findViewById(R.id.et_meeting_title)
        etLocation = findViewById(R.id.et_meeting_location)
        etDescription = findViewById(R.id.et_meeting_description)

        etStartDate = findViewById(R.id.et_meeting_start_date)
        etEndDate = findViewById(R.id.et_meeting_end_date)
        etStartTime = findViewById(R.id.et_meeting_start_time)
        etEndTime = findViewById(R.id.et_meeting_end_time)

        btnSaveMeeting = findViewById(R.id.btn_save_meeting_header)

        meetingNotesContentContainer = findViewById(R.id.meeting_notes_content_container)
        btnCreateMeetingNote = findViewById(R.id.btn_create_meeting_note)
    }
    private fun setupListeners() {
        btnGoToBack.setOnClickListener {
            navigateToMeetings()
        }
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }
        btnGoToEditMode.setOnClickListener {
            if (isViewMode && currentMeeting?.id != null && currentMeeting?.isActive == true) {
                isViewMode = false
                initUpdateMode(meeting = currentMeeting!!)
            }
        }
        etStartDate.setOnClickListener {  // Назначение даты начала встречи
            if (!isViewMode && currentMeeting?.isActive == true) {
                showDatePicker(dateValue = startDate) { timestamp ->
                    startDate = timestamp
                    etStartDate.setText(formatDateFromTimestamp(timestamp))
                }
            }
        }
        etEndDate.setOnClickListener {  // Назначение даты окончания встречи
            if (!isViewMode && currentMeeting?.isActive == true) {
                showDatePicker(dateValue = endDate) { timestamp ->
                    endDate = timestamp
                    etEndDate.setText(formatDateFromTimestamp(timestamp))
                }
            }
        }
        etStartTime.setOnClickListener {  // Назначение времени начала встречи
            if (!isViewMode && currentMeeting?.isActive == true) {
                showTimePicker(timeValue = startTime) { offset ->
                    startTime = offset
                    etStartTime.setText(formatTimeFromOffset(offset))
                }
            }
        }
        etEndTime.setOnClickListener {  // Назначение времени окончания встречи
            if (!isViewMode && currentMeeting?.isActive == true) {
                showTimePicker(timeValue = endTime) { offset ->
                    endTime = offset
                    etEndTime.setText(formatTimeFromOffset(offset))
                }
            }
        }
        btnSaveMeeting.setOnClickListener {

            etTitle.error = null
            etLocation.error = null
            etDescription.error = null
            etStartDate.error = null
            etEndDate.error = null
            etStartTime.error = null
            etEndTime.error = null

            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (isViewMode || currentMeeting == null || currentMeeting?.isActive != true) {
                error("Don`t saved")
            } else if (title.isEmpty() && location.isEmpty() && description.isEmpty()) {
                etTitle.error = resources.getString(R.string.errorNodeEmptySave)
                etLocation.error = resources.getString(R.string.errorNodeEmptySave)
                etDescription.error = resources.getString(R.string.errorNodeEmptySave)
            } else if (startDate == null) {
                etStartDate.error = resources.getString(R.string.AddValue)
            } else if (endDate == null) {
                etEndDate.error = resources.getString(R.string.AddValue)
            } else if (startTime == null) {
                etStartTime.error = resources.getString(R.string.AddValue)
            } else if (endTime == null) {
                etEndTime.error = resources.getString(R.string.AddValue)
            } else if (
                title == currentMeeting?.title &&
                location == currentMeeting?.location &&
                description == currentMeeting?.description &&
                startDate == currentMeeting!!.startDate &&
                endDate == currentMeeting!!.endDate &&
                startTime == currentMeeting!!.startTime && endTime == currentMeeting!!.endTime
            ) {
                etTitle.error = resources.getString(R.string.errorSaveNoChanged)
                etLocation.error = resources.getString(R.string.errorSaveNoChanged)
                etDescription.error = resources.getString(R.string.errorSaveNoChanged)
            } else if (isNotValidInputDates(startDate = startDate!!, endDate = endDate!!)) {
                etEndDate.error = resources.getString(R.string.errorMeetingEndMoreStart)
            } else if (isNotValidInputTimes(startTime = startTime!!, endTime = endTime!!)) {
                etEndTime.error = resources.getString(R.string.errorMeetingEndMoreStart)
            } else {
                val savedMeeting: Meeting = currentMeeting!!.copy(
                    title = title,
                    description = description,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    startTime = startTime,
                    endTime = endTime,
                    // если изменяем существующий - принудительно указываем дату / время нового изменения
                    updatedAt = if (currentMeeting!!.id == null) { currentMeeting!!.createdAt } else { System.currentTimeMillis() },
                )
                saveMeeting(
                    savedMeeting = savedMeeting
                )
            }
        }
    }
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val menu = popup.menu
        popup.menuInflater.inflate(R.menu.item_menu, popup.menu)
        menu.findItem(R.id.menu_item_action_set_priority)?.isVisible = false
        if (isCreateMode) {
            menu.findItem(R.id.menu_item_action_sync)?.isVisible = false
            menu.findItem(R.id.menu_item_action_delete)?.isVisible = false
            menu.findItem(R.id.menu_item_action_recover)?.isVisible = false
        } else if (currentMeeting?.isActive == true) {
            menu.findItem(R.id.menu_item_action_recover)?.isVisible = false
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_action_sync -> {
                    syncMeeting()
                    true
                }
                R.id.menu_item_action_delete -> {
                    showConfirmDeleteDialog()
                    true
                }
                R.id.menu_item_action_recover -> {
                    recoverMeeting()
                    true
                }
                R.id.menu_item_action_cancel -> {
                    navigateToMeetings()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun determineMode() {
        val user = session.getUser()
        if (user == null) {
            Log.d("LOGOUT", "-----===== Ошибка. Отсутствует, либо не залогинен пользователь =====-----")
            logout()
        }
        if (intent.hasExtra(NEED_VIEW_MODE)) {
            isViewMode = intent.getBooleanExtra(NEED_VIEW_MODE, true)
        }
        var extraDate: Long? = null
        if (intent.hasExtra(EXTRA_MEETING_DATE_DATA)) {
            extraDate = intent.getLongExtra(EXTRA_MEETING_DATE_DATA, 0L)
        }
        when {
            // Передан ID встречи - загружаем из БД
            intent.hasExtra(EXTRA_MEETING_ID) -> {
                isCreateMode = false
                val meetingId = intent.getIntExtra(EXTRA_MEETING_ID, -1)
                if (meetingId != -1) {
                    val meeting: Meeting? = session.getMeetingById(meetingId = meetingId.toLong())
                    if (meeting == null) {
                        Log.d("STRUCTURE_ERROR", "-----===== Ошибка. Попытка открыть не существующий объект встречи пользователя.\nДанные: meetingId = $meetingId =====-----")
                        error("Системная ошибка")
                    } else {
                        currentMeeting = meeting
                        if (isViewMode) {
                            initReadMode(meeting = meeting)
                        } else {
                            initUpdateMode(meeting = meeting)
                        }
                    }
                }
            }
            // Передан полный объект - Режим редактирования
            intent.hasExtra(EXTRA_MEETING_DATA) -> {
                isCreateMode = false
                var meeting: Meeting?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    meeting = intent.getParcelableExtra(EXTRA_MEETING_DATA, Meeting::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    meeting = intent.getParcelableExtra(EXTRA_MEETING_DATA) as? Meeting
                }
                if (meeting == null) {
                    Log.d("STRUCTURE_ERROR", "-----===== Ошибка. Попытка открыть не существующий объект встречи пользователя. =====-----")
                    error("Системная ошибка")
                } else {
                    currentMeeting = meeting
                    if (isViewMode) {
                        initReadMode(meeting = meeting)
                    } else {
                        initUpdateMode(meeting = meeting)
                    }
                }
            }
            else -> {
                isCreateMode = true
                initCreateMode(user = user!!, extraDate = extraDate)
            }
        }
    }
    private fun setNoteItemView(meeting: Meeting) {

        meetingNotesContentContainer.visibility = if (isCreateMode) { View.GONE } else { View.VISIBLE }

        if (!isCreateMode) {
            meetingNotesContentContainer.visibility = View.VISIBLE
            loadFragment(NotesFragment.newInstance(parentMeetingId = meeting.id))
            btnCreateMeetingNote.apply {
                setOnClickListener {
                    NoteItemActivity.startForCreate(
                        context = this@MeetingItemActivity,
                        parentMeeting = meeting
                    )
                }
                visibility = if (isViewMode) { View.GONE } else { View.VISIBLE }
            }
        }

        btnGoToBack.visibility = if (isViewMode) { View.VISIBLE } else { View.GONE }
        btnSaveMeeting.visibility = if (isViewMode) { View.GONE } else { View.VISIBLE }
        btnMenu.visibility = View.VISIBLE

        tvUpdatedAt.apply {
            text = formatDateFromTimestamp(timestamp = meeting.updatedAt, needDateTime = true)
            visibility = View.VISIBLE
        }
        tvIsArchieRecord.visibility = if (meeting.isActive) { View.GONE } else { View.VISIBLE }
        tvCreatedAt.text = formatDateFromTimestamp(timestamp = meeting.createdAt, needDateTime = true)

        startDate = currentMeeting?.startDate
        etStartDate.apply {
            setText(formatDateFromTimestamp(currentMeeting?.startDate))
            isClickable = !isViewMode
            visibility = View.VISIBLE
        }
        endDate = currentMeeting?.endDate
        etEndDate.apply {
            setText(formatDateFromTimestamp(currentMeeting?.endDate))
            isClickable = !isViewMode
            visibility = View.VISIBLE
        }
        startTime = currentMeeting?.startTime
        etStartTime.apply {
            setText(formatTimeFromOffset(currentMeeting?.startTime))
            isClickable = !isViewMode
            visibility = View.VISIBLE
        }
        endTime = currentMeeting?.endTime
        etEndTime.apply {
            setText(formatTimeFromOffset(currentMeeting?.endTime))
            isClickable = !isViewMode
            visibility = View.VISIBLE
        }

        etTitle.apply {
            setText(meeting.title)
            isEnabled = !isViewMode
            hint = if (isViewMode) { "" } else { etTitle.hint.toString() }
            visibility = View.VISIBLE
        }
        etLocation.apply {
            setText(meeting.location)
            isEnabled = !isViewMode
            hint = if (isViewMode) { "" } else { etLocation.hint.toString() }
            visibility = View.VISIBLE
        }
        etDescription.apply {
            setText(meeting.description)
            isEnabled = !isViewMode
            hint = if (isViewMode) { "" } else { etDescription.hint.toString() }
            visibility = View.VISIBLE
        }
        btnAlarmSync.apply {
            setOnClickListener {
                syncMeeting()
            }
            visibility = if (isCreateMode || meeting.isSync) { View.GONE } else { View.VISIBLE }
        }
    }
    private fun initReadMode(meeting: Meeting) {
        currentMeeting = meeting
        setNoteItemView(meeting = meeting)
        btnGoToEditMode.visibility = if (meeting.isActive) { View.VISIBLE } else { View.GONE }
        tvCreatedAt.visibility = View.VISIBLE
        containerCreatedAt.visibility = View.VISIBLE
    }
    private fun initUpdateMode(meeting: Meeting) {
        currentMeeting = meeting
        setNoteItemView(meeting = meeting)
        btnGoToEditMode.visibility = View.GONE
        tvCreatedAt.visibility = View.VISIBLE
        containerCreatedAt.visibility = View.VISIBLE
    }
    private fun initCreateMode(user: User, extraDate: Long? = null) {
        val calendar = Calendar.getInstance()
        val currentTimestamp = calendar.timeInMillis
        val currentDateTimestamp = extraDate ?: LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        ).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val currentTimeOffset = (calendar.get(Calendar.HOUR_OF_DAY) * 3600L + calendar.get(Calendar.MINUTE) * 60L) * 1000L
        val meeting = Meeting(
            id = null,
            externalId = null,
            userId = user.id!!,
            externalUserId = user.externalId,
            title = "",
            description = "",
            startDate = currentDateTimestamp,
            endDate = currentDateTimestamp,
            startTime = currentTimeOffset,
            endTime = currentTimeOffset,
            location = "",
            createdAt = currentTimestamp,
            updatedAt = currentTimestamp,
            isActive = true,
            isSync = false,
        )
        currentMeeting = meeting
        setNoteItemView(meeting)
        btnGoToEditMode.visibility = View.GONE
    }
    private fun syncMeeting(andInArchive: Boolean = false, meeting: Meeting? = null, goToViewMode: Boolean = false) {
        val syncMeeting: Meeting? = meeting?.copy(
            isSync = true,
            isActive = if (andInArchive) { false } else { meeting.isActive }
        ) ?: currentMeeting?.copy(
            isSync = true,
            isActive = if (andInArchive) { false } else { currentMeeting!!.isActive }
        )

        if (syncMeeting == null) {
            Log.d("STRUCTURE_ERROR", "----==== Ошибка. Попытка сохранить на бэке несуществующий объект Встречи пользователя ====----")
            error("Don`t save")
        }
        startViewProcess()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.createOrSyncMeeting(
                    request = SyncMeeting(
                        meeting = syncMeeting,
                        notes = session.getNotes(filters = NodeFilters(), meetingId = syncMeeting.id)
                    )
                )
                if (response.isSuccessful) {
                    val meetingResponseData = response.body()
                    val meetingData = meetingResponseData?.meeting
                    val meetingNotesData = meetingResponseData?.notes
                    if (meetingData != null) {
                        val savedMeeting = Meeting(
                            id = syncMeeting.id,
                            externalId = meetingData.externalId,
                            userId = syncMeeting.userId,
                            externalUserId = meetingData.externalUserId,
                            title = syncMeeting.title,
                            description = syncMeeting.description,
                            startDate = syncMeeting.startDate,
                            endDate = syncMeeting.endDate,
                            startTime = syncMeeting.startTime,
                            endTime = syncMeeting.endTime,
                            location = syncMeeting.location,
                            createdAt = meetingData.createdAt,
                            updatedAt = meetingData.updatedAt,
                            isSync = true,
                            isActive = meetingData.isActive,
                        )
                        if (!meetingNotesData.isNullOrEmpty()) {
                            meetingNotesData.forEach { note ->
                                val syncNote = note.copy(
                                    id = note.kaId,
                                    userId = note.kaUserId ?: 0L,
                                    parentNoteId = note.kaParentNoteId,
                                    meetingId = note.kaMeetingId,
                                    isSync = true
                                )
                                if (syncNote.id == null) {
                                    session.addNote(note = syncNote)
                                } else {
                                    session.updateNote(note = syncNote)
                                }
                            }
                        }
                        if (savedMeeting.id == null) {
                            val meetingId = session.addMeeting(meeting = savedMeeting)
                            Toast.makeText(this@MeetingItemActivity, resources.getString(R.string.SuccessfulSaving), Toast.LENGTH_SHORT).show()
                            endViewProcess()
                            startForEdit(context = this@MeetingItemActivity, meetingId = meetingId.toInt(), viewMode = true)
                        } else {
                            session.updateMeeting(meeting = savedMeeting)
                            currentMeeting = savedMeeting
                            if (andInArchive) {
                                session.deleteMeeting(meetingId = savedMeeting.id!!, archive = true, isSync = true)
                            }
                            endViewProcess()
                            if (isViewMode || goToViewMode) {
                                isViewMode = true
                                initReadMode(meeting = savedMeeting)
                            } else {
                                initUpdateMode(meeting = savedMeeting)
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MeetingItemActivity,
                            resources.getString(R.string.SaveError),
                            Toast.LENGTH_SHORT
                        ).show()
                        endViewProcess()
                    }
                } else {
                    Toast.makeText(
                        this@MeetingItemActivity,
                        parseError(response),
                        Toast.LENGTH_SHORT
                    ).show()
                    endViewProcess()
                }
            } catch (e: Exception) {
                AlertDialog.Builder(this@MeetingItemActivity)
                    .setMessage("${resources.getString(R.string.Error)}: $e")
                    .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                        dialog.dismiss()
                        endViewProcess()
                    }
                    .show()
            }
        }
    }
    private fun recoverMeeting() {
        if (currentMeeting?.id == null) {
            Log.d("STRUCTURE_ERROR", "-----===== Ошибка. Попытка синхронизировать несуществующую встречу =====-----")
            error("Системная ошибка")
        }

        fun endLocalProcess(meeting: Meeting) {
            session.updateMeeting(meeting)
            currentMeeting = meeting
            endViewProcess()
            if (isViewMode) {
                initReadMode(meeting = meeting)
            } else {
                initUpdateMode(meeting = meeting)
            }
        }

        startViewProcess()
        val recoveredMeeting: Meeting = currentMeeting!!.copy(
            isActive = true,
            isSync = false,
            updatedAt = System.currentTimeMillis()
        )

        if (syncMeetingsImmediately) {
            syncMeeting(meeting = recoveredMeeting)
        } else {
            endLocalProcess(meeting = recoveredMeeting)
        }
    }
    private fun showConfirmDeleteDialog() {
        DialogConfirmDelete
            .newInstance(isActive = currentMeeting?.isActive?.or(false) == true)
            .show(supportFragmentManager, "DialogConfirmDelete")
    }
    private fun deleteMeeting(archive: Boolean) {
        val user = session.getUser()
        if (user == null) {
            logout()
            return
        }
        if (currentMeeting?.id == null) {
            Log.d(
                "STRUCTURE_ERROR",
                "-----===== Ошибка. Попытка удалить несуществующую встречу =====-----"
            )
            error("Системная ошибка")
        }
        startViewProcess()
        val meetingId = currentMeeting!!.id!!

        fun delEndLocal(id: Long, isSync: Boolean) {
            session.deleteMeeting(meetingId = id, archive = archive, isSync = isSync)
            endViewProcess()
            if (archive) {
                startForEdit(context = this@MeetingItemActivity, meetingId = id.toInt(), viewMode = true)
            } else {
                navigateToMeetings()
            }
        }

        if (syncMeetingsImmediately) {
            lifecycleScope.launch {
                try {
                    if (archive) {
                        // ситуация, когда отсутствует externalId - т.е. запись не сохранена на бэке.
                        // Но по условиям выставленного флага syncMeetingsImmediately - его нужно сохранить.
                        syncMeeting(andInArchive = true)
                    } else if (currentMeeting!!.externalId == null) {
                        // в этой ситуации - просто удалим локально полностью
                        delEndLocal(id = meetingId, isSync = false)
                    } else {
                        // пробуем удалить на бэке, и в любом случае удаляем локально
                        val response = RetrofitClient.api.deleteMeeting(meetingId = currentMeeting!!.externalId!!.toInt())
                        val isSync: Boolean = response.isSuccessful
                        delEndLocal(id = meetingId, isSync = isSync)
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this@MeetingItemActivity)
                        .setMessage("${resources.getString(R.string.Error)}: $e")
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endViewProcess()
                        }
                        .show()
                }
            }
        } else {
            delEndLocal(id = meetingId, isSync = false)
        }
    }
    private fun saveMeeting(savedMeeting: Meeting) {
        if (isViewMode || !savedMeeting.isActive) {
            error("Don`t save")
        }
        startViewProcess()
        if (syncMeetingsImmediately) {
            syncMeeting(andInArchive = false, meeting = savedMeeting, goToViewMode = true)
            endViewProcess()
        } else {
            var meetingId: Long
            savedMeeting.isSync = false
            if (savedMeeting.id == null) {
                meetingId = session.addMeeting(savedMeeting)
            } else {
                meetingId = savedMeeting.id!!
                savedMeeting.updatedAt = System.currentTimeMillis()
                session.updateMeeting(savedMeeting)
            }
            Toast.makeText(this@MeetingItemActivity, resources.getString(R.string.SuccessfulSaving), Toast.LENGTH_SHORT).show()
            endViewProcess()
            startForEdit(context = this@MeetingItemActivity, meetingId = meetingId.toInt(), viewMode = true)
        }
    }
    private fun startViewProcess() {
        progressBar.visibility = View.VISIBLE
    }
    private fun endViewProcess() {
        progressBar.visibility = View.GONE
    }
    private fun logout() {
        Toast.makeText(this, resources.getString(R.string.userIsNotLogged), Toast.LENGTH_SHORT).show()
        session.logout()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun navigateToMeetings() {
        val intent = Intent(this, MainSelectionActivity::class.java).apply {
            putExtra(MainSelectionActivity.OPEN_FRAGMENT, MainSelectionActivity.FRAGMENT_MEETINGS)
            putExtra(MainSelectionActivity.CURRENT_DATE, currentMeeting?.startDate)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    private fun showDatePicker(dateValue: Long?, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val (year, month, day) = if (dateValue != null) {
            val localDate = Instant.ofEpochMilli(dateValue).atZone(ZoneId.systemDefault()).toLocalDate()
            Triple(localDate.year, localDate.monthValue - 1, localDate.dayOfMonth)
        } else {
            Triple(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        }
        DatePickerDialog(this, { _, y, m, d ->
            val selectedDate = LocalDate.of(y, m + 1, d)
            val timestamp = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            onDateSelected(timestamp)
        }, year, month, day).show()
    }
    private fun showTimePicker(timeValue: Long?, onTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val (hour, minute) = if (timeValue != null) {
            val hours = (timeValue / (3600 * 1000)).toInt()
            val minutes = ((timeValue % (3600 * 1000)) / (60 * 1000)).toInt()
            Pair(hours.coerceIn(0, 23), minutes.coerceIn(0, 59))
        } else {
            Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        }
        TimePickerDialog(this, { _, hourOfDay, minute ->
            val offset = (hourOfDay * 3600L + minute * 60L) * 1000L
            onTimeSelected(offset)
        }, hour, minute, true).show()
    }
    private fun formatDateFromTimestamp(timestamp: Long?, needDateTime: Boolean = false): String {
        return if (timestamp == null) {
            ""
        } else if (needDateTime) {
            val createdAtDate = Date(timestamp)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
            dateFormat.format(createdAtDate)
        } else {
            val localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            String.format("%02d.%02d.%d", localDate.dayOfMonth, localDate.monthValue, localDate.year)
        }
    }
    private fun formatTimeFromOffset(offset: Long?): String {
        if (offset == null) return ""
        val hours = (offset / (3600 * 1000)).toInt().coerceIn(0, 23)
        val minutes = ((offset % (3600 * 1000)) / (60 * 1000)).toInt().coerceIn(0, 59)
        return String.format("%02d:%02d", hours, minutes)
    }
    private fun isNotValidInputDates(startDate: Long, endDate: Long): Boolean {
        return endDate < startDate
    }
    private fun isNotValidInputTimes(startTime: Long, endTime: Long): Boolean {
        return endTime <= startTime
    }
}