package com.example.worknotebook

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.lang.System
import java.util.Date
import java.util.Locale


class NoteItemActivity : AppCompatActivity() {

    private lateinit var session: UserSessionManager
    private var syncNotesImmediately: Boolean = false

    private lateinit var btnMenu: ImageButton
    private lateinit var btnGoToEditMode: ImageButton
    private lateinit var btnGoToBack: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSaveNote: ImageView

    private var notePriority: NotePriority = NotePriority.NORMAL
    private lateinit var inputNodeTitle: EditText
    private lateinit var inputNodeContent: EditText

    private lateinit var llDateCreate: LinearLayout
    private lateinit var tvDateCreate: TextView
    private lateinit var tvDateUpdate: TextView
    private lateinit var priorityMarkerUpper: View

    private var currentNote: Note? = null

    private var isViewMode = true
    private var isCreateMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_note_item)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.page_note_item_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setupListeners()
        determineMode()

        // Прослушиваем событие смены значения приоритета (в режиме создания / редактирования)
        supportFragmentManager.setFragmentResultListener(
            DialogChangePriority.RESULT_KEY,
            this
        ) { _, bundle ->
            val priorityName = bundle.getString(DialogChangePriority.RESULT_KEY)
            val enteredPriority = NotePriority.fromString(priorityName)
            setNotePriority(enteredPriority)
        }

        // Прослушиваем событие попытки удаления
        supportFragmentManager.setFragmentResultListener(
            DialogConfirmDelete.RESULT_KEY,
            this
        ) { _, bundle ->
            val deleteMode = bundle.getString(DialogConfirmDelete.RESULT_KEY)
            when (deleteMode) {
                DialogConfirmDelete.RESULT_ARCHIVE -> deleteNote(archive = true)
                DialogConfirmDelete.RESULT_DELETE -> deleteNote(archive = false)
            }
        }
    }
    private fun initViews() {
        session = UserSessionManager.getInstance(this)
        syncNotesImmediately = session.getUserSettings()?.syncNotesImmediately ?: false
        progressBar = findViewById(R.id.note_item_header_progress_bar)
        btnMenu = findViewById(R.id.btn_note_item_header_menu)
        btnGoToEditMode = findViewById(R.id.btn_edit_note)
        btnGoToBack = findViewById(R.id.tv_note_item_header_back)
        btnSaveNote = findViewById(R.id.btn_save_note_header)

        inputNodeTitle = findViewById(R.id.et_note_title)
        inputNodeContent = findViewById(R.id.et_note_content)

        priorityMarkerUpper = findViewById(R.id.page_note_item_upper_priority_marker)
        llDateCreate = findViewById(R.id.note_item_footer)
        tvDateCreate = findViewById(R.id.tv_note_item_create_at)
        tvDateUpdate = findViewById(R.id.tv_note_item_update_at)
    }
    private fun setupListeners() {
        val user = session.getUser()
        if (user != null) {
            btnMenu.setOnClickListener { view ->
                showPopupMenu(view)
            }
            btnGoToEditMode.setOnClickListener {
                if (isViewMode && currentNote?.id != null && currentNote?.isActive == true) {
                    isViewMode = false
                    initUpdateMode(note = currentNote!!)
                }
            }
            btnGoToBack.setOnClickListener {
                navigateToNotes()
            }
            btnSaveNote.setOnClickListener {
                val title = inputNodeTitle.text.toString().trim()
                val content = inputNodeContent.text.toString().trim()
                if (isViewMode) {
                    error("Don`t saved")
                } else if (currentNote == null) {
                    inputNodeTitle.error = resources.getString(R.string.SaveError)
                    inputNodeContent.error = resources.getString(R.string.SaveError)
                } else if (currentNote?.isActive != true) {
                    inputNodeTitle.error = resources.getString(R.string.SaveError)
                    inputNodeContent.error = resources.getString(R.string.SaveError)
                } else if (title.isEmpty() && content.isEmpty()) {
                    inputNodeTitle.error = resources.getString(R.string.errorNodeEmptySave)
                    inputNodeContent.error = resources.getString(R.string.errorNodeEmptySave)
                } else if (currentNote!!.priority == notePriority && currentNote!!.title == title && currentNote!!.content == content) {
                    inputNodeTitle.error = resources.getString(R.string.errorSaveNoChanged)
                    inputNodeContent.error = resources.getString(R.string.errorSaveNoChanged)
                } else {
                    val createdAt: Long? = if (currentNote!!.id == null) { currentNote!!.createdAt } else { null }
                    val updatedAt: Long? = if (currentNote!!.id == null) { currentNote!!.updatedAt } else { null }
                    saveNote(
                        savedNote = Note(
                            id = currentNote!!.id,
                            externalId = currentNote!!.externalId,
                            userId = currentNote!!.userId,
                            externalUserId = currentNote!!.externalUserId,
                            parentNoteId = currentNote!!.parentNoteId,
                            externalParentNoteId = currentNote!!.externalParentNoteId,
                            meetingId = currentNote!!.meetingId,
                            externalMeetingId = currentNote!!.externalMeetingId,
                            title = title,
                            content = content,
                            priority = notePriority,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                        )
                    )
                }
            }
        } else {
            logout()
        }
    }
    private fun navigateToNotes() {
        val intent = Intent(this, MainSelectionActivity::class.java).apply {
            putExtra(MainSelectionActivity.OPEN_FRAGMENT, MainSelectionActivity.FRAGMENT_NOTES)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        if (isCreateMode) {
            popup.menuInflater.inflate(R.menu.create_node_item_menu, popup.menu)
        } else if (isViewMode && currentNote?.isActive != true) {
            popup.menuInflater.inflate(R.menu.view_node_not_active_item_menu, popup.menu)
        } else if (isViewMode) {
            popup.menuInflater.inflate(R.menu.view_node_item_menu, popup.menu)
        } else if (currentNote?.isActive != true) {
            popup.menuInflater.inflate(R.menu.update_node_not_active_item_menu, popup.menu)
        } else {
            popup.menuInflater.inflate(R.menu.update_node_item_menu, popup.menu)
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                // Создание записи
                R.id.action_set_priority_by_create -> {
                    showPriorityDialog()
                    true
                }
                R.id.action_cancel_by_create -> {
                    navigateToNotes()
                    true
                }

                // Просмотр записи
                R.id.action_delete_by_view -> {
                    showConfirmDeleteDialog()
                    true
                }
                R.id.action_recover_by_view -> {
                    recoverNode()
                    true
                }
                R.id.action_sync_by_view -> {
                    syncNote()
                    true
                }
                R.id.action_cancel_by_view -> {
                    navigateToNotes()
                    true
                }

                // Изменение записи
                R.id.action_set_priority_by_update -> {
                    showPriorityDialog()
                    true
                }
                R.id.action_sync_by_update -> {
                    syncNote()
                    true
                }
                R.id.action_delete_by_update -> {
                    showConfirmDeleteDialog()
                    true
                }
                R.id.action_recover_by_update -> {
                    recoverNode()
                    true
                }
                R.id.action_cancel_by_update -> {
                    navigateToNotes()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun showPriorityDialog() {
        DialogChangePriority
            .newInstance(priority = notePriority)
            .show(supportFragmentManager, "DialogChangePriority")
    }
    private fun showConfirmDeleteDialog() {
        DialogConfirmDelete
            .newInstance(isActive = currentNote?.isActive?.or(false) == true)
            .show(supportFragmentManager, "DialogConfirmDelete")
    }
    private fun setNoteIdemView(nodeData: Note) {
        setNotePriority(nodeData.priority)
        inputNodeTitle.apply {
            setText(nodeData.title)
            isEnabled = !isViewMode
            hint = if (isViewMode) { "" } else { inputNodeTitle.hint.toString() }
            visibility = View.VISIBLE
        }
        inputNodeContent.apply {
            setText(nodeData.content)
            isEnabled = !isViewMode
            hint = if (isViewMode) { "" } else { inputNodeContent.hint.toString() }
            visibility = View.VISIBLE
        }

        var formattedUpdatedAtDate = ""
        if (nodeData.updatedAt != null) {
            val updatedAtDate = Date(nodeData.updatedAt!!)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
            formattedUpdatedAtDate = dateFormat.format(updatedAtDate)
        }
        tvDateUpdate.apply {
            text = formattedUpdatedAtDate
            visibility = View.VISIBLE
        }

        var formattedCreatedAtDate = ""
        if (nodeData.createdAt != null) {
            val createdAtDate = Date(nodeData.createdAt)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
            formattedCreatedAtDate = dateFormat.format(createdAtDate)
        }
        tvDateCreate.text = formattedCreatedAtDate

        btnMenu.visibility = View.VISIBLE
        if (!isViewMode) {
            btnSaveNote.visibility = View.VISIBLE
        }
    }
    private fun initReadMode(note: Note) {
        setNoteIdemView(note)
        btnGoToBack.visibility = View.VISIBLE
        llDateCreate.visibility = View.VISIBLE
        if (note.isActive) {
            btnGoToEditMode.visibility = View.VISIBLE
        } else {
            btnGoToEditMode.visibility = View.GONE
        }
    }
    private fun initUpdateMode(note: Note) {
        setNoteIdemView(note)
        btnGoToEditMode.visibility = View.GONE
        btnGoToEditMode.visibility = View.GONE
        llDateCreate.visibility = View.VISIBLE
    }
    private fun initCreateMode() {
        btnGoToEditMode.visibility = View.GONE
        btnGoToEditMode.visibility = View.GONE
        llDateCreate.visibility = View.GONE
        val user = session.getUser()
        if (user?.id != null) {
            val currentTimestamp = System.currentTimeMillis()

            val newNote = Note(
                title = "",
                content = "",
                createdAt = currentTimestamp,
                updatedAt = currentTimestamp,
                priority = NotePriority.NORMAL,
                id = null,
                userId = user.id,
                parentNoteId = null,
                meetingId = null,
                externalId = null,
                externalUserId = user.externalId,
                externalParentNoteId = null,
                externalMeetingId = null
            )
            currentNote = newNote
            setNoteIdemView(newNote)
        } else {
            logout()
        }
    }
    private fun logout() {
        Toast.makeText(this, resources.getString(R.string.userIsNotLogged), Toast.LENGTH_SHORT).show()
        session.logout()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun setNotePriority(priority: NotePriority) {
        notePriority = priority
        val notePriorityColorRes = if (currentNote?.isActive == true) { priority.colorResId } else { R.color.textColor_10 }
        priorityMarkerUpper.setBackgroundColor(ContextCompat.getColor(this, notePriorityColorRes))
    }
    private fun determineMode() {
        if (intent.hasExtra(NEED_VIEW_MODE)) {
            isViewMode = intent.getBooleanExtra(NEED_VIEW_MODE, true)
        }
        when {
            // Передан ID заметки - загружаем с сервера
            intent.hasExtra(EXTRA_NOTE_ID) -> {
                isCreateMode = false
                val noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1)
                if (noteId != -1) {
                    val note: Note? = session.getNoteById(noteId = noteId.toLong())
                    if (note == null) {
                        Log.d(
                            "STRUCTURE_ERROR",
                            "-----===== Ошибка. Попытка открыть не существующий " +
                                    "объект заметки пользователя.\n" +
                                    "Данные: noteId = $noteId =====-----"
                        )
                        error("Системная ошибка")
                    } else {
                        currentNote = note
                        if (isViewMode) {
                            initReadMode(note = note)
                        } else {
                            initUpdateMode(note = note)
                        }
                    }
                }
            }
            // Передан полный объект Note - Режим редактирования
            intent.hasExtra(EXTRA_NOTE_DATA) -> {
                isCreateMode = false
                var note: Note?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    note = intent.getParcelableExtra(EXTRA_NOTE_DATA, Note::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    note = intent.getParcelableExtra(EXTRA_NOTE_DATA) as? Note
                }

                if (note == null) {
                    Log.d(
                        "STRUCTURE_ERROR",
                        "-----===== Ошибка. Попытка открыть не существующий " +
                                "объект заметки пользователя. =====-----"
                    )
                    error("Системная ошибка")
                } else {
                    currentNote = note
                    if (isViewMode) {
                        initReadMode(note = note)
                    } else {
                        initUpdateMode(note = note)
                    }
                }
            }
            else -> {
                // Режим создания новой заметки
                isCreateMode = true
                initCreateMode()
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_DATA = "extra_note_data"
        const val NEED_VIEW_MODE = "view_mode"

        fun startForCreate(context: Context) {
            val intent = Intent(context, NoteItemActivity::class.java)
            intent.putExtra(NEED_VIEW_MODE, false)
            context.startActivity(intent)
        }
        fun startForEdit(context: Context, noteId: Int, viewMode: Boolean = true) {
            val intent = Intent(context, NoteItemActivity::class.java)
            intent.putExtra(EXTRA_NOTE_ID, noteId)
            intent.putExtra(NEED_VIEW_MODE, viewMode)
            context.startActivity(intent)
        }
        fun startForEdit(context: Context, note: Note, viewMode: Boolean = true) {
            val intent = Intent(context, NoteItemActivity::class.java)
            intent.putExtra(EXTRA_NOTE_DATA, note)
            intent.putExtra(NEED_VIEW_MODE, viewMode)
            context.startActivity(intent)
        }
    }
    private fun recoverNode() {
        if (currentNote?.id == null) {
            Log.d(
                "STRUCTURE_ERROR",
                "-----===== Ошибка. Попытка синхронизировать несуществующую заметку =====-----"
            )
            error("Системная ошибка")
        }

        fun endLocalProcess(note: Note) {
            session.updateNote(note)
            currentNote = note
            endViewProcess()
            if (isViewMode) {
                initReadMode(note = note)
            } else {
                initUpdateMode(note = note)
            }
        }

        startViewProcess()
        val recoveredNote: Note = currentNote!!.copy(
            isActive = true,
            updatedAt = System.currentTimeMillis()
        )
        if (syncNotesImmediately) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.syncNote(recoveredNote)
                    if (response.isSuccessful) {
                        val noteData = response.body()
                        if (noteData != null) {
                            val syncNote = Note(
                                id = recoveredNote.id,
                                externalId = noteData.externalId,
                                userId = recoveredNote.userId,
                                externalUserId = noteData.externalUserId,
                                parentNoteId = recoveredNote.parentNoteId,
                                externalParentNoteId = noteData.externalParentNoteId,
                                meetingId = recoveredNote.meetingId,
                                externalMeetingId = noteData.externalMeetingId,
                                title = noteData.title,
                                content = noteData.content,
                                createdAt = noteData.createdAt,
                                updatedAt = noteData.updatedAt,
                                priority = noteData.priority,
                                isActive = noteData.isActive,
                            )
                            endLocalProcess(note = syncNote)
                        } else {
                            Toast.makeText(
                                this@NoteItemActivity,
                                resources.getString(R.string.SaveError),
                                Toast.LENGTH_SHORT
                            ).show()
                            endViewProcess()
                        }
                    } else {
                        Toast.makeText(
                            this@NoteItemActivity,
                            parseError(response),
                            Toast.LENGTH_SHORT
                        ).show()
                        endViewProcess()
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this@NoteItemActivity)
                        .setMessage("${resources.getString(R.string.Error)}: $e")
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endViewProcess()
                        }
                        .show()
                }
            }
        } else {
            endLocalProcess(note = recoveredNote)
        }
    }
    private fun syncNote(andInArchive: Boolean = false) {
        if (currentNote?.id == null) {
            Log.d(
                "STRUCTURE_ERROR",
                "-----===== Ошибка. Попытка синхронизировать несуществующую заметку =====-----"
            )
            error("Системная ошибка")
        }

        fun delEndLocal(note: Note) {
            session.deleteNote(noteId = note.id!!, archive = true)
            endViewProcess()
            if (isViewMode) {
                initReadMode(note = note)
            } else {
                initUpdateMode(note = note)
            }
        }

        startViewProcess()
        val syncNote: Note = currentNote!!.copy(
            isActive = if (andInArchive) { false } else { currentNote!!.isActive }
        )
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.syncNote(syncNote)
                if (response.isSuccessful) {
                    val noteData = response.body()
                    if (noteData != null) {
                        val note = Note(
                            id = syncNote.id,
                            externalId = noteData.externalId,
                            userId = syncNote.userId,
                            externalUserId = noteData.externalUserId,
                            parentNoteId = syncNote.parentNoteId,
                            externalParentNoteId = noteData.externalParentNoteId,
                            meetingId = syncNote.meetingId,
                            externalMeetingId = noteData.externalMeetingId,
                            title = noteData.title,
                            content = noteData.content,
                            createdAt = noteData.createdAt,
                            updatedAt = noteData.updatedAt,
                            priority = noteData.priority,
                            isActive = noteData.isActive,
                        )
                        session.updateNote(note)
                        currentNote = note
                        if (andInArchive) {
                            delEndLocal(note = note)
                        } else {
                            endViewProcess()
                            if (isViewMode) {
                                initReadMode(note = note)
                            } else {
                                initUpdateMode(note = note)
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@NoteItemActivity,
                            resources.getString(R.string.SaveError),
                            Toast.LENGTH_SHORT
                        ).show()
                        endViewProcess()
                    }
                } else {
                    Toast.makeText(
                        this@NoteItemActivity,
                        parseError(response),
                        Toast.LENGTH_SHORT
                    ).show()
                    endViewProcess()
                }
            } catch (e: Exception) {
                AlertDialog.Builder(this@NoteItemActivity)
                    .setMessage("${resources.getString(R.string.Error)}: $e")
                    .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                        dialog.dismiss()
                        endViewProcess()
                    }
                    .show()
            }
        }
    }
    private fun deleteNote(archive: Boolean) {
        val user = session.getUser()
        if (user == null) {
            logout()
            return
        }
        if (currentNote?.id == null) {
            Log.d(
                "STRUCTURE_ERROR",
                "-----===== Ошибка. Попытка удалить несуществующую заметку =====-----"
            )
            error("Системная ошибка")
        }
        startViewProcess()
        val noteId = currentNote!!.id!!

        fun delEndLocal(id: Long) {
            session.deleteNote(noteId = id, archive = archive)
            endViewProcess()
            if (archive) {
                startForEdit(context = this@NoteItemActivity, noteId = id.toInt(), viewMode = true)
            } else {
                navigateToNotes()
            }
        }

        if (syncNotesImmediately) {
            lifecycleScope.launch {
                try {
                    if (archive) {
                        // ситуация, когда отсутствует externalId - т.е. запись не сохранена на бэке.
                        // Но по условиям выставленного флага syncNotesImmediately - его нужно сохранить.
                        syncNote(andInArchive = true)
                    } else if (currentNote!!.externalId == null) {
                        // в этой ситуации - просто удалим локально полностью
                        delEndLocal(id = noteId)
                    } else {
                        // пробуем удалить на бэке, и в любом случае удаляем локально
                        RetrofitClient.api.deleteNote(noteId = currentNote!!.externalId!!.toInt())
                        delEndLocal(id = noteId)
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this@NoteItemActivity)
                        .setMessage("${resources.getString(R.string.Error)}: $e")
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endViewProcess()
                        }
                        .show()
                }
            }
        } else {
            delEndLocal(id = noteId)
        }
    }
    private fun saveNote(savedNote: Note) {
        if (isViewMode) {
            error("Don`t save")
        }
        startViewProcess()
        if (syncNotesImmediately) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.createNote(savedNote)
                    if (response.isSuccessful) {
                        val noteData = response.body()
                        var noteId = savedNote.id
                        if (noteData != null) {
                            val note = Note(
                                id = noteId,
                                externalId = noteData.externalId,
                                userId = savedNote.userId,
                                externalUserId = savedNote.externalUserId,
                                parentNoteId = savedNote.parentNoteId,
                                externalParentNoteId = noteData.externalParentNoteId,
                                meetingId = savedNote.meetingId,
                                externalMeetingId = noteData.externalMeetingId,
                                title = savedNote.title,
                                content = savedNote.content,
                                createdAt = noteData.createdAt,
                                updatedAt = noteData.updatedAt,
                                priority = noteData.priority
                            )
                            if (noteId == null) {
                                noteId = session.addNote(note)
                            } else {
                                session.updateNote(note)
                            }
                            Toast.makeText(
                                this@NoteItemActivity,
                                resources.getString(R.string.SuccessfulSaving),
                                Toast.LENGTH_SHORT
                            ).show()
                            endViewProcess()
                            startForEdit(context = this@NoteItemActivity, noteId = noteId.toInt(), viewMode = true)
                        } else {
                            Toast.makeText(
                                this@NoteItemActivity,
                                resources.getString(R.string.SaveError),
                                Toast.LENGTH_SHORT
                            ).show()
                            endViewProcess()
                        }
                    } else {
                        Toast.makeText(
                            this@NoteItemActivity,
                            resources.getString(R.string.SaveError),
                            Toast.LENGTH_SHORT
                        ).show()
                        endViewProcess()
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this@NoteItemActivity)
                        .setMessage("${resources.getString(R.string.Error)}: $e")
                        .setNegativeButton(resources.getString(R.string.Cancel)) { dialog, _ ->
                            dialog.dismiss()
                            endViewProcess()
                        }
                        .show()
                }
            }
        } else {
            var noteId: Long
            if (savedNote.id == null) {
                noteId = session.addNote(savedNote)
            } else {
                noteId = savedNote.id
                savedNote.updatedAt = System.currentTimeMillis()
                session.updateNote(savedNote)
            }
            Toast.makeText(
                this@NoteItemActivity,
                resources.getString(R.string.SuccessfulSaving),
                Toast.LENGTH_SHORT
            ).show()
            endViewProcess()
            startForEdit(context = this@NoteItemActivity, noteId = noteId.toInt(), viewMode = true)
        }
    }
    private fun startViewProcess() {
        progressBar.visibility = View.VISIBLE
    }
    private fun endViewProcess() {
        progressBar.visibility = View.GONE
    }
}