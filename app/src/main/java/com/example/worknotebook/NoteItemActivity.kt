package com.example.worknotebook

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Date
import java.util.Locale


class NoteItemActivity : AppCompatActivity() {

    private lateinit var btnMenu: ImageButton
    private lateinit var btnSaveNote: ImageView

    private lateinit var tvDateUpdate: TextView

    private lateinit var currentNote: Note
    private lateinit var changedNote: Note

    private var isEditMode = false
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
    }
    private fun initViews() {
        btnMenu = findViewById(R.id.btn_note_item_header_menu)
        btnSaveNote = findViewById(R.id.btn_save_note_header)
        tvDateUpdate = findViewById(R.id.tv_date_update)
    }
    private fun setupListeners() {
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.create_item_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_set_priority -> {
                    showPriorityDialog()
                    true
                }
                R.id.action_cancel -> {
                    finish() // Уходим со страницы
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
    private fun showPriorityDialog() {
        val priorities = NotePriority.entries.toTypedArray()
        val priorityNames = priorities.map { getString(it.displayNameResId) }

        val checkedItem = changedNote.priority.ordinal

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.select_priority)

        val adapter = object : ArrayAdapter<String>(this,
            android.R.layout.select_dialog_singlechoice,
            priorityNames) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).apply {
                    val priority = priorities[position]
                    setTextColor(ContextCompat.getColor(context, priority.colorResId))
                }
                return view
            }
        }

        builder.setSingleChoiceItems(adapter, checkedItem) { dialog, which ->
            changedNote.priority = priorities[which]
            updateNotePriority(changedNote.priority)
            dialog.dismiss()
        }

        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }
    private fun updateNotePriority(priority: NotePriority) {
        // Здесь обновляем поле priority в текущей заметке
        // Например: currentNote?.priority = priority
        val color = ContextCompat.getColor(this, priority.colorResId)  // TODO
        Toast.makeText(  // TODO
            this,
            "!!!: ${priority.colorResId} Note",
            Toast.LENGTH_LONG
        ).show()
        // Если нужно передать данные обратно в вызывающую активность
        val resultIntent = Intent()  // TODO
        resultIntent.putExtra("priority", priority.name)
        setResult(RESULT_OK, resultIntent)
    }

    private fun initCreateMode() {
        val currentTimestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(currentTimestamp))

        currentNote = Note(
            title = "",
            content = "",
            createdAt = currentTimestamp,
            updatedAt = currentTimestamp,
            priority = NotePriority.NORMAL,
            id = null,
            userId = 0,
            parentNoteId = null,
            meetingId = null
        )
        changedNote = Note(
            title = "",
            content = "",
            createdAt = currentTimestamp,
            updatedAt = currentTimestamp,
            priority = NotePriority.NORMAL,
            id = null,
            userId = 0,
            parentNoteId = null,
            meetingId = null
        )
        tvDateUpdate.apply {
            visibility = View.VISIBLE
            text = formattedDate
        }
        btnSaveNote.apply {
            visibility = View.VISIBLE
        }
    }
    private fun determineMode() {
        when {
            // Передан ID заметки - загружаем с сервера
            intent.hasExtra(EXTRA_NOTE_ID) -> {
                isEditMode = true
                isCreateMode = false
                val noteId = intent.getIntExtra(EXTRA_NOTE_ID, -1)
                if (noteId != -1) {
                    Toast.makeText(
                        this,
                        "Open: $noteId Note",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            // Передан полный объект Note - Режим редактирования
            intent.hasExtra(EXTRA_NOTE_DATA) -> {
                isEditMode = true
                isCreateMode = false
                var note: Note?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    note = intent.getParcelableExtra("note", Note::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    note = intent.getParcelableExtra(EXTRA_NOTE_DATA) as? Note
                }

                if (note != null) {
                    currentNote = note
                    Toast.makeText(
                        this,
                        "Note: $note",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {
                // Режим создания новой заметки
                isEditMode = false
                isCreateMode = true
                initCreateMode()
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_DATA = "extra_note_data"

        // Методы для удобного запуска активности
        fun startForCreate(context: Context) {
            val intent = Intent(context, NoteItemActivity::class.java)
            context.startActivity(intent)
        }
        // TODO
        fun startForEdit(context: Context, noteId: Int) {
            val intent = Intent(context, NoteItemActivity::class.java)
            intent.putExtra(EXTRA_NOTE_ID, noteId)
            context.startActivity(intent)
        }
        // TODO
        fun startForEdit(context: Context, note: Note) {
            val intent = Intent(context, NoteItemActivity::class.java)
            intent.putExtra(EXTRA_NOTE_DATA, note)
            context.startActivity(intent)
        }
    }

}